package javaapplication1;
import java.util.List;

class PersonInfo implements TreeNodeData {

    public Double accountSavings;

    public String name;

    PersonInfo(String name, Double accountSavings) {
        this.name = name;
        this.accountSavings = accountSavings; 
    }
    
    @Override public int compare(TreeNodeData otherKey) {
        Double valueToCompare = ((PersonInfo)otherKey).accountSavings;
        return accountSavings.compareTo(valueToCompare);
    }

    @Override public String toString() {
        String displayName = name == null ? "<unknown>" : name+""+accountSavings;
        return displayName ;
    }

}

public class main {
    public static void main(String[] args) {

        System.out.println("=== Testing BSTree Range Search ===");
       
        AVLTree tree = new AVLTree();

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
        PersonInfo entryToRemove = new PersonInfo(null, 10_000.0);

        tree.printTree();
        tree.delete(entryToRemove);
        
       tree.printTree();
    
       PersonInfo minKey = new PersonInfo(null, .0);
       PersonInfo maxKey = new PersonInfo(null, 50_000.0);
       
       List<TreeNodeData> rangeResults = tree.findInRange(minKey, maxKey);
       System.out.println("People with savings between 1,000 and 50,000:");
       for (TreeNodeData data : rangeResults) {
           PersonInfo person = (PersonInfo) data;
           System.out.println("- " + person.name + ": $" + person.accountSavings);
       }

     }
}
