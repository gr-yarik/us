package LinearHashing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import UnsortedFile.StorableRecord;

public class Person implements StorableRecord {
    static int sizeInBytes = 16 + 16 + 8 + 16;
    
    public String name;
    public String surname;
    public long birthdate; 
    public String id;

    public Person() {}

    @Override
    public boolean equals(StorableRecord record) {
        if (record == null || !(record instanceof Person)) {
            return false;
        }
        Person other = (Person) record;
        if (this.id == null) {
            return other.id == null;
        }
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
            int surnameLength = Math.min(surnameBytes.length, 15);
            byte[] surnameFixed = new byte[15];
            System.arraycopy(surnameBytes, 0, surnameFixed, 0, surnameLength);
            hlpOutStream.write(surnameFixed);
            hlpOutStream.writeByte(surnameLength);
            byte[] idBytes = (id != null ? id : "").getBytes(StandardCharsets.US_ASCII);
            int idLength = Math.min(idBytes.length, 15);
            byte[] idFixed = new byte[15];
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
            int nameLength = hlpInStream.readByte() & 0xFF;
            this.name = new String(nameBytes, 0, nameLength, StandardCharsets.US_ASCII);
            byte[] surnameBytes = new byte[15];
            hlpInStream.readFully(surnameBytes);
            int surnameLength = hlpInStream.readByte() & 0xFF;
            this.surname = new String(surnameBytes, 0, surnameLength, StandardCharsets.US_ASCII);
            byte[] idBytes = new byte[15];
            hlpInStream.readFully(idBytes);
            int idLength = hlpInStream.readByte() & 0xFF;
            this.id = new String(idBytes, 0, idLength, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.");
        }
    }
}

