package UnsortedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages empty and partially empty blocks in the heap file.
 * Maintains two lists: fully empty blocks and partially empty blocks.
 * Persists this metadata to a separate binary file.
 */
public class BlockManager {
    
    private List<Integer> emptyBlocks;      // Fully empty blocks (in middle of file)
    private List<Integer> partiallyEmptyBlocks; // Partially full blocks
    private String metadataFilePath;
    private BinaryFile metadataFile;
    
    /**
     * Creates a new BlockManager with provided lists
     * BlockManager does NOT read from file - Heap reads and passes the data
     * @param metadataFilePath path to the binary file storing block metadata
     * @param emptyBlocks list of empty block indices
     * @param partiallyEmptyBlocks list of partially empty block indices
     */
    public BlockManager(String metadataFilePath, List<Integer> emptyBlocks, List<Integer> partiallyEmptyBlocks) throws IOException {
        this.metadataFilePath = metadataFilePath;
        this.emptyBlocks = new ArrayList<>(emptyBlocks);
        this.partiallyEmptyBlocks = new ArrayList<>(partiallyEmptyBlocks);
        
        // Initialize metadata file
        this.metadataFile = new BinaryFile(metadataFilePath);
    }
    
    /**
     * Gets the next partially empty block index, or -1 if none available
     * Removes it from the list (will be re-added if still partially empty after insert)
     * Returns the minimum index (prefer earlier blocks)
     */
    public int getNextPartiallyEmptyBlock() {
        if (partiallyEmptyBlocks.isEmpty()) {
            return -1;
        }
        // Find and return the minimum index (more efficient than sorting entire list)
        int minIndex = Collections.min(partiallyEmptyBlocks);
        partiallyEmptyBlocks.remove(Integer.valueOf(minIndex));
        return minIndex;
    }
    
    /**
     * Gets the next empty block index, or -1 if none available
     * Removes it from the list (will be re-added if still empty after insert)
     * Returns the minimum index (prefer earlier blocks)
     */
    public int getNextEmptyBlock() {
        if (emptyBlocks.isEmpty()) {
            return -1;
        }
        // Find and return the minimum index (more efficient than sorting entire list)
        int minIndex = Collections.min(emptyBlocks);
        emptyBlocks.remove(Integer.valueOf(minIndex));
        return minIndex;
    }
    
    /**
     * Updates the block status after an insert operation
     * @param blockIndex the index of the block
     * @param validCount current number of valid records in the block
     * @param blockingFactor maximum number of records that fit in a block
     * @param blockSize the block size (needed for saving metadata)
     */
    public void updateAfterInsert(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        // Remove from both lists first
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        // Re-add based on new state
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
        // If full, don't add to any list
        
        saveToFile(blockSize);
    }
    
    /**
     * Updates the block status after a delete operation
     * @param blockIndex the index of the block
     * @param validCount current number of valid records in the block
     * @param blockingFactor maximum number of records that fit in a block
     * @param blockSize the block size (needed for saving metadata)
     */
    public void updateAfterDelete(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        // Remove from both lists first
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        // Re-add based on new state
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
        // If full, don't add to any list
        
        saveToFile(blockSize);
    }
    
    /**
     * Adds a new block index (when a new block is created at the end)
     * Initially it will be partially empty (has 1 record)
     * @param blockIndex the index of the new block
     * @param blockingFactor maximum number of records that fit in a block
     * @param blockSize the block size (needed for saving metadata)
     */
    public void addNewBlock(int blockIndex, int blockingFactor, int blockSize) {
        // New block starts with 1 record, so it's partially empty
        partiallyEmptyBlocks.add(blockIndex);
        saveToFile(blockSize);
    }
    
    /**
     * Removes a block index (when block is deleted/truncated from end)
     * @param blockIndex the index of the block to remove
     * @param blockSize the block size (needed for saving metadata)
     */
    public void removeBlock(int blockIndex, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        saveToFile(blockSize);
    }
    
    /**
     * Clears all blocks from both lists (for truncation when all blocks are empty)
     * More efficient than calling removeBlock() for each block
     * @param blockSize the block size (needed for saving metadata)
     */
    public void clearAllBlocks(int blockSize) {
        emptyBlocks.clear();
        partiallyEmptyBlocks.clear();
        saveToFile(blockSize);
    }
    
    /**
     * Gets the highest block index currently tracked
     * Used to determine the last block in the file
     */
    public int getHighestBlockIndex() {
        int max = -1;
        for (int idx : emptyBlocks) {
            if (idx > max) max = idx;
        }
        for (int idx : partiallyEmptyBlocks) {
            if (idx > max) max = idx;
        }
        return max;
    }
    
    /**
     * Checks if a block index is empty (for truncation purposes)
     */
    public boolean isEmptyBlock(int blockIndex) {
        return emptyBlocks.contains(blockIndex);
    }
    
    /**
     * Saves the block metadata to the binary file
     * Format: 
     * - 4 bytes: blockSize
     * - 4 bytes: count of empty blocks
     * - 4 bytes per empty block index
     * - 4 bytes: count of partially empty blocks
     * - 4 bytes per partially empty block index
     */
    public void saveToFile(int blockSize) {
        try {
            // Write to temporary buffer first
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            
            // Write blockSize first
            dos.writeInt(blockSize);
            
            // Write empty blocks
            dos.writeInt(emptyBlocks.size());
            for (int idx : emptyBlocks) {
                dos.writeInt(idx);
            }
            
            // Write partially empty blocks
            dos.writeInt(partiallyEmptyBlocks.size());
            for (int idx : partiallyEmptyBlocks) {
                dos.writeInt(idx);
            }
            
            byte[] data = baos.toByteArray();
            
            // Write to file and truncate
            metadataFile.seek(0);
            metadataFile.write(data);
            metadataFile.truncate(data.length);
            
        } catch (IOException e) {
            throw new RuntimeException("Error saving block metadata to file", e);
        }
    }
    
    /**
     * Static method to read metadata from file
     * @param metadataFilePath path to metadata file
     * @return MetadataResult containing blockSize and block lists, or null if file doesn't exist
     */
    public static MetadataResult loadFromFile(String metadataFilePath) throws IOException {
        BinaryFile metadataFile = new BinaryFile(metadataFilePath);
        
        if (metadataFile.getSize() == 0) {
            metadataFile.close();
            return null; // File doesn't exist or is empty
        }
        
        try {
            metadataFile.seek(0);
            java.io.DataInputStream dis = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(metadataFile.read((int)metadataFile.getSize())));
            
            // Read blockSize
            int blockSize = dis.readInt();
            
            // Read empty blocks
            int emptyCount = dis.readInt();
            List<Integer> emptyBlocks = new ArrayList<>();
            for (int i = 0; i < emptyCount; i++) {
                emptyBlocks.add(dis.readInt());
            }
            
            // Read partially empty blocks
            int partialCount = dis.readInt();
            List<Integer> partiallyEmptyBlocks = new ArrayList<>();
            for (int i = 0; i < partialCount; i++) {
                partiallyEmptyBlocks.add(dis.readInt());
            }
            
            return new MetadataResult(blockSize, emptyBlocks, partiallyEmptyBlocks);
        } finally {
            metadataFile.close();
        }
    }
    
    /**
     * Static method to write metadata to file (for initial creation)
     * @param metadataFilePath path to metadata file
     * @param blockSize the block size to store
     * @param emptyBlocks list of empty block indices
     * @param partiallyEmptyBlocks list of partially empty block indices
     */
    public static void saveToFile(String metadataFilePath, int blockSize, 
                                   List<Integer> emptyBlocks, List<Integer> partiallyEmptyBlocks) throws IOException {
        BinaryFile metadataFile = new BinaryFile(metadataFilePath);
        try {
            // Write to temporary buffer first
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            
            // Write blockSize first
            dos.writeInt(blockSize);
            
            // Write empty blocks
            dos.writeInt(emptyBlocks.size());
            for (int idx : emptyBlocks) {
                dos.writeInt(idx);
            }
            
            // Write partially empty blocks
            dos.writeInt(partiallyEmptyBlocks.size());
            for (int idx : partiallyEmptyBlocks) {
                dos.writeInt(idx);
            }
            
            byte[] data = baos.toByteArray();
            
            // Write to file and truncate
            metadataFile.seek(0);
            metadataFile.write(data);
            metadataFile.truncate(data.length);
        } finally {
            metadataFile.close();
        }
    }
    
    /**
     * Result class for loading metadata
     */
    public static class MetadataResult {
        public final int blockSize;
        public final List<Integer> emptyBlocks;
        public final List<Integer> partiallyEmptyBlocks;
        
        public MetadataResult(int blockSize, List<Integer> emptyBlocks, List<Integer> partiallyEmptyBlocks) {
            this.blockSize = blockSize;
            this.emptyBlocks = new ArrayList<>(emptyBlocks);
            this.partiallyEmptyBlocks = new ArrayList<>(partiallyEmptyBlocks);
        }
    }
    
    /**
     * Closes the metadata file
     * Note: Metadata is saved after each change, so no need to save here
     */
    public void close() throws IOException {
        if (metadataFile != null) {
            metadataFile.close();
        }
    }
}

