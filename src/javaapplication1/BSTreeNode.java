package javaapplication1;

public class BSTreeNode<T extends TreeNodeData> {

    private BSTreeNode<T> leftChild;
    private BSTreeNode<T> rightChild;
    private BSTreeNode<T> parent;

    private T data;
    
    public BSTreeNode<T> getLeftChild() {
        return leftChild;
    }
    
    public BSTreeNode<T> getRightChild() {
        return rightChild;
    }
    
    public BSTreeNode<T> getParent() {
        return parent;
    }
    
    public T getData() {
        return data;
    }
    
    public void setLeftChild(BSTreeNode<T> leftChild) {
        this.leftChild = leftChild;
    }
    
    public void setRightChild(BSTreeNode<T> rightChild) {
        this.rightChild = rightChild;
    }
    
    public void setParent(BSTreeNode<T> parent) {
        this.parent = parent;
    }
    
    public void setData(T data) {
        this.data = data;
    }
}
