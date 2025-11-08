package javaapplication1;

public class AVLTreeNode extends BSTreeNode {
    
    byte balanceFactor = 0;
    
    // Override getters to return AVLTreeNode type
    @Override
    public AVLTreeNode getLeftChild() {
        return (AVLTreeNode) super.getLeftChild();
    }
    
    @Override
    public AVLTreeNode getRightChild() {
        return (AVLTreeNode) super.getRightChild();
    }
    
    @Override
    public AVLTreeNode getParent() {
        return (AVLTreeNode) super.getParent();
    }
    
    // Override setters to accept AVLTreeNode type
    public void setLeftChild(AVLTreeNode leftChild) {
        super.setLeftChild(leftChild);
    }
    
    public void setRightChild(AVLTreeNode rightChild) {
        super.setRightChild(rightChild);
    }
    
    public void setParent(AVLTreeNode parent) {
        super.setParent(parent);
    }

    
    private int getHeightIterative(AVLTreeNode node) {
        if (node == null) {
            return -1;
        }
        
        java.util.Queue<AVLTreeNode> queue = new java.util.LinkedList<>();
        queue.offer(node);
        int height = -1;
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            height++;
            
            for (int i = 0; i < levelSize; i++) {
                AVLTreeNode current = queue.poll();
                
                if (current.getLeftChild() != null) {
                    queue.offer(current.getLeftChild());
                }
                if (current.getRightChild() != null) {
                    queue.offer(current.getRightChild());
                }
            }
        }
        
        return height;
    }
    
    public boolean isBalanced() {
        return Math.abs(balanceFactor) <= 1;
    }
    
    public boolean isLeftHeavy() {
        return balanceFactor < 0;
    }
    
    public boolean isRightHeavy() {
        return balanceFactor > 0;
    }
    
    public void updateBalanceFactor() {
        int leftHeight = getHeightIterative(getLeftChild());
        int rightHeight = getHeightIterative(getRightChild());
        balanceFactor = (byte) (rightHeight - leftHeight);
    }
}
