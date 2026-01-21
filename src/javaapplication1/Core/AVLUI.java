package javaapplication1.Core;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class AVLUI extends JFrame {
    
    private AVLUIController controller;
    
    // Operation radio buttons (21 operations)
    private JRadioButton rbInsertPerson, rbInsertTest, rbSearchTest, rbListPatientTests;
    private JRadioButton rbListDistrictPositiveTests, rbListDistrictAllTests;
    private JRadioButton rbListRegionPositiveTests, rbListRegionAllTests;
    private JRadioButton rbListAllPositiveTests, rbListAllTests;
    private JRadioButton rbListDistrictSick, rbListDistrictSickSorted;
    private JRadioButton rbListRegionSick, rbListAllSick;
    private JRadioButton rbListSickestPerDistrict, rbListDistrictsBySick, rbListRegionsBySick;
    private JRadioButton rbListWorkplaceTests, rbSearchTestByCode, rbDeleteTest, rbDeletePerson;
    private ButtonGroup operationGroup;
    
    // View selection
    private JRadioButton rbViewPersons, rbViewTests, rbViewQueryResults;
    private ButtonGroup viewGroup;
    
    // Input panel
    private JPanel inputPanel;
    private CardLayout inputCardLayout;
    
    // Content display - TABLE for data, TEXT AREA for query results
    private JTable contentTable;
    private DefaultTableModel tableModel;
    private JScrollPane tableScrollPane;
    private JTextArea queryResultsArea;
    private JScrollPane queryResultsScrollPane;
    private JPanel contentPanel;
    private CardLayout contentCardLayout;
    
    // Buttons
    private JButton btnExecute, btnReloadData, btnSelectFolder, btnCloseDatabase;
    private JButton btnGenerateData, btnClearQueryResults;
    
    // Generator fields
    private JSpinner spnPersonCount, spnTestsPerPerson;
    
    // Status
    private JLabel lblStatus;
    
    // Random generator
    private Random random = new Random();
    
    // Names for generation
    private static final String[] FIRST_NAMES = {
        "John", "Emma", "Michael", "Sophia", "William", "Olivia", "James", "Ava",
        "Robert", "Isabella", "David", "Mia", "Richard", "Charlotte", "Joseph", "Amelia",
        "Thomas", "Harper", "Charles", "Evelyn", "Daniel", "Abigail", "Matthew", "Emily",
        "Anthony", "Elizabeth", "Mark", "Sofia", "Donald", "Ella"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris",
        "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker"
    };
    
    private static final String[] NOTES = {
        "Routine test", "Follow-up", "Symptomatic", "Contact tracing", "Travel requirement",
        "Pre-surgery", "Workplace screening", "School requirement", "Urgent", ""
    };
    
    // Input fields - will be created for different operations
    private Map<String, JComponent> inputFields = new HashMap<>();
    
    public AVLUI() {
        controller = new AVLUIController();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Hrytsun AVL S1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Top panel: database management
        add(createTopPanel(), BorderLayout.NORTH);
        
        // Left panel: operations
        add(createLeftPanel(), BorderLayout.WEST);
        
        // Center: content display (TABLE)
        add(createCenterPanel(), BorderLayout.CENTER);
        
        // Handle window close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.closeDatabase();
            }
        });
        
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        updateStatus();
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Database Management"));
        
        btnSelectFolder = new JButton("Select Folder...");
        btnSelectFolder.addActionListener(e -> selectFolder());
        
        btnCloseDatabase = new JButton("Close Database");
        btnCloseDatabase.addActionListener(e -> {
            controller.closeDatabase();
            updateStatus();
            clearTable();
            queryResultsArea.setText("");
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
        panel.setPreferredSize(new Dimension(450, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Operations selection
        JPanel opsPanel = createOperationsPanel();
        
        // Input fields
        inputCardLayout = new CardLayout();
        inputPanel = new JPanel(inputCardLayout);
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Parameters"));
        
        createAllInputPanels();
        
        // Execute button
        JPanel executePanel = new JPanel();
        btnExecute = new JButton("Execute Operation");
        btnExecute.setFont(btnExecute.getFont().deriveFont(Font.BOLD, 14f));
        btnExecute.addActionListener(e -> executeOperation());
        executePanel.add(btnExecute);
        
        // Generator panel
        JPanel generatorPanel = createGeneratorPanel();
        
        // View selection
        JPanel viewPanel = createViewPanel();
        
        // Assemble
        JPanel topSection = new JPanel(new BorderLayout(5, 5));
        topSection.add(opsPanel, BorderLayout.CENTER);
        topSection.add(inputPanel, BorderLayout.SOUTH);
        
        JPanel bottomSection = new JPanel(new BorderLayout(5, 5));
        bottomSection.add(executePanel, BorderLayout.NORTH);
        bottomSection.add(generatorPanel, BorderLayout.CENTER);
        bottomSection.add(viewPanel, BorderLayout.SOUTH);
        
        panel.add(topSection, BorderLayout.CENTER);
        panel.add(bottomSection, BorderLayout.SOUTH);
        
        showInputCard();
        
        return panel;
    }
    
    private JPanel createOperationsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Operations (21 total)"));
        
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(430, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(48); // 3x faster scrolling
        
        operationGroup = new ButtonGroup();
        
        // Operations in the exact order from DatabaseCore.java
        rbInsertTest = createOpRadio("1. Vložiť výsledok PCR testu", panel);
        rbSearchTest = createOpRadio("2. Vyhľadať výsledok testu podľa kódu testu", panel);
        rbListPatientTests = createOpRadio("3. Vypísať všetky PCR testy pacienta", panel);
        rbListDistrictPositiveTests = createOpRadio("4. Vypísať pozitívne testy v okrese (časové obdobie)", panel);
        rbListDistrictAllTests = createOpRadio("5. Vypísať všetky testy v okrese (časové obdobie)", panel);
        rbListRegionPositiveTests = createOpRadio("6. Vypísať pozitívne testy v kraji (časové obdobie)", panel);
        rbListRegionAllTests = createOpRadio("7. Vypísať všetky testy v kraji (časové obdobie)", panel);
        rbListAllPositiveTests = createOpRadio("8. Vypísať všetky pozitívne testy (časové obdobie)", panel);
        rbListAllTests = createOpRadio("9. Vypísať všetky testy (časové obdobie)", panel);
        rbListDistrictSick = createOpRadio("10. Vypísať chorých v okrese (X dní)", panel);
        rbListDistrictSickSorted = createOpRadio("11. Vypísať chorých v okrese zoradených podľa hodnoty (X dní)", panel);
        rbListRegionSick = createOpRadio("12. Vypísať chorých v kraji (X dní)", panel);
        rbListAllSick = createOpRadio("13. Vypísať všetkých chorých (X dní)", panel);
        rbListSickestPerDistrict = createOpRadio("14. Vypísať najchorejšieho zo každého okresu (X dní)", panel);
        rbListDistrictsBySick = createOpRadio("15. Vypísať okresy zoradené podľa počtu chorých (X dní)", panel);
        rbListRegionsBySick = createOpRadio("16. Vypísať kraje zoradené podľa počtu chorých (X dní)", panel);
        rbListWorkplaceTests = createOpRadio("17. Vypísať testy na pracovisku (časové obdobie)", panel);
        rbSearchTestByCode = createOpRadio("18. Vyhľadať PCR test podľa kódu", panel);
        rbInsertPerson = createOpRadio("19. Vložiť osobu", panel);
        rbDeleteTest = createOpRadio("20. Zmazať výsledok PCR testu", panel);
        rbDeletePerson = createOpRadio("21. Zmazať osobu", panel);
        
        rbInsertTest.setSelected(true);
        
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    
    private JRadioButton createOpRadio(String text, JPanel panel) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(rb.getFont().deriveFont(11f));
        rb.addActionListener(e -> showInputCard());
        operationGroup.add(rb);
        panel.add(rb);
        return rb;
    }
    
    private void createAllInputPanels() {
        inputPanel.add(createInsertPersonPanel(), "INSERT_PERSON");
        inputPanel.add(createInsertTestPanel(), "INSERT_TEST");
        inputPanel.add(createSearchTestPanel(), "SEARCH_TEST");
        inputPanel.add(createListPatientTestsPanel(), "LIST_PATIENT_TESTS");
        inputPanel.add(createDistrictTimeRangePanel(), "DISTRICT_TIME_RANGE");
        inputPanel.add(createRegionTimeRangePanel(), "REGION_TIME_RANGE");
        inputPanel.add(createTimeRangePanel(), "TIME_RANGE");
        inputPanel.add(createDistrictSickPanel(), "DISTRICT_SICK");
        inputPanel.add(createRegionSickPanel(), "REGION_SICK");
        inputPanel.add(createAllSickPanel(), "ALL_SICK");
        inputPanel.add(createWorkplaceTimeRangePanel(), "WORKPLACE_TIME_RANGE");
        inputPanel.add(createDeleteTestPanel(), "DELETE_TEST");
        inputPanel.add(createDeletePersonPanel(), "DELETE_PERSON");
        inputPanel.add(createEmptyPanel(), "EMPTY");
    }
    
    private JPanel createInsertPersonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Patient ID:", createTextField("person_id", 15));
        addField(panel, gbc, 1, "First Name:", createTextField("first_name", 15));
        addField(panel, gbc, 2, "Last Name:", createTextField("last_name", 15));
        addField(panel, gbc, 3, "Date of Birth:", createDatePicker("dob"));
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillInsertPerson());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createInsertTestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Test Code:", createTextField("test_code", 15));
        addField(panel, gbc, 1, "Patient ID:", createTextField("test_patient_id", 15));
        addField(panel, gbc, 2, "Test Date & Time:", createDateTimePicker("test_timestamp"));
        addField(panel, gbc, 3, "Workplace Code:", createTextField("workplace_code", 15));
        addField(panel, gbc, 4, "District Code:", createTextField("district_code", 15));
        addField(panel, gbc, 5, "Region Code:", createTextField("region_code", 15));
        addField(panel, gbc, 6, "Test Result (true/false):", createTextField("test_result", 15));
        addField(panel, gbc, 7, "Test Value:", createTextField("test_value", 15));
        addField(panel, gbc, 8, "Note:", createTextField("note", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillInsertTest());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createSearchTestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Test Code:", createTextField("search_test_code", 15));
        addField(panel, gbc, 1, "Patient ID:", createTextField("search_patient_id", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillSearchTest());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createListPatientTestsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Patient ID:", createTextField("list_patient_id", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillListPatient());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createDistrictTimeRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "District Code:", createTextField("query_district_code", 15));
        addField(panel, gbc, 1, "Start Date & Time:", createDateTimePicker("district_start_time"));
        addField(panel, gbc, 2, "End Date & Time:", createDateTimePicker("district_end_time"));
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillDistrictTimeRange());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createRegionTimeRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Region Code:", createTextField("query_region_code", 15));
        addField(panel, gbc, 1, "Start Date & Time:", createDateTimePicker("region_start_time"));
        addField(panel, gbc, 2, "End Date & Time:", createDateTimePicker("region_end_time"));
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillRegionTimeRange());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createTimeRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Start Date & Time:", createDateTimePicker("global_start_time"));
        addField(panel, gbc, 1, "End Date & Time:", createDateTimePicker("global_end_time"));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillTimeRange());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createDistrictSickPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "District Code:", createTextField("sick_district_code", 15));
        addField(panel, gbc, 1, "As Of Date:", createDatePicker("sick_district_date"));
        addField(panel, gbc, 2, "Sickness Duration (days):", createTextField("sick_district_days", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillDistrictSick());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createRegionSickPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Region Code:", createTextField("sick_region_code", 15));
        addField(panel, gbc, 1, "As Of Date:", createDatePicker("sick_region_date"));
        addField(panel, gbc, 2, "Sickness Duration (days):", createTextField("sick_region_days", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillRegionSick());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createAllSickPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "As Of Date:", createDatePicker("sick_all_date"));
        addField(panel, gbc, 1, "Sickness Duration (days):", createTextField("sick_all_days", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillAllSick());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createWorkplaceTimeRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Workplace Code:", createTextField("query_workplace_code", 15));
        addField(panel, gbc, 1, "Start Date & Time:", createDateTimePicker("workplace_start_time"));
        addField(panel, gbc, 2, "End Date & Time:", createDateTimePicker("workplace_end_time"));
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillWorkplaceTimeRange());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createDeleteTestPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Test Code:", createTextField("delete_test_code", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillDeleteTest());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createDeletePersonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        addField(panel, gbc, 0, "Patient ID:", createTextField("delete_patient_id", 15));
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton btnAutoFill = new JButton("Auto-Fill Random");
        btnAutoFill.addActionListener(e -> autoFillDeletePerson());
        panel.add(btnAutoFill, gbc);
        
        return panel;
    }
    
    private JPanel createEmptyPanel() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("No parameters needed"));
        return panel;
    }
    
    private JTextField createTextField(String key, int cols) {
        JTextField field = new JTextField(cols);
        inputFields.put(key, field);
        return field;
    }
    
    private JPanel createDatePicker(String key) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        JTextField dateField = new JTextField(18);
        dateField.setEditable(false);
        inputFields.put(key, dateField);
        
        JButton btnPick = new JButton("📅");
        btnPick.setPreferredSize(new Dimension(40, 25));
        btnPick.addActionListener(e -> {
            Calendar selected = showDatePicker(dateField);
            if (selected != null) {
                dateField.setText(AVLUIController.DATE_FORMAT.format(selected.getTime()));
                dateField.putClientProperty("timestamp", selected.getTimeInMillis());
            }
        });
        
        panel.add(dateField);
        panel.add(btnPick);
        
        return panel;
    }
    
    private JPanel createDateTimePicker(String key) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        JTextField dateTimeField = new JTextField(18);
        dateTimeField.setEditable(false);
        inputFields.put(key, dateTimeField);
        
        JButton btnPick = new JButton("📅");
        btnPick.setPreferredSize(new Dimension(40, 25));
        btnPick.addActionListener(e -> {
            Calendar selected = showDateTimePicker(dateTimeField);
            if (selected != null) {
                dateTimeField.setText(AVLUIController.DATETIME_FORMAT.format(selected.getTime()));
                dateTimeField.putClientProperty("timestamp", selected.getTimeInMillis());
            }
        });
        
        panel.add(dateTimeField);
        panel.add(btnPick);
        
        return panel;
    }
    
    private Calendar showDatePicker(JTextField field) {
        // Get initial date from field or use today
        Date initialDate;
        Object timestamp = field.getClientProperty("timestamp");
        if (timestamp instanceof Long) {
            initialDate = new Date((Long) timestamp);
        } else {
            initialDate = new Date();
        }
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create date spinner with native look
        SpinnerDateModel dateModel = new SpinnerDateModel(initialDate, null, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        
        // Format as date only (no time)
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "dd MMMM yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(new Font("Dialog", Font.PLAIN, 14));
        
        panel.add(new JLabel("Select Date:"), BorderLayout.NORTH);
        panel.add(dateSpinner, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Select Date", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) dateSpinner.getValue());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        }
        return null;
    }
    
    private Calendar showDateTimePicker(JTextField field) {
        // Get initial date/time from field or use now
        Date initialDate;
        Object timestamp = field.getClientProperty("timestamp");
        if (timestamp instanceof Long) {
            initialDate = new Date((Long) timestamp);
        } else {
            initialDate = new Date();
        }
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create date/time spinner with native look
        SpinnerDateModel dateModel = new SpinnerDateModel(initialDate, null, null, Calendar.MINUTE);
        JSpinner dateTimeSpinner = new JSpinner(dateModel);
        
        // Format as date and time
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateTimeSpinner, "dd MMMM yyyy HH:mm");
        dateTimeSpinner.setEditor(dateEditor);
        dateTimeSpinner.setFont(new Font("Dialog", Font.PLAIN, 14));
        
        panel.add(new JLabel("Select Date & Time (use arrow keys or type to adjust):"), BorderLayout.NORTH);
        panel.add(dateTimeSpinner, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Select Date & Time", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) dateTimeSpinner.getValue());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        }
        return null;
    }
    
    private void addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }
    
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Bulk Data Generator"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Persons:"), gbc);
        
        gbc.gridx = 1;
        spnPersonCount = new JSpinner(new SpinnerNumberModel(50, 1, 10000, 10));
        panel.add(spnPersonCount, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Tests/Person:"), gbc);
        
        gbc.gridx = 1;
        spnTestsPerPerson = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
        panel.add(spnTestsPerPerson, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnGenerateData = new JButton("Generate Data");
        btnGenerateData.addActionListener(e -> generateBulkData());
        panel.add(btnGenerateData, gbc);
        
        return panel;
    }
    
    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("View Database Contents"));
        
        viewGroup = new ButtonGroup();
        rbViewPersons = new JRadioButton("All Persons", true);
        rbViewTests = new JRadioButton("All Tests");
        rbViewQueryResults = new JRadioButton("Query Results");
        
        rbViewPersons.addActionListener(e -> switchToView());
        rbViewTests.addActionListener(e -> switchToView());
        rbViewQueryResults.addActionListener(e -> switchToView());
        
        viewGroup.add(rbViewPersons);
        viewGroup.add(rbViewTests);
        viewGroup.add(rbViewQueryResults);
        
        panel.add(rbViewPersons);
        panel.add(rbViewTests);
        panel.add(rbViewQueryResults);
        
        btnReloadData = new JButton("Reload");
        btnReloadData.addActionListener(e -> reloadData());
        panel.add(btnReloadData);
        
        btnClearQueryResults = new JButton("Clear Query Results");
        btnClearQueryResults.addActionListener(e -> queryResultsArea.setText(""));
        panel.add(btnClearQueryResults);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Database Content / Operation Results"));
        
        // Create card layout to switch between table and text area
        contentCardLayout = new CardLayout();
        contentPanel = new JPanel(contentCardLayout);
        
        // Create table with sortable columns
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        contentTable = new JTable(tableModel);
        contentTable.setAutoCreateRowSorter(true); // Enable sorting by clicking column headers
        contentTable.setFont(new Font("Monospaced", Font.PLAIN, 11));
        contentTable.setCellSelectionEnabled(true); // Enable individual cell selection
        contentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contentTable.setRowHeight(22);
        
        tableScrollPane = new JScrollPane(contentTable);
        
        // Create query results text area
        queryResultsArea = new JTextArea();
        queryResultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        queryResultsArea.setEditable(false);
        queryResultsScrollPane = new JScrollPane(queryResultsArea);
        
        // Add Ctrl+F search functionality
        setupSearchFunctionality();
        
        // Add both to content panel
        contentPanel.add(tableScrollPane, "TABLE");
        contentPanel.add(queryResultsScrollPane, "QUERY_RESULTS");
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        // Show table by default
        contentCardLayout.show(contentPanel, "TABLE");
        
        return panel;
    }
    
    private void clearTable() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
    }
    
    private void switchToView() {
        if (rbViewQueryResults.isSelected()) {
            contentCardLayout.show(contentPanel, "QUERY_RESULTS");
        } else {
            contentCardLayout.show(contentPanel, "TABLE");
            reloadData();
        }
    }
    
    private void showInputCard() {
        String cardName = null;
        String firstFieldKey = null;
        
        if (rbInsertTest.isSelected()) {
            cardName = "INSERT_TEST";
            firstFieldKey = "test_code";
        } else if (rbSearchTest.isSelected() || rbSearchTestByCode.isSelected()) {
            cardName = "SEARCH_TEST";
            firstFieldKey = "search_test_code";
        } else if (rbListPatientTests.isSelected()) {
            cardName = "LIST_PATIENT_TESTS";
            firstFieldKey = "list_patient_id";
        } else if (rbListDistrictPositiveTests.isSelected() || rbListDistrictAllTests.isSelected()) {
            cardName = "DISTRICT_TIME_RANGE";
            firstFieldKey = "query_district_code";
        } else if (rbListRegionPositiveTests.isSelected() || rbListRegionAllTests.isSelected()) {
            cardName = "REGION_TIME_RANGE";
            firstFieldKey = "query_region_code";
        } else if (rbListAllPositiveTests.isSelected() || rbListAllTests.isSelected()) {
            cardName = "TIME_RANGE";
            firstFieldKey = "global_start_time";
        } else if (rbListDistrictSick.isSelected() || rbListDistrictSickSorted.isSelected()) {
            cardName = "DISTRICT_SICK";
            firstFieldKey = "sick_district_code";
        } else if (rbListRegionSick.isSelected()) {
            cardName = "REGION_SICK";
            firstFieldKey = "sick_region_code";
        } else if (rbListAllSick.isSelected() || rbListSickestPerDistrict.isSelected() || 
                   rbListDistrictsBySick.isSelected() || rbListRegionsBySick.isSelected()) {
            cardName = "ALL_SICK";
            firstFieldKey = "sick_all_date";
        } else if (rbListWorkplaceTests.isSelected()) {
            cardName = "WORKPLACE_TIME_RANGE";
            firstFieldKey = "query_workplace_code";
        } else if (rbInsertPerson.isSelected()) {
            cardName = "INSERT_PERSON";
            firstFieldKey = "person_id";
        } else if (rbDeleteTest.isSelected()) {
            cardName = "DELETE_TEST";
            firstFieldKey = "delete_test_code";
        } else if (rbDeletePerson.isSelected()) {
            cardName = "DELETE_PERSON";
            firstFieldKey = "delete_patient_id";
        } else {
            cardName = "EMPTY";
        }
        
        if (cardName != null) {
            inputCardLayout.show(inputPanel, cardName);
            
            // Focus on first field after card is shown
            if (firstFieldKey != null) {
                final String fieldKey = firstFieldKey;
                SwingUtilities.invokeLater(() -> {
                    JComponent field = inputFields.get(fieldKey);
                    if (field != null) {
                        field.requestFocusInWindow();
                    }
                });
            }
        }
    }
    
    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Database Folder");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String folderPath = chooser.getSelectedFile().getAbsolutePath();
            AVLUIController.FolderStatus status = controller.checkFolderStatus(folderPath);
            
            switch (status) {
                case EMPTY:
                    // Auto-create without confirmation
                    String error = controller.createNewDatabase(folderPath);
                    if (error != null) {
                        JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                    
                case COMPLETE:
                    String error2 = controller.openExistingDatabase(folderPath);
                    if (error2 != null) {
                        JOptionPane.showMessageDialog(this, error2, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                    
                case INVALID:
                    JOptionPane.showMessageDialog(this,
                        "Invalid folder. Expected 0 or 2 database files.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    break;
            }
            
            updateStatus();
            reloadData();
        }
    }
    
    private void updateStatus() {
        if (controller.isDatabaseOpen()) {
            lblStatus.setText("Database: " + controller.getCurrentFolder());
            lblStatus.setForeground(new Color(0, 128, 0));
        } else {
            lblStatus.setText("No database open");
            lblStatus.setForeground(Color.RED);
        }
    }
    
    // ==================== Auto-fill methods ====================
    
    private long randomDateBetween1980And2010() {
        Calendar cal = Calendar.getInstance();
        int year = 1980 + random.nextInt(31); // 1980-2010
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, random.nextInt(12));
        cal.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    
    private long randomDateTimeBetween2020And2024() {
        Calendar cal = Calendar.getInstance();
        int year = 2020 + random.nextInt(5); // 2020-2024
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, random.nextInt(12));
        cal.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));
        cal.set(Calendar.HOUR_OF_DAY, random.nextInt(24));
        cal.set(Calendar.MINUTE, random.nextInt(60));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    
    private void setDateField(String key, long timestamp) {
        JComponent comp = inputFields.get(key);
        if (comp instanceof JTextField) {
            JTextField field = (JTextField) comp;
            field.setText(AVLUIController.DATE_FORMAT.format(new Date(timestamp)));
            field.putClientProperty("timestamp", timestamp);
        }
    }
    
    private void setDateTimeField(String key, long timestamp) {
        JComponent comp = inputFields.get(key);
        if (comp instanceof JTextField) {
            JTextField field = (JTextField) comp;
            field.setText(AVLUIController.DATETIME_FORMAT.format(new Date(timestamp)));
            field.putClientProperty("timestamp", timestamp);
        }
    }
    
    private void autoFillInsertPerson() {
        setFieldText("person_id", "P" + String.format("%08d", 10000000 + random.nextInt(90000000)));
        setFieldText("first_name", FIRST_NAMES[random.nextInt(FIRST_NAMES.length)]);
        setFieldText("last_name", LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
        setDateField("dob", randomDateBetween1980And2010());
    }
    
    private void autoFillInsertTest() {
        setFieldText("test_code", String.valueOf(random.nextInt(1000000000)));
        
        // Try to get a random existing person ID
        String randomPersonId = getRandomExistingPersonId();
        if (randomPersonId != null) {
            setFieldText("test_patient_id", randomPersonId);
        } else {
            setFieldText("test_patient_id", "P" + String.format("%08d", 10000000 + random.nextInt(90000000)));
        }
        
        setDateTimeField("test_timestamp", randomDateTimeBetween2020And2024());
        setFieldText("workplace_code", String.valueOf(1 + random.nextInt(100)));
        setFieldText("district_code", String.valueOf(1 + random.nextInt(20)));
        setFieldText("region_code", String.valueOf(1 + random.nextInt(5)));
        setFieldText("test_result", String.valueOf(random.nextDouble() < 0.15));
        int testValue = random.nextDouble() < 0.15 ? 
            (random.nextInt(900) + 100) : random.nextInt(50);
        setFieldText("test_value", String.valueOf(testValue));
        setFieldText("note", NOTES[random.nextInt(NOTES.length)]);
    }
    
    private void autoFillSearchTest() {
        // Try to get a random existing test and its patient ID
        try {
            if (controller.isDatabaseOpen()) {
                java.util.List<PCRTest> tests = new ArrayList<>();
                controller.getDatabaseCore().getTestMasterTree().inorderTraversal(t -> {
                    tests.add(t);
                    return true;
                });
                if (!tests.isEmpty()) {
                    PCRTest randomTest = tests.get(random.nextInt(tests.size()));
                    setFieldText("search_test_code", String.valueOf(randomTest.testCode));
                    setFieldText("search_patient_id", randomTest.patientId);
                    return;
                }
            }
        } catch (Exception e) {
            // Fall back to random generation
        }
        
        // Fallback: generate random values
        setFieldText("search_test_code", String.valueOf(random.nextInt(1000000000)));
        setFieldText("search_patient_id", "P" + String.format("%08d", 10000000 + random.nextInt(90000000)));
    }
    
    private void autoFillListPatient() {
        String randomPersonId = getRandomExistingPersonId();
        if (randomPersonId != null) {
            setFieldText("list_patient_id", randomPersonId);
        } else {
            setFieldText("list_patient_id", "P" + String.format("%08d", 10000000 + random.nextInt(90000000)));
        }
    }
    
    private void autoFillDistrictTimeRange() {
        setFieldText("query_district_code", String.valueOf(1 + random.nextInt(20)));
        long start = randomDateTimeBetween2020And2024();
        long end = start + (30L * 24 * 60 * 60 * 1000); // 30 days later
        setDateTimeField("district_start_time", start);
        setDateTimeField("district_end_time", end);
    }
    
    private void autoFillRegionTimeRange() {
        setFieldText("query_region_code", String.valueOf(1 + random.nextInt(5)));
        long start = randomDateTimeBetween2020And2024();
        long end = start + (30L * 24 * 60 * 60 * 1000);
        setDateTimeField("region_start_time", start);
        setDateTimeField("region_end_time", end);
    }
    
    private void autoFillTimeRange() {
        long start = randomDateTimeBetween2020And2024();
        long end = start + (90L * 24 * 60 * 60 * 1000); // 90 days later
        setDateTimeField("global_start_time", start);
        setDateTimeField("global_end_time", end);
    }
    
    private void autoFillDistrictSick() {
        setFieldText("sick_district_code", String.valueOf(1 + random.nextInt(20)));
        setDateField("sick_district_date", System.currentTimeMillis());
        setFieldText("sick_district_days", String.valueOf(7 + random.nextInt(14))); // 7-20 days
    }
    
    private void autoFillRegionSick() {
        setFieldText("sick_region_code", String.valueOf(1 + random.nextInt(5)));
        setDateField("sick_region_date", System.currentTimeMillis());
        setFieldText("sick_region_days", String.valueOf(7 + random.nextInt(14)));
    }
    
    private void autoFillAllSick() {
        setDateField("sick_all_date", System.currentTimeMillis());
        setFieldText("sick_all_days", String.valueOf(7 + random.nextInt(14)));
    }
    
    private void autoFillWorkplaceTimeRange() {
        setFieldText("query_workplace_code", String.valueOf(1 + random.nextInt(100)));
        long start = randomDateTimeBetween2020And2024();
        long end = start + (30L * 24 * 60 * 60 * 1000);
        setDateTimeField("workplace_start_time", start);
        setDateTimeField("workplace_end_time", end);
    }
    
    private void autoFillDeleteTest() {
        String randomTestCode = getRandomExistingTestCode();
        if (randomTestCode != null) {
            setFieldText("delete_test_code", randomTestCode);
        } else {
            setFieldText("delete_test_code", String.valueOf(random.nextInt(1000000000)));
        }
    }
    
    private void autoFillDeletePerson() {
        String randomPersonId = getRandomExistingPersonId();
        if (randomPersonId != null) {
            setFieldText("delete_patient_id", randomPersonId);
        } else {
            setFieldText("delete_patient_id", "P" + String.format("%08d", 10000000 + random.nextInt(90000000)));
        }
    }
    
    private String getRandomExistingPersonId() {
        if (!controller.isDatabaseOpen()) return null;
        try {
            java.util.List<Person> persons = new ArrayList<>();
            controller.getDatabaseCore().getPersonMasterTree().inorderTraversal(p -> {
                persons.add(p);
                return true;
            });
            if (persons.isEmpty()) return null;
            return persons.get(random.nextInt(persons.size())).patientId;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getRandomExistingTestCode() {
        if (!controller.isDatabaseOpen()) return null;
        try {
            java.util.List<PCRTest> tests = new ArrayList<>();
            controller.getDatabaseCore().getTestMasterTree().inorderTraversal(t -> {
                tests.add(t);
                return true;
            });
            if (tests.isEmpty()) return null;
            return String.valueOf(tests.get(random.nextInt(tests.size())).testCode);
        } catch (Exception e) {
            return null;
        }
    }
    
    // ==================== Execute operations ====================
    
    private void executeOperation() {
        if (!controller.isDatabaseOpen()) {
            JOptionPane.showMessageDialog(this, "Please open a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String result = null;
            
            // Execute the appropriate operation
            if (rbInsertTest.isSelected()) {
                result = executeInsertTest();
            } else if (rbSearchTest.isSelected() || rbSearchTestByCode.isSelected()) {
                result = executeSearchTest();
            } else if (rbListPatientTests.isSelected()) {
                result = executeListPatientTests();
            } else if (rbListDistrictPositiveTests.isSelected()) {
                result = executeListDistrictPositiveTests();
            } else if (rbListDistrictAllTests.isSelected()) {
                result = executeListDistrictAllTests();
            } else if (rbListRegionPositiveTests.isSelected()) {
                result = executeListRegionPositiveTests();
            } else if (rbListRegionAllTests.isSelected()) {
                result = executeListRegionAllTests();
            } else if (rbListAllPositiveTests.isSelected()) {
                result = executeListAllPositiveTests();
            } else if (rbListAllTests.isSelected()) {
                result = executeListAllTests();
            } else if (rbListDistrictSick.isSelected()) {
                result = executeListDistrictSick();
            } else if (rbListDistrictSickSorted.isSelected()) {
                result = executeListDistrictSickSorted();
            } else if (rbListRegionSick.isSelected()) {
                result = executeListRegionSick();
            } else if (rbListAllSick.isSelected()) {
                result = executeListAllSick();
            } else if (rbListSickestPerDistrict.isSelected()) {
                result = executeListSickestPerDistrict();
            } else if (rbListDistrictsBySick.isSelected()) {
                result = executeListDistrictsBySick();
            } else if (rbListRegionsBySick.isSelected()) {
                result = executeListRegionsBySick();
            } else if (rbListWorkplaceTests.isSelected()) {
                result = executeListWorkplaceTests();
            } else if (rbInsertPerson.isSelected()) {
                result = executeInsertPerson();
            } else if (rbDeleteTest.isSelected()) {
                result = executeDeleteTest();
            } else if (rbDeletePerson.isSelected()) {
                result = executeDeletePerson();
            }
            
            // Handle result
            if (result == null) {
                // Success for insert/delete operations - reload current view
                reloadData();
            } else if (result.startsWith("Error:") || result.contains("Failed")) {
                // Error occurred
                JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // Query result - switch to query results view and prepend result
                rbViewQueryResults.setSelected(true);
                switchToView();
                String currentText = queryResultsArea.getText();
                String separator = currentText.isEmpty() ? "" : "\n" + "=".repeat(100) + "\n\n";
                queryResultsArea.setText(result + separator + currentText);
                queryResultsArea.setCaretPosition(0); // Scroll to top
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private String executeInsertPerson() {
        AVLUIController.PersonDTO dto = new AVLUIController.PersonDTO();
        dto.patientId = getFieldText("person_id");
        dto.firstName = getFieldText("first_name");
        dto.lastName = getFieldText("last_name");
        dto.dateOfBirth = getFieldTimestamp("dob");
        return controller.insertPerson(dto);
    }
    
    private String executeInsertTest() {
        AVLUIController.PCRTestDTO dto = new AVLUIController.PCRTestDTO();
        dto.testCode = Integer.parseInt(getFieldText("test_code"));
        dto.patientId = getFieldText("test_patient_id");
        dto.timestamp = getFieldTimestamp("test_timestamp");
        dto.workplaceCode = Integer.parseInt(getFieldText("workplace_code"));
        dto.districtCode = Integer.parseInt(getFieldText("district_code"));
        dto.regionCode = Integer.parseInt(getFieldText("region_code"));
        dto.testResult = Boolean.parseBoolean(getFieldText("test_result"));
        dto.testValue = Double.parseDouble(getFieldText("test_value"));
        dto.note = getFieldText("note");
        return controller.insertPCRTest(dto);
    }
    
    private String executeSearchTest() {
        int testCode = Integer.parseInt(getFieldText("search_test_code"));
        String patientId = getFieldText("search_patient_id");
        String inputParams = String.format("QUERY: Search Test by Code and Patient ID\nINPUT: Test Code = %d, Patient ID = %s\n%s\n", 
            testCode, patientId, "=".repeat(80));
        return inputParams + controller.searchTestByCode(testCode, patientId);
    }
    
    private String executeListPatientTests() {
        String patientId = getFieldText("list_patient_id");
        String inputParams = String.format("QUERY: List All Tests for Patient\nINPUT: Patient ID = %s\n%s\n", 
            patientId, "=".repeat(80));
        return inputParams + controller.listTestsForPatient(patientId);
    }
    
    private String executeListDistrictPositiveTests() {
        int districtCode = Integer.parseInt(getFieldText("query_district_code"));
        long startTime = getFieldTimestamp("district_start_time");
        long endTime = getFieldTimestamp("district_end_time");
        String inputParams = String.format("QUERY: List Positive Tests in District (Time Range)\nINPUT: District = %d, Start = %s, End = %s\n%s\n", 
            districtCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listPositiveTestsInDistrictTimeRange(districtCode, startTime, endTime);
    }
    
    private String executeListDistrictAllTests() {
        int districtCode = Integer.parseInt(getFieldText("query_district_code"));
        long startTime = getFieldTimestamp("district_start_time");
        long endTime = getFieldTimestamp("district_end_time");
        String inputParams = String.format("QUERY: List All Tests in District (Time Range)\nINPUT: District = %d, Start = %s, End = %s\n%s\n", 
            districtCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listAllTestsInDistrictTimeRange(districtCode, startTime, endTime);
    }
    
    private String executeListRegionPositiveTests() {
        int regionCode = Integer.parseInt(getFieldText("query_region_code"));
        long startTime = getFieldTimestamp("region_start_time");
        long endTime = getFieldTimestamp("region_end_time");
        String inputParams = String.format("QUERY: List Positive Tests in Region (Time Range)\nINPUT: Region = %d, Start = %s, End = %s\n%s\n", 
            regionCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listPositiveTestsInRegionTimeRange(regionCode, startTime, endTime);
    }
    
    private String executeListRegionAllTests() {
        int regionCode = Integer.parseInt(getFieldText("query_region_code"));
        long startTime = getFieldTimestamp("region_start_time");
        long endTime = getFieldTimestamp("region_end_time");
        String inputParams = String.format("QUERY: List All Tests in Region (Time Range)\nINPUT: Region = %d, Start = %s, End = %s\n%s\n", 
            regionCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listAllTestsInRegionTimeRange(regionCode, startTime, endTime);
    }
    
    private String executeListAllPositiveTests() {
        long startTime = getFieldTimestamp("global_start_time");
        long endTime = getFieldTimestamp("global_end_time");
        String inputParams = String.format("QUERY: List All Positive Tests (Time Range)\nINPUT: Start = %s, End = %s\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listAllPositiveTestsInTimeRange(startTime, endTime);
    }
    
    private String executeListAllTests() {
        long startTime = getFieldTimestamp("global_start_time");
        long endTime = getFieldTimestamp("global_end_time");
        String inputParams = String.format("QUERY: List All Tests (Time Range)\nINPUT: Start = %s, End = %s\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listAllTestsInTimeRange(startTime, endTime);
    }
    
    private String executeListDistrictSick() {
        int districtCode = Integer.parseInt(getFieldText("sick_district_code"));
        long asOfDate = getFieldTimestamp("sick_district_date");
        int days = Integer.parseInt(getFieldText("sick_district_days"));
        String inputParams = String.format("QUERY: List Sick Persons in District\nINPUT: District = %d, As of Date = %s, Sick Duration = %d days\n%s\n", 
            districtCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listSickPersonsInDistrict(districtCode, asOfDate, days);
    }
    
    private String executeListDistrictSickSorted() {
        int districtCode = Integer.parseInt(getFieldText("sick_district_code"));
        long asOfDate = getFieldTimestamp("sick_district_date");
        int days = Integer.parseInt(getFieldText("sick_district_days"));
        String inputParams = String.format("QUERY: List Sick Persons in District (Sorted by Test Value)\nINPUT: District = %d, As of Date = %s, Sick Duration = %d days\n%s\n", 
            districtCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listSickPersonsInDistrictSortedByValue(districtCode, asOfDate, days);
    }
    
    private String executeListRegionSick() {
        int regionCode = Integer.parseInt(getFieldText("sick_region_code"));
        long asOfDate = getFieldTimestamp("sick_region_date");
        int days = Integer.parseInt(getFieldText("sick_region_days"));
        String inputParams = String.format("QUERY: List Sick Persons in Region\nINPUT: Region = %d, As of Date = %s, Sick Duration = %d days\n%s\n", 
            regionCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listSickPersonsInRegion(regionCode, asOfDate, days);
    }
    
    private String executeListAllSick() {
        long asOfDate = getFieldTimestamp("sick_all_date");
        int days = Integer.parseInt(getFieldText("sick_all_days"));
        String inputParams = String.format("QUERY: List All Sick Persons\nINPUT: As of Date = %s, Sick Duration = %d days\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listAllSickPersons(asOfDate, days);
    }
    
    private String executeListSickestPerDistrict() {
        long asOfDate = getFieldTimestamp("sick_all_date");
        int days = Integer.parseInt(getFieldText("sick_all_days"));
        String inputParams = String.format("QUERY: List Sickest Person Per District\nINPUT: As of Date = %s, Sick Duration = %d days\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listSickestPersonPerDistrict(asOfDate, days);
    }
    
    private String executeListDistrictsBySick() {
        long asOfDate = getFieldTimestamp("sick_all_date");
        int days = Integer.parseInt(getFieldText("sick_all_days"));
        String inputParams = String.format("QUERY: List Districts Sorted by Sick Count\nINPUT: As of Date = %s, Sick Duration = %d days\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listDistrictsSortedBySickCount(asOfDate, days);
    }
    
    private String executeListRegionsBySick() {
        long asOfDate = getFieldTimestamp("sick_all_date");
        int days = Integer.parseInt(getFieldText("sick_all_days"));
        String inputParams = String.format("QUERY: List Regions Sorted by Sick Count\nINPUT: As of Date = %s, Sick Duration = %d days\n%s\n", 
            AVLUIController.DATETIME_FORMAT.format(new Date(asOfDate)),
            days,
            "=".repeat(80));
        return inputParams + controller.listRegionsSortedBySickCount(asOfDate, days);
    }
    
    private String executeListWorkplaceTests() {
        int workplaceCode = Integer.parseInt(getFieldText("query_workplace_code"));
        long startTime = getFieldTimestamp("workplace_start_time");
        long endTime = getFieldTimestamp("workplace_end_time");
        String inputParams = String.format("QUERY: List Tests at Workplace (Time Range)\nINPUT: Workplace = %d, Start = %s, End = %s\n%s\n", 
            workplaceCode, 
            AVLUIController.DATETIME_FORMAT.format(new Date(startTime)),
            AVLUIController.DATETIME_FORMAT.format(new Date(endTime)),
            "=".repeat(80));
        return inputParams + controller.listAllTestsAtWorkplaceInTimeRange(workplaceCode, startTime, endTime);
    }
    
    private String executeDeleteTest() {
        int testCode = Integer.parseInt(getFieldText("delete_test_code"));
        return controller.deletePCRTest(testCode);
    }
    
    private String executeDeletePerson() {
        String patientId = getFieldText("delete_patient_id");
        return controller.deletePerson(patientId);
    }
    
    private String getFieldText(String key) {
        JComponent comp = inputFields.get(key);
        if (comp instanceof JTextField) {
            return ((JTextField) comp).getText().trim();
        }
        return "";
    }
    
    private void setFieldText(String key, String value) {
        JComponent comp = inputFields.get(key);
        if (comp instanceof JTextField) {
            ((JTextField) comp).setText(value);
        }
    }
    
    private long getFieldTimestamp(String key) {
        JComponent comp = inputFields.get(key);
        if (comp instanceof JTextField) {
            JTextField field = (JTextField) comp;
            Object timestamp = field.getClientProperty("timestamp");
            if (timestamp instanceof Long) {
                return (Long) timestamp;
            }
        }
        return System.currentTimeMillis();
    }
    
    // ==================== Table display methods ====================
    
    private void displayAllPersons() {
        if (!controller.isDatabaseOpen()) {
            clearTable();
            return;
        }
        
        java.util.List<Person> persons = new ArrayList<>();
        controller.getDatabaseCore().getPersonMasterTree().inorderTraversal(p -> {
            persons.add(p);
            return true;
        });
        
        // Setup table columns
        tableModel.setColumnIdentifiers(new String[]{"Patient ID", "First Name", "Last Name", "Date of Birth"});
        tableModel.setRowCount(0);
        
        // Add rows
        for (Person p : persons) {
            AVLUIController.PersonDTO dto = new AVLUIController.PersonDTO(p);
            tableModel.addRow(new Object[]{
                dto.patientId,
                dto.firstName,
                dto.lastName,
                dto.getFormattedDateOfBirth()
            });
        }
    }
    
    private void displayAllTests() {
        if (!controller.isDatabaseOpen()) {
            clearTable();
            return;
        }
        
        java.util.List<PCRTest> tests = new ArrayList<>();
        controller.getDatabaseCore().getTestMasterTree().inorderTraversal(t -> {
            tests.add(t);
            return true;
        });
        
        // Setup table columns
        tableModel.setColumnIdentifiers(new String[]{"Test Code", "Patient ID", "Date & Time", 
            "Result", "Value", "District", "Region", "Workplace", "Note"});
        tableModel.setRowCount(0);
        
        // Add rows
        for (PCRTest t : tests) {
            AVLUIController.PCRTestDTO dto = new AVLUIController.PCRTestDTO(t);
            tableModel.addRow(new Object[]{
                dto.testCode,
                dto.patientId,
                dto.getFormattedTimestamp(),
                dto.getResultString(),
                String.format("%.2f", dto.testValue),
                dto.districtCode,
                dto.regionCode,
                dto.workplaceCode,
                dto.note
            });
        }
    }
    
    private void reloadData() {
        if (!controller.isDatabaseOpen()) {
            clearTable();
            queryResultsArea.setText("");
            return;
        }
        
        if (rbViewPersons.isSelected()) {
            displayAllPersons();
        } else if (rbViewTests.isSelected()) {
            displayAllTests();
        }
        // Query results are not reloaded, they stay as they are
    }
    
    private void generateBulkData() {
        if (!controller.isDatabaseOpen()) {
            JOptionPane.showMessageDialog(this, "Please open a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int personCount = (Integer) spnPersonCount.getValue();
        int testsPerPerson = (Integer) spnTestsPerPerson.getValue();
        
        try {
            for (int i = 0; i < personCount; i++) {
                // Generate person (born 1980-2010)
                AVLUIController.PersonDTO person = new AVLUIController.PersonDTO();
                person.patientId = "P" + String.format("%08d", 10000000 + i);
                person.firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                person.lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                person.dateOfBirth = randomDateBetween1980And2010();
                
                String error = controller.insertPerson(person);
                if (error == null) {
                    // Generate tests for this person (2020-2024)
                    for (int j = 0; j < testsPerPerson; j++) {
                        AVLUIController.PCRTestDTO test = new AVLUIController.PCRTestDTO();
                        test.testCode = random.nextInt(1000000000);
                        test.patientId = person.patientId;
                        test.timestamp = randomDateTimeBetween2020And2024();
                        test.workplaceCode = random.nextInt(100) + 1;
                        test.districtCode = random.nextInt(20) + 1;
                        test.regionCode = random.nextInt(5) + 1;
                        test.testResult = random.nextDouble() < 0.15; // 15% positive rate
                        test.testValue = test.testResult ? 
                            (random.nextInt(900) + 100) : // 100-999 if positive
                            random.nextInt(50); // 0-49 if negative
                        test.note = NOTES[random.nextInt(NOTES.length)];
                        
                        controller.insertPCRTest(test);
                    }
                }
            }
            
            // No popup, just refresh
            reloadData();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error during generation: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public DatabaseCore getDatabaseCore() {
        return controller.getDatabaseCore();
    }
    
    // ==================== Search Functionality ====================
    
    private void setupSearchFunctionality() {
        // Bind Ctrl+F to open search dialog
        KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
        queryResultsArea.getInputMap().put(ctrlF, "find");
        queryResultsArea.getActionMap().put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSearchDialog();
            }
        });
    }
    
    private void showSearchDialog() {
        JDialog searchDialog = new JDialog(this, "Find in Query Results", false);
        searchDialog.setLayout(new BorderLayout(10, 10));
        
        // Search panel
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Search field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        searchPanel.add(new JLabel("Find:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField searchField = new JTextField(20);
        searchPanel.add(searchField, gbc);
        
        // Case sensitive checkbox
        gbc.gridx = 1;
        gbc.gridy = 1;
        JCheckBox caseSensitive = new JCheckBox("Case sensitive");
        searchPanel.add(caseSensitive, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnFindNext = new JButton("Find Next");
        JButton btnFindPrevious = new JButton("Find Previous");
        JButton btnClose = new JButton("Close");
        
        buttonPanel.add(btnFindPrevious);
        buttonPanel.add(btnFindNext);
        buttonPanel.add(btnClose);
        
        searchDialog.add(searchPanel, BorderLayout.CENTER);
        searchDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Search action
        ActionListener searchAction = new ActionListener() {
            private int lastFoundIndex = -1;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchField.getText();
                if (searchText.isEmpty()) {
                    return;
                }
                
                String content = queryResultsArea.getText();
                if (!caseSensitive.isSelected()) {
                    content = content.toLowerCase();
                    searchText = searchText.toLowerCase();
                }
                
                int startIndex;
                if (e.getSource() == btnFindNext) {
                    startIndex = (lastFoundIndex == -1) ? 0 : lastFoundIndex + 1;
                    lastFoundIndex = content.indexOf(searchText, startIndex);
                    
                    // Wrap around if not found
                    if (lastFoundIndex == -1 && startIndex > 0) {
                        lastFoundIndex = content.indexOf(searchText, 0);
                    }
                } else { // Find Previous
                    startIndex = (lastFoundIndex == -1) ? content.length() : lastFoundIndex - 1;
                    lastFoundIndex = content.lastIndexOf(searchText, startIndex);
                    
                    // Wrap around if not found
                    if (lastFoundIndex == -1 && startIndex < content.length()) {
                        lastFoundIndex = content.lastIndexOf(searchText, content.length());
                    }
                }
                
                if (lastFoundIndex != -1) {
                    // Highlight and scroll to found text
                    queryResultsArea.setSelectionStart(lastFoundIndex);
                    queryResultsArea.setSelectionEnd(lastFoundIndex + searchText.length());
                    queryResultsArea.getCaret().setSelectionVisible(true);
                    
                    // Calculate and scroll to the found position
                    try {
                        Rectangle viewRect = queryResultsArea.modelToView2D(lastFoundIndex).getBounds();
                        queryResultsArea.scrollRectToVisible(viewRect);
                    } catch (Exception ex) {
                        // Ignore - older Java versions or other exceptions
                    }
                } else {
                    JOptionPane.showMessageDialog(searchDialog, 
                        "Text not found: " + searchField.getText(), 
                        "Not Found", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        
        btnFindNext.addActionListener(searchAction);
        btnFindPrevious.addActionListener(searchAction);
        btnClose.addActionListener(e -> searchDialog.dispose());
        
        // Enter key triggers Find Next
        searchField.addActionListener(searchAction);
        
        // Position and show dialog
        searchDialog.pack();
        searchDialog.setLocationRelativeTo(this);
        searchDialog.setVisible(true);
        searchField.requestFocus();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AVLUI ui = new AVLUI();
            ui.setVisible(true);
        });
    }
}
