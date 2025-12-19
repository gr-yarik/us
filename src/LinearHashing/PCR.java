package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import UnsortedFile.StorableRecord;

public class PCR implements StorableRecord {
    
    // Size: 8 (date/time) + 10 (patient number) + 1 (patient number length) + 4 (test code) + 1 (test result) + 8 (test value) + 11 (note) + 1 (note length) = 44
    static int sizeInBytes = 8 + 10 + 1 + 4 + 1 + 8 + 11 + 1;
    
    public long dateTime;  // Instant.now().toEpochMilli())
    public String patientNumber;  // max 10 char
    public int testCode;  
    public boolean testResult; 
    public double testValue;  
    public String note;  // Note, max 11 characters

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
        ByteArrayOutputStream hlpByteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try {
            hlpOutStream.writeLong(dateTime);
            byte[] patientNumberBytes = (patientNumber != null ? patientNumber : "").getBytes(StandardCharsets.US_ASCII);
            int patientNumberLength = Math.min(patientNumberBytes.length, 10);
            byte[] patientNumberFixed = new byte[10];
            System.arraycopy(patientNumberBytes, 0, patientNumberFixed, 0, patientNumberLength);
            hlpOutStream.write(patientNumberFixed);
            hlpOutStream.writeByte(patientNumberLength);
            hlpOutStream.writeInt(testCode);
            hlpOutStream.writeBoolean(testResult);
            hlpOutStream.writeDouble(testValue);
            byte[] noteBytes = (note != null ? note : "").getBytes(StandardCharsets.US_ASCII);
            int noteLength = Math.min(noteBytes.length, 11);
            byte[] noteFixed = new byte[11];
            System.arraycopy(noteBytes, 0, noteFixed, 0, noteLength);
            hlpOutStream.write(noteFixed);
            hlpOutStream.writeByte(noteLength);
            return hlpByteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion to byte array.");
        }
    }

    public void FromByteArray(byte[] paArray) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(paArray);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            this.dateTime = hlpInStream.readLong();
            byte[] patientNumberBytes = new byte[10];
            hlpInStream.readFully(patientNumberBytes);
            int patientNumberLength = hlpInStream.readByte();
            this.patientNumber = new String(patientNumberBytes, 0, patientNumberLength, StandardCharsets.US_ASCII);
            this.testCode = hlpInStream.readInt();
            this.testResult = hlpInStream.readBoolean();
            this.testValue = hlpInStream.readDouble();
            byte[] noteBytes = new byte[11];
            hlpInStream.readFully(noteBytes);
            int noteLength = hlpInStream.readByte();
            this.note = new String(noteBytes, 0, noteLength, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }
    }
}

