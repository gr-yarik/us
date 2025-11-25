package UnsortedFile;

import java.io.IOException;

public class Heap<T extends StorableRecord> {
    
    // All records are stored in individual blocks on disk, not in memory
    private String pathToFile;
    private int blockSize;     
    private int recordSize;     // The size of one specific record
    private int blockingFactor; // How many records fit in one block
    private Class<T> recordClass;
    private BinaryFile binaryFile;
    private BlockManager blockManager;

      // Constructor
      public Heap(String pathToFile, int blockSize, Class<T> recordClass) {
        this.pathToFile = pathToFile;
        this.blockSize = blockSize; 
        this.recordClass = recordClass;
        
        // Get record size from the template class
        try {
            T templateRecord = recordClass.getDeclaredConstructor().newInstance();
            this.recordSize = templateRecord.sizeInBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
        }
        
        this.blockingFactor = (int) Math.floor((double) (this.blockSize - 4) / this.recordSize); // -4 for validBlockCount
        
        // Validation (Optional but recommended)
        if (this.blockingFactor < 1) {
            throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
        }
        
        // Initialize BinaryFile
        try {
            this.binaryFile = new BinaryFile(pathToFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open binary file: " + pathToFile, e);
        }
        
        // Initialize BlockManager (metadata file path: original file path + ".meta")
        try {
            String metadataPath = pathToFile + ".meta";
            
            // Create metadata file with blockSize if it doesn't exist
            java.util.List<Integer> emptyBlocks = new java.util.ArrayList<>();
            java.util.List<Integer> partiallyEmptyBlocks = new java.util.ArrayList<>();
            
            java.io.File metaFile = new java.io.File(metadataPath);
            if (!metaFile.exists() || metaFile.length() == 0) {
                // Create new metadata file with blockSize
                BlockManager.saveToFile(metadataPath, blockSize, emptyBlocks, partiallyEmptyBlocks);
            }
            
            // Initialize BlockManager with empty lists (metadata file already created)
            this.blockManager = new BlockManager(metadataPath, emptyBlocks, partiallyEmptyBlocks);
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize block manager: " + pathToFile + ".meta", e);
        }
    }
    
    /**
     * Constructor that loads Heap from existing metadata file
     * Reads blockSize and block lists from metadata file once during startup
     * @param pathToFile path to the heap binary file
     * @param metadataFilePath path to the metadata file
     * @param recordClass the class of records to be stored
     */
    public Heap(String pathToFile, String metadataFilePath, Class<T> recordClass) {
        this.pathToFile = pathToFile;
        this.recordClass = recordClass;
        
        // Load metadata from file once during startup
        try {
            BlockManager.MetadataResult metadata = BlockManager.loadFromFile(metadataFilePath);
            if (metadata == null) {
                throw new RuntimeException("Metadata file is empty or doesn't exist: " + metadataFilePath);
            }
            
            // Extract blockSize from metadata
            this.blockSize = metadata.blockSize;
            
            // Get record size from the template class
            try {
                T templateRecord = recordClass.getDeclaredConstructor().newInstance();
                this.recordSize = templateRecord.sizeInBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
            }
            
            this.blockingFactor = (int) Math.floor((double) (this.blockSize - 4) / this.recordSize); // -4 for validBlockCount
            
            // Validation
            if (this.blockingFactor < 1) {
                throw new IllegalArgumentException("Record size (" + recordSize + ") is larger than Block size (" + blockSize + ")");
            }
            
            // Initialize BinaryFile
            this.binaryFile = new BinaryFile(pathToFile);
            
            // Initialize BlockManager with data loaded from metadata file
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
            
            // Priority 1: Try to get a partially empty block
            blockNumber = blockManager.getNextPartiallyEmptyBlock();
            if (blockNumber != -1) {
                block = readBlock(blockNumber);
                if (block != null && !block.isFull()) {
                    // Use this partially empty block
                } else {
                    // Block was marked as partially empty but is actually full or doesn't exist
                    blockNumber = -1;
                    block = null;
                }
            }
            
            // Priority 2: Try to get an empty block (in middle of file)
            if (blockNumber == -1) {
                blockNumber = blockManager.getNextEmptyBlock();
                if (blockNumber != -1) {
                    block = readBlock(blockNumber);
                    if (block != null && block.isEmpty()) {
                        // Use this empty block
                    } else {
                        // Block was marked as empty but isn't
                        blockNumber = -1;
                        block = null;
                    }
                }
            }
            
            // Priority 3: Create new block at end of file
            if (blockNumber == -1) {
                long fileSize = binaryFile.getSize();
                blockNumber = (int) (fileSize / blockSize);
                block = new Block<>(blockingFactor, blockSize, recordClass);
            }
            
            // Add record to block
            boolean added = block.addRecord(instance);
            if (!added) {
                throw new RuntimeException("Block is full, cannot insert record");
            }
            
            // Write the updated block back to file
            writeBlock(blockNumber, block);
            
            // Update BlockManager with new block state (saves to disk immediately)
            blockManager.updateAfterInsert(blockNumber, block.getValidBlockCount(), blockingFactor, blockSize);
            
            // Return the block number where the record is stored
            return blockNumber;
            
        } catch (IOException e) {
            throw new RuntimeException("Error inserting record to file", e);
        }
    }

    public T get(int blockNumber, T partialRecord) {
        try {
            // Read the block from file
            Block<T> block = readBlock(blockNumber);
            if (block == null) {
                return null;
            }
            
            // Find the record in the block using equals
            int recordIndex = block.findRecordIndex(partialRecord);
            if (recordIndex == -1) {
                return null; // Record not found in block
            }
            
            // Get the record at the found index
            T record = block.getRecord(recordIndex);
            return record;
        } catch (IOException e) {
            throw new RuntimeException("Error reading record from file", e);
        }
    }

    public boolean delete(int blockNumber, T partialRecord) {
        try {
            // Read the block from file (1 file access)
            Block<T> block = readBlock(blockNumber);
            if (block == null) {
                return false;
            }
            
            // Find the record in the block using equals
            int recordIndex = block.findRecordIndex(partialRecord);
            if (recordIndex == -1) {
                return false; // Record not found in block
            }
            
            // Delete the record from the block (shifts left and decreases count)
            boolean deleted = block.delete(partialRecord);
            
            if (deleted) {
                int newValidCount = block.getValidBlockCount();
                
                // Update BlockManager with new block state (saves to disk immediately)
                blockManager.updateAfterDelete(blockNumber, newValidCount, blockingFactor, blockSize);
                
                // If block is now empty, check if we can truncate instead of writing
                if (block.isEmpty()) {
                    // Try to truncate at the end if possible - returns true if truncation happened
                    if (truncateAtTheEndIfPossible(blockNumber)) {
                        // Truncation happened - only 1 file access total: read + truncate (no write needed)
                    } else {
                        // Block is empty but not at end, write it back
                        writeBlock(blockNumber, block);
                        // 2 file accesses: read + write
                    }
                } else {
                    // Block is not empty, write it back
                    writeBlock(blockNumber, block);
                    // 2 file accesses: read + write
                }
                
                return true;
            }
            
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Error deleting record from file", e);
        }
    }
    
    /**
     * Counts consecutive empty blocks at the end of the file
     * Uses BlockManager to efficiently find empty blocks without reading from disk
     * @return number of consecutive empty blocks at the end, or 0 if none
     */
    private int countConsecutiveEmptyBlocksAtEnd() throws IOException {
        long fileSize = binaryFile.getSize();
        int totalBlocks = (int) (fileSize / blockSize);
        
        if (totalBlocks == 0) {
            return 0; // File is empty, no blocks to count
        }
        
        // Count consecutive empty blocks from the end
        int consecutiveEmptyBlocks = 0;
        for (int i = totalBlocks - 1; i >= 0; i--) {
            if (blockManager.isEmptyBlock(i)) {
                consecutiveEmptyBlocks++;
            } else {
                // Found a non-empty block, stop counting
                break;
            }
        }
        
        return consecutiveEmptyBlocks;
    }
    
    /**
     * Attempts to truncate the file at the end if the given block is at the end and empty
     * Checks if truncation is possible and performs it in one operation
     * @param blockNumber the block number that became empty
     * @return true if truncation was performed, false otherwise
     */
    private boolean truncateAtTheEndIfPossible(int blockNumber) throws IOException {
        long fileSize = binaryFile.getSize();
        int totalBlocks = (int) (fileSize / blockSize);
        
        if (totalBlocks == 0) {
            // File is already empty, nothing to truncate
            return false;
        }
        
        // Check if this block is at the end
        if (blockNumber < totalBlocks - 1) {
            return false; // Block is not at the end, cannot truncate
        }
        
        // Use shared logic to count consecutive empty blocks
        int consecutiveEmptyBlocks = countConsecutiveEmptyBlocksAtEnd();
        
        if (consecutiveEmptyBlocks == 0) {
            return false; // No empty blocks to truncate
        }
        
        // Perform truncation
        // If all blocks are empty, truncate to 0
        if (consecutiveEmptyBlocks == totalBlocks) {
            binaryFile.truncate(0);
            // Clear all blocks from BlockManager (more efficient than removing one by one)
            blockManager.clearAllBlocks(blockSize);
            return true;
        }
        
        // Truncate file by removing consecutive empty blocks at the end
        int newTotalBlocks = totalBlocks - consecutiveEmptyBlocks;
        long newFileSize = (long) newTotalBlocks * blockSize;
        
        binaryFile.truncate(newFileSize);
        
        // Remove truncated blocks from BlockManager (saves to disk immediately)
        for (int i = newTotalBlocks; i < totalBlocks; i++) {
            blockManager.removeBlock(i, blockSize);
        }
        
        return true;
    }
    
    /**
     * Reads a block from the file
     * @param blockNumber the block number (0-based)
     * @return the Block object, or null if block doesn't exist
     */
    private Block<T> readBlock(int blockNumber) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        // Check if block exists
        if (position >= binaryFile.getSize()) {
            return null;
        }
        
        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);
        
        Block<T> block = new Block<>(blockingFactor, blockSize, recordClass);
        block.FromByteArray(blockData, recordClass);
        
        return block;
    }
    
    /**
     * Writes a block to the file
     * @param blockNumber the block number (0-based)
     * @param block the Block object to write
     */
    private void writeBlock(int blockNumber, Block<T> block) throws IOException {
        long position = (long) blockNumber * blockSize;
        binaryFile.seek(position);
        
        byte[] blockData = block.ToByteArray();
        binaryFile.write(blockData);
    }
    
    
    /**
     * Closes the binary file and block manager
     */
    public void close() throws IOException {
        if (blockManager != null) {
            blockManager.close();
        }
        if (binaryFile != null) {
            binaryFile.close();
        }
    }
    
}
