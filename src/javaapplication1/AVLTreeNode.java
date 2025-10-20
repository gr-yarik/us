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
    
    public void incrementBalanceFactor() {
        if (balanceFactor < 2) {
            balanceFactor++;
        }
    }
    
    public void decrementBalanceFactor() {
        if (balanceFactor > -2) {
            balanceFactor--;
        }
    }
    
    public int getHeight() {
        return getHeight(this);
    }
    
    private int getHeight(AVLTreeNode node) {
        if (node == null) {
            return -1;
        }
        return 1 + Math.max(getHeight((AVLTreeNode) node.leftChild), 
                           getHeight((AVLTreeNode) node.rightChild));
    }
    
    public void updateBalanceFactor() {
        int leftHeight = (leftChild == null) ? -1 : ((AVLTreeNode) leftChild).getHeight();
        int rightHeight = (rightChild == null) ? -1 : ((AVLTreeNode) rightChild).getHeight();
        balanceFactor = (byte) (rightHeight - leftHeight);
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
