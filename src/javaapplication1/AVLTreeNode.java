package javaapplication1;

import java.util.LinkedList;
import java.util.Queue;

import javaapplication1.BSTree.ChildSide;

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

    
    public boolean isBalanced() {
        return Math.abs(balance) <= 1;
    }
    
    public boolean isLeftHeavy() {
        return balance < 0;
    }
    
    public boolean isRightHeavy() {
        return balance > 0;
    }
}
