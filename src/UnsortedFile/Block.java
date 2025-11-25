package UnsortedFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Block<T extends StorableRecord> {
    
    private int validBlockCount;
    private T[] records;
    private int recordSize;
    private int blockingFactor;
    private int blockSize;
    
    
    @SuppressWarnings("unchecked")
    public Block(int blockingFactor, int blockSize, Class<T> recordClass) {
        this.blockingFactor = blockingFactor;
        this.blockSize = blockSize;
        this.validBlockCount = 0;
        this.records = (T[]) java.lang.reflect.Array.newInstance(recordClass, blockingFactor);
        
        // Get record size from the template class
        try {
            T templateRecord = recordClass.getDeclaredConstructor().newInstance();
            this.recordSize = templateRecord.sizeInBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot instantiate record class to determine size: " + recordClass.getName(), e);
        }
    }
    
    public int getSize() {
        return blockSize;
    }

    /**
     * Gets a record at the specified index
     * @param index index of the record (0-based)
     * @return the record at index, or null if index is invalid or record is empty
     */
    public T getRecord(int index) {
        if (index < 0 || index >= blockingFactor || index >= validBlockCount) {
            return null;
        }
        return records[index];
    }
    
    /**
     * Gets the number of valid records in this block
     * @return number of valid records
     */
    public int getValidBlockCount() {
        return validBlockCount;
    }
    
    /**
     * Finds the index of a record using equals method
     * @param partialRecord the record to search for (using equals)
     * @return index of the record, or -1 if not found
     */
    public int findRecordIndex(T partialRecord) {
        for (int i = 0; i < validBlockCount; i++) {
            if (records[i] != null && partialRecord.equals(records[i])) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Adds a record to the next available position
     * @param record the record to add
     * @return true if successful, false if block is full
     */
    public boolean addRecord(T record) {
        if (validBlockCount >= blockingFactor) {
            return false; // Block is full
        }
        records[validBlockCount] = record;
        validBlockCount++;
        return true;
    }
    
    /**
     * Deletes a record using equals method to find it
     * Shifts remaining records left to fill the gap
     * @param partialRecord the record to delete (using equals)
     * @return true if record was found and deleted, false otherwise
     */
    public boolean delete(T partialRecord) {
        int index = findRecordIndex(partialRecord);
        if (index == -1) {
            return false; // Record not found
        }
        
        // Shift all records after the deleted one to the left
        for (int i = index; i < validBlockCount - 1; i++) {
            records[i] = records[i + 1];
        }
        validBlockCount--;
        // Null out the last element to avoid keeping a stale reference
        records[validBlockCount] = null;
        
        return true;
    }
    
    /**
     * Checks if the block is full
     * @return true if block is full, false otherwise
     */
    public boolean isFull() {
        return validBlockCount >= blockingFactor;
    }
    
    /**
     * Checks if the block is empty (has no valid records)
     * @return true if block is empty, false otherwise
     */
    public boolean isEmpty() {
        return validBlockCount == 0;
    }
    

    /**
     * Converts the block to a byte array for file storage
     * Format: records (blockingFactor * recordSize bytes) + validBlockCount (4 bytes)
     * Total size will be exactly blockSize bytes
     * @return byte array representation of the block (exactly blockSize bytes)
     */
    public byte[] ToByteArray() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            // Write all records first
            for (int i = 0; i < blockingFactor; i++) {
                if(i < validBlockCount) {
                    if (records[i] != null) {
                        byte[] recordBytes = records[i].ToByteArray();
                        hlpOutStream.write(recordBytes);
                    } else {
                        // Write empty record (zeros)
                        byte[] emptyRecord = new byte[recordSize];
                        hlpOutStream.write(emptyRecord);
                    }
                } else {
                    // Write empty record (zeros) for unused slots
                    byte[] emptyRecord = new byte[recordSize];
                    hlpOutStream.write(emptyRecord);
                }
            }
            // Write validBlockCount at the end
            hlpOutStream.writeInt(validBlockCount);
            
            byte[] result = hlpByteArrayOutputStream.toByteArray();
            
            // Ensure the result is exactly blockSize bytes
            if (result.length > blockSize) {
                throw new IllegalStateException("Block serialization exceeds blockSize: " + result.length + " > " + blockSize);
            }
            if (result.length < blockSize) {
                // Pad with zeros if smaller (shouldn't happen if blockingFactor calculated correctly)
                byte[] paddedResult = new byte[blockSize];
                System.arraycopy(result, 0, paddedResult, 0, result.length);
                return paddedResult;
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.", e);
        }
    }
    
    /**
     * Loads the block from a byte array
     * @param paArray byte array containing the block data (should be exactly blockSize bytes)
     * @param recordClass class type for instantiating records
     */
    public void FromByteArray(byte[] paArray, Class<T> recordClass) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(paArray);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            // Read all blockingFactor records first
            byte[][] recordBytesArray = new byte[blockingFactor][];
            for (int i = 0; i < blockingFactor; i++) {
                recordBytesArray[i] = new byte[recordSize];
                hlpInStream.readFully(recordBytesArray[i]);
            }
            // Read validBlockCount from the end (last 4 bytes)
            validBlockCount = hlpInStream.readInt();
            
            // Deserialize only the valid records
            for (int i = 0; i < blockingFactor; i++) {
                if (i < validBlockCount) {
                    try {
                        T record = recordClass.getDeclaredConstructor().newInstance();
                        record.FromByteArray(recordBytesArray[i]);
                        records[i] = record;
                    } catch (Exception e) {
                        records[i] = null;
                    }
                } else {
                    records[i] = null;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}

