package javaapplication1;

import java.util.*;

public class Tester {
    
    // Test configuration constants - change these in one place to modify all tests
    private static final int DATA_SIZE = 10_000_000; // 10 million elements as required
    private static final int DELETE_COUNT = 2_000_000; // 2 million deletions as required
    private static final int SEARCH_COUNT = 5_000_000; // 5 million random searches as required
    private static final int INTERVAL_SEARCH_COUNT = 1_000_000; // 1 million interval searches as required
    private static final int MIN_MAX_OPERATIONS = 2_000_000; // 2 million min/max operations each as required
    
    // Simple data class for integer keys
    static class IntegerData implements BSTreeNodeData {
        private int value;
        
        public IntegerData(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        @Override
        public int compare(BSTreeNodeData other) {
            return Integer.compare(this.value, ((IntegerData) other).value);
        }
        
        public String toString() {
            return String.valueOf(value);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Performance Testing Suite ===");
        
        // Test 1: Basic performance test
        testBasicPerformance();
        
        // Test 2: AVL vs HashMap comparison
        testAVLvsHashMap();
    }
    
    public static void testBasicPerformance() {
        System.out.println("\n=== Basic Performance Test ===");
        
        // Generate random data with unique values
        Random random = new Random(2025); // Fixed seed for reproducibility
        int dataSize = DATA_SIZE;
        int deleteCount = DELETE_COUNT;
        int searchCount = SEARCH_COUNT;
        int intervalSearchCount = INTERVAL_SEARCH_COUNT;
        int minMaxOperations = MIN_MAX_OPERATIONS;
        
        // Pre-allocate lists
        Set<Integer> uniqueValues = new HashSet<>();
        List<IntegerData> allData = new ArrayList<>(dataSize);
        List<IntegerData> dataToDelete = new ArrayList<>(deleteCount);
        List<IntegerData> dataToSearch = new ArrayList<>(searchCount);
        
        System.out.println("Generating " + dataSize + " unique random integers...");
        while (allData.size() < dataSize) {
            int value = random.nextInt();
            if (uniqueValues.add(value)) {
                allData.add(new IntegerData(value));
            }
        }
        
        // Select random subset for deletion
        Collections.shuffle(allData, random);
        for (int i = 0; i < deleteCount; i++) {
            dataToDelete.add(allData.get(i));
        }
        
        // Select random subset for searching (ensure they exist in tree)
        for (int i = 0; i < searchCount; i++) {
            dataToSearch.add(allData.get(random.nextInt(dataSize)));
        }
        
        // Test both BSTree and AVLTree simultaneously
        System.out.println("\n--- Testing BSTree and AVLTree Simultaneously ---");
        testBothTreesPerformance(allData, dataToDelete, dataToSearch, 
                                intervalSearchCount, minMaxOperations, random);
    }
    
    private static void testBothTreesPerformance(List<IntegerData> allData, 
                                               List<IntegerData> dataToDelete, 
                                               List<IntegerData> dataToSearch,
                                               int intervalSearchCount, 
                                               int minMaxOperations, 
                                               Random random) {
        
        BSTree bstTree = new BSTree();
        AVLTree avlTree = new AVLTree();
        
        // Insert 10,000,000 elements into both trees simultaneously
        System.out.println("Inserting " + allData.size() + " elements into both trees...");
        for (IntegerData data : allData) {
            bstTree.insert(data);
            avlTree.insert(data);
        }
        System.out.println("Insertion completed. BSTree height: " + bstTree.getHeight() + 
                          ", AVLTree height: " + avlTree.getHeight());
        
        // Delete 2,000,000 elements from both trees simultaneously
        System.out.println("Deleting " + dataToDelete.size() + " elements from both trees...");
        for (IntegerData data : dataToDelete) {
            bstTree.delete(data);
            avlTree.delete(data);
        }
        System.out.println("Deletion completed. BSTree height: " + bstTree.getHeight() + 
                          ", AVLTree height: " + avlTree.getHeight());
        
        // Random search 5,000,000 elements in both trees simultaneously
        System.out.println("Performing " + dataToSearch.size() + " random searches in both trees...");
        int bstFoundCount = 0;
        int avlFoundCount = 0;
        for (IntegerData data : dataToSearch) {
            if (bstTree.find(data) != null) {
                bstFoundCount++;
            }
            if (avlTree.find(data) != null) {
                avlFoundCount++;
            }
        }
        System.out.println("Random search completed. BSTree found: " + bstFoundCount + "/" + dataToSearch.size() +
                          ", AVLTree found: " + avlFoundCount + "/" + dataToSearch.size());
        
        // Interval search 1,000,000 times in both trees (ensure at least 500 results each time)
        System.out.println("Performing " + intervalSearchCount + " interval searches in both trees...");
        int bstTotalIntervalResults = 0;
        int avlTotalIntervalResults = 0;
        
        // Create remaining data by filtering out deleted items
        Set<Integer> deletedValues = new HashSet<>();
        for (IntegerData deletedItem : dataToDelete) {
            deletedValues.add(deletedItem.getValue());
        }
        
        List<IntegerData> remainingData = new ArrayList<>();
        for (IntegerData data : allData) {
            if (!deletedValues.contains(data.getValue())) {
                remainingData.add(data);
            }
        }
        
        // Sort the remaining data to enable efficient range generation
        remainingData.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        // Ensure we have enough data for interval searches
        if (remainingData.size() < 500) {
            System.out.println("Warning: Not enough data remaining for interval searches. Remaining: " + 
                             remainingData.size() + " (need at least 500)");
            return;
        }
        
        for (int i = 0; i < intervalSearchCount; i++) {
            // Generate range based on remaining data to ensure at least 500 elements
            int startIndex = random.nextInt(remainingData.size() - 500);
            int endIndex = startIndex + 500 + random.nextInt(Math.min(500, remainingData.size() - startIndex - 500));
            
            IntegerData minKey = remainingData.get(startIndex);
            IntegerData maxKey = remainingData.get(endIndex);
            
            List<BSTreeNodeData> bstResults = bstTree.findInRange(minKey, maxKey);
            List<BSTreeNodeData> avlResults = avlTree.findInRange(minKey, maxKey);
            
            bstTotalIntervalResults += bstResults.size();
            avlTotalIntervalResults += avlResults.size();
            
            // Verify we found at least 500 elements (for debugging)
            if (bstResults.size() < 500) {
                System.out.println("Warning: BSTree found only " + bstResults.size() + " elements in range [" + 
                                 minKey.getValue() + ", " + maxKey.getValue() + "]");
            }
            if (avlResults.size() < 500) {
                System.out.println("Warning: AVLTree found only " + avlResults.size() + " elements in range [" + 
                                 minKey.getValue() + ", " + maxKey.getValue() + "]");
            }
        }
        System.out.println("Interval search completed. BSTree average results per search: " + 
                          (bstTotalIntervalResults / (double) intervalSearchCount) +
                          ", AVLTree average results per search: " + 
                          (avlTotalIntervalResults / (double) intervalSearchCount));
        
        // Min/Max operations 2,000,000 times each in both trees
        System.out.println("Performing " + minMaxOperations + " min operations in both trees...");
        for (int i = 0; i < minMaxOperations; i++) {
            bstTree.findMin();
            avlTree.findMin();
        }
        System.out.println("Min operations completed.");
        
        System.out.println("Performing " + minMaxOperations + " max operations in both trees...");
        for (int i = 0; i < minMaxOperations; i++) {
            bstTree.findMax();
            avlTree.findMax();
        }
        System.out.println("Max operations completed.");
        
        System.out.println("Final BSTree height: " + bstTree.getHeight() + 
                          ", Final AVLTree height: " + avlTree.getHeight());
    }
    
    public static void testAVLvsHashMap() {
        System.out.println("\n=== AVL Tree vs HashMap Comparison ===");
        
        Random random = new Random(123); // Different seed for this test
        
        // Test with equal insert/delete probabilities
        System.out.println("\n--- Test 1: Equal Insert/Delete Probabilities ---");
        testAVLvsHashMapWithProbabilities(0.5, 0.5, random);
        
        // Test with higher insert probability
        System.out.println("\n--- Test 2: Higher Insert Probability ---");
        testAVLvsHashMapWithProbabilities(0.7, 0.3, random);
    }
    
    private static void testAVLvsHashMapWithProbabilities(double insertProb, double deleteProb, Random random) {
        
        // Generate random data with unique values using the same constants as first test
        Set<Integer> uniqueValues = new HashSet<>();
        List<IntegerData> allData = new ArrayList<>(DATA_SIZE);
        List<IntegerData> dataToDelete = new ArrayList<>(DELETE_COUNT);
        List<IntegerData> dataToSearch = new ArrayList<>(SEARCH_COUNT);
        
        System.out.println("Generating " + DATA_SIZE + " unique random integers...");
        while (allData.size() < DATA_SIZE) {
            int value = random.nextInt();
            if (uniqueValues.add(value)) {
                allData.add(new IntegerData(value));
            }
        }
        
        // Select random subset for deletion
        Collections.shuffle(allData, random);
        for (int i = 0; i < DELETE_COUNT; i++) {
            dataToDelete.add(allData.get(i));
        }
        
        // Select random subset for searching (ensure they exist in tree)
        for (int i = 0; i < SEARCH_COUNT; i++) {
            dataToSearch.add(allData.get(random.nextInt(DATA_SIZE)));
        }
        
        // Test both AVL Tree and HashMap simultaneously with same structure as first test
        System.out.println("Testing AVL Tree and HashMap simultaneously with insert prob: " + insertProb + ", delete prob: " + deleteProb);
        AVLTree avlTree = new AVLTree();
        Map<Integer, IntegerData> hashMap = new HashMap<>();
        testBothDataStructuresWithSameStructure(avlTree, hashMap, allData, dataToDelete, dataToSearch, 
                                               INTERVAL_SEARCH_COUNT, MIN_MAX_OPERATIONS, random);
    }
    
    private static void testBothDataStructuresWithProbabilities(AVLTree avlTree, Map<Integer, IntegerData> hashMap, 
                                                               List<IntegerData> data, double insertProb, 
                                                               double deleteProb, Random random) {
        int avlInsertCount = 0;
        int avlDeleteCount = 0;
        int avlSearchCount = 0;
        int hashMapInsertCount = 0;
        int hashMapDeleteCount = 0;
        int hashMapSearchCount = 0;
        
        for (IntegerData item : data) {
            double operation = random.nextDouble();
            int key = item.getValue();
            
            if (operation < insertProb) {
                avlTree.insert(item);
                hashMap.put(key, item);
                avlInsertCount++;
                hashMapInsertCount++;
            } else if (operation < insertProb + deleteProb) {
                avlTree.delete(item);
                hashMap.remove(key);
                avlDeleteCount++;
                hashMapDeleteCount++;
            } else {
                avlTree.find(item);
                hashMap.get(key);
                avlSearchCount++;
                hashMapSearchCount++;
            }
        }
        
        System.out.println("AVL Tree - Insert: " + avlInsertCount + 
                          ", Delete: " + avlDeleteCount + ", Search: " + avlSearchCount);
        System.out.println("AVL Tree final height: " + avlTree.getHeight());
        System.out.println("HashMap - Insert: " + hashMapInsertCount + 
                          ", Delete: " + hashMapDeleteCount + ", Search: " + hashMapSearchCount);
        System.out.println("HashMap final size: " + hashMap.size());
    }
    
    private static void testBothDataStructuresWithSameStructure(AVLTree avlTree, Map<Integer, IntegerData> hashMap,
                                                               List<IntegerData> allData, 
                                                               List<IntegerData> dataToDelete, 
                                                               List<IntegerData> dataToSearch,
                                                               int intervalSearchCount, 
                                                               int minMaxOperations, 
                                                               Random random) {
        
        // Insert 10,000,000 elements into both structures simultaneously
        System.out.println("Inserting " + allData.size() + " elements into both structures...");
        for (IntegerData data : allData) {
            avlTree.insert(data);
            hashMap.put(data.getValue(), data);
        }
        System.out.println("Insertion completed. AVLTree height: " + avlTree.getHeight() + 
                          ", HashMap size: " + hashMap.size());
        
        // Delete 2,000,000 elements from both structures simultaneously
        System.out.println("Deleting " + dataToDelete.size() + " elements from both structures...");
        for (IntegerData data : dataToDelete) {
            avlTree.delete(data);
            hashMap.remove(data.getValue());
        }
        System.out.println("Deletion completed. AVLTree height: " + avlTree.getHeight() + 
                          ", HashMap size: " + hashMap.size());
        
        // Random search 5,000,000 elements in both structures simultaneously
        System.out.println("Performing " + dataToSearch.size() + " random searches in both structures...");
        int avlFoundCount = 0;
        int hashMapFoundCount = 0;
        for (IntegerData data : dataToSearch) {
            if (avlTree.find(data) != null) {
                avlFoundCount++;
            }
            if (hashMap.get(data.getValue()) != null) {
                hashMapFoundCount++;
            }
        }
        System.out.println("Random search completed. AVLTree found: " + avlFoundCount + "/" + dataToSearch.size() +
                          ", HashMap found: " + hashMapFoundCount + "/" + dataToSearch.size());
        
        // Interval search 1,000,000 times in AVLTree (HashMap doesn't support range queries)
        System.out.println("Performing " + intervalSearchCount + " interval searches in AVLTree...");
        int avlTotalIntervalResults = 0;
        
        // Create remaining data by filtering out deleted items
        Set<Integer> deletedValues = new HashSet<>();
        for (IntegerData deletedItem : dataToDelete) {
            deletedValues.add(deletedItem.getValue());
        }
        
        List<IntegerData> remainingData = new ArrayList<>();
        for (IntegerData data : allData) {
            if (!deletedValues.contains(data.getValue())) {
                remainingData.add(data);
            }
        }
        
        // Sort the remaining data to enable efficient range generation
        remainingData.sort((a, b) -> Integer.compare(a.getValue(), b.getValue()));
        
        // Ensure we have enough data for interval searches
        if (remainingData.size() < 500) {
            System.out.println("Warning: Not enough data remaining for interval searches. Remaining: " + 
                             remainingData.size() + " (need at least 500)");
            return;
        }
        
        for (int i = 0; i < intervalSearchCount; i++) {
            // Generate range based on remaining data to ensure at least 500 elements
            int startIndex = random.nextInt(remainingData.size() - 500);
            int endIndex = startIndex + 500 + random.nextInt(Math.min(500, remainingData.size() - startIndex - 500));
            
            IntegerData minKey = remainingData.get(startIndex);
            IntegerData maxKey = remainingData.get(endIndex);
            
            List<BSTreeNodeData> avlResults = avlTree.findInRange(minKey, maxKey);
            avlTotalIntervalResults += avlResults.size();
            
            // Verify we found at least 500 elements (for debugging)
            if (avlResults.size() < 500) {
                System.out.println("Warning: AVLTree found only " + avlResults.size() + " elements in range [" + 
                                 minKey.getValue() + ", " + maxKey.getValue() + "]");
            }
        }
        System.out.println("Interval search completed. AVLTree average results per search: " + 
                          (avlTotalIntervalResults / (double) intervalSearchCount));
        System.out.println("Note: HashMap does not support interval/range queries");
        
        // Min/Max operations 2,000,000 times each in AVLTree (HashMap doesn't support min/max)
        System.out.println("Performing " + minMaxOperations + " min operations in AVLTree...");
        for (int i = 0; i < minMaxOperations; i++) {
            avlTree.findMin();
        }
        System.out.println("Min operations completed.");
        
        System.out.println("Performing " + minMaxOperations + " max operations in AVLTree...");
        for (int i = 0; i < minMaxOperations; i++) {
            avlTree.findMax();
        }
        System.out.println("Max operations completed.");
        System.out.println("Note: HashMap does not support min/max operations");
        
        System.out.println("Final AVLTree height: " + avlTree.getHeight() + 
                          ", Final HashMap size: " + hashMap.size());
    }
    
    private static void testDataStructureWithProbabilities(BSTree tree, List<IntegerData> data, 
                                                          double insertProb, double deleteProb, 
                                                          Random random, String structureName) {
        int insertCount = 0;
        int deleteCount = 0;
        int searchCount = 0;
        
        for (IntegerData item : data) {
            double operation = random.nextDouble();
            
            if (operation < insertProb) {
                tree.insert(item);
                insertCount++;
            } else if (operation < insertProb + deleteProb) {
                tree.delete(item);
                deleteCount++;
            } else {
                tree.find(item);
                searchCount++;
            }
        }
        
        System.out.println(structureName + " - Insert: " + insertCount + 
                          ", Delete: " + deleteCount + ", Search: " + searchCount);
        System.out.println(structureName + " final height: " + tree.getHeight());
    }
    
    private static void testHashMapWithProbabilities(Map<Integer, IntegerData> map, List<IntegerData> data, 
                                                   double insertProb, double deleteProb, Random random) {
        int insertCount = 0;
        int deleteCount = 0;
        int searchCount = 0;
        
        for (IntegerData item : data) {
            double operation = random.nextDouble();
            int key = item.getValue();
            
            if (operation < insertProb) {
                map.put(key, item);
                insertCount++;
            } else if (operation < insertProb + deleteProb) {
                map.remove(key);
                deleteCount++;
            } else {
                map.get(key);
                searchCount++;
            }
        }
        
        System.out.println("HashMap - Insert: " + insertCount + 
                          ", Delete: " + deleteCount + ", Search: " + searchCount);
        System.out.println("HashMap final size: " + map.size());
    }
}
