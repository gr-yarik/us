package UnsortedFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    public BlockManager(String metadataFilePath, int blockSize) {
        this.metadataFilePath = metadataFilePath;
        this.blockSize = blockSize;
        this.emptyBlocks = new ArrayList<>();
        this.partiallyEmptyBlocks = new ArrayList<>();
        this.metadataFile = new BinaryFile(metadataFilePath);
    }

    public BlockManager(String metadataFilePath) {
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
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

            dataOutputStream.writeInt(blockSize);

            dataOutputStream.writeInt(emptyBlocks.size());
            for (int idx : emptyBlocks) {
                dataOutputStream.writeInt(idx);
            }

            dataOutputStream.writeInt(partiallyEmptyBlocks.size());
            for (int idx : partiallyEmptyBlocks) {
                dataOutputStream.writeInt(idx);
            }

            byte[] data = byteArrayOutputStream.toByteArray();

            metadataFile.seek(0);
            metadataFile.write(data);
            metadataFile.truncate(data.length);

        } catch (IOException e) {
            throw new RuntimeException("Error saving block metadata to file", e);
        }
    }

    private void loadFromFile() {
        try {
            if (metadataFile.getSize() == 0) {
                throw new RuntimeException("Metadata file is empty or doesn't exist: " + metadataFilePath);
            }

            metadataFile.seek(0);
            DataInputStream dataInputStream = new DataInputStream(
                    new ByteArrayInputStream(metadataFile.read((int) metadataFile.getSize())));

            this.blockSize = dataInputStream.readInt();

            int emptyCount = dataInputStream.readInt();
            this.emptyBlocks = new ArrayList<>();
            for (int i = 0; i < emptyCount; i++) {
                this.emptyBlocks.add(dataInputStream.readInt());
            }

            int partialCount = dataInputStream.readInt();
            this.partiallyEmptyBlocks = new ArrayList<>();
            for (int i = 0; i < partialCount; i++) {
                this.partiallyEmptyBlocks.add(dataInputStream.readInt());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error saving block metadata to file", e);
        }
    }

    public void close() {
        if (metadataFile != null) {
            saveToFile();
            metadataFile.close();
        }
    }
}
