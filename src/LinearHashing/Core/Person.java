package LinearHashing.Core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import LinearHashing.HashableStorableRecord;
import UnsortedFile.StorableRecord;

public class Person implements HashableStorableRecord {
    static int sizeInBytes = 50 + 4 + (6 * 44);

    public String name;
    public String surname;
    public long birthdate;
    public String id;

    public int validTestsCount = 0;
    public PCR[] pcrTests = new PCR[6];

    public Person() {
        for (int i = 0; i < 6; i++) {
            pcrTests[i] = new PCR();
        }
    }

    public boolean addTest(PCR test) {
        if (validTestsCount >= 6) {
            return false;
        }
        pcrTests[validTestsCount++] = test;
        return true;
    }

    @Override
    public boolean equals(StorableRecord record) {
        Person other = (Person) record;

        return this.id.equals(other.id);
    }

    @Override
    public int hashableIdentifier() {
        try {
            return Integer.parseInt(id);
        } catch (Error e) {
            int sum = 0;
            for (int i = 0; i < id.length(); i++) {
                sum += (int) id.charAt(i);
            }
            return sum;
        }
    }

    @Override
    public int sizeInBytes() {
        return sizeInBytes;
    }

    @Override
    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeLong(birthdate);

            byte[] nameBytes = (name != null ? name : "").getBytes(StandardCharsets.US_ASCII);
            int nameLength = Math.min(nameBytes.length, 15);
            byte[] nameFixed = new byte[15];
            System.arraycopy(nameBytes, 0, nameFixed, 0, nameLength);
            dataOutputStream.write(nameFixed);
            dataOutputStream.writeByte(nameLength);

            byte[] surnameBytes = (surname != null ? surname : "").getBytes(StandardCharsets.US_ASCII);
            int surnameLength = Math.min(surnameBytes.length, 14);
            byte[] surnameFixed = new byte[14];
            System.arraycopy(surnameBytes, 0, surnameFixed, 0, surnameLength);
            dataOutputStream.write(surnameFixed);
            dataOutputStream.writeByte(surnameLength);

            byte[] idBytes = (id).getBytes(StandardCharsets.US_ASCII);
            int idLength = Math.min(idBytes.length, 10);
            byte[] idFixed = new byte[10];
            System.arraycopy(idBytes, 0, idFixed, 0, idLength);
            dataOutputStream.write(idFixed);
            dataOutputStream.writeByte(idLength);

            dataOutputStream.writeInt(validTestsCount);

            byte[] emptyPCR = new byte[44];

            for (int i = 0; i < 6; i++) {
                if (i < validTestsCount && pcrTests[i] != null) {
                    dataOutputStream.write(pcrTests[i].ToByteArray());
                } else {
                    dataOutputStream.write(emptyPCR);
                }
            }

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error during conversion to byte array.", e);
        }
    }

    @Override
    public void FromByteArray(byte[] inputArray) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try {
            this.birthdate = dataInputStream.readLong();

            byte[] nameBytes = new byte[15];
            dataInputStream.readFully(nameBytes);
            int nameLength = dataInputStream.readByte();
            this.name = new String(nameBytes, 0, nameLength, StandardCharsets.US_ASCII);

            byte[] surnameBytes = new byte[14];
            dataInputStream.readFully(surnameBytes);
            int surnameLength = dataInputStream.readByte();
            this.surname = new String(surnameBytes, 0, surnameLength, StandardCharsets.US_ASCII);

            byte[] idBytes = new byte[10];
            dataInputStream.readFully(idBytes);
            int idLength = dataInputStream.readByte();
            this.id = new String(idBytes, 0, idLength, StandardCharsets.US_ASCII);

            this.validTestsCount = dataInputStream.readInt();

            for (int i = 0; i < 6; i++) {
                byte[] pcrBytes = new byte[44];
                dataInputStream.readFully(pcrBytes);

                if (i < validTestsCount) {
                    PCR p = new PCR();
                    p.FromByteArray(pcrBytes);
                    pcrTests[i] = p;
                } else {
                    pcrTests[i] = new PCR();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error during conversion from byte array.", e);
        }
    }
}