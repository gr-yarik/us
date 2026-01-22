package javaapplication1;

public class AVLTree<T extends TreeNodeData> extends BSTree<T> {
    
    @Override
    protected BSTreeNode<T> createNode() {
        return new AVLTreeNode<T>();
    }
    
    @Override
    public void insert(T newData) {
        AVLTreeNode<T> newNode = (AVLTreeNode<T>) super.insert(newData, null);
        if (newNode != null) {
            updateBalanceFactorsAndRebalance(newNode);
        }
    }
    
    @Override
    public void delete(T data) {
        
        TryFindRecord<T> result = tryFind(data, null);
        
        if (!result.found()) {
            System.out.println("Didn't find a node with this key ");
            return;
        }
        
        AVLTreeNode<T> nodeToDelete = (AVLTreeNode<T>) result.searchStoppedAtNode();
        deleteAVL(nodeToDelete);
    }
    
    private void deleteAVL(AVLTreeNode<T> nodeToDelete) {
        if (nodeToDelete == null) return;
        
        DeletionRecord<T> result = deleteNode(nodeToDelete);
        
        if (result == null || result.side() == null && result.parentNode() == null) {
            return;
        }
        
        AVLTreeNode<T> parent = (AVLTreeNode<T>) result.parentNode();
        if (parent != null) {
            ChildSide deletedSide = (result.side() == BSTree.ChildSide.LEFT) ? ChildSide.LEFT : ChildSide.RIGHT;
            updateBalanceFactorsAndRebalanceAfterDelete(parent, deletedSide);
        }
    }

    private void updateBalanceFactorsAndRebalance(AVLTreeNode<T> node) {
        AVLTreeNode<T> current = node;
        AVLTreeNode<T> parent = (AVLTreeNode<T>) current.getParent();

        while (parent != null) {
            if (current == parent.getLeftChild()) {
                parent.balance--;
            } else {
                parent.balance++;
            }

            if (parent.balance == 0) {
                return;
            }

            if (Math.abs(parent.balance) == 1) {
                current = parent;
                parent = (AVLTreeNode<T>) current.getParent();
                continue;
            }

            if (Math.abs(parent.balance) == 2) {
                performRotation(parent);
                return;
            }
        }
    }

    private void updateBalanceFactorsAndRebalanceAfterDelete(AVLTreeNode<T> startNode, ChildSide deletedSide) {
        AVLTreeNode<T> current = startNode;
        boolean isLeftSide = (deletedSide == ChildSide.LEFT);

        while (current != null) {
            if (isLeftSide) {
                current.balance++; 
            } else {
                current.balance--;
            }

            if (Math.abs(current.balance) == 1) {
                return;
            }

            if (current.balance == 0) {
                AVLTreeNode parent = (AVLTreeNode) current.getParent();
                if (parent != null) {
                    isLeftSide = (current == parent.getLeftChild());
                }
                current = parent;
                continue;
            }

            if (Math.abs(current.balance) == 2) {
                AVLTreeNode newSubtreeRoot = performRotation(current);
                
                if (newSubtreeRoot.balance == 0) {
                    AVLTreeNode parent = (AVLTreeNode) newSubtreeRoot.getParent();
                    if (parent != null) {
                        isLeftSide = (newSubtreeRoot == parent.getLeftChild());
                    }
                    current = parent;
                    continue;
                } else {
                    return;
                }
            }
        }
    }

    private AVLTreeNode<T> performRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> newRoot = null;

        if (N.balance == -2) {
            AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
            
            if (B == null) return N; 

            if (B.balance == -1 || B.balance == 0) {
                newRoot = performRightRotation(N);
            } 
            else {
                newRoot = performLeftRightRotation(N);
            }
        }
        else {
            AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
            
            if (B == null) return N;

            if (B.balance == 1 || B.balance == 0) {
                newRoot = performLeftRotation(N);
            }
            else {
                newRoot = performRightLeftRotation(N);
            }
        }
        return newRoot;
    }
    private AVLTreeNode<T> performRightRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
        AVLTreeNode<T> T2 = (AVLTreeNode<T>) B.getRightChild();
        AVLTreeNode<T> P = (AVLTreeNode<T>) N.getParent();

        B.setRightChild(N);
        N.setLeftChild(T2);

        if (T2 != null) T2.setParent(N);
        N.setParent(B);
        B.setParent(P);

        if (P != null) {
            if (P.getLeftChild() == N) P.setLeftChild(B);
            else P.setRightChild(B);
        } else {
            setRoot(B);
        }

        if (B.balance == -1) {
            N.balance = 0;
            B.balance = 0;
        } else {
            N.balance = -1;
            B.balance = 1;
        }

        return B;
    }

    private AVLTreeNode<T> performLeftRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
        AVLTreeNode<T> T2 = (AVLTreeNode<T>) B.getLeftChild();
        AVLTreeNode<T> P = (AVLTreeNode<T>) N.getParent();

        B.setLeftChild(N);
        N.setRightChild(T2);

        if (T2 != null) T2.setParent(N);
        N.setParent(B);
        B.setParent(P);

        if (P != null) {
            if (P.getLeftChild() == N) P.setLeftChild(B);
            else P.setRightChild(B);
        } else {
            setRoot(B);
        }

        if (B.balance == 1) {
            N.balance = 0;
            B.balance = 0;
        } else {
            N.balance = 1;
            B.balance = -1;
        }

        return B;
    }

    private AVLTreeNode<T> performLeftRightRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
        AVLTreeNode<T> F = (AVLTreeNode<T>) B.getRightChild();
        int originalFBalance = F.balance;

        super.rotateLeft(B);

        AVLTreeNode<T> newRoot = performRightRotation(N);

        if (originalFBalance == -1) {
            B.balance = 0;
            N.balance = 1;
        } else if (originalFBalance == 1) {
            B.balance = -1;
            N.balance = 0;
        } else {
            B.balance = 0;
            N.balance = 0;
        }
        newRoot.balance = 0;

        return newRoot;
    }

    private AVLTreeNode<T> performRightLeftRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
        AVLTreeNode<T> F = (AVLTreeNode<T>) B.getLeftChild();
        int originalFBalance = F.balance;

        super.rotateRight(B);

        AVLTreeNode<T> newRoot = performLeftRotation(N);

        if (originalFBalance == 1) {
            N.balance = -1;
            B.balance = 0;
        } else if (originalFBalance == -1) {
            N.balance = 0;
            B.balance = 1;
        } else {
            N.balance = 0;
            B.balance = 0;
        }
        newRoot.balance = 0;

        return newRoot;
    }

    
    public boolean verifyAllBalanceFactors() {
        VerifyState state = new VerifyState();
        verifyAndGetHeight((AVLTreeNode<T>) getRoot(), state);
        return state.ok;
    }

    private class VerifyState {
        boolean ok = true;
    }

    private int verifyAndGetHeight(AVLTreeNode<T> node, VerifyState state) {
        if (node == null) {
            return -1;
        }

        int leftHeight = verifyAndGetHeight(node.getLeftChild(), state);
        int rightHeight = verifyAndGetHeight(node.getRightChild(), state);
        int computedBalance = rightHeight - leftHeight;

        if (node.balance != (byte) computedBalance) {
            state.ok = false;
        }
        if (Math.abs(computedBalance) > 1) {
            state.ok = false;
        }

        return 1 + Math.max(leftHeight, rightHeight);
    }
    
    
   
}