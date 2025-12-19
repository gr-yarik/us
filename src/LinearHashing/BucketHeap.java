package LinearHashing;

import java.io.IOException;
import java.util.*;

import UnsortedFile.Block;
import UnsortedFile.Heap;
import UnsortedFile.StorableRecord;

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

    public boolean insertIntoBucket(int bucketNumber, T record) throws IOException {
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
                bucket.incrementTotalElementCountBy(1);
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
            bucket.setOverflowBlockCount(1);
            bucket.incrementTotalElementCountBy(1);
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
                    bucket.incrementTotalElementCountBy(1);

                    mainBucketsHeap.writeBlock(bucketNumber, bucket);
                    return true;
                }
                if (current.getNextOverflowBlock() == -1) {
                    if (blocksTraversed != bucket.getTotalOverflowBlockCount()) {
                        throw new IOException("Traversed and expected number of overflow buckets does not match.");
                    }

                    OverflowBlock<T> newOverflow = createEmptyOverflowBlock();
                    newOverflow.addRecord(record);
                    int newOverflowBlockNum = allocateOverflowBlock(newOverflow);
                    current.setNextOverflowBlock(newOverflowBlockNum);
                    overflowHeap.writeBlock(currentBlockNum, current);

                    bucket.setOverflowBlockCount(bucket.getTotalOverflowBlockCount() + 1);
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

        Boolean[] deletionResult = { Boolean.FALSE, Boolean.FALSE };

        deletionResult[0] = mainBucketsHeap.delete(bucketNumber, partialRecord,

                // record was found in mainHeap and deleted
                (Block<T> block) -> {
                    Bucket<T> bucket = (Bucket<T>) block;
                    bucket.decrementTotalElementCountBy(1);

                    int minimalRequiredOverflowBlockNumber = minimalRequiredOverflowBlockNumber(
                            bucket.getTotalRecordCount());

                    if (minimalRequiredOverflowBlockNumber < bucket.getTotalOverflowBlockCount()
                            && bucket.getFirstOverflowBlock() != -1) {
                        try {
                            List<T> emptyCollectedRecords = new ArrayList<T>();

                            List<Integer> overflowBlockNumbers = new ArrayList<>();
                            overflowBlockNumbers.add(bucket.getFirstOverflowBlock());

                            List<OverflowBlock<T>> emptyOverflowBlockInstances = new ArrayList<OverflowBlock<T>>();
                            shuffle(bucket, bucketNumber, minimalRequiredOverflowBlockNumber, emptyCollectedRecords,
                                    overflowBlockNumbers, emptyOverflowBlockInstances);                            
                        } catch (Exception e) {
                        }
                    }
                },

                // record was not found in mainHeap. Trying to delete in overflow
                (Block<T> block) -> {
                    Bucket<T> bucket = ((Bucket<T>) block);
                    try {
                        deletionResult[1] = deleteFromOverflowChain(bucket, bucketNumber, partialRecord);
                    } catch (IOException e) {
                        throw new RuntimeException("Error deleting from overflow chain", e);
                    }
                }

        );

        return deletionResult[0] || deletionResult[1];

    }

    // if (deletedFromOverflow) {
    // bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
    // bucket.decrementTotalElementCountByOne();
    // mainBucketsHeap.writeBlock(bucketNumber, bucket);
    // return true;
    // }

    private boolean deleteFromOverflowChain(Bucket<T> bucket, int bucketNumber, T partialRecord) throws IOException {
        // Holder for next block number (modified in lambda callbacks)
        int[] nextBlockNumberRef = { bucket.getFirstOverflowBlock() };

        OverflowBlock<T>[] previousOverflowBlockInstance = new OverflowBlock[1];
        int[] previousOverflowBlockNumber = new int[1];
        boolean[] deletionOccurred = new boolean[1];
        deletionOccurred[0] = false;

        List<T> collectedRecords = new ArrayList<>();
        List<Integer> overflowBlockNumbers = new ArrayList<>();
        List<OverflowBlock<T>> overflowBlockInstances = new ArrayList<>();

        Collections.addAll(collectedRecords, bucket.getAllValidRecords());

        // Traverse overflow chain until record is found or end is reached
        for (int blockNum = nextBlockNumberRef[0]; blockNum != -1; blockNum = nextBlockNumberRef[0]) {

            final int currentBlockNum = blockNum; // Final for use in lambda
            boolean deleted = overflowHeap.delete(currentBlockNum, partialRecord,

                    // record was found in the current overflow block and was deleted
                    (UnsortedFile.Block<T> block) -> {
                        try {
                            deletionOccurred[0] = true;
                            overflowBlockNumbers.add(currentBlockNum);
                            overflowBlockInstances.add((OverflowBlock<T>) block);
                            Collections.addAll(collectedRecords, block.getAllValidRecords());

                            bucket.decrementTotalElementCountBy(1);

                            if (block.getValidBlockCount() == 0) {
                                bucket.decrementOverflowBlockCountBy(1);

                                if (previousOverflowBlockInstance[0] == null) {
                                    // This was the first overflow block
                                    if (bucket.getFirstOverflowBlock() == -1) {
                                        bucket.setOverflowBlockCount(0);
                                    }
                                } else {
                                    // Unlink this empty block from the chain
                                    previousOverflowBlockInstance[0].setNextOverflowBlock(-1);
                                    // Write the previous block back to disk
                                    overflowHeap.writeBlock(previousOverflowBlockNumber[0],
                                            previousOverflowBlockInstance[0]);
                                }
                            }

                            int minimalRequiredOverflowBlockNumber = minimalRequiredOverflowBlockNumber(
                                    bucket.getTotalRecordCount());

                            // Check if striasenie (shuffle) can be done
                            if (minimalRequiredOverflowBlockNumber < bucket.getTotalOverflowBlockCount()
                                    && bucket.getFirstOverflowBlock() != -1) {
                                try {
                                    shuffle(bucket, bucketNumber, minimalRequiredOverflowBlockNumber, collectedRecords,
                                            overflowBlockNumbers, overflowBlockInstances);
                                    return;
                                } catch (Exception e) {
                                }
                            }

                            // Write bucket back to disk if shuffle didn't happen
                            mainBucketsHeap.writeBlock(bucketNumber, bucket);
                        } catch (IOException e) {
                            throw new RuntimeException("Error during overflow deletion", e);
                        }
                    },

                    // record was not found in the current overflow block and was not deleted
                    (Block<T> block) -> {
                        OverflowBlock<T> overflowBlockObj = (OverflowBlock<T>) block;

                        overflowBlockNumbers.add(currentBlockNum);
                        overflowBlockInstances.add(overflowBlockObj);
                        Collections.addAll(collectedRecords, overflowBlockObj.debugGetAllRecords());

                        previousOverflowBlockInstance[0] = overflowBlockObj;
                        previousOverflowBlockNumber[0] = currentBlockNum;
                        nextBlockNumberRef[0] = overflowBlockObj.getNextOverflowBlock();
                    });

            // If deletion occurred, break out of the loop
            if (deleted) {
                break;
            }
        }

        return deletionOccurred[0];
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
            Bucket<T> bucketInstance,
            int bucketNumber,
            int minimalRequiredOverflowBlockNumber,
            List<T> collectedRecords,
            List<Integer> overflowBlockNumbers,
            List<OverflowBlock<T>> overflowBlockInstances) throws IOException {

        // collectAllRecords(bucketInstance, allRecords);
        for (int i = overflowBlockInstances.getLast().getNextOverflowBlock(); i != -1;) {
            OverflowBlock<T> block = overflowHeap.readBlock(i, OverflowBlock.class);

            Collections.addAll(collectedRecords, block.debugGetAllRecords());
            overflowBlockNumbers.add(i);
            overflowBlockInstances.add(block);

            i = block.getNextOverflowBlock();
        }

        {
            int deletedElementsNumber = bucketInstance.deleteAllRecords();
            bucketInstance.decrementTotalElementCountBy(deletedElementsNumber);
        }
        // int bucketRecordCount = bucket.getValidBlockCount();
        // for (int i = bucketRecordCount - 1; i >= 0; i--) {
        // T record = bucket.getRecord(i);
        // if (record != null) {
        // bucket.delete(record);
        // bucket.decrementTotalElementCountByOne();
        // }
        // }

        for (OverflowBlock<T> overflowBlock : overflowBlockInstances) {
            int deletedElementsNumber = overflowBlock.deleteAllRecords();
            bucketInstance.decrementTotalElementCountBy(deletedElementsNumber);
            // overflowBlock.setNextOverflowBlock(-1);
        }

        if (bucketInstance.getTotalRecordCount() != 0) {
            throw new IllegalArgumentException("An error occured during shuffling");
        }

        // Reset bucket overflow chain info
        // bucketInstance.setFirstOverflowBlock(-1);
        bucketInstance.setOverflowBlockCount(0);

        // Reinsert records starting with the bucket, then overflow regions
        int recordIndex = 0;

        // Fill the main bucket first
        while (recordIndex < collectedRecords.size() && !bucketInstance.isFull()) {
            bucketInstance.addRecord(collectedRecords.get(recordIndex));
            bucketInstance.incrementTotalElementCountBy(1);
            recordIndex++;
        }
        // Write bucket after filling it
        // mainBucketsHeap.writeBlock(bucketNumber, bucket);

        // Fill overflow blocks - reuse existing ones only
        int overflowBlockIndex = 0;

        while (recordIndex < collectedRecords.size() && overflowBlockIndex < overflowBlockInstances.size()) {
            OverflowBlock<T> overflowBlock = overflowBlockInstances.get(overflowBlockIndex);
            int blockNum = overflowBlockNumbers.get(overflowBlockIndex);

            // // Link previous overflow block to this one
            // if (overflowBlockIndex == 0) {
                // This is the first overflow block
                //bucketInstance.setFirstOverflowBlock(blockNum);
                bucketInstance.incrementOverflowBlockCountBy(1);
            // } else {
            //     // Link previous block to this one
            //     int prevBlockNum = overflowBlockNumbers.get(overflowBlockIndex - 1);
            //     OverflowBlock<T> prevBlock = overflowBlockInstances.get(overflowBlockIndex - 1);
            //     prevBlock.setNextOverflowBlock(blockNum);
            //     // overflowHeap.writeBlock(prevBlockNum, prevBlock);
            //     bucketInstance.incrementOverflowBlockCountBy(1);
            // }

            // Fill the overflow block
            while (recordIndex < collectedRecords.size() && !overflowBlock.isFull()) {
                overflowBlock.addRecord(collectedRecords.get(recordIndex));
                bucketInstance.incrementTotalElementCountBy(1);
                recordIndex++;
            }

            // Set next overflow block to -1 for the last block
            if (recordIndex >= collectedRecords.size() || overflowBlockIndex == overflowBlockInstances.size() - 1) {
                overflowBlock.setNextOverflowBlock(-1);
            } else {
                // Link to next block
                int nextBlockNum = overflowBlockNumbers.get(overflowBlockIndex + 1);
                overflowBlock.setNextOverflowBlock(nextBlockNum);
            }

            // Write overflow block after filling it
            overflowHeap.writeBlock(blockNum, overflowBlock);
            overflowBlockIndex++;
        }
        // Update bucket overflow count
        bucketInstance.setOverflowBlockCount(overflowBlockIndex);

        if (bucketInstance.getTotalRecordCount() != collectedRecords.size()) {
            throw new IOException("Shuffling went wrong");
        }

        // Write bucket again with updated overflow chain info
        mainBucketsHeap.writeBlock(bucketNumber, bucketInstance);

        // // Clean up any extra overflow blocks that are no longer needed
        // while (overflowBlockIndex < overflowBlockNumbers.size()) {
        // int blockNum = overflowBlockNumbers.get(overflowBlockIndex);
        // OverflowBlock<T> overflowBlock = overflowBlocks.get(overflowBlockIndex);
        // // Clear the overflow block
        // int overflowRecordCount = overflowBlock.getValidBlockCount();
        // for (int i = overflowRecordCount - 1; i >= 0; i--) {
        // T record = overflowBlock.getRecord(i);
        // if (record != null) {
        // overflowBlock.delete(record);
        // }
        // }
        // overflowBlock.setNextOverflowBlock(-1);
        // overflowHeap.writeBlock(blockNum, overflowBlock);
        // overflowBlockIndex++;
        // }
    }

    private OverflowBlock<T> createEmptyOverflowBlock() {
        return new OverflowBlock<>(
                overflowHeap.getBlockingFactor(),
                overflowHeap.getBlockSize(),
                recordClass);
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

    public void collectAllRecords(int bucketNumber, List<T> records) throws IOException {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket != null) {
            collectAllRecords(bucket, records);
        }
    }

    public void collectAllRecords(Bucket<T> bucket, List<T> records) throws IOException {
        collectAllRecords(bucket, records, null);
    }

    public void collectAllRecords(Bucket<T> bucket, List<T> records,
            java.util.function.BiConsumer<Bucket<T>, List<OverflowBlock<T>>> cleanupCallback) throws IOException {
        List<OverflowBlock<T>> overflowBlocks = new ArrayList<>();

        for (int i = 0; i < bucket.getValidBlockCount(); i++) {
            T record = bucket.getRecord(i);

            records.add(record);

        }

        int firstOverflowBlock = bucket.getFirstOverflowBlock();
        if (firstOverflowBlock != -1) {
            OverflowBlock<T> current = overflowHeap.readBlock(firstOverflowBlock, OverflowBlock.class);
            while (true) {
                overflowBlocks.add(current);
                for (int i = 0; i < current.getValidBlockCount(); i++) {
                    T record = current.getRecord(i);
                    records.add(record);

                }
                if (current.getNextOverflowBlock() == -1) {
                    break;
                }
                current = overflowHeap.readBlock(current.getNextOverflowBlock(), OverflowBlock.class);
            }
        }

        if (cleanupCallback != null) {
            cleanupCallback.accept(bucket, overflowBlocks);
        }
    }

    public void clearOverflowChain(int bucketNumber) throws IOException {
        Bucket<T> bucket = (Bucket<T>) mainBucketsHeap.readBlock(bucketNumber);
        if (bucket != null) {
            bucket.setFirstOverflowBlock(-1);
            bucket.setOverflowBlockCount(0);
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
