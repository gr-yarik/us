package UnsortedFile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import LinearHashing.Person;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeapDebugger extends JFrame {
    
    private JTextField heapFilePathField;
    private JTextField metadataFilePathField;
    private JButton loadButton;
    private JButton refreshButton;
    private JTable blocksTable;
    private DefaultTableModel tableModel;
    private JTextArea detailsArea;
    private JScrollPane detailsScrollPane;
    
    private List<BlockInfo> blockInfos;
    private BlockManager blockManager;
    private int blockSize;
    private int blockingFactor;
    
    private static class BlockInfo {
        int blockNumber;
        String status;
        int validCount;
        int capacity;
        List<Person> records;
        
        BlockInfo(int blockNumber, String status, int validCount, int capacity, List<Person> records) {
            this.blockNumber = blockNumber;
            this.status = status;
            this.validCount = validCount;
            this.capacity = capacity;
            this.records = records != null ? records : new ArrayList<>();
        }
    }
    
    public HeapDebugger() {
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Heap File Debugger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Heap File:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        heapFilePathField = new JTextField("test_heap.bin", 30);
        topPanel.add(heapFilePathField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        topPanel.add(new JLabel("Metadata File:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        metadataFilePathField = new JTextField("test_heap.bin.meta", 30);
        topPanel.add(metadataFilePathField, gbc);
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loadButton = new JButton("Load");
        refreshButton = new JButton("Refresh");
        refreshButton.setEnabled(false);
        loadButton.addActionListener(new LoadActionListener());
        refreshButton.addActionListener(new RefreshActionListener());
        buttonPanel.add(loadButton);
        buttonPanel.add(refreshButton);
        topPanel.add(buttonPanel, gbc);
        
        add(topPanel, BorderLayout.NORTH);
        
        String[] columnNames = {"Block #", "Status", "Records", "Capacity", "Usage"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        blocksTable = new JTable(tableModel);
        blocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        blocksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showBlockDetails();
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(blocksTable);
        tableScrollPane.setPreferredSize(new Dimension(600, 300));
        
        detailsArea = new JTextArea(10, 50);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("All Blocks - Complete View (All Record Slots)"));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.5);
        
        add(splitPane, BorderLayout.CENTER);
        
        JLabel statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        add(statusLabel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setSize(800, 600);
    }
    
    private class LoadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadHeapData();
        }
    }
    
    private class RefreshActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadHeapData();
        }
    }
    
    private void loadHeapData() {
        String heapPath = heapFilePathField.getText().trim();
        String metadataPath = metadataFilePathField.getText().trim();
        
        if (heapPath.isEmpty() || metadataPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both file paths.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            blockManager = new BlockManager(metadataPath);
            blockSize = blockManager.getBlockSize();
            
            Person templatePerson = new Person();
            int recordSize = templatePerson.sizeInBytes();
            blockingFactor = (int) Math.floor((double) (blockSize - 4) / recordSize);
            
            blockInfos = new ArrayList<>();
            BinaryFile binaryFile = new BinaryFile(heapPath);
            long fileSize = binaryFile.getSize();
            int totalBlocks = (int) (fileSize / blockSize);
            
            if (totalBlocks == 0) {
                binaryFile.close();
                updateTable();
                displayAllBlocks();
                refreshButton.setEnabled(true);
                JOptionPane.showMessageDialog(this, 
                    "Heap file is empty. Showing metadata only.", 
                    "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            for (int i = 0; i < totalBlocks; i++) {
                Block<Person> block = readBlock(binaryFile, i, blockSize, blockingFactor);
                if (block != null) {
                    int validCount = block.getValidBlockCount();
                    String status;
                    if (validCount == 0) {
                        status = "Empty";
                    } else if (validCount < blockingFactor) {
                        status = "Partially Full";
                    } else {
                        status = "Full";
                    }
                    
                    List<Person> records = new ArrayList<>();
                    Person[] allSlots = block.getAllRecordSlots();
                    for (int j = 0; j < blockingFactor; j++) {
                        records.add(allSlots[j]);
                    }
                    
                    blockInfos.add(new BlockInfo(i, status, validCount, blockingFactor, records));
                }
            }
            
            binaryFile.close();
            
            updateTable();
            displayAllBlocks();
            refreshButton.setEnabled(true);
            
            String message = "Loaded " + totalBlocks + " blocks successfully.";
            if (totalBlocks == 0) {
                message += "\nHeap is empty - showing metadata only.";
            }
            JOptionPane.showMessageDialog(this, 
                message, 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading heap data: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private Block<Person> readBlock(BinaryFile binaryFile, int blockNumber, 
                                   int blockSize, int blockingFactor) throws IOException {
        long position = (long) blockNumber * blockSize;
        
        if (position >= binaryFile.getSize()) {
            return null;
        }
        
        binaryFile.seek(position);
        byte[] blockData = binaryFile.read(blockSize);
        
        Block<Person> block = new Block<>(blockingFactor, blockSize, Person.class);
        block.FromByteArray(blockData, Person.class);
        
        return block;
    }
    
    private void updateTable() {
        tableModel.setRowCount(0);
        
        if (blockInfos == null) {
            return;
        }
        
        for (BlockInfo info : blockInfos) {
            String usage = String.format("%d/%d (%.1f%%)", 
                info.validCount, info.capacity, 
                (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0));
            
            tableModel.addRow(new Object[]{
                info.blockNumber,
                info.status,
                info.validCount,
                info.capacity,
                usage
            });
        }
    }
    
    private void showBlockDetails() {
        int selectedRow = blocksTable.getSelectedRow();
        if (selectedRow < 0 || blockInfos == null || selectedRow >= blockInfos.size()) {
            detailsArea.setText("");
            return;
        }
        
        BlockInfo info = blockInfos.get(selectedRow);
        displayBlockDetails(info);
    }
    
    private void displayAllBlocks() {
        StringBuilder sb = new StringBuilder();
        
        displayMetadata(sb);
        sb.append("\n");
        
        if (blockInfos == null || blockInfos.isEmpty()) {
            sb.append("NO BLOCKS IN HEAP FILE\n");
            sb.append(repeatString("=", 80)).append("\n");
            sb.append("Heap file is empty. No blocks to display.\n");
        } else {
            sb.append("ALL BLOCKS - COMPLETE VIEW\n");
            sb.append(repeatString("=", 80)).append("\n\n");
            
            for (BlockInfo info : blockInfos) {
                displayBlockDetails(info, sb);
                sb.append("\n");
            }
        }
        
        detailsArea.setText(sb.toString());
        detailsArea.setCaretPosition(0);
    }
    
    private void displayMetadata(StringBuilder sb) {
        if (blockManager == null) {
            sb.append("METADATA: Not available\n");
            return;
        }
        
        sb.append("METADATA INFORMATION\n");
        sb.append(repeatString("=", 80)).append("\n");
        sb.append("Block Size:        ").append(blockSize).append(" bytes\n");
        sb.append("Blocking Factor:   ").append(blockingFactor).append(" records per block\n");
        Person templatePerson = new Person();
        int recordSize = templatePerson.sizeInBytes();
        sb.append("Record Size:       ").append(recordSize).append(" bytes\n");
        sb.append("\n");
        
        List<Integer> emptyBlocks = blockManager.getEmptyBlocks();
        sb.append("Empty Blocks List:\n");
        sb.append(repeatString("-", 80)).append("\n");
        if (emptyBlocks == null || emptyBlocks.isEmpty()) {
            sb.append("  (no empty blocks)\n");
        } else {
            sb.append("  Count: ").append(emptyBlocks.size()).append("\n");
            sb.append("  Block indices: ");
            for (int i = 0; i < emptyBlocks.size(); i++) {
                sb.append(emptyBlocks.get(i));
                if (i < emptyBlocks.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }
        sb.append("\n");
        
        List<Integer> partiallyEmptyBlocks = blockManager.getPartiallyEmptyBlocks();
        sb.append("Partially Empty Blocks List:\n");
        sb.append(repeatString("-", 80)).append("\n");
        if (partiallyEmptyBlocks == null || partiallyEmptyBlocks.isEmpty()) {
            sb.append("  (no partially empty blocks)\n");
        } else {
            sb.append("  Count: ").append(partiallyEmptyBlocks.size()).append("\n");
            sb.append("  Block indices: ");
            for (int i = 0; i < partiallyEmptyBlocks.size(); i++) {
                sb.append(partiallyEmptyBlocks.get(i));
                if (i < partiallyEmptyBlocks.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("\n");
        }
    }
    
    private void displayBlockDetails(BlockInfo info) {
        StringBuilder sb = new StringBuilder();
        displayBlockDetails(info, sb);
        detailsArea.setText(sb.toString());
        detailsArea.setCaretPosition(0);
    }
    
    private void displayBlockDetails(BlockInfo info, StringBuilder sb) {
        sb.append(repeatString("=", 80)).append("\n");
        sb.append("BLOCK #").append(info.blockNumber).append("\n");
        sb.append(repeatString("-", 80)).append("\n");
        sb.append("Status:        ").append(info.status).append("\n");
        sb.append("Valid Records: ").append(info.validCount).append(" / ").append(info.capacity).append("\n");
        sb.append("Usage:         ").append(String.format("%.1f%%", 
            (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0))).append("\n");
        sb.append("Block Size:    ").append(blockSize).append(" bytes\n");
        sb.append(repeatString("-", 80)).append("\n");
        sb.append("ALL RECORD SLOTS (showing all ").append(info.capacity).append(" slots):\n");
        sb.append(repeatString("-", 80)).append("\n\n");
        
        for (int i = 0; i < info.capacity; i++) {
            Person person = (i < info.records.size()) ? info.records.get(i) : null;
            
            sb.append("Slot #").append(String.format("%2d", i)).append(": ");
            if (person == null) {
                sb.append("[EMPTY]\n");
            } else {
                sb.append("\n");
                sb.append("    Name:      ").append(person.name != null ? person.name : "(null)").append("\n");
                sb.append("    Surname:   ").append(person.surname != null ? person.surname : "(null)").append("\n");
                sb.append("    Birthdate: ").append(person.birthdate).append("\n");
                sb.append("    ID:        ").append(person.id != null ? person.id : "(null)").append("\n");
            }
            if (i < info.capacity - 1) {
                sb.append("\n");
            }
        }
        
        sb.append("\n").append(repeatString("=", 80)).append("\n");
    }
    
    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new HeapDebugger().setVisible(true);
        });
    }
}

