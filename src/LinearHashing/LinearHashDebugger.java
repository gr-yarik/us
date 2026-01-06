package LinearHashing;

import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;

import UnsortedFile.Heap;

public class LinearHashDebugger extends JFrame {
    
    private LinearHash<Person> linearHash;
    private BucketHeap<Person> bucketHeap;
    
    // UI Components - Operations Panel
    private JTextField nameField;
    private JTextField surnameField;
    private JTextField birthdateField;
    private JTextField idField;
    private JButton addButton;
    private JButton findButton;
    private JButton removeButton;
    private JButton refreshButton;
    
    // UI Components - Main Buckets
    private JTable mainBucketsTable;
    private DefaultTableModel mainBucketsTableModel;
    private JTextArea mainBucketsDetailsArea;
    
    // UI Components - Overflow Blocks
    private JTable overflowBlocksTable;
    private DefaultTableModel overflowBlocksTableModel;
    private JTextArea overflowBlocksDetailsArea;
    
    // UI Components - Metadata
    private JTextArea metadataArea;
    
    // Data structures
    private List<BucketInfo> mainBucketInfos;
    private List<OverflowBlockInfo> overflowBlockInfos;
    
    private static class BucketInfo {
        int blockNumber;
        String status;
        int validCount;
        int capacity;
        int overflowBucketCount;
        int totalElementCount;
        int firstOverflowBlock;
        List<Person> records;
        
        BucketInfo(int blockNumber, String status, int validCount, int capacity,
                  int overflowBucketCount, int totalElementCount, int firstOverflowBlock,
                  List<Person> records) {
            this.blockNumber = blockNumber;
            this.status = status;
            this.validCount = validCount;
            this.capacity = capacity;
            this.overflowBucketCount = overflowBucketCount;
            this.totalElementCount = totalElementCount;
            this.firstOverflowBlock = firstOverflowBlock;
            this.records = records != null ? records : new ArrayList<>();
        }
    }
    
    private static class OverflowBlockInfo {
        int blockNumber;
        String status;
        int validCount;
        int capacity;
        int nextOverflowBlock;
        List<Person> records;
        
        OverflowBlockInfo(int blockNumber, String status, int validCount, int capacity,
                         int nextOverflowBlock, List<Person> records) {
            this.blockNumber = blockNumber;
            this.status = status;
            this.validCount = validCount;
            this.capacity = capacity;
            this.nextOverflowBlock = nextOverflowBlock;
            this.records = records != null ? records : new ArrayList<>();
        }
    }
    
    public LinearHashDebugger(LinearHash<Person> linearHash) {
        this.linearHash = linearHash;
        this.bucketHeap = linearHash.getBucketHeap();
        this.mainBucketInfos = new ArrayList<>();
        this.overflowBlockInfos = new ArrayList<>();
        
        initializeUI();
        loadData();
    }
    
    private void initializeUI() {
        setTitle("LinearHash Debugger");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Operations Panel (Top)
        JPanel operationsPanel = createOperationsPanel();
        add(operationsPanel, BorderLayout.NORTH);
        
        // Main Content - Horizontal Split
        JSplitPane mainSplitPane = createMainSplitPane();
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Metadata Panel (Bottom)
        JPanel metadataPanel = createMetadataPanel();
        add(metadataPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setSize(1400, 800);
    }
    
    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        nameField = new JTextField(15);
        panel.add(nameField, gbc);
        
        // Surname field
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Surname:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        surnameField = new JTextField(15);
        panel.add(surnameField, gbc);
        
        // Birthdate field
        gbc.gridx = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Birthdate:"), gbc);
        gbc.gridx = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        birthdateField = new JTextField(15);
        panel.add(birthdateField, gbc);
        
        // ID field
        gbc.gridx = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        idField = new JTextField(15);
        panel.add(idField, gbc);
        
        // Buttons row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 8;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        addButton = new JButton("Add Person");
        addButton.addActionListener(e -> performAdd());
        buttonPanel.add(addButton);
        
        findButton = new JButton("Find Person");
        findButton.addActionListener(e -> performFind());
        buttonPanel.add(findButton);
        
        removeButton = new JButton("Remove Person");
        removeButton.addActionListener(e -> performRemove());
        buttonPanel.add(removeButton);
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshView());
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, gbc);
        
        panel.setBorder(BorderFactory.createTitledBorder("Operations"));
        return panel;
    }
    
    private JSplitPane createMainSplitPane() {
        // Left side - Main Buckets
        JPanel mainBucketsPanel = createMainBucketsPanel();
        
        // Right side - Overflow Blocks
        JPanel overflowBlocksPanel = createOverflowBlocksPanel();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainBucketsPanel, overflowBlocksPanel);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.5);
        
        return splitPane;
    }
    
    private JPanel createMainBucketsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table
        String[] columnNames = {"Block #", "Status", "Valid Block Count", "Capacity", "Usage", "Overflow Count", "Total Elements", "First Overflow"};
        mainBucketsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        mainBucketsTable = new JTable(mainBucketsTableModel);
        mainBucketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mainBucketsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showMainBucketDetails();
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(mainBucketsTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 200));
        
        // Details area
        mainBucketsDetailsArea = new JTextArea(15, 50);
        mainBucketsDetailsArea.setEditable(false);
        mainBucketsDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane detailsScrollPane = new JScrollPane(mainBucketsDetailsArea);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Bucket Details"));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.3);
        
        panel.add(splitPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder("Main Buckets"));
        
        return panel;
    }
    
    private JPanel createOverflowBlocksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table
        String[] columnNames = {"Block #", "Status", "Valid Block Count", "Capacity", "Usage", "Next Overflow"};
        overflowBlocksTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        overflowBlocksTable = new JTable(overflowBlocksTableModel);
        overflowBlocksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overflowBlocksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showOverflowBlockDetails();
            }
        });
        JScrollPane tableScrollPane = new JScrollPane(overflowBlocksTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 200));
        
        // Details area
        overflowBlocksDetailsArea = new JTextArea(15, 50);
        overflowBlocksDetailsArea.setEditable(false);
        overflowBlocksDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane detailsScrollPane = new JScrollPane(overflowBlocksDetailsArea);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Overflow Block Details"));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.3);
        
        panel.add(splitPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder("Overflow Blocks"));
        
        return panel;
    }
    
    private JPanel createMetadataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        metadataArea = new JTextArea(4, 80);
        metadataArea.setEditable(false);
        metadataArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(metadataArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("LinearHash Metadata"));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void loadData() {
        try {
            loadMainBucketsData();
            loadOverflowBlocksData();
            updateMainBucketsTable();
            updateOverflowBlocksTable();
            updateMetadata();
            displayAllMainBuckets();
            displayAllOverflowBlocks();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading data: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void loadMainBucketsData() throws IOException {
        mainBucketInfos.clear();
        
        Heap<Person> mainHeap = bucketHeap.getMainBucketsHeap();
        int totalBlocks = mainHeap.getTotalBlockCount();
        int blockingFactor = mainHeap.getBlockingFactor();
        
        for (int i = 0; i < totalBlocks; i++) {
            try {
                Bucket<Person> bucket = (Bucket<Person>) mainHeap.readBlock(i);
                if (bucket != null) {
                    int validCount = bucket.getValidBlockCount();
                    String status;
                    if (validCount == 0) {
                        status = "Empty";
                    } else if (validCount < blockingFactor) {
                        status = "Partially Full";
                    } else {
                        status = "Full";
                    }
                    
                    List<Person> records = new ArrayList<>();
                    Person[] allSlots = bucket.debugGetAllRecords();
                    for (int j = 0; j < blockingFactor; j++) {
                        records.add(allSlots[j]);
                    }
                    
                    mainBucketInfos.add(new BucketInfo(
                        i, status, validCount, blockingFactor,
                        bucket.getTotalOverflowBlockCount(),
                        bucket.getTotalRecordCount(),
                        bucket.getFirstOverflowBlock(),
                        records
                    ));
                }
            } catch (Exception e) {
                // Skip blocks that can't be read
            }
        }
    }
    
    private void loadOverflowBlocksData() throws IOException {
        overflowBlockInfos.clear();
        
        Heap<Person> overflowHeap = bucketHeap.getOverflowHeap();
        int totalBlocks = overflowHeap.getTotalBlockCount();
        int blockingFactor = overflowHeap.getBlockingFactor();
        
        for (int i = 0; i < totalBlocks; i++) {
            try {
                OverflowBlock<Person> overflowBlock = overflowHeap.readBlock(i, OverflowBlock.class);
                if (overflowBlock != null) {
                    int validCount = overflowBlock.getValidBlockCount();
                    String status;
                    if (validCount == 0) {
                        status = "Empty";
                    } else if (validCount < blockingFactor) {
                        status = "Partially Full";
                    } else {
                        status = "Full";
                    }
                    
                    List<Person> records = new ArrayList<>();
                    Person[] allSlots = overflowBlock.debugGetAllRecords();
                    for (int j = 0; j < blockingFactor; j++) {
                        records.add(allSlots[j]);
                    }
                    
                    overflowBlockInfos.add(new OverflowBlockInfo(
                        i, status, validCount, blockingFactor,
                        overflowBlock.getNextOverflowBlock(),
                        records
                    ));
                }
            } catch (Exception e) {
                // Skip blocks that can't be read
            }
        }
    }
    
    private void updateMainBucketsTable() {
        mainBucketsTableModel.setRowCount(0);
        
        for (BucketInfo info : mainBucketInfos) {
            String usage = String.format("%d/%d (%.1f%%)", 
                info.validCount, info.capacity, 
                (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0));
            
            String firstOverflow = info.firstOverflowBlock == -1 ? "None" : String.valueOf(info.firstOverflowBlock);
            
            mainBucketsTableModel.addRow(new Object[]{
                info.blockNumber,
                info.status,
                info.validCount,
                info.capacity,
                usage,
                info.overflowBucketCount,
                info.totalElementCount,
                firstOverflow
            });
        }
    }
    
    private void updateOverflowBlocksTable() {
        overflowBlocksTableModel.setRowCount(0);
        
        for (OverflowBlockInfo info : overflowBlockInfos) {
            String usage = String.format("%d/%d (%.1f%%)", 
                info.validCount, info.capacity, 
                (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0));
            
            String nextOverflow = info.nextOverflowBlock == -1 ? "None" : String.valueOf(info.nextOverflowBlock);
            
            overflowBlocksTableModel.addRow(new Object[]{
                info.blockNumber,
                info.status,
                info.validCount,
                info.capacity,
                usage,
                nextOverflow
            });
        }
    }
    
    private void updateMetadata() {
        StringBuilder sb = new StringBuilder();
        sb.append("Level:              ").append(linearHash.getLevel()).append("\n");
        sb.append("Split Pointer:      ").append(linearHash.getSplitPointer()).append("\n");
        sb.append("Total Primary Buckets: ").append(linearHash.getTotalPrimaryBuckets()).append("\n");
        sb.append("Overflow Ratio:     ").append(String.format("%.4f", linearHash.getOverflowRatio())).append("\n");
        sb.append("Main Buckets Block Size: ").append(bucketHeap.getMainBucketsBlockSize()).append(" bytes\n");
        sb.append("Overflow Block Size:     ").append(bucketHeap.getOverflowBlockSize()).append(" bytes\n");
        
        metadataArea.setText(sb.toString());
    }
    
    private void showMainBucketDetails() {
        int selectedRow = mainBucketsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= mainBucketInfos.size()) {
            mainBucketsDetailsArea.setText("");
            return;
        }
        
        BucketInfo info = mainBucketInfos.get(selectedRow);
        displayBucketDetails(info);
    }
    
    private void showOverflowBlockDetails() {
        int selectedRow = overflowBlocksTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= overflowBlockInfos.size()) {
            overflowBlocksDetailsArea.setText("");
            return;
        }
        
        OverflowBlockInfo info = overflowBlockInfos.get(selectedRow);
        displayOverflowBlockDetails(info);
    }
    
    private void displayAllMainBuckets() {
        StringBuilder sb = new StringBuilder();
        
        if (mainBucketInfos.isEmpty()) {
            sb.append("NO MAIN BUCKETS\n");
            sb.append(repeatString("=", 80)).append("\n");
            sb.append("No buckets in main heap.\n");
        } else {
            sb.append("ALL MAIN BUCKETS - COMPLETE VIEW\n");
            sb.append(repeatString("=", 80)).append("\n\n");
            
            for (BucketInfo info : mainBucketInfos) {
                displayBucketDetails(info, sb);
                sb.append("\n");
            }
        }
        
        mainBucketsDetailsArea.setText(sb.toString());
        mainBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void displayAllOverflowBlocks() {
        StringBuilder sb = new StringBuilder();
        
        if (overflowBlockInfos.isEmpty()) {
            sb.append("NO OVERFLOW BLOCKS\n");
            sb.append(repeatString("=", 80)).append("\n");
            sb.append("No overflow blocks in overflow heap.\n");
        } else {
            sb.append("ALL OVERFLOW BLOCKS - COMPLETE VIEW\n");
            sb.append(repeatString("=", 80)).append("\n\n");
            
            for (OverflowBlockInfo info : overflowBlockInfos) {
                displayOverflowBlockDetails(info, sb);
                sb.append("\n");
            }
        }
        
        overflowBlocksDetailsArea.setText(sb.toString());
        overflowBlocksDetailsArea.setCaretPosition(0);
    }
    
    private void displayBucketDetails(BucketInfo info) {
        StringBuilder sb = new StringBuilder();
        displayBucketDetails(info, sb);
        mainBucketsDetailsArea.setText(sb.toString());
        mainBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void displayBucketDetails(BucketInfo info, StringBuilder sb) {
        sb.append(repeatString("=", 80)).append("\n");
        sb.append("BUCKET #").append(info.blockNumber).append("\n");
        sb.append(repeatString("-", 80)).append("\n");
        sb.append("Status:                ").append(info.status).append("\n");
        sb.append("Valid Block Count:     ").append(info.validCount).append("\n");
        sb.append("Valid Records:         ").append(info.validCount).append(" / ").append(info.capacity).append("\n");
        sb.append("Usage:                 ").append(String.format("%.1f%%", 
            (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0))).append("\n");
        sb.append("Overflow Bucket Count: ").append(info.overflowBucketCount).append("\n");
        sb.append("Total Element Count:   ").append(info.totalElementCount).append("\n");
        sb.append("First Overflow Block:  ").append(info.firstOverflowBlock == -1 ? "None" : String.valueOf(info.firstOverflowBlock)).append("\n");
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
    
    private void displayOverflowBlockDetails(OverflowBlockInfo info) {
        StringBuilder sb = new StringBuilder();
        displayOverflowBlockDetails(info, sb);
        overflowBlocksDetailsArea.setText(sb.toString());
        overflowBlocksDetailsArea.setCaretPosition(0);
    }
    
    private void displayOverflowBlockDetails(OverflowBlockInfo info, StringBuilder sb) {
        sb.append(repeatString("=", 80)).append("\n");
        sb.append("OVERFLOW BLOCK #").append(info.blockNumber).append("\n");
        sb.append(repeatString("-", 80)).append("\n");
        sb.append("Status:                ").append(info.status).append("\n");
        sb.append("Valid Block Count:     ").append(info.validCount).append("\n");
        sb.append("Valid Records:         ").append(info.validCount).append(" / ").append(info.capacity).append("\n");
        sb.append("Usage:                 ").append(String.format("%.1f%%", 
            (info.capacity > 0 ? (100.0 * info.validCount / info.capacity) : 0))).append("\n");
        sb.append("Next Overflow Block: ").append(info.nextOverflowBlock == -1 ? "None" : String.valueOf(info.nextOverflowBlock)).append("\n");
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
    
    private void performAdd() {
        try {
            String name = nameField.getText().trim();
            String surname = surnameField.getText().trim();
            String birthdateStr = birthdateField.getText().trim();
            String id = idField.getText().trim();
            
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "ID is required to add a person.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Person person = new Person();
            person.name = name.isEmpty() ? null : name;
            person.surname = surname.isEmpty() ? null : surname;
            person.id = id;
            
            if (!birthdateStr.isEmpty()) {
                try {
                    person.birthdate = Long.parseLong(birthdateStr);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, 
                        "Invalid birthdate format. Use numeric format (e.g., 20000101).", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                person.birthdate = 0L;
            }
            
            linearHash.insert(person);
            JOptionPane.showMessageDialog(this, 
                "Person added successfully.", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshView();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error adding person: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void performFind() {
        try {
            String id = idField.getText().trim();
            
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "ID is required to find a person.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Person searchPerson = new Person();
            searchPerson.id = id;
            
            Person found = linearHash.get(searchPerson);
            if (found != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("PERSON FOUND:\n");
                sb.append(repeatString("=", 80)).append("\n");
                sb.append("Name:      ").append(found.name != null ? found.name : "(null)").append("\n");
                sb.append("Surname:   ").append(found.surname != null ? found.surname : "(null)").append("\n");
                sb.append("Birthdate: ").append(found.birthdate).append("\n");
                sb.append("ID:        ").append(found.id != null ? found.id : "(null)").append("\n");
                sb.append(repeatString("=", 80)).append("\n");
                
                // Update input fields
                nameField.setText(found.name != null ? found.name : "");
                surnameField.setText(found.surname != null ? found.surname : "");
                birthdateField.setText(String.valueOf(found.birthdate));
                idField.setText(found.id != null ? found.id : "");
                
                JOptionPane.showMessageDialog(this, 
                    sb.toString(), 
                    "Person Found", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Person with ID '" + id + "' not found.", 
                    "Not Found", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error finding person: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void performRemove() {
        try {
            String id = idField.getText().trim();
            
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "ID is required to remove a person.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Person searchPerson = new Person();
            searchPerson.id = id;
            
            boolean success = linearHash.delete(searchPerson);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                    "Person removed successfully.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshView();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Person with ID '" + id + "' not found.", 
                    "Not Found", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error removing person: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void refreshView() {
        try {
            loadData();
            JOptionPane.showMessageDialog(this, 
                "View refreshed successfully.", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error refreshing view: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public static void launch(LinearHash<Person> linearHash) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new LinearHashDebugger(linearHash).setVisible(true);
        });
    }
}

