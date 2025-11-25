package UnsortedFile;

import java.io.IOException;

public class Heap<T extends StorableRecord> {
    
    private int blockSize;     
    private int recordSize;
    private int blockingFactor;
    private Class<T> recordClass;
    private BinaryFile binaryFile;
    private BlockManager blockManager;

      public Heap(String pathToFile, int blockSize, Class<T> recordClass) {
        this.blockSize = blockSize; 
        this.recordClass = recordClass;
        
        try {
            T templateRecord = recordClass.getDeclaredConstructor().newInstance();
            this.recordSize = templateRecord.sizeInBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
        }
        
        this.blockingFactor = (int) Math.floor((double) (this.blockSize - 4) / this.recordSize);
        
        if (this.blockingFactor < 1) {
            throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
        }
        
        try {
            this.binaryFile = new BinaryFile(pathToFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open binary file: " + pathToFile, e);
        }
        
        try {
            String metadataPath = pathToFile + ".meta";
            
            java.util.List<Integer> emptyBlocks = new java.util.ArrayList<>();
            java.util.List<Integer> partiallyEmptyBlocks = new java.util.ArrayList<>();
            
            java.io.File metaFile = new java.io.File(metadataPath);
            if (!metaFile.exists() || metaFile.length() == 0) {
                BlockManager.saveToFile(metadataPath, blockSize, emptyBlocks, partiallyEmptyBlocks);
            }
            
            this.blockManager = new BlockManager(metadataPath, emptyBlocks, partiallyEmptyBlocks);
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize block manager: " + pathToFile + ".meta", e);
        }
    }
    
    public Heap(String pathToFile, String metadataFilePath, Class<T> recordClass) {
        this.recordClass = recordClass;
        
        try {
            BlockManager.MetadataResult metadata = BlockManager.loadFromFile(metadataFilePath);
            if (metadata == null) {
                throw new RuntimeException("Metadata file is empty or doesn't exist: " + metadataFilePath);
            }
            
            this.blockSize = metadata.blockSize;
            
            try {
                T templateRecord = recordClass.getDeclaredConstructor().newInstance();
                this.recordSize = templateRecord.sizeInBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
            }
            
            this.blockingFactor = (int) Math.floor((double) (this.blockSize - 4) / this.recordSize);
            
            if (this.blockingFactor < 1) {
                throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
            }
            
            this.binaryFile = new BinaryFile(pathToFile);
            
            this.blockManager = new BlockManager(metadataFilePath, metadata.emptyBlocks, metadata.partiallyEmptyBlocks);
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot load heap from metadata file: " + metadataFilePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing heap from metadata", e);
        }
    }


    public int insert(T instance) {
        try {
            int blockNumber = -1;
            Block<T> block = null;
            
            blockNumber = blockManager.getNextPartiallyEmptyBlock();
            if (blockNumber != -1) {
                block = readBlock(blockNumber);
                if (block == null || block.isFull()) {
                    blockNumber = -1;
                    block = null;
                }
            }
            
            if (blockNumber == -1) {
                blockNumber = blockManager.getNextEmptyBlock();
                if (blockNumber != -1) {
                    block = readBlock(blockNumber);
                    if (block == null || !block.isEmpty()) {
                        blockNumber = -1;
                        block = null;
                    }
                }
            }
            
            if (blockNumber == -1) {
                long fileSize = binaryFile.getSize();
                blockNumber = (int) (fileSize / blockSize);
                block = new Block<>(blockingFactor, blockSize, recordClass);
            }
            
            boolean added = block.addRecord(instance);
            if (!added) {
                throw new RuntimeException("Block is full, cannot insert record");
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
            
            T record = block.getRecord(recordIndex);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Error reading record from file", e);
        }
    }

    public boolean delete(int blockNumber, T partialRecord) {
        try {
            Block<T> block = readBlock(blockNumber);
            if (block == null) {
                return false;
            }
            
            int recordIndex = block.findRecordIndex(partialRecord);
            if (recordIndex == -1) {
                return false;
            }
            
            boolean deleted = block.delete(partialRecord);
            
            if (deleted) {
                int newValidCount = block.getValidBlockCount();
                
                blockManager.updateAfterDelete(blockNumber, newValidCount, blockingFactor, blockSize);
                
                if (block.isEmpty()) {
                    if (truncateAtTheEndIfPossible(blockNumber)) {
                    } else {
                        writeBlock(blockNumber, block);
                    }
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
    
    private boolean truncateAtTheEndIfPossible(int blockNumber) throws IOException {
        long fileSize = binaryFile.getSize();
        int totalBlocks = (int) (fileSize / blockSize);
        
        if (totalBlocks == 0) {
            return false;
        }
        
        if (blockNumber < totalBlocks - 1) {
            return false;
        }
        
        int consecutiveEmptyBlocks = countConsecutiveEmptyBlocksAtEnd();
        
        if (consecutiveEmptyBlocks == 0) {
            return false;
        }
        
        if (consecutiveEmptyBlocks == totalBlocks) {
            binaryFile.truncate(0);
            blockManager.clearAllBlocks(blockSize);
            return true;
        }
        
        int newTotalBlocks = totalBlocks - consecutiveEmptyBlocks;
        long newFileSize = (long) newTotalBlocks * blockSize;
        
        binaryFile.truncate(newFileSize);
        
        for (int i = newTotalBlocks; i < totalBlocks; i++) {
            blockManager.removeBlock(i, blockSize);
        }
        
        return true;
    }
    
    private Block<T> readBlock(int blockNumber) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        if (position >= binaryFile.getSize()) {
            return null;
        }
        
        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);
        
        Block<T> block = new Block<>(blockingFactor, blockSize, recordClass);
        block.FromByteArray(blockData, recordClass);
        
        return block;
    }
    
    private void writeBlock(int blockNumber, Block<T> block) throws IOException {
        long position = (long) blockNumber * blockSize;
        binaryFile.seek(position);
        
        byte[] blockData = block.ToByteArray();
        binaryFile.write(blockData);
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
