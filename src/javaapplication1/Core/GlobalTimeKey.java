package javaapplication1.Core;

import javaapplication1.TreeNodeData;

public class GlobalTimeKey implements TreeNodeData {
    
    public long timestamp;
    public int testCode;
    public PCRTest test; // Reference to the full test object
    
    public GlobalTimeKey() {
        this.timestamp = 0;
        this.testCode = 0;
        this.test = null;
    }
    
    public GlobalTimeKey(long timestamp, int testCode, PCRTest test) {
        this.timestamp = timestamp;
        this.testCode = testCode;
        this.test = test;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof GlobalTimeKey)) {
            return 1;
        }
        GlobalTimeKey other = (GlobalTimeKey) otherData;
        
        // Compare order: timestamp → testCode
        int timestampComparison = Long.compare(this.timestamp, other.timestamp);
        if (timestampComparison != 0) {
            return timestampComparison;
        }
        
        return Integer.compare(this.testCode, other.testCode);
    }
    
    public PCRTest getTest() {
        return test;
    }
}
