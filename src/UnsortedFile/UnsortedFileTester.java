package UnsortedFile;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


class Person implements StorableRecord {
 
    static int sizeInBytes = 16 + 16 + 8 + 16; //56
    
    String name;//max15
    String surname; //max15
    long birthdate; 
    String id; //max15

    @Override
    public boolean equals(StorableRecord record) {
        if (record == null || !(record instanceof Person)) {
            return false;
        }
        Person other = (Person) record;
        // Handle null ids - both must be null or both must be non-null and equal
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
            // Write name: fixed 15 bytes (padded), then length byte (after the string)
            byte[] nameBytes = (name != null ? name : "").getBytes(StandardCharsets.US_ASCII);
            int nameLength = Math.min(nameBytes.length, 15); // Cap at 15 bytes
            byte[] nameFixed = new byte[15];
            System.arraycopy(nameBytes, 0, nameFixed, 0, nameLength);
            hlpOutStream.write(nameFixed);
            hlpOutStream.writeByte(nameLength);
            // Write surname: fixed 15 bytes (padded), then length byte (after the string)
            byte[] surnameBytes = (surname != null ? surname : "").getBytes(StandardCharsets.US_ASCII);
            int surnameLength = Math.min(surnameBytes.length, 15); // Cap at 15 bytes
            byte[] surnameFixed = new byte[15];
            System.arraycopy(surnameBytes, 0, surnameFixed, 0, surnameLength);
            hlpOutStream.write(surnameFixed);
            hlpOutStream.writeByte(surnameLength);
            // Write id: fixed 15 bytes (padded), then length byte (after the string)
            byte[] idBytes = (id != null ? id : "").getBytes(StandardCharsets.US_ASCII);
            int idLength = Math.min(idBytes.length, 15); // Cap at 15 bytes
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
              // Read name: read 15 bytes, then read length byte, then trim to that length
              byte[] nameBytes = new byte[15];
              hlpInStream.readFully(nameBytes);
              int nameLength = hlpInStream.readByte() & 0xFF;
              this.name = new String(nameBytes, 0, nameLength, StandardCharsets.US_ASCII);
              // Read surname: read 15 bytes, then read length byte, then trim to that length
              byte[] surnameBytes = new byte[15];
              hlpInStream.readFully(surnameBytes);
              int surnameLength = hlpInStream.readByte() & 0xFF;
              this.surname = new String(surnameBytes, 0, surnameLength, StandardCharsets.US_ASCII);
              // Read id: read 15 bytes, then read length byte, then trim to that length
              byte[] idBytes = new byte[15];
              hlpInStream.readFully(idBytes);
              int idLength = hlpInStream.readByte() & 0xFF;
              this.id = new String(idBytes, 0, idLength, StandardCharsets.US_ASCII);
  
          } catch (IOException e) {
  throw new IllegalStateException("Error during conversion from byte array.");
  }
  }

}

public class UnsortedFileTester {
    public static void main(String[] args) {
        System.out.println("FileTester main method executed!");
        Person p1 = new Person();
        System.out.println("Size in bytes: " + p1.sizeInBytes());
        
        // Assign some test values
        p1.name = "John";
        p1.surname = "Doe";
        p1.birthdate = 19900101L;
        p1.id = "ID123456789";
        
        System.out.println("\nOriginal values:");
        System.out.println("Name: " + p1.name);
        System.out.println("Surname: " + p1.surname);
        System.out.println("Birthdate: " + p1.birthdate);
        System.out.println("ID: " + p1.id);
        
        // Convert to byte array
        byte[] byteArray = p1.ToByteArray();
        System.out.println("\nByte array length: " + byteArray.length);
        
        // Create new Person and load from byte array
        Person p2 = new Person();
        p2.FromByteArray(byteArray);
        
        System.out.println("\nAfter FromByteArray values:");
        System.out.println("Name: " + p2.name);
        System.out.println("Surname: " + p2.surname);
        System.out.println("Birthdate: " + p2.birthdate);
        System.out.println("ID: " + p2.id);
        
        // Verify values match
        System.out.println("\nVerification:");
        boolean nameMatch = p1.name.equals(p2.name);
        boolean surnameMatch = p1.surname.equals(p2.surname);
        boolean birthdateMatch = p1.birthdate == p2.birthdate;
        boolean idMatch = p1.id.equals(p2.id);
        
        System.out.println("Name matches: " + nameMatch);
        System.out.println("Surname matches: " + surnameMatch);
        System.out.println("Birthdate matches: " + birthdateMatch);
        System.out.println("ID matches: " + idMatch);
        
        if (nameMatch && surnameMatch && birthdateMatch && idMatch) {
            System.out.println("\nSUCCESS: All values match!");
        } else {
            System.out.println("\nERROR: Values do not match!");
        }
    }
}

