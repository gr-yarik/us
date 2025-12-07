package UnsortedFile;

import java.io.IOException;

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
                     String overflowBlocksPath, String overflowMetadataPath, Class<T> recordClass) {
        this.recordClass = recordClass;
        this.mainBucketsHeap = new Heap<>(mainBucketsPath, mainMetadataPath, recordClass, true, 16);
        this.overflowHeap = new Heap<>(overflowBlocksPath, overflowMetadataPath, recordClass, false, 8);
        this.mainBucketsBlockSize = mainBucketsHeap.getBlockSize();
        this.overflowBlockSize = overflowHeap.getBlockSize();
    }
    
    public boolean insertIntoBucket(int bucketNumber, T record) throws IOException {
        ensureBucketExists(bucketNumber);
        
        Bucket<T> bucket = readBucket(bucketNumber);
        if (!bucket.isFull()) {
            boolean inserted = bucket.addRecord(record);
            if (inserted) {
                bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);
                writeBucket(bucketNumber, bucket);
                return true;
            }
        }
        
        return insertIntoOverflow(bucketNumber, record);
    }
    
    private boolean insertIntoOverflow(int bucketNumber, T record) throws IOException {
        Bucket<T> bucket = readBucket(bucketNumber);
        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        
        if (firstOverflowBlock == -1) {
            OverflowBlock<T> newOverflow = createEmptyOverflowBlock();
            newOverflow.addRecord(record);
            int overflowBlockNum = allocateOverflowBlock(newOverflow);
            bucket.setFirstOverflowBlock(overflowBlockNum);
            bucket.setOverflowBucketCount(bucket.getOverflowBucketCount() + 1);
        } else {

            int overflowBucketCount = bucket.getOverflowBucketCount();
            OverflowBlock<T> current = readOverflowBlock(firstOverflowBlock);
            int currentBlockNum = firstOverflowBlock;
            OverflowBlock<T> targetBlock = null;
            int targetBlockNum = -1;
            int blocksTraversed = 1; 

            while (blocksTraversed <= overflowBucketCount) {
                if (!current.isFull()) {
                    targetBlock = current;
                    targetBlockNum = currentBlockNum;
                    break;
                }
                if (current.getNextOverflowBlock() == -1) {
                
                    targetBlock = current;
                    targetBlockNum = currentBlockNum;
                    break;
                }
               
                currentBlockNum = current.getNextOverflowBlock();
                current = readOverflowBlock(currentBlockNum);
                blocksTraversed++;
            }

            if (targetBlock != null && !targetBlock.isFull()) {
                targetBlock.addRecord(record);
                writeOverflowBlock(targetBlockNum, targetBlock);
            } else if (targetBlock != null) {
                OverflowBlock<T> newOverflow = createEmptyOverflowBlock();
                newOverflow.addRecord(record);
                int newOverflowBlockNum = allocateOverflowBlock(newOverflow);
                targetBlock.setNextOverflowBlock(newOverflowBlockNum);
                writeOverflowBlock(targetBlockNum, targetBlock);
                bucket.setOverflowBucketCount(bucket.getOverflowBucketCount() + 1);
            }
        }
        
        bucket.setTotalElementCount(bucket.getTotalElementCount() + 1);
        writeBucket(bucketNumber, bucket);
        return true;
    }
    
    public void ensureBucketExists(int bucketNumber) throws IOException {
        mainBucketsHeap.ensureBlockExists(bucketNumber);
    }
    
    public void extendToBucketCount(int bucketCount) throws IOException {
        mainBucketsHeap.extendToBlockCount(bucketCount);
    }
    
    @SuppressWarnings("unchecked")
    public Bucket<T> readBucket(int bucketNumber) throws IOException {
        return (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
    }
    
    public void writeBucket(int bucketNumber, Bucket<T> bucket) throws IOException {
        mainBucketsHeap.writeBlock(bucketNumber, bucket);
    }
    
    public T get(int bucketNumber, T partialRecord) throws IOException {
        T found = mainBucketsHeap.get(bucketNumber, partialRecord);
        if (found != null) {
            return found;
        }
        
        Bucket<T> bucket = readBucket(bucketNumber);
        if (bucket != null) {
            int firstOverflowBlock = bucket.getFirstOverflowBlock();
            if (firstOverflowBlock != -1) {
                return searchOverflowChain(firstOverflowBlock, partialRecord);
            }
        }
        
        return null;
    }
    
    public boolean delete(int bucketNumber, T partialRecord) throws IOException {
        Bucket<T> bucket = readBucket(bucketNumber);
        if (bucket == null) {
            return false;
        }
        
        boolean deleted = bucket.delete(partialRecord);
        if (deleted) {
            bucket.setTotalElementCount(bucket.getTotalElementCount() - 1);
            writeBucket(bucketNumber, bucket);
            return true;
        }
        
        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        if (firstOverflowBlock != -1) {
            boolean deletedFromOverflow = deleteFromOverflowChain(bucketNumber, firstOverflowBlock, partialRecord);
            if (deletedFromOverflow) {
                bucket = readBucket(bucketNumber);
                bucket.setTotalElementCount(bucket.getTotalElementCount() - 1);
                writeBucket(bucketNumber, bucket);
                return true;
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private OverflowBlock<T> readOverflowBlock(int blockNumber) throws IOException {
        return overflowHeap.readBlock(blockNumber, OverflowBlock.class);
    }
    
    private void writeOverflowBlock(int blockNumber, OverflowBlock<T> overflowBlock) throws IOException {
        overflowHeap.writeBlock(blockNumber, overflowBlock);
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
        writeOverflowBlock(blockNumber, overflowBlock);
        return blockNumber;
    }
    private T searchOverflowChain(int firstOverflowBlock, T partialRecord) throws IOException {
        OverflowBlock<T> current = readOverflowBlock(firstOverflowBlock);
        while (current != null) {
            int index = current.findRecordIndex(partialRecord);
            if (index != -1) {
                return current.getRecord(index);
            }
            if (current.getNextOverflowBlock() == -1) {
                break;
            }
            current = readOverflowBlock(current.getNextOverflowBlock());
        }
        return null;
    }
    
    private boolean deleteFromOverflowChain(int bucketNumber, int firstOverflowBlock, T partialRecord) throws IOException {
        OverflowBlock<T> current = readOverflowBlock(firstOverflowBlock);
        OverflowBlock<T> prev = null;
        int currentBlockNum = firstOverflowBlock;
        
        while (current != null) {
            if (current.delete(partialRecord)) {
                writeOverflowBlock(currentBlockNum, current);
                
                if (current.isEmpty() && prev != null) {
                    prev.setNextOverflowBlock(current.getNextOverflowBlock());
                    int prevBlockNum = firstOverflowBlock;
                    OverflowBlock<T> temp = readOverflowBlock(firstOverflowBlock);
                    while (temp.getNextOverflowBlock() != currentBlockNum) {
                        prevBlockNum = temp.getNextOverflowBlock();
                        temp = readOverflowBlock(prevBlockNum);
                    }
                    writeOverflowBlock(prevBlockNum, prev);
                    
                    Bucket<T> bucket = readBucket(bucketNumber);
                    bucket.setOverflowBucketCount(bucket.getOverflowBucketCount() - 1);
                    writeBucket(bucketNumber, bucket);
                } else if (current.isEmpty() && prev == null) {
                    Bucket<T> bucket = readBucket(bucketNumber);
                    bucket.setFirstOverflowBlock(current.getNextOverflowBlock());
                    bucket.setOverflowBucketCount(bucket.getOverflowBucketCount() - 1);
                    writeBucket(bucketNumber, bucket);
                }
                
                return true;
            }
            
            prev = current;
            if (current.getNextOverflowBlock() == -1) {
                break;
            }
            currentBlockNum = current.getNextOverflowBlock();
            current = readOverflowBlock(currentBlockNum);
        }
        
        return false;
    }
    
    public void collectAllRecords(int bucketNumber, java.util.List<T> records) throws IOException {
        Bucket<T> bucket = readBucket(bucketNumber);
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
            OverflowBlock<T> current = readOverflowBlock(firstOverflowBlock);
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
                current = readOverflowBlock(current.getNextOverflowBlock());
            }
        }
    }
    
    public void clearOverflowChain(int bucketNumber) throws IOException {
        Bucket<T> bucket = readBucket(bucketNumber);
        if (bucket != null) {
            bucket.setFirstOverflowBlock(-1);
            bucket.setOverflowBucketCount(0);
            writeBucket(bucketNumber, bucket);
        }
    }
    
    public int getTotalBuckets() throws IOException {
        return mainBucketsHeap.getTotalBlocks();
    }
    
    public int getTotalOverflowBlocks() throws IOException {
        return overflowHeap.getTotalBlocks();
    }
    
    public int getBlockingFactor() {
        return mainBucketsHeap.getBlockingFactor();
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
    
    public void close() throws IOException {
        if (mainBucketsHeap != null) {
            mainBucketsHeap.close();
        }
        if (overflowHeap != null) {
            overflowHeap.close();
        }
    }
}
