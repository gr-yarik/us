package javaapplication1;

class PersonInfo implements BSTreeNodeData {

    public Double accountSavings;

    public String name;

    PersonInfo(String name, Double accountSavings) {
        this.name = name;
        this.accountSavings = accountSavings; 
    }
    
    @Override public int compare(BSTreeNodeData otherKey) {
        Double valueToCompare = ((PersonInfo)otherKey).accountSavings;
        return accountSavings.compareTo(valueToCompare);
    }

    @Override public String toString() {
        String displayName = name == null ? "<unknown>" : name;
        return displayName + " : $" + String.format("%,.2f", accountSavings);
    }

}

public class main {
    public static void main(String[] args) {

        System.out.println("=== Testing BSTree Range Search ===");
        BSTree tree = new BSTree();
       
        tree.insert(new PersonInfo("Yaroslav", 1_000.0));
        tree.insert(new PersonInfo("Martin", 1.0));
        tree.insert(new PersonInfo("Vlad", 100_000_000.0));        
        tree.insert(new PersonInfo("Andrew", 10_000.0));
        tree.insert(new PersonInfo("Sarah", 5_000.0));
        tree.insert(new PersonInfo("John", 50_000.0));

        PersonInfo entryToFind = new PersonInfo(null, 10_000.0);
        BSTreeNodeData found = tree.find(entryToFind);
        if (found != null) {
            System.out.println("Found person with 10,000 savings: " + ((PersonInfo)found).name);
        } else {
            System.out.println("No person found with 10,000 savings");
        }
        
        tree.printTree();
       
       System.out.println("\n=== Range Search Results ===");
       PersonInfo minKey = new PersonInfo(null, 1_000.0);
       PersonInfo maxKey = new PersonInfo(null, 50_000.0);
       
       java.util.List<BSTreeNodeData> rangeResults = tree.findInRange(minKey, maxKey);
       System.out.println("People with savings between 1,000 and 50,000:");
       for (BSTreeNodeData data : rangeResults) {
           PersonInfo person = (PersonInfo) data;
           System.out.println("- " + person.name + ": $" + person.accountSavings);
       }
//
//        System.out.println("\n=== Testing AVLTree Range Search ===");
//        AVLTree avlTree = new AVLTree();
//        
//        avlTree.insert(new PersonInfo("Alice", 2_000.0));
//        avlTree.insert(new PersonInfo("Bob", 15_000.0));
//        avlTree.insert(new PersonInfo("Charlie", 8_000.0));
//        avlTree.insert(new PersonInfo("Diana", 25_000.0));
//        avlTree.insert(new PersonInfo("Eve", 3_000.0));
//
//        PersonInfo avlMinKey = new PersonInfo(null, 5_000.0);
//        PersonInfo avlMaxKey = new PersonInfo(null, 20_000.0);
//        
//        java.util.List<BSTreeNodeData> avlRangeResults = avlTree.findInRange(avlMinKey, avlMaxKey);
//        System.out.println("People with savings between 5,000 and 20,000 (AVL Tree):");
//        for (BSTreeNodeData data : avlRangeResults) {
//            PersonInfo person = (PersonInfo) data;
//            System.out.println("- " + person.name + ": $" + person.accountSavings);
//        }

     }
}
