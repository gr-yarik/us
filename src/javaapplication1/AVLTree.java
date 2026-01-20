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
        
        // Perform BST deletion
        DeletionRecord<T> result = deleteNode(nodeToDelete);
        
        // If deletion didn't actually happen or root was deleted with no children
        if (result == null || result.side() == null && result.parentNode() == null) {
            return;
        }
        
        AVLTreeNode<T> parent = (AVLTreeNode<T>) result.parentNode();
        if (parent != null) {
            // Convert the side result to our internal ChildSide enum or boolean logic
            // Assuming DeletionRecord.side() returns something indicating LEFT or RIGHT
            ChildSide deletedSide = (result.side() == BSTree.ChildSide.LEFT) ? ChildSide.LEFT : ChildSide.RIGHT;
            updateBalanceFactorsAndRebalanceAfterDelete(parent, deletedSide);
        }
    }

    // =========================================================================
    // INSERTION REBALANCING
    // =========================================================================

    private void updateBalanceFactorsAndRebalance(AVLTreeNode<T> node) {
        AVLTreeNode<T> current = node;
        AVLTreeNode<T> parent = (AVLTreeNode<T>) current.getParent();

        while (parent != null) {
            // 1. Update Balance
            if (current == parent.getLeftChild()) {
                parent.balance--; // Left side grew
            } else {
                parent.balance++; // Right side grew
            }

            // 2. Check Logic (Stop / Continue / Rotate)
            
            // Case: Balance became 0. 
            // The tree was previously +/- 1, now it's balanced. Height did NOT change.
            if (parent.balance == 0) {
                return; // STOP
            }

            // Case: Balance became +/- 1.
            // The tree was 0, now it leans. Height INCREASED.
            if (Math.abs(parent.balance) == 1) {
                // Continue up the tree
                current = parent;
                parent = (AVLTreeNode<T>) current.getParent();
                continue;
            }

            // Case: Balance became +/- 2. Critical Imbalance.
            if (Math.abs(parent.balance) == 2) {
                performRotation(parent);
                return; // For insertion, one rotation fixes the height. STOP.
            }
        }
    }

    // =========================================================================
    // DELETION REBALANCING
    // =========================================================================

    private void updateBalanceFactorsAndRebalanceAfterDelete(AVLTreeNode<T> startNode, ChildSide deletedSide) {
        AVLTreeNode<T> current = startNode;
        boolean isLeftSide = (deletedSide == ChildSide.LEFT);

        while (current != null) {
            // 1. Update Balance
            // If deleted from Left, Right side becomes relatively heavier (+)
            // If deleted from Right, Left side becomes relatively heavier (-)
            if (isLeftSide) {
                current.balance++; 
            } else {
                current.balance--;
            }

            // 2. Check Logic
            
            // Case: Balance became +/- 1.
            // Node was 0. Now it leans. Height is same as before (max(L,R) was H, now still H).
            if (Math.abs(current.balance) == 1) {
                return; // STOP propagation
            }

            // Case: Balance became 0.
            // Node was +/- 1. Now 0. Height DECREASED.
            if (current.balance == 0) {
                // Determine side for next iteration before moving up
                AVLTreeNode parent = (AVLTreeNode) current.getParent();
                if (parent != null) {
                    isLeftSide = (current == parent.getLeftChild());
                }
                current = parent;
                continue; // CONTINUE UP
            }

            // Case: Balance became +/- 2. Rotate.
            if (Math.abs(current.balance) == 2) {
                AVLTreeNode newSubtreeRoot = performRotation(current);
                
                // Special Delete Logic:
                // If the rotation resulted in a balanced node (0), height decreased -> Continue Up.
                // If the rotation resulted in a leaning node (+/- 1), height maintained -> Stop.
                
                if (newSubtreeRoot.balance == 0) {
                    AVLTreeNode parent = (AVLTreeNode) newSubtreeRoot.getParent();
                    if (parent != null) {
                        isLeftSide = (newSubtreeRoot == parent.getLeftChild());
                    }
                    current = parent;
                    continue; // CONTINUE UP
                } else {
                    return; // STOP
                }
            }
        }
    }

    // =========================================================================
    // ROTATION LOGIC
    // =========================================================================
// =========================================================================
    // ROTATION LOGIC (Corrected)
    // =========================================================================

    private AVLTreeNode<T> performRotation(AVLTreeNode<T> N) {
        // N is the critical node (+/- 2)
        AVLTreeNode<T> newRoot = null;

        // Left Heavy (-2)
        if (N.balance == -2) {
            AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
            
            // --- FIX START: Safety Check ---
            // If balance says -2 but Left child is missing, tree state is inconsistent.
            // We return N to prevent crash.
            if (B == null) return N; 
            // --- FIX END ---

            // LL Case: B is Left Heavy (-1) or Balanced (0 - possible in delete)
            if (B.balance == -1 || B.balance == 0) {
                newRoot = performRightRotation(N);
            } 
            // LR Case: B is Right Heavy (+1)
            else {
                newRoot = performLeftRightRotation(N);
            }
        }
        // Right Heavy (+2)
        else {
            AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
            
            // --- FIX START: Safety Check ---
            // This specifically fixes your NullPointerException.
            // We check if B is null before accessing B.balance later.
            if (B == null) return N;
            // --- FIX END ---

            // RR Case: B is Right Heavy (+1) or Balanced (0)
            if (B.balance == 1 || B.balance == 0) {
                newRoot = performLeftRotation(N);
            }
            // RL Case: B is Left Heavy (-1)
            else {
                newRoot = performRightLeftRotation(N);
            }
        }
        return newRoot;
    }
    /**
     * Single Right Rotation (LL)
     * Updates Parents strictly.
     */
    private AVLTreeNode<T> performRightRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
        AVLTreeNode<T> T2 = (AVLTreeNode<T>) B.getRightChild();
        AVLTreeNode<T> P = (AVLTreeNode<T>) N.getParent();

        // Rotate
        B.setRightChild(N);
        N.setLeftChild(T2);

        // Update Parents
        if (T2 != null) T2.setParent(N);
        N.setParent(B);
        B.setParent(P);

        if (P != null) {
            if (P.getLeftChild() == N) P.setLeftChild(B);
            else P.setRightChild(B);
        } else {
            setRoot(B); // Helper from BSTree
        }

        // Update Balances
        if (B.balance == -1) {
            // Standard LL Insert Case
            N.balance = 0;
            B.balance = 0;
        } else {
            // Special Delete Case (B was 0)
            N.balance = -1;
            B.balance = 1;
        }

        return B;
    }

    /**
     * Single Left Rotation (RR)
     */
    private AVLTreeNode<T> performLeftRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
        AVLTreeNode<T> T2 = (AVLTreeNode<T>) B.getLeftChild();
        AVLTreeNode<T> P = (AVLTreeNode<T>) N.getParent();

        // Rotate
        B.setLeftChild(N);
        N.setRightChild(T2);

        // Update Parents
        if (T2 != null) T2.setParent(N);
        N.setParent(B);
        B.setParent(P);

        if (P != null) {
            if (P.getLeftChild() == N) P.setLeftChild(B);
            else P.setRightChild(B);
        } else {
            setRoot(B);
        }

        // Update Balances
        if (B.balance == 1) {
            // Standard RR Insert Case
            N.balance = 0;
            B.balance = 0;
        } else {
            // Special Delete Case (B was 0)
            N.balance = 1;
            B.balance = -1;
        }

        return B;
    }

    /**
     * Double Left-Right Rotation (LR)
     */
    private AVLTreeNode<T> performLeftRightRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getLeftChild();
        AVLTreeNode<T> F = (AVLTreeNode<T>) B.getRightChild();
        int originalFBalance = F.balance;

        // 1. Rotate Left on Left Child (B)
        // Since rotateLeft updates parents, we don't need to manually link N->F yet,
        // but rotateLeft will return F.
        super.rotateLeft(B); // B becomes child of F

        // 2. Rotate Right on Node (N)
        // F is now N.left. Rotate Right will make F the new root of this trio.
        AVLTreeNode<T> newRoot = performRightRotation(N);

        // Balance Updates (Standard Logic from Document)
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

    /**
     * Double Right-Left Rotation (RL)
     */
    private AVLTreeNode<T> performRightLeftRotation(AVLTreeNode<T> N) {
        AVLTreeNode<T> B = (AVLTreeNode<T>) N.getRightChild();
        AVLTreeNode<T> F = (AVLTreeNode<T>) B.getLeftChild();
        int originalFBalance = F.balance;

        // 1. Rotate Right on Right Child (B)
        super.rotateRight(B);

        // 2. Rotate Left on Node (N)
        AVLTreeNode<T> newRoot = performLeftRotation(N);

        // Balance Updates
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


    
    /**
     * Verifies that every node's stored AVL balance factor equals the balance
     * computed from the heights of its children (rightHeight - leftHeight).
     * This performs a full traversal (O(n)) and does not rely on incremental
     * updates.
     */
    public boolean verifyAllBalanceFactors() {
        VerifyState state = new VerifyState();
        verifyAndGetHeight((AVLTreeNode<T>) getRoot(), state);
        return state.ok;
    }

    private class VerifyState {
        boolean ok = true;
    }

    /**
     * @return subtree height where null has height -1
     */
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