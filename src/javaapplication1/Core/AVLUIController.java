package javaapplication1.Core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AVLUIController {
    
    // This class manages DatabaseCore instances for the AVL tree-based database
    // UI allows user to create/open/close databases stored in folders
    // Each folder contains 2 files: persons_data and tests_data
    
    private DatabaseCore database;
    private String currentFolder;
    
    // File names
    private static final String PERSONS_FILE = "persons_data";
    private static final String TESTS_FILE = "tests_data";
    
    // Date formatters (public for UI access)
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy");
    public static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd MMMM yyyy HH:mm");
    
    // ==================== DTO Inner Classes ====================
    
    public static class PersonDTO {
        public String firstName;
        public String lastName;
        public long dateOfBirth;
        public String patientId;
        
        public PersonDTO() {}
        
        public PersonDTO(Person p) {
            this.firstName = p.firstName;
            this.lastName = p.lastName;
            this.dateOfBirth = p.dateOfBirth;
            this.patientId = p.patientId;
        }
        
        public String getFormattedDateOfBirth() {
            return DATE_FORMAT.format(new Date(dateOfBirth));
        }
        
        @Override
        public String toString() {
            return String.format("ID: %s | Name: %s %s | DOB: %s", 
                patientId, firstName, lastName, getFormattedDateOfBirth());
        }
    }
    
    public static class PCRTestDTO {
        public int testCode;
        public String patientId;
        public long timestamp;
        public int workplaceCode;
        public int districtCode;
        public int regionCode;
        public boolean testResult;
        public double testValue;
        public String note;
        
        public PCRTestDTO() {}
        
        public PCRTestDTO(PCRTest test) {
            this.testCode = test.testCode;
            this.patientId = test.patientId;
            this.timestamp = test.timestamp;
            this.workplaceCode = test.workplaceCode;
            this.districtCode = test.districtCode;
            this.regionCode = test.regionCode;
            this.testResult = test.testResult;
            this.testValue = test.testValue;
            this.note = test.note;
        }
        
        public String getFormattedTimestamp() {
            return DATETIME_FORMAT.format(new Date(timestamp));
        }
        
        public String getResultString() {
            return testResult ? "POSITIVE" : "NEGATIVE";
        }
        
        @Override
        public String toString() {
            return String.format("Code: %d | Patient: %s | Time: %s | Result: %s\n| Value: %.2f | District: %d | Region: %d | Workplace: %d | Note: %s",
                testCode, patientId, getFormattedTimestamp(), getResultString(), testValue, districtCode, regionCode, workplaceCode, note);
        }
    }
    
    public static class TestDetailDTO {
        public PCRTestDTO test;
        public PersonDTO person;
        
        public TestDetailDTO(PCRTestDTO test, PersonDTO person) {
            this.test = test;
            this.person = person;
        }
        
        @Override
        public String toString() {
            return String.format("Test[%s]\nPerson[%s]", test.toString(), person.toString());
        }
    }
    
    public static class QueryResultDTO {
        public List<TestDetailDTO> results;
        public int totalCount;
        
        public QueryResultDTO(List<TestDetailDTO> results, int totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total Count: %d\n", totalCount));
            sb.append("=".repeat(80)).append("\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append(String.format("[%d] %s\n", i + 1, results.get(i).toString()));
            }
            return sb.toString();
        }
    }
    
    public static class SickPersonDTO {
        public PersonDTO person;
        public PCRTestDTO causingTest;
        
        public SickPersonDTO(PersonDTO person, PCRTestDTO causingTest) {
            this.person = person;
            this.causingTest = causingTest;
        }
        
        @Override
        public String toString() {
            return String.format("Sick Person[%s]\nCaused by Test[%s]", person.toString(), causingTest.toString());
        }
    }
    
    public static class SickPersonQueryResultDTO {
        public List<SickPersonDTO> results;
        public int totalCount;
        
        public SickPersonQueryResultDTO(List<SickPersonDTO> results, int totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total Sick Persons: %d\n", totalCount));
            sb.append("=".repeat(80)).append("\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append(String.format("[%d] %s\n", i + 1, results.get(i).toString()));
            }
            return sb.toString();
        }
    }
    
    public static class DistrictStatisticDTO {
        public int districtCode;
        public int sickCount;
        
        public DistrictStatisticDTO(int districtCode, int sickCount) {
            this.districtCode = districtCode;
            this.sickCount = sickCount;
        }
        
        @Override
        public String toString() {
            return String.format("District %d: %d sick persons", districtCode, sickCount);
        }
    }
    
    public static class RegionStatisticDTO {
        public int regionCode;
        public int sickCount;
        
        public RegionStatisticDTO(int regionCode, int sickCount) {
            this.regionCode = regionCode;
            this.sickCount = sickCount;
        }
        
        @Override
        public String toString() {
            return String.format("Region %d: %d sick persons", regionCode, sickCount);
        }
    }
    
    // ==================== Database Management ====================
    
    public enum FolderStatus {
        EMPTY,           // 0 database files - can create new
        COMPLETE,        // Both files present - can open existing
        INVALID          // Only one file present - invalid state
    }
    
    public FolderStatus checkFolderStatus(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return FolderStatus.INVALID;
        }
        
        File personsFile = new File(folder, PERSONS_FILE);
        File testsFile = new File(folder, TESTS_FILE);
        
        boolean personsExists = personsFile.exists();
        boolean testsExists = testsFile.exists();
        
        if (!personsExists && !testsExists) {
            return FolderStatus.EMPTY;
        } else if (personsExists && testsExists) {
            return FolderStatus.COMPLETE;
        } else {
            return FolderStatus.INVALID;
        }
    }
    
    public String createNewDatabase(String folderPath) {
        try {
            closeDatabase();
            
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            
            String personsPath = folderPath + File.separator + PERSONS_FILE;
            String testsPath = folderPath + File.separator + TESTS_FILE;
            
            database = new DatabaseCore(personsPath, testsPath, true);
            currentFolder = folderPath;
            return null; // Success
        } catch (Exception e) {
            return "Error creating database: " + e.getMessage();
        }
    }
    
    public String openExistingDatabase(String folderPath) {
        try {
            closeDatabase();
            
            String personsPath = folderPath + File.separator + PERSONS_FILE;
            String testsPath = folderPath + File.separator + TESTS_FILE;
            
            database = new DatabaseCore(personsPath, testsPath, false);
            currentFolder = folderPath;
            return null; // Success
        } catch (Exception e) {
            return "Error opening database: " + e.getMessage();
        }
    }
    
    public void closeDatabase() {
        if (database != null) {
            try {
                database.closeDatabase();
            } catch (Exception e) {
                // Ignore errors on close
            }
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
    
    // ==================== Operations (21 total) ====================
    
    // 1. Insert a PCR test result into the system
    public String insertPCRTest(PCRTestDTO testDTO) {
        if (database == null) return "Error: No database open";
        try {
            PCRTest test = new PCRTest();
            test.testCode = testDTO.testCode;
            test.patientId = testDTO.patientId;
            test.timestamp = testDTO.timestamp;
            test.workplaceCode = testDTO.workplaceCode;
            test.districtCode = testDTO.districtCode;
            test.regionCode = testDTO.regionCode;
            test.testResult = testDTO.testResult;
            test.testValue = testDTO.testValue;
            test.note = testDTO.note;
            
            boolean success = database.insertPCRTest(test);
            return success ? null : "Failed to insert PCR test (duplicate test code or patient not found)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 2. Search for a test result by test code and patient ID
    public String searchTestByCode(int testCode, String patientId) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.TestDetailRecord result = database.searchTestByCode(testCode, patientId);
            if (result == null) {
                return "Test not found or does not belong to specified patient";
            }
            PCRTestDTO testDTO = new PCRTestDTO(result.test());
            PersonDTO personDTO = new PersonDTO(result.person());
            TestDetailDTO detailDTO = new TestDetailDTO(testDTO, personDTO);
            return detailDTO.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 3. List all PCR tests for a given patient
    public String listTestsForPatient(String patientId) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listTestsForPatient(patientId);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 4. List positive tests in district within time range
    public String listPositiveTestsInDistrictTimeRange(int districtCode, long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listPositiveTestsInDistrictTimeRange(districtCode, startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 5. List all tests in district within time range
    public String listAllTestsInDistrictTimeRange(int districtCode, long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listAllTestsInDistrictTimeRange(districtCode, startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 6. List positive tests in region within time range
    public String listPositiveTestsInRegionTimeRange(int regionCode, long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listPositiveTestsInRegionTimeRange(regionCode, startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 7. List all tests in region within time range
    public String listAllTestsInRegionTimeRange(int regionCode, long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listAllTestsInRegionTimeRange(regionCode, startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 8. List all positive tests within time period
    public String listAllPositiveTestsInTimeRange(long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listAllPositiveTestsInTimeRange(startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 9. List all tests within time period
    public String listAllTestsInTimeRange(long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listAllTestsInTimeRange(startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 10. List sick persons in district
    public String listSickPersonsInDistrict(int districtCode, long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.SickPersonQueryResult result = database.listSickPersonsInDistrict(districtCode, asOfDate, sicknessDurationDays);
            List<SickPersonDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.SickPersonRecord rec : result.results()) {
                dtoList.add(new SickPersonDTO(new PersonDTO(rec.person()), new PCRTestDTO(rec.causingTest())));
            }
            SickPersonQueryResultDTO dto = new SickPersonQueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 11. List sick persons in district sorted by test value
    public String listSickPersonsInDistrictSortedByValue(int districtCode, long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.SickPersonQueryResult result = database.listSickPersonsInDistrictSortedByTestValue(districtCode, asOfDate, sicknessDurationDays);
            List<SickPersonDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.SickPersonRecord rec : result.results()) {
                dtoList.add(new SickPersonDTO(new PersonDTO(rec.person()), new PCRTestDTO(rec.causingTest())));
            }
            SickPersonQueryResultDTO dto = new SickPersonQueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 12. List sick persons in region
    public String listSickPersonsInRegion(int regionCode, long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.SickPersonQueryResult result = database.listSickPersonsInRegion(regionCode, asOfDate, sicknessDurationDays);
            List<SickPersonDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.SickPersonRecord rec : result.results()) {
                dtoList.add(new SickPersonDTO(new PersonDTO(rec.person()), new PCRTestDTO(rec.causingTest())));
            }
            SickPersonQueryResultDTO dto = new SickPersonQueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 13. List all sick persons
    public String listAllSickPersons(long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.SickPersonQueryResult result = database.listAllSickPersons(asOfDate, sicknessDurationDays);
            List<SickPersonDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.SickPersonRecord rec : result.results()) {
                dtoList.add(new SickPersonDTO(new PersonDTO(rec.person()), new PCRTestDTO(rec.causingTest())));
            }
            SickPersonQueryResultDTO dto = new SickPersonQueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 14. List sickest person per district
    public String listSickestPersonPerDistrict(long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.SickPersonQueryResult result = database.listSickestPersonPerDistrict(asOfDate, sicknessDurationDays);
            List<SickPersonDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.SickPersonRecord rec : result.results()) {
                dtoList.add(new SickPersonDTO(new PersonDTO(rec.person()), new PCRTestDTO(rec.causingTest())));
            }
            SickPersonQueryResultDTO dto = new SickPersonQueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 15. List districts sorted by sick count
    public String listDistrictsSortedBySickCount(long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            List<DatabaseCore.DistrictStatistic> result = database.listDistrictsSortedBySickCount(asOfDate, sicknessDurationDays);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total Districts: %d\n", result.size()));
            sb.append("=".repeat(80)).append("\n");
            for (int i = 0; i < result.size(); i++) {
                DatabaseCore.DistrictStatistic stat = result.get(i);
                sb.append(String.format("[%d] District %d: %d sick persons\n", i + 1, stat.districtCode(), stat.sickCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 16. List regions sorted by sick count
    public String listRegionsSortedBySickCount(long asOfDate, int sicknessDurationDays) {
        if (database == null) return "Error: No database open";
        try {
            List<DatabaseCore.RegionStatistic> result = database.listRegionsSortedBySickCount(asOfDate, sicknessDurationDays);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Total Regions: %d\n", result.size()));
            sb.append("=".repeat(80)).append("\n");
            for (int i = 0; i < result.size(); i++) {
                DatabaseCore.RegionStatistic stat = result.get(i);
                sb.append(String.format("[%d] Region %d: %d sick persons\n", i + 1, stat.regionCode(), stat.sickCount()));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 17. List tests at workplace in time range
    public String listAllTestsAtWorkplaceInTimeRange(int workplaceCode, long startTime, long endTime) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.QueryResult result = database.listAllTestsAtWorkplaceInTimeRange(workplaceCode, startTime, endTime);
            List<TestDetailDTO> dtoList = new ArrayList<>();
            for (DatabaseCore.TestDetailRecord rec : result.results()) {
                dtoList.add(new TestDetailDTO(new PCRTestDTO(rec.test()), new PersonDTO(rec.person())));
            }
            QueryResultDTO dto = new QueryResultDTO(dtoList, result.totalCount());
            return dto.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 18. Search for PCR test by code (without patient ID verification)
    public String searchPCRTestByCode(int testCode) {
        if (database == null) return "Error: No database open";
        try {
            DatabaseCore.TestDetailRecord result = database.searchPCRTestByCode(testCode);
            if (result == null) {
                return "Test not found";
            }
            PCRTestDTO testDTO = new PCRTestDTO(result.test());
            PersonDTO personDTO = new PersonDTO(result.person());
            TestDetailDTO detailDTO = new TestDetailDTO(testDTO, personDTO);
            return detailDTO.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 19. Insert a person
    public String insertPerson(PersonDTO personDTO) {
        if (database == null) return "Error: No database open";
        try {
            Person person = new Person();
            person.firstName = personDTO.firstName;
            person.lastName = personDTO.lastName;
            person.dateOfBirth = personDTO.dateOfBirth;
            person.patientId = personDTO.patientId;
            
            boolean success = database.insertPerson(person);
            return success ? null : "Failed to insert person (duplicate patient ID)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 20. Delete PCR test
    public String deletePCRTest(int testCode) {
        if (database == null) return "Error: No database open";
        try {
            boolean success = database.deletePCRTest(testCode);
            return success ? null : "Failed to delete PCR test (test not found)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // 21. Delete person and all their tests
    public String deletePerson(String patientId) {
        if (database == null) return "Error: No database open";
        try {
            boolean success = database.deletePerson(patientId);
            return success ? null : "Failed to delete person (person not found)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // ==================== Data Display ====================
    
    public String getAllPersonsAsString() {
        if (database == null) return "No database open";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("========== ALL PERSONS ==========\n\n");
            
            List<Person> allPersons = new ArrayList<>();
            database.getPersonMasterTree().inorderTraversal(person -> {
                allPersons.add(person);
                return true;
            });
            
            sb.append(String.format("Total Persons: %d\n", allPersons.size()));
            sb.append("=".repeat(100)).append("\n");
            
            for (int i = 0; i < allPersons.size(); i++) {
                Person p = allPersons.get(i);
                PersonDTO dto = new PersonDTO(p);
                sb.append(String.format("[%d] %s\n", i + 1, dto.toString()));
            }
            
            return sb.toString();
        } catch (Exception e) {
            return "Error reading persons: " + e.getMessage();
        }
    }
    
    public String getAllTestsAsString() {
        if (database == null) return "No database open";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("========== ALL PCR TESTS ==========\n\n");
            
            List<PCRTest> allTests = new ArrayList<>();
            database.getTestMasterTree().inorderTraversal(test -> {
                allTests.add(test);
                return true;
            });
            
            sb.append(String.format("Total Tests: %d\n", allTests.size()));
            sb.append("=".repeat(150)).append("\n");
            
            for (int i = 0; i < allTests.size(); i++) {
                PCRTest test = allTests.get(i);
                PCRTestDTO dto = new PCRTestDTO(test);
                sb.append(String.format("[%d] %s\n", i + 1, dto.toString()));
            }
            
            return sb.toString();
        } catch (Exception e) {
            return "Error reading tests: " + e.getMessage();
        }
    }
}
