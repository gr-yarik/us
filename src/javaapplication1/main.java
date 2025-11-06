package javaapplication1;
import java.util.List;

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
        return displayName ;//+ " : $" + String.format("%,.2f", accountSavings);
    }

}

public class main {
    public static void main(String[] args) {

        System.out.println("=== Testing BSTree Range Search ===");
        BSTree tree = new BSTree();
       
        tree.insert(new PersonInfo("Yaro", 1_000.0));
        tree.insert(new PersonInfo("Mato", 1.0));
        tree.insert(new PersonInfo("Vlad", 100_000_000.0));        
        tree.insert(new PersonInfo("Andy", 10_000.0));
        tree.insert(new PersonInfo("Sara", 5_000.0));
        tree.insert(new PersonInfo("Jon", 50_000.0));
        tree.insert(new PersonInfo("Ken", 500.0));
        tree.insert(new PersonInfo("Jack", 900.0));
        tree.insert(new PersonInfo("Sam", 400.0));



        PersonInfo entryToFind = new PersonInfo(null, 400.0);


        // BSTreeNodeData found = tree.find(entryToFind);
        // if (found != null) {
        //     System.out.println("Found person with 10,000 savings: " + ((PersonInfo)found).name);
        // } else {
        //     System.out.println("No person found with 10,000 savings");
        // }
        
       tree.printTree();
    
    //    tree.inorderTraversal(data -> {
    //     PersonInfo p = (PersonInfo) data;
    //     System.out.println(p.name + " : $" + String.format("%,.2f", p.accountSavings));
    //     return true; // continue traversing
    // }
    // );

        // tree.inorderFromInclusive(entryToFind, data -> {
        //     PersonInfo p = (PersonInfo) data;
        //     System.out.println(p.name + " : $" + String.format("%,.2f", p.accountSavings));
        //     return true; // continue traversing
        // });

       
    //    System.out.println("\n=== Range Search Results ===");
       PersonInfo minKey = new PersonInfo(null, -6.0);
       PersonInfo maxKey = new PersonInfo(null, 50_000.0);
       
       List<BSTreeNodeData> rangeResults = tree.findInRange(minKey, maxKey);
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
