package UnsortedFile;

import java.io.*;
import java.util.*;

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
        return records;
    }

    public List<T> getAllValidRecords() {
        List<T> validRecords = new ArrayList<>();
        for (int i = 0; i < validBlockCount; i++) {
            validRecords.add(records[i]);
        }
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

    public T getRecord(T partialRecord) {
        int index = findRecordIndex(partialRecord);
        return index == -1 ? null : getRecord(index);
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
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            for (int i = 0; i < blockingFactor; i++) {
                if (i < validBlockCount) {
                    byte[] recordBytes = records[i].ToByteArray();
                    dataOutputStream.write(recordBytes);
                } else {
                    byte[] emptyRecord = new byte[recordSize];
                    dataOutputStream.write(emptyRecord);
                }
            }
            dataOutputStream.writeInt(validBlockCount);

            byte[] result = byteArrayOutputStream.toByteArray();

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

    public void FromByteArray(byte[] inputArray, Class<T> recordClass) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            byte[][] recordBytesArray = new byte[blockingFactor][recordSize];
            for (int i = 0; i < blockingFactor; i++) {
                dataInputStream.readFully(recordBytesArray[i]);
            }

            for (int i = 0; i < blockingFactor; i++) {
                T record = recordClass.getDeclaredConstructor().newInstance();
                record.FromByteArray(recordBytesArray[i]);
                records[i] = record;
            }
            validBlockCount = dataInputStream.readInt();

        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}
