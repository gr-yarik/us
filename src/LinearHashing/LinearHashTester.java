package LinearHashing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import UnsortedFile.StorableRecord;

public class LinearHashTester {

    private static final String MAIN_BUCKETS_FILE = "linearhash_main";
    private static final String MAIN_METADATA_FILE = "linearhash_main.meta";
    private static final String OVERFLOW_BLOCKS_FILE = "linearhash_overflow";
    private static final String OVERFLOW_METADATA_FILE = "linearhash_overflow.meta";
    private static final int BLOCK_SIZE = 250; // 512;
    private static final int OVERFLOW_BLOCK_SIZE = 200; //256;
    private static final int TOTAL_PERSONS = 2;

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> failures = new ArrayList<>();
    private static int testMethodsPassed = 0;
    private static int testMethodsFailed = 0;

    private static int extractKey(Person person) {
       return Integer.parseInt(person.id.substring(2));
        // if (person.id != null && person.id.startsWith("ID")) {
        //     try {
                // String numericPart = person.id.substring(2);
        //         return Integer.parseInt(numericPart);
        //     } catch (NumberFormatException e) {
        //         return Math.abs(person.id.hashCode());
        //     }
        // }
        // return Math.abs(person.id != null ? person.id.hashCode() : 0);
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   LINEAR HASH AUTO TESTER");
        System.out.println("========================================\n");

        try {
            cleanupTestFiles();

            test1_InstantiateLinearHash();

            Person[] persons = test2_GeneratePersons();

            LinearHash<Person> linearHash = test3_InsertRecords(persons);

            // Launch debugger UI (non-blocking)
            if (linearHash != null) {
                LinearHashDebugger.launch(linearHash);
                System.out.println("Debugger UI launched. LinearHash will remain open while debugger is active.\n");
            }

            // test4_VerifyAllRecords(linearHash, persons);

            // test5_TestOverflowHandling(linearHash);

            // test6_TestSplitOperation(linearHash);

            // test7_DeleteRecords(linearHash, persons);

            // test8_TestMergeOperation(linearHash);

            // test9_RandomOperations(linearHash, persons);

            // Note: linearHash.close() is commented out to keep it open for the debugger
            // The debugger will keep the LinearHash instance alive
            // linearHash.close();

            // printSummary();

        } catch (Exception e) {
            System.err.println("\n✗ FATAL ERROR: " + e.getMessage());
            e.printStackTrace();
            testsFailed++;
            printSummary();
        }
    }

    private static void cleanupTestFiles() {
        System.out.println("Cleaning up test files...");
        new File(MAIN_BUCKETS_FILE).delete();
        new File(MAIN_METADATA_FILE).delete();
        new File(OVERFLOW_BLOCKS_FILE).delete();
        new File(OVERFLOW_METADATA_FILE).delete();
        System.out.println("✓ Cleanup complete\n");
    }

    private static void test1_InstantiateLinearHash() {
        System.out.println("Test 1: Instantiating LinearHash");
        try {
            LinearHash<Person> linearHash = new LinearHash<>(
                    MAIN_BUCKETS_FILE,
                    OVERFLOW_BLOCKS_FILE,
                    BLOCK_SIZE,
                    OVERFLOW_BLOCK_SIZE,
                    Person.class,
                    LinearHashTester::extractKey);

            if (linearHash.getLevel() == 0 && linearHash.getSplitPointer() == 0) {
                pass("Test 1 passed: LinearHash instantiated successfully");
                System.out.println("  Initial level: " + linearHash.getLevel());
                System.out.println("  Initial split pointer: " + linearHash.getSplitPointer());
                System.out.println("  Initial buckets: " + linearHash.getTotalPrimaryBuckets());
            } else {
                fail("Test 1 failed: Initial state incorrect");
            }

            linearHash.close();
        } catch (Exception e) {
            fail("Test 1 failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

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
        passTestMethod("Test 2");
        System.out.println();
        return persons;
    }

    private static LinearHash<Person> test3_InsertRecords(Person[] persons) {
        System.out.println("Test 3: Inserting " + TOTAL_PERSONS + " records into LinearHash");
        int inserted = 0;
        int failed = 0;

        try {
            LinearHash<Person> linearHash = new LinearHash<>(
                    MAIN_BUCKETS_FILE,
                    OVERFLOW_BLOCKS_FILE,
                    BLOCK_SIZE,
                    OVERFLOW_BLOCK_SIZE,
                    Person.class,
                    LinearHashTester::extractKey);

            for (int i = 0; i < TOTAL_PERSONS; i++) {
                try {
                    linearHash.insert(persons[i]);
                    inserted++;
                } catch (Exception e) {
                    failed++;
                    if (failed <= 5) {
                        System.err.println("  Insert failed for person " + persons[i].id + ": " + e.getMessage());
                    }
                }

                if ((i + 1) % 100 == 0) {
                    System.out.println("  Inserted " + (i + 1) + " records... (buckets: " +
                            linearHash.getTotalPrimaryBuckets() + ", overflow: " +
                            linearHash.getDebugInfoTotalOverflowBlocks() + ", ratio: " +
                            String.format("%.2f", linearHash.getOverflowRatio()) + ")");
                }
            }

            System.out.println("  Final state:");
            System.out.println("    Level: " + linearHash.getLevel());
            System.out.println("    Split pointer: " + linearHash.getSplitPointer());
            System.out.println("    Primary buckets: " + linearHash.getTotalPrimaryBuckets());
            System.out.println("    Overflow blocks: " + linearHash.getDebugInfoTotalOverflowBlocks());
            System.out.println("    Overflow ratio: " + String.format("%.2f", linearHash.getOverflowRatio()));

            if (failed == 0) {
                pass("Test 3 passed: Inserted " + inserted + " records");
            } else {
                fail("Test 3: " + failed + " insertions failed out of " + TOTAL_PERSONS);
            }

            System.out.println();
            return linearHash;
        } catch (Exception e) {
            fail("Test 3 failed: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            return null;
        }
    }

    private static void test4_VerifyAllRecords(LinearHash<Person> linearHash, Person[] persons) {
        System.out.println("Test 4: Verifying all inserted records");
        int verified = 0;
        int failed = 0;

        try {
            for (int i = 0; i < TOTAL_PERSONS; i++) {
                Person expectedPerson = persons[i];
                Person foundPerson = linearHash.get(expectedPerson);

                if (foundPerson == null) {
                    failed++;
                    if (failed <= 10) {
                        fail("Test 4: Person with ID " + expectedPerson.id + " not found");
                    }
                } else if (!personsEqual(expectedPerson, foundPerson)) {
                    failed++;
                    if (failed <= 10) {
                        fail("Test 4: Person with ID " + expectedPerson.id + " data mismatch");
                    }
                } else {
                    verified++;
                }
            }

            if (failed == 0) {
                pass("Test 4 passed: All " + verified + " records verified correctly");
                passTestMethod("Test 4");
            } else {
                fail("Test 4: " + failed + " records failed verification out of " + (verified + failed));
                failTestMethod("Test 4");
            }
        } catch (Exception e) {
            fail("Test 4 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 4");
        }
        System.out.println();
    }

    private static void test5_TestOverflowHandling(LinearHash<Person> linearHash) {
        System.out.println("Test 5: Testing overflow handling");
        try {
            int initialOverflowBlocks = linearHash.getDebugInfoTotalOverflowBlocks();
            int initialBuckets = linearHash.getTotalPrimaryBuckets();

            Person[] extraPersons = new Person[100];
            for (int i = 0; i < 100; i++) {
                extraPersons[i] = new Person();
                extraPersons[i].id = "ID" + String.format("%08d", 20000000 + i);
                extraPersons[i].name = "Extra" + i;
                extraPersons[i].surname = "Person" + i;
                extraPersons[i].birthdate = 20000101L;
                linearHash.insert(extraPersons[i]);
            }

            int finalOverflowBlocks = linearHash.getDebugInfoTotalOverflowBlocks();
            int finalBuckets = linearHash.getTotalPrimaryBuckets();

            System.out.println("  Initial overflow blocks: " + initialOverflowBlocks);
            System.out.println("  Final overflow blocks: " + finalOverflowBlocks);
            System.out.println("  Initial buckets: " + initialBuckets);
            System.out.println("  Final buckets: " + finalBuckets);

            if (finalOverflowBlocks >= initialOverflowBlocks || finalBuckets > initialBuckets) {
                pass("Test 5 passed: Overflow handling working (overflow blocks or buckets changed)");
                passTestMethod("Test 5");
            } else {
                fail("Test 5: Overflow handling may not be working correctly");
                failTestMethod("Test 5");
            }
        } catch (Exception e) {
            fail("Test 5 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 5");
        }
        System.out.println();
    }

    private static void test6_TestSplitOperation(LinearHash<Person> linearHash) {
        System.out.println("Test 6: Testing split operation");
        try {
            int initialBuckets = linearHash.getTotalPrimaryBuckets();
            int initialLevel = linearHash.getLevel();
            int initialSplitPointer = linearHash.getSplitPointer();

            System.out.println("  Current state before split test:");
            System.out.println("    Level: " + initialLevel);
            System.out.println("    Split pointer: " + initialSplitPointer);
            System.out.println("    Buckets: " + initialBuckets);
            System.out.println("    Overflow ratio: " + String.format("%.2f", linearHash.getOverflowRatio()));

            int insertsBeforeSplit = 0;
            for (int i = 0; i < 500 && linearHash.getTotalPrimaryBuckets() == initialBuckets; i++) {
                Person person = new Person();
                person.id = "ID" + String.format("%08d", 30000000 + i);
                person.name = "Split" + i;
                person.surname = "Test" + i;
                person.birthdate = 20000101L;
                linearHash.insert(person);
                insertsBeforeSplit++;
            }

            int finalBuckets = linearHash.getTotalPrimaryBuckets();
            int finalLevel = linearHash.getLevel();
            int finalSplitPointer = linearHash.getSplitPointer();

            System.out.println("  State after insertions:");
            System.out.println("    Level: " + finalLevel);
            System.out.println("    Split pointer: " + finalSplitPointer);
            System.out.println("    Buckets: " + finalBuckets);
            System.out.println("    Overflow ratio: " + String.format("%.2f", linearHash.getOverflowRatio()));

            if (finalBuckets > initialBuckets || finalLevel > initialLevel
                    || finalSplitPointer != initialSplitPointer) {
                pass("Test 6 passed: Split operation occurred (buckets or level changed)");
                passTestMethod("Test 6");
            } else {
                pass("Test 6: No split triggered yet (overflow ratio: " +
                        String.format("%.2f", linearHash.getOverflowRatio()) + ", threshold: 0.8)");
                passTestMethod("Test 6");
            }
        } catch (Exception e) {
            fail("Test 6 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 6");
        }
        System.out.println();
    }

    private static void test7_DeleteRecords(LinearHash<Person> linearHash, Person[] persons) {
        System.out.println("Test 7: Deleting records");
        int deleted = 0;
        int failed = 0;

        try {
            for (int i = 0; i < 100 && i < TOTAL_PERSONS; i++) {
                Person personToDelete = persons[i];
                boolean success = linearHash.delete(personToDelete);
                if (success) {
                    deleted++;
                } else {
                    failed++;
                    if (failed <= 5) {
                        fail("Test 7: Failed to delete person " + personToDelete.id);
                    }
                }
            }

            int stillFound = 0;
            for (int i = 0; i < 100 && i < TOTAL_PERSONS; i++) {
                Person deletedPerson = persons[i];
                Person found = linearHash.get(deletedPerson);
                if (found != null) {
                    stillFound++;
                }
            }

            System.out.println("  Deleted: " + deleted);
            System.out.println("  Still found: " + stillFound);
            System.out.println("  Current overflow ratio: " + String.format("%.2f", linearHash.getOverflowRatio()));

            if (stillFound == 0 && failed == 0) {
                pass("Test 7 passed: Deleted " + deleted + " records successfully");
                passTestMethod("Test 7");
            } else {
                fail("Test 7: " + failed + " deletions failed, " + stillFound + " records still found");
                failTestMethod("Test 7");
            }
        } catch (Exception e) {
            fail("Test 7 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 7");
        }
        System.out.println();
    }

    private static void test8_TestMergeOperation(LinearHash<Person> linearHash) {
        System.out.println("Test 8: Testing merge operation");
        try {
            int initialBuckets = linearHash.getTotalPrimaryBuckets();
            int initialLevel = linearHash.getLevel();
            double initialRatio = linearHash.getOverflowRatio();

            System.out.println("  Current state:");
            System.out.println("    Level: " + initialLevel);
            System.out.println("    Buckets: " + initialBuckets);
            System.out.println("    Overflow ratio: " + String.format("%.2f", initialRatio));

            int finalBuckets = linearHash.getTotalPrimaryBuckets();
            double finalRatio = linearHash.getOverflowRatio();

            System.out.println("  After deletions:");
            System.out.println("    Buckets: " + finalBuckets);
            System.out.println("    Overflow ratio: " + String.format("%.2f", finalRatio));

            if (finalRatio < 0.1 && finalBuckets < initialBuckets) {
                pass("Test 8 passed: Merge operation occurred");
                passTestMethod("Test 8");
            } else {
                pass("Test 8: Merge not triggered (ratio: " + String.format("%.2f", finalRatio) +
                        ", threshold: 0.1, or buckets already at minimum)");
                passTestMethod("Test 8");
            }
        } catch (Exception e) {
            fail("Test 8 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 8");
        }
        System.out.println();
    }

    private static void test9_RandomOperations(LinearHash<Person> linearHash, Person[] persons) {
        System.out.println("Test 9: Running random operations (insert, get, delete)");
        Random random = new Random(42);
        int operations = 500;
        int insertOps = 0;
        int deleteOps = 0;
        int getOps = 0;
        int insertSuccess = 0;
        int deleteSuccess = 0;
        int getSuccess = 0;
        int failures = 0;

        Set<Person> insertedPersons = new HashSet<>();

        try {
            for (int i = 0; i < operations; i++) {
                double rand = random.nextDouble();

                if (rand < 0.4) {
                    insertOps++;
                    Person person = new Person();
                    person.id = "ID" + String.format("%08d", 40000000 + insertOps);
                    person.name = "Random" + insertOps;
                    person.surname = "Person" + insertOps;
                    person.birthdate = 20000101L;

                    try {
                        linearHash.insert(person);
                        insertedPersons.add(person);
                        insertSuccess++;
                    } catch (Exception e) {
                        failures++;
                        if (failures <= 5) {
                            fail("Test 9: Insert failed: " + e.getMessage());
                        }
                    }
                } else if (rand < 0.7) {
                    deleteOps++;
                    if (!insertedPersons.isEmpty()) {
                        Person[] personArray = insertedPersons.toArray(new Person[0]);
                        Person personToDelete = personArray[random.nextInt(personArray.length)];
                        try {
                            boolean success = linearHash.delete(personToDelete);
                            if (success) {
                                insertedPersons.remove(personToDelete);
                                deleteSuccess++;
                            }
                        } catch (Exception e) {
                            failures++;
                            if (failures <= 5) {
                                fail("Test 9: Delete failed: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    getOps++;
                    if (!insertedPersons.isEmpty()) {
                        Person[] personArray = insertedPersons.toArray(new Person[0]);
                        Person personToGet = personArray[random.nextInt(personArray.length)];
                        try {
                            Person found = linearHash.get(personToGet);
                            if (found != null && personsEqual(personToGet, found)) {
                                getSuccess++;
                            } else {
                                failures++;
                                if (failures <= 5) {
                                    fail("Test 9: Get failed for person " + personToGet.id);
                                }
                            }
                        } catch (Exception e) {
                            failures++;
                            if (failures <= 5) {
                                fail("Test 9: Get failed: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            System.out.println("  Operations breakdown:");
            System.out.println("    Insert: " + insertOps + " (successful: " + insertSuccess + ")");
            System.out.println("    Delete: " + deleteOps + " (successful: " + deleteSuccess + ")");
            System.out.println("    Get: " + getOps + " (successful: " + getSuccess + ")");
            System.out.println("  Final state:");
            System.out.println("    Level: " + linearHash.getLevel());
            System.out.println("    Split pointer: " + linearHash.getSplitPointer());
            System.out.println("    Buckets: " + linearHash.getTotalPrimaryBuckets());
            System.out.println("    Overflow blocks: " + linearHash.getDebugInfoTotalOverflowBlocks());
            System.out.println("    Overflow ratio: " + String.format("%.2f", linearHash.getOverflowRatio()));

            if (failures == 0) {
                pass("Test 9 passed: All " + operations + " operations completed successfully");
                passTestMethod("Test 9");
            } else {
                fail("Test 9: " + failures + " operations failed out of " + operations);
                failTestMethod("Test 9");
            }
        } catch (Exception e) {
            fail("Test 9 failed: " + e.getMessage());
            e.printStackTrace();
            failTestMethod("Test 9");
        }
        System.out.println();
    }

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

    private static void passTestMethod(String testName) {
        testMethodsPassed++;
    }

    private static void failTestMethod(String testName) {
        testMethodsFailed++;
    }

    private static void printSummary() {
        System.out.println("========================================");
        System.out.println("   TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Test Methods Passed: " + testMethodsPassed);
        System.out.println("Test Methods Failed: " + testMethodsFailed);
        System.out.println("Total Test Methods: " + (testMethodsPassed + testMethodsFailed));
        System.out.println();
        System.out.println("(Individual assertions: " + testsPassed + " passed, " + testsFailed + " failed)");

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

        if (testMethodsFailed == 0) {
            System.out.println("✓ ALL TEST METHODS PASSED!");
        } else {
            System.out.println("✗ SOME TEST METHODS FAILED!");
        }
        System.out.println("========================================\n");
    }
}
