package UnsortedFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BinaryFile {
    
    private RandomAccessFile file;
    
 
    public BinaryFile(String pathToFile) throws IOException {
        File f = new File(pathToFile);
        
        // Create parent directories if they don't exist
        if (f.getParentFile() != null && !f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        
        this.file = new RandomAccessFile(f, "rw");
    }
    
    /**
     * Seeks to a specific position in the file
     * @param position the position (in bytes) to seek to
     * @throws IOException if seek operation fails
     */
    public void seek(long position) throws IOException {
        file.seek(position);
    }
    
    /**
     * Gets the current file size in bytes
     * @return the size of the file in bytes
     * @throws IOException if file size cannot be determined
     */
    public long getSize() throws IOException {
        return file.length();
    }
    
    /**
     * Writes a byte array to the file at the current position
     * @param data the byte array to write
     * @throws IOException if write operation fails
     */
    public void write(byte[] data) throws IOException {
        file.write(data);
    }
    
    /**
     * Reads a byte array from the file at the current position
     * @param length the number of bytes to read
     * @return the byte array read from the file
     * @throws IOException if read operation fails
     */
    public byte[] read(int length) throws IOException {
        byte[] data = new byte[length];
        file.readFully(data);
        return data;
    }
    
    /**
     * Truncates the file to the specified length
     * @param length the new length of the file in bytes
     * @throws IOException if truncation fails
     */
    public void truncate(long length) throws IOException {
        file.setLength(length);
    }
    
    /**
     * Closes the file
     * @throws IOException if file cannot be closed
     */
    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
    }
}
