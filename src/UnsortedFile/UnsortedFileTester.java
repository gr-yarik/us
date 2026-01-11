package UnsortedFile;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Scanner;


class Person implements StorableRecord {
 
    static int sizeInBytes = 16 + 16 + 8 + 16; //56
    
    String name;//max15
    String surname; //max15
    long birthdate; 
    String id; //max15

    @Override
    public boolean equals(StorableRecord record) {
        if (record == null || !(record instanceof Person)) {
            return false;
        }
        Person other = (Person) record;
        if (this.id == null) {
            return other.id == null;
        }
        return this.id.equals(other.id);
    }

    @Override
    public int sizeInBytes() {
        return sizeInBytes;
    }

    public byte[] ToByteArray() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try{
            dataOutputStream.writeLong(birthdate);
            byte[] nameBytes = (name != null ? name : "").getBytes(StandardCharsets.US_ASCII);
            int nameLength = Math.min(nameBytes.length, 15);
            byte[] nameFixed = new byte[15];
            System.arraycopy(nameBytes, 0, nameFixed, 0, nameLength);
            dataOutputStream.write(nameFixed);
            dataOutputStream.writeByte(nameLength);
            byte[] surnameBytes = (surname != null ? surname : "").getBytes(StandardCharsets.US_ASCII);
            int surnameLength = Math.min(surnameBytes.length, 15);
            byte[] surnameFixed = new byte[15];
            System.arraycopy(surnameBytes, 0, surnameFixed, 0, surnameLength);
            dataOutputStream.write(surnameFixed);
            dataOutputStream.writeByte(surnameLength);
            byte[] idBytes = (id != null ? id : "").getBytes(StandardCharsets.US_ASCII);
            int idLength = Math.min(idBytes.length, 15);
            byte[] idFixed = new byte[15];
            System.arraycopy(idBytes, 0, idFixed, 0, idLength);
            dataOutputStream.write(idFixed);
            dataOutputStream.writeByte(idLength);
            return byteArrayOutputStream.toByteArray();
        }catch (Exception e){
            throw new IllegalStateException("Error during conversion to byte array.");
        }
    }
  
  public void FromByteArray(byte[] inputArray) {
          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputArray);
          DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
          try {
              this.birthdate = dataInputStream.readLong();
              byte[] nameBytes = new byte[15];
              dataInputStream.readFully(nameBytes);
              int nameLength = dataInputStream.readByte() & 0xFF;
              this.name = new String(nameBytes, 0, nameLength, StandardCharsets.US_ASCII);
              byte[] surnameBytes = new byte[15];
              dataInputStream.readFully(surnameBytes);
              int surnameLength = dataInputStream.readByte() & 0xFF;
              this.surname = new String(surnameBytes, 0, surnameLength, StandardCharsets.US_ASCII);
              byte[] idBytes = new byte[15];
              dataInputStream.readFully(idBytes);
              int idLength = dataInputStream.readByte() & 0xFF;
              this.id = new String(idBytes, 0, idLength, StandardCharsets.US_ASCII);
  
          } catch (IOException e) {
  throw new IllegalStateException("Error during conversion from byte array.");
  }
  }

}

public class UnsortedFileTester {
    private static final String HEAP_FILE_PATH = "test_heap.bin";
    private static final String METADATA_FILE_PATH = "test_heap.bin.meta";
    private static final int BLOCK_SIZE = 512;
    
    public static void main(String[] args) {
        System.out.println("=== Heap Test ===");
        
        try {
            File heapFile = new File(HEAP_FILE_PATH);
            File metaFile = new File(METADATA_FILE_PATH);
            boolean filesExist = heapFile.exists() && metaFile.exists() && heapFile.length() > 0 && metaFile.length() > 0;
            
            Heap<Person> heap;
            
            if (!filesExist) {
                System.out.println("\n=== Creating new Heap (clean state) ===");
                heap = createHeapFromCleanState();
            } else {
                System.out.println("\n=== Loading Heap from existing files ===");
                heap = loadHeapFromExistingFiles();
            }
           
            runInteractiveMenu(heap);
            
            heap.close();
            System.out.println("\nHeap closed successfully.");
            
        } catch (Exception e) {
            System.err.println("\n✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Heap<Person> createHeapFromCleanState() throws Exception {
        System.out.println("Creating new Heap with:");
        System.out.println("  Heap file: " + HEAP_FILE_PATH);
        System.out.println("  Block size: " + BLOCK_SIZE + " bytes");
        
        Heap<Person> heap = new Heap<>(HEAP_FILE_PATH, BLOCK_SIZE, Person.class);
        System.out.println("✓ Heap created successfully.");
        
        return heap;
    }
    
    private static Heap<Person> loadHeapFromExistingFiles() throws Exception {
        System.out.println("Loading Heap from existing files:");
        System.out.println("  Heap file: " + HEAP_FILE_PATH);
        System.out.println("  Metadata file: " + METADATA_FILE_PATH);
        
        Heap<Person> heap = new Heap<>(HEAP_FILE_PATH, METADATA_FILE_PATH, Person.class);
        System.out.println("✓ Heap loaded successfully from metadata file.");
        
        return heap;
    }
    
    private static void populateHeapWithPeople(Heap<Person> heap, int count) {
        System.out.println("\n=== Populating heap with " + count + " people ===");
        
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
        
        int insertedCount = 0;
        int failedCount = 0;
        
        for (int i = 0; i < count; i++) {
            try {
                Person person = new Person();
                person.name = firstNames[i % firstNames.length];
                person.surname = lastNames[i % lastNames.length];
                int year = 1950 + (i % 51);
                int month = 1 + (i % 12);
                int day = 1 + (i % 28);
                person.birthdate = Long.parseLong(String.format("%04d%02d%02d", year, month, day));
                person.id = "ID" + String.format("%08d", 10000000 + i);
                
                int blockNumber = heap.insert(person);
                insertedCount++;
                
                if ((i + 1) % 10 == 0) {
                    System.out.println("  Inserted " + (i + 1) + " people...");
                }
            } catch (Exception e) {
                failedCount++;
                System.err.println("  Failed to insert person #" + (i + 1) + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n✓ Population complete!");
        System.out.println("  Successfully inserted: " + insertedCount + " people");
        if (failedCount > 0) {
            System.out.println("  Failed: " + failedCount + " people");
        }
    }
    
    private static Person generatePersonFromId(String id) {
        Person person = new Person();
        person.id = id;
        
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
        
        int hash = Math.abs(id.hashCode());
        person.name = firstNames[hash % firstNames.length];
        person.surname = lastNames[(hash * 7) % lastNames.length];
        
        int year = 1950 + (hash % 51);
        int month = 1 + ((hash * 3) % 12);
        int day = 1 + ((hash * 5) % 28);
        person.birthdate = Long.parseLong(String.format("%04d%02d%02d", year, month, day));
        
        return person;
    }
    
    public static int insertUserById(Heap<Person> heap, String id) {
        try {
            Person person = generatePersonFromId(id);
            int blockNumber = heap.insert(person);
            System.out.println("✓ Inserted user with ID: " + id + " at block " + blockNumber);
            return blockNumber;
        } catch (Exception e) {
            System.err.println("✗ Failed to insert user with ID: " + id + " - " + e.getMessage());
            return -1;
        }
    }
    
    public static Person findUserById(Heap<Person> heap, String id, int blockNumber) {
        try {
            Person searchKey = new Person();
            searchKey.id = id;
            
            Person found = heap.get(blockNumber, searchKey);
            if (found != null) {
                System.out.println("✓ Found user with ID: " + id + " in block " + blockNumber);
            } else {
                System.out.println("✗ User with ID: " + id + " not found in block " + blockNumber);
            }
            return found;
            
        } catch (Exception e) {
            System.err.println("✗ Error finding user with ID: " + id + " - " + e.getMessage());
            return null;
        }
    }
    
    public static boolean deleteUserById(Heap<Person> heap, String id, int blockNumber) {
        try {
            Person searchKey = new Person();
            searchKey.id = id;
            
            boolean deleted = heap.delete(blockNumber, searchKey, null);
            if (deleted) {
                System.out.println("✓ Deleted user with ID: " + id + " from block " + blockNumber);
            } else {
                System.out.println("✗ User with ID: " + id + " not found in block " + blockNumber);
            }
            return deleted;
            
        } catch (Exception e) {
            System.err.println("✗ Error deleting user with ID: " + id + " - " + e.getMessage());
            return false;
        }
    }
    
    private static void runInteractiveMenu(Heap<Person> heap) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n" + repeatString("=", 60));
            System.out.println("HEAP OPERATIONS MENU");
            System.out.println(repeatString("=", 60));
            System.out.println("1. Insert user by ID");
            System.out.println("2. Find user by ID");
            System.out.println("3. Delete user by ID");
            System.out.println("4. Populate heap with multiple users");
            System.out.println("0. Exit");
            System.out.print("\nSelect option: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    handleInsertUser(heap, scanner);
                    break;
                case "2":
                    handleFindUser(heap, scanner);
                    break;
                case "3":
                    handleDeleteUser(heap, scanner);
                    break;
                case "4":
                    handlePopulateHeap(heap, scanner);
                    break;
                case "0":
                    System.out.println("Exiting menu...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void handleInsertUser(Heap<Person> heap, Scanner scanner) {
        System.out.println("\n--- Insert User by ID ---");
        System.out.print("Enter user ID: ");
        String id = scanner.nextLine().trim();
        
        if (id.isEmpty()) {
            System.out.println("✗ Error: ID cannot be empty.");
            return;
        }
        
        int blockNumber = insertUserById(heap, id);
        if (blockNumber != -1) {
            System.out.println("  Block number: " + blockNumber);
        }
    }
    
    private static void handleFindUser(Heap<Person> heap, Scanner scanner) {
        System.out.println("\n--- Find User by ID ---");
        System.out.print("Enter user ID to search: ");
        String id = scanner.nextLine().trim();
        
        if (id.isEmpty()) {
            System.out.println("✗ Error: ID cannot be empty.");
            return;
        }
        
        System.out.print("Enter block number where to search: ");
        String blockInput = scanner.nextLine().trim();
        
        if (blockInput.isEmpty()) {
            System.out.println("✗ Error: Block number cannot be empty.");
            return;
        }
        
        int blockNumber;
        try {
            blockNumber = Integer.parseInt(blockInput);
            if (blockNumber < 0) {
                System.out.println("✗ Error: Block number must be non-negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("✗ Error: Invalid block number format.");
            return;
        }
        
        Person found = findUserById(heap, id, blockNumber);
        if (found != null) {
            System.out.println("\nUser Details:");
            System.out.println("  Name:      " + (found.name != null ? found.name : "(null)"));
            System.out.println("  Surname:   " + (found.surname != null ? found.surname : "(null)"));
            System.out.println("  Birthdate: " + found.birthdate);
            System.out.println("  ID:        " + (found.id != null ? found.id : "(null)"));
        }
    }
    
    private static void handleDeleteUser(Heap<Person> heap, Scanner scanner) {
        System.out.println("\n--- Delete User by ID ---");
        System.out.print("Enter user ID to delete: ");
        String id = scanner.nextLine().trim();
        
        if (id.isEmpty()) {
            System.out.println("✗ Error: ID cannot be empty.");
            return;
        }
        
        System.out.print("Enter block number where the user is located: ");
        String blockInput = scanner.nextLine().trim();
        
        if (blockInput.isEmpty()) {
            System.out.println("✗ Error: Block number cannot be empty.");
            return;
        }
        
        int blockNumber;
        try {
            blockNumber = Integer.parseInt(blockInput);
            if (blockNumber < 0) {
                System.out.println("✗ Error: Block number must be non-negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("✗ Error: Invalid block number format.");
            return;
        }
        
        System.out.print("Are you sure you want to delete user with ID \"" + id + "\" from block " + blockNumber + "? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("yes") || confirmation.equals("y")) {
            boolean deleted = deleteUserById(heap, id, blockNumber);
            if (deleted) {
                System.out.println("✓ User successfully deleted.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }
    
    private static void handlePopulateHeap(Heap<Person> heap, Scanner scanner) {
        System.out.println("\n--- Populate Heap ---");
        System.out.print("Enter number of users to insert (default: 30): ");
        String input = scanner.nextLine().trim();
        
        int count = 30;
        if (!input.isEmpty()) {
            try {
                count = Integer.parseInt(input);
                if (count <= 0) {
                    System.out.println("✗ Error: Number must be positive. Using default: 30");
                    count = 30;
                }
            } catch (NumberFormatException e) {
                System.out.println("✗ Error: Invalid number. Using default: 30");
                count = 30;
            }
        }
        
        populateHeapWithPeople(heap, count);
    }
    
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
}

