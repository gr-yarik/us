package LinearHashing;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.*;
import UnsortedFile.*;

public class BucketHeap<T extends StorableRecord> {

    private Heap<T> mainBucketsHeap;
    private Heap<T> overflowHeap;
    private int mainBucketsBlockSize;
    private int overflowBlockSize;
    private Class<T> recordClass;

    public void insertIntoBucket(int bucketNumber, List<T> records) {

        Bucket<T> bucket;
        if (mainBucketsHeap.checkIfBlockExists(bucketNumber)) {
            bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        } else {
            bucket = new Bucket<>(
                    mainBucketsHeap.getBlockingFactor(),
                    mainBucketsBlockSize,
                    recordClass);
        }

        List<OverflowBlockAndNumber> overflowBlocks = new ArrayList<OverflowBlockAndNumber>();
        OverflowBlockAndNumber lastOverflowBlock = null;

        {
            OverflowBlock<T> overflowBlock = null;
            int overflowBlockNumber;
            overflowBlockNumber = bucket.getFirstOverflowBlock();
            int neededSpaces = records.size();

            neededSpaces -= (mainBucketsHeap.getBlockingFactor() - bucket.getValidBlockCount());

            while (overflowBlockNumber != -1 && neededSpaces > 0) {
                overflowBlock = overflowHeap.readBlock(overflowBlockNumber, OverflowBlock.class);
                if (!overflowBlock.isFull()) {
                    overflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber));
                    neededSpaces -= (overflowHeap.getBlockingFactor() - overflowBlock.getValidBlockCount());
                }
                overflowBlockNumber = overflowBlock.getNextOverflowBlock();
            }

            if (neededSpaces > 0) {
                int requiredOverflowBlocks = minimalRequiredOverflowBlockNumber(neededSpaces, false);
                lastOverflowBlock = new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber);
                appendNewOverflowBlocks(requiredOverflowBlocks, bucket, overflowBlocks, lastOverflowBlock);
            }
        }

        {
            int currentRecordIndex = 0;
            while (currentRecordIndex < records.size() && !bucket.isFull()) {
                bucket.addRecord(records.get(currentRecordIndex));
                currentRecordIndex++;
            }

            int currentOverflowBlockIndex = 0;
            while (currentRecordIndex < records.size()) {
                OverflowBlockAndNumber blockEntry = overflowBlocks.get(currentOverflowBlockIndex);
                OverflowBlock<T> overflowBlock = (OverflowBlock<T>) blockEntry.block;
                if (overflowBlock.isFull()) {
                    currentOverflowBlockIndex++;
                    continue;
                }

                while (currentRecordIndex < records.size() && !overflowBlock.isFull()) {
                    overflowBlock.addRecord(records.get(currentRecordIndex));
                    bucket.incrementTotalElementCountBy(1);
                    currentRecordIndex++;
                }

                if (currentRecordIndex == records.size()) {
                    overflowBlock.setNextOverflowBlock(-1);
                }
                currentOverflowBlockIndex++;
            }
        }

        mainBucketsHeap.writeBlock(bucketNumber, bucket);
        if (lastOverflowBlock != null) {
            overflowHeap.writeBlock(lastOverflowBlock.number, lastOverflowBlock.block);
        }
        for (OverflowBlockAndNumber overflowBlockAndNumber : overflowBlocks) {
            overflowHeap.writeBlock(overflowBlockAndNumber.number, overflowBlockAndNumber.block);
        }
    }

    private void appendNewOverflowBlocks(int howManyBlocksToAdd, Bucket<T> bucket,
            List<OverflowBlockAndNumber> overflowBlocks, OverflowBlockAndNumber lastOverflowBlock) {
        int numberOfBlocksAtTheEnd = 0;
        for (int i = 0; i < howManyBlocksToAdd; i++) {
            OverflowBlock<T> newOverflowBlock = new OverflowBlock<>(overflowHeap.getBlockingFactor(),
                    overflowHeap.getBlockSize(), recordClass);
            int overflowBlockNumber = overflowHeap.getEmptyBlock();
            if (overflowBlockNumber == -1) {
                overflowBlockNumber = overflowHeap.getNumberForNewBlock() + numberOfBlocksAtTheEnd;
                numberOfBlocksAtTheEnd++;
            }

            if (i == 0) {
                if (lastOverflowBlock.block == null) {
                    bucket.setFirstOverflowBlock(overflowBlockNumber);
                } else {
                    lastOverflowBlock.block.setNextOverflowBlock(overflowBlockNumber);
                }
            } else {
                overflowBlocks.getLast().block.setNextOverflowBlock(overflowBlockNumber);
            }

            bucket.incrementOverflowBlockCountBy(1);
            overflowBlocks.add(new OverflowBlockAndNumber(newOverflowBlock, overflowBlockNumber));
        }

    }

    public record OverflowBlockAndNumber(OverflowBlock block, int number) {
    }

    public BucketHeap(String mainBucketsPath, String overflowBlocksPath, int mainBucketsBlockSize,
            int overflowBlockSize, Class<T> recordClass) {
        this.mainBucketsBlockSize = mainBucketsBlockSize;
        this.overflowBlockSize = overflowBlockSize;
        this.recordClass = recordClass;
        this.mainBucketsHeap = new Heap<>(mainBucketsPath, mainBucketsBlockSize, recordClass, true, 16);
        this.overflowHeap = new Heap<>(overflowBlocksPath, overflowBlockSize, recordClass, false, 8);
    }

    public BucketHeap(String mainBucketsPath, String mainMetadataPath,
            String overflowBlocksPath, String overflowMetadataPath, Class<T> recordClass) {
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

    public void insertIntoBucket(int bucketNumber, T record) {
        insertIntoBucket(bucketNumber, Arrays.asList(record));
    }

    public T get(int bucketNumber, T partialRecord) {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        T foundRecord = bucket.getRecord(partialRecord);
        if (foundRecord != null) {
            return foundRecord;
        }

        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        if (firstOverflowBlock != -1) {
            return searchOverflowChain(firstOverflowBlock, partialRecord);
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

                    int minRequiredOverflowBlocks = minimalRequiredOverflowBlockNumber(bucket.getTotalRecordCount(),
                            true);

                    if (minRequiredOverflowBlocks < bucket.getTotalOverflowBlockCount()
                            && bucket.getFirstOverflowBlock() != -1) {
                        List<OverflowBlockAndNumber> overflowBlocks = new ArrayList<OverflowBlockAndNumber>();
                        shuffle(bucket, minRequiredOverflowBlocks, overflowBlocks);
                    }
                },
                // record was not found in the main bucket. Trying to delete in overflow
                (Block<T> block) -> {
                    BlockAndNumber bucketAndNumber = new BlockAndNumber(block, bucketNumber);
                    deletionResult[0] = deleteFromOverflowChain(bucketAndNumber, partialRecord);
                });

        return deletionResult[0];

    }

    private boolean deleteFromOverflowChain(BlockAndNumber bucketAndNumber, T partialRecord) {
        Bucket<T> bucket = (Bucket<T>) bucketAndNumber.blockInstance;
        int bucketNumber = bucketAndNumber.blockNumber;

        AtomicInteger nextOverflowBlockNumber = new AtomicInteger(bucket.getFirstOverflowBlock());
        AtomicReference<OverflowBlock<T>> previousOverflowBlock = new AtomicReference<>();
        AtomicInteger previousOverflowBlockNumber = new AtomicInteger();
        AtomicBoolean deletionOccurred = new AtomicBoolean(false);

        List<OverflowBlockAndNumber> visitedOverflowBlocks = new ArrayList<>();

        while (nextOverflowBlockNumber.get() != -1) {
            final int currentBlockNum = nextOverflowBlockNumber.get();

            boolean deleted = overflowHeap.delete(currentBlockNum, partialRecord,

                    // Record was found in the current overflow block and was deleted
                    (Block<T> block) -> {
                        deletionOccurred.set(true);
                        visitedOverflowBlocks
                                .add(new OverflowBlockAndNumber((OverflowBlock<T>) block, currentBlockNum));

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

                        int minRequiredOverflowBlocks = minimalRequiredOverflowBlockNumber(
                                bucket.getTotalRecordCount(), true);
                        if (minRequiredOverflowBlocks < bucket.getTotalOverflowBlockCount()
                                && bucket.getFirstOverflowBlock() != -1) {
                            shuffle(bucket, minRequiredOverflowBlocks, visitedOverflowBlocks);
                        }
                        mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    },

                    // Record was not found in the current overflow block
                    (Block<T> block) -> {
                        OverflowBlock<T> overflowBlock = (OverflowBlock<T>) block;
                        visitedOverflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, currentBlockNum));

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

    private void shuffle(Bucket<T> bucket, int minRequiredOverflowBlocks, List<OverflowBlockAndNumber> overflowBlocks) {
        List<T> allRecords = new ArrayList<T>();
        collectAllRecords(bucket, allRecords, overflowBlocks);
        List<OverflowBlockAndNumber> freedOverflowBlocks = insertCompactly(bucket, allRecords, overflowBlocks);
        clearOverflowChain(freedOverflowBlocks);
        overflowHeap.truncateAtTheEndIfPossible();
        if (bucket.getTotalRecordCount() != allRecords.size() ||
                bucket.getTotalOverflowBlockCount() != minRequiredOverflowBlocks) {
            throw new Error("Shuffling went wrong");
        }
    }

    private int minimalRequiredOverflowBlockNumber(int totalElements, boolean includingBucket) {
        if (includingBucket)
            return Math.abs((int) Math.ceil(
                    (double) (totalElements - mainBucketsHeap.getBlockingFactor()) / overflowHeap.getBlockingFactor()));
        else
            return (int) Math.ceil((double) totalElements / overflowHeap.getBlockingFactor());
    }

    private List<OverflowBlockAndNumber> insertCompactly(
            Bucket<T> bucket,
            List<T> allRecords,
            List<OverflowBlockAndNumber> overflowBlocks) {
        int currentRecordIndex = 0;
        while (currentRecordIndex < allRecords.size() && !bucket.isFull()) {
            bucket.addRecord(allRecords.get(currentRecordIndex));
            currentRecordIndex++;
        }

        int currentOverflowBlockIndex = 0;
        while (currentRecordIndex < allRecords.size()) {
            OverflowBlockAndNumber blockEntry = overflowBlocks.get(currentOverflowBlockIndex);
            OverflowBlock<T> overflowBlock = (OverflowBlock<T>) blockEntry.block;
            int blockNumber = blockEntry.number;
            if (overflowBlock.isFull()) {
                currentOverflowBlockIndex++;
                continue;
            }

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

        List<OverflowBlockAndNumber> freedOverflowBlocks = new ArrayList<>(
                overflowBlocks.subList(currentOverflowBlockIndex, overflowBlocks.size()));
        return freedOverflowBlocks;
    }

    public void clearOverflowChain(List<OverflowBlockAndNumber> overflowBlocks) {
        for (int i = 0; i < overflowBlocks.size(); i++) {
            OverflowBlockAndNumber blockEntry = overflowBlocks.get(i);
            OverflowBlock<T> freedOverflowBlock = (OverflowBlock<T>) blockEntry.block;
            if (!freedOverflowBlock.isEmpty()) {
                throw new Error("Block is not empty. This should not happen");
            }
            freedOverflowBlock.setNextOverflowBlock(-1);
            overflowHeap.writeBlock(blockEntry.number, freedOverflowBlock);
            overflowHeap.manageEmptyBlock(blockEntry.number);
        }
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

    public void collectAllRecords(Bucket<T> bucket, List<T> records,
            List<OverflowBlockAndNumber> visitedOverflowBlocks) {
        records.addAll(bucket.getAllValidRecords());
        bucket.deleteAllRecords();

        for (OverflowBlockAndNumber overflowBlock : visitedOverflowBlocks) {
            records.addAll(overflowBlock.block.getAllValidRecords());
            bucket.decrementTotalElementCountBy(overflowBlock.block.deleteAllRecords());
        }

        int overflowBlockNumber;
        if (visitedOverflowBlocks.isEmpty()) {
            overflowBlockNumber = bucket.getFirstOverflowBlock();
        } else {
            overflowBlockNumber = ((OverflowBlock<T>) visitedOverflowBlocks.getLast().block)
                    .getNextOverflowBlock();
        }

        while (overflowBlockNumber != -1) {
            OverflowBlock<T> overflowBlock = overflowHeap.readBlock(overflowBlockNumber, OverflowBlock.class);
            records.addAll(overflowBlock.getAllValidRecords());
            visitedOverflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber));

            bucket.decrementTotalElementCountBy(overflowBlock.deleteAllRecords());
            overflowBlockNumber = overflowBlock.getNextOverflowBlock();
        }

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
