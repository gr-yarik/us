package javaapplication1.Core;

import javaapplication1.TreeNodeData;

public class WorkplaceTimeKey implements TreeNodeData {
    
    public int workplaceCode;
    public long timestamp;
    public int testCode;
    public PCRTest test; // Reference to the full test object
    
    public WorkplaceTimeKey() {
        this.workplaceCode = 0;
        this.timestamp = 0;
        this.testCode = 0;
        this.test = null;
    }
    
    public WorkplaceTimeKey(int workplaceCode, long timestamp, int testCode, PCRTest test) {
        this.workplaceCode = workplaceCode;
        this.timestamp = timestamp;
        this.testCode = testCode;
        this.test = test;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof WorkplaceTimeKey)) {
            return 1;
        }
        WorkplaceTimeKey other = (WorkplaceTimeKey) otherData;
        
        int workplaceComparison = Integer.compare(this.workplaceCode, other.workplaceCode);
        if (workplaceComparison != 0) {
            return workplaceComparison;
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
}
