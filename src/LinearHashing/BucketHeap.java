package LinearHashing;

import java.io.IOException;

import UnsortedFile.Heap;
import UnsortedFile.StorableRecord;

public class BucketHeap<T extends StorableRecord> {
    
    private Heap<T> mainBucketsHeap;
    private Heap<T> overflowHeap;
    private int mainBucketsBlockSize;
    private int overflowBlockSize;
    private Class<T> recordClass;
    
    
    public BucketHeap(String mainBucketsPath, String overflowBlocksPath, int mainBucketsBlockSize, int overflowBlockSize, Class<T> recordClass) {
        this.mainBucketsBlockSize = mainBucketsBlockSize;
        this.overflowBlockSize = overflowBlockSize;
        this.recordClass = recordClass;
        this.mainBucketsHeap = new Heap<>(mainBucketsPath, mainBucketsBlockSize, recordClass, true, 16);
        this.overflowHeap = new Heap<>(overflowBlocksPath, overflowBlockSize, recordClass, false, 8);
    }
    
    public BucketHeap(String mainBucketsPath, String mainMetadataPath, 
                     String overflowBlocksPath, String overflowMetadataPath, Class<T> recordClass) throws IOException {
        this.recordClass = recordClass;
        UnsortedFile.BlockManager mainMetadata = new UnsortedFile.BlockManager(mainMetadataPath);
        UnsortedFile.BlockManager overflowMetadata = new UnsortedFile.BlockManager(overflowMetadataPath);
        this.mainBucketsBlockSize = mainMetadata.getBlockSize();
        this.overflowBlockSize = overflowMetadata.getBlockSize();
        mainMetadata.close();
        overflowMetadata.close();
        this.mainBucketsHeap = new Heap<T>(mainBucketsPath, mainBucketsBlockSize, recordClass, true, 16);
        this.overflowHeap = new Heap<T>(overflowBlocksPath, overflowBlockSize, recordClass, false, 8);
    }
    
    public boolean insertIntoBucket(int bucketNumber, T record) throws IOException {
        Bucket<T> bucket;
        if(mainBucketsHeap.checkIfBlockExists(bucketNumber)) {
            bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        } else {
            bucket = new Bucket<>(
                mainBucketsHeap.getBlockingFactor(),
                mainBucketsBlockSize,
                recordClass
            );
        }
        
        if (!bucket.isFull()) {
            boolean inserted = bucket.addRecord(record);
            if (inserted) {
                bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);
                mainBucketsHeap.writeBlock(bucketNumber, bucket);
                return true;
            }
        }
        
        return insertIntoOverflow(bucket, bucketNumber, record);
    }
    
    private boolean insertIntoOverflow(Bucket<T> bucket, int bucketNumber, T record) throws IOException {

        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        
        if (firstOverflowBlock == -1) {
            OverflowBlock<T> newOverflow = createEmptyOverflowBlock();
            newOverflow.addRecord(record);
            int overflowBlockNum = allocateOverflowBlock(newOverflow);
            bucket.setFirstOverflowBlock(overflowBlockNum);
            bucket.setOverflowBucketCount(1);
            bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);
            mainBucketsHeap.writeBlock(bucketNumber, bucket);
            return true;
        } else {
            OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
            int currentBlockNum = firstOverflowBlock;
            int blocksTraversed = 1; 

            while (true) {
                if (!current.isFull()) {
                    current.addRecord(record);
                    overflowHeap.writeBlock(currentBlockNum, current);
                    bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);

                    mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    return true;
                }
                if (current.getNextOverflowBlock() == -1) {
                    if(blocksTraversed != bucket.getOverflowBucketCount()) {
                        throw new IOException("Traversed and expected number of overflow buckets does not match.");
                    }

                    OverflowBlock<T> newOverflow = createEmptyOverflowBlock();
                    newOverflow.addRecord(record);
                    int newOverflowBlockNum = allocateOverflowBlock(newOverflow);
                    current.setNextOverflowBlock(newOverflowBlockNum);
                    overflowHeap.writeBlock(currentBlockNum, current);

                    bucket.setOverflowBucketCount(bucket.getOverflowBucketCount() + 1);
                    bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);
                    mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    return true;
                }
               
                currentBlockNum = current.getNextOverflowBlock();
                current = overflowHeap.readBlock(currentBlockNum, OverflowBlock.class);
                blocksTraversed++;
            }
        }
    }
    
    
    public T get(int bucketNumber, T partialRecord) throws IOException {
        T found = mainBucketsHeap.get(bucketNumber, partialRecord);
        if (found != null) {
            return found;
        }
        
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket != null) {
            int firstOverflowBlock = bucket.getFirstOverflowBlock();
            if (firstOverflowBlock != -1) {
                return searchOverflowChain(firstOverflowBlock, partialRecord);
            }
        }
        
        return null;
    }
    
    public boolean delete(int bucketNumber, T partialRecord) throws IOException {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket == null) {
            return false;
        }
        
        boolean deleted = bucket.delete(partialRecord);
        if (deleted) {
            bucket.setTotalElementCount(bucket.getTotalElementCount() - 1);
            mainBucketsHeap.writeBlock(bucketNumber, bucket);
            return true;
        }
        
        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        if (firstOverflowBlock != -1) {
            boolean deletedFromOverflow = deleteFromOverflowChain(bucketNumber, firstOverflowBlock, partialRecord);
            if (deletedFromOverflow) {
                bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
                bucket.setTotalElementCount(bucket.getTotalElementCount() - 1);
                mainBucketsHeap.writeBlock(bucketNumber, bucket);
                return true;
            }
        }
        
        return false;
    }
    
    
    private OverflowBlock<T> createEmptyOverflowBlock() {
        return new OverflowBlock<>(
            overflowHeap.getBlockingFactor(),
            overflowHeap.getBlockSize(),
            recordClass
        );
    }
    
    private int allocateOverflowBlock(OverflowBlock<T> overflowBlock) throws IOException {
        int blockNumber = overflowHeap.getTotalBlocks();
        overflowHeap.writeBlock(blockNumber, overflowBlock);
        return blockNumber;
    }
    private T searchOverflowChain(int firstOverflowBlock, T partialRecord) throws IOException {
        OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
        while (current != null) {
            int index = current.findRecordIndex(partialRecord);
            if (index != -1) {
                return current.getRecord(index);
            }
            if (current.getNextOverflowBlock() == -1) {
                break;
            }
            current = overflowHeap.readBlock(current.getNextOverflowBlock(), OverflowBlock.class);
        }
        return null;
    }
    
    private boolean deleteFromOverflowChain(int bucketNumber, int firstOverflowBlock, T partialRecord) throws IOException {
        OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
        OverflowBlock<T> prev = null;
        int currentBlockNum = firstOverflowBlock;
        
        while (current != null) {
            if (current.delete(partialRecord)) {
                overflowHeap.writeBlock(currentBlockNum, current);
                return true;
            }
            
            prev = current;
            if (current.getNextOverflowBlock() == -1) {
                break;
            }
            currentBlockNum = current.getNextOverflowBlock();
            current = overflowHeap.readBlock(currentBlockNum, OverflowBlock.class);
        }
        
        return false;
    }
    
    public void collectAllRecords(int bucketNumber, java.util.List<T> records) throws IOException {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket != null) {
            for (int i = 0; i < bucket.getValidBlockCount(); i++) {
                T record = bucket.getRecord(i);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        
        int firstOverflowBlock = bucket != null ? bucket.getFirstOverflowBlock() : -1;
        if (firstOverflowBlock != -1) {
            OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
            while (current != null) {
                for (int i = 0; i < current.getValidBlockCount(); i++) {
                    T record = current.getRecord(i);
                    if (record != null) {
                        records.add(record);
                    }
                }
                if (current.getNextOverflowBlock() == -1) {
                    break;
                }
                current = overflowHeap.readBlock(current.getNextOverflowBlock(), OverflowBlock.class);
            }
        }
    }
    
    public void clearOverflowChain(int bucketNumber) throws IOException {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket != null) {
            bucket.setFirstOverflowBlock(-1);
            bucket.setOverflowBucketCount(0);
            mainBucketsHeap.writeBlock(bucketNumber, bucket);
        }
    }
    
    
    public int getBlockSize() {
        return mainBucketsBlockSize;
    }
    
    public int getMainBucketsBlockSize() {
        return mainBucketsBlockSize;
    }
    
    public int getOverflowBlockSize() {
        return overflowBlockSize;
    }
    
    public Heap<T> getMainBucketsHeap() {
        return mainBucketsHeap;
    }
    
    public Heap<T> getOverflowHeap() {
        return overflowHeap;
    }
    
    public void close() throws IOException {
        if (mainBucketsHeap != null) {
            mainBucketsHeap.close();
        }
        if (overflowHeap != null) {
            overflowHeap.close();
        }
    }
}

