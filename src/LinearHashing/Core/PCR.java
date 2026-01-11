package LinearHashing.Core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import UnsortedFile.StorableRecord;

public class PCR implements StorableRecord {
    
    static int sizeInBytes = 8 + 10 + 1 + 4 + 1 + 8 + 11 + 1;
    
    public long dateTime;
    public String patientNumber;
    public int testCode;  
    public boolean testResult; 
    public double testValue;  
    public String note;

    public PCR() {}

    @Override
    public boolean equals(StorableRecord record) {
        if (record == null || !(record instanceof PCR)) {
            return false;
        }
        PCR other = (PCR) record;
        return this.testCode == other.testCode;
    }

    @Override
    public int sizeInBytes() {
        return sizeInBytes;
    }

    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeLong(dateTime);
            byte[] patientNumberBytes = (patientNumber != null ? patientNumber : "").getBytes(StandardCharsets.US_ASCII);
            int patientNumberLength = Math.min(patientNumberBytes.length, 10);
            byte[] patientNumberFixed = new byte[10];
            System.arraycopy(patientNumberBytes, 0, patientNumberFixed, 0, patientNumberLength);
            dataOutputStream.write(patientNumberFixed);
            dataOutputStream.writeByte(patientNumberLength);
            dataOutputStream.writeInt(testCode);
            dataOutputStream.writeBoolean(testResult);
            dataOutputStream.writeDouble(testValue);
            byte[] noteBytes = (note != null ? note : "").getBytes(StandardCharsets.US_ASCII);
            int noteLength = Math.min(noteBytes.length, 11);
            byte[] noteFixed = new byte[11];
            System.arraycopy(noteBytes, 0, noteFixed, 0, noteLength);
            dataOutputStream.write(noteFixed);
            dataOutputStream.writeByte(noteLength);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }
    }

    public void FromByteArray(byte[] inputArray) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            this.dateTime = dataInputStream.readLong();
            byte[] patientNumberBytes = new byte[10];
            dataInputStream.readFully(patientNumberBytes);
            int patientNumberLength = dataInputStream.readByte();
            this.patientNumber = new String(patientNumberBytes, 0, patientNumberLength, StandardCharsets.US_ASCII);
            this.testCode = dataInputStream.readInt();
            this.testResult = dataInputStream.readBoolean();
            this.testValue = dataInputStream.readDouble();
            byte[] noteBytes = new byte[11];
            dataInputStream.readFully(noteBytes);
            int noteLength = dataInputStream.readByte();
            this.note = new String(noteBytes, 0, noteLength, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }
    }
}

