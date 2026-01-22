package javaapplication1.Core;

import javaapplication1.TreeNodeData;

/**
 * Composite key for efficient searching by patient ID and test code.
 * Used for optimizing operation #2: Search for a test result by test code and patient ID.
 * 
 * Comparison order:
 * 1. patientId (String comparison)
 * 2. testCode (Integer comparison)
 */
public class PatientTestCodeKey implements TreeNodeData {
    
    private String patientId;
    private int testCode;
    private PCRTest test; // Reference to the actual test object
    private Person person; // Reference to the person object
    
    public PatientTestCodeKey() {
        this.patientId = "";
        this.testCode = 0;
        this.test = null;
        this.person = null;
    }
    
    public PatientTestCodeKey(String patientId, int testCode, PCRTest test, Person person) {
        this.patientId = patientId;
        this.testCode = testCode;
        this.test = test;
        this.person = person;
    }
    
    @Override
    public int compare(TreeNodeData otherData) {
        if (!(otherData instanceof PatientTestCodeKey)) {
            throw new IllegalArgumentException("Cannot compare PatientTestCodeKey with other types");
        }
        
        PatientTestCodeKey other = (PatientTestCodeKey) otherData;
        
        int patientIdComparison = this.patientId.compareTo(other.patientId);
        if (patientIdComparison != 0) {
            return patientIdComparison;
        }
        
        return Integer.compare(this.testCode, other.testCode);
    }
    
    public String getPatientId() {
        return patientId;
    }
    
    public int getTestCode() {
        return testCode;
    }
    
    public PCRTest getTest() {
        return test;
    }
    
    public Person getPerson() {
        return person;
    }
    
    @Override
    public String toString() {
        return String.format("PatientTestCodeKey[patientId=%s, testCode=%d]", patientId, testCode);
    }
}
