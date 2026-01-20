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
    private static final int INSERTION_TEST_SIZE = 10_000;
    private static final int DELETION_TEST_COUNT = 2_000;
    private static final int SEARCH_TEST_COUNT = 5_000;
    private static final int INTERVAL_SEARCH_TEST_COUNT = 1_000;
    private static final int MIN_MAX_TEST_OPERATIONS = 2_000;
    private static final int INCREASING_SEQUENCE_SIZE = 1_000;
    private static final int RANDOMIZED_OPERATIONS_COUNT = 5_000;
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
    
    // Balance factor check frequency
    private static final int BALANCE_CHECK_FREQUENCY = 1;
    
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
    private static List<IntegerData> getInOrderTraversal(BSTree tree) {
        List<IntegerData> result = new ArrayList<>();
        tree.inorderTraversal(data -> {
            result.add((IntegerData) data);
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
    
    // Test 1: Insertion Test
    public static List<IntegerData> test1_insertionTest(AVLTree avlTree, BSTree bstTree, Random random) {
        System.out.println("\n=== Test 1: Insertion Test ===");
        
        int dataSize = INSERTION_TEST_SIZE;
        Set<Integer> uniqueValues = new HashSet<>();
        List<IntegerData> helperStructure = new ArrayList<>(dataSize);
        
        System.out.println("Generating " + dataSize + " unique random integers...");
        while (helperStructure.size() < dataSize) {
            int value = Math.abs(random.nextInt()%(INSERTION_TEST_SIZE*20));
            if (uniqueValues.add(value)) {
                IntegerData data = new IntegerData(value);
                helperStructure.add(data);
            }
        }
        
        System.out.println("Inserting into AVL tree, BST, and helper structure...");
        for (int i = 0; i < helperStructure.size(); i++) {
            IntegerData data = helperStructure.get(i);
            avlTree.insert(data);
            bstTree.insert(data);
            
            // Check balance factors every Nth insertion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!avlTree.verifyAllBalanceFactors()) {
                    avlTree.printTree();
                    throw new Error("WARNING: Balance factor violation detected after " + (i + 1) + " insertions");
                }
            }
        }
        
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
                    break;
                }
            }
        }
        
        if (bstMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (bstInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    bstMatch = false;
                    break;
                }
            }
        }
        
        if (avlMatch && bstMatch) {
            System.out.println("✓ Test 1 PASSED: In-order traversals match sorted helper structure");
        } else {
            System.out.println("✗ Test 1 FAILED: In-order traversals do not match");
            if (!avlMatch) System.out.println("  AVL tree mismatch");
            if (!bstMatch) System.out.println("  BST mismatch");
        }
        
        // Return unsorted helper structure for use in subsequent tests
        Collections.shuffle(helperStructure, random);
        return helperStructure;
    }
    
    // Test 2: Deletion Test
    public static void test2_deletionTest(AVLTree avlTree, BSTree bstTree, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 2: Deletion Test ===");
        
        int deleteCount = DELETION_TEST_COUNT;
      //  Collections.shuffle(helperStructure, random);
        
        List<IntegerData> toDelete = new ArrayList<>(helperStructure.subList(0, Math.min(deleteCount, helperStructure.size())));
        Set<Integer> deletedValues = new HashSet<>();
        for (IntegerData data : toDelete) {
            deletedValues.add(data.getValue());
        }
        
        System.out.println("Deleting " + toDelete.size() + " elements...");
        for (int i = 0; i < toDelete.size(); i++) {
            IntegerData data = toDelete.get(i);
            avlTree.delete(data);
            bstTree.delete(data);
            helperStructure.remove(data);
            
            // Check balance factors every Nth deletion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!avlTree.verifyAllBalanceFactors()) {
                    avlTree.printTree();
                    throw new Error("WARNING: Balance factor violation detected after " + (i + 1) + " deletions");

                  // System.out.println("WARNING: Balance factor violation detected after " + (i + 1) + " deletions");
                }
            }
        }
        
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
                    break;
                }
            }
        }
        
        if (bstMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (bstInOrder.get(i).getValue() != helperStructure.get(i).getValue()) {
                    bstMatch = false;
                    break;
                }
            }
        }
        
        if (avlMatch && bstMatch) {
            System.out.println("✓ Test 2 PASSED: In-order traversals match sorted helper structure after deletions");
        } else {
            System.out.println("✗ Test 2 FAILED: In-order traversals do not match after deletions");
            if (!avlMatch) System.out.println("  AVL tree mismatch. Expected size: " + helperStructure.size() + ", Got: " + avlInOrder.size());
            if (!bstMatch) System.out.println("  BST mismatch. Expected size: " + helperStructure.size() + ", Got: " + bstInOrder.size());
        }
    }
    
    // Test 3: Search Test
    public static void test3_searchTest(AVLTree avlTree, BSTree bstTree, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 3: Search Test ===");

        if (helperStructure.isEmpty()) {
            System.out.println("Skipping: no elements to search (helper structure is empty).");
            return;
        }
        
        int searchCount = SEARCH_TEST_COUNT;
        Set<Integer> helperSet = new HashSet<>();
        for (IntegerData data : helperStructure) {
            helperSet.add(data.getValue());
        }
        
        System.out.println("Performing " + searchCount + " searches...");
        boolean allFound = true;
        int missingKey = 0;
        
        for (int i = 0; i < searchCount; i++) {
            int randomIndex = random.nextInt(helperStructure.size());
            IntegerData searchKey = helperStructure.get(randomIndex);
            
            TreeNodeData avlResult = avlTree.find(searchKey);
            TreeNodeData bstResult = bstTree.find(searchKey);
            
            boolean inHelper = helperSet.contains(searchKey.getValue());
            boolean avlFound = avlResult != null;
            boolean bstFound = bstResult != null;
            
            if (inHelper && (!avlFound || !bstFound)) {
                allFound = false;
                missingKey = searchKey.getValue();
                System.out.println("✗ Test 3 FAILED: Element " + missingKey + " found in helper but missing in tree(s)");
                System.out.println("  AVL found: " + avlFound + ", BST found: " + bstFound);
                break;
            }
        }
        
        if (allFound) {
            System.out.println("✓ Test 3 PASSED: All searches successful");
        }
    }
    
    // Test 4: Range (Interval) Search Test
    public static void test4_rangeSearchTest(AVLTree avlTree, BSTree bstTree, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 4: Range (Interval) Search Test ===");
        
        int intervalSearchCount = INTERVAL_SEARCH_TEST_COUNT;
        Map<Integer, Boolean> recordedElements = new HashMap<>();
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        System.out.println("Performing " + intervalSearchCount + " interval searches...");
        boolean allCorrect = true;
        
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
            List<TreeNodeData> avlResults = avlTree.findInRange(a, b);
            List<TreeNodeData> bstResults = bstTree.findInRange(a, b);
            
            // Convert to integer lists for comparison
            List<Integer> avlValues = new ArrayList<>();
            for (TreeNodeData data : avlResults) {
                avlValues.add(((IntegerData) data).getValue());
            }
            Collections.sort(avlValues);
            
            List<Integer> bstValues = new ArrayList<>();
            for (TreeNodeData data : bstResults) {
                bstValues.add(((IntegerData) data).getValue());
            }
            Collections.sort(bstValues);
            
            // Verify correctness
            if (avlValues.size() != expected.size() || bstValues.size() != expected.size()) {
                allCorrect = false;
                System.out.println("✗ Test 4 FAILED: Size mismatch in interval search");
                System.out.println("  Expected: " + expected.size() + ", AVL: " + avlValues.size() + ", BST: " + bstValues.size());
                break;
            }
            
            for (int j = 0; j < expected.size(); j++) {
                if (!avlValues.get(j).equals(expected.get(j)) || !bstValues.get(j).equals(expected.get(j))) {
                    allCorrect = false;
                    System.out.println("✗ Test 4 FAILED: Content mismatch in interval search");
                    break;
                }
            }
            
            if (!allCorrect) break;
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
            
            List<TreeNodeData> avlResults = avlTree.findInRange(a, b);
            for (TreeNodeData data : avlResults) {
                recordedElements.put(((IntegerData) data).getValue(), true);
            }
        }
        
        if (allCorrect) {
            System.out.println("✓ Test 4 PASSED: All interval searches correct. Recorded " + recordedElements.size() + " unique elements");
        }
    }
    
    // Test 5: Minimum and Maximum Key Test
    public static void test5_minMaxTest(AVLTree avlTree, BSTree bstTree, List<IntegerData> helperStructure) {
        System.out.println("\n=== Test 5: Minimum and Maximum Key Test ===");
        
        int operations = MIN_MAX_TEST_OPERATIONS;
        boolean allCorrect = true;
        
        for (int i = 0; i < operations; i++) {
            if (helperStructure.isEmpty()) break;
            
            // Get expected min and max from helper structure
            int expectedMin = helperStructure.stream().mapToInt(IntegerData::getValue).min().orElse(0);
            int expectedMax = helperStructure.stream().mapToInt(IntegerData::getValue).max().orElse(0);
            
            // Get from trees
            TreeNodeData avlMin = avlTree.findMin();
            TreeNodeData avlMax = avlTree.findMax();
            TreeNodeData bstMin = bstTree.findMin();
            TreeNodeData bstMax = bstTree.findMax();
            
            int avlMinValue = avlMin != null ? ((IntegerData) avlMin).getValue() : 0;
            int avlMaxValue = avlMax != null ? ((IntegerData) avlMax).getValue() : 0;
            int bstMinValue = bstMin != null ? ((IntegerData) bstMin).getValue() : 0;
            int bstMaxValue = bstMax != null ? ((IntegerData) bstMax).getValue() : 0;
            
            if (avlMinValue != expectedMin || avlMaxValue != expectedMax ||
                bstMinValue != expectedMin || bstMaxValue != expectedMax) {
                allCorrect = false;
                System.out.println("✗ Test 5 FAILED: Min/Max mismatch");
                System.out.println("  Expected Min: " + expectedMin + ", AVL Min: " + avlMinValue + ", BST Min: " + bstMinValue);
                System.out.println("  Expected Max: " + expectedMax + ", AVL Max: " + avlMaxValue + ", BST Max: " + bstMaxValue);
                break;
            }
        }
        
        if (allCorrect) {
            System.out.println("✓ Test 5 PASSED: All " + operations + " min/max operations correct");
        }
    }
    
    // Test 6: Increasing Sequence Insertion Test
    public static void test6_increasingSequenceTest(AVLTree avlTree, BSTree bstTree) {
        System.out.println("\n=== Test 6: Increasing Sequence Insertion Test ===");
        
        int sequenceSize = INCREASING_SEQUENCE_SIZE;
        List<IntegerData> increasingSequence = new ArrayList<>(sequenceSize);
        
        System.out.println("Generating strictly increasing sequence of " + sequenceSize + " integers...");
        for (int i = 1; i <= sequenceSize; i++) {
            increasingSequence.add(new IntegerData(i));
        }
        
        System.out.println("Inserting into both trees...");
        for (IntegerData data : increasingSequence) {
            avlTree.insert(data);
            bstTree.insert(data);
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
                    break;
                }
            }
        }
        
        if (bstCorrect) {
            for (int i = 0; i < increasingSequence.size(); i++) {
                if (bstInOrder.get(i).getValue() != increasingSequence.get(i).getValue()) {
                    bstCorrect = false;
                    break;
                }
            }
        }
        
        if (avlCorrect && bstCorrect) {
            System.out.println("✓ Test 6 PASSED: In-order traversals match increasing sequence");
        } else {
            System.out.println("✗ Test 6 FAILED: In-order traversals do not match");
        }
    }
    
    // Test 7: Randomized Operation Probability Test 1
    public static void test7_randomizedOperationsTest1(AVLTree avlTree, BSTree bstTree, Random random) {
        System.out.println("\n=== Test 7: Randomized Operation Probability Test 1 (" + 
                         (int)(RANDOMIZED_TEST1_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST1_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST1_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        
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
                }
            } else if (operation < RANDOMIZED_TEST1_INSERT_PROB + RANDOMIZED_TEST1_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    bstTree.delete(data);
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST1_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        avlTree.find(data);
                        bstTree.find(data);
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    avlTree.find(data);
                    bstTree.find(data);
                }
            }
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        System.out.println("✓ Test 7 COMPLETED");
    }
    
    // Test 8: Randomized Operation Probability Test 2
    public static void test8_randomizedOperationsTest2(AVLTree avlTree, BSTree bstTree, Random random) {
        System.out.println("\n=== Test 8: Randomized Operation Probability Test 2 (" + 
                         (int)(RANDOMIZED_TEST2_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST2_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST2_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        
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
                }
            } else if (operation < RANDOMIZED_TEST2_INSERT_PROB + RANDOMIZED_TEST2_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    bstTree.delete(data);
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST2_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        avlTree.find(data);
                        bstTree.find(data);
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    avlTree.find(data);
                    bstTree.find(data);
                }
            }
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        System.out.println("✓ Test 8 COMPLETED");
    }
    
    // ========== GROUP 2: AVL Tree vs TreeMap Tests ==========
    
    // Test 1 (TreeMap): Insertion Test
    public static List<IntegerData> test1_insertionTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, Random random) {
        System.out.println("\n=== Test 1 (TreeMap): Insertion Test ===");
        
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
        
        System.out.println("Inserting into AVL tree, TreeMap, and helper structure...");
        for (int i = 0; i < helperStructure.size(); i++) {
            IntegerData data = helperStructure.get(i);
            avlTree.insert(data);
            treeMap.put(data.getValue(), data);
            
            // Check balance factors every Nth insertion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!avlTree.verifyAllBalanceFactors()) {
                    System.out.println("WARNING: Balance factor violation detected after " + (i + 1) + " insertions");
                }
            }
        }
        
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
                    break;
                }
            }
        }
        
        if (treeMapMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (!treeMapKeys.get(i).equals(helperStructure.get(i).getValue())) {
                    treeMapMatch = false;
                    break;
                }
            }
        }
        
        if (avlMatch && treeMapMatch) {
            System.out.println("✓ Test 1 (TreeMap) PASSED: Key sets match sorted helper structure");
        } else {
            System.out.println("✗ Test 1 (TreeMap) FAILED: Key sets do not match");
            if (!avlMatch) System.out.println("  AVL tree mismatch");
            if (!treeMapMatch) System.out.println("  TreeMap mismatch");
        }
        
        // Return unsorted helper structure for use in subsequent tests
        Collections.shuffle(helperStructure, random);
        return helperStructure;
    }
    
    // Test 2 (TreeMap): Deletion Test
    public static void test2_deletionTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 2 (TreeMap): Deletion Test ===");
        
        int deleteCount = DELETION_TEST_COUNT;
        Collections.shuffle(helperStructure, random);
        
        List<IntegerData> toDelete = new ArrayList<>(helperStructure.subList(0, Math.min(deleteCount, helperStructure.size())));
        
        System.out.println("Deleting " + toDelete.size() + " elements...");
        for (int i = 0; i < toDelete.size(); i++) {
            IntegerData data = toDelete.get(i);
            avlTree.delete(data);
            treeMap.remove(data.getValue());
            helperStructure.remove(data);
            
            // Check balance factors every Nth deletion
            if ((i + 1) % BALANCE_CHECK_FREQUENCY == 0) {
                if (!avlTree.verifyAllBalanceFactors()) {
                    System.out.println("WARNING: Balance factor violation detected after " + (i + 1) + " deletions");
                }
            }
        }
        
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
                    break;
                }
            }
        }
        
        if (treeMapMatch) {
            for (int i = 0; i < helperStructure.size(); i++) {
                if (!treeMapKeys.get(i).equals(helperStructure.get(i).getValue())) {
                    treeMapMatch = false;
                    break;
                }
            }
        }
        
        if (avlMatch && treeMapMatch) {
            System.out.println("✓ Test 2 (TreeMap) PASSED: Key sets match sorted helper structure after deletions");
        } else {
            System.out.println("✗ Test 2 (TreeMap) FAILED: Key sets do not match after deletions");
            if (!avlMatch) System.out.println("  AVL tree mismatch. Expected size: " + helperStructure.size() + ", Got: " + avlInOrder.size());
            if (!treeMapMatch) System.out.println("  TreeMap mismatch. Expected size: " + helperStructure.size() + ", Got: " + treeMapKeys.size());
        }
    }
    
    // Test 3 (TreeMap): Search Test
    public static void test3_searchTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 3 (TreeMap): Search Test ===");

        if (helperStructure.isEmpty()) {
            System.out.println("Skipping: no elements to search (helper structure is empty).");
            return;
        }
        
        int searchCount = SEARCH_TEST_COUNT;
        Set<Integer> helperSet = new HashSet<>();
        for (IntegerData data : helperStructure) {
            helperSet.add(data.getValue());
        }
        
        System.out.println("Performing " + searchCount + " searches...");
        boolean allFound = true;
        
        for (int i = 0; i < searchCount; i++) {
            int randomIndex = random.nextInt(helperStructure.size());
            IntegerData searchKey = helperStructure.get(randomIndex);
            
            TreeNodeData avlResult = avlTree.find(searchKey);
            IntegerData treeMapResult = treeMap.get(searchKey.getValue());
            
            boolean inHelper = helperSet.contains(searchKey.getValue());
            boolean avlFound = avlResult != null;
            boolean treeMapFound = treeMapResult != null;
            
            if (inHelper && (!avlFound || !treeMapFound)) {
                allFound = false;
                int missingKey = searchKey.getValue();
                System.out.println("✗ Test 3 (TreeMap) FAILED: Element " + missingKey + " found in helper but missing in structure(s)");
                System.out.println("  AVL found: " + avlFound + ", TreeMap found: " + treeMapFound);
                break;
            }
        }
        
        if (allFound) {
            System.out.println("✓ Test 3 (TreeMap) PASSED: All searches successful");
        }
    }
    
    // Test 4 (TreeMap): Range (Interval) Search Test
    public static void test4_rangeSearchTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure, Random random) {
        System.out.println("\n=== Test 4 (TreeMap): Range (Interval) Search Test ===");
        
        int intervalSearchCount = INTERVAL_SEARCH_TEST_COUNT;
        Map<Integer, Boolean> recordedElements = new HashMap<>();
        
        Collections.sort(helperStructure, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        System.out.println("Performing " + intervalSearchCount + " interval searches...");
        boolean allCorrect = true;
        
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
            
            // Query AVL tree
            List<TreeNodeData> avlResults = avlTree.findInRange(a, b);
            
            // Query TreeMap using subMap
            SortedMap<Integer, IntegerData> treeMapSubMap = treeMap.subMap(a.getValue(), true, b.getValue(), true);
            List<Integer> treeMapValues = new ArrayList<>(treeMapSubMap.keySet());
            Collections.sort(treeMapValues);
            
            // Convert AVL results to integer list
            List<Integer> avlValues = new ArrayList<>();
            for (TreeNodeData data : avlResults) {
                avlValues.add(((IntegerData) data).getValue());
            }
            Collections.sort(avlValues);
            
            // Verify correctness
            if (avlValues.size() != expected.size() || treeMapValues.size() != expected.size()) {
                allCorrect = false;
                System.out.println("✗ Test 4 (TreeMap) FAILED: Size mismatch in interval search");
                System.out.println("  Expected: " + expected.size() + ", AVL: " + avlValues.size() + ", TreeMap: " + treeMapValues.size());
                break;
            }
            
            for (int j = 0; j < expected.size(); j++) {
                if (!avlValues.get(j).equals(expected.get(j)) || !treeMapValues.get(j).equals(expected.get(j))) {
                    allCorrect = false;
                    System.out.println("✗ Test 4 (TreeMap) FAILED: Content mismatch in interval search");
                    break;
                }
            }
            
            if (!allCorrect) break;
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
            
            List<TreeNodeData> avlResults = avlTree.findInRange(a, b);
            for (TreeNodeData data : avlResults) {
                recordedElements.put(((IntegerData) data).getValue(), true);
            }
        }
        
        if (allCorrect) {
            System.out.println("✓ Test 4 (TreeMap) PASSED: All interval searches correct. Recorded " + recordedElements.size() + " unique elements");
        }
    }
    
    // Test 5 (TreeMap): Minimum and Maximum Key Test
    public static void test5_minMaxTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, List<IntegerData> helperStructure) {
        System.out.println("\n=== Test 5 (TreeMap): Minimum and Maximum Key Test ===");
        
        int operations = MIN_MAX_TEST_OPERATIONS;
        boolean allCorrect = true;
        
        for (int i = 0; i < operations; i++) {
            if (helperStructure.isEmpty() || treeMap.isEmpty()) break;
            
            // Get expected min and max from helper structure
            int expectedMin = helperStructure.stream().mapToInt(IntegerData::getValue).min().orElse(0);
            int expectedMax = helperStructure.stream().mapToInt(IntegerData::getValue).max().orElse(0);
            
            // Get from AVL tree
            TreeNodeData avlMin = avlTree.findMin();
            TreeNodeData avlMax = avlTree.findMax();
            
            // Get from TreeMap
            int treeMapMin = treeMap.firstKey();
            int treeMapMax = treeMap.lastKey();
            
            int avlMinValue = avlMin != null ? ((IntegerData) avlMin).getValue() : 0;
            int avlMaxValue = avlMax != null ? ((IntegerData) avlMax).getValue() : 0;
            
            if (avlMinValue != expectedMin || avlMaxValue != expectedMax ||
                treeMapMin != expectedMin || treeMapMax != expectedMax) {
                allCorrect = false;
                System.out.println("✗ Test 5 (TreeMap) FAILED: Min/Max mismatch");
                System.out.println("  Expected Min: " + expectedMin + ", AVL Min: " + avlMinValue + ", TreeMap Min: " + treeMapMin);
                System.out.println("  Expected Max: " + expectedMax + ", AVL Max: " + avlMaxValue + ", TreeMap Max: " + treeMapMax);
                break;
            }
        }
        
        if (allCorrect) {
            System.out.println("✓ Test 5 (TreeMap) PASSED: All " + operations + " min/max operations correct");
        }
    }
    
    // Test 6 (TreeMap): Increasing Sequence Insertion Test
    public static void test6_increasingSequenceTest_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap) {
        System.out.println("\n=== Test 6 (TreeMap): Increasing Sequence Insertion Test ===");
        
        int sequenceSize = INCREASING_SEQUENCE_SIZE;
        List<IntegerData> increasingSequence = new ArrayList<>(sequenceSize);
        
        System.out.println("Generating strictly increasing sequence of " + sequenceSize + " integers...");
        for (int i = 1; i <= sequenceSize; i++) {
            increasingSequence.add(new IntegerData(i));
        }
        
        System.out.println("Inserting into both structures...");
        for (IntegerData data : increasingSequence) {
            avlTree.insert(data);
            treeMap.put(data.getValue(), data);
        }
        
        int avlHeight = avlTree.getHeight();
        
        System.out.println("AVL Tree height: " + avlHeight);
        System.out.println("TreeMap size: " + treeMap.size());
        
        // Verify in-order traversal
        List<IntegerData> avlInOrder = getInOrderTraversal(avlTree);
        List<Integer> treeMapKeys = getInOrderFromTreeMap(treeMap);
        
        boolean avlCorrect = avlInOrder.size() == increasingSequence.size();
        boolean treeMapCorrect = treeMapKeys.size() == increasingSequence.size();
        
        if (avlCorrect) {
            for (int i = 0; i < increasingSequence.size(); i++) {
                if (avlInOrder.get(i).getValue() != increasingSequence.get(i).getValue()) {
                    avlCorrect = false;
                    break;
                }
            }
        }
        
        if (treeMapCorrect) {
            for (int i = 0; i < increasingSequence.size(); i++) {
                if (!treeMapKeys.get(i).equals(increasingSequence.get(i).getValue())) {
                    treeMapCorrect = false;
                    break;
                }
            }
        }
        
        if (avlCorrect && treeMapCorrect) {
            System.out.println("✓ Test 6 (TreeMap) PASSED: Key sets match increasing sequence");
        } else {
            System.out.println("✗ Test 6 (TreeMap) FAILED: Key sets do not match");
        }
    }
    
    // Test 7 (TreeMap): Randomized Operation Probability Test 1
    public static void test7_randomizedOperationsTest1_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, Random random) {
        System.out.println("\n=== Test 7 (TreeMap): Randomized Operation Probability Test 1 (" + 
                         (int)(RANDOMIZED_TEST1_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST1_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST1_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        
        for (int i = 0; i < totalOperations; i++) {
            double operation = random.nextDouble();
            
            if (operation < RANDOMIZED_TEST1_INSERT_PROB) {
                // Insertion
                int value = random.nextInt();
                IntegerData data = new IntegerData(value);
                if (!helperSet.contains(value)) {
                    avlTree.insert(data);
                    treeMap.put(value, data);
                    helperStructure.add(data);
                    helperSet.add(value);
                }
            } else if (operation < RANDOMIZED_TEST1_INSERT_PROB + RANDOMIZED_TEST1_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    treeMap.remove(data.getValue());
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST1_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        avlTree.find(data);
                        treeMap.get(data.getValue());
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    avlTree.find(data);
                    treeMap.get(value);
                }
            }
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        System.out.println("✓ Test 7 (TreeMap) COMPLETED");
    }
    
    // Test 8 (TreeMap): Randomized Operation Probability Test 2
    public static void test8_randomizedOperationsTest2_TreeMap(AVLTree avlTree, TreeMap<Integer, IntegerData> treeMap, Random random) {
        System.out.println("\n=== Test 8 (TreeMap): Randomized Operation Probability Test 2 (" + 
                         (int)(RANDOMIZED_TEST2_INSERT_PROB * 100) + "% insert, " +
                         (int)(RANDOMIZED_TEST2_DELETE_PROB * 100) + "% delete, " +
                         (int)(RANDOMIZED_TEST2_SEARCH_PROB * 100) + "% search) ===");
        
        int totalOperations = RANDOMIZED_OPERATIONS_COUNT;
        List<IntegerData> helperStructure = new ArrayList<>();
        Set<Integer> helperSet = new HashSet<>();
        
        System.out.println("Performing " + totalOperations + " randomized operations...");
        
        for (int i = 0; i < totalOperations; i++) {
            double operation = random.nextDouble();
            
            if (operation < RANDOMIZED_TEST2_INSERT_PROB) {
                // Insertion
                int value = random.nextInt();
                IntegerData data = new IntegerData(value);
                if (!helperSet.contains(value)) {
                    avlTree.insert(data);
                    treeMap.put(value, data);
                    helperStructure.add(data);
                    helperSet.add(value);
                }
            } else if (operation < RANDOMIZED_TEST2_INSERT_PROB + RANDOMIZED_TEST2_DELETE_PROB) {
                // Deletion
                if (!helperStructure.isEmpty()) {
                    int randomIndex = random.nextInt(helperStructure.size());
                    IntegerData data = helperStructure.remove(randomIndex);
                    helperSet.remove(data.getValue());
                    avlTree.delete(data);
                    treeMap.remove(data.getValue());
                }
            } else {
                // Search
                if (random.nextDouble() < RANDOMIZED_TEST2_EXISTING_SEARCH_PROB) {
                    // Search for existing element
                    if (!helperStructure.isEmpty()) {
                        int randomIndex = random.nextInt(helperStructure.size());
                        IntegerData data = helperStructure.get(randomIndex);
                        avlTree.find(data);
                        treeMap.get(data.getValue());
                    }
                } else {
                    // Search for non-existing element
                    int value = random.nextInt();
                    while (helperSet.contains(value)) {
                        value = random.nextInt();
                    }
                    IntegerData data = new IntegerData(value);
                    avlTree.find(data);
                    treeMap.get(value);
                }
            }
        }
        
        System.out.println("Final helper structure size: " + helperStructure.size());
        System.out.println("✓ Test 8 (TreeMap) COMPLETED");
    }
    
    public static void main(String[] args) {
        System.out.println("=== Comprehensive Testing Suite ===");
        
        Random random = new Random(2025);
        
        // ========== GROUP 1: AVL Tree vs BST ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GROUP 1: AVL Tree vs BST");
        System.out.println("=".repeat(60));
        
        // Test 1: Insertion Test
        AVLTree avlTree1 = new AVLTree();
        BSTree bstTree1 = new BSTree();
        List<IntegerData> helperStructure1 = test1_insertionTest(avlTree1, bstTree1, random);
        
        // Test 2: Deletion Test
        test2_deletionTest(avlTree1, bstTree1, helperStructure1, random);
        
        // Test 3: Search Test
        test3_searchTest(avlTree1, bstTree1, helperStructure1, random);
        
        // Test 4: Range Search Test
        test4_rangeSearchTest(avlTree1, bstTree1, helperStructure1, random);
        
        // Test 5: Min/Max Test
        test5_minMaxTest(avlTree1, bstTree1, helperStructure1);
        
        // Test 6: Increasing Sequence Test (new trees)
        AVLTree avlTree6 = new AVLTree();
        BSTree bstTree6 = new BSTree();
        test6_increasingSequenceTest(avlTree6, bstTree6);
        
        // Test 7: Randomized Operations Test 1 (new trees)
        AVLTree avlTree7 = new AVLTree();
        BSTree bstTree7 = new BSTree();
        test7_randomizedOperationsTest1(avlTree7, bstTree7, random);
        
        // Test 8: Randomized Operations Test 2 (new trees)
        AVLTree avlTree8 = new AVLTree();
        BSTree bstTree8 = new BSTree();
        test8_randomizedOperationsTest2(avlTree8, bstTree8, random);
        
        // ========== GROUP 2: AVL Tree vs TreeMap ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GROUP 2: AVL Tree vs TreeMap");
        System.out.println("=".repeat(60));
        
        // Test 1 (TreeMap): Insertion Test
        AVLTree avlTreeMap1 = new AVLTree();
        TreeMap<Integer, IntegerData> treeMap1 = new TreeMap<>();
        List<IntegerData> helperStructureMap1 = test1_insertionTest_TreeMap(avlTreeMap1, treeMap1, random);
        
        // Test 2 (TreeMap): Deletion Test
        test2_deletionTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random);
        
        // Test 3 (TreeMap): Search Test
        test3_searchTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random);
        
        // Test 4 (TreeMap): Range Search Test
        test4_rangeSearchTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1, random);
        
        // Test 5 (TreeMap): Min/Max Test
        test5_minMaxTest_TreeMap(avlTreeMap1, treeMap1, helperStructureMap1);
        
        // Test 6 (TreeMap): Increasing Sequence Test (new trees)
        AVLTree avlTreeMap6 = new AVLTree();
        TreeMap<Integer, IntegerData> treeMap6 = new TreeMap<>();
        test6_increasingSequenceTest_TreeMap(avlTreeMap6, treeMap6);
        
        // Test 7 (TreeMap): Randomized Operations Test 1 (new trees)
        AVLTree avlTreeMap7 = new AVLTree();
        TreeMap<Integer, IntegerData> treeMap7 = new TreeMap<>();
        test7_randomizedOperationsTest1_TreeMap(avlTreeMap7, treeMap7, random);
        
        // Test 8 (TreeMap): Randomized Operations Test 2 (new trees)
        AVLTree avlTreeMap8 = new AVLTree();
        TreeMap<Integer, IntegerData> treeMap8 = new TreeMap<>();
        test8_randomizedOperationsTest2_TreeMap(avlTreeMap8, treeMap8, random);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("All tests completed!");
        System.out.println("=".repeat(60));
    }
} 
