package LinearHashing;

import java.util.*;
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
            int overflowBlockNumber;
            overflowBlockNumber = bucket.getFirstOverflowBlock();
            int neededSpaces = records.size();

            neededSpaces -= (mainBucketsHeap.getBlockingFactor() - bucket.getValidBlockCount());

            while (overflowBlockNumber != -1 && neededSpaces > 0) {
                OverflowBlock<T> overflowBlock = overflowHeap.readBlock(overflowBlockNumber, OverflowBlock.class);
                lastOverflowBlock = new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber);
                if (!overflowBlock.isFull()) {
                    overflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber));
                    neededSpaces -= (overflowHeap.getBlockingFactor() - overflowBlock.getValidBlockCount());
                }
                overflowBlockNumber = overflowBlock.getNextOverflowBlock();
            }

            if (neededSpaces > 0) {
                int requiredOverflowBlocks = minimalRequiredOverflowBlockNumber(neededSpaces, false);
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

                currentOverflowBlockIndex++;
            }
        }

        mainBucketsHeap.writeBlock(bucketNumber, bucket);
        if (lastOverflowBlock != null && !overflowBlocks.contains(lastOverflowBlock)) {
            overflowHeap.writeBlock(lastOverflowBlock.number, lastOverflowBlock.block);
        }
        for (OverflowBlockAndNumber overflowBlockAndNumber : overflowBlocks) {
            overflowHeap.writeBlock(overflowBlockAndNumber.number, overflowBlockAndNumber.block);
        }
    }

    private void appendNewOverflowBlocks(int howManyBlocksToAdd, Bucket<T> bucket,
            List<OverflowBlockAndNumber> overflowBlocks, OverflowBlockAndNumber lastOverflowBlock) {
        int numberOfBlocksAtTheEnd = 0;
        int indexOfNewBlock = overflowHeap.getNumberForNewBlock();
        for (int i = 0; i < howManyBlocksToAdd; i++) {
            OverflowBlock<T> newOverflowBlock = new OverflowBlock<>(overflowHeap.getBlockingFactor(),
                    overflowHeap.getBlockSize(), recordClass);
            int overflowBlockNumber = overflowHeap.getEmptyBlock();
            if (overflowBlockNumber == -1) {
                overflowBlockNumber = indexOfNewBlock + numberOfBlocksAtTheEnd;
                numberOfBlocksAtTheEnd++;
            } 

            if (i == 0) {
                if (lastOverflowBlock == null) {
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
        this.recordClass = recordClass;
        this.mainBucketsHeap = new Heap<>(mainBucketsPath, mainBucketsBlockSize, recordClass, true, 16);
        this.overflowHeap = new Heap<>(overflowBlocksPath, overflowBlockSize, recordClass, false, 8);
        this.mainBucketsBlockSize = mainBucketsHeap.getBlockSize();
        this.overflowBlockSize = overflowHeap.getBlockSize();
    }

    public BucketHeap(String mainBucketsPath, String mainMetadataPath,
            String overflowBlocksPath, String overflowMetadataPath, Class<T> recordClass) {
        this.recordClass = recordClass;
        this.mainBucketsHeap = new Heap<>(mainBucketsPath, mainMetadataPath, recordClass, true, 16);
        this.overflowHeap = new Heap<>(overflowBlocksPath, overflowMetadataPath, recordClass, false, 8);
        this.mainBucketsBlockSize = mainBucketsHeap.getBlockSize();
        this.overflowBlockSize = overflowHeap.getBlockSize();
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

    @SuppressWarnings("unchecked")
    public boolean delete(int bucketNumber, T partialRecord) {
        if (!mainBucketsHeap.checkIfBlockExists(bucketNumber)) {
            return false;
        }

        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        List<OverflowBlockAndNumber> overflowBlocks = new ArrayList<>();
        boolean deletionOccurred = false;

        if (bucket.delete(partialRecord)) {
            deletionOccurred = true;
        } else {
            int overflowBlockNumber = bucket.getFirstOverflowBlock();
            OverflowBlock<T> previousOverflowBlock = null;
            int previousOverflowBlockNumber = -1;

            while (overflowBlockNumber != -1) {
                OverflowBlock<T> overflowBlock = overflowHeap.readBlock(overflowBlockNumber, OverflowBlock.class);

                if (overflowBlock.delete(partialRecord)) {
                    deletionOccurred = true;
                    bucket.decrementTotalElementCountBy(1);
                    overflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber));

                    if (overflowBlock.isEmpty()) {
                        int nextBlockNum = overflowBlock.getNextOverflowBlock();
                        if (previousOverflowBlock == null) {
                            bucket.setFirstOverflowBlock(nextBlockNum);
                        } else {
                            previousOverflowBlock.setNextOverflowBlock(nextBlockNum);
                            overflowHeap.writeBlock(previousOverflowBlockNumber, previousOverflowBlock);
                        }
                    }
                    break;
                }

                overflowBlocks.add(new OverflowBlockAndNumber(overflowBlock, overflowBlockNumber));
                previousOverflowBlock = overflowBlock;
                previousOverflowBlockNumber = overflowBlockNumber;
                overflowBlockNumber = overflowBlock.getNextOverflowBlock();
            }
        }

        if (deletionOccurred) {
            int minRequiredOverflowBlocks = minimalRequiredOverflowBlockNumber(bucket.getTotalRecordCount(), true);
            boolean needsShuffle = minRequiredOverflowBlocks < bucket.getTotalOverflowBlockCount();

            if (needsShuffle) {
                shuffle(bucket, minRequiredOverflowBlocks, overflowBlocks);
            } else if (!overflowBlocks.isEmpty()) {
                OverflowBlockAndNumber deletedFrom = overflowBlocks.getLast();

                if (deletedFrom.block.isEmpty()) {
                    deletedFrom.block.setNextOverflowBlock(-1);
                    bucket.decrementOverflowBlockCountBy(1);
                    overflowHeap.manageEmptyBlock(deletedFrom.number);
                    overflowHeap.truncateAtTheEndIfPossible();
                } else {
                    overflowHeap.writeBlock(deletedFrom.number, deletedFrom.block);
                }
            }

            mainBucketsHeap.writeBlock(bucketNumber, bucket);
        }

        return deletionOccurred;
    }

    private void shuffle(Bucket<T> bucket, int minRequiredOverflowBlocks, List<OverflowBlockAndNumber> overflowBlocks) {
        List<T> allRecords = new ArrayList<T>();
        collectAllRecords(bucket, allRecords, overflowBlocks);
        List<OverflowBlockAndNumber> freedOverflowBlocks = insertCompactly(bucket, allRecords, overflowBlocks);
        if (bucket.getTotalOverflowBlockCount() > 0 && !overflowBlocks.isEmpty()) {
            bucket.setFirstOverflowBlock(overflowBlocks.get(0).number);
        }
        clearOverflowChain(freedOverflowBlocks);
        overflowHeap.truncateAtTheEndIfPossible();
        if (bucket.getTotalRecordCount() != allRecords.size() ||
                bucket.getTotalOverflowBlockCount() != minRequiredOverflowBlocks) {
            throw new Error("Shuffling went wrong");
        }
    }

    private int minimalRequiredOverflowBlockNumber(int totalElements, boolean includingBucket) {
        if (includingBucket) {
            int overflowRecords = Math.max(0, totalElements - mainBucketsHeap.getBlockingFactor());
            return (int) Math.ceil((double) overflowRecords / overflowHeap.getBlockingFactor());
        } else {
            return (int) Math.ceil((double) totalElements / overflowHeap.getBlockingFactor());
        }
    }

    public List<OverflowBlockAndNumber> insertCompactly(
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

            if (currentRecordIndex < allRecords.size() && (currentOverflowBlockIndex + 1) < overflowBlocks.size()) {
                overflowBlock.setNextOverflowBlock(overflowBlocks.get(currentOverflowBlockIndex + 1).number);
            } else {
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
            bucket.decrementOverflowBlockCountBy(1);
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
            bucket.decrementOverflowBlockCountBy(1);
            overflowBlockNumber = overflowBlock.getNextOverflowBlock();
        }

        if (bucket.getTotalRecordCount() != 0
                || bucket.getTotalOverflowBlockCount() != 0) {
            throw new Error("An error occurred during collecting all records in the chain");
        }

        bucket.setFirstOverflowBlock(-1);
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
