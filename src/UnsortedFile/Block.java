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

    public T getRecord(int index) {
        if (index < 0 || index >= blockingFactor || index >= validBlockCount) {
            return null;
        }
        return records[index];
    }
    
    public T[] getAllRecordSlots() {
        @SuppressWarnings("unchecked")
        T[] allSlots = (T[]) java.lang.reflect.Array.newInstance(
            records.getClass().getComponentType(), blockingFactor);
        System.arraycopy(records, 0, allSlots, 0, blockingFactor);
        return allSlots;
    }
    
    public int getValidBlockCount() {
        return validBlockCount;
    }
    
    public int findRecordIndex(T partialRecord) {
        for (int i = 0; i < validBlockCount; i++) {
            if (records[i] != null && partialRecord.equals(records[i])) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean addRecord(T record) {
        if (validBlockCount >= blockingFactor) {
            return false;
        }
        records[validBlockCount] = record;
        validBlockCount++;
        return true;
    }
    
    public boolean delete(T partialRecord) {
        int index = findRecordIndex(partialRecord);
        if (index == -1) {
            return false;
        }
        
        for (int i = index; i < validBlockCount - 1; i++) {
            records[i] = records[i + 1];
        }
        validBlockCount--;
        records[validBlockCount] = null;
        
        return true;
    }
    
    public boolean isFull() {
        return validBlockCount >= blockingFactor;
    }
    
    public boolean isEmpty() {
        return validBlockCount == 0;
    }
    

    public byte[] ToByteArray() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            for (int i = 0; i < blockingFactor; i++) {
                if(i < validBlockCount) {
                    if (records[i] != null) {
                        byte[] recordBytes = records[i].ToByteArray();
                        hlpOutStream.write(recordBytes);
                    } else {
                        byte[] emptyRecord = new byte[recordSize];
                        hlpOutStream.write(emptyRecord);
                    }
                } else {
                    byte[] emptyRecord = new byte[recordSize];
                    hlpOutStream.write(emptyRecord);
                }
            }
            hlpOutStream.writeInt(validBlockCount);
            
            byte[] result = hlpByteArrayOutputStream.toByteArray();
            
            if (result.length > blockSize) {
                throw new IllegalStateException("Block serialization exceeds blockSize: " + result.length + " > " + blockSize);
            }
            if (result.length < blockSize) {
                byte[] paddedResult = new byte[blockSize];
                System.arraycopy(result, 0, paddedResult, 0, result.length);
                return paddedResult;
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.", e);
        }
    }
    
    public void FromByteArray(byte[] paArray, Class<T> recordClass) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(paArray);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            byte[][] recordBytesArray = new byte[blockingFactor][];
            for (int i = 0; i < blockingFactor; i++) {
                recordBytesArray[i] = new byte[recordSize];
                hlpInStream.readFully(recordBytesArray[i]);
            }
            validBlockCount = hlpInStream.readInt();
            
            for (int i = 0; i < blockingFactor; i++) {
                    try {
                        T record = recordClass.getDeclaredConstructor().newInstance();
                        record.FromByteArray(recordBytesArray[i]);
                        records[i] = record;
                    } catch (Exception e) {
                        records[i] = null;
                    }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}

