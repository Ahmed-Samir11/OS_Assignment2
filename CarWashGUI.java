import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * CarWashGUI (View) updated to MVC: listens to CarWashModel and controls a CarWashController.
 */
public class CarWashGUI extends JFrame implements PropertyChangeListener {

    private final CarWashModel model;
    private final CarWashController controller;

    // View components
    private final DefaultListModel<String> queueModel = new DefaultListModel<>();
    private final JList<String> queueList = new JList<>(queueModel);

    private final DefaultListModel<String> logModel = new DefaultListModel<>();
    private final JList<String> logList = new JList<>(logModel);
    // keep all log entries (unfiltered) so we can apply UI filters
    private final java.util.List<String> allLogs = new java.util.ArrayList<>();

    private JProgressBar[] progressBars;
    private JLabel[] carLabels;

    private final Color COLOR_FREE = new Color(245, 245, 245);
    private final Color COLOR_OCCUPIED = new Color(220, 230, 255);
    private final Color COLOR_BACKGROUND = Color.WHITE;
    private final Color COLOR_BORDER = new Color(200, 200, 200);

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    public CarWashGUI(CarWashModel model, CarWashController controller, int queueCapacity) {
        super("Car Wash & Gas Station Simulation");
        this.model = model;
        this.controller = controller;

        model.addPropertyChangeListener(this);
    initUi(model.getNumPumps(), queueCapacity);
    // initialize animation targets to pumps count
    targetProgress = new int[progressBars.length];
    for (int i = 0; i < progressBars.length; i++) targetProgress[i] = 0;
    ensureAnimTimer();
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initUi(int numPumps, int queueCapacity) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(COLOR_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Pumps panel
        JPanel pumpsPanel = new JPanel(new GridLayout(1, numPumps, 10, 10));
        pumpsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Service Bays (Pumps)"));

        progressBars = new JProgressBar[numPumps];
        carLabels = new JLabel[numPumps];

        for (int i = 0; i < numPumps; i++) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(COLOR_FREE);
            p.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

            JLabel title = new JLabel("Bay " + (i + 1), SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 14));
            p.add(title, BorderLayout.NORTH);

            // vertical progress bar looks better when pump panels are tall
            progressBars[i] = new JProgressBar(SwingConstants.VERTICAL, 0, 100);
            progressBars[i].setValue(0);
            // we'll show percent on the label beneath the bar for better legibility
            progressBars[i].setStringPainted(false);
            // make bars wider so the icon + label have room and percent is readable in the label
            progressBars[i].setPreferredSize(new Dimension(60, 160));

            carLabels[i] = new JLabel("Free", SwingConstants.CENTER);
            carLabels[i].setOpaque(true);
            carLabels[i].setBackground(COLOR_FREE);
            carLabels[i].setVerticalTextPosition(SwingConstants.BOTTOM);
            carLabels[i].setHorizontalTextPosition(SwingConstants.CENTER);

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(Box.createVerticalGlue());
            center.add(progressBars[i]);
            center.add(Box.createVerticalStrut(8));
            center.add(carLabels[i]);

            p.add(center, BorderLayout.CENTER);
            pumpsPanel.add(p);
        }

        // Queue panel (JList)
        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Waiting Queue (Capacity: " + queueCapacity + ")"));
        queueList.setVisibleRowCount(4);
        queuePanel.add(new JScrollPane(queueList), BorderLayout.CENTER);

        // Log panel (JList)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Activity Log"));
        logList.setVisibleRowCount(8);
        logPanel.add(new JScrollPane(logList), BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton startBtn = new JButton("Start");
    startBtn.setMnemonic('S');
    JButton pauseBtn = new JButton("Pause");
    pauseBtn.setMnemonic('P');
    JButton resumeBtn = new JButton("Resume");
    resumeBtn.setMnemonic('R');
    JButton stopBtn = new JButton("Stop");
    stopBtn.setMnemonic('T');
        JSlider speed = new JSlider(100, 2000, 800);
        speed.setToolTipText("Simulation speed (ms)");
    JButton saveLog = new JButton("Save log");
    saveLog.setMnemonic('L');
    // log filter combo
    JComboBox<String> filterBox = new JComboBox<>(new String[]{"ALL", "INFO", "DEBUG"});
    filterBox.setToolTipText("Filter log level");

        startBtn.addActionListener(e -> controller.start());
        pauseBtn.addActionListener(e -> controller.pause());
        resumeBtn.addActionListener(e -> controller.resume());
        stopBtn.addActionListener(e -> controller.stop());
        speed.addChangeListener(e -> controller.setDelayMs(speed.getValue()));
    saveLog.addActionListener(e -> saveLogToFile());
    filterBox.addActionListener(e -> applyLogFilter((String) filterBox.getSelectedItem()));

        controls.add(startBtn);
        controls.add(pauseBtn);
        controls.add(resumeBtn);
        controls.add(stopBtn);
        controls.add(new JLabel("Speed:"));
        controls.add(speed);
    controls.add(saveLog);
    controls.add(new JLabel("Filter:"));
    controls.add(filterBox);

    // Assemble right-hand side (queue + log)
    JPanel right = new JPanel(new BorderLayout(10, 10));
    right.add(queuePanel, BorderLayout.NORTH);
    right.add(logPanel, BorderLayout.CENTER);
    right.setPreferredSize(new java.awt.Dimension(300, 400));

    // Make center resizable: left = pumps, right = queue+log
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pumpsPanel, right);
    split.setResizeWeight(0.7); // pumps get most space
    split.setOneTouchExpandable(true);
    split.setContinuousLayout(true);

    // Accessibility: set minimum/preferred sizes
    pumpsPanel.setMinimumSize(new Dimension(300, 200));
    pumpsPanel.setPreferredSize(new Dimension(600, 400));

    // Top controls and center split
    mainPanel.add(controls, BorderLayout.NORTH);
    mainPanel.add(split, BorderLayout.CENTER);

    // Accessibility: set names and descriptions
    queueList.getAccessibleContext().setAccessibleName("Waiting Queue");
    queueList.getAccessibleContext().setAccessibleDescription("List of cars waiting for service");
    logList.getAccessibleContext().setAccessibleName("Activity Log");
    logList.getAccessibleContext().setAccessibleDescription("Timestamped activity messages");

    // Add keyboard accelerators (global) for common actions
    InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = getRootPane().getActionMap();
    im.put(KeyStroke.getKeyStroke("control S"), "startAction");
    im.put(KeyStroke.getKeyStroke("control P"), "pauseAction");
    im.put(KeyStroke.getKeyStroke("control R"), "resumeAction");
    im.put(KeyStroke.getKeyStroke("control T"), "stopAction");
    am.put("startAction", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e){ controller.start(); } });
    am.put("pauseAction", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e){ controller.pause(); } });
    am.put("resumeAction", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e){ controller.resume(); } });
    am.put("stopAction", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e){ controller.stop(); } });
    }

    private void saveLogToFile() {
        JFileChooser chooser = new JFileChooser();
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter out = new java.io.PrintWriter(chooser.getSelectedFile())) {
                for (int i = 0; i < logModel.size(); i++) out.println(logModel.get(i));
                model.fireLog("Log saved to " + chooser.getSelectedFile().getName());
            } catch (Exception ex) {
                model.fireLog("Failed to save log: " + ex.getMessage());
            }
        }
    }

    private void appendLog(String msg) {
        String entry = LocalTime.now().format(timeFmt) + " - " + msg;
        synchronized (allLogs) {
            allLogs.add(entry);
        }
        SwingUtilities.invokeLater(() -> {
            // honor current filter (default ALL)
            logModel.addElement(entry);
            logList.ensureIndexIsVisible(logModel.size() - 1);
        });
    }

    private void applyLogFilter(String level) {
        SwingUtilities.invokeLater(() -> {
            logModel.clear();
            synchronized (allLogs) {
                for (String e : allLogs) {
                    if ("ALL".equals(level) || e.startsWith(level + ":") || e.contains(" " + level + ":")) {
                        logModel.addElement(e);
                    }
                }
            }
            if (!logModel.isEmpty()) logList.ensureIndexIsVisible(logModel.size() - 1);
        });
    }

    private void refreshQueue(List<String> snapshot) {
        SwingUtilities.invokeLater(() -> {
            queueModel.clear();
            for (String s : snapshot) queueModel.addElement(s);
        });
    }

    // animation support: target values per pump
    private int[] targetProgress;
    private Timer animTimer;

    // Simple colored car icon
    private static class CarIcon implements Icon {
        private final Color color;
        private final int w;
        private final int h;
        CarIcon(Color color) { this(color, 20, 12); }
        CarIcon(Color color, int w, int h) { this.color = color; this.w = w; this.h = h; }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, w, h, 6, 6);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y, w, h, 6, 6);
            g2.dispose();
        }
    }

    private void ensureAnimTimer() {
        if (animTimer != null) return;
        animTimer = new Timer(40, e -> {
            boolean any = false;
            for (int i = 0; i < progressBars.length; i++) {
                int cur = progressBars[i].getValue();
                int tgt = targetProgress[i];
                if (cur < tgt) {
                    progressBars[i].setValue(Math.min(tgt, cur + 2));
                    any = true;
                } else if (cur > tgt) {
                    progressBars[i].setValue(Math.max(tgt, cur - 2));
                    any = true;
                }
                // update dynamic color based on current (animated) progress
                progressBars[i].setForeground(colorForPercent(progressBars[i].getValue()));
            }
            if (!any) animTimer.stop();
        });
    }

    /**
     * Map percentage to a traffic-light color: green (>=70), yellow (30-69), red (<30).
     */
    private Color colorForPercent(int pct) {
        if (pct >= 70) return new Color(0, 153, 51); // green
        if (pct >= 30) return new Color(255, 153, 51); // orange
        return new Color(204, 51, 51); // red
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("queue".equals(name)) {
            @SuppressWarnings("unchecked")
            List<String> snapshot = (List<String>) evt.getNewValue();
            refreshQueue(snapshot);
        } else if (name != null && name.startsWith("pump")) {
            // pumpN
            int pumpId = Integer.parseInt(name.substring(4));
            CarWashModel.PumpState ps = (CarWashModel.PumpState) evt.getNewValue();
            SwingUtilities.invokeLater(() -> {
                // animate smoothly to the new progress target
                if (targetProgress == null || targetProgress.length != progressBars.length) {
                    targetProgress = new int[progressBars.length];
                }
                targetProgress[pumpId] = ps.progress.get();
                ensureAnimTimer();
                animTimer.start();

                if (ps.occupied) {
                    // show percent next to car name for readability
                        String display = ps.carName == null ? "Busy" : ps.carName;
                        // use ASCII hyphen to avoid source-encoding mojibake in some editors/environments
                        carLabels[pumpId].setText(display + " - " + ps.progress.get() + "%");
                    carLabels[pumpId].setBackground(COLOR_OCCUPIED);
                    carLabels[pumpId].setIcon(new CarIcon(Color.BLUE));
                    // color bar according to percent
                    progressBars[pumpId].setForeground(colorForPercent(ps.progress.get()));
                } else {
                    carLabels[pumpId].setText("Free");
                    carLabels[pumpId].setBackground(COLOR_FREE);
                    carLabels[pumpId].setIcon(null);
                    // reset progress instantly if released
                    progressBars[pumpId].setValue(0);
                    progressBars[pumpId].setForeground(Color.LIGHT_GRAY);
                }
                // tooltip with remaining time
                if (ps.remainingMs > 0) {
                    long s = (ps.remainingMs + 999) / 1000;
                    progressBars[pumpId].setToolTipText("Remaining: " + s + "s");
                    carLabels[pumpId].setToolTipText("Remaining: " + s + "s");
                } else {
                    progressBars[pumpId].setToolTipText(null);
                    carLabels[pumpId].setToolTipText(null);
                }
            });
        } else if ("log".equals(name)) {
            String msg = (String) evt.getNewValue();
            appendLog(msg);
        }
    }

    // Minimal harness: start GUI and controller
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CarWashModel model = new CarWashModel(3);
            CarWashController controller = new CarWashController(model);
            new CarWashGUI(model, controller, 5);
            // optionally auto-start
            controller.start();
        });
    }
}
