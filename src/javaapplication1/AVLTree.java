package javaapplication1;

import java.util.Stack;

public class AVLTree extends BSTree {
    
    @Override
    protected BSTreeNode createNode() {
        return new AVLTreeNode();
    }
    
    @Override
    public void insert(BSTreeNodeData newData) {
        Stack<AVLTreeNode> path = new Stack<>();
        
        super.insert(newData, node -> path.push((AVLTreeNode) node));
        
        updateBalanceFactorsAndRebalance(path);
    }
    
    protected void insert(BSTreeNodeData newData, java.util.function.Consumer<BSTreeNode> operation) {
        Stack<AVLTreeNode> path = new Stack<>();
        
        super.insert(newData, node -> {
            path.push((AVLTreeNode) node);
            if (operation != null) {
                operation.accept(node);
            }
        });
        
        updateBalanceFactorsAndRebalance(path);
    }
    
    @Override
    public void delete(BSTreeNodeData data) {
        Stack<AVLTreeNode> path = new Stack<>();
        
        TryFindRecord result = tryFind(data, node -> path.push((AVLTreeNode) node));
        
        if (!result.found()) {
            return;
        }
        
        delete(result.searchStoppedAtNode());
        
        if (!path.isEmpty()) {
            path.pop();
        }
        updateBalanceFactorsAndRebalance(path);
    }
    
    
    
    private void updateBalanceFactorsAndRebalance(Stack<AVLTreeNode> path) {
        while (!path.isEmpty()) {
            AVLTreeNode node = path.pop();
            node.updateBalanceFactor();
            
            if (!node.isBalanced()) {
                rebalance(node);
                break;
            }
            
            if (node.getBalanceFactor() == 0) {
                break;
            }
        }
    }
    
    private void rebalance(AVLTreeNode node) {
        if (node.isRightHeavy()) {
            AVLTreeNode rightChild = (AVLTreeNode) node.rightChild;
            if (rightChild.isLeftHeavy()) {
                rotateRight(rightChild);
                rotateLeft(node);
            } else {
                rotateLeft(node);
            }
        } else if (node.isLeftHeavy()) {
            AVLTreeNode leftChild = (AVLTreeNode) node.leftChild;
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
        if (pivotNode == null || pivotNode.rightChild == null) return;
        
        super.rotateLeft(pivotNode);
        
        ((AVLTreeNode) pivotNode).updateBalanceFactor();
        ((AVLTreeNode) pivotNode.parent).updateBalanceFactor();
    }
    
    @Override
    protected void rotateRight(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.leftChild == null) return;
        
        super.rotateRight(pivotNode);
        
        ((AVLTreeNode) pivotNode).updateBalanceFactor();
        ((AVLTreeNode) pivotNode.parent).updateBalanceFactor();
    }
    
    public boolean isBalanced() {
        return isBalanced((AVLTreeNode) root);
    }
    
    private boolean isBalanced(AVLTreeNode node) {
        if (node == null) return true;
        
        Stack<AVLTreeNode> stack = new Stack<>();
        stack.push(node);
        
        while (!stack.isEmpty()) {
            AVLTreeNode current = stack.pop();
            
            if (!current.isBalanced()) {
                return false;
            }
            
            if (current.leftChild != null) {
                stack.push((AVLTreeNode) current.leftChild);
            }
            if (current.rightChild != null) {
                stack.push((AVLTreeNode) current.rightChild);
            }
        }
        
        return true;
    }
    
}