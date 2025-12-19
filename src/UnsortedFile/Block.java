package UnsortedFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Block<T extends StorableRecord> {

    protected int validBlockCount;
    protected T[] records;
    protected int recordSize;
    protected int blockingFactor;
    protected int blockSize;

    public Block(int blockingFactor, int blockSize, Class<T> recordClass) {
        this.blockingFactor = blockingFactor;
        this.blockSize = blockSize;
        this.validBlockCount = 0;
        this.records = (T[]) java.lang.reflect.Array.newInstance(recordClass, blockingFactor);

        try {
            T templateInstance = recordClass.getDeclaredConstructor().newInstance();
            this.recordSize = templateInstance.sizeInBytes();
        } catch (Exception e) { }
    }

    public T getRecord(int index) {
        return records[index];
    }

    public T[] debugGetAllRecords() {
        // T[] allSlots = (T[]) java.lang.reflect.Array.newInstance(
        // records.getClass().getComponentType(), blockingFactor);
        // System.arraycopy(records, 0, allSlots, 0, blockingFactor);
        // return allSlots;
        return records;
    }

    public T[] getAllValidRecords() {
        T[] validRecords = (T[]) java.lang.reflect.Array.newInstance(
                records.getClass().getComponentType(), validBlockCount);
        System.arraycopy(records, 0, validRecords, 0, validBlockCount);
        return validRecords;
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

    public int deleteAllRecords() {
        int temp = validBlockCount;
        for (int i = 0; i < temp; i++) {
            records[i] = null;
        }
        validBlockCount = 0;
        return temp;
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
                if (i < validBlockCount) {
                    byte[] recordBytes = records[i].ToByteArray();
                    hlpOutStream.write(recordBytes);
                } else {
                    byte[] emptyRecord = new byte[recordSize];
                    hlpOutStream.write(emptyRecord);
                }
            }
            hlpOutStream.writeInt(validBlockCount);

            byte[] result = hlpByteArrayOutputStream.toByteArray();

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
            byte[][] recordBytesArray = new byte[blockingFactor][recordSize];
            for (int i = 0; i < blockingFactor; i++) {
                hlpInStream.readFully(recordBytesArray[i]);
            }

            for (int i = 0; i < blockingFactor; i++) {
                T record = recordClass.getDeclaredConstructor().newInstance();
                record.FromByteArray(recordBytesArray[i]);
                records[i] = record;
            }
            validBlockCount = hlpInStream.readInt();

        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}
