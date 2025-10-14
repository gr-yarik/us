
package javaapplication1;

public class BSTree {

    private BSTreeNode root;

    public BSTree() {
        this.root = null;
    }

    public void insert(BSTreeNodeData newData) {
        TryFindRecord searchResult = tryFind(newData);

        if (searchResult.found) {
            System.out.println("Error! Key is already present in the tree: " + newData);
            return;
        }

        if (root == null) {
            root = new BSTreeNode();
            root.data = newData;
        } else {
            BSTreeNode newChild = new BSTreeNode();
            BSTreeNode searchStoppedAtNode = searchResult.searchStoppedAtNode;
            newChild.data = newData;
            newChild.parent = searchStoppedAtNode;
            switch (searchResult.side) {
                case LEFT:
                    searchStoppedAtNode.leftChild = newChild;        
                    break;
                case RIGHT:
                    searchStoppedAtNode.rightChild = newChild;
                    break;
                default:
                    break;
            }
            

        }


    }
    enum ChildSide {
        RIGHT, LEFT;
    }
    record TryFindRecord(boolean found, BSTreeNode searchStoppedAtNode, ChildSide side) {}

    private TryFindRecord tryFind(BSTreeNodeData key){

        if (root == null) {
            return new TryFindRecord(false, null, null);
        }

        BSTreeNode currentNode = root;

        while (true) {

            int comparisonResult = currentNode.data.compare(key);

            if (comparisonResult > 0) {

                if (currentNode.rightChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.RIGHT);    
                }

                currentNode = currentNode.rightChild;
             } else if (comparisonResult < 0) {

                if (currentNode.leftChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.LEFT);    
                }
                currentNode = currentNode.leftChild;
             } else if (comparisonResult == 0) {
                return new TryFindRecord(true, currentNode, null);
             }
        }
    
    }

    public BSTreeNodeData find(BSTreeNodeData key){
        TryFindRecord record = tryFind(key);
        if(record.found) {
            return record.searchStoppedAtNode.data;
        }
        return null;
    }
    
}
