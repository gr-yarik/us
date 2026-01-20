package javaapplication1.Core;

import javaapplication1.TreeNodeData;
import javaapplication1.SerializableData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PCRTest implements TreeNodeData, SerializableData {
    
    public int testCode;
    public String patientId;
    public long timestamp;
    public int workplaceCode;
    public int districtCode;
    public int regionCode;
    public boolean testResult;
    public double testValue;
    public String note;
    
    public PCRTest() {
        this.testCode = 0;
        this.patientId = "";
        this.timestamp = 0;
        this.workplaceCode = 0;
        this.districtCode = 0;
        this.regionCode = 0;
        this.testResult = false;
        this.testValue = 0.0;
        this.note = "";
    }
    
    public PCRTest(int testCode, String patientId, long timestamp, int workplaceCode, 
                   int districtCode, int regionCode, boolean testResult, double testValue, String note) {
        this.testCode = testCode;
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.workplaceCode = workplaceCode;
        this.districtCode = districtCode;
        this.regionCode = regionCode;
        this.testResult = testResult;
        this.testValue = testValue;
        this.note = note;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof PCRTest)) {
            return 1;
        }
        PCRTest other = (PCRTest) otherData;
        return Integer.compare(this.testCode, other.testCode);
    }
    
    @Override
    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            // Write primitive fields
            dataOutputStream.writeInt(testCode);
            dataOutputStream.writeLong(timestamp);
            dataOutputStream.writeInt(workplaceCode);
            dataOutputStream.writeInt(districtCode);
            dataOutputStream.writeInt(regionCode);
            dataOutputStream.writeBoolean(testResult);
            dataOutputStream.writeDouble(testValue);
            
            // Write patientId (length-prefixed)
            byte[] patientIdBytes = (patientId != null ? patientId : "").getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(patientIdBytes.length);
            dataOutputStream.write(patientIdBytes);
            
            // Write note (length-prefixed)
            byte[] noteBytes = (note != null ? note : "").getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(noteBytes.length);
            dataOutputStream.write(noteBytes);
            
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion to byte array.", e);
        }
    }
    
    @Override
    public void FromByteArray(byte[] inputArray) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            // Read primitive fields
            this.testCode = dataInputStream.readInt();
            this.timestamp = dataInputStream.readLong();
            this.workplaceCode = dataInputStream.readInt();
            this.districtCode = dataInputStream.readInt();
            this.regionCode = dataInputStream.readInt();
            this.testResult = dataInputStream.readBoolean();
            this.testValue = dataInputStream.readDouble();
            
            // Read patientId (length-prefixed)
            int patientIdLength = dataInputStream.readInt();
            byte[] patientIdBytes = new byte[patientIdLength];
            dataInputStream.readFully(patientIdBytes);
            this.patientId = new String(patientIdBytes, StandardCharsets.UTF_8);
            
            // Read note (length-prefixed)
            int noteLength = dataInputStream.readInt();
            byte[] noteBytes = new byte[noteLength];
            dataInputStream.readFully(noteBytes);
            this.note = new String(noteBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
    
    @Override
    public String toString() {
        return "PCRTest{" +
                "testCode=" + testCode +
                ", patientId='" + patientId + '\'' +
                ", timestamp=" + timestamp +
                ", workplaceCode=" + workplaceCode +
                ", districtCode=" + districtCode +
                ", regionCode=" + regionCode +
                ", testResult=" + testResult +
                ", testValue=" + testValue +
                ", note='" + note + '\'' +
                '}';
    }
}
