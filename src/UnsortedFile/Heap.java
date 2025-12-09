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

      public Heap(String pathToFile, int blockSize, Class<T> recordClass) {
        this(pathToFile, blockSize, recordClass, false, 4);
    }
    
    public Heap(String pathToFile, int blockSize, Class<T> recordClass, boolean sequentialMode) {
        this(pathToFile, blockSize, recordClass, sequentialMode, sequentialMode ? 12 : 4);
    }
    
    public Heap(String pathToFile, int blockSize, Class<T> recordClass, boolean sequentialMode, int reservedBytes) {
        this.blockSize = blockSize; 
        this.recordClass = recordClass;
        this.sequentialMode = sequentialMode;

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
        
        if (!sequentialMode) {
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
        } else {
            this.blockManager = null;
        }
    }
    
    public Heap(String pathToFile, String metadataFilePath, Class<T> recordClass) {
        this(pathToFile, metadataFilePath, recordClass, false, 4);
    }
    
    public Heap(String pathToFile, String metadataFilePath, Class<T> recordClass, boolean sequentialMode) {
        this(pathToFile, metadataFilePath, recordClass, sequentialMode, sequentialMode ? 12 : 4);
    }
    
    public Heap(String pathToFile, String metadataFilePath, Class<T> recordClass, boolean sequentialMode, int reservedBytes) {
        this.recordClass = recordClass;
        this.sequentialMode = sequentialMode;
        this.reservedBytes = reservedBytes;
        
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
            
            this.blockingFactor = (int) Math.floor((double) (this.blockSize - reservedBytes) / this.recordSize);
            
            if (this.blockingFactor < 1) {
                throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
            }
            
            this.binaryFile = new BinaryFile(pathToFile);
            
            if (!sequentialMode) {
                this.blockManager = new BlockManager(metadataFilePath, metadata.emptyBlocks, metadata.partiallyEmptyBlocks);
            } else {
                this.blockManager = null;
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot load heap from metadata file: " + metadataFilePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing heap from metadata", e);
        }
    }


    public int insert(T instance) {
        if (sequentialMode) {
            throw new UnsupportedOperationException("Cannot use insert() in sequential mode. Use insertIntoBlock() instead.");
        }
        
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
                
                if (!sequentialMode && blockManager != null) {
                    blockManager.updateAfterDelete(blockNumber, newValidCount, blockingFactor, blockSize);
                }
                
                if (block.isEmpty() && !sequentialMode && truncateAtTheEndIfPossible(blockNumber)) {
                    // Block truncated, no need to write
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
