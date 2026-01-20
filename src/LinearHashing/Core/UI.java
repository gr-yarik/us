package LinearHashing.Core;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import LinearHashing.*;
import UnsortedFile.Heap;
import UnsortedFile.BlockManager;
import java.util.Collections;

public class UI extends JFrame {
    
    // ui should support all 8 operations suported by the DatabaseCore.
    //use radiogroup to allow user to select the operation.

    // look at the LinearHashDebugger UI; this ui should also have an option to 
    // see the full contents of both files (both tests and persons, with all the contents)
    // use a second radiogroup to allow user which of two file contents they want to see
    //the file contents should take biggest space
    //dont give popups if there were no errors, popup only for error

    //files should be read only when user presses re-read the files button

    //UI allows user to select a different folder that contains database files (8 or 0)
    // UI allows to close the current database and then resume work with the same one
    
    private UIController controller;
    
    // Operation selection
    private JRadioButton rbInsertPCR, rbSearchPerson, rbSearchPCR, rbInsertPerson;
    private JRadioButton rbDeletePCR, rbDeletePerson, rbEditPerson, rbEditPCR;
    private ButtonGroup operationGroup;
    
    // File view selection
    private JRadioButton rbViewPersons, rbViewPCRIndex;
    private ButtonGroup viewGroup;
    
    // Input fields panel
    private JPanel inputPanel;
    private CardLayout inputCardLayout;
    
    // Content display - Debugger style
    private CardLayout contentCardLayout;
    private JPanel contentPanel;
    
    // Persons view components
    private JTable personsBucketsTable;
    private DefaultTableModel personsBucketsTableModel;
    private JTextArea personsBucketsDetailsArea;
    private JTable personsOverflowTable;
    private DefaultTableModel personsOverflowTableModel;
    private JTextArea personsOverflowDetailsArea;
    
    // PCR Index view components
    private JTable pcrBucketsTable;
    private DefaultTableModel pcrBucketsTableModel;
    private JTextArea pcrBucketsDetailsArea;
    private JTable pcrOverflowTable;
    private DefaultTableModel pcrOverflowTableModel;
    private JTextArea pcrOverflowDetailsArea;
    
    // Data caches
    private List<BucketInfo<Person>> personBucketInfos = new ArrayList<>();
    private List<OverflowInfo<Person>> personOverflowInfos = new ArrayList<>();
    private List<BucketInfo<PCRIndex>> pcrBucketInfos = new ArrayList<>();
    private List<OverflowInfo<PCRIndex>> pcrOverflowInfos = new ArrayList<>();
    
    // Buttons
    private JButton btnExecute, btnRereadFiles, btnSelectFolder, btnCloseDatabase;
    private JButton btnFillRandom, btnGenerate;
    
    // Generator fields
    private JSpinner spnPersonCount, spnTestsPerPerson;
    
    // Status
    private JLabel lblStatus;
    
    // Random generator
    private Random random = new Random();
    private int generatedIdCounter = 10000000;
    private int generatedTestCodeCounter = 1000;
    
    // Date format
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // Name arrays for generation
    private static final String[] FIRST_NAMES = {
        "Alex", "Bob", "Charlie", "David", "Emma", "Frank", "Grace", "Henry",
        "Ivy", "Jack", "Kate", "Liam", "Mia", "Noah", "Olivia", "Paul",
        "Quinn", "Rachel", "Sam", "Tina", "Uma", "Victor", "Wendy", "Xavier",
        "Yara", "Zoe", "Adam", "Bella", "Chris", "Diana"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Wilson", "Anderson", "Thomas", "Taylor",
        "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris", "Sanchez",
        "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young"
    };
    
    private static final String[] NOTES = {
        "Normal", "Retest", "Urgent", "Routine", "Follow-up", "Initial", "Final", ""
    };
    
    // ==================== Data structures for block info ====================
    
    private static class BucketInfo<T> {
        int blockNumber;
        int validCount;
        int capacity;
        int overflowBlockCount;
        int totalElementCount;
        int firstOverflowBlock;
        List<T> records = new ArrayList<>();
    }
    
    private static class OverflowInfo<T> {
        int blockNumber;
        int validCount;
        int capacity;
        int nextOverflowBlock;
        List<T> records = new ArrayList<>();
    }
    
    public UI() {
        controller = new UIController();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("S2 Hrytsun");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Top panel: folder selection and database status
        add(createTopPanel(), BorderLayout.NORTH);
        
        // Left panel: operations
        add(createLeftPanel(), BorderLayout.WEST);
        
        // Center: file contents with debugger-style view
        add(createCenterPanel(), BorderLayout.CENTER);
        
        // Handle window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.closeDatabase();
            }
        });
        
        setSize(1400, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        updateStatus();
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Database"));
        
        btnSelectFolder = new JButton("Select Folder...");
        btnSelectFolder.addActionListener(e -> selectFolder());
        
        btnCloseDatabase = new JButton("Close Database");
        btnCloseDatabase.addActionListener(e -> {
            controller.closeDatabase();
            updateStatus();
            clearAllViews();
        });
        
        lblStatus = new JLabel("No database open");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
        
        panel.add(btnSelectFolder);
        panel.add(btnCloseDatabase);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblStatus);
        
        return panel;
    }
    
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setPreferredSize(new Dimension(380, 0));
        
        // Operations radio group
        JPanel opsPanel = new JPanel(new GridLayout(0, 1, 2, 2));
        opsPanel.setBorder(BorderFactory.createTitledBorder("Operations"));
        
        operationGroup = new ButtonGroup();
        rbInsertPerson = createOperationRadio("Insert Person", opsPanel);
        rbInsertPCR = createOperationRadio("Insert PCR Test", opsPanel);
        rbSearchPerson = createOperationRadio("Search Person", opsPanel);
        rbSearchPCR = createOperationRadio("Search PCR by Code", opsPanel);
        rbEditPerson = createOperationRadio("Edit Person", opsPanel);
        rbEditPCR = createOperationRadio("Edit PCR Test", opsPanel);
        rbDeletePerson = createOperationRadio("Delete Person", opsPanel);
        rbDeletePCR = createOperationRadio("Delete PCR Test", opsPanel);
        
        rbInsertPerson.setSelected(true);
        
        // Input fields with card layout
        inputCardLayout = new CardLayout();
        inputPanel = new JPanel(inputCardLayout);
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        
        inputPanel.add(createPersonInputPanel(), "PERSON");
        inputPanel.add(createPCRInputPanel(), "PCR");
        inputPanel.add(createSearchPersonPanel(), "SEARCH_PERSON");
        inputPanel.add(createSearchPCRPanel(), "SEARCH_PCR");
        inputPanel.add(createEditPersonPanel(), "EDIT_PERSON");
        inputPanel.add(createEditPCRPanel(), "EDIT_PCR");
        inputPanel.add(createDeletePersonPanel(), "DELETE_PERSON");
        inputPanel.add(createDeletePCRPanel(), "DELETE_PCR");
        
        // Execute and Fill Random buttons
        btnExecute = new JButton("Execute");
        btnExecute.setFont(btnExecute.getFont().deriveFont(Font.BOLD, 14f));
        btnExecute.addActionListener(e -> executeOperation());
        
        btnFillRandom = new JButton("Fill Random");
        btnFillRandom.addActionListener(e -> fillRandomValues());
        
        JPanel executePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        executePanel.add(btnExecute);
        executePanel.add(btnFillRandom);
        
        // Generator panel
        JPanel generatorPanel = new JPanel(new GridBagLayout());
        generatorPanel.setBorder(BorderFactory.createTitledBorder("Bulk Generator"));
        GridBagConstraints gbcGen = new GridBagConstraints();
        gbcGen.insets = new Insets(2, 5, 2, 5);
        gbcGen.anchor = GridBagConstraints.WEST;
        
        gbcGen.gridx = 0; gbcGen.gridy = 0;
        generatorPanel.add(new JLabel("Persons:"), gbcGen);
        gbcGen.gridx = 1;
        spnPersonCount = new JSpinner(new SpinnerNumberModel(10, 1, 10000, 1));
        generatorPanel.add(spnPersonCount, gbcGen);
        
        gbcGen.gridx = 0; gbcGen.gridy = 1;
        generatorPanel.add(new JLabel("Tests/Person:"), gbcGen);
        gbcGen.gridx = 1;
        spnTestsPerPerson = new JSpinner(new SpinnerNumberModel(2, 0, 6, 1));
        generatorPanel.add(spnTestsPerPerson, gbcGen);
        
        gbcGen.gridx = 0; gbcGen.gridy = 2; gbcGen.gridwidth = 2;
        gbcGen.anchor = GridBagConstraints.CENTER;
        btnGenerate = new JButton("Generate Data");
        btnGenerate.addActionListener(e -> generateBulkData());
        generatorPanel.add(btnGenerate, gbcGen);
        
        // View selection radio group
        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewPanel.setBorder(BorderFactory.createTitledBorder("View File Contents"));
        
        viewGroup = new ButtonGroup();
        rbViewPersons = new JRadioButton("Persons File", true);
        rbViewPCRIndex = new JRadioButton("PCR Index File");
        rbViewPersons.addActionListener(e -> switchContentView());
        rbViewPCRIndex.addActionListener(e -> switchContentView());
        viewGroup.add(rbViewPersons);
        viewGroup.add(rbViewPCRIndex);
        viewPanel.add(rbViewPersons);
        viewPanel.add(rbViewPCRIndex);
        
        btnRereadFiles = new JButton("Re-read Files");
        btnRereadFiles.addActionListener(e -> rereadAllFiles());
        viewPanel.add(btnRereadFiles);
        
        // Assemble left panel
        JPanel topLeft = new JPanel(new BorderLayout(5, 5));
        topLeft.add(opsPanel, BorderLayout.NORTH);
        topLeft.add(inputPanel, BorderLayout.CENTER);
        topLeft.add(executePanel, BorderLayout.SOUTH);
        
        // Bottom section with generator and view
        JPanel bottomSection = new JPanel(new BorderLayout(5, 5));
        bottomSection.add(generatorPanel, BorderLayout.NORTH);
        bottomSection.add(viewPanel, BorderLayout.SOUTH);
        
        panel.add(topLeft, BorderLayout.CENTER);
        panel.add(bottomSection, BorderLayout.SOUTH);
        
        showInputCard();
        
        return panel;
    }
    
    private JRadioButton createOperationRadio(String text, JPanel panel) {
        JRadioButton rb = new JRadioButton(text);
        rb.addActionListener(e -> showInputCard());
        operationGroup.add(rb);
        panel.add(rb);
        return rb;
    }
    
    private void showInputCard() {
        if (rbInsertPerson.isSelected()) {
            inputCardLayout.show(inputPanel, "PERSON");
        } else if (rbInsertPCR.isSelected()) {
            inputCardLayout.show(inputPanel, "PCR");
        } else if (rbSearchPerson.isSelected()) {
            inputCardLayout.show(inputPanel, "SEARCH_PERSON");
        } else if (rbSearchPCR.isSelected()) {
            inputCardLayout.show(inputPanel, "SEARCH_PCR");
        } else if (rbEditPerson.isSelected()) {
            inputCardLayout.show(inputPanel, "EDIT_PERSON");
        } else if (rbEditPCR.isSelected()) {
            inputCardLayout.show(inputPanel, "EDIT_PCR");
        } else if (rbDeletePerson.isSelected()) {
            inputCardLayout.show(inputPanel, "DELETE_PERSON");
        } else if (rbDeletePCR.isSelected()) {
            inputCardLayout.show(inputPanel, "DELETE_PCR");
        }
    }
    
    // ==================== Date/Time Picker Helper ====================
    
    private JPanel createDateTimeField(String labelText, JPanel parentPanel, GridBagConstraints gbc, int row) {
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        JTextField tfManual = new JTextField(12);
        JButton btnPicker = new JButton("📅");
        btnPicker.setMargin(new Insets(2, 4, 2, 4));
        JButton btnNow = new JButton("Now");
        btnNow.setMargin(new Insets(2, 4, 2, 4));
        
        btnNow.addActionListener(e -> {
            tfManual.setText(String.valueOf(System.currentTimeMillis()));
        });
        
        btnPicker.addActionListener(e -> {
            showDateTimePicker(tfManual);
        });
        
        datePanel.add(tfManual);
        datePanel.add(btnPicker);
        datePanel.add(btnNow);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        parentPanel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        parentPanel.add(datePanel, gbc);
        gbc.gridwidth = 1;
        
        datePanel.putClientProperty("textField", tfManual);
        return datePanel;
    }
    
    private void showDateTimePicker(JTextField targetField) {
        JDialog dialog = new JDialog(this, "Select Date and Time", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        Calendar cal = Calendar.getInstance();
        
        // Date spinners
        SpinnerNumberModel yearModel = new SpinnerNumberModel(cal.get(Calendar.YEAR), 1900, 2100, 1);
        SpinnerNumberModel monthModel = new SpinnerNumberModel(cal.get(Calendar.MONTH) + 1, 1, 12, 1);
        SpinnerNumberModel dayModel = new SpinnerNumberModel(cal.get(Calendar.DAY_OF_MONTH), 1, 31, 1);
        
        JSpinner yearSpinner = new JSpinner(yearModel);
        JSpinner monthSpinner = new JSpinner(monthModel);
        JSpinner daySpinner = new JSpinner(dayModel);
        
        // Time spinners
        SpinnerNumberModel hourModel = new SpinnerNumberModel(cal.get(Calendar.HOUR_OF_DAY), 0, 23, 1);
        SpinnerNumberModel minModel = new SpinnerNumberModel(cal.get(Calendar.MINUTE), 0, 59, 1);
        SpinnerNumberModel secModel = new SpinnerNumberModel(cal.get(Calendar.SECOND), 0, 59, 1);
        
        JSpinner hourSpinner = new JSpinner(hourModel);
        JSpinner minSpinner = new JSpinner(minModel);
        JSpinner secSpinner = new JSpinner(secModel);
        
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(yearSpinner, gbc);
        
        gbc.gridx = 2;
        mainPanel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 3;
        mainPanel.add(monthSpinner, gbc);
        
        gbc.gridx = 4;
        mainPanel.add(new JLabel("Day:"), gbc);
        gbc.gridx = 5;
        mainPanel.add(daySpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Hour:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(hourSpinner, gbc);
        
        gbc.gridx = 2;
        mainPanel.add(new JLabel("Min:"), gbc);
        gbc.gridx = 3;
        mainPanel.add(minSpinner, gbc);
        
        gbc.gridx = 4;
        mainPanel.add(new JLabel("Sec:"), gbc);
        gbc.gridx = 5;
        mainPanel.add(secSpinner, gbc);
        
        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        
        btnOk.addActionListener(e -> {
            Calendar result = Calendar.getInstance();
            result.set((int) yearSpinner.getValue(), (int) monthSpinner.getValue() - 1, 
                       (int) daySpinner.getValue(), (int) hourSpinner.getValue(),
                       (int) minSpinner.getValue(), (int) secSpinner.getValue());
            result.set(Calendar.MILLISECOND, 0);
            targetField.setText(String.valueOf(result.getTimeInMillis()));
            dialog.dispose();
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnOk);
        btnPanel.add(btnCancel);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    // ==================== Input Panels ====================
    
    private JPanel createPersonInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tfPersonId = new JTextField(15);
        JTextField tfName = new JTextField(15);
        JTextField tfSurname = new JTextField(15);
        
        addLabeledField(panel, gbc, 0, "Person ID:", tfPersonId);
        addLabeledField(panel, gbc, 1, "Name:", tfName);
        addLabeledField(panel, gbc, 2, "Surname:", tfSurname);
        JPanel bdPanel = createDateTimeField("Birthdate:", panel, gbc, 3);
        
        panel.putClientProperty("personId", tfPersonId);
        panel.putClientProperty("name", tfName);
        panel.putClientProperty("surname", tfSurname);
        panel.putClientProperty("birthdate", bdPanel.getClientProperty("textField"));
        
        return panel;
    }
    
    private JPanel createPCRInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tfPatientId = new JTextField(15);
        JTextField tfTestCode = new JTextField(15);
        JCheckBox cbTestResult = new JCheckBox("Positive");
        JTextField tfTestValue = new JTextField(15);
        JTextField tfNote = new JTextField(15);
        
        addLabeledField(panel, gbc, 0, "Patient ID:", tfPatientId);
        addLabeledField(panel, gbc, 1, "Test Code:", tfTestCode);
        JPanel dtPanel = createDateTimeField("DateTime:", panel, gbc, 2);
        
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Result:"), gbc);
        gbc.gridx = 1;
        panel.add(cbTestResult, gbc);
        
        addLabeledField(panel, gbc, 4, "Test Value:", tfTestValue);
        addLabeledField(panel, gbc, 5, "Note:", tfNote);
        
        panel.putClientProperty("patientId", tfPatientId);
        panel.putClientProperty("testCode", tfTestCode);
        panel.putClientProperty("dateTime", dtPanel.getClientProperty("textField"));
        panel.putClientProperty("testResult", cbTestResult);
        panel.putClientProperty("testValue", tfTestValue);
        panel.putClientProperty("note", tfNote);
        
        return panel;
    }
    
    private JPanel createSearchPersonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tf = new JTextField(15);
        addLabeledField(panel, gbc, 0, "Person ID:", tf);
        panel.putClientProperty("searchPersonId", tf);
        
        return panel;
    }
    
    private JPanel createSearchPCRPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tf = new JTextField(15);
        addLabeledField(panel, gbc, 0, "Test Code:", tf);
        panel.putClientProperty("searchTestCode", tf);
        
        return panel;
    }
    
    private JPanel createEditPersonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tfId = new JTextField(15);
        JTextField tfNewName = new JTextField(15);
        JTextField tfNewSurname = new JTextField(15);
        
        addLabeledField(panel, gbc, 0, "Person ID:", tfId);
        addLabeledField(panel, gbc, 1, "New Name (empty=keep):", tfNewName);
        addLabeledField(panel, gbc, 2, "New Surname (empty=keep):", tfNewSurname);
        JPanel bdPanel = createDateTimeField("New Birthdate:", panel, gbc, 3);
        
        panel.putClientProperty("editPersonId", tfId);
        panel.putClientProperty("editPersonName", tfNewName);
        panel.putClientProperty("editPersonSurname", tfNewSurname);
        panel.putClientProperty("editPersonBirthdate", bdPanel.getClientProperty("textField"));
        
        return panel;
    }
    
    private JPanel createEditPCRPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tfCode = new JTextField(15);
        JComboBox<String> cbNewResult = new JComboBox<>(new String[]{"(keep)", "Positive", "Negative"});
        JTextField tfNewValue = new JTextField(15);
        JTextField tfNewNote = new JTextField(15);
        
        addLabeledField(panel, gbc, 0, "Test Code:", tfCode);
        JPanel dtPanel = createDateTimeField("New DateTime:", panel, gbc, 1);
        
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("New Result:"), gbc);
        gbc.gridx = 1;
        panel.add(cbNewResult, gbc);
        
        addLabeledField(panel, gbc, 3, "New Value (empty=keep):", tfNewValue);
        addLabeledField(panel, gbc, 4, "New Note (empty=keep):", tfNewNote);
        
        panel.putClientProperty("editPCRCode", tfCode);
        panel.putClientProperty("editPCRDateTime", dtPanel.getClientProperty("textField"));
        panel.putClientProperty("editPCRResult", cbNewResult);
        panel.putClientProperty("editPCRValue", tfNewValue);
        panel.putClientProperty("editPCRNote", tfNewNote);
        
        return panel;
    }
    
    private JPanel createDeletePersonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tf = new JTextField(15);
        addLabeledField(panel, gbc, 0, "Person ID:", tf);
        panel.putClientProperty("deletePersonId", tf);
        
        return panel;
    }
    
    private JPanel createDeletePCRPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        JTextField tf = new JTextField(15);
        addLabeledField(panel, gbc, 0, "Test Code:", tf);
        panel.putClientProperty("deletePCRCode", tf);
        
        return panel;
    }
    
    private void addLabeledField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }
    
    // ==================== Center Panel - Debugger Style ====================
    
    private JPanel createCenterPanel() {
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        contentPanel.setBorder(BorderFactory.createTitledBorder("File Contents"));
        
        // Persons view
        contentPanel.add(createPersonsDebugView(), "PERSONS");
        
        // PCR Index view
        contentPanel.add(createPCRDebugView(), "PCR");
        
        return contentPanel;
    }
    
    private JPanel createPersonsDebugView() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Main buckets section
        JPanel mainBucketsPanel = new JPanel(new BorderLayout());
        mainBucketsPanel.setBorder(BorderFactory.createTitledBorder("Main Buckets (Persons)"));
        
        String[] bucketCols = {"Block#", "Valid Count", "Capacity", "Overflow Block Count", "Total Elements", "First Overflow Block"};
        personsBucketsTableModel = new DefaultTableModel(bucketCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        personsBucketsTable = new JTable(personsBucketsTableModel);
        personsBucketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        personsBucketsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPersonBucketDetails();
        });
        
        personsBucketsDetailsArea = new JTextArea(12, 40);
        personsBucketsDetailsArea.setEditable(false);
        personsBucketsDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JSplitPane bucketSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(personsBucketsTable), new JScrollPane(personsBucketsDetailsArea));
        bucketSplit.setDividerLocation(150);
        mainBucketsPanel.add(bucketSplit, BorderLayout.CENTER);
        
        // Overflow blocks section
        JPanel overflowPanel = new JPanel(new BorderLayout());
        overflowPanel.setBorder(BorderFactory.createTitledBorder("Overflow Blocks (Persons)"));
        
        String[] overflowCols = {"Block#", "Valid Count", "Capacity", "Next Overflow Block"};
        personsOverflowTableModel = new DefaultTableModel(overflowCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        personsOverflowTable = new JTable(personsOverflowTableModel);
        personsOverflowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        personsOverflowTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPersonOverflowDetails();
        });
        
        personsOverflowDetailsArea = new JTextArea(12, 40);
        personsOverflowDetailsArea.setEditable(false);
        personsOverflowDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JSplitPane overflowSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(personsOverflowTable), new JScrollPane(personsOverflowDetailsArea));
        overflowSplit.setDividerLocation(150);
        overflowPanel.add(overflowSplit, BorderLayout.CENTER);
        
        // Horizontal split between main and overflow
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainBucketsPanel, overflowPanel);
        mainSplit.setDividerLocation(500);
        mainSplit.setResizeWeight(0.5);
        
        panel.add(mainSplit, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createPCRDebugView() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Main buckets section
        JPanel mainBucketsPanel = new JPanel(new BorderLayout());
        mainBucketsPanel.setBorder(BorderFactory.createTitledBorder("Main Buckets (PCR Index)"));
        
        String[] bucketCols = {"Block#", "Valid Count", "Capacity", "Overflow Block Count", "Total Elements", "First Overflow Block"};
        pcrBucketsTableModel = new DefaultTableModel(bucketCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pcrBucketsTable = new JTable(pcrBucketsTableModel);
        pcrBucketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pcrBucketsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPCRBucketDetails();
        });
        
        pcrBucketsDetailsArea = new JTextArea(12, 40);
        pcrBucketsDetailsArea.setEditable(false);
        pcrBucketsDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JSplitPane bucketSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(pcrBucketsTable), new JScrollPane(pcrBucketsDetailsArea));
        bucketSplit.setDividerLocation(150);
        mainBucketsPanel.add(bucketSplit, BorderLayout.CENTER);
        
        // Overflow blocks section
        JPanel overflowPanel = new JPanel(new BorderLayout());
        overflowPanel.setBorder(BorderFactory.createTitledBorder("Overflow Blocks (PCR Index)"));
        
        String[] overflowCols = {"Block#", "Valid Count", "Capacity", "Next Overflow Block"};
        pcrOverflowTableModel = new DefaultTableModel(overflowCols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pcrOverflowTable = new JTable(pcrOverflowTableModel);
        pcrOverflowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pcrOverflowTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showPCROverflowDetails();
        });
        
        pcrOverflowDetailsArea = new JTextArea(12, 40);
        pcrOverflowDetailsArea.setEditable(false);
        pcrOverflowDetailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        JSplitPane overflowSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(pcrOverflowTable), new JScrollPane(pcrOverflowDetailsArea));
        overflowSplit.setDividerLocation(150);
        overflowPanel.add(overflowSplit, BorderLayout.CENTER);
        
        // Horizontal split
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainBucketsPanel, overflowPanel);
        mainSplit.setDividerLocation(500);
        mainSplit.setResizeWeight(0.5);
        
        panel.add(mainSplit, BorderLayout.CENTER);
        return panel;
    }
    
    private void switchContentView() {
        if (rbViewPersons.isSelected()) {
            contentCardLayout.show(contentPanel, "PERSONS");
        } else {
            contentCardLayout.show(contentPanel, "PCR");
        }
    }
    
    // ==================== Data Loading ====================
    
    private void rereadAllFiles() {
        if (!controller.isDatabaseOpen()) {
            clearAllViews();
            return;
        }
        
        loadPersonsData();
        loadPCRData();
        updatePersonsTables();
        updatePCRTables();
        displayAllPersonBuckets();
        displayAllPersonOverflows();
        displayAllPCRBuckets();
        displayAllPCROverflows();
    }
    
    @SuppressWarnings("unchecked")
    private void loadPersonsData() {
        personBucketInfos.clear();
        personOverflowInfos.clear();
        
        try {
            LinearHash<Person> personFile = controller.getDatabaseCore().getPersonFile();
            BucketHeap<Person> bucketHeap = personFile.getBucketHeap();
            Heap<Person> mainHeap = bucketHeap.getMainBucketsHeap();
            Heap<Person> overflowHeap = bucketHeap.getOverflowHeap();
            
            int totalBuckets = personFile.getTotalPrimaryBuckets();
            int blockingFactor = mainHeap.getBlockingFactor();
            
            for (int i = 0; i < totalBuckets; i++) {
                try {
                    Bucket<Person> bucket = (Bucket<Person>) mainHeap.readBlock(i);
                    BucketInfo<Person> info = new BucketInfo<>();
                    info.blockNumber = i;
                    info.validCount = bucket.getValidBlockCount();
                    info.capacity = blockingFactor;
                    info.overflowBlockCount = bucket.getTotalOverflowBlockCount();
                    info.totalElementCount = bucket.getTotalRecordCount();
                    info.firstOverflowBlock = bucket.getFirstOverflowBlock();
                    for (Person p : bucket.getAllValidRecords()) {
                        info.records.add(p);
                    }
                    personBucketInfos.add(info);
                } catch (Exception e) { }
            }
            
            int totalOverflow = overflowHeap.getTotalBlockCount();
            int ovBlockingFactor = overflowHeap.getBlockingFactor();
            
            for (int i = 0; i < totalOverflow; i++) {
                try {
                    OverflowBlock<Person> block = overflowHeap.readBlock(i, OverflowBlock.class);
                    OverflowInfo<Person> info = new OverflowInfo<>();
                    info.blockNumber = i;
                    info.validCount = block.getValidBlockCount();
                    info.capacity = ovBlockingFactor;
                    info.nextOverflowBlock = block.getNextOverflowBlock();
                    for (Person p : block.getAllValidRecords()) {
                        info.records.add(p);
                    }
                    personOverflowInfos.add(info);
                } catch (Exception e) { }
            }
        } catch (Exception e) { }
    }
    
    @SuppressWarnings("unchecked")
    private void loadPCRData() {
        pcrBucketInfos.clear();
        pcrOverflowInfos.clear();
        
        try {
            LinearHash<PCRIndex> indexFile = controller.getDatabaseCore().getIndexFile();
            BucketHeap<PCRIndex> bucketHeap = indexFile.getBucketHeap();
            Heap<PCRIndex> mainHeap = bucketHeap.getMainBucketsHeap();
            Heap<PCRIndex> overflowHeap = bucketHeap.getOverflowHeap();
            
            int totalBuckets = indexFile.getTotalPrimaryBuckets();
            int blockingFactor = mainHeap.getBlockingFactor();
            
            for (int i = 0; i < totalBuckets; i++) {
                try {
                    Bucket<PCRIndex> bucket = (Bucket<PCRIndex>) mainHeap.readBlock(i);
                    BucketInfo<PCRIndex> info = new BucketInfo<>();
                    info.blockNumber = i;
                    info.validCount = bucket.getValidBlockCount();
                    info.capacity = blockingFactor;
                    info.overflowBlockCount = bucket.getTotalOverflowBlockCount();
                    info.totalElementCount = bucket.getTotalRecordCount();
                    info.firstOverflowBlock = bucket.getFirstOverflowBlock();
                    for (PCRIndex idx : bucket.getAllValidRecords()) {
                        info.records.add(idx);
                    }
                    pcrBucketInfos.add(info);
                } catch (Exception e) { }
            }
            
            int totalOverflow = overflowHeap.getTotalBlockCount();
            int ovBlockingFactor = overflowHeap.getBlockingFactor();
            
            for (int i = 0; i < totalOverflow; i++) {
                try {
                    OverflowBlock<PCRIndex> block = overflowHeap.readBlock(i, OverflowBlock.class);
                    OverflowInfo<PCRIndex> info = new OverflowInfo<>();
                    info.blockNumber = i;
                    info.validCount = block.getValidBlockCount();
                    info.capacity = ovBlockingFactor;
                    info.nextOverflowBlock = block.getNextOverflowBlock();
                    for (PCRIndex idx : block.getAllValidRecords()) {
                        info.records.add(idx);
                    }
                    pcrOverflowInfos.add(info);
                } catch (Exception e) { }
            }
        } catch (Exception e) { }
    }
    
    private void updatePersonsTables() {
        personsBucketsTableModel.setRowCount(0);
        for (BucketInfo<Person> info : personBucketInfos) {
            personsBucketsTableModel.addRow(new Object[]{
                info.blockNumber, info.validCount, info.capacity,
                info.overflowBlockCount, info.totalElementCount, info.firstOverflowBlock
            });
        }
        
        personsOverflowTableModel.setRowCount(0);
        for (OverflowInfo<Person> info : personOverflowInfos) {
            personsOverflowTableModel.addRow(new Object[]{
                info.blockNumber, info.validCount, info.capacity, info.nextOverflowBlock
            });
        }
    }
    
    private void updatePCRTables() {
        pcrBucketsTableModel.setRowCount(0);
        for (BucketInfo<PCRIndex> info : pcrBucketInfos) {
            pcrBucketsTableModel.addRow(new Object[]{
                info.blockNumber, info.validCount, info.capacity,
                info.overflowBlockCount, info.totalElementCount, info.firstOverflowBlock
            });
        }
        
        pcrOverflowTableModel.setRowCount(0);
        for (OverflowInfo<PCRIndex> info : pcrOverflowInfos) {
            pcrOverflowTableModel.addRow(new Object[]{
                info.blockNumber, info.validCount, info.capacity, info.nextOverflowBlock
            });
        }
    }
    
    // ==================== Details Display ====================
    
    private void displayAllPersonBuckets() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALL PERSON BUCKETS\n");
        sb.append("==================\n\n");
        for (BucketInfo<Person> info : personBucketInfos) {
            appendPersonBucketDetails(sb, info);
        }
        personsBucketsDetailsArea.setText(sb.toString());
        personsBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void displayAllPersonOverflows() {
        
        
        StringBuilder sb = new StringBuilder();
        
        
        
        try {
            LinearHash<Person> personFile = controller.getDatabaseCore().getPersonFile();
            BucketHeap<Person> bucketHeap = personFile.getBucketHeap();
            Heap<Person> overflowHeap = bucketHeap.getOverflowHeap();
            BlockManager overflowBlockManager = overflowHeap.getBlockManager();
            
            if (overflowBlockManager != null) {
              
                java.util.List<Integer> emptyBlocks = overflowBlockManager.getEmptyBlocks();
                java.util.List<Integer> partiallyEmptyBlocks = overflowBlockManager.getPartiallyEmptyBlocks();
                Collections.sort(emptyBlocks);
                Collections.sort(partiallyEmptyBlocks);
                
                sb.append("Empty Blocks (").append(emptyBlocks.size()).append("): ");
                if (emptyBlocks.isEmpty()) {
                    sb.append("none");
                } else {
                    sb.append(emptyBlocks.toString());
                }
                sb.append("\n");
                
                // sb.append("Partially Empty Blocks (").append(partiallyEmptyBlocks.size()).append("): ");
                // if (partiallyEmptyBlocks.isEmpty()) {
                //     sb.append("none");
                // } else {
                //     sb.append(partiallyEmptyBlocks.toString());
                // }
                // sb.append("\n");
            }
        } catch (Exception e) { }
        
        // sb.append("ALL PERSON OVERFLOW BLOCKS\n");
        // sb.append("==========================\n\n");
        for (OverflowInfo<Person> info : personOverflowInfos) {
            appendPersonOverflowDetails(sb, info);
        }
        
        
        personsOverflowDetailsArea.setText(sb.toString());
        personsOverflowDetailsArea.setCaretPosition(0);
    }
    
    private void displayAllPCRBuckets() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALL PCR INDEX BUCKETS\n");
        sb.append("=====================\n\n");
        for (BucketInfo<PCRIndex> info : pcrBucketInfos) {
            appendPCRBucketDetails(sb, info);
        }
        pcrBucketsDetailsArea.setText(sb.toString());
        pcrBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void displayAllPCROverflows() {
        
        
        StringBuilder sb = new StringBuilder();
        
        
        
        try {
            LinearHash<PCRIndex> indexFile = controller.getDatabaseCore().getIndexFile();
            BucketHeap<PCRIndex> bucketHeap = indexFile.getBucketHeap();
            Heap<PCRIndex> overflowHeap = bucketHeap.getOverflowHeap();
            BlockManager overflowBlockManager = overflowHeap.getBlockManager();
            
            if (overflowBlockManager != null) {
              
                java.util.List<Integer> emptyBlocks = overflowBlockManager.getEmptyBlocks();
                java.util.List<Integer> partiallyEmptyBlocks = overflowBlockManager.getPartiallyEmptyBlocks();
                Collections.sort(emptyBlocks);
                Collections.sort(partiallyEmptyBlocks);
                
                sb.append("Empty Blocks (").append(emptyBlocks.size()).append("): ");
                if (emptyBlocks.isEmpty()) {
                    sb.append("none");
                } else {
                    sb.append(emptyBlocks.toString());
                }
                sb.append("\n");
                
                // sb.append("Partially Empty Blocks (").append(partiallyEmptyBlocks.size()).append("): ");
                // if (partiallyEmptyBlocks.isEmpty()) {
                //     sb.append("none");
                // } else {
                //     sb.append(partiallyEmptyBlocks.toString());
                // }
                // sb.append("\n");
            }
        } catch (Exception e) { }
        
        // sb.append("ALL PCR INDEX OVERFLOW BLOCKS\n");
        // sb.append("=============================\n\n");
        for (OverflowInfo<PCRIndex> info : pcrOverflowInfos) {
            appendPCROverflowDetails(sb, info);
        }
        
        
        pcrOverflowDetailsArea.setText(sb.toString());
        pcrOverflowDetailsArea.setCaretPosition(0);
    }
    
    private void showPersonBucketDetails() {
        int row = personsBucketsTable.getSelectedRow();
        if (row < 0 || row >= personBucketInfos.size()) return;
        StringBuilder sb = new StringBuilder();
        appendPersonBucketDetails(sb, personBucketInfos.get(row));
        personsBucketsDetailsArea.setText(sb.toString());
        personsBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void showPersonOverflowDetails() {
        int row = personsOverflowTable.getSelectedRow();
        if (row < 0 || row >= personOverflowInfos.size()) return;
        StringBuilder sb = new StringBuilder();
        appendPersonOverflowDetails(sb, personOverflowInfos.get(row));
        personsOverflowDetailsArea.setText(sb.toString());
        personsOverflowDetailsArea.setCaretPosition(0);
    }
    
    private void showPCRBucketDetails() {
        int row = pcrBucketsTable.getSelectedRow();
        if (row < 0 || row >= pcrBucketInfos.size()) return;
        StringBuilder sb = new StringBuilder();
        appendPCRBucketDetails(sb, pcrBucketInfos.get(row));
        pcrBucketsDetailsArea.setText(sb.toString());
        pcrBucketsDetailsArea.setCaretPosition(0);
    }
    
    private void showPCROverflowDetails() {
        int row = pcrOverflowTable.getSelectedRow();
        if (row < 0 || row >= pcrOverflowInfos.size()) return;
        StringBuilder sb = new StringBuilder();
        appendPCROverflowDetails(sb, pcrOverflowInfos.get(row));
        pcrOverflowDetailsArea.setText(sb.toString());
        pcrOverflowDetailsArea.setCaretPosition(0);
    }
    
    private void appendPersonBucketDetails(StringBuilder sb, BucketInfo<Person> info) {
        sb.append("BUCKET #").append(info.blockNumber).append("\n");
        sb.append("  Valid Count: ").append(info.validCount).append("/").append(info.capacity);
        sb.append(", Overflow Block Count: ").append(info.overflowBlockCount);
        sb.append(", Total: ").append(info.totalElementCount);
        sb.append(", First Overflow Block: ").append(info.firstOverflowBlock).append("\n");
        sb.append("  Records:\n");
        for (int i = 0; i < info.records.size(); i++) {
            Person p = info.records.get(i);
            sb.append("    [").append(i).append("] ID=").append(p.id);
            sb.append(", Name=").append(p.name);
            sb.append(", Surname=").append(p.surname);
            sb.append(", Birth=").append(p.birthdate > 0 ? DATE_FORMAT.format(new Date(p.birthdate)) : "N/A");
            sb.append(", Tests=").append(p.validTestsCount).append("\n");
            // Show all tests for this person
            for (int t = 0; t < p.validTestsCount; t++) {
                PCR test = p.pcrTests[t];
                sb.append("        Test[").append(t).append("]: Code=").append(test.testCode);
                sb.append(", Result=").append(test.testResult ? "Positive" : "Negative");
                sb.append(", Value=").append(String.format("%.2f", test.testValue));
                sb.append(", Date=").append(test.dateTime > 0 ? DATE_FORMAT.format(new Date(test.dateTime)) : "N/A");
                if (test.note != null && !test.note.isEmpty()) {
                    sb.append(", Note=").append(test.note);
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
    }
    
    private void appendPersonOverflowDetails(StringBuilder sb, OverflowInfo<Person> info) {
        sb.append("OVERFLOW BLOCK #").append(info.blockNumber).append("\n");
        sb.append("  Valid Count: ").append(info.validCount).append("/").append(info.capacity);
        sb.append(", Next Overflow Block: ").append(info.nextOverflowBlock).append("\n");
        sb.append("  Records:\n");
        for (int i = 0; i < info.records.size(); i++) {
            Person p = info.records.get(i);
            sb.append("    [").append(i).append("] ID=").append(p.id);
            sb.append(", Name=").append(p.name);
            sb.append(", Surname=").append(p.surname);
            sb.append(", Birth=").append(p.birthdate > 0 ? DATE_FORMAT.format(new Date(p.birthdate)) : "N/A");
            sb.append(", Tests=").append(p.validTestsCount).append("\n");
            // Show all tests for this person
            for (int t = 0; t < p.validTestsCount; t++) {
                PCR test = p.pcrTests[t];
                sb.append("        Test[").append(t).append("]: Code=").append(test.testCode);
                sb.append(", Result=").append(test.testResult ? "Positive" : "Negative");
                sb.append(", Value=").append(String.format("%.2f", test.testValue));
                sb.append(", Date=").append(test.dateTime > 0 ? DATE_FORMAT.format(new Date(test.dateTime)) : "N/A");
                if (test.note != null && !test.note.isEmpty()) {
                    sb.append(", Note=").append(test.note);
                }
                sb.append("\n");
            }
        }
        sb.append("\n");
    }
    
    private void appendPCRBucketDetails(StringBuilder sb, BucketInfo<PCRIndex> info) {
        sb.append("BUCKET #").append(info.blockNumber).append("\n");
        sb.append("  Valid Count: ").append(info.validCount).append("/").append(info.capacity);
        sb.append(", Overflow Block Count: ").append(info.overflowBlockCount);
        sb.append(", Total: ").append(info.totalElementCount);
        sb.append(", First Overflow Block: ").append(info.firstOverflowBlock).append("\n");
        sb.append("  Records:\n");
        for (int i = 0; i < info.records.size(); i++) {
            PCRIndex idx = info.records.get(i);
            sb.append("    [").append(i).append("] TestID=").append(idx.testId);
            sb.append(", PatientNo=").append(idx.patientNumber).append("\n");
        }
        sb.append("\n");
    }
    
    private void appendPCROverflowDetails(StringBuilder sb, OverflowInfo<PCRIndex> info) {
        sb.append("OVERFLOW BLOCK #").append(info.blockNumber).append("\n");
        sb.append("  Valid Count: ").append(info.validCount).append("/").append(info.capacity);
        sb.append(", Next Overflow Block: ").append(info.nextOverflowBlock).append("\n");
        sb.append("  Records:\n");
        for (int i = 0; i < info.records.size(); i++) {
            PCRIndex idx = info.records.get(i);
            sb.append("    [").append(i).append("] TestID=").append(idx.testId);
            sb.append(", PatientNo=").append(idx.patientNumber).append("\n");
        }
        sb.append("\n");
    }
    
    private void clearAllViews() {
        personBucketInfos.clear();
        personOverflowInfos.clear();
        pcrBucketInfos.clear();
        pcrOverflowInfos.clear();
        
        personsBucketsTableModel.setRowCount(0);
        personsOverflowTableModel.setRowCount(0);
        pcrBucketsTableModel.setRowCount(0);
        pcrOverflowTableModel.setRowCount(0);
        
        personsBucketsDetailsArea.setText("");
        personsOverflowDetailsArea.setText("");
        pcrBucketsDetailsArea.setText("");
        pcrOverflowDetailsArea.setText("");
    }
    
    // ==================== Folder and Status ====================
    
    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Database Folder");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            String result = controller.openDatabase(folder.getAbsolutePath());
            
            if ("NEEDS_BLOCK_SIZES".equals(result)) {
                // Show block size selection dialog for new database
                int[] blockSizes = showBlockSizeDialog();
                if (blockSizes != null) {
                    String error = controller.createNewDatabase(folder.getAbsolutePath(),
                            blockSizes[0], blockSizes[1], blockSizes[2], blockSizes[3]);
                    if (error != null) {
                        JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else if (result != null) {
                JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
            }
            
            updateStatus();
            rereadAllFiles();
        }
    }
    
    private int[] showBlockSizeDialog() {
        JDialog dialog = new JDialog(this, "New Database - Block Sizes", true);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel headerLabel = new JLabel("Configure block sizes for the new database:");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(headerLabel, gbc);
        gbc.gridwidth = 1;
        
        // Persons Main Block Size
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Persons Main Block Size:"), gbc);
        JSpinner spnPersonsMain = new JSpinner(new SpinnerNumberModel(
                UIController.DEFAULT_PERSONS_MAIN_BLOCK_SIZE, 128, 65536, 64));
        gbc.gridx = 1;
        mainPanel.add(spnPersonsMain, gbc);
        
        // Persons Overflow Block Size
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Persons Overflow Block Size:"), gbc);
        JSpinner spnPersonsOverflow = new JSpinner(new SpinnerNumberModel(
                UIController.DEFAULT_PERSONS_OVERFLOW_BLOCK_SIZE, 128, 65536, 64));
        gbc.gridx = 1;
        mainPanel.add(spnPersonsOverflow, gbc);
        
        // PCR Main Block Size
        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(new JLabel("Tests Main Block Size:"), gbc);
        JSpinner spnPCRMain = new JSpinner(new SpinnerNumberModel(
                UIController.DEFAULT_PCR_MAIN_BLOCK_SIZE, 128, 65536, 64));
        gbc.gridx = 1;
        mainPanel.add(spnPCRMain, gbc);
        
        // PCR Overflow Block Size
        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("Tests Overflow Block Size:"), gbc);
        JSpinner spnPCROverflow = new JSpinner(new SpinnerNumberModel(
                UIController.DEFAULT_PCR_OVERFLOW_BLOCK_SIZE, 128, 65536, 64));
        gbc.gridx = 1;
        mainPanel.add(spnPCROverflow, gbc);
        
        // Note about block sizes
        JLabel noteLabel = new JLabel("");
        noteLabel.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        mainPanel.add(noteLabel, gbc);
        
        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnCreate = new JButton("Create Database");
        JButton btnCancel = new JButton("Cancel");
        
        final int[][] result = {null};
        
        btnCreate.addActionListener(e -> {
            result[0] = new int[] {
                (int) spnPersonsMain.getValue(),
                (int) spnPersonsOverflow.getValue(),
                (int) spnPCRMain.getValue(),
                (int) spnPCROverflow.getValue()
            };
            dialog.dispose();
        });
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(btnCreate);
        btnPanel.add(btnCancel);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        return result[0];
    }
    
    private void updateStatus() {
        if (controller.isDatabaseOpen()) {
            lblStatus.setText("Database open: " + controller.getCurrentFolder());
            lblStatus.setForeground(new Color(0, 128, 0));
        } else {
            lblStatus.setText("No database open");
            lblStatus.setForeground(Color.RED);
        }
    }
    
    // ==================== Operations Execution ====================
    
    private void executeOperation() {
        if (!controller.isDatabaseOpen()) {
            JOptionPane.showMessageDialog(this, "No database open", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String error = null;
        
        try {
            if (rbInsertPerson.isSelected()) {
                error = executeInsertPerson();
            } else if (rbInsertPCR.isSelected()) {
                error = executeInsertPCR();
            } else if (rbSearchPerson.isSelected()) {
                executeSearchPerson();
                return;
            } else if (rbSearchPCR.isSelected()) {
                executeSearchPCR();
                return;
            } else if (rbEditPerson.isSelected()) {
                error = executeEditPerson();
            } else if (rbEditPCR.isSelected()) {
                error = executeEditPCR();
            } else if (rbDeletePerson.isSelected()) {
                error = executeDeletePerson();
            } else if (rbDeletePCR.isSelected()) {
                error = executeDeletePCR();
            }
        } catch (Exception e) {
            error = "Error: " + e.getMessage();
        }
        
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
        }
        rereadAllFiles();
    }
    
    private String executeInsertPerson() {
        JPanel panel = findInputPanel("personId");
        if (panel == null) return "Internal error";
        
        JTextField tfId = (JTextField) panel.getClientProperty("personId");
        JTextField tfName = (JTextField) panel.getClientProperty("name");
        JTextField tfSurname = (JTextField) panel.getClientProperty("surname");
        JTextField tfBirthdate = (JTextField) panel.getClientProperty("birthdate");
        
        UIController.PersonDTO dto = new UIController.PersonDTO();
        dto.id = tfId.getText().trim();
        dto.name = tfName.getText().trim();
        dto.surname = tfSurname.getText().trim();
        
        String bd = tfBirthdate.getText().trim();
        if (!bd.isEmpty()) {
            try {
                dto.birthdate = Long.parseLong(bd);
            } catch (NumberFormatException e) {
                return "Invalid birthdate format";
            }
        }
        
        if (dto.id.isEmpty()) return "Person ID is required";
        return controller.insertPerson(dto);
    }
    
    private String executeInsertPCR() {
        JPanel panel = findInputPanel("patientId");
        if (panel == null) return "Internal error";
        
        JTextField tfPatientId = (JTextField) panel.getClientProperty("patientId");
        JTextField tfTestCode = (JTextField) panel.getClientProperty("testCode");
        JTextField tfDateTime = (JTextField) panel.getClientProperty("dateTime");
        JCheckBox cbResult = (JCheckBox) panel.getClientProperty("testResult");
        JTextField tfValue = (JTextField) panel.getClientProperty("testValue");
        JTextField tfNote = (JTextField) panel.getClientProperty("note");
        
        UIController.PCRDTO dto = new UIController.PCRDTO();
        dto.patientNumber = tfPatientId.getText().trim();
        
        try {
            dto.testCode = Integer.parseInt(tfTestCode.getText().trim());
        } catch (NumberFormatException e) {
            return "Invalid test code";
        }
        
        String dt = tfDateTime.getText().trim();
        if (!dt.isEmpty()) {
            try {
                dto.dateTime = Long.parseLong(dt);
            } catch (NumberFormatException e) {
                return "Invalid datetime format";
            }
        } else {
            dto.dateTime = System.currentTimeMillis();
        }
        
        dto.testResult = cbResult.isSelected();
        
        String val = tfValue.getText().trim();
        if (!val.isEmpty()) {
            try {
                dto.testValue = Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return "Invalid test value";
            }
        }
        
        dto.note = tfNote.getText().trim();
        if (dto.patientNumber.isEmpty()) return "Patient ID is required";
        
        return controller.insertPCR(dto);
    }
    
    private void executeSearchPerson() {
        JPanel panel = findInputPanel("searchPersonId");
        if (panel == null) return;
        
        JTextField tf = (JTextField) panel.getClientProperty("searchPersonId");
        String id = tf.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Person ID is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        UIController.PersonDTO result = controller.searchPerson(id);
        if (result == null) {
            JOptionPane.showMessageDialog(this, "Person not found", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            showPersonDetails(result);
        }
    }
    
    private void executeSearchPCR() {
        JPanel panel = findInputPanel("searchTestCode");
        if (panel == null) return;
        
        JTextField tf = (JTextField) panel.getClientProperty("searchTestCode");
        try {
            int code = Integer.parseInt(tf.getText().trim());
            UIController.PersonDTO result = controller.searchPCR(code);
            if (result == null) {
                JOptionPane.showMessageDialog(this, "PCR test not found", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                showPersonDetails(result);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid test code", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showPersonDetails(UIController.PersonDTO p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Person Details:\n");
        sb.append("ID: ").append(p.id).append("\n");
        sb.append("Name: ").append(p.name).append("\n");
        sb.append("Surname: ").append(p.surname).append("\n");
        sb.append("Birthdate: ").append(p.birthdate > 0 ? DATE_FORMAT.format(new Date(p.birthdate)) : "N/A").append("\n");
        sb.append("\nTests (").append(p.tests.size()).append("):\n");
        
        for (UIController.PCRDTO test : p.tests) {
            sb.append("  - Code: ").append(test.testCode);
            sb.append(", Result: ").append(test.testResult ? "Positive" : "Negative");
            sb.append(", Value: ").append(test.testValue);
            sb.append(", Note: ").append(test.note).append("\n");
        }
        
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Search Result", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private String executeEditPerson() {
        JPanel panel = findInputPanel("editPersonId");
        if (panel == null) return "Internal error";
        
        JTextField tfId = (JTextField) panel.getClientProperty("editPersonId");
        JTextField tfName = (JTextField) panel.getClientProperty("editPersonName");
        JTextField tfSurname = (JTextField) panel.getClientProperty("editPersonSurname");
        JTextField tfBd = (JTextField) panel.getClientProperty("editPersonBirthdate");
        
        String id = tfId.getText().trim();
        if (id.isEmpty()) return "Person ID is required";
        
        String name = tfName.getText().trim();
        String surname = tfSurname.getText().trim();
        String bdStr = tfBd.getText().trim();
        
        Long birthdate = null;
        if (!bdStr.isEmpty()) {
            try {
                birthdate = Long.parseLong(bdStr);
            } catch (NumberFormatException e) {
                return "Invalid birthdate format";
            }
        }
        
        return controller.editPerson(id, name.isEmpty() ? null : name, 
                                     surname.isEmpty() ? null : surname, birthdate);
    }
    
    @SuppressWarnings("unchecked")
    private String executeEditPCR() {
        JPanel panel = findInputPanel("editPCRCode");
        if (panel == null) return "Internal error";
        
        JTextField tfCode = (JTextField) panel.getClientProperty("editPCRCode");
        JTextField tfDt = (JTextField) panel.getClientProperty("editPCRDateTime");
        JComboBox<String> cbResult = (JComboBox<String>) panel.getClientProperty("editPCRResult");
        JTextField tfValue = (JTextField) panel.getClientProperty("editPCRValue");
        JTextField tfNote = (JTextField) panel.getClientProperty("editPCRNote");
        
        int code;
        try {
            code = Integer.parseInt(tfCode.getText().trim());
        } catch (NumberFormatException e) {
            return "Invalid test code";
        }
        
        Long dateTime = null;
        String dtStr = tfDt.getText().trim();
        if (!dtStr.isEmpty()) {
            try {
                dateTime = Long.parseLong(dtStr);
            } catch (NumberFormatException e) {
                return "Invalid datetime format";
            }
        }
        
        Boolean result = null;
        int idx = cbResult.getSelectedIndex();
        if (idx == 1) result = true;
        else if (idx == 2) result = false;
        
        Double value = null;
        String valStr = tfValue.getText().trim();
        if (!valStr.isEmpty()) {
            try {
                value = Double.parseDouble(valStr);
            } catch (NumberFormatException e) {
                return "Invalid test value";
            }
        }
        
        String note = tfNote.getText().trim();
        return controller.editPCR(code, dateTime, result, value, note.isEmpty() ? null : note);
    }
    
    private String executeDeletePerson() {
        JPanel panel = findInputPanel("deletePersonId");
        if (panel == null) return "Internal error";
        
        JTextField tf = (JTextField) panel.getClientProperty("deletePersonId");
        String id = tf.getText().trim();
        if (id.isEmpty()) return "Person ID is required";
        return controller.deletePerson(id);
    }
    
    private String executeDeletePCR() {
        JPanel panel = findInputPanel("deletePCRCode");
        if (panel == null) return "Internal error";
        
        JTextField tf = (JTextField) panel.getClientProperty("deletePCRCode");
        try {
            int code = Integer.parseInt(tf.getText().trim());
            return controller.deletePCR(code);
        } catch (NumberFormatException e) {
            return "Invalid test code";
        }
    }
    
    private JPanel findInputPanel(String propertyName) {
        for (Component c : inputPanel.getComponents()) {
            if (c instanceof JPanel) {
                if (((JPanel)c).getClientProperty(propertyName) != null) {
                    return (JPanel) c;
                }
            }
        }
        return null;
    }
    
    // ==================== Random Fill and Bulk Generation ====================
    
    private void fillRandomValues() {
        if (rbInsertPerson.isSelected()) {
            fillRandomPerson();
        } else if (rbInsertPCR.isSelected()) {
            fillRandomPCR();
        } else if (rbSearchPerson.isSelected() || rbDeletePerson.isSelected()) {
            fillRandomPersonId();
        } else if (rbSearchPCR.isSelected() || rbDeletePCR.isSelected()) {
            fillRandomTestCode();
        } else if (rbEditPerson.isSelected()) {
            fillRandomEditPerson();
        } else if (rbEditPCR.isSelected()) {
            fillRandomEditPCR();
        }
    }
    
    private void fillRandomPerson() {
        JPanel panel = findInputPanel("personId");
        if (panel == null) return;
        
        JTextField tfId = (JTextField) panel.getClientProperty("personId");
        JTextField tfName = (JTextField) panel.getClientProperty("name");
        JTextField tfSurname = (JTextField) panel.getClientProperty("surname");
        JTextField tfBirthdate = (JTextField) panel.getClientProperty("birthdate");
        
        tfId.setText(String.format("%08d", generatedIdCounter++));
        tfName.setText(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        tfSurname.setText(LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        
        int year = 1950 + random.nextInt(55);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        tfBirthdate.setText(String.valueOf(cal.getTimeInMillis()));
    }
    
    private void fillRandomPCR() {
        JPanel panel = findInputPanel("patientId");
        if (panel == null) return;
        
        JTextField tfPatientId = (JTextField) panel.getClientProperty("patientId");
        JTextField tfTestCode = (JTextField) panel.getClientProperty("testCode");
        JTextField tfDateTime = (JTextField) panel.getClientProperty("dateTime");
        JCheckBox cbResult = (JCheckBox) panel.getClientProperty("testResult");
        JTextField tfValue = (JTextField) panel.getClientProperty("testValue");
        JTextField tfNote = (JTextField) panel.getClientProperty("note");
        
        // Try to get a real person ID from loaded data
        String patientId = String.format("%08d", 10000000 + random.nextInt(1000));
        if (!personBucketInfos.isEmpty()) {
            List<Person> allPersons = new ArrayList<>();
            for (BucketInfo<Person> info : personBucketInfos) {
                allPersons.addAll(info.records);
            }
            if (!allPersons.isEmpty()) {
                patientId = allPersons.get(random.nextInt(allPersons.size())).id;
            }
        }
        
        tfPatientId.setText(patientId);
        tfTestCode.setText(String.valueOf(generatedTestCodeCounter++));
        tfDateTime.setText(String.valueOf(System.currentTimeMillis() - random.nextInt(86400000 * 30)));
        cbResult.setSelected(random.nextBoolean());
        tfValue.setText(String.format("%.2f", random.nextDouble() * 100));
        tfNote.setText(NOTES[random.nextInt(NOTES.length)]);
    }
    
    private void fillRandomPersonId() {
        String propertyName = rbSearchPerson.isSelected() ? "searchPersonId" : "deletePersonId";
        JPanel panel = findInputPanel(propertyName);
        if (panel == null) return;
        
        JTextField tf = (JTextField) panel.getClientProperty(propertyName);
        
        // Try to get a real person ID
        String id = String.format("%08d", 10000000 + random.nextInt(1000));
        if (!personBucketInfos.isEmpty()) {
            List<Person> allPersons = new ArrayList<>();
            for (BucketInfo<Person> info : personBucketInfos) {
                allPersons.addAll(info.records);
            }
            if (!allPersons.isEmpty()) {
                id = allPersons.get(random.nextInt(allPersons.size())).id;
            }
        }
        tf.setText(id);
    }
    
    private void fillRandomTestCode() {
        String propertyName = rbSearchPCR.isSelected() ? "searchTestCode" : "deletePCRCode";
        JPanel panel = findInputPanel(propertyName);
        if (panel == null) return;
        
        JTextField tf = (JTextField) panel.getClientProperty(propertyName);
        
        // Try to get a real test code
        int testCode = 1000 + random.nextInt(1000);
        if (!pcrBucketInfos.isEmpty()) {
            List<PCRIndex> allIndices = new ArrayList<>();
            for (BucketInfo<PCRIndex> info : pcrBucketInfos) {
                allIndices.addAll(info.records);
            }
            if (!allIndices.isEmpty()) {
                testCode = allIndices.get(random.nextInt(allIndices.size())).testId;
            }
        }
        tf.setText(String.valueOf(testCode));
    }
    
    private void fillRandomEditPerson() {
        JPanel panel = findInputPanel("editPersonId");
        if (panel == null) return;
        
        JTextField tfId = (JTextField) panel.getClientProperty("editPersonId");
        JTextField tfName = (JTextField) panel.getClientProperty("editPersonName");
        JTextField tfSurname = (JTextField) panel.getClientProperty("editPersonSurname");
        JTextField tfBirthdate = (JTextField) panel.getClientProperty("editPersonBirthdate");
        
        // Get existing person ID
        String id = String.format("%08d", 10000000 + random.nextInt(1000));
        if (!personBucketInfos.isEmpty()) {
            List<Person> allPersons = new ArrayList<>();
            for (BucketInfo<Person> info : personBucketInfos) {
                allPersons.addAll(info.records);
            }
            if (!allPersons.isEmpty()) {
                id = allPersons.get(random.nextInt(allPersons.size())).id;
            }
        }
        
        tfId.setText(id);
        tfName.setText(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        tfSurname.setText(LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        
        int year = 1950 + random.nextInt(55);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        tfBirthdate.setText(String.valueOf(cal.getTimeInMillis()));
    }
    
    @SuppressWarnings("unchecked")
    private void fillRandomEditPCR() {
        JPanel panel = findInputPanel("editPCRCode");
        if (panel == null) return;
        
        JTextField tfCode = (JTextField) panel.getClientProperty("editPCRCode");
        JTextField tfDateTime = (JTextField) panel.getClientProperty("editPCRDateTime");
        JComboBox<String> cbResult = (JComboBox<String>) panel.getClientProperty("editPCRResult");
        JTextField tfValue = (JTextField) panel.getClientProperty("editPCRValue");
        JTextField tfNote = (JTextField) panel.getClientProperty("editPCRNote");
        
        // Get existing test code
        int testCode = 1000 + random.nextInt(1000);
        if (!pcrBucketInfos.isEmpty()) {
            List<PCRIndex> allIndices = new ArrayList<>();
            for (BucketInfo<PCRIndex> info : pcrBucketInfos) {
                allIndices.addAll(info.records);
            }
            if (!allIndices.isEmpty()) {
                testCode = allIndices.get(random.nextInt(allIndices.size())).testId;
            }
        }
        
        tfCode.setText(String.valueOf(testCode));
        tfDateTime.setText(String.valueOf(System.currentTimeMillis() - random.nextInt(86400000 * 30)));
        cbResult.setSelectedIndex(1 + random.nextInt(2)); // Positive or Negative
        tfValue.setText(String.format("%.2f", random.nextDouble() * 100));
        tfNote.setText(NOTES[random.nextInt(NOTES.length)]);
    }
    
    private void generateBulkData() {
        if (!controller.isDatabaseOpen()) {
            JOptionPane.showMessageDialog(this, "No database open", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int personCount = (int) spnPersonCount.getValue();
        int testsPerPerson = (int) spnTestsPerPerson.getValue();
        
        int personsCreated = 0;
        int testsCreated = 0;
        List<String> createdPersonIds = new ArrayList<>();
        
        // Generate persons
        for (int i = 0; i < personCount; i++) {
            UIController.PersonDTO person = new UIController.PersonDTO();
            person.id = String.format("%08d", generatedIdCounter++);
            person.name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            person.surname = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            
            int year = 1950 + random.nextInt(55);
            int month = 1 + random.nextInt(12);
            int day = 1 + random.nextInt(28);
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, day, 0, 0, 0);
            person.birthdate = cal.getTimeInMillis();
            
            String error = controller.insertPerson(person);
            if (error == null) {
                personsCreated++;
                createdPersonIds.add(person.id);
            }
        }
        
        // Generate tests for created persons (respecting max 6 per person)
        if (testsPerPerson > 0 && !createdPersonIds.isEmpty()) {
            for (String personId : createdPersonIds) {
                // Check how many tests this person already has
                UIController.PersonDTO existingPerson = controller.searchPerson(personId);
                int existingTests = existingPerson != null ? existingPerson.tests.size() : 0;
                int testsToAdd = Math.min(testsPerPerson, 6 - existingTests);
                
                for (int j = 0; j < testsToAdd; j++) {
                    UIController.PCRDTO test = new UIController.PCRDTO();
                    test.testCode = generatedTestCodeCounter++;
                    test.patientNumber = personId;
                    test.dateTime = System.currentTimeMillis() - random.nextInt(86400000 * 365);
                    test.testResult = random.nextBoolean();
                    test.testValue = random.nextDouble() * 100;
                    test.note = NOTES[random.nextInt(NOTES.length)];
                    
                    String error = controller.insertPCR(test);
                    if (error == null) {
                        testsCreated++;
                    }
                }
            }
        }
        
        rereadAllFiles();
        
        // Only show message if there were issues
        if (personsCreated < personCount) {
            JOptionPane.showMessageDialog(this, 
                "Generated " + personsCreated + "/" + personCount + " persons, " + testsCreated + " tests",
                "Generation Partial", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            new UI().setVisible(true);
        });
    }
}
