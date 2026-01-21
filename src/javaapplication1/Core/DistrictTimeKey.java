package javaapplication1.Core;

import javaapplication1.TreeNodeData;

public class DistrictTimeKey implements TreeNodeData {
    
    public int districtCode;
    public long timestamp;
    public int testCode;
    public PCRTest test; // Reference to the full test object
    
    public DistrictTimeKey() {
        this.districtCode = 0;
        this.timestamp = 0;
        this.testCode = 0;
        this.test = null;
    }
    
    public DistrictTimeKey(int districtCode, long timestamp, int testCode, PCRTest test) {
        this.districtCode = districtCode;
        this.timestamp = timestamp;
        this.testCode = testCode;
        this.test = test;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof DistrictTimeKey)) {
            return 1;
        }
        DistrictTimeKey other = (DistrictTimeKey) otherData;
        
        // Compare order: districtCode → timestamp → testCode
        int districtComparison = Integer.compare(this.districtCode, other.districtCode);
        if (districtComparison != 0) {
            return districtComparison;
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
