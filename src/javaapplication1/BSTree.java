
package javaapplication1;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class BSTree {

    protected BSTreeNode root;

    public BSTree() {
        this.root = null;
    }

    public void insert(BSTreeNodeData newData) {
        insert(newData, null);
    }

    public void delete(BSTreeNodeData data) {

        TryFindRecord result = tryFind(data, null);

        if(result.found()) {
            delete(result.searchStoppedAtNode());
        } else {
            System.out.println("Could not delete: " + data);
        }
        
    }

    protected void delete(BSTreeNode node) {
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
            BSTreeNode successor = findMinimum(node.rightChild);
            node.data = successor.data;
            delete(successor);
        }
    }

    protected void rotateLeft(BSTreeNode pivotNode) {
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

    protected void rotateRight(BSTreeNode pivotNode) {
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
    
    protected void insert(BSTreeNodeData newData, Consumer<BSTreeNode> operation) {
        TryFindRecord searchResult = tryFind(newData, operation);

        if (searchResult.found()) {
            System.out.println("Error - key is already present in the tree: " + newData);
            return;
        }

        if (root == null) {
            root = createNode();
            root.data = newData;
        } else {
            BSTreeNode newChild = createNode();
            BSTreeNode searchStoppedAtNode = searchResult.searchStoppedAtNode();
            newChild.data = newData;
            newChild.parent = searchStoppedAtNode;
            switch (searchResult.side()) {
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
    public record TryFindRecord(boolean found, BSTreeNode searchStoppedAtNode, ChildSide side) {}
    
    protected TryFindRecord tryFind(BSTreeNodeData key, Consumer<BSTreeNode> operation){

        if (root == null) {
            return new TryFindRecord(false, null, null);
        }

        BSTreeNode currentNode = root;

        while (true) {
            if (operation != null) {
                operation.accept(currentNode);
            }

            int comparisonResult = currentNode.data.compare(key);

            if (comparisonResult > 0) {
                if (currentNode.leftChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.LEFT);    
                }
                currentNode = currentNode.leftChild;
             } else if (comparisonResult < 0) {
                if (currentNode.rightChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.RIGHT);    
                }
                currentNode = currentNode.rightChild;
             } else if (comparisonResult == 0) {
                return new TryFindRecord(true, currentNode, null);
             }
        }
    
    }

    public BSTreeNodeData find(BSTreeNodeData key){
        TryFindRecord record = tryFind(key, null);
        if(record.found()) {
            return record.searchStoppedAtNode().data;
        }
        System.out.println("Key not found: " + key);
        return null;
    }
    
    public void inorderTraversal(Consumer<BSTreeNodeData> action) {
        inorderTraversal(root, action);
    }
    
    protected BSTreeNode findMinimum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.leftChild != null) {
                current = current.leftChild;
            } else {
                break;
            }
        }
        
        return current;
    }

    protected BSTreeNode findMaximum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.rightChild != null) {
                current = current.rightChild;
            } else {
                break;
            }
        }
        
        return current;
    }

    protected void inorderTraversal(BSTreeNode node, Consumer<BSTreeNodeData> action) {
        if (node == null) {
            return;
        }

        BSTreeNode current = findMinimum(node);
        
        while (current != null) {
            action.accept(current.data);
            
            if (current.rightChild != null) {
                current = current.rightChild;
                while (current.leftChild != null) {
                    current = current.leftChild;
                }
            } else {
                BSTreeNode parent = current.parent;
                
                while (parent != null && current == parent.rightChild) {
                    current = parent;
                    parent = parent.parent;
                }
                
                current = parent;
            }
        }
    }
    
    protected BSTreeNode createNode() {
        return new BSTreeNode();
    }
    
    public int getHeight() {
        return getHeight(root);
    }
    
    protected int getHeight(BSTreeNode startingAtNode) {
        if (startingAtNode == null) {
            return -1;
        }

        int maxDepth = 0;
        int depth = 0;

        BSTreeNode current = startingAtNode;
        BSTreeNode previous = null;

        while (current != null) {
            if (previous == current.parent) {
                if (current.leftChild != null) {
                    previous = current;
                    current = current.leftChild;
                    depth++;
                    continue;
                } else if (current.rightChild != null) {
                    previous = current;
                    current = current.rightChild;
                    depth++;
                    continue;
                } else {
                    if (depth > maxDepth) {
                        maxDepth = depth;
                    }
                    previous = current;
                    current = current.parent;
                    depth--;
                    continue;
                }
            } else if (previous == current.leftChild) {
                if (current.rightChild != null) {
                    previous = current;
                    current = current.rightChild;
                    depth++;
                    continue;
                } else {
                    previous = current;
                    current = current.parent;
                    depth--;
                    continue;
                }
            } else {
                previous = current;
                current = current.parent;
                depth--;
            }
        }

        return maxDepth;
    }
    
    public boolean isEmpty() {
        return root == null;
    }
    
    protected BSTreeNode getRoot() {
        return root;
    }
    
    protected void setRoot(BSTreeNode newRoot) {
        this.root = newRoot;
    }
    
    
    public List<BSTreeNodeData> findInRange(BSTreeNodeData minKey, BSTreeNodeData maxKey) {
        List<BSTreeNodeData> results = new ArrayList<>();
        if (root == null) {
            return results;
        }
    
        BSTreeNode current = findMinimum(root);
        
        while (current != null) {
            int minComparison = current.data.compare(minKey);
            int maxComparison = current.data.compare(maxKey);
            
            if (minComparison >= 0 && maxComparison <= 0) {
                results.add(current.data);
            }
            
            if (maxComparison > 0) {
                break;
            }
            
            if (current.rightChild != null) {
                current = current.rightChild;
                while (current.leftChild != null) {
                    current = current.leftChild;
                }
            } else {
                BSTreeNode parent = current.parent;
                
                while (parent != null && current == parent.rightChild) {
                    current = parent;
                    parent = parent.parent;
                }
                
                current = parent;
            }
        }
    
        return results;
    }
    
    public BSTreeNodeData findMin() {
        if (root == null) {
            return null;
        }
        
        BSTreeNode current = root;
        while (current.leftChild != null) {
            current = current.leftChild;
        }
        return current.data;
    }
    
    public BSTreeNodeData findMax() {
        if (root == null) {
            return null;
        }
        
        BSTreeNode current = root;
        while (current.rightChild != null) {
            current = current.rightChild;
        }
        return current.data;
    }
    
}
