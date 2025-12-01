package UnsortedFile;

import java.io.File;
import java.util.*;

public class AutoTester {
    
    private static final String HEAP_FILE = "autotest";
    private static final String METADATA_FILE = "autotest.meta";
    private static final int BLOCK_SIZE = 512;
    private static final int TOTAL_PERSONS = 10000;
    private static final int INSERT_COUNT = 300;
    
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> failures = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   HEAP AUTO TESTER");
        System.out.println("========================================\n");
        
        try {
            // Clean up any existing test files
            cleanupTestFiles();
            
            // Run all tests in sequence, passing data between them
            test1_InstantiateHeap();
            
            Person[] persons = test2_GeneratePersons();
            
            Map<Integer, List<Person>> blockMap = test3_Insert300Persons(persons);
            
            Heap<Person> heap = test4_ReinstantiateHeap();
            if (heap == null) {
                throw new RuntimeException("Cannot continue tests - heap initialization failed");
            }
            
            test5_VerifyAllRecords(heap, blockMap);
            
            test6_DeleteRecords(heap, blockMap);
            
            test7_AddPersonAndVerifyBlock2(heap);
            
            test8_RandomOperations(heap, persons, blockMap);
            
            heap.close();
            
            // Print summary
            printSummary();
            
        } catch (Exception e) {
            System.err.println("\n✗ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
            printSummary();
        }
    }
    
    private static void cleanupTestFiles() {
        System.out.println("Cleaning up test files...");
        new File(HEAP_FILE).delete();
        new File(METADATA_FILE).delete();
        System.out.println("✓ Cleanup complete\n");
    }
    
    // Test 1: Instantiate Heap
    private static void test1_InstantiateHeap() {
        System.out.println("Test 1: Instantiating Heap with file 'autotest', Person class, blockSize 512");
        try {
            Heap<Person> heap = new Heap<>(HEAP_FILE, BLOCK_SIZE, Person.class);
            heap.close();
            pass("Test 1 passed: Heap instantiated successfully");
        } catch (Exception e) {
            fail("Test 1 failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    // Test 2: Generate 10,000 Person objects
    private static Person[] test2_GeneratePersons() {
        System.out.println("Test 2: Generating " + TOTAL_PERSONS + " Person objects");
        Person[] persons = new Person[TOTAL_PERSONS];
        
        String[] firstNames = {
            "Alex", "Bob", "Charlie", "David", "Emma", "Frank", "Grace", "Henry",
            "Ivy", "Jack", "Kate", "Liam", "Mia", "Noah", "Olivia", "Paul",
            "Quinn", "Rachel", "Sam", "Tina", "Uma", "Victor", "Wendy", "Xavier",
            "Yara", "Zoe", "Adam", "Bella", "Chris", "Diana"
        };
        
        String[] lastNames = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Wilson", "Anderson", "Thomas", "Taylor",
            "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris", "Sanchez",
            "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young"
        };
        
        for (int i = 0; i < TOTAL_PERSONS; i++) {
            Person person = new Person();
            person.name = firstNames[i % firstNames.length];
            person.surname = lastNames[i % lastNames.length];
            int year = 1950 + (i % 51);
            int month = 1 + (i % 12);
            int day = 1 + (i % 28);
            person.birthdate = Long.parseLong(String.format("%04d%02d%02d", year, month, day));
            person.id = "ID" + String.format("%08d", 10000000 + i);
            persons[i] = person;
        }
        
        pass("Test 2 passed: Generated " + TOTAL_PERSONS + " Person objects");
        System.out.println();
        return persons;
    }
    
    // Test 3: Insert 300 persons and track block numbers
    private static Map<Integer, List<Person>> test3_Insert300Persons(Person[] persons) {
        System.out.println("Test 3: Inserting " + INSERT_COUNT + " persons and tracking block numbers");
        Map<Integer, List<Person>> blockMap = new HashMap<>();
        
        try {
            Heap<Person> heap = new Heap<>(HEAP_FILE, BLOCK_SIZE, Person.class);
            
            for (int i = 0; i < INSERT_COUNT; i++) {
                Person person = persons[i];
                int blockNumber = heap.insert(person);
                
                blockMap.putIfAbsent(blockNumber, new ArrayList<>());
                blockMap.get(blockNumber).add(person);
            }
            
            heap.close();
            pass("Test 3 passed: Inserted " + INSERT_COUNT + " persons");
            System.out.println("  Blocks used: " + blockMap.size());
        } catch (Exception e) {
            fail("Test 3 failed: " + e.getMessage());
        }
        System.out.println();
        return blockMap;
    }
    
    // Test 4: Heap goes out of scope and reinstantiate
    private static Heap<Person> test4_ReinstantiateHeap() {
        System.out.println("Test 4: Reinstantiating Heap from existing files");
        try {
            // Heap from previous test goes out of scope here
            Heap<Person> heap = new Heap<>(HEAP_FILE, METADATA_FILE, Person.class);
            pass("Test 4 passed: Heap reinstantiated successfully");
            System.out.println();
            return heap;
        } catch (Exception e) {
            fail("Test 4 failed: " + e.getMessage());
            System.out.println();
            return null;
        }
    }
    
    // Test 5: Verify all records
    private static void test5_VerifyAllRecords(Heap<Person> heap, Map<Integer, List<Person>> blockMap) {
        System.out.println("Test 5: Verifying all records against helper structure");
        int verified = 0;
        int failed = 0;
        
        try {
            for (Map.Entry<Integer, List<Person>> entry : blockMap.entrySet()) {
                int blockNumber = entry.getKey();
                List<Person> expectedPersons = entry.getValue();
                
                for (Person expectedPerson : expectedPersons) {
                    Person foundPerson = heap.get(blockNumber, expectedPerson);
                    
                    if (foundPerson == null) {
                        failed++;
                        fail("Test 5: Person with ID " + expectedPerson.id + " not found in block " + blockNumber);
                    } else if (!personsEqual(expectedPerson, foundPerson)) {
                        failed++;
                        fail("Test 5: Person with ID " + expectedPerson.id + " data mismatch in block " + blockNumber);
                    } else {
                        verified++;
                    }
                }
            }
            
            if (failed == 0) {
                pass("Test 5 passed: All " + verified + " records verified correctly");
            } else {
                fail("Test 5: " + failed + " records failed verification out of " + (verified + failed));
            }
        } catch (Exception e) {
            fail("Test 5 failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    // Test 6: Delete records from blocks 5, 6, 7 and 3 records from block 2
    private static void test6_DeleteRecords(Heap<Person> heap, Map<Integer, List<Person>> blockMap) {
        System.out.println("Test 6: Deleting records from blocks 5, 6, 7 and 3 records from block 2");
        int deleted = 0;
        int failed = 0;
        
        try {
            // Delete all records from blocks 5, 6, 7
            for (int blockNum : new int[]{5, 6, 7}) {
                if (blockMap.containsKey(blockNum)) {
                    List<Person> personsToDelete = new ArrayList<>(blockMap.get(blockNum));
                    for (Person person : personsToDelete) {
                        if (heap.delete(blockNum, person)) {
                            blockMap.get(blockNum).remove(person);
                            deleted++;
                        } else {
                            failed++;
                            fail("Test 6: Failed to delete person " + person.id + " from block " + blockNum);
                        }
                    }
                    if (blockMap.get(blockNum).isEmpty()) {
                        blockMap.remove(blockNum);
                    }
                }
            }
            
            // Delete 3 records from block 2
            if (blockMap.containsKey(2) && blockMap.get(2).size() >= 3) {
                List<Person> personsToDelete = blockMap.get(2).subList(0, 3);
                List<Person> copy = new ArrayList<>(personsToDelete);
                for (Person person : copy) {
                    if (heap.delete(2, person)) {
                        blockMap.get(2).remove(person);
                        deleted++;
                    } else {
                        failed++;
                        fail("Test 6: Failed to delete person " + person.id + " from block 2");
                    }
                }
                if (blockMap.get(2).isEmpty()) {
                    blockMap.remove(2);
                }
            }
            
            if (failed == 0) {
                pass("Test 6 passed: Deleted " + deleted + " records");
            } else {
                fail("Test 6: " + failed + " deletions failed out of " + (deleted + failed) + " attempts");
            }
        } catch (Exception e) {
            fail("Test 6 failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    // Test 7: Add one new person and verify it returns block 2
    private static void test7_AddPersonAndVerifyBlock2(Heap<Person> heap) {
        System.out.println("Test 7: Adding one new person and verifying it returns block 2");
        try {
            Person newPerson = new Person();
            newPerson.name = "TestName";
            newPerson.surname = "TestSurname";
            newPerson.birthdate = 20000101L;
            newPerson.id = "TEST001";
            
            int blockNumber = heap.insert(newPerson);
            
            if (blockNumber == 2) {
                pass("Test 7 passed: New person inserted at block 2");
            } else {
                fail("Test 7 failed: Expected block 2, got block " + blockNumber);
            }
        } catch (Exception e) {
            fail("Test 7 failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    // Test 8: Random operations (10,000 operations: 40% insert, 30% delete, 30% get)
    private static void test8_RandomOperations(Heap<Person> heap, Person[] allPersons, Map<Integer, List<Person>> blockMap) {
        System.out.println("Test 8: Running 10,000 random operations (40% insert, 30% delete, 30% get)");
        Random random = new Random(42); // Fixed seed for reproducibility
        int operations = 10000;
        int insertOps = 0;
        int deleteOps = 0;
        int getOps = 0;
        int insertSuccess = 0;
        int deleteSuccess = 0;
        int getSuccess = 0;
        int failures = 0;
        
        // Track all inserted persons for random operations
        List<Person> insertedPersons = new ArrayList<>();
        Map<Integer, List<Person>> operationBlockMap = new HashMap<>(blockMap);
        
        try {
            for (int i = 0; i < operations; i++) {
                double rand = random.nextDouble();
                
                if (rand < 0.4) {
                    // INSERT (40%)
                    insertOps++;
                    Person person = allPersons[INSERT_COUNT + insertedPersons.size()];
                    if (INSERT_COUNT + insertedPersons.size() < TOTAL_PERSONS) {
                        try {
                            int blockNumber = heap.insert(person);
                            insertedPersons.add(person);
                            operationBlockMap.putIfAbsent(blockNumber, new ArrayList<>());
                            operationBlockMap.get(blockNumber).add(person);
                            
                            // Verify insertion immediately
                            Person found = heap.get(blockNumber, person);
                            if (found != null && personsEqual(person, found)) {
                                insertSuccess++;
                            } else {
                                failures++;
                                if (failures <= 10) {
                                    fail("Test 8: Insert verification failed for person " + person.id);
                                }
                            }
                        } catch (Exception e) {
                            failures++;
                            if (failures <= 10) {
                                fail("Test 8: Insert failed: " + e.getMessage());
                            }
                        }
                    }
                } else if (rand < 0.7) {
                    // DELETE (30%)
                    deleteOps++;
                    if (!insertedPersons.isEmpty() || !operationBlockMap.isEmpty()) {
                        // Try to delete from inserted persons first
                        if (!insertedPersons.isEmpty() && random.nextDouble() < 0.5) {
                            Person personToDelete = insertedPersons.remove(random.nextInt(insertedPersons.size()));
                            // Find which block it's in
                            for (Map.Entry<Integer, List<Person>> entry : operationBlockMap.entrySet()) {
                                if (entry.getValue().remove(personToDelete)) {
                                    int blockNumber = entry.getKey();
                                    if (heap.delete(blockNumber, personToDelete)) {
                                        deleteSuccess++;
                                        if (entry.getValue().isEmpty()) {
                                            operationBlockMap.remove(blockNumber);
                                        }
                                    } else {
                                        failures++;
                                        if (failures <= 10) {
                                            fail("Test 8: Delete failed for person " + personToDelete.id);
                                        }
                                    }
                                    break;
                                }
                            }
                        } else if (!operationBlockMap.isEmpty()) {
                            // Delete from existing blocks
                            List<Integer> blocks = new ArrayList<>(operationBlockMap.keySet());
                            int blockNumber = blocks.get(random.nextInt(blocks.size()));
                            List<Person> personsInBlock = operationBlockMap.get(blockNumber);
                            if (!personsInBlock.isEmpty()) {
                                Person personToDelete = personsInBlock.remove(random.nextInt(personsInBlock.size()));
                                if (heap.delete(blockNumber, personToDelete)) {
                                    deleteSuccess++;
                                    if (personsInBlock.isEmpty()) {
                                        operationBlockMap.remove(blockNumber);
                                    }
                                } else {
                                    failures++;
                                    if (failures <= 10) {
                                        fail("Test 8: Delete failed for person " + personToDelete.id);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // GET (30%)
                    getOps++;
                    if (!operationBlockMap.isEmpty()) {
                        List<Integer> blocks = new ArrayList<>(operationBlockMap.keySet());
                        int blockNumber = blocks.get(random.nextInt(blocks.size()));
                        List<Person> personsInBlock = operationBlockMap.get(blockNumber);
                        if (!personsInBlock.isEmpty()) {
                            Person personToGet = personsInBlock.get(random.nextInt(personsInBlock.size()));
                            Person found = heap.get(blockNumber, personToGet);
                            if (found != null && personsEqual(personToGet, found)) {
                                getSuccess++;
                            } else {
                                failures++;
                                if (failures <= 10) {
                                    fail("Test 8: Get failed for person " + personToGet.id);
                                }
                            }
                        }
                    }
                }
                
                if ((i + 1) % 1000 == 0) {
                    System.out.println("  Completed " + (i + 1) + " operations...");
                }
            }
            
            System.out.println("  Operations breakdown:");
            System.out.println("    Insert: " + insertOps + " (successful: " + insertSuccess + ")");
            System.out.println("    Delete: " + deleteOps + " (successful: " + deleteSuccess + ")");
            System.out.println("    Get: " + getOps + " (successful: " + getSuccess + ")");
            
            if (failures == 0) {
                pass("Test 8 passed: All " + operations + " operations completed successfully");
            } else {
                fail("Test 8: " + failures + " operations failed out of " + operations);
            }
        } catch (Exception e) {
            fail("Test 8 failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }
    
    // Helper method to compare two Person objects
    private static boolean personsEqual(Person p1, Person p2) {
        if (p1 == null || p2 == null) {
            return p1 == p2;
        }
        return Objects.equals(p1.id, p2.id) &&
               Objects.equals(p1.name, p2.name) &&
               Objects.equals(p1.surname, p2.surname) &&
               p1.birthdate == p2.birthdate;
    }
    
    private static void pass(String message) {
        System.out.println("✓ " + message);
        testsPassed++;
    }
    
    private static void fail(String message) {
        System.out.println("✗ " + message);
        testsFailed++;
        failures.add(message);
    }
    
    private static void printSummary() {
        System.out.println("========================================");
        System.out.println("   TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Tests Passed: " + testsPassed);
        System.out.println("Tests Failed: " + testsFailed);
        System.out.println("Total Tests: " + (testsPassed + testsFailed));
        
        if (testsFailed > 0) {
            System.out.println("\nFailures:");
            for (int i = 0; i < Math.min(failures.size(), 20); i++) {
                System.out.println("  " + (i + 1) + ". " + failures.get(i));
            }
            if (failures.size() > 20) {
                System.out.println("  ... and " + (failures.size() - 20) + " more failures");
            }
        }
        
        System.out.println("========================================");
        
        if (testsFailed == 0) {
            System.out.println("✓ ALL TESTS PASSED!");
        } else {
            System.out.println("✗ SOME TESTS FAILED!");
        }
        System.out.println("========================================\n");
    }
}

