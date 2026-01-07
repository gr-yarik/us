package UnsortedFile;


public interface StorableRecord {

    public boolean equals(StorableRecord record);

    public int sizeInBytes();
    
    public byte[] ToByteArray();
    
    public void FromByteArray(byte[] paArray);
}