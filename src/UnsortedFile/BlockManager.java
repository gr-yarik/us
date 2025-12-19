package UnsortedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockManager {
    
    private List<Integer> emptyBlocks;
    private List<Integer> partiallyEmptyBlocks;
    private BinaryFile metadataFile;
    private String metadataFilePath;
    private int blockSize;
    
    //used when starting from scratch
    public BlockManager(String metadataFilePath, int blockSize) throws IOException {
        this.metadataFilePath = metadataFilePath;
        this.blockSize = blockSize;
        this.emptyBlocks = new ArrayList<>();
        this.partiallyEmptyBlocks = new ArrayList<>();
        this.metadataFile = new BinaryFile(metadataFilePath);
    }

    //used when restoring state
    public BlockManager(String metadataFilePath) throws IOException {
        this.metadataFilePath = metadataFilePath;
        this.metadataFile = new BinaryFile(metadataFilePath);
        loadFromFile();
    }

    public int getBlockSize() {
        return blockSize;
    }
    
    public List<Integer> getEmptyBlocks() {
        return new ArrayList<>(emptyBlocks);
    }
    
    public List<Integer> getPartiallyEmptyBlocks() {
        return new ArrayList<>(partiallyEmptyBlocks);
    }

    
    public int getNextPartiallyEmptyBlock() {
        if (partiallyEmptyBlocks.isEmpty()) {
            return -1;
        }
        int minIndex = Collections.min(partiallyEmptyBlocks);
        partiallyEmptyBlocks.remove(Integer.valueOf(minIndex));
        return minIndex;
    }
    
    public int getNextEmptyBlock() {
        if (emptyBlocks.isEmpty()) {
            return -1;
        }
        int minIndex = Collections.min(emptyBlocks);
        emptyBlocks.remove(Integer.valueOf(minIndex));
        return minIndex;
    }

    public void manageEmptyBlock(int blockNumber) {
        emptyBlocks.add(blockNumber);
    }
    
    public void updateAfterInsert(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
    }
    
    public void updateAfterDelete(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
    }
    
    public void removeBlock(int blockIndex, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
    }
    
    public boolean isEmptyBlock(int blockIndex) {
        return emptyBlocks.contains(blockIndex);
    }
    
    public void saveToFile() {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
            
            dos.writeInt(blockSize);
            
            dos.writeInt(emptyBlocks.size());
            for (int idx : emptyBlocks) {
                dos.writeInt(idx);
            }
            
            dos.writeInt(partiallyEmptyBlocks.size());
            for (int idx : partiallyEmptyBlocks) {
                dos.writeInt(idx);
            }
            
            byte[] data = baos.toByteArray();
            
            metadataFile.seek(0);
            metadataFile.write(data);
            metadataFile.truncate(data.length);
            
        } catch (IOException e) {
            throw new RuntimeException("Error saving block metadata to file", e);
        }
    }
    
    private void loadFromFile() throws IOException {
        if (metadataFile.getSize() == 0) {
            throw new RuntimeException("Metadata file is empty or doesn't exist: " + metadataFilePath);
        }
        
        metadataFile.seek(0);
        java.io.DataInputStream dis = new java.io.DataInputStream(
            new java.io.ByteArrayInputStream(metadataFile.read((int)metadataFile.getSize())));
        
        this.blockSize = dis.readInt();
        
        int emptyCount = dis.readInt();
        this.emptyBlocks = new ArrayList<>();
        for (int i = 0; i < emptyCount; i++) {
            this.emptyBlocks.add(dis.readInt());
        }
        
        int partialCount = dis.readInt();
        this.partiallyEmptyBlocks = new ArrayList<>();
        for (int i = 0; i < partialCount; i++) {
            this.partiallyEmptyBlocks.add(dis.readInt());
        }
    }
    
    public void close() throws IOException {
        if (metadataFile != null) {
            saveToFile();
            metadataFile.close();
        }
    }
}
