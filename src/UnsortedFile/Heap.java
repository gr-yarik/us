package UnsortedFile;

import java.io.IOException;

import LinearHashing.Bucket;

public class Heap<T extends StorableRecord> {
    
    private int blockSize;     
    private int recordSize;
    private int blockingFactor;
    private Class<T> recordClass;
    private BinaryFile binaryFile;
    private BlockManager blockManager;
    private boolean sequentialMode;
    
    public Heap(String pathToFile, int blockSize, Class<T> recordClass, boolean sequentialMode, int reservedBytes) {
        this.recordClass = recordClass;
        this.sequentialMode = sequentialMode;

        java.io.File heapFile = new java.io.File(pathToFile);
        
        if (heapFile.exists()) {
            String metadataPath = pathToFile + ".meta";
            try {
                this.blockManager = new BlockManager(metadataPath);
                this.blockSize = blockManager.getBlockSize();
            } catch (IOException e) {
                throw new RuntimeException("BlockManager must be instantiated: " + metadataPath, e);
            }
            
            try {
                T templateRecord = recordClass.getDeclaredConstructor().newInstance();
                this.recordSize = templateRecord.sizeInBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
            }
            
            this.blockingFactor = (int) Math.floor((double) (this.blockSize - reservedBytes) / this.recordSize);
            
            if (this.blockingFactor < 1) {
                throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
            }
            
            try {
                this.binaryFile = new BinaryFile(pathToFile);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open binary file: " + pathToFile, e);
            }
            
            if (sequentialMode) {
                this.blockManager = null;
            }
        } else {
            String metadataPath = pathToFile + ".meta";
            java.io.File metaFile = new java.io.File(metadataPath);
            if (metaFile.exists()) {
                metaFile.delete();
            }
            
            this.blockSize = blockSize;
            
            try {
                T templateRecord = recordClass.getDeclaredConstructor().newInstance();
                this.recordSize = templateRecord.sizeInBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
            }
          
            this.blockingFactor = (int) Math.floor((double) (this.blockSize - reservedBytes) / this.recordSize);
            
            if (this.blockingFactor < 1) {
                throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
            }

            try {
                this.binaryFile = new BinaryFile(pathToFile);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open binary file: " + pathToFile, e);
            }
            
            try {
                this.blockManager = new BlockManager(metadataPath, blockSize);
            } catch (IOException e) {
                throw new RuntimeException("Cannot initialize block manager: " + metadataPath, e);
            }
        }
    }

    public int insert(T instance) {
        if (sequentialMode) {
            throw new UnsupportedOperationException("Cannot use insert() in sequential mode.");
        }
        
        try {
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
                long fileSize = binaryFile.getSize();
                blockNumber = (int) (fileSize / blockSize);
                block = new Block<>(blockingFactor, blockSize, recordClass);
            }
            
            boolean added = block.addRecord(instance);

            if (!added) {
                throw new RuntimeException("Could not insert a record. This should not happen.");
            }
            
            writeBlock(blockNumber, block);
            
            blockManager.updateAfterInsert(blockNumber, block.getValidBlockCount(), blockingFactor, blockSize);
            
            return blockNumber;
        } catch (IOException e) {
            throw new RuntimeException("Error inserting record to file", e);
        }
    }

    public T get(int blockNumber, T partialRecord) {
        try {
            Block<T> block = readBlock(blockNumber);
            if (block == null) {
                return null;
            }
            
            int recordIndex = block.findRecordIndex(partialRecord);
            if (recordIndex == -1) {
                return null;
            }
            
            return block.getRecord(recordIndex);
        } catch (IOException e) {
            throw new RuntimeException("Error reading record from file", e);
        }
    }

    public boolean delete(int blockNumber, T partialRecord) {
        try {
            Block<T> block = readBlock(blockNumber);
        
            int recordIndex = block.findRecordIndex(partialRecord);
            if (recordIndex == -1) {
                return false;
            }
            
            boolean deleted = block.delete(partialRecord);
            
            if (deleted) {
                int newValidCount = block.getValidBlockCount();
                
                if (!sequentialMode) {
                    blockManager.updateAfterDelete(blockNumber, newValidCount, blockingFactor, blockSize);
                }
                
                if (block.isEmpty() && !sequentialMode) {
                    truncateAtTheEndIfPossible();
                } else {
                    writeBlock(blockNumber, block);
                }
                
                return true;
            }
            
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Error deleting record from file", e);
        }
    }
    
    private int countConsecutiveEmptyBlocksAtEnd() throws IOException {
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
    
    private void truncateAtTheEndIfPossible() throws IOException {
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
    
    public Block<T> readBlock(int blockNumber) throws IOException {
        if(sequentialMode) {
            return readBlock(blockNumber, Bucket.class);
        } else {
            return readBlock(blockNumber, Block.class);
        }
    }

    public boolean checkIfBlockExists(int blockNumber) throws IOException {
        long position = (long) blockNumber * blockSize;
        return position < binaryFile.getSize();
    }
    
    public <B extends Block<T>> B readBlock(int blockNumber, Class<B> blockClass) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        if (position >= binaryFile.getSize()) {
            throw new RuntimeException("Passed blockNumber does not exist in the file");
        }
        
        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);
        
        try {
            B block = blockClass.getDeclaredConstructor(int.class, int.class, Class.class)
                    .newInstance(blockingFactor, blockSize, recordClass);
            block.FromByteArray(blockData, recordClass);
            return block;
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate block class: " + blockClass.getName(), e);
        }
    }
    
    public void writeBlock(int blockNumber, Block<T> block) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        binaryFile.seek(position);
        byte[] blockData = block.ToByteArray();
        binaryFile.write(blockData);
    }
    
    public void extendToBlockCount(int blockCount) throws IOException {
        if (!sequentialMode) {
            throw new UnsupportedOperationException("Cannot be used in classic heap");
        }
        
        int currentBlocks = getTotalBlocks();
        if (blockCount > currentBlocks) {
            for (int i = currentBlocks; i < blockCount; i++) {
                Block<T> emptyBlock = new Bucket<>(blockingFactor, blockSize, recordClass);
                writeBlock(i, emptyBlock);
            }
        }
    }
    
    public int getTotalBlocks() throws IOException {
        long fileSize = binaryFile.getSize();
        return (int) (fileSize / blockSize);
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public int getBlockingFactor() {
        return blockingFactor;
    }
    
    public void close() throws IOException {
        if (blockManager != null) {
            blockManager.close();
        }
        if (binaryFile != null) {
            binaryFile.close();
        }
    }
    
}
