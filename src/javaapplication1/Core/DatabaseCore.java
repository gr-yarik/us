package javaapplication1.Core;

import javaapplication1.AVLTree;
import UnsortedFile.BinaryFile;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class DatabaseCore {
    
    public record TestDetailRecord(PCRTest test, Person person) {}
    
    public record QueryResult(List<TestDetailRecord> results, int totalCount) {}
    
    public record SickPersonRecord(Person person, PCRTest causingTest) {}
    
    public record SickPersonQueryResult(List<SickPersonRecord> results, int totalCount) {}
    
    public record DistrictStatistic(int districtCode, int sickCount) {}
    
    public record RegionStatistic(int regionCode, int sickCount) {}

    private AVLTree<Person> personMasterTree;
    private AVLTree<PCRTest> testMasterTree;
    
    private AVLTree<PatientTimeKey> patientHistoryIndex;
    private AVLTree<DistrictTimeKey> districtAllIndex;
    private AVLTree<DistrictTimeKey> districtPositiveIndex;
    private AVLTree<RegionTimeKey> regionAllIndex;
    private AVLTree<RegionTimeKey> regionPositiveIndex;
    private AVLTree<GlobalTimeKey> globalAllIndex;
    private AVLTree<GlobalTimeKey> globalPositiveIndex;
    private AVLTree<WorkplaceTimeKey> workplaceIndex;
    private AVLTree<PatientTestCodeKey> patientTestCodeIndex;
    
    private BinaryFile personFile;
    private BinaryFile testFile;
    private String personFilePath;
    private String testFilePath;
    
    public DatabaseCore(String personFilePath, String testFilePath, boolean cleanStart) {
        this.personFilePath = personFilePath;
        this.testFilePath = testFilePath;
        
        this.personMasterTree = new AVLTree<>();
        this.testMasterTree = new AVLTree<>();
        this.patientHistoryIndex = new AVLTree<>();
        this.districtAllIndex = new AVLTree<>();
        this.districtPositiveIndex = new AVLTree<>();
        this.regionAllIndex = new AVLTree<>();
        this.regionPositiveIndex = new AVLTree<>();
        this.globalAllIndex = new AVLTree<>();
        this.globalPositiveIndex = new AVLTree<>();
        this.workplaceIndex = new AVLTree<>();
        this.patientTestCodeIndex = new AVLTree<>();
        
        if (cleanStart) {
            File personFileObj = new File(personFilePath);
            File testFileObj = new File(testFilePath);
            if (personFileObj.exists()) {
                personFileObj.delete();
            }
            if (testFileObj.exists()) {
                testFileObj.delete();
            }
        } else {
            loadFromFiles();
        }
    }
    
    private void loadFromFiles() {
        File personFileObj = new File(personFilePath);
        File testFileObj = new File(testFilePath);
        
        if (personFileObj.exists() && personFileObj.length() > 0) {
            try {
                personFile = new BinaryFile(personFilePath);
                long fileSize = personFile.getSize();
                personFile.seek(0);
                
                while (personFile.getSize() > 0) {
                    try {
                        byte[] allBytes = personFile.read((int)fileSize);
                        
                        int offset = 0;
                        while (offset < allBytes.length) {
                            Person person = new Person();
                            byte[] personBytes = extractPersonBytes(allBytes, offset);
                            if (personBytes.length == 0) break;
                            
                            person.FromByteArray(personBytes);
                            personMasterTree.insert(person);
                            offset += personBytes.length;
                        }
                        break;
                    } catch (Exception e) {
                        break;
                    }
                }
                personFile.close();
            } catch (Exception e) {
                System.err.println("Error loading persons from file: " + e.getMessage());
            }
        }
        
        if (testFileObj.exists() && testFileObj.length() > 0) {
            try {
                testFile = new BinaryFile(testFilePath);
                long fileSize = testFile.getSize();
                testFile.seek(0);
                
                while (testFile.getSize() > 0) {
                    try {
                        byte[] allBytes = testFile.read((int)fileSize);
                        
                        int offset = 0;
                        while (offset < allBytes.length) {
                            PCRTest test = new PCRTest();
                            byte[] testBytes = extractTestBytes(allBytes, offset);
                            if (testBytes.length == 0) break;
                            
                            test.FromByteArray(testBytes);
                            testMasterTree.insert(test);
                            
                            insertTestIntoIndexes(test);
                            
                            offset += testBytes.length;
                        }
                        break;
                    } catch (Exception e) {
                        break;
                    }
                }
                testFile.close();
            } catch (Exception e) {
                System.err.println("Error loading tests from file: " + e.getMessage());
            }
        }
    }
    
    private byte[] extractPersonBytes(byte[] allBytes, int offset) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(allBytes, offset, allBytes.length - offset);
            DataInputStream dis = new DataInputStream(bais);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            long birthdate = dis.readLong();
            dos.writeLong(birthdate);
            
            int firstNameLength = dis.readInt();
            dos.writeInt(firstNameLength);
            byte[] firstNameBytes = new byte[firstNameLength];
            dis.readFully(firstNameBytes);
            dos.write(firstNameBytes);
            
            int lastNameLength = dis.readInt();
            dos.writeInt(lastNameLength);
            byte[] lastNameBytes = new byte[lastNameLength];
            dis.readFully(lastNameBytes);
            dos.write(lastNameBytes);
            
            int patientIdLength = dis.readInt();
            dos.writeInt(patientIdLength);
            byte[] patientIdBytes = new byte[patientIdLength];
            dis.readFully(patientIdBytes);
            dos.write(patientIdBytes);
            
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    private byte[] extractTestBytes(byte[] allBytes, int offset) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(allBytes, offset, allBytes.length - offset);
            DataInputStream dis = new DataInputStream(bais);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            int testCode = dis.readInt();
            dos.writeInt(testCode);
            
            long timestamp = dis.readLong();
            dos.writeLong(timestamp);
            
            int workplaceCode = dis.readInt();
            dos.writeInt(workplaceCode);
            
            int districtCode = dis.readInt();
            dos.writeInt(districtCode);
            
            int regionCode = dis.readInt();
            dos.writeInt(regionCode);
            
            boolean testResult = dis.readBoolean();
            dos.writeBoolean(testResult);
            
            double testValue = dis.readDouble();
            dos.writeDouble(testValue);
            
            int patientIdLength = dis.readInt();
            dos.writeInt(patientIdLength);
            byte[] patientIdBytes = new byte[patientIdLength];
            dis.readFully(patientIdBytes);
            dos.write(patientIdBytes);
            
            int noteLength = dis.readInt();
            dos.writeInt(noteLength);
            byte[] noteBytes = new byte[noteLength];
            dis.readFully(noteBytes);
            dos.write(noteBytes);
            
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    public void closeDatabase() {
        try {
            ByteArrayOutputStream personStream = new ByteArrayOutputStream();
            personMasterTree.inorderTraversal(person -> {
                try {
                    byte[] personBytes = person.ToByteArray();
                    personStream.write(personBytes);
                    return true;
                } catch (Exception e) {
                    System.err.println("Error serializing person: " + e.getMessage());
                    return false;
                }
            });
            
            if (personStream.size() > 0) {
                personFile = new BinaryFile(personFilePath);
                personFile.seek(0);
                personFile.write(personStream.toByteArray());
                personFile.close();
            }
            
            ByteArrayOutputStream testStream = new ByteArrayOutputStream();
            testMasterTree.inorderTraversal(test -> {
                try {
                    byte[] testBytes = test.ToByteArray();
                    testStream.write(testBytes);
                    return true;
                } catch (Exception e) {
                    System.err.println("Error serializing test: " + e.getMessage());
                    return false;
                }
            });
            
            if (testStream.size() > 0) {
                testFile = new BinaryFile(testFilePath);
                testFile.seek(0);
                testFile.write(testStream.toByteArray());
                testFile.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
    
    private void insertTestIntoIndexes(PCRTest test) {
        Person searchPerson = new Person();
        searchPerson.patientId = test.patientId;
        Person person = personMasterTree.find(searchPerson);
        
        patientHistoryIndex.insert(new PatientTimeKey(test.patientId, test.timestamp, test.testCode, test));
        districtAllIndex.insert(new DistrictTimeKey(test.districtCode, test.timestamp, test.testCode, test));
        regionAllIndex.insert(new RegionTimeKey(test.regionCode, test.timestamp, test.testCode, test));
        globalAllIndex.insert(new GlobalTimeKey(test.timestamp, test.testCode, test));
        workplaceIndex.insert(new WorkplaceTimeKey(test.workplaceCode, test.timestamp, test.testCode, test));
        
        if (person != null) {
            patientTestCodeIndex.insert(new PatientTestCodeKey(test.patientId, test.testCode, test, person));
        }
        
        if (test.testResult) {
            districtPositiveIndex.insert(new DistrictTimeKey(test.districtCode, test.timestamp, test.testCode, test));
            regionPositiveIndex.insert(new RegionTimeKey(test.regionCode, test.timestamp, test.testCode, test));
            globalPositiveIndex.insert(new GlobalTimeKey(test.timestamp, test.testCode, test));
        }
    }
    
    private void deleteTestFromIndexes(PCRTest test) {
        patientHistoryIndex.delete(new PatientTimeKey(test.patientId, test.timestamp, test.testCode, null));
        districtAllIndex.delete(new DistrictTimeKey(test.districtCode, test.timestamp, test.testCode, null));
        regionAllIndex.delete(new RegionTimeKey(test.regionCode, test.timestamp, test.testCode, null));
        globalAllIndex.delete(new GlobalTimeKey(test.timestamp, test.testCode, null));
        workplaceIndex.delete(new WorkplaceTimeKey(test.workplaceCode, test.timestamp, test.testCode, null));
        
        patientTestCodeIndex.delete(new PatientTestCodeKey(test.patientId, test.testCode, null, null));
        
        if (test.testResult) {
            districtPositiveIndex.delete(new DistrictTimeKey(test.districtCode, test.timestamp, test.testCode, null));
            regionPositiveIndex.delete(new RegionTimeKey(test.regionCode, test.timestamp, test.testCode, null));
            globalPositiveIndex.delete(new GlobalTimeKey(test.timestamp, test.testCode, null));
        }
    }
    
    // ==================== Public Operations ====================
    
    // 1. Insert a PCR test result into the system.
    public boolean insertPCRTest(PCRTest test) {
        if (test == null) {
            return false;
        }
        
        Person searchPerson = new Person();
        searchPerson.patientId = test.patientId;
        Person person = personMasterTree.find(searchPerson);
        if (person == null) {
            return false;
        }
        
        testMasterTree.insert(test);
        
        insertTestIntoIndexes(test);
        
        return true;
    }
    
    // 2. Search for a test result (defined by the PCR test code) for a patient (defined by the unique patient number) and display all data.
    public TestDetailRecord searchTestByCode(int testCode, String patientId) {
        PatientTestCodeKey searchKey = new PatientTestCodeKey(patientId, testCode, null, null);
        PatientTestCodeKey result = patientTestCodeIndex.find(searchKey);
        
        if (result == null) {
            return null;
        }
        
        return new TestDetailRecord(result.getTest(), result.getPerson());
    }
    
    // 3. List all PCR tests performed for a given patient (defined by the unique patient number), sorted by the date and time of performance.
    public QueryResult listTestsForPatient(String patientId) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        PatientTimeKey rangeStart = new PatientTimeKey(patientId, Long.MIN_VALUE, Integer.MIN_VALUE, null);
        PatientTimeKey rangeEnd = new PatientTimeKey(patientId, Long.MAX_VALUE, Integer.MAX_VALUE, null);
        
        List<PatientTimeKey> keys = patientHistoryIndex.findInRange(rangeStart, rangeEnd);
        
        Person searchPerson = new Person();
        searchPerson.patientId = patientId;
        Person person = personMasterTree.find(searchPerson);
        
        for (PatientTimeKey key : keys) {
            results.add(new TestDetailRecord(key.getTest(), person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    // 4. List all positive tests performed within a specified time period for a given district (defined by the district code).
    public QueryResult listPositiveTestsInDistrictTimeRange(int districtCode, long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        DistrictTimeKey rangeStart = new DistrictTimeKey(districtCode, startTime, Integer.MIN_VALUE, null);
        DistrictTimeKey rangeEnd = new DistrictTimeKey(districtCode, endTime, Integer.MAX_VALUE, null);
        
        List<DistrictTimeKey> keys = districtPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        for (DistrictTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    public QueryResult listAllTestsInDistrictTimeRange(int districtCode, long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        DistrictTimeKey rangeStart = new DistrictTimeKey(districtCode, startTime, Integer.MIN_VALUE, null);
        DistrictTimeKey rangeEnd = new DistrictTimeKey(districtCode, endTime, Integer.MAX_VALUE, null);
        
        List<DistrictTimeKey> keys = districtAllIndex.findInRange(rangeStart, rangeEnd);
        
        for (DistrictTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    // 6. List all positive tests performed within a specified time period for a given region (defined by the region code).
    public QueryResult listPositiveTestsInRegionTimeRange(int regionCode, long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        RegionTimeKey rangeStart = new RegionTimeKey(regionCode, startTime, Integer.MIN_VALUE, null);
        RegionTimeKey rangeEnd = new RegionTimeKey(regionCode, endTime, Integer.MAX_VALUE, null);
        
        List<RegionTimeKey> keys = regionPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        for (RegionTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    public QueryResult listAllTestsInRegionTimeRange(int regionCode, long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        RegionTimeKey rangeStart = new RegionTimeKey(regionCode, startTime, Integer.MIN_VALUE, null);
        RegionTimeKey rangeEnd = new RegionTimeKey(regionCode, endTime, Integer.MAX_VALUE, null);
        
        List<RegionTimeKey> keys = regionAllIndex.findInRange(rangeStart, rangeEnd);
        
        for (RegionTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    // 8. List all positive tests performed within a specified time period.
    public QueryResult listAllPositiveTestsInTimeRange(long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(endTime, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    public QueryResult listAllTestsInTimeRange(long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(endTime, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalAllIndex.findInRange(rangeStart, rangeEnd);
        
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    // 10. List sick persons in a district (defined by the district code) as of a given date, where a person is considered sick for X days after a positive test (X is provided by the user).
    public SickPersonQueryResult listSickPersonsInDistrict(int districtCode, long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        DistrictTimeKey rangeStart = new DistrictTimeKey(districtCode, startTime, Integer.MIN_VALUE, null);
        DistrictTimeKey rangeEnd = new DistrictTimeKey(districtCode, asOfDate, Integer.MAX_VALUE, null);
        
        List<DistrictTimeKey> keys = districtPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<String, PCRTest> latestTestPerPerson = new HashMap<>();
        for (DistrictTimeKey key : keys) {
            PCRTest test = key.getTest();
            if (!latestTestPerPerson.containsKey(test.patientId) || 
                test.timestamp > latestTestPerPerson.get(test.patientId).timestamp) {
                latestTestPerPerson.put(test.patientId, test);
            }
        }
        
        List<SickPersonRecord> results = new ArrayList<>();
        for (PCRTest test : latestTestPerPerson.values()) {
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            if (person != null) {
                results.add(new SickPersonRecord(person, test));
            }
        }
        
        return new SickPersonQueryResult(results, results.size());
    }
    
    public SickPersonQueryResult listSickPersonsInDistrictSortedByTestValue(int districtCode, long asOfDate, int sicknessDurationDays) {
        SickPersonQueryResult unsorted = listSickPersonsInDistrict(districtCode, asOfDate, sicknessDurationDays);
        
        List<SickPersonRecord> sorted = unsorted.results().stream()
            .sorted((a, b) -> Double.compare(b.causingTest().testValue, a.causingTest().testValue))
            .collect(Collectors.toList());
        
        return new SickPersonQueryResult(sorted, sorted.size());
    }
    
    // 12. List sick persons in a region (defined by the region code) as of a given date, where a person is considered sick for X days after a positive test (X is provided by the user).
    public SickPersonQueryResult listSickPersonsInRegion(int regionCode, long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        RegionTimeKey rangeStart = new RegionTimeKey(regionCode, startTime, Integer.MIN_VALUE, null);
        RegionTimeKey rangeEnd = new RegionTimeKey(regionCode, asOfDate, Integer.MAX_VALUE, null);
        
        List<RegionTimeKey> keys = regionPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<String, PCRTest> latestTestPerPerson = new HashMap<>();
        for (RegionTimeKey key : keys) {
            PCRTest test = key.getTest();
            if (!latestTestPerPerson.containsKey(test.patientId) || 
                test.timestamp > latestTestPerPerson.get(test.patientId).timestamp) {
                latestTestPerPerson.put(test.patientId, test);
            }
        }
        
        List<SickPersonRecord> results = new ArrayList<>();
        for (PCRTest test : latestTestPerPerson.values()) {
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            if (person != null) {
                results.add(new SickPersonRecord(person, test));
            }
        }
        
        return new SickPersonQueryResult(results, results.size());
    }
    
    public SickPersonQueryResult listAllSickPersons(long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(asOfDate, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<String, PCRTest> latestTestPerPerson = new HashMap<>();
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            if (!latestTestPerPerson.containsKey(test.patientId) || 
                test.timestamp > latestTestPerPerson.get(test.patientId).timestamp) {
                latestTestPerPerson.put(test.patientId, test);
            }
        }
        
        List<SickPersonRecord> results = new ArrayList<>();
        for (PCRTest test : latestTestPerPerson.values()) {
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            if (person != null) {
                results.add(new SickPersonRecord(person, test));
            }
        }
        
        return new SickPersonQueryResult(results, results.size());
    }
    
    // 14. List one sick person as of a given date from each district, where a person is considered sick for X days after a positive test (X is provided by the user), specifically the person with the highest test value.
    public SickPersonQueryResult listSickestPersonPerDistrict(long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(asOfDate, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<Integer, SickPersonRecord> sickestPerDistrict = new HashMap<>();
        
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            
            if (!sickestPerDistrict.containsKey(test.districtCode) || 
                test.testValue > sickestPerDistrict.get(test.districtCode).causingTest().testValue) {
                
                Person searchPerson = new Person();
                searchPerson.patientId = test.patientId;
                Person person = personMasterTree.find(searchPerson);
                
                if (person != null) {
                    sickestPerDistrict.put(test.districtCode, new SickPersonRecord(person, test));
                }
            }
        }
        
        List<SickPersonRecord> results = new ArrayList<>(sickestPerDistrict.values());
        return new SickPersonQueryResult(results, results.size());
    }
    
    public List<DistrictStatistic> listDistrictsSortedBySickCount(long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(asOfDate, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<Integer, Map<String, PCRTest>> sickPerDistrict = new HashMap<>();
        
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            sickPerDistrict.putIfAbsent(test.districtCode, new HashMap<>());
            
            Map<String, PCRTest> patientsInDistrict = sickPerDistrict.get(test.districtCode);
            if (!patientsInDistrict.containsKey(test.patientId) || 
                test.timestamp > patientsInDistrict.get(test.patientId).timestamp) {
                patientsInDistrict.put(test.patientId, test);
            }
        }
        
        List<DistrictStatistic> results = sickPerDistrict.entrySet().stream()
            .map(entry -> new DistrictStatistic(entry.getKey(), entry.getValue().size()))
            .sorted((a, b) -> Integer.compare(b.sickCount(), a.sickCount()))
            .collect(Collectors.toList());
        
        return results;
    }
    
    // 16. List regions sorted by the number of sick persons as of a given date, where a person is considered sick for X days after a positive test (X is provided by the user).
    public List<RegionStatistic> listRegionsSortedBySickCount(long asOfDate, int sicknessDurationDays) {
        long sicknessDurationMillis = (long) sicknessDurationDays * 24 * 60 * 60 * 1000;
        long startTime = asOfDate - sicknessDurationMillis;
        
        GlobalTimeKey rangeStart = new GlobalTimeKey(startTime, Integer.MIN_VALUE, null);
        GlobalTimeKey rangeEnd = new GlobalTimeKey(asOfDate, Integer.MAX_VALUE, null);
        
        List<GlobalTimeKey> keys = globalPositiveIndex.findInRange(rangeStart, rangeEnd);
        
        Map<Integer, Map<String, PCRTest>> sickPerRegion = new HashMap<>();
        
        for (GlobalTimeKey key : keys) {
            PCRTest test = key.getTest();
            sickPerRegion.putIfAbsent(test.regionCode, new HashMap<>());
            
            Map<String, PCRTest> patientsInRegion = sickPerRegion.get(test.regionCode);
            if (!patientsInRegion.containsKey(test.patientId) || 
                test.timestamp > patientsInRegion.get(test.patientId).timestamp) {
                patientsInRegion.put(test.patientId, test);
            }
        }
        
        List<RegionStatistic> results = sickPerRegion.entrySet().stream()
            .map(entry -> new RegionStatistic(entry.getKey(), entry.getValue().size()))
            .sorted((a, b) -> Integer.compare(b.sickCount(), a.sickCount()))
            .collect(Collectors.toList());
        
        return results;
    }
    
    // 17. List all tests performed within a specified time period at a given workplace (defined by the workplace code).
    public QueryResult listAllTestsAtWorkplaceInTimeRange(int workplaceCode, long startTime, long endTime) {
        List<TestDetailRecord> results = new ArrayList<>();
        
        WorkplaceTimeKey rangeStart = new WorkplaceTimeKey(workplaceCode, startTime, Integer.MIN_VALUE, null);
        WorkplaceTimeKey rangeEnd = new WorkplaceTimeKey(workplaceCode, endTime, Integer.MAX_VALUE, null);
        
        List<WorkplaceTimeKey> keys = workplaceIndex.findInRange(rangeStart, rangeEnd);
        
        for (WorkplaceTimeKey key : keys) {
            PCRTest test = key.getTest();
            Person searchPerson = new Person();
            searchPerson.patientId = test.patientId;
            Person person = personMasterTree.find(searchPerson);
            results.add(new TestDetailRecord(test, person));
        }
        
        return new QueryResult(results, results.size());
    }
    
    public TestDetailRecord searchPCRTestByCode(int testCode) {
        PCRTest searchTest = new PCRTest();
        searchTest.testCode = testCode;
        PCRTest test = testMasterTree.find(searchTest);
        
        if (test == null) {
            return null;
        }
        
        Person searchPerson = new Person();
        searchPerson.patientId = test.patientId;
        Person person = personMasterTree.find(searchPerson);
        
        return new TestDetailRecord(test, person);
    }
    
    // 19. Insert a person into the system.
    public boolean insertPerson(Person person) {
        if (person == null) {
            return false;
        }
        
        personMasterTree.insert(person);
        return true;
    }
    
    public boolean deletePCRTest(int testCode) {
        PCRTest searchTest = new PCRTest();
        searchTest.testCode = testCode;
        PCRTest test = testMasterTree.find(searchTest);
        
        if (test == null) {
            return false;
        }
        
        testMasterTree.delete(test);
        
        deleteTestFromIndexes(test);
        
        return true;
    }
    
    // 21. Delete a person from the system (defined by the unique patient number), including all of their PCR test results.
    public boolean deletePerson(String patientId) {
        Person searchPerson = new Person();
        searchPerson.patientId = patientId;
        Person person = personMasterTree.find(searchPerson);
        
        if (person == null) {
            return false;
        }
        
        PatientTimeKey rangeStart = new PatientTimeKey(patientId, Long.MIN_VALUE, Integer.MIN_VALUE, null);
        PatientTimeKey rangeEnd = new PatientTimeKey(patientId, Long.MAX_VALUE, Integer.MAX_VALUE, null);
        
        List<PatientTimeKey> keys = patientHistoryIndex.findInRange(rangeStart, rangeEnd);
        
        for (PatientTimeKey key : keys) {
            PCRTest test = key.getTest();
            testMasterTree.delete(test);
            deleteTestFromIndexes(test);
        }
        
        personMasterTree.delete(person);
        
        return true;
    }
    
    public AVLTree<Person> getPersonMasterTree() {
        return personMasterTree;
    }
    
    public AVLTree<PCRTest> getTestMasterTree() {
        return testMasterTree;
    }
}