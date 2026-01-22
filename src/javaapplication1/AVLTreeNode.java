package javaapplication1;


public class AVLTreeNode<T extends TreeNodeData> extends BSTreeNode<T> {
    
    byte balance = 0;
    
    @Override
    public AVLTreeNode<T> getLeftChild() {
        return (AVLTreeNode<T>) super.getLeftChild();
    }
    
    @Override
    public AVLTreeNode<T> getRightChild() {
        return (AVLTreeNode<T>) super.getRightChild();
    }
    
    @Override
    public AVLTreeNode<T> getParent() {
        return (AVLTreeNode<T>) super.getParent();
    }
    
    public void setLeftChild(AVLTreeNode<T> leftChild) {
        super.setLeftChild(leftChild);
    }
    
    public void setRightChild(AVLTreeNode<T> rightChild) {
        super.setRightChild(rightChild);
    }
    
    public void setParent(AVLTreeNode<T> parent) {
        super.setParent(parent);
    }
}
