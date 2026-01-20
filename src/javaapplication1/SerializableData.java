package javaapplication1;

public interface SerializableData {
    byte[] ToByteArray();
    void FromByteArray(byte[] inputArray);
}
