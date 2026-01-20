package javaapplication1;

interface TreeNodeData {
   
    int compare(TreeNodeData otherData);
    
    public byte[] ToByteArray();
    
    public void FromByteArray(byte[] inputArray);
    
}