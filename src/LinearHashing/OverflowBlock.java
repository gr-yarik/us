package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import UnsortedFile.Block;
import UnsortedFile.StorableRecord;

public class OverflowBlock<T extends StorableRecord> extends Block<T> {

    private int nextOverflowBlock;

    public OverflowBlock(int blockingFactor, int blockSize, Class<T> recordClass) {
        super(blockingFactor, blockSize, recordClass);
        this.nextOverflowBlock = -1;
    }

    public int getNextOverflowBlock() {
        return nextOverflowBlock;
    }

    public void setNextOverflowBlock(int blockNumber) {
        this.nextOverflowBlock = blockNumber;
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

            hlpOutStream.writeInt(nextOverflowBlock);

            hlpOutStream.writeInt(validBlockCount);

            byte[] result = hlpByteArrayOutputStream.toByteArray();

            if (result.length > blockSize) {
                throw new IllegalStateException(
                        "OverflowBlock serialization exceeds blockSize: " + result.length + " > " + blockSize);
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

            nextOverflowBlock = hlpInStream.readInt();

            validBlockCount = hlpInStream.readInt();

            for (int i = 0; i < blockingFactor; i++) {
                T record = recordClass.getDeclaredConstructor().newInstance();
                record.FromByteArray(recordBytesArray[i]);
                records[i] = record;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}
