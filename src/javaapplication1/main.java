package javaapplication1;

class PersonInfo implements BSTreeNodeData {

    private Double accountSavings;

    public String name;

    PersonInfo(String name, Double accountSavings) {
        this.name = name;
        this.accountSavings = accountSavings; 
    }
    
    @Override public int compare(BSTreeNodeData otherKey) {
        Double valueToCompare = ((PersonInfo)otherKey).accountSavings;
        return accountSavings.compareTo(valueToCompare);
    }

}

public class main {
    public static void main(String[] args) {

        BSTree tree = new BSTree();
       
        tree.insert(new PersonInfo("Yaroslav", 1_000.0));
        tree.insert(new PersonInfo("Martin", 1.0));
        tree.insert(new PersonInfo("Vlad", 100_000_000.0));        
        tree.insert(new PersonInfo("Andrew", 10_000.0));

        PersonInfo entryToFind = new PersonInfo(null, 10_000.0);

        BSTreeNodeData found = tree.find(entryToFind);

        System.out.print(((PersonInfo)found).name);

     }
}
