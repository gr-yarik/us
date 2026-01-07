package UnsortedFile;

import java.io.File;
import java.io.RandomAccessFile;

public class BinaryFile {
    
    private RandomAccessFile file;
    
    public BinaryFile(String pathToFile) {
        File f = new File(pathToFile);
        
        try {
            this.file = new RandomAccessFile(f, "rw");
        } catch (Exception e){}
    }
    
    public void seek(long position) {
        try {
            file.seek(position);
        } catch (Exception e){}
    }
    
    public long getSize() {
        try {
            return file.length();
        } catch (Exception e){}
        throw new Error();
    }
    
    public void write(byte[] data) {
        try {
            file.write(data);
        } catch (Exception e){}
    }
    
    public byte[] read(int length) {
        try {
            byte[] data = new byte[length];
            file.readFully(data);
            return data;
        } catch (Exception e){}
        throw new Error();
    }
    
    public void truncate(long length) {
        try {
            file.setLength(length);
        } catch (Exception e){}
    }
    
    public void close() {
        try {
            if (file != null) {
                file.close();
            }
        } catch (Exception e){}
    }
}
