package javaapplication1.Core;

import javaapplication1.TreeNodeData;
import javaapplication1.SerializableData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Person implements TreeNodeData, SerializableData {
    
    public String firstName;
    public String lastName;
    public long dateOfBirth;
    public String patientId;
    
    public Person() {
        this.firstName = "";
        this.lastName = "";
        this.dateOfBirth = 0;
        this.patientId = "";
    }
    
    public Person(String patientId, String firstName, String lastName, long dateOfBirth) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof Person)) {
            return 1;
        }
        Person other = (Person) otherData;
        return this.patientId.compareTo(other.patientId);
    }
    
    @Override
    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeLong(dateOfBirth);
            
            byte[] firstNameBytes = (firstName != null ? firstName : "").getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(firstNameBytes.length);
            dataOutputStream.write(firstNameBytes);
            
            byte[] lastNameBytes = (lastName != null ? lastName : "").getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(lastNameBytes.length);
            dataOutputStream.write(lastNameBytes);
            
            byte[] patientIdBytes = (patientId != null ? patientId : "").getBytes(StandardCharsets.UTF_8);
            dataOutputStream.writeInt(patientIdBytes.length);
            dataOutputStream.write(patientIdBytes);
            
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
            this.dateOfBirth = dataInputStream.readLong();
            
            int firstNameLength = dataInputStream.readInt();
            byte[] firstNameBytes = new byte[firstNameLength];
            dataInputStream.readFully(firstNameBytes);
            this.firstName = new String(firstNameBytes, StandardCharsets.UTF_8);
            
            int lastNameLength = dataInputStream.readInt();
            byte[] lastNameBytes = new byte[lastNameLength];
            dataInputStream.readFully(lastNameBytes);
            this.lastName = new String(lastNameBytes, StandardCharsets.UTF_8);
            
            int patientIdLength = dataInputStream.readInt();
            byte[] patientIdBytes = new byte[patientIdLength];
            dataInputStream.readFully(patientIdBytes);
            this.patientId = new String(patientIdBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
    
    @Override
    public String toString() {
        return "Person{" +
                "patientId='" + patientId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                '}';
    }
}
