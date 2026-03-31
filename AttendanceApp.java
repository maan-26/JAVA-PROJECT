import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

public class AttendanceApp extends JFrame {
    private static final String DATA_FILE = "attendance_data.ser";
    private AppData data;

    // UI Components
    private JTextField startField, targetField, targetPctField, courseNameField;
    private JComboBox<String> removeCourseCombo, calcCourseCombo;
    private JPanel timetablePanel;
    private JTextField exceptionDateField;
    private JComboBox<String> exceptionTypeCombo;
    private DefaultListModel<String> exceptionsListModel;
    private JTextField attendedField, totalHeldField;
    private JTextPane resultPane;

    public AttendanceApp() {
        loadData();
        setTitle("Attendance Strategist");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Settings & Courses", createSettingsPanel());
        tabbedPane.addTab("Timetable", createTimetablePanel());
        tabbedPane.addTab("Exceptions", createExceptionsPanel());
        tabbedPane.addTab("Calculator", createCalculatorPanel());

        add(tabbedPane);
        refreshDataBindings();
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        topPanel.add(new JLabel("Semester Start (YYYY-MM-DD):"));
        startField = new JTextField(data.startDate.toString());
        topPanel.add(startField);

        topPanel.add(new JLabel("Target Date (YYYY-MM-DD):"));
        targetField = new JTextField(data.targetDate.toString());
        topPanel.add(targetField);

        topPanel.add(new JLabel("Target Attendance %:"));
        targetPctField = new JTextField(String.valueOf(data.targetPct));
        topPanel.add(targetPctField);

        JButton saveSettingsBtn = new JButton("Save Settings");
        saveSettingsBtn.addActionListener(e -> {
            try {
                data.startDate = LocalDate.parse(startField.getText());
                data.targetDate = LocalDate.parse(targetField.getText());
                data.targetPct = Double.parseDouble(targetPctField.getText());
                saveData();
                JOptionPane.showMessageDialog(this, "Settings Saved!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format or number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        topPanel.add(new JLabel(""));
        topPanel.add(saveSettingsBtn);

        JPanel bottomPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Manage Courses"));
        
        bottomPanel.add(new JLabel("Add Course:"));
        courseNameField = new JTextField();
        bottomPanel.add(courseNameField);
        JButton addCourseBtn = new JButton("Add");
        addCourseBtn.addActionListener(e -> {
            String c = courseNameField.getText().trim();
            if (!c.isEmpty() && !data.courses.contains(c)) {
                data.courses.add(c);
                courseNameField.setText("");
                saveData();
                refreshDataBindings();
            }
        });
        bottomPanel.add(addCourseBtn);

        bottomPanel.add(new JLabel("Remove Course:"));
        removeCourseCombo = new JComboBox<>();
        bottomPanel.add(removeCourseCombo);
        JButton removeCourseBtn = new JButton("Remove");
        removeCourseBtn.addActionListener(e -> {
            String c = (String) removeCourseCombo.getSelectedItem();
            if (c != null) {
                data.courses.remove(c);
                for (List<String> list : data.timetable.values()) list.removeIf(val -> val.equals(c));
                saveData();
                refreshDataBindings();
            }
        });
        bottomPanel.add(removeCourseBtn);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTimetablePanel() {
        timetablePanel = new JPanel(new GridLayout(1, 5, 5, 5));
        timetablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return timetablePanel;
    }

    private void refreshTimetableUI() {
        timetablePanel.removeAll();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (String day : days) {
            JPanel dayPanel = new JPanel(new BorderLayout());
            dayPanel.setBorder(BorderFactory.createTitledBorder(day));

            DefaultListModel<String> model = new DefaultListModel<>();
            data.timetable.get(day).forEach(model::addElement);
            JList<String> list = new JList<>(model);
            dayPanel.add(new JScrollPane(list), BorderLayout.CENTER);

            JPanel controlPanel = new JPanel(new BorderLayout());
            JComboBox<String> addCombo = new JComboBox<>(data.courses.toArray(new String[0]));
            JButton addBtn = new JButton("+");
            addBtn.addActionListener(e -> {
                if (addCombo.getSelectedItem() != null) {
                    data.timetable.get(day).add((String) addCombo.getSelectedItem());
                    saveData();
                    refreshTimetableUI();
                }
            });
            JButton remBtn = new JButton("-");
            remBtn.addActionListener(e -> {
                int idx = list.getSelectedIndex();
                if (idx != -1) {
                    data.timetable.get(day).remove(idx);
                    saveData();
                    refreshTimetableUI();
                }
            });
            
            JPanel btnPanel = new JPanel(new GridLayout(1, 2));
            btnPanel.add(addBtn);
            btnPanel.add(remBtn);

            controlPanel.add(addCombo, BorderLayout.CENTER);
            controlPanel.add(btnPanel, BorderLayout.EAST);
            dayPanel.add(controlPanel, BorderLayout.SOUTH);

            timetablePanel.add(dayPanel);
        }
        timetablePanel.revalidate();
        timetablePanel.repaint();
    }

    private JPanel createExceptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        exceptionDateField = new JTextField(10);
        inputPanel.add(exceptionDateField);
        
        inputPanel.add(new JLabel("Type:"));
        exceptionTypeCombo = new JComboBox<>(new String[]{"Holiday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"});
        inputPanel.add(exceptionTypeCombo);

        JButton addExBtn = new JButton("Set Exception");
        addExBtn.addActionListener(e -> {
            try {
                LocalDate d = LocalDate.parse(exceptionDateField.getText());
                data.exceptions.put(d, (String) exceptionTypeCombo.getSelectedItem());
                saveData();
                refreshExceptionsUI();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Date.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        inputPanel.add(addExBtn);

        exceptionsListModel = new DefaultListModel<>();
        JList<String> exceptionsList = new JList<>(exceptionsListModel);
        
        JButton remExBtn = new JButton("Remove Selected");
        remExBtn.addActionListener(e -> {
            String sel = exceptionsList.getSelectedValue();
            if (sel != null) {
                LocalDate d = LocalDate.parse(sel.split(" : ")[0]);
                data.exceptions.remove(d);
                saveData();
                refreshExceptionsUI();
            }
        });

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(exceptionsList), BorderLayout.CENTER);
        panel.add(remExBtn, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshExceptionsUI() {
        exceptionsListModel.clear();
        data.exceptions.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> exceptionsListModel.addElement(e.getKey() + " : " + e.getValue()));
    }

    private JPanel createCalculatorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Select Course:"));
        calcCourseCombo = new JComboBox<>();
        inputPanel.add(calcCourseCombo);

        inputPanel.add(new JLabel("Classes Attended:"));
        attendedField = new JTextField("0");
        inputPanel.add(attendedField);

        inputPanel.add(new JLabel("Total Classes Held So Far:"));
        totalHeldField = new JTextField("0");
        inputPanel.add(totalHeldField);

        JButton calcBtn = new JButton("Calculate Prediction");
        calcBtn.addActionListener(e -> calculate());

        resultPane = new JTextPane();
        resultPane.setContentType("text/html");
        resultPane.setEditable(false);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(calcBtn, BorderLayout.CENTER);
        panel.add(new JScrollPane(resultPane), BorderLayout.SOUTH);
        
        // Adjust layout constraints
        panel.add(inputPanel, BorderLayout.NORTH);
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(calcBtn, BorderLayout.NORTH);
        centerWrapper.add(new JScrollPane(resultPane), BorderLayout.CENTER);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private void refreshDataBindings() {
        removeCourseCombo.setModel(new DefaultComboBoxModel<>(data.courses.toArray(new String[0])));
        calcCourseCombo.setModel(new DefaultComboBoxModel<>(data.courses.toArray(new String[0])));
        refreshTimetableUI();
        refreshExceptionsUI();
    }

    private void calculate() {
        String course = (String) calcCourseCombo.getSelectedItem();
        if (course == null) return;

        try {
            int attended = Integer.parseInt(attendedField.getText());
            int totalHeld = Integer.parseInt(totalHeldField.getText());

            int futureSlots = 0;
            LocalDate iterator = LocalDate.now().plusDays(1);
            
            while (!iterator.isAfter(data.targetDate)) {
                String dayName = iterator.getDayOfWeek().toString();
                dayName = dayName.charAt(0) + dayName.substring(1).toLowerCase();
                String exception = data.exceptions.get(iterator);

                String scheduleDay = null;
                if ("Sunday".equals(dayName)) scheduleDay = null;
                else if ("Holiday".equals(exception)) scheduleDay = null;
                else if ("Saturday".equals(dayName)) {
                    if (exception != null && !"Holiday".equals(exception)) scheduleDay = exception;
                } else {
                    if ("Holiday".equals(exception)) scheduleDay = null;
                    else scheduleDay = dayName;
                }

                if (scheduleDay != null && data.timetable.containsKey(scheduleDay)) {
                    for (String c : data.timetable.get(scheduleDay)) {
                        if (c.equals(course)) futureSlots++;
                    }
                }
                iterator = iterator.plusDays(1);
            }

            int projectedTotal = totalHeld + futureSlots;
            double targetPctDecimal = data.targetPct / 100.0;
            int needed = (int) Math.ceil((targetPctDecimal * projectedTotal) - attended);
            needed = Math.max(0, needed);

            double currentPct = totalHeld == 0 ? 0 : ((double) attended / totalHeld) * 100;
            double maxPossiblePct = projectedTotal == 0 ? 0 : ((double) (attended + futureSlots) / projectedTotal) * 100;

            StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif; padding:10px;'>");
            html.append("<h3>Results for ").append(course).append("</h3>");
            html.append("<b>Current Attendance:</b> ").append(String.format("%.2f%%", currentPct)).append("<br>");
            html.append("<b>Classes Remaining:</b> ").append(futureSlots).append("<br>");
            html.append("<b>Projected Total:</b> ").append(projectedTotal).append("<br><hr>");

            if (maxPossiblePct < data.targetPct) {
                html.append("<h2 style='color:red;'>⚠️ IMPOSSIBLE TO REACH ").append(data.targetPct).append("%</h2>");
                html.append("Even if you attend ALL remaining classes, you will reach only <b>").append(String.format("%.2f%%", maxPossiblePct)).append("</b>.");
            } else if (needed > 0) {
                html.append("<h2 style='color:orange;'>⚠️ Action Required</h2>");
                html.append("You must attend at least <b>").append(needed).append("</b> more classes.<br>");
                html.append("You can afford to miss <b>").append(futureSlots - needed).append("</b> classes.");
            } else {
                html.append("<h2 style='color:green;'>✅ You are Safe</h2>");
                html.append("You have met the requirement for the projected total.<br>");
                html.append("You can skip ALL <b>").append(futureSlots).append("</b> remaining classes.");
            }
            html.append("</body></html>");
            resultPane.setText(html.toString());

        } catch (NumberFormatException ex) {
            resultPane.setText("<html><b style='color:red;'>Error: Please enter valid numbers for attendance.</b></html>");
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            data = (AppData) ois.readObject();
        } catch (Exception e) {
            data = new AppData();
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceApp().setVisible(true));
    }
}

class AppData implements Serializable {
    private static final long serialVersionUID = 1L;
    LocalDate startDate = LocalDate.now();
    LocalDate targetDate = LocalDate.now().plusDays(90);
    double targetPct = 75.0;
    List<String> courses = new ArrayList<>();
    Map<String, List<String>> timetable = new HashMap<>();
    Map<LocalDate, String> exceptions = new HashMap<>();

    public AppData() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (String day : days) timetable.put(day, new ArrayList<>());
    }
}