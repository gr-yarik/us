package javaapplication1;

public class AVLTree extends BSTree {
    
    @Override
    protected BSTreeNode createNode() {
        return new AVLTreeNode();
    }
    
    @Override
    public void insert(TreeNodeData newData) {
        AVLTreeNode newNode = (AVLTreeNode) super.insert(newData, null);
        updateBalanceFactorsAndRebalance(newNode);
    }
    
    @Override
    public void delete(TreeNodeData data) {
        
        TryFindRecord result = tryFind(data, null);
        
        if (!result.found()) {
            System.out.println("Didn't find a node with this key ");
            return;
        }
        
        AVLTreeNode nodeToDelete = (AVLTreeNode) result.searchStoppedAtNode();
        delete(nodeToDelete);
        
        updateBalanceFactorsAndRebalance(nodeToDelete);
    }
    
    private void updateBalanceFactorsAndRebalance(AVLTreeNode newNode) {
        AVLTreeNode currentNode = newNode;
        while (currentNode != null) {
            currentNode.updateBalanceFactor();
            
            if (!currentNode.isBalanced()) {
                rebalance(currentNode);
                break;
            }
            
            if (currentNode.balanceFactor == 0) {
                break;
            }
            
            currentNode = currentNode.getParent();
        }
    }
    
    private void rebalance(AVLTreeNode node) {
        if (node.isRightHeavy()) {
            AVLTreeNode rightChild = node.getRightChild();
            if (rightChild.isLeftHeavy()) {
                rotateRight(rightChild);
                rotateLeft(node);
            } else {
                rotateLeft(node);
            }
        } else if (node.isLeftHeavy()) {
            AVLTreeNode leftChild = node.getLeftChild();
            if (leftChild.isRightHeavy()) {
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
        
        super.rotateLeft(pivotNode);
        
        ((AVLTreeNode) pivotNode).updateBalanceFactor();
        if (pivotNode.getParent() != null) {
            ((AVLTreeNode) pivotNode.getParent()).updateBalanceFactor();
        }
    }
    
    @Override
    protected void rotateRight(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.getLeftChild() == null) return;
        
        super.rotateRight(pivotNode);
        
        ((AVLTreeNode) pivotNode).updateBalanceFactor();
        if (pivotNode.getParent() != null) {
            ((AVLTreeNode) pivotNode.getParent()).updateBalanceFactor();
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
    
}