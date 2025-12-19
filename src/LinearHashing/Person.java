package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import UnsortedFile.StorableRecord;

public class Person implements StorableRecord {
    // Size: 8 (birthdate) + 15 (name) + 1 (name length) + 14 (surname) + 1 (surname length) + 10 (patient number) + 1 (patient number length) = 50
    static int sizeInBytes = 8 + 15 + 1 + 14 + 1 + 10 + 1;
    
    public String name;  // max 15 char
    public String surname;  // max 14 char
    public long birthdate;  // using LocalDate.now().toEpochDay()
    public String id;  // max 10 char

    public Person() {}

    @Override
    public boolean equals(StorableRecord record) {
        Person other = (Person) record;
        return this.id.equals(other.id);
    }

    @Override
    public int sizeInBytes() {
        return sizeInBytes;
    }

    public byte[] ToByteArray() {
        ByteArrayOutputStream hlpByteArrayOutputStream= new ByteArrayOutputStream();
        DataOutputStream hlpOutStream = new DataOutputStream(hlpByteArrayOutputStream);
        try{
            hlpOutStream.writeLong(birthdate);
            byte[] nameBytes = (name != null ? name : "").getBytes(StandardCharsets.US_ASCII);
            int nameLength = Math.min(nameBytes.length, 15);
            byte[] nameFixed = new byte[15];
            System.arraycopy(nameBytes, 0, nameFixed, 0, nameLength);
            hlpOutStream.write(nameFixed);
            hlpOutStream.writeByte(nameLength);
            byte[] surnameBytes = (surname != null ? surname : "").getBytes(StandardCharsets.US_ASCII);
            int surnameLength = Math.min(surnameBytes.length, 14);
            byte[] surnameFixed = new byte[14];
            System.arraycopy(surnameBytes, 0, surnameFixed, 0, surnameLength);
            hlpOutStream.write(surnameFixed);
            hlpOutStream.writeByte(surnameLength);
            byte[] idBytes = (id != null ? id : "").getBytes(StandardCharsets.US_ASCII);
            int idLength = Math.min(idBytes.length, 10);
            byte[] idFixed = new byte[10];
            System.arraycopy(idBytes, 0, idFixed, 0, idLength);
            hlpOutStream.write(idFixed);
            hlpOutStream.writeByte(idLength);
            return hlpByteArrayOutputStream.toByteArray();
        }catch (Exception e){
            throw new IllegalStateException("Error during conversion to byte array.");
        }
    }
  
    public void FromByteArray(byte[] paArray) {
        ByteArrayInputStream hlpByteArrayInputStream = new ByteArrayInputStream(paArray);
        DataInputStream hlpInStream = new DataInputStream(hlpByteArrayInputStream);
        try {
            this.birthdate = hlpInStream.readLong();
            byte[] nameBytes = new byte[15];
            hlpInStream.readFully(nameBytes);
            int nameLength = hlpInStream.readByte();
            this.name = new String(nameBytes, 0, nameLength, StandardCharsets.US_ASCII);
            byte[] surnameBytes = new byte[14];
            hlpInStream.readFully(surnameBytes);
            int surnameLength = hlpInStream.readByte();
            this.surname = new String(surnameBytes, 0, surnameLength, StandardCharsets.US_ASCII);
            byte[] idBytes = new byte[10];
            hlpInStream.readFully(idBytes);
            int idLength = hlpInStream.readByte();
            this.id = new String(idBytes, 0, idLength, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }
    }
}

