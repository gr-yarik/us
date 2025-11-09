package javaapplication1;

public class AVLTree extends BSTree {
    
    @Override
    protected BSTreeNode createNode() {
        return new AVLTreeNode();
    }
    
    @Override
    public void insert(TreeNodeData newData) {
        AVLTreeNode newNode = (AVLTreeNode) super.insert(newData, null);
        if (newNode != null) {
            updateBalanceFactorsAndRebalance(newNode);
        }
    }
    
    @Override
    public void delete(TreeNodeData data) {
        
        TryFindRecord result = tryFind(data, null);
        
        if (!result.found()) {
            System.out.println("Didn't find a node with this key ");
            return;
        }
        
        AVLTreeNode nodeToDelete = (AVLTreeNode) result.searchStoppedAtNode();
        deleteAVL(nodeToDelete);
    }
    
    private void deleteAVL(AVLTreeNode nodeToDelete) {
        if (nodeToDelete == null) return;
        
        DeletionRecord result = deleteNode(nodeToDelete);
        
        if (result == null || result.side() == null) {
            return;
        }
        
        AVLTreeNode parent = (AVLTreeNode) result.parentNode();
        if (parent != null) {
            updateBalanceFactorsAndRebalanceAfterDelete(parent, result.side());
        }
    }
    
    private void updateBalanceFactorsAndRebalance(AVLTreeNode newNode) {
        AVLTreeNode currentNode = newNode.getParent();
        AVLTreeNode previousNode = newNode;
        
        while (currentNode != null) {
            ChildSide side = (currentNode.getLeftChild() == previousNode) ? ChildSide.LEFT : ChildSide.RIGHT;
            
            boolean heightIncreased = currentNode.updateBalanceFactorIncremental(side);
            
            if (!currentNode.isBalanced()) {
                rebalance(currentNode);
                break;
            }
            
            if (!heightIncreased) {
                break;
            }
            
            previousNode = currentNode;
            currentNode = currentNode.getParent();
        }
    }
    
    private void updateBalanceFactorsAndRebalanceAfterDelete(AVLTreeNode startNode, ChildSide deletedSide) {
        AVLTreeNode currentNode = startNode;
        ChildSide side = deletedSide;
        
        while (currentNode != null) {
            boolean heightDecreased = currentNode.updateBalanceFactorDecremental(side);
            
            if (!currentNode.isBalanced()) {
                AVLTreeNode parent = currentNode.getParent();
                
                ChildSide wasOnSide = null;
                if (parent != null) {
                    wasOnSide = (parent.getLeftChild() == currentNode) ? ChildSide.LEFT : ChildSide.RIGHT;
                }
                
                rebalance(currentNode);
                
                if (parent != null) {
                    boolean parentHeightDecreased = parent.updateBalanceFactorDecremental(wasOnSide);
                    
                    currentNode = parent;
                    side = wasOnSide;
                    
                    if (!parentHeightDecreased) {
                        break;
                    }
                    continue;
                } else {
                    break;
                }
            }
            
            if (!heightDecreased) {
                break;
            }
            
            AVLTreeNode parent = currentNode.getParent();
            if (parent != null) {
                side = (parent.getLeftChild() == currentNode) ? ChildSide.LEFT : ChildSide.RIGHT;
            }
            currentNode = parent;
        }
    }
    
    private void rebalance(AVLTreeNode node) {
        if (node.isRightHeavy()) {
            AVLTreeNode rightChild = node.getRightChild();
            if (rightChild != null && rightChild.isLeftHeavy()) {
                rotateRight(rightChild);
                rotateLeft(node);
            } else {
                rotateLeft(node);
            }
        } else if (node.isLeftHeavy()) {
            AVLTreeNode leftChild = node.getLeftChild();
            if (leftChild != null && leftChild.isRightHeavy()) {
                rotateLeft(leftChild);
                rotateRight(node);
            } else {
                rotateRight(node);
            }
        }
    }
    
    @Override
    protected void rotateLeft(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.getRightChild() == null) return;
        
        AVLTreeNode pivot = (AVLTreeNode) pivotNode;
        AVLTreeNode rightChild = pivot.getRightChild();
        
        byte pivotBF = pivot.balanceFactor;
        byte rightBF = rightChild.balanceFactor;
        
        super.rotateLeft(pivotNode);
        
        if (rightBF >= 0) {
            pivot.balanceFactor = (byte) (pivotBF - 1 - rightBF);
            rightChild.balanceFactor = (byte) (rightBF - 1);
        } else {
            pivot.balanceFactor = (byte) (pivotBF - 1);
            rightChild.balanceFactor = (byte) (rightBF - 1 + pivotBF);
        }
    }
    
    @Override
    protected void rotateRight(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.getLeftChild() == null) return;
        
        AVLTreeNode pivot = (AVLTreeNode) pivotNode;
        AVLTreeNode leftChild = pivot.getLeftChild();
        
        byte pivotBF = pivot.balanceFactor;
        byte leftBF = leftChild.balanceFactor;
        
        super.rotateRight(pivotNode);
        
        if (leftBF <= 0) {
            pivot.balanceFactor = (byte) (pivotBF + 1 - leftBF);
            leftChild.balanceFactor = (byte) (leftBF + 1);
        } else {
            pivot.balanceFactor = (byte) (pivotBF + 1);
            leftChild.balanceFactor = (byte) (leftBF + 1 + pivotBF);
        }
    }
    
    public boolean isBalanced() {
        return isBalanced((AVLTreeNode) root);
    }
    
    private boolean isBalanced(AVLTreeNode node) {
        if (node == null) return true;
        
        return node.isBalanced() && 
               isBalanced(node.getLeftChild()) && 
               isBalanced(node.getRightChild());
    }
    
    public boolean verifyAllBalanceFactors() {
        return verifyAllBalanceFactors((AVLTreeNode) root);
    }
    
    private boolean verifyAllBalanceFactors(AVLTreeNode node) {
        if (node == null) return true;
        
        boolean nodeBalanced = node.isBalanced();
        if (!nodeBalanced) {
            System.out.println("Balance factor violation: node with data " + node.getData() + 
                             " has balance factor " + node.balanceFactor);
        }
        
        return nodeBalanced && 
               verifyAllBalanceFactors(node.getLeftChild()) && 
               verifyAllBalanceFactors(node.getRightChild());
    }
    
}