package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import UnsortedFile.*;

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

            dataOutputStream.writeInt(nextOverflowBlock);

            dataOutputStream.writeInt(validBlockCount);

            byte[] result = byteArrayOutputStream.toByteArray();

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
    public void FromByteArray(byte[] inputArray, Class<T> recordClass) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            byte[][] recordBytesArray = new byte[blockingFactor][recordSize];
            for (int i = 0; i < blockingFactor; i++) {
                dataInputStream.readFully(recordBytesArray[i]);
            }

            nextOverflowBlock = dataInputStream.readInt();

            validBlockCount = dataInputStream.readInt();

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
