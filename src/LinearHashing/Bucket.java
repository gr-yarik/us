package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import UnsortedFile.Block;
import UnsortedFile.StorableRecord;

public class Bucket<T extends StorableRecord> extends Block<T> {
    
    private int overflowBucketCount;
    private int totalElementCount;
    private int firstOverflowBlock;
    
    public Bucket(int blockingFactor, int blockSize, Class<T> recordClass) {
        super(blockingFactor, blockSize, recordClass);
        this.overflowBucketCount = 0;
        this.totalElementCount = 0;
        this.firstOverflowBlock = -1;
    }
    
    public int getFirstOverflowBlock() {
        return firstOverflowBlock;
    }
    
    public void setFirstOverflowBlock(int blockNumber) {
        this.firstOverflowBlock = blockNumber;
    }
    
    public int getOverflowBucketCount() {
        return overflowBucketCount;
    }
    
    public void setOverflowBucketCount(int count) {
        this.overflowBucketCount = count;
    }

    public void decrementOverflowBucketCountByOne() {
        this.overflowBucketCount--;
    }

    public void incrementOverflowBucketCountByOne() {
        this.overflowBucketCount++;
    }
    
    public int getTotalElementCount() {
        return totalElementCount;
    }
    
    public void incrementTotalElementCountByOne() {
        this.totalElementCount++;
    }

    public void incrementTotalElementCountBy(int howMuch) {
        this.totalElementCount += howMuch;
    }
    
    public void decrementTotalElementCountByOne() {
        this.totalElementCount--;
    }

    public void decrementTotalElementCountBy(int howMuch) {
        this.totalElementCount -= howMuch;
    }
    
    @Override
    public byte[] ToByteArray() {
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            for (int i = 0; i < blockingFactor; i++) {
                if (i < validBlockCount) {
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
            
            hlpOutStream.writeInt(overflowBucketCount);
            
            hlpOutStream.writeInt(totalElementCount);
            
            hlpOutStream.writeInt(firstOverflowBlock);
            
            hlpOutStream.writeInt(validBlockCount);
            
            byte[] result = hlpByteArrayOutputStream.toByteArray();
            
            if (result.length > blockSize) {
                throw new IllegalStateException("Bucket serialization exceeds blockSize: " + result.length + " > " + blockSize);
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
    
    @Override
    public void FromByteArray(byte[] paArray, Class<T> recordClass) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(paArray);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            byte[][] recordBytesArray = new byte[blockingFactor][];
            for (int i = 0; i < blockingFactor; i++) {
                recordBytesArray[i] = new byte[recordSize];
                hlpInStream.readFully(recordBytesArray[i]);
            }
            
            overflowBucketCount = hlpInStream.readInt();
            
            totalElementCount = hlpInStream.readInt();
            
            firstOverflowBlock = hlpInStream.readInt();
            
            validBlockCount = hlpInStream.readInt();
            
            for (int i = 0; i < blockingFactor; i++) {
                if (i < validBlockCount) {
                    try {
                        boolean isEmpty = true;
                        for (byte b : recordBytesArray[i]) {
                            if (b != 0) {
                                isEmpty = false;
                                break;
                            }
                        }
                        
                        if (isEmpty) {
                            records[i] = null;
                        } else {
                            T record = recordClass.getDeclaredConstructor().newInstance();
                            record.FromByteArray(recordBytesArray[i]);
                            records[i] = record;
                        }
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
