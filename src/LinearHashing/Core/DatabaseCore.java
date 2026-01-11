package LinearHashing.Core;

import LinearHashing.*;

public class DatabaseCore {
    
    private LinearHash<Person> personFile;
    private LinearHash<PCRIndex> indexFile;

    public DatabaseCore(String personsMain, String personsOverflow,
                       int personsMainBlockSize, int personsOverflowBlockSize,
                       String pcrMain, String pcrOverflow,
                       int pcrMainBlockSize, int pcrOverflowBlockSize) {
        this.personFile = new LinearHash<>(personsMain, personsOverflow, 
            personsMainBlockSize, personsOverflowBlockSize, Person.class);
        this.indexFile = new LinearHash<>(pcrMain, pcrOverflow,
            pcrMainBlockSize, pcrOverflowBlockSize, PCRIndex.class);
    }

    public DatabaseCore(String personsMain, String personsMainMeta,
                       String personsOverflow, String personsOverflowMeta,
                       String pcrMain, String pcrMainMeta,
                       String pcrOverflow, String pcrOverflowMeta) {
        this.personFile = new LinearHash<>(personsMain, personsMainMeta,
            personsOverflow, personsOverflowMeta, Person.class);
        this.indexFile = new LinearHash<>(pcrMain, pcrMainMeta,
            pcrOverflow, pcrOverflowMeta, PCRIndex.class);
    }

    public boolean insertPCR(PCR test) {
    
        Person partialPersonRecord = new Person();
        partialPersonRecord.id = test.patientNumber;
        Person personRecord = personFile.get(partialPersonRecord);
        
        if (personRecord == null) {
            System.out.println("Error: Person with ID " + test.patientNumber + " not found.");
            return false;
        }

        if (!personRecord.addTest(test)) {
            System.out.println("Error: Max 6 tests reached for this person.");
            return false;
        }

        if (!personFile.delete(partialPersonRecord)) {
            System.out.println("Error: Failed to delete old person record for update.");
            return false;
        }
        personFile.insert(personRecord);            

        indexFile.insert(new PCRIndex(test.testCode, test.patientNumber));
        return true;
    }

    public Person searchPerson(String patientID) {
        Person partialRecord = new Person();
        partialRecord.id = patientID;
        return personFile.get(partialRecord);
    }

    public Person searchPCR(int testCode) {
        PCRIndex partialTestRecord = new PCRIndex();
        partialTestRecord.testId = testCode;
        PCRIndex testRecord = indexFile.get(partialTestRecord);
        
        if (testRecord == null) {
            System.out.println("Error: PCR Test Code " + testCode + " not found.");
            return null;
        }

        return searchPerson(testRecord.patientNumber);
    }

    public boolean insertPerson(Person person) {
        personFile.insert(person);
        return true;
    }

    public boolean deletePCR(int testCode) {
        PCRIndex partialTestRecord = new PCRIndex();
        partialTestRecord.testId = testCode;
        PCRIndex testRecord = indexFile.get(partialTestRecord);

        if (testRecord == null) {
            System.out.println("Error: Cannot delete. PCR Test Code " + testCode + " not found.");
            return false;
        }

        Person partialPersonRecord = new Person();
        partialPersonRecord.id = testRecord.patientNumber;
        Person personRecord = personFile.get(partialPersonRecord);

        if (personRecord == null) {
            throw new Error("Error: Index is present but person no. This sould not happen");
        }

        boolean foundInArray = false;
        for (int i = 0; i < personRecord.validTestsCount; i++) {
            if (personRecord.pcrTests[i].testCode == testCode) {
                for (int j = i; j < personRecord.validTestsCount - 1; j++) {
                    personRecord.pcrTests[j] = personRecord.pcrTests[j + 1];
                }
                personRecord.pcrTests[personRecord.validTestsCount - 1] = new PCR(); 
                personRecord.validTestsCount--;
                foundInArray = true;
                break;
            }
        }

        if (!foundInArray) {
            throw new Error("Error: Test found in index but not in person record. This should not happen"); 
        }

        personFile.delete(partialPersonRecord);
        personFile.insert(personRecord);

        indexFile.delete(testRecord);
        return true;
    }

    public boolean deletePerson(String personId) {
        Person partialRecord = new Person();
        partialRecord.id = personId;
        Person personRecord = personFile.get(partialRecord);
        
        if (personRecord == null) {
            System.out.println("Error: Person with ID " + personId + " not found.");
            return false;
        }

        for (int i = 0; i < personRecord.validTestsCount; i++) {
            PCRIndex testRecord = new PCRIndex();
            testRecord.testId = personRecord.pcrTests[i].testCode;
            indexFile.delete(testRecord);
        }

        return personFile.delete(partialRecord);
    }

    public boolean editPerson(Person updatedData) {
        Person existing = personFile.get(updatedData);
        if (existing == null) {
            System.out.println("Error: Person with ID " + updatedData.id + " not found.");
            return false;
        }

        existing.name = updatedData.name;
        existing.surname = updatedData.surname;
        existing.birthdate = updatedData.birthdate;

        personFile.delete(existing);
        personFile.insert(existing);
        return true;
    }

    public boolean editPCR(int testCode, PCR updatedTestData) {
        Person p = searchPCR(testCode);
        if (p == null) {
            return false;
        }

        boolean found = false;
        for (int i = 0; i < p.validTestsCount; i++) {
            if (p.pcrTests[i].testCode == testCode) {
                
                p.pcrTests[i].dateTime = updatedTestData.dateTime;
                p.pcrTests[i].testResult = updatedTestData.testResult;
                p.pcrTests[i].testValue = updatedTestData.testValue;
                p.pcrTests[i].note = updatedTestData.note;
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Error: PCR Data consistency error.");
            return false;
        }

        personFile.delete(p);
        personFile.insert(p);
        return true;
    }
    
    public LinearHash<Person> getPersonFile() {
        return personFile;
    }

    public LinearHash<PCRIndex> getIndexFile() {
        return indexFile;
    }

    public void close() {
        personFile.close();
        indexFile.close();
    }
}