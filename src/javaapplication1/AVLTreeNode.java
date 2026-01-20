package javaapplication1;

import java.util.LinkedList;
import java.util.Queue;

import javaapplication1.BSTree.ChildSide;

public class AVLTreeNode extends BSTreeNode {
    
    byte balance = 0;
    
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
        
        Queue<AVLTreeNode> queue = new LinkedList<>();
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
        return Math.abs(balance) <= 1;
    }
    
    public boolean isLeftHeavy() {
        return balance < 0;
    }
    
    public boolean isRightHeavy() {
        return balance > 0;
    }
    
    public void updateBalanceFactor() {
        int leftHeight = getHeightIterative(getLeftChild());
        int rightHeight = getHeightIterative(getRightChild());
        balance = (byte) (rightHeight - leftHeight);
    }
    
    public boolean updateBalanceFactorIncremental(ChildSide side) {
        byte oldBF = balance;
        
        if (side == BSTree.ChildSide.LEFT) {
            balance--;
        } else {
            balance++;
        }
        
        return (oldBF == 0 && Math.abs(balance) == 1) || 
               (Math.abs(oldBF) == 1 && Math.abs(balance) == 2);
    }
    
    public boolean updateBalanceFactorDecremental(BSTree.ChildSide side) {
        byte oldBF = balance;
        
        if (side == BSTree.ChildSide.LEFT) {
            balance++;
        } else {
            balance--;
        }
        
        return (oldBF == 0 && Math.abs(balance) == 1) || 
               (Math.abs(oldBF) == 1 && balance == 0);
    }
}
