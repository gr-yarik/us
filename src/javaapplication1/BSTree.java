
package javaapplication1;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.io.ByteArrayOutputStream;
import UnsortedFile.BinaryFile;
import UnsortedFile.StorableRecord;

public class BSTree<T extends TreeNodeData> {

    protected BSTreeNode<T> root;

    public BSTree() {
        this.root = null;
    }

    public void insert(T newData) {
        insert(newData, null);
    }

    public void delete(T data) {

        TryFindRecord<T> result = tryFind(data, null);

        if(result.found()) {
            delete(result.searchStoppedAtNode());
        } else {
            // Suppress verbose output during testing
            // System.out.println("Could not delete: " + data);
        }
        
    }

    public record DeletionRecord<T extends TreeNodeData>(ChildSide side, BSTreeNode<T> parentNode) {}
    
    protected DeletionRecord<T> deleteNode(BSTreeNode<T> node) {
        if (node == null) return null;
        
        BSTreeNode<T> parent = node.getParent();
        ChildSide deletedSide = null;
        
        if (parent != null) {
            deletedSide = (parent.getLeftChild() == node) ? ChildSide.LEFT : ChildSide.RIGHT;
        }

        if (node.getLeftChild() == null && node.getRightChild() == null) {
            if (parent == null) {
                root = null;
                return null;
            } else if (deletedSide == ChildSide.LEFT) {
                parent.setLeftChild(null);
            } else {
                parent.setRightChild(null);
            }
            return new DeletionRecord<>(deletedSide, parent);

        } else if (node.getLeftChild() == null || node.getRightChild() == null) {
            BSTreeNode<T> child = (node.getLeftChild() != null) ? node.getLeftChild() : node.getRightChild();

            if (parent == null) {
                root = child;
                child.setParent(null);  // FIX: Clear parent pointer when child becomes new root
                return null;
            } else if (deletedSide == ChildSide.LEFT) {
                parent.setLeftChild(child);
            } else {
                parent.setRightChild(child);
            }

            if (child != null) {
                child.setParent(parent);
            }
            return new DeletionRecord<>(deletedSide, parent);

        } else {
            BSTreeNode<T> successor = findMinimum(node.getRightChild());
            node.setData(successor.getData());
            return deleteNode(successor);
        }
    }
    
    protected void delete(BSTreeNode<T> node) {
        deleteNode(node);
    }

    protected void rotateLeft(BSTreeNode<T> pivotNode) {
        if (pivotNode == null || pivotNode.getRightChild() == null) return;

        BSTreeNode<T> newParent = pivotNode.getRightChild();
        BSTreeNode<T> leftSubtreeOfNewParent = newParent.getLeftChild();

        newParent.setParent(pivotNode.getParent());
        if (pivotNode.getParent() == null) {
            root = newParent;
        } else if (pivotNode.getParent().getLeftChild() == pivotNode) {
            pivotNode.getParent().setLeftChild(newParent);
        } else {
            pivotNode.getParent().setRightChild(newParent);
        }

        pivotNode.setRightChild(leftSubtreeOfNewParent);
        if (leftSubtreeOfNewParent != null) {
            leftSubtreeOfNewParent.setParent(pivotNode);
        }

        newParent.setLeftChild(pivotNode);
        pivotNode.setParent(newParent);
    }

    protected void rotateRight(BSTreeNode<T> pivotNode) {
        if (pivotNode == null || pivotNode.getLeftChild() == null) return;

        BSTreeNode<T> newParent = pivotNode.getLeftChild();
        BSTreeNode<T> rightSubtreeOfNewParent = newParent.getRightChild();

        newParent.setParent(pivotNode.getParent());
        if (pivotNode.getParent() == null) {
            root = newParent;
        } else if (pivotNode.getParent().getLeftChild() == pivotNode) {
            pivotNode.getParent().setLeftChild(newParent);
        } else {
            pivotNode.getParent().setRightChild(newParent);
        }

        pivotNode.setLeftChild(rightSubtreeOfNewParent);
        if (rightSubtreeOfNewParent != null) {
            rightSubtreeOfNewParent.setParent(pivotNode);
        }

        newParent.setRightChild(pivotNode);
        pivotNode.setParent(newParent);
    }
    
    protected BSTreeNode<T> insert(T newData, Consumer<BSTreeNode<T>> operation) {
        TryFindRecord<T> searchResult = tryFind(newData, operation);

        if (searchResult.found()) {
            System.out.println("Error - key is already present in the tree: " + newData);
            return null;
        }

        if (root == null) {
            root = createNode();
            root.setData(newData);
            return root;
        } else {
            BSTreeNode<T> newChild = createNode();
            BSTreeNode<T> searchStoppedAtNode = searchResult.searchStoppedAtNode();
            newChild.setData(newData);
            newChild.setParent(searchStoppedAtNode);
            switch (searchResult.side()) {
                case LEFT:
                    searchStoppedAtNode.setLeftChild(newChild);        
                    break;
                case RIGHT:
                    searchStoppedAtNode.setRightChild(newChild);
                    break;
                default:
                    break;
            }
            return newChild;
        }
    }

    enum ChildSide {
        RIGHT, LEFT;
    }
    
    protected ChildSide getChildSide(BSTreeNode<T> node) {
        if (node == null || node.getParent() == null) {
            return null;
        }
        
        if (node.getParent().getLeftChild() == node) {
            return ChildSide.LEFT;
        } else if (node.getParent().getRightChild() == node) {
            return ChildSide.RIGHT;
        }
        
        return null;
    }
    
    public record TryFindRecord<T extends TreeNodeData>(boolean found, BSTreeNode<T> searchStoppedAtNode, ChildSide side) {}
    
    protected TryFindRecord<T> tryFind(T key, Consumer<BSTreeNode<T>> operation){
        
        if (root == null) {
            return new TryFindRecord<>(false, null, null);
        }
        
        BSTreeNode<T> currentNode = root;

        while (true) {
            if (operation != null) {
                operation.accept(currentNode);
            }

            int comparisonResult = currentNode.getData().compare(key);

            if (comparisonResult > 0) {
                if (currentNode.getLeftChild() == null) {
                    return new TryFindRecord<>(false, currentNode, ChildSide.LEFT);    
                }
                currentNode = currentNode.getLeftChild();
             } else if (comparisonResult < 0) {
                if (currentNode.getRightChild() == null) {
                    return new TryFindRecord<>(false, currentNode, ChildSide.RIGHT);    
                }
                currentNode = currentNode.getRightChild();
             } else if (comparisonResult == 0) {
                return new TryFindRecord<>(true, currentNode, null);
             }
        }
    
    }

    public T find(T key){
        TryFindRecord<T> record = tryFind(key, null);
        if(record.found()) {
            return record.searchStoppedAtNode().getData();
        }
        // Suppress verbose output during testing
        // System.out.println("Key not found: " + key);
        return null;
    }
    
    protected BSTreeNode findMinimum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.getLeftChild() != null) {
                current = current.getLeftChild();
            } else {
                break;
            }
        }
        
        return current;
    }

    protected BSTreeNode findMaximum(BSTreeNode startingAt) {

        BSTreeNode current = startingAt == null ? root : startingAt;
 
        while(true) {
            if (current.getRightChild() != null) {
                current = current.getRightChild();
            } else {
                break;
            }
        }
        
        return current;
    }
    
    public T findMin() {
        if (root == null) {
            return null;
        }
        BSTreeNode<T> minNode = findMinimum(null);
        return minNode != null ? minNode.getData() : null;
    }
    
    public T findMax() {
        if (root == null) {
            return null;
        }
        BSTreeNode<T> maxNode = findMaximum(null);
        return maxNode != null ? maxNode.getData() : null;
    }

    public void inorderTraversal(Predicate<T> action) {
        inorderTraversal(root, action);
    }

    protected void inorderTraversal(BSTreeNode<T> node, Predicate<T> action) {
        if (node == null) {
            return;
        }

        BSTreeNode<T> current = findMinimum(node);
        
        while (current != null) {
            boolean shouldContinue = action.test(current.getData());
            if (!shouldContinue) {
                return;
            }
            
            if (current.getRightChild() != null) {
                current = current.getRightChild();
                while (current.getLeftChild() != null) {
                    current = current.getLeftChild();
                }
            } else {
                BSTreeNode<T> parent = current.getParent();
                
                while (parent != null && current == parent.getRightChild()) {
                    current = parent;
                    parent = parent.getParent();
                }
                
                current = parent;
            }
        }
    }
    
    public List<T> findInRange(T minKey, T maxKey) {
        List<T> results = new ArrayList<>();

        if (root == null) {
            return results;
        }

        TryFindRecord<T> searchResult = tryFind(minKey, null);
        BSTreeNode<T> startNode = searchResult.searchStoppedAtNode();

        inorderTraversal(startNode, item -> {
            if (item.compare(minKey) >= 0 && item.compare(maxKey) <= 0) {
                results.add(item);
                return true;
            } else if (item.compare(maxKey) > 0) {
                return false; // Stop traversal
            }
            return true; // Continue traversal
            
        });

        return results;
    }
    
    protected BSTreeNode<T> createNode() {
        return new BSTreeNode<T>();
    }
    
    public int getHeight() {
        return getHeight(root);
    }

    protected int getHeight(BSTreeNode<T> startingAtNode) {
        if (startingAtNode == null) {
            return -1;
        }

        int maxDepth = 0;
        int depth = 0;

        BSTreeNode<T> current = startingAtNode;
        BSTreeNode<T> previous = null;

        while (current != null) {
            if (previous == current.getParent()) {
                if (current.getLeftChild() != null) {
                    previous = current;
                    current = current.getLeftChild();
                    depth++;
                    continue;
                } else if (current.getRightChild() != null) {
                    previous = current;
                    current = current.getRightChild();
                    depth++;
                    continue;
                } else {
                    if (depth > maxDepth) {
                        maxDepth = depth;
                    }
                    previous = current;
                    current = current.getParent();
                    depth--;
                    continue;
                }
            } else if (previous == current.getLeftChild()) {
                if (current.getRightChild() != null) {
                    previous = current;
                    current = current.getRightChild();
                    depth++;
                    continue;
                } else {
                    previous = current;
                    current = current.getParent();
                    depth--;
                    continue;
                }
            } else {
                previous = current;
                current = current.getParent();
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
    
    protected void setRoot(BSTreeNode<T> newRoot) {
        this.root = newRoot;
    }

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
        List<String> lines;
        int width;
        int height;
        int middle;

        AsciiBox(List<String> lines, int width, int height, int middle) {
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

        String label = String.valueOf(node.getData());
        AsciiBox left = buildAsciiBox(node.getLeftChild());
        AsciiBox right = buildAsciiBox(node.getRightChild());

        if (left.width == 0 && right.width == 0) {
            List<String> lines = new ArrayList<>();
            lines.add(label);
            return new AsciiBox(lines, label.length(), 1, label.length() / 2);
        }

        if (right.width == 0) {
            int leftMiddle = left.middle;
            int leftRest = left.width - leftMiddle - 1;
            String line1 = repeat(' ', leftMiddle + 1) + repeat('_', leftRest) + label;
            String line2 = repeat(' ', leftMiddle) + '/' + repeat(' ', leftRest + label.length());

            List<String> merged = mergeLeft(left.lines, label.length());
            merged.add(0, line2);
            merged.add(0, line1);

            int width = Math.max(line1.length(), merged.get(2).length());
            padLinesToWidth(merged, width);
            return new AsciiBox(merged, width, merged.size(), left.width + label.length() / 2);
        }

        if (left.width == 0) {
            int rightMiddle = right.middle;
            String line1 = label + repeat('_', rightMiddle) + repeat(' ', right.width - rightMiddle);
            String line2 = repeat(' ', label.length() + rightMiddle) + '\\' + repeat(' ', right.width - rightMiddle - 1);

            List<String> merged = mergeRight(right.lines, label.length());
            merged.add(0, line2);
            merged.add(0, line1);

            int width = Math.max(line1.length(), merged.get(2).length());
            padLinesToWidth(merged, width);
            return new AsciiBox(merged, width, merged.size(), label.length() / 2);
        }

        int leftMiddle = left.middle;
        int rightMiddle = right.middle;
        String line1 = repeat(' ', leftMiddle + 1) + repeat('_', left.width - leftMiddle - 1) + label
                + repeat('_', rightMiddle) + repeat(' ', right.width - rightMiddle);
        String line2 = repeat(' ', leftMiddle) + '/' + repeat(' ', left.width - leftMiddle - 1 + label.length() + rightMiddle)
                + '\\' + repeat(' ', right.width - rightMiddle - 1);

        List<String> mergedChildren = mergeBoth(left.lines, right.lines, label.length());
        mergedChildren.add(0, line2);
        mergedChildren.add(0, line1);

        int width = Math.max(Math.max(line1.length(), line2.length()), mergedChildren.get(2).length());
        padLinesToWidth(mergedChildren, width);
        return new AsciiBox(mergedChildren, width, mergedChildren.size(), left.width + label.length() / 2);
    }

    private List<String> mergeLeft(List<String> leftLines, int labelWidth) {
        List<String> merged = new ArrayList<>(leftLines.size());
        for (String l : leftLines) {
            merged.add(l + repeat(' ', labelWidth));
        }
        return merged;
    }

    private List<String> mergeRight(List<String> rightLines, int labelWidth) {
        List<String> merged = new ArrayList<>(rightLines.size());
        for (String r : rightLines) {
            merged.add(repeat(' ', labelWidth) + r);
        }
        return merged;
    }

    private List<String> mergeBoth(List<String> leftLines, List<String> rightLines, int labelWidth) {
        int height = Math.max(leftLines.size(), rightLines.size());
        List<String> merged = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : repeat(' ', leftLines.get(0).length());
            String rightLine = i < rightLines.size() ? rightLines.get(i) : repeat(' ', rightLines.get(0).length());
            merged.add(leftLine + repeat(' ', labelWidth) + rightLine);
        }
        return merged;
    }

    private void padLinesToWidth(List<String> lines, int width) {
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
        Arrays.fill(arr, ch);
        return new String(arr);
    }
}
