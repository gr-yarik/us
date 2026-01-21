package javaapplication1.Core;

import javaapplication1.TreeNodeData;

public class RegionTimeKey implements TreeNodeData {
    
    public int regionCode;
    public long timestamp;
    public int testCode;
    public PCRTest test; // Reference to the full test object
    
    public RegionTimeKey() {
        this.regionCode = 0;
        this.timestamp = 0;
        this.testCode = 0;
        this.test = null;
    }
    
    public RegionTimeKey(int regionCode, long timestamp, int testCode, PCRTest test) {
        this.regionCode = regionCode;
        this.timestamp = timestamp;
        this.testCode = testCode;
        this.test = test;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof RegionTimeKey)) {
            return 1;
        }
        RegionTimeKey other = (RegionTimeKey) otherData;
        
        // Compare order: regionCode → timestamp → testCode
        int regionComparison = Integer.compare(this.regionCode, other.regionCode);
        if (regionComparison != 0) {
            return regionComparison;
        }
        
        int timestampComparison = Long.compare(this.timestamp, other.timestamp);
        if (timestampComparison != 0) {
            return timestampComparison;
        }
        
        return Integer.compare(this.testCode, other.testCode);
    }
    
    public PCRTest getTest() {
        return test;
    }
    
    @Override
    public byte[] ToByteArray() {
        throw new UnsupportedOperationException("RegionTimeKey is not serializable (RAM-only)");
    }
    
    @Override
    public void FromByteArray(byte[] inputArray) {
        throw new UnsupportedOperationException("RegionTimeKey is not serializable (RAM-only)");
    }
}
