package javaapplication1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Tester {
    
    // Test configuration constants
    private static final int INSERTION_TEST_SIZE = 30_000;
    private static final int DELETION_TEST_COUNT = 5_000;
    private static final int SEARCH_TEST_COUNT = 10_000;
    private static final int INTERVAL_SEARCH_TEST_COUNT = 10_000;
    private static final int MIN_MAX_TEST_OPERATIONS = 20_000;
    private static final int INCREASING_SEQUENCE_SIZE = 10_000;
    private static final int RANDOMIZED_OPERATIONS_COUNT = 25_000;
    private static final int INTERVAL_SEARCH_MIN_ELEMENTS = 50;
    
    // Randomized test probabilities (Test 7)
    private static final double RANDOMIZED_TEST1_INSERT_PROB = 0.6;
    private static final double RANDOMIZED_TEST1_DELETE_PROB = 0.2;
    private static final double RANDOMIZED_TEST1_SEARCH_PROB = 0.2;
    private static final double RANDOMIZED_TEST1_EXISTING_SEARCH_PROB = 0.5;
    
    // Randomized test probabilities (Test 8)
    private static final double RANDOMIZED_TEST2_INSERT_PROB = 0.2;
    private static final double RANDOMIZED_TEST2_DELETE_PROB = 0.6;
    private static final double RANDOMIZED_TEST2_SEARCH_PROB = 0.2;
    private static final double RANDOMIZED_TEST2_EXISTING_SEARCH_PROB = 0.5;
    
    // Balance factor check frequency (check after every Nth operation)
    private static final int BALANCE_CHECK_FREQUENCY = 2;
    
    // Test result tracking
    private static class TestResults {
        List<String> failures = new ArrayList<>();
        int totalTests = 0;
        int passedTests = 0;
        
        void addFailure(String testName, String message) {
            failures.add("[" + testName + "] " + message);
        }
        
        void recordTest(boolean passed) {
            totalTests++;
            if (passed) passedTests++;
        }
        
        void printSummary() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("TEST SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("Total Tests: " + totalTests);
            System.out.println("Passed: " + passedTests);
            System.out.println("Failed: " + (totalTests - passedTests));
            
            if (!failures.isEmpty()) {
                System.out.println("\nFAILURES:");
                for (int i = 0; i < failures.size(); i++) {
                    System.out.println((i + 1) + ". " + failures.get(i));
                }
            } else {
                System.out.println("\n✓ ALL TESTS PASSED!");
            }
            System.out.println("=".repeat(60));
        }
    }
    
    // Performance tracking for TreeMap comparisons
    private static class PerformanceMetrics {
        long avlInsertTime = 0;
        long treeMapInsertTime = 0;
        long avlDeleteTime = 0;
        long treeMapDeleteTime = 0;
        long avlSearchTime = 0;
        long treeMapSearchTime = 0;
        long avlRangeSearchTime = 0;
        long treeMapRangeSearchTime = 0;
        int insertCount = 0;
        int deleteCount = 0;
        int searchCount = 0;
        int rangeSearchCount = 0;
        
        void printPerformanceComparison() {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PERFORMANCE COMPARISON: AVL Tree vs TreeMap");
            System.out.println("=".repeat(60));
            
            if (insertCount > 0) {
                double avlAvgInsert = (double) avlInsertTime / insertCount;
                double treeMapAvgInsert = (double) treeMapInsertTime / insertCount;
                double diffPercent = calculatePercentageDifference(avlAvgInsert, treeMapAvgInsert);
                System.out.println("INSERT:");
                System.out.println("  AVL Tree:    " + avlInsertTime + " ns total (" + String.format("%.2f", avlAvgInsert) + " ns/op)");
                System.out.println("  TreeMap:    " + treeMapInsertTime + " ns total (" + String.format("%.2f", treeMapAvgInsert) + " ns/op)");
                printComparison("AVL Tree", "TreeMap", diffPercent);
            }
            
            if (deleteCount > 0) {
                double avlAvgDelete = (double) avlDeleteTime / deleteCount;
                double treeMapAvgDelete = (double) treeMapDeleteTime / deleteCount;
                double diffPercent = calculatePercentageDifference(avlAvgDelete, treeMapAvgDelete);
                System.out.println("DELETE:");
                System.out.println("  AVL Tree:    " + avlDeleteTime + " ns total (" + String.format("%.2f", avlAvgDelete) + " ns/op)");
                System.out.println("  TreeMap:    " + treeMapDeleteTime + " ns total (" + String.format("%.2f", treeMapAvgDelete) + " ns/op)");
                printComparison("AVL Tree", "TreeMap", diffPercent);
            }
            
            if (searchCount > 0) {
                double avlAvgSearch = (double) avlSearchTime / searchCount;
                double treeMapAvgSearch = (double) treeMapSearchTime / searchCount;
                double diffPercent = calculatePercentageDifference(avlAvgSearch, treeMapAvgSearch);
                System.out.println("SEARCH:");
                System.out.println("  AVL Tree:    " + avlSearchTime + " ns total (" + String.format("%.2f", avlAvgSearch) + " ns/op)");
                System.out.println("  TreeMap:    " + treeMapSearchTime + " ns total (" + String.format("%.2f", treeMapAvgSearch) + " ns/op)");
                printComparison("AVL Tree", "TreeMap", diffPercent);
            }
            
            if (rangeSearchCount > 0) {
                double avlAvgRangeSearch = (double) avlRangeSearchTime / rangeSearchCount;
                double treeMapAvgRangeSearch = (double) treeMapRangeSearchTime / rangeSearchCount;
                double diffPercent = calculatePercentageDifference(avlAvgRangeSearch, treeMapAvgRangeSearch);
                System.out.println("RANGE SEARCH:");
                System.out.println("  AVL Tree:    " + avlRangeSearchTime + " ns total (" + String.format("%.2f", avlAvgRangeSearch) + " ns/op)");
                System.out.println("  TreeMap:    " + treeMapRangeSearchTime + " ns total (" + String.format("%.2f", treeMapAvgRangeSearch) + " ns/op)");
                printComparison("AVL Tree", "TreeMap", diffPercent);
            }
            
            System.out.println("=".repeat(60));
        }
        
        private double calculatePercentageDifference(double time1, double time2) {
            if (time1 == 0 && time2 == 0) return 0;
            if (time1 == 0) return 100; // time1 is infinitely faster
            if (time2 == 0) return -100; // time2 is infinitely faster
            return ((time2 - time1) / time1) * 100;
        }
        
        private void printComparison(String name1, String name2, double diffPercent) {
            if (Math.abs(diffPercent) < 0.01) {
                System.out.println("  → Performance is nearly identical");
            } else if (diffPercent > 0) {
                System.out.println("  → " + name1 + " is " + String.format("%.2f", Math.abs(diffPercent)) + "% faster than " + name2);
            } else {
                System.out.println("  → " + name2 + " is " + String.format("%.2f", Math.abs(diffPercent)) + "% faster than " + name1);
            }
        }
    }
    
    static class IntegerData implements TreeNodeData {
        private int value;
        
        public IntegerData(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        @Override
        public int compare(TreeNodeData other) {
            return Integer.compare(this.value, ((IntegerData) other).value);
        }
        
        public String toString() {
            return String.valueOf(value);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            IntegerData that = (IntegerData) obj;
            return value == that.value;
        }
        
        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }
    
    // Helper method to get in-order traversal as a list
    private static List<IntegerData> getInOrderTraversal(BSTree<IntegerData> tree) {
        List<IntegerData> result = new ArrayList<>();
        tree.inorderTraversal(data -> {
            result.add(data);
            return true;
        });
        return result;
    }
    
    // Helper method to get in-order traversal from TreeMap
    private static List<Integer> getInOrderFromTreeMap(TreeMap<Integer, IntegerData> treeMap) {
        List<Integer> result = new ArrayList<>();
        for (Integer key : treeMap.keySet()) {
            result.add(key);
        }
        return result;
    }
    
    // Helper method to verify AVL balance factors
    private static boolean verifyAVLBalanceFactors(AVLTree<IntegerData> tree, TestResults results, String testName, String operation) {
        if (!tree.verifyAllBalanceFactors()) {
             results.addFailure(testName, "AVL balance factor violation detected after " + operation);
            
            throw new Error("AVL balance factor violation detected");
           
        }
        return true;
    }
    
    // Test 1: Insertion Test
    public static List<IntegerData> test1_insertionTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, Random random, TestResults results) {
        String testName = "Test 1: Insertion Test";
        System.out.println("\n===" + testName + "===");
        
        int dataSize = INSERTION_TEST_SIZE;
        Set<Integer> uniqueValues = new HashSet<>();
        List<IntegerData> helperStructure = new ArrayList<>(dataSize);
        
        System.out.println("Generating " + dataSize + " unique random integers...");
        while (helperStructure.size() < dataSize) {
            int value = Math.abs(random.nextInt() % (INSERTION_TEST_SIZE * 20));
            if (uniqueValues.add(value)) {
                IntegerData data = new IntegerData(value);
                helperStructure.add(data);
            }
        }
        
        System.out.println("Inserting into AVL tree and BST...");
        int insertionFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < helperStructure.size(); i++) {
            IntegerData data = helperStructure.get(i);
            avlTree.insert(data);
            bstTree.insert(data);
            
            // Verify insertion immediately by finding the element
            IntegerData avlFound = avlTree.find(data);
            IntegerData bstFound = bstTree.find(data);
            
            if (avlFound == null || avlFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in AVL tree after insertion");
            }
            
            if (bstFound == null || bstFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in BST after insertion");
            }
            
            // Check balance factors every Nth insertion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "insertion " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after insertion " + (i + 1));
                }
            }
        }
        
        if (insertionFailures > 0) {
            results.addFailure(testName, insertionFailures + " insertion verification failures");
        }
        
        // Verify in-order traversal matches sorted helper structure
        System.out.println("Verifying in-order traversal matches sorted helper structure...");
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<IntegerData> bstInOrder = getInOrderTraversal(bstTree);
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        boolean avlMatch = avlInOrder.size() == helperStructure.size();
        boolean bstMatch = bstInOrder.size() == helperStructure.size();
        
        if (avlMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (avlInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    avlMatch = false;
                    results.addFailure(testName, "AVL in-order traversal mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "AVL size mismatch: expected " + helperStructure.size() + ", got " + avlInOrder.size());
        }
        
        if (bstMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (bstInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    bstMatch = false;
                    results.addFailure(testName, "BST in-order traversal mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "BST size mismatch: expected " + helperStructure.size() + ", got " + bstInOrder.size());
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all insertions")) {
            throw new Error("AVL balance factor violation detected after all insertions");
        }
        
        boolean passed = avlMatch && bstMatch && insertionFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED");
        }
        
        // Return unsorted helper structure for use in subsequent tests
        Collections.shuffle(helperStructure, random);
        return helperStructure;
    }
    
    // Test 2: Deletion Test
    public static void test2_deletionTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, List<IntegerData> helperStructure, Random random, TestResults results) {
        String testName = "Test 2: Deletion Test";
        System.out.println("\n===" + testName + "===");
        
        int deleteCount = DELETION_TEST_COUNT;
        Collections.shuffle(helperStructure, random);
        
        List<IntegerData> toDelete = new ArrayList<>(helperStructure.subList(0, Math.min(deleteCount, helperStructure.size())));
        Set<Integer> deletedValues = new HashSet<>();
        for (IntegerData data : toDelete) {
            deletedValues.add(data.getValue());
        }
        
        System.out.println("Deleting " + toDelete.size() + " elements...");
        int deletionFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < toDelete.size(); i++) {
            IntegerData data = toDelete.get(i);
            avlTree.delete(data);
            bstTree.delete(data);
            helperStructure.remove(data);
            
            // Verify deletion immediately by checking element is gone
            IntegerData avlFound = avlTree.find(data);
            IntegerData bstFound = bstTree.find(data);
            
            if (avlFound != null) {
                throw new Error("Element " + data.getValue() + " still found in AVL tree after deletion");
            }
            
            if (bstFound != null) {
                throw new Error("Element " + data.getValue() + " still found in BST after deletion");
            }
            
            // Check balance factors every Nth deletion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "deletion " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after deletion " + (i + 1));
                }
            }
        }
        
        if (deletionFailures > 0) {
            results.addFailure(testName, deletionFailures + " deletion verification failures");
        }
        
        // Verify in-order traversal matches sorted helper structure after deletions
        System.out.println("Verifying in-order traversal matches sorted helper structure after deletions...");
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<IntegerData> bstInOrder = getInOrderTraversal(bstTree);
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        boolean avlMatch = avlInOrder.size() == helperStructure.size();
        boolean bstMatch = bstInOrder.size() == helperStructure.size();
        
        if (avlMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (avlInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    avlMatch = false;
                    results.addFailure(testName, "AVL in-order traversal mismatch after deletions at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "AVL size mismatch after deletions: expected " + helperStructure.size() + ", got " + avlInOrder.size());
        }
        
        if (bstMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (bstInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    bstMatch = false;
                    results.addFailure(testName, "BST in-order traversal mismatch after deletions at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "BST size mismatch after deletions: expected " + helperStructure.size() + ", got " + bstInOrder.size());
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all deletions")) {
            throw new Error("AVL balance factor violation detected after all deletions");
        }
        
        boolean passed = avlMatch && bstMatch && deletionFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED");
        }
    }
    
    // Test 3: Search Test
    public static void test3_searchTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, List<IntegerData> helperStructure, Random random, TestResults results) {
        String testName = "Test 3: Search Test";
        System.out.println("\n===" + testName + "===");

        if (helperStructure.isEmpty()) {
            System.out.println("Skipping: no elements to search (helper structure is empty).");
            results.recordTest(true);
            return;
        }
        
        int searchCount = SEARCH_TEST_COUNT;
        Set<Integer> helperSet = new HashSet<>();
        for (IntegerData data : helperStructure) {
            helperSet.add(data.getValue());
        }
        
        System.out.println("Performing " + searchCount + " searches...");
        int searchFailures = 0;
        
        for (int i = 0; i < searchCount; i++) {
            int randomIndex = random.nextInt(helperStructure.size());
            IntegerData searchKey = helperStructure.get(randomIndex);
            
            IntegerData avlResult = avlTree.find(searchKey);
            IntegerData bstResult = bstTree.find(searchKey);
            
            boolean inHelper = helperSet.contains(searchKey.getValue());
            boolean avlFound = avlResult != null && avlResult.getValue() == searchKey.getValue();
            boolean bstFound = bstResult != null && bstResult.getValue() == searchKey.getValue();
            
            if (inHelper && !avlFound) {
                throw new Error("Element " + searchKey.getValue() + " found in helper but missing in AVL tree");
            }
            
            if (inHelper && !bstFound) {
                throw new Error("Element " + searchKey.getValue() + " found in helper but missing in BST");
            }
        }
        
        // Test that non-existent elements return null
        System.out.println("Testing searches for non-existent elements...");
        for (int i = 0; i < 100; i++) {
            int nonExistentValue = Integer.MAX_VALUE - i;
            IntegerData searchKey = new IntegerData(nonExistentValue);
            
            if (!helperSet.contains(nonExistentValue)) {
                IntegerData avlResult = avlTree.find(searchKey);
                IntegerData bstResult = bstTree.find(searchKey);
                
                if (avlResult != null) {
                    throw new Error("Non-existent element " + nonExistentValue + " found in AVL tree (should be null)");
                }
                
                if (bstResult != null) {
                    throw new Error("Non-existent element " + nonExistentValue + " found in BST (should be null)");
                }
            }
        }
        
        boolean passed = searchFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + searchFailures + " search failures");
        }
    }
    
    // Test 4: Range (Interval) Search Test
    public static void test4_rangeSearchTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, List<IntegerData> helperStructure, Random random, TestResults results) {
        String testName = "Test 4: Range (Interval) Search Test";
        System.out.println("\n===" + testName + "===");
        
        int intervalSearchCount = INTERVAL_SEARCH_TEST_COUNT;
        Map<Integer, Boolean> recordedElements = new HashMap<>();
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        System.out.println("Performing " + intervalSearchCount + " interval searches...");
        int rangeSearchFailures = 0;
        
        for (int i = 0; i < intervalSearchCount; i++) {
            if (helperStructure.size() < 2) break;
            
            int indexA = random.nextInt(helperStructure.size());
            int indexB = random.nextInt(helperStructure.size());
            
            if (indexA > indexB) {
                int temp = indexA;
                indexA = indexB;
                indexB = temp;
            }
            
            IntegerData a = helperStructure.get(indexA);
            IntegerData b = helperStructure.get(indexB);
            
            // Generate expected list
            List<Integer> expected = new ArrayList<>();
            for (IntegerData data : helperStructure) {
                int value = data.getValue();
                if (value >= a.getValue() && value <= b.getValue()) {
                    expected.add(value);
                    recordedElements.put(value, true);
                }
            }
            Collections.sort(expected);
            
            // Query both trees
            List<IntegerData> avlResults = avlTree.findInRange(a, b);
            List<IntegerData> bstResults = bstTree.findInRange(a, b);
            
            // Convert to integer lists for comparison
            List<Integer> avlValues = new ArrayList<>();
            for (IntegerData data : avlResults) {
                avlValues.add(data.getValue());
            }
            Collections.sort(avlValues);
            
            List<Integer> bstValues = new ArrayList<>();
            for (IntegerData data : bstResults) {
                bstValues.add(data.getValue());
            }
            Collections.sort(bstValues);
            
            // Verify correctness
            if (avlValues.size() != expected.size()) {
                throw new Error("AVL range search size mismatch: expected " + expected.size() + ", got " + avlValues.size() + " for range [" + a.getValue() + ", " + b.getValue() + "]");
            }
            
            if (bstValues.size() != expected.size()) {
                throw new Error("BST range search size mismatch: expected " + expected.size() + ", got " + bstValues.size() + " for range [" + a.getValue() + ", " + b.getValue() + "]");
            }
            
            if (avlValues.size() == expected.size()) {
                for (int j = 0; j < expected.size(); j++) {
                    if (!avlValues.get(j).equals(expected.get(j))) {
                        throw new Error("AVL range search content mismatch at position " + j + " for range [" + a.getValue() + ", " + b.getValue() + "]");
                    }
                }
            }
            
            if (bstValues.size() == expected.size()) {
                for (int j = 0; j < expected.size(); j++) {
                    if (!bstValues.get(j).equals(expected.get(j))) {
                        throw new Error("BST range search content mismatch at position " + j + " for range [" + a.getValue() + ", " + b.getValue() + "]");
                    }
                }
            }
            
            if (rangeSearchFailures > 0) break;
        }
        
        // Continue if HashMap has fewer than minimum required elements
        while (recordedElements.size() < INTERVAL_SEARCH_MIN_ELEMENTS && helperStructure.size() >= 2) {
            int indexA = random.nextInt(helperStructure.size());
            int indexB = random.nextInt(helperStructure.size());
            
            if (indexA > indexB) {
                int temp = indexA;
                indexA = indexB;
                indexB = temp;
            }
            
            IntegerData a = helperStructure.get(indexA);
            IntegerData b = helperStructure.get(indexB);
            
            List<IntegerData> avlResults = avlTree.findInRange(a, b);
            for (IntegerData data : avlResults) {
                recordedElements.put(data.getValue(), true);
            }
        }
        
        boolean passed = rangeSearchFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED. Recorded " + recordedElements.size() + " unique elements");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + rangeSearchFailures + " range search failures");
        }
    }
    
    // Test 5: Minimum and Maximum Key Test
    public static void test5_minMaxTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, List<IntegerData> helperStructure, TestResults results) {
        String testName = "Test 5: Minimum and Maximum Key Test";
        System.out.println("\n===" + testName + "===");
        
        int operations = MIN_MAX_TEST_OPERATIONS;
        int minMaxFailures = 0;
        
        for (int i = 0; i < operations; i++) {
            if (helperStructure.isEmpty()) break;
            
            // Get expected min and max from helper structure
            int expectedMin = helperStructure.stream().mapToInt(IntegerData::getValue).min().orElse(0);
            int expectedMax = helperStructure.stream().mapToInt(IntegerData::getValue).max().orElse(0);
            
            // Get from trees
            IntegerData avlMin = avlTree.findMin();
            IntegerData avlMax = avlTree.findMax();
            IntegerData bstMin = bstTree.findMin();
            IntegerData bstMax = bstTree.findMax();
            
            int avlMinValue = avlMin != null ? avlMin.getValue() : 0;
            int avlMaxValue = avlMax != null ? avlMax.getValue() : 0;
            int bstMinValue = bstMin != null ? bstMin.getValue() : 0;
            int bstMaxValue = bstMax != null ? bstMax.getValue() : 0;
            
            if (avlMinValue != expectedMin) {
                throw new Error("AVL min mismatch: expected " + expectedMin + ", got " + avlMinValue);
            }
            
            if (avlMaxValue != expectedMax) {
                throw new Error("AVL max mismatch: expected " + expectedMax + ", got " + avlMaxValue);
            }
            
            if (bstMinValue != expectedMin) {
                throw new Error("BST min mismatch: expected " + expectedMin + ", got " + bstMinValue);
            }
            
            if (bstMaxValue != expectedMax) {
                throw new Error("BST max mismatch: expected " + expectedMax + ", got " + bstMaxValue);
            }
            
            if (minMaxFailures > 0) break;
        }
        
        boolean passed = minMaxFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + minMaxFailures + " min/max failures");
        }
    }
    
    // Test 6: Increasing Sequence Insertion Test
    public static void test6_increasingSequenceTest(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, TestResults results) {
        String testName = "Test 6: Increasing Sequence Insertion Test";
        System.out.println("\n===" + testName + "===");
        
        int sequenceSize = INCREASING_SEQUENCE_SIZE;
        List<IntegerData> increasingSequence = new ArrayList<>(sequenceSize);
        
        System.out.println("Generating strictly increasing sequence of " + sequenceSize + " integers...");
        for (int i = 1; i <= sequenceSize; i++) {
            increasingSequence.add(new IntegerData(i));
        }
        
        System.out.println("Inserting into both trees...");
        int insertionFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < increasingSequence.size(); i++) {
            IntegerData data = increasingSequence.get(i);
            avlTree.insert(data);
            bstTree.insert(data);
            
            // Verify each insertion
            IntegerData avlFound = avlTree.find(data);
            IntegerData bstFound = bstTree.find(data);
            
            if (avlFound == null || avlFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in AVL tree after insertion");
            }
            
            if (bstFound == null || bstFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in BST after insertion");
            }
            
            // Check balance factors every Nth insertion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "insertion " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after insertion " + (i + 1));
                }
            }
        }
        
        int avlHeight = avlTree.getHeight();
        int bstHeight = bstTree.getHeight();
        
        System.out.println("AVL Tree height: " + avlHeight);
        System.out.println("BST height: " + bstHeight);
        
        // Verify in-order traversal
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<IntegerData> bstInOrder = getInOrderTraversal(bstTree);
        
        boolean avlCorrect = avlInOrder.size() == increasingSequence.size();
        boolean bstCorrect = bstInOrder.size() == increasingSequence.size();
        
        if (avlCorrect) {
            for (int i = 0; i < increasingSequence.size(); i++) {
                if (avlInOrder.get(i).getValue() != increasingSequence.get(i).getValue()) {
                    avlCorrect = false;
                    results.addFailure(testName, "AVL in-order traversal mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "AVL size mismatch: expected " + increasingSequence.size() + ", got " + avlInOrder.size());
        }
        
        if (bstCorrect) {
            for (int i = 0; i < increasingSequence.size(); i++) {
                if (bstInOrder.get(i).getValue() != increasingSequence.get(i).getValue()) {
                    bstCorrect = false;
                    results.addFailure(testName, "BST in-order traversal mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "BST size mismatch: expected " + increasingSequence.size() + ", got " + bstInOrder.size());
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all insertions")) {
            throw new Error("AVL balance factor violation detected after all insertions");
        }
        
        boolean passed = avlCorrect && bstCorrect && insertionFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED");
        }
    }
    
    // Test 7: Randomized Operation Probability Test 1
    public static void test7_randomizedOperationsTest1(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, Random random, TestResults results) {
        String testName = "Test 7: Randomized Operations Test 1";
        System.out.println("\n===" + testName + " (" + 
                         (int)(RANDOMIZED_TEST1_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST1_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST1_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        int operationFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < totalOperations; i++) {
            double operation = random.nextDouble();
            
            if (operation < RANDOMIZED_TEST1_INSERT_PROB) {
                // Insertion
                int value = random.nextInt();
                IntegerData data = new IntegerData(value);
                if (!helperSet.contains(value)) {
                    avlTree.insert(data);
                    bstTree.insert(data);
                    helperStructure.add(data);
                    helperSet.add(value);
                    
                    // Verify insertion
                    IntegerData avlFound = avlTree.find(data);
                    if (avlFound == null || avlFound.getValue() != value) {
                        throw new Error("Insertion verification failed for value " + value);
                    }
                }
            } else if (operation < RANDOMIZED_TEST1_INSERT_PROB + RANDOMIZED_TEST1_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    bstTree.delete(data);
                    
                    // Verify deletion
                    IntegerData avlFound = avlTree.find(data);
                    if (avlFound != null) {
                        throw new Error("Deletion verification failed for value " + data.getValue());
                    }
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST1_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        IntegerData avlResult = avlTree.find(data);
                        IntegerData bstResult = bstTree.find(data);
                        
                        if (avlResult == null || avlResult.getValue() != data.getValue()) {
                            throw new Error("Search failed for existing element " + data.getValue());
                        }
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    IntegerData avlResult = avlTree.find(data);
                    IntegerData bstResult = bstTree.find(data);
                    
                    if (avlResult != null) {
                        throw new Error("Non-existent element " + value + " found in AVL tree");
                    }
                }
            }
            
            // Check balance factors every Nth operation
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "operation " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after operation " + (i + 1));
                }
            }
        }
        
        // Final verification: check all elements in helper structure exist in trees
        System.out.println("Verifying final state...");
        for (IntegerData data : helperStructure) {
            IntegerData avlFound = avlTree.find(data);
            if (avlFound == null || avlFound.getValue() != data.getValue()) {
                throw new Error("Final state verification failed: element " + data.getValue() + " missing");
            }
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all operations")) {
            throw new Error("AVL balance factor violation detected after all operations");
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        
        boolean passed = operationFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + operationFailures + " operation failures, " + balanceFactorFailures + " balance factor failures");
        }
    }
    
    // Test 8: Randomized Operation Probability Test 2
    public static void test8_randomizedOperationsTest2(AVLTree<IntegerData> avlTree, BSTree<IntegerData> bstTree, Random random, TestResults results) {
        String testName = "Test 8: Randomized Operations Test 2";
        System.out.println("\n===" + testName + " (" + 
                         (int)(RANDOMIZED_TEST2_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST2_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST2_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        int operationFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < totalOperations; i++) {
            double operation = random.nextDouble();
            
            if (operation < RANDOMIZED_TEST2_INSERT_PROB) {
                // Insertion
                int value = random.nextInt();
                IntegerData data = new IntegerData(value);
                if (!helperSet.contains(value)) {
                    avlTree.insert(data);
                    bstTree.insert(data);
                    helperStructure.add(data);
                    helperSet.add(value);
                    
                    // Verify insertion
                    IntegerData avlFound = avlTree.find(data);
                    if (avlFound == null || avlFound.getValue() != value) {
                        throw new Error("Insertion verification failed for value " + value);
                    }
                }
            } else if (operation < RANDOMIZED_TEST2_INSERT_PROB + RANDOMIZED_TEST2_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    bstTree.delete(data);
                    
                    // Verify deletion
                    IntegerData avlFound = avlTree.find(data);
                    if (avlFound != null) {
                        throw new Error("Deletion failed");
                        // operationFailures++;
                        // if (operationFailures == 1) {
                        //     results.addFailure(testName, "Deletion verification failed for value " + data.getValue());
                        // }
                    }
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST2_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        IntegerData avlResult = avlTree.find(data);
                        IntegerData bstResult = bstTree.find(data);
                        
                        if (avlResult == null || avlResult.getValue() != data.getValue()) {
                            throw new Error("Search failed");
                            // operationFailures++;
                            // if (operationFailures == 1) {
                            //     results.addFailure(testName, "Search failed for existing element " + data.getValue());
                            // }
                        }
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    IntegerData avlResult = avlTree.find(data);
                    IntegerData bstResult = bstTree.find(data);
                    
                    if (avlResult != null) {
                        throw new Error("Non-existent element " + value + " found in AVL tree");
                    }
                }
            }
            
            // Check balance factors every Nth operation
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "operation " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after operation " + (i + 1));
                }
            }
        }
        
        // Final verification: check all elements in helper structure exist in trees
        System.out.println("Verifying final state...");
        for (IntegerData data : helperStructure) {
            IntegerData avlFound = avlTree.find(data);
            if (avlFound == null || avlFound.getValue() != data.getValue()) {
                throw new Error("Final state verification failed: element " + data.getValue() + " missing");
            }
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all operations")) {
            throw new Error("AVL balance factor violation detected after all operations");
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        
        boolean passed = operationFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + operationFailures + " operation failures, " + balanceFactorFailures + " balance factor failures");
        }
    }
    
    // ========== GROUP 2: AVL Tree vs TreeMap (with Performance Measurement) ==========
    
    // Test 1 (TreeMap): Insertion Test with Timing
    public static List<IntegerData> test1_insertionTest_TreeMap(AVLTree<IntegerData> avlTree, TreeMap<Integer, IntegerData> treeMap, Random random, TestResults results, PerformanceMetrics perf) {
        String testName = "Test 1 (TreeMap): Insertion Test";
        System.out.println("\n===" + testName + "===");
        
        int dataSize = INSERTION_TEST_SIZE;
        Set<Integer> uniqueValues = new HashSet<>();
        List<IntegerData> helperStructure = new ArrayList<>(dataSize);
        
        System.out.println("Generating " + dataSize + " unique random integers...");
        while (helperStructure.size() < dataSize) {
            int value = random.nextInt();
            if (uniqueValues.add(value)) {
                IntegerData data = new IntegerData(value);
                helperStructure.add(data);
            }
        }
        
        System.out.println("Inserting into AVL tree and TreeMap (with timing)...");
        int insertionFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < helperStructure.size(); i++) {
            IntegerData data = helperStructure.get(i);
            
            // Time AVL insertion
            long startTime = System.nanoTime();
            avlTree.insert(data);
            long avlTime = System.nanoTime() - startTime;
            perf.avlInsertTime += avlTime;
            perf.insertCount++;
            
            // Time TreeMap insertion
            startTime = System.nanoTime();
            treeMap.put(data.getValue(), data);
            long treeMapTime = System.nanoTime() - startTime;
            perf.treeMapInsertTime += treeMapTime;
            
            // Verify insertion
            IntegerData avlFound = avlTree.find(data);
            IntegerData treeMapFound = treeMap.get(data.getValue());
            
            if (avlFound == null || avlFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in AVL tree after insertion");
            }
            
            if (treeMapFound == null || treeMapFound.getValue() != data.getValue()) {
                throw new Error("Element " + data.getValue() + " not found in TreeMap after insertion");
            }
            
            // Check balance factors every Nth insertion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "insertion " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after insertion " + (i + 1));
                }
            }
        }
        
        if (insertionFailures > 0) {
            results.addFailure(testName, insertionFailures + " insertion verification failures");
        }
        
        // Verify in-order traversal
        System.out.println("Verifying key sets match sorted helper structure...");
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<Integer> treeMapKeys = getInOrderFromTreeMap(treeMap);
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        boolean avlMatch = avlInOrder.size() == helperStructure.size();
        boolean treeMapMatch = treeMapKeys.size() == helperStructure.size();
        
        if (avlMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (avlInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    avlMatch = false;
                    results.addFailure(testName, "AVL in-order traversal mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "AVL size mismatch: expected " + helperStructure.size() + ", got " + avlInOrder.size());
        }
        
        if (treeMapMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (!treeMapKeys.get(i).equals(helperStructure.get(i).getValue())) {
                    treeMapMatch = false;
                    results.addFailure(testName, "TreeMap key mismatch at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "TreeMap size mismatch: expected " + helperStructure.size() + ", got " + treeMapKeys.size());
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all insertions")) {
            throw new Error("AVL balance factor violation detected after all insertions");
        }
        
        boolean passed = avlMatch && treeMapMatch && insertionFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED");
        }
        
        Collections.shuffle(helperStructure, random);
        return helperStructure;
    }
    
    // Test 2 (TreeMap): Deletion Test with Timing
    public static void test2_deletionTest_TreeMap(AVLTree<IntegerData> avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random, TestResults results, PerformanceMetrics perf) {
        String testName = "Test 2 (TreeMap): Deletion Test";
        System.out.println("\n===" + testName + "===");
        
        int deleteCount = DELETION_TEST_COUNT;
        Collections.shuffle(helperStructure, random);
        
        List<IntegerData> toDelete = new ArrayList<>(helperStructure.subList(0, Math.min(deleteCount, helperStructure.size())));
        
        System.out.println("Deleting " + toDelete.size() + " elements (with timing)...");
        int deletionFailures = 0;
        int balanceFactorFailures = 0;
        
        for (int i = 0; i < toDelete.size(); i++) {
            IntegerData data = toDelete.get(i);
            
            // Time AVL deletion
            long startTime = System.nanoTime();
            avlTree.delete(data);
            long avlTime = System.nanoTime() - startTime;
            perf.avlDeleteTime += avlTime;
            perf.deleteCount++;
            
            // Time TreeMap deletion
            startTime = System.nanoTime();
            treeMap.remove(data.getValue());
            long treeMapTime = System.nanoTime() - startTime;
            perf.treeMapDeleteTime += treeMapTime;
            
            helperStructure.remove(data);
            
            // Verify deletion
            IntegerData avlFound = avlTree.find(data);
            IntegerData treeMapFound = treeMap.get(data.getValue());
            
            if (avlFound != null) {
                throw new Error("Element " + data.getValue() + " still found in AVL tree after deletion");
            }
            
            if (treeMapFound != null) {
                throw new Error("Element " + data.getValue() + " still found in TreeMap after deletion");
            }
            
            // Check balance factors every Nth deletion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!verifyAVLBalanceFactors(avlTree, results, testName, "deletion " + (i + 1))) {
                    throw new Error("AVL balance factor violation detected after deletion " + (i + 1));
                }
            }
        }
        
        if (deletionFailures > 0) {
            results.addFailure(testName, deletionFailures + " deletion verification failures");
        }
        
        // Verify in-order traversal
        System.out.println("Verifying key sets match sorted helper structure after deletions...");
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<Integer> treeMapKeys = getInOrderFromTreeMap(treeMap);
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        boolean avlMatch = avlInOrder.size() == helperStructure.size();
        boolean treeMapMatch = treeMapKeys.size() == helperStructure.size();
        
        if (avlMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (avlInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    avlMatch = false;
                    results.addFailure(testName, "AVL in-order traversal mismatch after deletions at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "AVL size mismatch after deletions: expected " + helperStructure.size() + ", got " + avlInOrder.size());
        }
        
        if (treeMapMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (!treeMapKeys.get(i).equals(helperStructure.get(i).getValue())) {
                    treeMapMatch = false;
                    results.addFailure(testName, "TreeMap key mismatch after deletions at position " + i);
                    break;
                }
            }
        } else {
            results.addFailure(testName, "TreeMap size mismatch after deletions: expected " + helperStructure.size() + ", got " + treeMapKeys.size());
        }
        
        // Final balance factor check
        if (!verifyAVLBalanceFactors(avlTree, results, testName, "all deletions")) {
            throw new Error("AVL balance factor violation detected after all deletions");
        }
        
        boolean passed = avlMatch && treeMapMatch && deletionFailures == 0 && balanceFactorFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED");
        }
    }
    
    // Test 3 (TreeMap): Search Test with Timing
    public static void test3_searchTest_TreeMap(AVLTree<IntegerData> avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random, TestResults results, PerformanceMetrics perf) {
        String testName = "Test 3 (TreeMap): Search Test";
        System.out.println("\n===" + testName + "===");

        if (helperStructure.isEmpty()) {
            System.out.println("Skipping: no elements to search (helper structure is empty).");
            results.recordTest(true);
            return;
        }
        
        int searchCount = SEARCH_TEST_COUNT;
        Set<Integer> helperSet = new HashSet<>();
        for (IntegerData data : helperStructure) {
            helperSet.add(data.getValue());
        }
        
        System.out.println("Performing " + searchCount + " searches (with timing)...");
        int searchFailures = 0;
        
        for (int i = 0; i < searchCount; i++) {
            int randomIndex = random.nextInt(helperStructure.size());
            IntegerData searchKey = helperStructure.get(randomIndex);
            
            // Time AVL search
            long startTime = System.nanoTime();
            IntegerData avlResult = avlTree.find(searchKey);
            long avlTime = System.nanoTime() - startTime;
            perf.avlSearchTime += avlTime;
            perf.searchCount++;
            
            // Time TreeMap search
            startTime = System.nanoTime();
            IntegerData treeMapResult = treeMap.get(searchKey.getValue());
            long treeMapTime = System.nanoTime() - startTime;
            perf.treeMapSearchTime += treeMapTime;
            
            boolean inHelper = helperSet.contains(searchKey.getValue());
            boolean avlFound = avlResult != null && avlResult.getValue() == searchKey.getValue();
            boolean treeMapFound = treeMapResult != null && treeMapResult.getValue() == searchKey.getValue();
            
            if (inHelper && !avlFound) {
                throw new Error("Element " + searchKey.getValue() + " found in helper but missing in AVL tree");
            }
            
            if (inHelper && !treeMapFound) {
                throw new Error("Element " + searchKey.getValue() + " found in helper but missing in TreeMap");
            }
        }
        
        // Test non-existent elements
        System.out.println("Testing searches for non-existent elements...");
        for (int i = 0; i < 100; i++) {
            int nonExistentValue = Integer.MAX_VALUE - i;
            IntegerData searchKey = new IntegerData(nonExistentValue);
            
            if (!helperSet.contains(nonExistentValue)) {
                IntegerData avlResult = avlTree.find(searchKey);
                IntegerData treeMapResult = treeMap.get(nonExistentValue);
                
                if (avlResult != null) {
                    throw new Error("Non-existent element " + nonExistentValue + " found in AVL tree (should be null)");
                }
                
                if (treeMapResult != null) {
                    throw new Error("Non-existent element " + nonExistentValue + " found in TreeMap (should be null)");
                }
            }
        }
        
        boolean passed = searchFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + searchFailures + " search failures");
        }
    }
    
    // Test 4 (TreeMap): Range Search Test with Timing
    public static void test4_rangeSearchTest_TreeMap(AVLTree<IntegerData> avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random, TestResults results, PerformanceMetrics perf) {
        String testName = "Test 4 (TreeMap): Range (Interval) Search Test";
        System.out.println("\n===" + testName + "===");
        
        int intervalSearchCount = INTERVAL_SEARCH_TEST_COUNT;
        Map<Integer, Boolean> recordedElements = new HashMap<>();
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        System.out.println("Performing " + intervalSearchCount + " interval searches (with timing)...");
        int rangeSearchFailures = 0;
        
        for (int i = 0; i < intervalSearchCount; i++) {
            if (helperStructure.size() < 2) break;
            
            int indexA = random.nextInt(helperStructure.size());
            int indexB = random.nextInt(helperStructure.size());
            
            if (indexA > indexB) {
                int temp = indexA;
                indexA = indexB;
                indexB = temp;
            }
            
            IntegerData a = helperStructure.get(indexA);
            IntegerData b = helperStructure.get(indexB);
            
            // Generate expected list
            List<Integer> expected = new ArrayList<>();
            for (IntegerData data : helperStructure) {
                int value = data.getValue();
                if (value >= a.getValue() && value <= b.getValue()) {
                    expected.add(value);
                    recordedElements.put(value, true);
                }
            }
            Collections.sort(expected);
            
            // Time AVL range search
            long startTime = System.nanoTime();
            List<IntegerData> avlResults = avlTree.findInRange(a, b);
            long avlTime = System.nanoTime() - startTime;
            perf.avlRangeSearchTime += avlTime;
            perf.rangeSearchCount++;
            
            // Time TreeMap range search
            startTime = System.nanoTime();
            SortedMap<Integer, IntegerData> treeMapSubMap = treeMap.subMap(a.getValue(), true, b.getValue(), true);
            List<Integer> treeMapValues = new ArrayList<>(treeMapSubMap.keySet());
            long treeMapTime = System.nanoTime() - startTime;
            perf.treeMapRangeSearchTime += treeMapTime;
            
            Collections.sort(treeMapValues);
            
            // Convert AVL results to integer list
            List<Integer> avlValues = new ArrayList<>();
            for (IntegerData data : avlResults) {
                avlValues.add(data.getValue());
            }
            Collections.sort(avlValues);
            
            // Verify correctness
            if (avlValues.size() != expected.size()) {
                throw new Error("AVL range search size mismatch: expected " + expected.size() + ", got " + avlValues.size() + " for range [" + a.getValue() + ", " + b.getValue() + "]");
            }
            
            if (treeMapValues.size() != expected.size()) {
                throw new Error("TreeMap range search size mismatch: expected " + expected.size() + ", got " + treeMapValues.size() + " for range [" + a.getValue() + ", " + b.getValue() + "]");
            }
            
            if (avlValues.size() == expected.size()) {
                for (int j = 0; j < expected.size(); j++) {
                    if (!avlValues.get(j).equals(expected.get(j))) {
                        throw new Error("AVL range search content mismatch at position " + j + " for range [" + a.getValue() + ", " + b.getValue() + "]");
                    }
                }
            }
            
            if (treeMapValues.size() == expected.size()) {
                for (int j = 0; j < expected.size(); j++) {
                    if (!treeMapValues.get(j).equals(expected.get(j))) {
                        throw new Error("TreeMap range search content mismatch at position " + j + " for range [" + a.getValue() + ", " + b.getValue() + "]");
                    }
                }
            }
            
            if (rangeSearchFailures > 0) break;
        }
        
        // Continue if HashMap has fewer than minimum required elements
        while (recordedElements.size() < INTERVAL_SEARCH_MIN_ELEMENTS && helperStructure.size() >= 2) {
            int indexA = random.nextInt(helperStructure.size());
            int indexB = random.nextInt(helperStructure.size());
            
            if (indexA > indexB) {
                int temp = indexA;
                indexA = indexB;
                indexB = temp;
            }
            
            IntegerData a = helperStructure.get(indexA);
            IntegerData b = helperStructure.get(indexB);
            
            List<IntegerData> avlResults = avlTree.findInRange(a, b);
            for (IntegerData data : avlResults) {
                recordedElements.put(data.getValue(), true);
            }
        }
        
        boolean passed = rangeSearchFailures == 0;
        results.recordTest(passed);
        
        if (passed) {
            System.out.println("✓ " + testName + " PASSED. Recorded " + recordedElements.size() + " unique elements");
        } else {
            System.out.println("✗ " + testName + " FAILED: " + rangeSearchFailures + " range search failures");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Testing Suite ===");
        
        Random random = new Random(2025);
        TestResults results = new TestResults();
        PerformanceMetrics perf = new PerformanceMetrics();
        
        // ========== GROUP 1: AVL Tree vs BST ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GROUP 1: AVL Tree vs BST");
        System.out.println("=".repeat(60));
        
        // Test 1: Insertion Test
        AVLTree<IntegerData> avlTree1 = new AVLTree<>();
        BSTree<IntegerData> bstTree1 = new BSTree<>();
        List<IntegerData> helperStructure1 = test1_insertionTest(avlTree1, bstTree1, random, results);
        
        // Test 2: Deletion Test
        test2_deletionTest(avlTree1, bstTree1, helperStructure1, random, results);
        
        // Test 3: Search Test
        test3_searchTest(avlTree1, bstTree1, helperStructure1, random, results);
        
        // Test 4: Range Search Test
        test4_rangeSearchTest(avlTree1, bstTree1, helperStructure1, random, results);
        
        // Test 5: Min/Max Test
        test5_minMaxTest(avlTree1, bstTree1, helperStructure1, results);
        
        // Test 6: Increasing Sequence Test (new trees)
        AVLTree<IntegerData> avlTree6 = new AVLTree<>();
        BSTree<IntegerData> bstTree6 = new BSTree<>();
        test6_increasingSequenceTest(avlTree6, bstTree6, results);
        
        // Test 7: Randomized Operations Test 1 (new trees)
        AVLTree<IntegerData> avlTree7 = new AVLTree<>();
        BSTree<IntegerData> bstTree7 = new BSTree<>();
        test7_randomizedOperationsTest1(avlTree7, bstTree7, random, results);
        
        // Test 8: Randomized Operations Test 2 (new trees)
        AVLTree<IntegerData> avlTree8 = new AVLTree<>();
        BSTree<IntegerData> bstTree8 = new BSTree<>();
        test8_randomizedOperationsTest2(avlTree8, bstTree8, random, results);
        
        // ========== GROUP 2: AVL Tree vs TreeMap (with Performance) ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GROUP 2: AVL Tree vs TreeMap (Performance Comparison)");
        System.out.println("=".repeat(60));
        
        // Test 1 (TreeMap): Insertion Test
        AVLTree<IntegerData> avlTreeMap1 = new AVLTree<>();
        TreeMap<Integer, IntegerData> treeMap1 = new TreeMap<>();
        List<IntegerData> helperStructureMap1 = test1_insertionTest_TreeMap(avlTreeMap1, treeMap1, random, results, perf);
        
        // Test 2 (TreeMap): Deletion Test
        test2_deletionTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random, results, perf);
        
        // Test 3 (TreeMap): Search Test
        test3_searchTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random, results, perf);
        
        // Test 4 (TreeMap): Range Search Test
        test4_rangeSearchTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random, results, perf);
        
        // Print performance comparison
        perf.printPerformanceComparison();
        
        // Print final summary
        results.printSummary();
    }
}
