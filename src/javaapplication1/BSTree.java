
package javaapplication1;

public class BSTree {

    private BSTreeNode root;

    public BSTree() {
        this.root = null;
    }

    public void delete(BSTreeNodeData data) {
        TryFindRecord result = tryFind(data);

        if(result.found) {
            delete(result.searchStoppedAtNode);
        }
    }

    private void delete(BSTreeNode node) {
        if (node == null) return;

        if (node.leftChild == null && node.rightChild == null) {
            
            if (node.parent == null) {
                root = null;
            } else if (node.parent.leftChild == node) {
                node.parent.leftChild = null;
            } else {
                node.parent.rightChild = null;
            }

        } else if (node.leftChild == null || node.rightChild == null) {
            
            BSTreeNode child = (node.leftChild != null) ? node.leftChild : node.rightChild;

            if (node.parent == null) {
                root = child;
            } else if (node.parent.leftChild == node) {
                node.parent.leftChild = child;
            } else {
                node.parent.rightChild = child;
            }

            if (child != null) {
                child.parent = node.parent;
            }

        } else {
            BSTreeNode successor = node.rightChild;
            while (successor.leftChild != null) {
                successor = successor.leftChild;
            }
            
            node.data = successor.data;
          
            delete(successor);
        }
    }

    void rotateLeft(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.rightChild == null) return;

        BSTreeNode newParent = pivotNode.rightChild;
        BSTreeNode leftSubtreeOfNewParent = newParent.leftChild;

        newParent.parent = pivotNode.parent;
        if (pivotNode.parent == null) {
            root = newParent;
        } else if (pivotNode.parent.leftChild == pivotNode) {
            pivotNode.parent.leftChild = newParent;
        } else {
            pivotNode.parent.rightChild = newParent;
        }

        pivotNode.rightChild = leftSubtreeOfNewParent;
        if (leftSubtreeOfNewParent != null) {
            leftSubtreeOfNewParent.parent = pivotNode;
        }

        newParent.leftChild = pivotNode;
        pivotNode.parent = newParent;
    }

    void rotateRight(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.leftChild == null) return;

        BSTreeNode newParent = pivotNode.leftChild;
        BSTreeNode rightSubtreeOfNewParent = newParent.rightChild;

        newParent.parent = pivotNode.parent;
        if (pivotNode.parent == null) {
            root = newParent;
        } else if (pivotNode.parent.leftChild == pivotNode) {
            pivotNode.parent.leftChild = newParent;
        } else {
            pivotNode.parent.rightChild = newParent;
        }

        pivotNode.leftChild = rightSubtreeOfNewParent;
        if (rightSubtreeOfNewParent != null) {
            rightSubtreeOfNewParent.parent = pivotNode;
        }

        newParent.rightChild = pivotNode;
        pivotNode.parent = newParent;
    }


    public void insert(BSTreeNodeData newData) {
        TryFindRecord searchResult = tryFind(newData);

        if (searchResult.found) {
            System.out.println("Error - key is already present in the tree: " + newData);
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
                    return new TryFindRecord(false, currentNode, ChildSide.LEFT);    
                }

                currentNode = currentNode.rightChild;
             } else if (comparisonResult < 0) {

                if (currentNode.leftChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.RIGHT);    
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
    
    public void inorderTraversal(java.util.function.Consumer<BSTreeNodeData> action) {
        inorderTraversal(root, action);
    }
    
    private void inorderTraversal(BSTreeNode node, java.util.function.Consumer<BSTreeNodeData> action) {
        if (node != null) {
            inorderTraversal(node.leftChild, action);
            action.accept(node.data);
            inorderTraversal(node.rightChild, action);
        }
    }
    
}
