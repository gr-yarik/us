
package javaapplication1;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BSTree {

    protected BSTreeNode root;

    public BSTree() {
        this.root = null;
    }

    public void insert(BSTreeNodeData newData) {
        insert(newData, null);
    }

    public void delete(BSTreeNodeData data) {

        TryFindRecord result = tryFind(data, null);

        if(result.found()) {
            delete(result.searchStoppedAtNode());
        } else {
            System.out.println("Could not delete: " + data);
        }
        
    }

    protected void delete(BSTreeNode node) {
        if (node == null) return;

        if (node.leftChild == null && node.rightChild == null) {
            
            if (node.parent == null) {
                root = null;
            } else if (node.parent.leftChild == node) {
                node.parent.leftChild = null;
            } else {
                node.parent.rightChild = null;
            }

        } else if (node.leftChild == null || node.rightChild == null) {
            
            BSTreeNode child = (node.leftChild != null) ? node.leftChild : node.rightChild;

            if (node.parent == null) {
                root = child;
            } else if (node.parent.leftChild == node) {
                node.parent.leftChild = child;
            } else {
                node.parent.rightChild = child;
            }

            if (child != null) {
                child.parent = node.parent;
            }

        } else {
            BSTreeNode successor = findMinimum(node.rightChild);
            node.data = successor.data;
            delete(successor);
        }
    }

    protected void rotateLeft(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.rightChild == null) return;

        BSTreeNode newParent = pivotNode.rightChild;
        BSTreeNode leftSubtreeOfNewParent = newParent.leftChild;

        newParent.parent = pivotNode.parent;
        if (pivotNode.parent == null) {
            root = newParent;
        } else if (pivotNode.parent.leftChild == pivotNode) {
            pivotNode.parent.leftChild = newParent;
        } else {
            pivotNode.parent.rightChild = newParent;
        }

        pivotNode.rightChild = leftSubtreeOfNewParent;
        if (leftSubtreeOfNewParent != null) {
            leftSubtreeOfNewParent.parent = pivotNode;
        }

        newParent.leftChild = pivotNode;
        pivotNode.parent = newParent;
    }

    protected void rotateRight(BSTreeNode pivotNode) {
        if (pivotNode == null || pivotNode.leftChild == null) return;

        BSTreeNode newParent = pivotNode.leftChild;
        BSTreeNode rightSubtreeOfNewParent = newParent.rightChild;

        newParent.parent = pivotNode.parent;
        if (pivotNode.parent == null) {
            root = newParent;
        } else if (pivotNode.parent.leftChild == pivotNode) {
            pivotNode.parent.leftChild = newParent;
        } else {
            pivotNode.parent.rightChild = newParent;
        }

        pivotNode.leftChild = rightSubtreeOfNewParent;
        if (rightSubtreeOfNewParent != null) {
            rightSubtreeOfNewParent.parent = pivotNode;
        }

        newParent.rightChild = pivotNode;
        pivotNode.parent = newParent;
    }
    
    protected void insert(BSTreeNodeData newData, Consumer<BSTreeNode> operation) {
        TryFindRecord searchResult = tryFind(newData, operation);

        if (searchResult.found()) {
            System.out.println("Error - key is already present in the tree: " + newData);
            return;
        }

        if (root == null) {
            root = createNode();
            root.data = newData;
        } else {
            BSTreeNode newChild = createNode();
            BSTreeNode searchStoppedAtNode = searchResult.searchStoppedAtNode();
            newChild.data = newData;
            newChild.parent = searchStoppedAtNode;
            switch (searchResult.side()) {
                case LEFT:
                    searchStoppedAtNode.leftChild = newChild;        
                    break;
                case RIGHT:
                    searchStoppedAtNode.rightChild = newChild;
                    break;
                default:
                    break;
            }
        }
    }

    enum ChildSide {
        RIGHT, LEFT;
    }
    public record TryFindRecord(boolean found, BSTreeNode searchStoppedAtNode, ChildSide side) {}
    
    protected TryFindRecord tryFind(BSTreeNodeData key, Consumer<BSTreeNode> operation){

        if (root == null) {
            return new TryFindRecord(false, null, null);
        }

        BSTreeNode currentNode = root;

        while (true) {
            if (operation != null) {
                operation.accept(currentNode);
            }

            int comparisonResult = currentNode.data.compare(key);

            if (comparisonResult > 0) {
                if (currentNode.leftChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.LEFT);    
                }
                currentNode = currentNode.leftChild;
             } else if (comparisonResult < 0) {
                if (currentNode.rightChild == null) {
                    return new TryFindRecord(false, currentNode, ChildSide.RIGHT);    
                }
                currentNode = currentNode.rightChild;
             } else if (comparisonResult == 0) {
                return new TryFindRecord(true, currentNode, null);
             }
        }
    
    }

    public BSTreeNodeData find(BSTreeNodeData key){
        TryFindRecord record = tryFind(key, null);
        if(record.found()) {
            return record.searchStoppedAtNode().data;
        }
        System.out.println("Key not found: " + key);
        return null;
    }
    
    protected BSTreeNode findMinimum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.leftChild != null) {
                current = current.leftChild;
            } else {
                break;
            }
        }
        
        return current;
    }

    protected BSTreeNode findMaximum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.rightChild != null) {
                current = current.rightChild;
            } else {
                break;
            }
        }
        
        return current;
    }

    public void inorderTraversal(Predicate<BSTreeNodeData> action) {
        inorderTraversal(root, action);
    }

    protected void inorderTraversal(BSTreeNode node, Predicate<BSTreeNodeData> action) {
        if (node == null) {
            return;
        }

        BSTreeNode current = findMinimum(node);
        
        while (current != null) {
            boolean shouldContinue = action.test(current.data);
            if (!shouldContinue) {
                return;
            }
            
            if (current.rightChild != null) {
                current = current.rightChild;
                while (current.leftChild != null) {
                    current = current.leftChild;
                }
            } else {
                BSTreeNode parent = current.parent;
                
                while (parent != null && current == parent.rightChild) {
                    current = parent;
                    parent = parent.parent;
                }
                
                current = parent;
            }
        }
    }
    
    public List<BSTreeNodeData> findInRange(BSTreeNodeData minKey, BSTreeNodeData maxKey) {
        List<BSTreeNodeData> results = new ArrayList<>();

        if (root == null) {
            return results;
        }

        TryFindRecord searchResult = tryFind(minKey, null);
        BSTreeNode startNode = searchResult.searchStoppedAtNode();

        inorderTraversal(startNode, data -> {
            if (data.compare(minKey) < 0) {
                return true; 
            } 
            if(data.compare(maxKey) > 0) {
                return false;
            }
            results.add(data);
            return true;
            
        });

        return results;
    }
    
    protected BSTreeNode createNode() {
        return new BSTreeNode();
    }
    
    public int getHeight() {
        return getHeight(root);
    }
    

    protected int getHeight(BSTreeNode startingAtNode) {
        if (startingAtNode == null) {
            return -1;
        }

        int maxDepth = 0;
        int depth = 0;

        BSTreeNode current = startingAtNode;
        BSTreeNode previous = null;

        while (current != null) {
            if (previous == current.parent) {
                if (current.leftChild != null) {
                    previous = current;
                    current = current.leftChild;
                    depth++;
                    continue;
                } else if (current.rightChild != null) {
                    previous = current;
                    current = current.rightChild;
                    depth++;
                    continue;
                } else {
                    if (depth > maxDepth) {
                        maxDepth = depth;
                    }
                    previous = current;
                    current = current.parent;
                    depth--;
                    continue;
                }
            } else if (previous == current.leftChild) {
                if (current.rightChild != null) {
                    previous = current;
                    current = current.rightChild;
                    depth++;
                    continue;
                } else {
                    previous = current;
                    current = current.parent;
                    depth--;
                    continue;
                }
            } else {
                previous = current;
                current = current.parent;
                depth--;
            }
        }

        return maxDepth;
    }
    
    public boolean isEmpty() {
        return root == null;
    }
    
    protected BSTreeNode getRoot() {
        return root;
    }
    
    protected void setRoot(BSTreeNode newRoot) {
        this.root = newRoot;
    }

    // kody vypisu stromov do konzoly boli vygenerovane pomocou AI
    public void printTree() {
        if (root == null) {
            System.out.println("<empty>");
            return;
        }
        AsciiBox box = buildAsciiBox(root);
        for (String line : box.lines) {
            System.out.println(line);
        }
    }

    private static final class AsciiBox {
        java.util.List<String> lines;
        int width;
        int height;
        int middle;

        AsciiBox(java.util.List<String> lines, int width, int height, int middle) {
            this.lines = lines;
            this.width = width;
            this.height = height;
            this.middle = middle;
        }
    }

    private AsciiBox buildAsciiBox(BSTreeNode node) {
        if (node == null) {
            return new AsciiBox(new ArrayList<>(), 0, 0, 0);
        }

        String label = String.valueOf(node.data);
        AsciiBox left = buildAsciiBox(node.leftChild);
        AsciiBox right = buildAsciiBox(node.rightChild);

        if (left.width == 0 && right.width == 0) {
            java.util.List<String> lines = new ArrayList<>();
            lines.add(label);
            return new AsciiBox(lines, label.length(), 1, label.length() / 2);
        }

        if (right.width == 0) {
            // Only left child
            int leftMiddle = left.middle;
            int leftRest = left.width - leftMiddle - 1;
            String line1 = repeat(' ', leftMiddle + 1) + repeat('_', leftRest) + label;
            String line2 = repeat(' ', leftMiddle) + '/' + repeat(' ', leftRest + label.length());

            java.util.List<String> merged = mergeLeft(left.lines, label.length());
            merged.add(0, line2);
            merged.add(0, line1);

            int width = Math.max(line1.length(), merged.get(2).length());
            padLinesToWidth(merged, width);
            return new AsciiBox(merged, width, merged.size(), (left.width + label.length()) / 2);
        }

        if (left.width == 0) {
            // Only right child
            int rightMiddle = right.middle;
            String line1 = label + repeat('_', rightMiddle) + repeat(' ', right.width - rightMiddle);
            String line2 = repeat(' ', label.length() + rightMiddle) + '\\' + repeat(' ', right.width - rightMiddle - 1);

            java.util.List<String> merged = mergeRight(right.lines, label.length());
            merged.add(0, line2);
            merged.add(0, line1);

            int width = Math.max(line1.length(), merged.get(2).length());
            padLinesToWidth(merged, width);
            return new AsciiBox(merged, width, merged.size(), label.length() / 2);
        }

        // Both children
        int leftMiddle = left.middle;
        int rightMiddle = right.middle;
        String line1 = repeat(' ', leftMiddle + 1) + repeat('_', left.width - leftMiddle - 1) + label
                + repeat('_', rightMiddle) + repeat(' ', right.width - rightMiddle);
        String line2 = repeat(' ', leftMiddle) + '/' + repeat(' ', left.width - leftMiddle - 1 + label.length() + rightMiddle)
                + '\\' + repeat(' ', right.width - rightMiddle - 1);

        java.util.List<String> mergedChildren = mergeBoth(left.lines, right.lines, label.length());
        mergedChildren.add(0, line2);
        mergedChildren.add(0, line1);

        int width = Math.max(Math.max(line1.length(), line2.length()), mergedChildren.get(2).length());
        padLinesToWidth(mergedChildren, width);
        return new AsciiBox(mergedChildren, width, mergedChildren.size(), left.width + label.length() / 2);
    }

    private java.util.List<String> mergeLeft(java.util.List<String> leftLines, int labelWidth) {
        java.util.List<String> merged = new ArrayList<>(leftLines.size());
        for (String l : leftLines) {
            merged.add(l + repeat(' ', labelWidth));
        }
        return merged;
    }

    private java.util.List<String> mergeRight(java.util.List<String> rightLines, int labelWidth) {
        java.util.List<String> merged = new ArrayList<>(rightLines.size());
        for (String r : rightLines) {
            merged.add(repeat(' ', labelWidth) + r);
        }
        return merged;
    }

    private java.util.List<String> mergeBoth(java.util.List<String> leftLines, java.util.List<String> rightLines, int labelWidth) {
        int height = Math.max(leftLines.size(), rightLines.size());
        java.util.List<String> merged = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : repeat(' ', leftLines.get(0).length());
            String rightLine = i < rightLines.size() ? rightLines.get(i) : repeat(' ', rightLines.get(0).length());
            merged.add(leftLine + repeat(' ', labelWidth) + rightLine);
        }
        return merged;
    }

    private void padLinesToWidth(java.util.List<String> lines, int width) {
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.length() < width) {
                lines.set(i, s + repeat(' ', width - s.length()));
            }
        }
    }

    private String repeat(char ch, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }
    
   
}
