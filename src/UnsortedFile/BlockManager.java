package UnsortedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockManager {
    
    private List<Integer> emptyBlocks;
    private List<Integer> partiallyEmptyBlocks;
    private BinaryFile metadataFile;
    
    public BlockManager(String metadataFilePath, List<Integer> emptyBlocks, List<Integer> partiallyEmptyBlocks) throws IOException {
        this.emptyBlocks = new ArrayList<>(emptyBlocks);
        this.partiallyEmptyBlocks = new ArrayList<>(partiallyEmptyBlocks);
        
        this.metadataFile = new BinaryFile(metadataFilePath);
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
    
    public void updateAfterInsert(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
        
        saveToFile(blockSize);
    }
    
    public void updateAfterDelete(int blockIndex, int validCount, int blockingFactor, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        
        if (validCount == 0) {
            emptyBlocks.add(blockIndex);
        } else if (validCount < blockingFactor) {
            partiallyEmptyBlocks.add(blockIndex);
        }
        
        saveToFile(blockSize);
    }
    
    public void removeBlock(int blockIndex, int blockSize) {
        emptyBlocks.remove(Integer.valueOf(blockIndex));
        partiallyEmptyBlocks.remove(Integer.valueOf(blockIndex));
        saveToFile(blockSize);
    }
    
    public void clearAllBlocks(int blockSize) {
        emptyBlocks.clear();
        partiallyEmptyBlocks.clear();
        saveToFile(blockSize);
    }
    
    public boolean isEmptyBlock(int blockIndex) {
        return emptyBlocks.contains(blockIndex);
    }
    
    public void saveToFile(int blockSize) {
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
    
    public static MetadataResult loadFromFile(String metadataFilePath) throws IOException {
        BinaryFile metadataFile = new BinaryFile(metadataFilePath);
        
        if (metadataFile.getSize() == 0) {
            metadataFile.close();
            return null;
        }
        
        try {
            metadataFile.seek(0);
            java.io.DataInputStream dis = new java.io.DataInputStream(
                new java.io.ByteArrayInputStream(metadataFile.read((int)metadataFile.getSize())));
            
            int blockSize = dis.readInt();
            
            int emptyCount = dis.readInt();
            List<Integer> emptyBlocks = new ArrayList<>();
            for (int i = 0; i < emptyCount; i++) {
                emptyBlocks.add(dis.readInt());
            }
            
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
    
    public static void saveToFile(String metadataFilePath, int blockSize, 
                                   List<Integer> emptyBlocks, List<Integer> partiallyEmptyBlocks) throws IOException {
        BinaryFile metadataFile = new BinaryFile(metadataFilePath);
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
        } finally {
            metadataFile.close();
        }
    }
    
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
    
    public void close() throws IOException {
        if (metadataFile != null) {
            metadataFile.close();
        }
    }
}

