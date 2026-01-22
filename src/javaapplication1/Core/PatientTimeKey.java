package javaapplication1.Core;

import javaapplication1.TreeNodeData;

public class PatientTimeKey implements TreeNodeData {
    
    public String patientId;
    public long timestamp;
    public int testCode;
    public PCRTest test; // Reference to the full test object
    
    public PatientTimeKey() {
        this.patientId = "";
        this.timestamp = 0;
        this.testCode = 0;
        this.test = null;
    }
    
    public PatientTimeKey(String patientId, long timestamp, int testCode, PCRTest test) {
        this.patientId = patientId;
        this.timestamp = timestamp;
        this.testCode = testCode;
        this.test = test;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (otherData == null || !(otherData instanceof PatientTimeKey)) {
            return 1;
        }
        PatientTimeKey other = (PatientTimeKey) otherData;
        
        int patientIdComparison = this.patientId.compareTo(other.patientId);
        if (patientIdComparison != 0) {
            return patientIdComparison;
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
