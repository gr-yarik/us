package UnsortedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import LinearHashing.Person;

public class HeapDebuggerConsole {
    
    private String heapFilePath;
    private String metadataFilePath;
    private List<BlockInfo> blockInfos;
    private BlockManager blockManager;
    private int blockSize;
    private int blockingFactor;
    
    private static class BlockInfo {
        int blockNumber;
        String status;
        int validCount;
        int capacity;
        List<Person> records;
        
        BlockInfo(int blockNumber, String status, int validCount, int capacity, List<Person> records) {
            this.blockNumber = blockNumber;
            this.status = status;
            this.validCount = validCount;
            this.capacity = capacity;
            this.records = records != null ? records : new ArrayList<>();
        }
    }
    
    public static void main(String[] args) {
        HeapDebuggerConsole debugger = new HeapDebuggerConsole();
        
        Scanner scanner = new Scanner(System.in);
        
        if (args.length >= 2) {
            debugger.heapFilePath = args[0];
            debugger.metadataFilePath = args[1];
        } else {
            debugger.heapFilePath = "test_heap.bin";
            debugger.metadataFilePath = "test_heap.bin.meta";
            System.out.println("=== Heap File Debugger (Console) ===\n");
            System.out.println("Using default file paths:");
            System.out.println("  Heap file: " + debugger.heapFilePath);
            System.out.println("  Metadata file: " + debugger.metadataFilePath);
            System.out.println();
        }
        
        debugger.run(scanner);
        scanner.close();
    }
    
    private void run(Scanner scanner) {
        loadHeapData();
        
        if (blockInfos != null && !blockInfos.isEmpty()) {
            displayDetailedBlocks();
        }
        
        while (true) {
            System.out.println("\n" + repeatString("=", 70));
            System.out.println("HEAP FILE DEBUGGER - MENU");
            System.out.println(repeatString("=", 70));
            System.out.println("1. Load heap data (and show details)");
            System.out.println("2. Display all blocks summary");
            System.out.println("3. Display detailed block information");
            System.out.println("4. Display specific block");
            System.out.println("5. Refresh (reload files and show details)");
            System.out.println("0. Exit");
            System.out.print("\nSelect option: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    loadHeapData();
                    if (blockInfos != null && !blockInfos.isEmpty()) {
                        displayDetailedBlocks();
                    }
                    break;
                case "2":
                    displayBlocksSummary();
                    break;
                case "3":
                    displayDetailedBlocks();
                    break;
                case "4":
                    displaySpecificBlock(scanner);
                    break;
                case "5":
                    loadHeapData();
                    if (blockInfos != null && !blockInfos.isEmpty()) {
                        displayDetailedBlocks();
                    }
                    break;
                case "0":
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private void loadHeapData() {
        System.out.println("\nLoading heap data...");
        
        try {
            blockManager = new BlockManager(metadataFilePath);
            blockSize = blockManager.getBlockSize();
            
            Person templatePerson = new Person();
            int recordSize = templatePerson.sizeInBytes();
            blockingFactor = (int) Math.floor((double) (blockSize - 4) / recordSize);
            
            blockInfos = new ArrayList<>();
            BinaryFile binaryFile = new BinaryFile(heapFilePath);
            long fileSize = binaryFile.getSize();
            int totalBlocks = (int) (fileSize / blockSize);
            
            Set<Integer> emptyBlocks = new HashSet<>(blockManager.getEmptyBlocks());
            Set<Integer> partiallyEmptyBlocks = new HashSet<>(blockManager.getPartiallyEmptyBlocks());
            
            System.out.println("Reading " + totalBlocks + " blocks...");
            
            for (int i = 0; i < totalBlocks; i++) {
                Block<Person> block = readBlock(binaryFile, i, blockSize, blockingFactor);
                if (block != null) {
                    int validCount = block.getValidBlockCount();
                    String status;
                    if (validCount == 0) {
                        status = "Empty";
                    } else if (validCount < blockingFactor) {
                        status = "Partially Full";
                    } else {
                        status = "Full";
                    }
                    
                    boolean inEmptyList = emptyBlocks.contains(i);
                    boolean inPartialList = partiallyEmptyBlocks.contains(i);
                    if (validCount == 0 && !inEmptyList) {
                        System.err.println("WARNING: Block " + i + " is empty but not in emptyBlocks list");
                    } else if (validCount > 0 && validCount < blockingFactor && !inPartialList && !inEmptyList) {
                        System.err.println("WARNING: Block " + i + " is partially full but not in partiallyEmptyBlocks list");
                    } else if (validCount == blockingFactor && (inEmptyList || inPartialList)) {
                        System.err.println("WARNING: Block " + i + " is full but still in empty/partial list");
                    }
                    
                    List<Person> records = new ArrayList<>();
                    for (int j = 0; j < validCount; j++) {
                        Person record = block.getRecord(j);
                        if (record != null) {
                            records.add(record);
                        }
                    }
                    
                    blockInfos.add(new BlockInfo(i, status, validCount, blockingFactor, records));
                }
            }
            
            binaryFile.close();
            
            System.out.println("✓ Successfully loaded " + totalBlocks + " blocks.");
            System.out.println("  Block size: " + blockSize + " bytes");
            System.out.println("  Blocking factor: " + blockingFactor + " records per block");
            System.out.println("  Record size: " + recordSize + " bytes");
            
        } catch (Exception ex) {
            System.out.println("ERROR loading heap data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private Block<Person> readBlock(BinaryFile binaryFile, int blockNumber, 
                                   int blockSize, int blockingFactor) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        if (position >= binaryFile.getSize()) {
            return null;
        }
        
        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);
        
        Block<Person> block = new Block<>(blockingFactor, blockSize, Person.class);
        block.FromByteArray(blockData, Person.class);
        
        return block;
    }
    
    private void displayBlocksSummary() {
        if (blockInfos == null || blockInfos.isEmpty()) {
            System.out.println("\nNo data loaded. Please load heap data first (option 1).");
            return;
        }
        
        System.out.println("\n" + repeatString("=", 70));
        System.out.println("BLOCKS SUMMARY");
        System.out.println(repeatString("=", 70));
        System.out.printf("%-8s %-18s %-12s %-12s %-12s%n", 
            "Block #", "Status", "Records", "Capacity", "Usage");
        System.out.println(repeatString("-", 70));
        
        for (BlockInfo info : blockInfos) {
            double usagePercent = info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0;
            System.out.printf("%-8d %-18s %-12d %-12d %-12.1f%%%n",
                info.blockNumber, info.status, info.validCount, info.capacity, usagePercent);
        }
        
        System.out.println(repeatString("=", 70));
        System.out.println("Total blocks: " + blockInfos.size());
    }
    
    private void displayDetailedBlocks() {
        if (blockInfos == null || blockInfos.isEmpty()) {
            System.out.println("\nNo data loaded. Please load heap data first (option 1).");
            return;
        }
        
        System.out.println("\n" + repeatString("=", 70));
        System.out.println("DETAILED BLOCK INFORMATION");
        System.out.println(repeatString("=", 70));
        
        for (BlockInfo info : blockInfos) {
            displayBlockDetails(info);
            System.out.println();
        }
    }
    
    private void displaySpecificBlock(Scanner scanner) {
        if (blockInfos == null || blockInfos.isEmpty()) {
            System.out.println("\nNo data loaded. Please load heap data first (option 1).");
            return;
        }
        
        System.out.print("\nEnter block number to display: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine().trim());
            
            BlockInfo info = null;
            for (BlockInfo bi : blockInfos) {
                if (bi.blockNumber == blockNum) {
                    info = bi;
                    break;
                }
            }
            
            if (info != null) {
                displayBlockDetails(info);
            } else {
                System.out.println("Block " + blockNum + " not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid block number.");
        }
    }
    
    private void displayBlockDetails(BlockInfo info) {
        System.out.println(repeatString("=", 70));
        System.out.println("BLOCK #" + info.blockNumber);
        System.out.println(repeatString("-", 70));
        System.out.println("Status:        " + info.status);
        System.out.println("Valid Records: " + info.validCount + " / " + info.capacity);
        System.out.println("Usage:         " + String.format("%.1f%%", 
            (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0)));
        System.out.println("Block Size:    " + blockSize + " bytes");
        System.out.println(repeatString("-", 70));
        
        if (info.records.isEmpty()) {
            System.out.println("RECORDS: (No records in this block)");
        } else {
            System.out.println("RECORDS:");
            System.out.println(repeatString("-", 70));
            for (int i = 0; i < info.records.size(); i++) {
                Person person = info.records.get(i);
                System.out.println("  Record #" + (i + 1) + ":");
                System.out.println("    Name:      " + (person.name != null ? person.name : "(null)"));
                System.out.println("    Surname:   " + (person.surname != null ? person.surname : "(null)"));
                System.out.println("    Birthdate: " + person.birthdate);
                System.out.println("    ID:        " + (person.id != null ? person.id : "(null)"));
                if (i < info.records.size() - 1) {
                    System.out.println();
                }
            }
        }
        System.out.println(repeatString("=", 70));
    }
    
    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}

