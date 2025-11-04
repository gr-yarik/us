package javaapplication1;

public class AVLTreeNode extends BSTreeNode {
    
    private byte balanceFactor = 0;
    
    public AVLTreeNode() {
        super();
    }
    
    public byte getBalanceFactor() {
        return balanceFactor;
    }
    
    public void setBalanceFactor(byte balanceFactor) {
        this.balanceFactor = balanceFactor;
    }
    
    
    public void updateBalanceFactor() {
        int leftHeight = getHeightIterative((AVLTreeNode) leftChild);
        int rightHeight = getHeightIterative((AVLTreeNode) rightChild);
        balanceFactor = (byte) (rightHeight - leftHeight);
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
                
                if (current.leftChild != null) {
                    queue.offer((AVLTreeNode) current.leftChild);
                }
                if (current.rightChild != null) {
                    queue.offer((AVLTreeNode) current.rightChild);
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
}
