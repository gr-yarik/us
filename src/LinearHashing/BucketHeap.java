package LinearHashing;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import UnsortedFile.*;

public class BucketHeap<T extends StorableRecord> {

    private Heap<T> mainBucketsHeap;
    private Heap<T> overflowHeap;
    private int mainBucketsBlockSize;
    private int overflowBlockSize;
    private Class<T> recordClass;

    public BucketHeap(String mainBucketsPath, String overflowBlocksPath, int mainBucketsBlockSize,
            int overflowBlockSize, Class<T> recordClass) {
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

    public boolean insertIntoBucket(int bucketNumber, T record) {
        Bucket<T> bucket;
        if (mainBucketsHeap.checkIfBlockExists(bucketNumber)) {
            bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        } else {
            bucket = new Bucket<>(
                    mainBucketsHeap.getBlockingFactor(),
                    mainBucketsBlockSize,
                    recordClass);
        }

        if (!bucket.isFull()) {
            boolean inserted = bucket.addRecord(record);
            if (inserted) {
                mainBucketsHeap.writeBlock(bucketNumber, bucket);
                return true;
            }
        }

        return insertIntoOverflow(bucket, bucketNumber, record);
    }

    private boolean insertIntoOverflow(Bucket<T> bucket, int bucketNumber, T record) {

        int firstOverflowBlock = bucket.getFirstOverflowBlock();

        if (firstOverflowBlock == -1) {
            OverflowBlock<T> newOverflowBlock = new OverflowBlock<>(overflowHeap.getBlockingFactor(),
                    overflowHeap.getBlockSize(), recordClass);
            newOverflowBlock.addRecord(record);

            int overflowBlockNum = overflowHeap.getEmptyBlock();
            if (overflowBlockNum == -1) {
                overflowBlockNum = overflowHeap.getNumberForNewBlock();
            }

            bucket.setFirstOverflowBlock(overflowBlockNum);
            bucket.setOverflowBlockCount(1);
            bucket.incrementTotalElementCountBy(1);
            mainBucketsHeap.writeBlock(bucketNumber, bucket);
            overflowHeap.writeBlock(overflowBlockNum, newOverflowBlock);
            return true;
        } else {
            OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
            int currentBlockNum = firstOverflowBlock;
            int blocksTraversed = 1;

            while (true) {
                if (!current.isFull()) {
                    current.addRecord(record);
                    overflowHeap.writeBlock(currentBlockNum, current);
                    bucket.incrementTotalElementCountBy(1);

                    mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    return true;
                }
                if (current.getNextOverflowBlock() == -1) {
                    if (blocksTraversed != bucket.getTotalOverflowBlockCount()) {
                        throw new Error("Traversed and expected number of overflow buckets does not match.");
                    }

                    OverflowBlock<T> newOverflowBlock = new OverflowBlock<>(overflowHeap.getBlockingFactor(),
                            overflowHeap.getBlockSize(), recordClass);
                    newOverflowBlock.addRecord(record);
                    int newOverflowBlockNumber = overflowHeap.getEmptyBlock();
                    if (newOverflowBlockNumber == -1) {
                        newOverflowBlockNumber = overflowHeap.getNumberForNewBlock();
                    }
                    current.setNextOverflowBlock(newOverflowBlockNumber);
                    overflowHeap.writeBlock(currentBlockNum, current);
                    overflowHeap.writeBlock(newOverflowBlockNumber, newOverflowBlock);

                    bucket.incrementOverflowBlockCountBy(1);
                    bucket.incrementTotalElementCountBy(1);
                    mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    return true;
                }

                currentBlockNum = current.getNextOverflowBlock();
                current = overflowHeap.readBlock(currentBlockNum, OverflowBlock.class);
                blocksTraversed++;
            }
        }
    }

    public T get(int bucketNumber, T partialRecord) {
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

    public record BlockAndNumber(Block blockInstance, int blockNumber) {
    }

    public boolean delete(int bucketNumber, T partialRecord) {

        Boolean[] deletionResult = { Boolean.FALSE };

        deletionResult[0] = mainBucketsHeap.delete(bucketNumber, partialRecord,

                // record was found in the main bucket and deleted
                (Block<T> block) -> {
                    Bucket<T> bucket = (Bucket<T>) block;

                    int minRequiredOverflowBlocks = minimalRequiredOverflowBlockNumber(bucket.getTotalRecordCount());

                    if (minRequiredOverflowBlocks < bucket.getTotalOverflowBlockCount() && bucket.getFirstOverflowBlock() != -1) {
                        List<T> allRecords = new ArrayList<T>();
                        List<BlockAndNumber> overflowBlocks = new ArrayList<BlockAndNumber>();
                        collectAllRecords(bucket, allRecords,overflowBlocks);
                        shuffle(bucket, allRecords, minRequiredOverflowBlocks, overflowBlocks);
                    }
                },
                // record was not found in the main bucket. Trying to delete in overflow
                (Block<T> block) -> {
                    BlockAndNumber bucketAndNumber = new BlockAndNumber(block, bucketNumber);
                    deletionResult[0] = deleteFromOverflowChain(bucketAndNumber, partialRecord);
                }
        );

        return deletionResult[0];

    }

    private boolean deleteFromOverflowChain(BlockAndNumber bucketAndNumber, T partialRecord) {
        Bucket<T> bucket = (Bucket<T>) bucketAndNumber.blockInstance;
        int bucketNumber = bucketAndNumber.blockNumber;

        AtomicInteger nextOverflowBlockNumber = new AtomicInteger(bucket.getFirstOverflowBlock());
        AtomicReference<OverflowBlock<T>> previousOverflowBlock = new AtomicReference<>();
        AtomicInteger previousOverflowBlockNumber = new AtomicInteger();
        AtomicBoolean deletionOccurred = new AtomicBoolean(false);

        List<BlockAndNumber> visitedOverflowBlocks = new ArrayList<>();

        while (nextOverflowBlockNumber.get() != -1) {
            final int currentBlockNum = nextOverflowBlockNumber.get();

            boolean deleted = overflowHeap.delete(currentBlockNum, partialRecord,

                    // Record was found in the current overflow block and was deleted
                    (Block<T> block) -> {
                        deletionOccurred.set(true);
                        visitedOverflowBlocks.add(new BlockAndNumber(block, currentBlockNum));

                        bucket.decrementTotalElementCountBy(1);
                        if (block.isEmpty()) {
                            bucket.decrementOverflowBlockCountBy(1);

                            if (previousOverflowBlock.get() == null) {
                                // This was the first overflow block
                                bucket.setFirstOverflowBlock(-1);
                            } else {
                                // Unlink this empty block from the chain
                                previousOverflowBlock.get().setNextOverflowBlock(-1);
                                overflowHeap.writeBlock(previousOverflowBlockNumber.get(), previousOverflowBlock.get());
                            }
                        }

                        int minRequiredOverflowBlocks = minimalRequiredOverflowBlockNumber(bucket.getTotalRecordCount());
                        if (minRequiredOverflowBlocks < bucket.getTotalOverflowBlockCount() && bucket.getFirstOverflowBlock() != -1) {
                            List<T> allRecords = new ArrayList<T>();
                            collectAllRecords(bucket, allRecords, visitedOverflowBlocks);
                            shuffle(bucket, allRecords, minRequiredOverflowBlocks, visitedOverflowBlocks);
                        }
                        mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    },

                    // Record was not found in the current overflow block
                    (Block<T> block) -> {
                        OverflowBlock<T> overflowBlock = (OverflowBlock<T>) block;
                        visitedOverflowBlocks.add(new BlockAndNumber(overflowBlock, currentBlockNum));

                        previousOverflowBlock.set(overflowBlock);
                        previousOverflowBlockNumber.set(currentBlockNum);
                        nextOverflowBlockNumber.set(overflowBlock.getNextOverflowBlock());
                    });

            if (deleted) {
                break;
            }
        }

        return deletionOccurred.get();
    }

    private int minimalRequiredOverflowBlockNumber(int totalElements) {
        int requiredOverflowBlocks = 0;
        while (true) {
            if (totalElements > mainBucketsHeap.getBlockingFactor()
                    + requiredOverflowBlocks * overflowHeap.getBlockingFactor()) {
                requiredOverflowBlocks++;
            } else {
                break;
            }
        }
        return requiredOverflowBlocks;
    }

    private void shuffle(
            Bucket<T> bucket,
            List<T> allRecords,
            int minRequiredOverflowBlocks,
            List<BlockAndNumber> visitedOverflowBlocks
    ) {
        
        int currentRecordIndex = 0;
        while (currentRecordIndex < allRecords.size() && !bucket.isFull()) {
            bucket.addRecord(allRecords.get(currentRecordIndex));
            currentRecordIndex++;
        }
       
        int currentOverflowBlockIndex = 0;
        while (currentRecordIndex < allRecords.size()) {
            BlockAndNumber blockEntry = visitedOverflowBlocks.get(currentOverflowBlockIndex);
            OverflowBlock<T> overflowBlock = (OverflowBlock<T>) blockEntry.blockInstance;
            int blockNumber = blockEntry.blockNumber;

            while (currentRecordIndex < allRecords.size() && !overflowBlock.isFull()) {
                overflowBlock.addRecord(allRecords.get(currentRecordIndex));
                bucket.incrementTotalElementCountBy(1);
                currentRecordIndex++;
            }

            if (currentRecordIndex == allRecords.size()) {
                overflowBlock.setNextOverflowBlock(-1);
            }

            overflowHeap.writeBlock(blockNumber, overflowBlock);
            bucket.incrementOverflowBlockCountBy(1);
            currentOverflowBlockIndex++;
        }

        if (bucket.getTotalRecordCount() != allRecords.size() ||
            bucket.getTotalOverflowBlockCount() != minRequiredOverflowBlocks)
        {
            throw new Error("Shuffling went wrong");
        }  

        List<BlockAndNumber> freedOverflowBlocks = new ArrayList<>(
            visitedOverflowBlocks.subList(currentOverflowBlockIndex, visitedOverflowBlocks.size()));
        clearOverflowChain(freedOverflowBlocks);
    }

    public void clearOverflowChain(List<BlockAndNumber> overflowBlocks) {
        for (int i = 0; i < overflowBlocks.size(); i++) {
            BlockAndNumber blockEntry = overflowBlocks.get(i);
            OverflowBlock<T> freedOverflowBlock = (OverflowBlock<T>)blockEntry.blockInstance;
            freedOverflowBlock.setNextOverflowBlock(-1);
            overflowHeap.writeBlock(blockEntry.blockNumber, freedOverflowBlock);
            overflowHeap.manageEmptyBlock(blockEntry.blockNumber);
        }
        overflowHeap.truncateAtTheEndIfPossible();
    }

    private T searchOverflowChain(int firstOverflowBlock, T partialRecord) {
        OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
        while (true) {
            if (current.getRecord(partialRecord) != null) {
                return current.getRecord(partialRecord);
            }
            if (current.getNextOverflowBlock() == -1) {
                return null;
            }
            current = overflowHeap.readBlock(current.getNextOverflowBlock(), OverflowBlock.class);
        }
    }

    public void collectAllRecords(Bucket<T> bucket, List<T> records, List<BlockAndNumber> visitedOverflowBlocks) {
        records.addAll(bucket.getAllValidRecords());
        bucket.deleteAllRecords();

        for (BlockAndNumber overflowBlock : visitedOverflowBlocks) {
            records.addAll(overflowBlock.blockInstance.getAllValidRecords());
            bucket.decrementTotalElementCountBy(overflowBlock.blockInstance.deleteAllRecords());
        }

        int overflowBlockNumber;
        if (visitedOverflowBlocks.isEmpty()) {
            overflowBlockNumber = bucket.getFirstOverflowBlock();
        } else {
            overflowBlockNumber = ((OverflowBlock<T>) visitedOverflowBlocks.getLast().blockInstance).getNextOverflowBlock();
        }

        while (overflowBlockNumber != -1) {
            OverflowBlock<T> overflowBlock = overflowHeap.readBlock(overflowBlockNumber, OverflowBlock.class);
            records.addAll(overflowBlock.getAllValidRecords());
            visitedOverflowBlocks.add(new BlockAndNumber(overflowBlock, overflowBlockNumber));
            bucket.decrementTotalElementCountBy(overflowBlock.deleteAllRecords());
            overflowBlockNumber = overflowBlock.getNextOverflowBlock();
        }

        bucket.setOverflowBlockCount(0);

        if (bucket.getTotalRecordCount() != 0) {
            throw new Error("An error occurred during collecting all records in the chain");
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

    public void close() {
        if (mainBucketsHeap != null) {
            mainBucketsHeap.close();
        }
        if (overflowHeap != null) {
            overflowHeap.close();
        }
    }
}
