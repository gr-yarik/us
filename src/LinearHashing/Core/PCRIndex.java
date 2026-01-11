package LinearHashing.Core;

import java.io.*;
import java.nio.charset.StandardCharsets;

import LinearHashing.HashableStorableRecord;
import UnsortedFile.StorableRecord;

public class PCRIndex implements HashableStorableRecord {
    public int testId;
    public String patientNumber;

    public PCRIndex() {}

    public PCRIndex(int testId, String patientNumber) {
        this.testId = testId;
        this.patientNumber = patientNumber;
    }

    @Override
    public boolean equals(StorableRecord record) {
        return this.testId == ((PCRIndex) record).testId;
    }

    @Override
    public int hashableIdentifier() {
        return testId;
    }

    @Override
    public int sizeInBytes() {
        return 15; 
    }

    @Override
    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeInt(testId);
            byte[] strBytes = (patientNumber).getBytes(StandardCharsets.US_ASCII);
            int length = Math.min(strBytes.length, 10);
            byte[] fixed = new byte[10];
            System.arraycopy(strBytes, 0, fixed, 0, length);
            dataOutputStream.write(fixed);
            dataOutputStream.writeByte(length);
            
            byte[] result = byteArrayOutputStream.toByteArray();
            if (result.length != sizeInBytes()) throw new RuntimeException("Size mismatch");
            return result;
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override
    public void FromByteArray(byte[] array) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            this.testId = dataInputStream.readInt();
            byte[] strBytes = new byte[10];
            dataInputStream.readFully(strBytes);
            int length = dataInputStream.readByte();
            this.patientNumber = new String(strBytes, 0, length, StandardCharsets.US_ASCII);
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}