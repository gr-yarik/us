package javaapplication1;

public class BSTreeNode {

    private BSTreeNode leftChild;
    private BSTreeNode rightChild;
    private BSTreeNode parent;

    private TreeNodeData data;
    
    public BSTreeNode getLeftChild() {
        return leftChild;
    }
    
    public BSTreeNode getRightChild() {
        return rightChild;
    }
    
    public BSTreeNode getParent() {
        return parent;
    }
    
    public TreeNodeData getData() {
        return data;
    }
    
    public void setLeftChild(BSTreeNode leftChild) {
        this.leftChild = leftChild;
    }
    
    public void setRightChild(BSTreeNode rightChild) {
        this.rightChild = rightChild;
    }
    
    public void setParent(BSTreeNode parent) {
        this.parent = parent;
    }
    
    public void setData(TreeNodeData data) {
        this.data = data;
    }
}
