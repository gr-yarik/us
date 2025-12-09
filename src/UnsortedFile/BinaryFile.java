package UnsortedFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BinaryFile {
    
    private RandomAccessFile file;
    
    public BinaryFile(String pathToFile) throws IOException {
        File f = new File(pathToFile);
        
        this.file = new RandomAccessFile(f, "rw");
    }
    
    public void seek(long position) throws IOException {
        file.seek(position);
    }
    
    public long getSize() throws IOException {
        return file.length();
    }
    
    public void write(byte[] data) throws IOException {
        file.write(data);
    }
    
    public byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        file.readFully(data);
        return data;
    }
    
    public void truncate(long length) throws IOException {
        file.setLength(length);
    }
    
    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
    }
}
