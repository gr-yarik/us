package LinearHashing.Core;

import LinearHashing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UIController {
    
    //this class should create and destroy database core instances, as a user wants in ui

    //in ui, user specifies that they want to open the database in a folder. each folder will either contain all
    // eight files, called persons_main, persons_main.meta, persons_overflow, persons_overflow.meta, and the same for pcr ....
    // or 0 files. depending on this, one or another constructor of LinearHash should be called. 

    //for editing operations, if the user leaves empty fields, they should be remain the same 

    //when sending objects to the ui layer, a helper structure must be used
    //  (not pass by reference the original obbject from database)
    // create all such structures as inner structures in this class

    private DatabaseCore database;
    private String currentFolder;
    
    // File names
    private static final String PERSONS_MAIN = "persons_main";
    private static final String PERSONS_MAIN_META = "persons_main.meta";
    private static final String PERSONS_OVERFLOW = "persons_overflow";
    private static final String PERSONS_OVERFLOW_META = "persons_overflow.meta";
    private static final String PCR_MAIN = "pcr_main";
    private static final String PCR_MAIN_META = "pcr_main.meta";
    private static final String PCR_OVERFLOW = "pcr_overflow";
    private static final String PCR_OVERFLOW_META = "pcr_overflow.meta";
    
    // Default block sizes for new databases
    public static final int DEFAULT_PERSONS_MAIN_BLOCK_SIZE = 2048;
    public static final int DEFAULT_PERSONS_OVERFLOW_BLOCK_SIZE = 1024;
    public static final int DEFAULT_PCR_MAIN_BLOCK_SIZE = 512;
    public static final int DEFAULT_PCR_OVERFLOW_BLOCK_SIZE = 256;
    
    // ==================== DTO Inner Classes ====================
    
    public static class PersonDTO {
        public String id;
        public String name;
        public String surname;
        public long birthdate;
        public List<PCRDTO> tests;
        
        public PersonDTO() {
            this.tests = new ArrayList<>();
        }
        
        public PersonDTO(Person p) {
            this.id = p.id;
            this.name = p.name;
            this.surname = p.surname;
            this.birthdate = p.birthdate;
            this.tests = new ArrayList<>();
            for (int i = 0; i < p.validTestsCount; i++) {
                this.tests.add(new PCRDTO(p.pcrTests[i]));
            }
        }
        
        public Person toEntity() {
            Person p = new Person();
            p.id = this.id;
            p.name = this.name;
            p.surname = this.surname;
            p.birthdate = this.birthdate;
            return p;
        }
    }
    
    public static class PCRDTO {
        public int testCode;
        public String patientNumber;
        public long dateTime;
        public boolean testResult;
        public double testValue;
        public String note;
        
        public PCRDTO() {}
        
        public PCRDTO(PCR pcr) {
            this.testCode = pcr.testCode;
            this.patientNumber = pcr.patientNumber;
            this.dateTime = pcr.dateTime;
            this.testResult = pcr.testResult;
            this.testValue = pcr.testValue;
            this.note = pcr.note;
        }
        
        public PCR toEntity() {
            PCR pcr = new PCR();
            pcr.testCode = this.testCode;
            pcr.patientNumber = this.patientNumber;
            pcr.dateTime = this.dateTime;
            pcr.testResult = this.testResult;
            pcr.testValue = this.testValue;
            pcr.note = this.note;
            return pcr;
        }
    }
    
    public static class PCRIndexDTO {
        public int testId;
        public String patientNumber;
        
        public PCRIndexDTO() {}
        
        public PCRIndexDTO(PCRIndex idx) {
            this.testId = idx.testId;
            this.patientNumber = idx.patientNumber;
        }
    }
    
    // ==================== Database Management ====================
    
    public enum FolderStatus {
        EMPTY,           // 0 database files - can create new
        COMPLETE,        // All 8 files present - can open existing
        INVALID          // Some files present - invalid state
    }
    
    public FolderStatus checkFolderStatus(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return FolderStatus.INVALID;
        }
        
        String[] requiredFiles = {
            PERSONS_MAIN, PERSONS_MAIN_META, PERSONS_OVERFLOW, PERSONS_OVERFLOW_META,
            PCR_MAIN, PCR_MAIN_META, PCR_OVERFLOW, PCR_OVERFLOW_META
        };
        
        int existingCount = 0;
        for (String fileName : requiredFiles) {
            File f = new File(folder, fileName);
            if (f.exists()) {
                existingCount++;
            }
        }
        
        if (existingCount == 0) {
            return FolderStatus.EMPTY;
        } else if (existingCount == 8) {
            return FolderStatus.COMPLETE;
        } else {
            return FolderStatus.INVALID;
        }
    }
    
    public String openDatabase(String folderPath) {
        FolderStatus status = checkFolderStatus(folderPath);
        
        switch (status) {
            case COMPLETE:
                return openExistingDatabase(folderPath);
            case INVALID:
                return "Error: Folder contains incomplete database files. Expected 0 or 8 files.";
            case EMPTY:
            default:
                // For empty folders, caller should use createNewDatabase with custom block sizes
                return "NEEDS_BLOCK_SIZES";
        }
    }
    
    public String createNewDatabase(String folderPath, int personsMainBlockSize, int personsOverflowBlockSize,
                                     int pcrMainBlockSize, int pcrOverflowBlockSize) {
        try {
            closeDatabase();
            
            String personsMain = folderPath + File.separator + PERSONS_MAIN;
            String personsOverflow = folderPath + File.separator + PERSONS_OVERFLOW;
            String pcrMain = folderPath + File.separator + PCR_MAIN;
            String pcrOverflow = folderPath + File.separator + PCR_OVERFLOW;
            
            database = new DatabaseCore(
                personsMain, personsOverflow,
                personsMainBlockSize, personsOverflowBlockSize,
                pcrMain, pcrOverflow,
                pcrMainBlockSize, pcrOverflowBlockSize
            );
            
            currentFolder = folderPath;
            return null; // Success
        } catch (Exception e) {
            return "Error creating database: " + e.getMessage();
        }
    }
    
    private String openExistingDatabase(String folderPath) {
        try {
            closeDatabase();
            
            String personsMain = folderPath + File.separator + PERSONS_MAIN;
            String personsMainMeta = folderPath + File.separator + PERSONS_MAIN_META;
            String personsOverflow = folderPath + File.separator + PERSONS_OVERFLOW;
            String personsOverflowMeta = folderPath + File.separator + PERSONS_OVERFLOW_META;
            String pcrMain = folderPath + File.separator + PCR_MAIN;
            String pcrMainMeta = folderPath + File.separator + PCR_MAIN_META;
            String pcrOverflow = folderPath + File.separator + PCR_OVERFLOW;
            String pcrOverflowMeta = folderPath + File.separator + PCR_OVERFLOW_META;
            
            database = new DatabaseCore(
                personsMain, personsMainMeta, personsOverflow, personsOverflowMeta,
                pcrMain, pcrMainMeta, pcrOverflow, pcrOverflowMeta
            );
            
            currentFolder = folderPath;
            return null; // Success
        } catch (Exception e) {
            return "Error opening database: " + e.getMessage();
        }
    }
    
    public void closeDatabase() {
        if (database != null) {
            database.close();
            database = null;
            currentFolder = null;
        }
    }
    
    public boolean isDatabaseOpen() {
        return database != null;
    }
    
    public String getCurrentFolder() {
        return currentFolder;
    }
    
    public DatabaseCore getDatabaseCore() {
        return database;
    }
    
    // ==================== Operations (using DTOs) ====================
    
    // Operation 1: Insert PCR Test
    public String insertPCR(PCRDTO testDTO) {
        if (database == null) return "Error: No database open";
        boolean success = database.insertPCR(testDTO.toEntity());
        return success ? null : "Failed to insert PCR test";
    }
    
    // Operation 2: Search Person
    public PersonDTO searchPerson(String personId) {
        if (database == null) return null;
        Person p = database.searchPerson(personId);
        return p != null ? new PersonDTO(p) : null;
    }
    
    // Operation 3: Search PCR by Code
    public PersonDTO searchPCR(int testCode) {
        if (database == null) return null;
        Person p = database.searchPCR(testCode);
        return p != null ? new PersonDTO(p) : null;
    }
    
    // Operation 4: Insert Person
    public String insertPerson(PersonDTO personDTO) {
        if (database == null) return "Error: No database open";
        boolean success = database.insertPerson(personDTO.toEntity());
        return success ? null : "Failed to insert person";
    }
    
    // Operation 5: Delete PCR Test
    public String deletePCR(int testCode) {
        if (database == null) return "Error: No database open";
        boolean success = database.deletePCR(testCode);
        return success ? null : "Failed to delete PCR test";
    }
    
    // Operation 6: Delete Person
    public String deletePerson(String personId) {
        if (database == null) return "Error: No database open";
        boolean success = database.deletePerson(personId);
        return success ? null : "Failed to delete person";
    }
    
    // Operation 7: Edit Person (empty fields remain unchanged)
    public String editPerson(String personId, String newName, String newSurname, Long newBirthdate) {
        if (database == null) return "Error: No database open";
        
        Person existing = database.searchPerson(personId);
        if (existing == null) {
            return "Error: Person not found";
        }
        
        // Only update non-empty fields
        Person updated = new Person();
        updated.id = personId;
        updated.name = (newName != null && !newName.isEmpty()) ? newName : existing.name;
        updated.surname = (newSurname != null && !newSurname.isEmpty()) ? newSurname : existing.surname;
        updated.birthdate = (newBirthdate != null) ? newBirthdate : existing.birthdate;
        
        boolean success = database.editPerson(updated);
        return success ? null : "Failed to edit person";
    }
    
    // Operation 8: Edit PCR Test (empty fields remain unchanged)
    public String editPCR(int testCode, Long newDateTime, Boolean newResult, Double newValue, String newNote) {
        if (database == null) return "Error: No database open";
        
        Person personWithTest = database.searchPCR(testCode);
        if (personWithTest == null) {
            return "Error: PCR test not found";
        }
        
        // Find existing test data
        PCR existingTest = null;
        for (int i = 0; i < personWithTest.validTestsCount; i++) {
            if (personWithTest.pcrTests[i].testCode == testCode) {
                existingTest = personWithTest.pcrTests[i];
                break;
            }
        }
        
        if (existingTest == null) {
            return "Error: PCR test not found in person record";
        }
        
        // Only update non-null fields
        PCR updated = new PCR();
        updated.testCode = testCode;
        updated.patientNumber = existingTest.patientNumber;
        updated.dateTime = (newDateTime != null) ? newDateTime : existingTest.dateTime;
        updated.testResult = (newResult != null) ? newResult : existingTest.testResult;
        updated.testValue = (newValue != null) ? newValue : existingTest.testValue;
        updated.note = (newNote != null && !newNote.isEmpty()) ? newNote : existingTest.note;
        
        boolean success = database.editPCR(testCode, updated);
        return success ? null : "Failed to edit PCR test";
    }
    
    // ==================== File Content Reading ====================
    
    public List<PersonDTO> getAllPersons() {
        List<PersonDTO> result = new ArrayList<>();
        if (database == null) return result;
        
        LinearHash<Person> personFile = database.getPersonFile();
        BucketHeap<Person> bucketHeap = personFile.getBucketHeap();
        int totalBuckets = personFile.getTotalPrimaryBuckets();
        
        for (int i = 0; i < totalBuckets; i++) {
            try {
                Bucket<Person> bucket = (Bucket<Person>) bucketHeap.getMainBucketsHeap().readBlock(i);
                for (Person p : bucket.getAllValidRecords()) {
                    result.add(new PersonDTO(p));
                }
                
                // Read overflow chain
                int overflowNum = bucket.getFirstOverflowBlock();
                while (overflowNum != -1) {
                    OverflowBlock<Person> overflow = bucketHeap.getOverflowHeap()
                        .readBlock(overflowNum, OverflowBlock.class);
                    for (Person p : overflow.getAllValidRecords()) {
                        result.add(new PersonDTO(p));
                    }
                    overflowNum = overflow.getNextOverflowBlock();
                }
            } catch (Exception e) {
                // Skip invalid blocks
            }
        }
        
        return result;
    }
    
    public List<PCRIndexDTO> getAllPCRIndices() {
        List<PCRIndexDTO> result = new ArrayList<>();
        if (database == null) return result;
        
        LinearHash<PCRIndex> indexFile = database.getIndexFile();
        BucketHeap<PCRIndex> bucketHeap = indexFile.getBucketHeap();
        int totalBuckets = indexFile.getTotalPrimaryBuckets();
        
        for (int i = 0; i < totalBuckets; i++) {
            try {
                Bucket<PCRIndex> bucket = (Bucket<PCRIndex>) bucketHeap.getMainBucketsHeap().readBlock(i);
                for (PCRIndex idx : bucket.getAllValidRecords()) {
                    result.add(new PCRIndexDTO(idx));
                }
                
                // Read overflow chain
                int overflowNum = bucket.getFirstOverflowBlock();
                while (overflowNum != -1) {
                    OverflowBlock<PCRIndex> overflow = bucketHeap.getOverflowHeap()
                        .readBlock(overflowNum, OverflowBlock.class);
                    for (PCRIndex idx : overflow.getAllValidRecords()) {
                        result.add(new PCRIndexDTO(idx));
                    }
                    overflowNum = overflow.getNextOverflowBlock();
                }
            } catch (Exception e) {
                // Skip invalid blocks
            }
        }
        
        return result;
    }
}
