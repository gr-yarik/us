package UnsortedFile;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import LinearHashing.Bucket;
import LinearHashing.OverflowBlock;

public class Heap<T extends StorableRecord> {

    private int blockSize;
    private int recordSize;
    private int blockingFactor;
    private Class<T> recordClass;
    private BinaryFile binaryFile;
    private BlockManager blockManager;
    private boolean directBlockAddressingMode;

    public Heap(String pathToFile, int blockSize, Class<T> recordClass, boolean directBlockAddressingMode,
            int reservedBytes) {
        try {
            this.recordClass = recordClass;
            this.directBlockAddressingMode = directBlockAddressingMode;

            File heapFile = new File(pathToFile);

            if (heapFile.exists()) {
                String metadataPath = pathToFile + ".meta";
                this.blockManager = new BlockManager(metadataPath);
                this.blockSize = blockManager.getBlockSize();

                T templateRecord = recordClass.getDeclaredConstructor().newInstance();

                this.recordSize = templateRecord.sizeInBytes();

                this.blockingFactor = (int) Math.floor((double) (this.blockSize - reservedBytes) / this.recordSize);

                if (this.blockingFactor < 1) {
                    throw new Error(
                            "Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
                }

                this.binaryFile = new BinaryFile(pathToFile);

                if (directBlockAddressingMode) {
                    this.blockManager = null;
                }
            } else {
                String metadataPath = pathToFile + ".meta";
                File metaFile = new File(metadataPath);
                if (metaFile.exists()) {
                    metaFile.delete();
                }

                this.blockSize = blockSize;

                T templateRecord = recordClass.getDeclaredConstructor().newInstance();
                this.recordSize = templateRecord.sizeInBytes();

                this.blockingFactor = (int) Math.floor((double) (this.blockSize - reservedBytes) / this.recordSize);

                if (this.blockingFactor < 1) {
                    throw new Error();
                }

                this.binaryFile = new BinaryFile(pathToFile);

                this.blockManager = new BlockManager(metadataPath, blockSize);
            }
        } catch (Exception e) {
        }
        ;
    }

    public int insert(T instance) {
        if (directBlockAddressingMode) {
            throw new Error("Cannot use insert() in sequential mode.");
        }

        int blockNumber = -1;
        Block<T> block = null;

        blockNumber = blockManager.getNextPartiallyEmptyBlock();

        if (blockNumber != -1) {
            block = readBlock(blockNumber);
        }

        if (blockNumber == -1) {
            blockNumber = blockManager.getNextEmptyBlock();
            if (blockNumber != -1) {
                block = readBlock(blockNumber);
            }
        }

        if (blockNumber == -1) {
            blockNumber = getNumberForNewBlock();
            block = new Block<>(blockingFactor, blockSize, recordClass);
        }

        boolean added = block.addRecord(instance);

        if (!added) {
            throw new Error("Could not insert a record. This should not happen.");
        }

        writeBlock(blockNumber, block);

        blockManager.updateAfterInsert(blockNumber, block.getValidBlockCount(), blockingFactor, blockSize);

        return blockNumber;

    }

    public T get(int blockNumber, T partialRecord) {
        Block<T> block = readBlock(blockNumber);
        if (block == null) {
            return null;
        }
        return block.getRecord(partialRecord);
    }

    public <B extends Block<T>> boolean delete(int blockNumber, T partialRecord, Class<B> blockClass,
            Consumer<B> onSuccessDelete, Consumer<B> onUnsuccessDelete) {
        B block = readBlock(blockNumber, blockClass);
        boolean deleted = block.delete(partialRecord);

        if (deleted) {
            if (onSuccessDelete != null) {
                onSuccessDelete.accept(block);
            }
        
            int newValidCount = block.getValidBlockCount();
        
            if (!directBlockAddressingMode) {
                blockManager.updateAfterDelete(blockNumber, newValidCount, blockingFactor, blockSize);
            }
        
            writeBlock(blockNumber, block);
        
            if (block.isEmpty() && !directBlockAddressingMode) {
                truncateAtTheEndIfPossible();
            }
        
            return true;
        }

        if (onUnsuccessDelete != null) {
            onUnsuccessDelete.accept(block);
        }
        return false;
    }

    public int getTotalBlockCount() {
        return getNumberForNewBlock();
    }

    public int getNumberForNewBlock() {
        long fileSize = binaryFile.getSize();
        return (int) (fileSize / blockSize);
    }

    public int getEmptyBlock() {
        return blockManager.getNextEmptyBlock();
    }

    private int countConsecutiveEmptyBlocksAtEnd() {
        long fileSize = binaryFile.getSize();
        int totalBlocks = (int) (fileSize / blockSize);

        if (totalBlocks == 0) {
            return 0;
        }

        int consecutiveEmptyBlocks = 0;
        for (int i = totalBlocks - 1; i >= 0; i--) {
            if (blockManager.isEmptyBlock(i)) {
                consecutiveEmptyBlocks++;
            } else {
                break;
            }
        }

        return consecutiveEmptyBlocks;
    }

    public void manageEmptyBlock(int blockNumber) {
        blockManager.manageEmptyBlock(blockNumber);
    }

    public void truncateAtTheEndIfPossible() {
        long fileSize = binaryFile.getSize();
        int totalBlocks = (int) (fileSize / blockSize);

        int consecutiveEmptyBlocks = countConsecutiveEmptyBlocksAtEnd();

        if (consecutiveEmptyBlocks == 0) {
            return;
        }

        int newTotalBlocks = totalBlocks - consecutiveEmptyBlocks;
        long newFileSize = (long) newTotalBlocks * blockSize;

        binaryFile.truncate(newFileSize);

        for (int i = newTotalBlocks; i < totalBlocks; i++) {
            blockManager.removeBlock(i, blockSize);
        }
    }

    public Block<T> readBlock(int blockNumber) {
        if (directBlockAddressingMode) {
            return readBlock(blockNumber, Bucket.class);
        } else {
            return readBlock(blockNumber, Block.class);
        }
    }

    public boolean checkIfBlockExists(int blockNumber) {
        long position = (long) blockNumber * blockSize;
        return position < binaryFile.getSize();
    }

    public <B extends Block<T>> B readBlock(int blockNumber, Class<B> blockClass) {
        long position = (long) blockNumber * blockSize;

        if (position >= binaryFile.getSize()) {
            throw new Error("Passed blockNumber does not exist in the file");
        }

        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);

        try {
            B block = blockClass.getDeclaredConstructor(int.class, int.class, Class.class)
                    .newInstance(blockingFactor, blockSize, recordClass);
            block.FromByteArray(blockData, recordClass);
            return block;
        } catch (Exception e) {
            throw new Error("Cannot instantiate block class: " + blockClass.getName(), e);
        }
    }

    public void writeBlock(int blockNumber, Block<T> block) {
        if(blockNumber==22 && block instanceof OverflowBlock) {
            int i =0;
        }
        long position = (long) blockNumber * blockSize;
        binaryFile.seek(position);
        byte[] blockData = block.ToByteArray();
        binaryFile.write(blockData);
    }

    public void extendToBlockCount(int blockCount) {
        if (!directBlockAddressingMode) {
            throw new Error("Cannot be used in classic heap");
        }

        int currentBlocks = getTotalBlockCount();
        if (blockCount > currentBlocks) {
            for (int i = currentBlocks; i < blockCount; i++) {
                Block<T> emptyBlock = new Bucket<>(blockingFactor, blockSize, recordClass);
                writeBlock(i, emptyBlock);
            }
        }
    }

    public void truncateToBlockCount(int blockCount) {
        if (!directBlockAddressingMode) {
            throw new Error("Cannot be used in classic heap");
        }

        int currentBlocks = getTotalBlockCount();
        if (blockCount < currentBlocks) {
            long newFileSize = (long) blockCount * blockSize;
            binaryFile.truncate(newFileSize);
        }
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getBlockingFactor() {
        return blockingFactor;
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public void close() {
        if (blockManager != null) {
            blockManager.close();
        }
        if (binaryFile != null) {
            binaryFile.close();
        }
    }
}
