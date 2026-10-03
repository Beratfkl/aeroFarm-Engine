import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * HandbookDialog
 * Interactive reference manual and API guide for in-game drone programming.
 * Uses custom dark-themed tab buttons and CardLayout for crisp readability.
 */
public class HandbookDialog extends JDialog {

    private final FloatingCodeWindow codeWindow;
    private final CardLayout cardLayout;
    private final JPanel contentCards;
    private JButton btnActions;
    private JButton btnSensors;
    private JButton btnExamples;

    public HandbookDialog(JFrame parent, FloatingCodeWindow codeWindow) {
        super(parent, "aeroFarm Engine - API & Drone Handbook", false);
        this.codeWindow = codeWindow;

        setSize(700, 540);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(24, 26, 30));
        setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(32, 35, 42));
        headerPanel.setBorder(new EmptyBorder(14, 18, 10, 18));

        JLabel titleLabel = new JLabel("aeroFarm Engine - API & Drone Handbook");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        titleLabel.setForeground(new Color(253, 224, 71));

        JLabel subtitleLabel = new JLabel("Complete reference guide for built-in actions, sensors, and code patterns");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(156, 163, 175));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 3));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);
        headerPanel.add(titleBox, BorderLayout.NORTH);

        // Custom tab button bar (avoids OS Look-and-Feel styling issues)
        JPanel tabButtonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        tabButtonBar.setOpaque(false);

        btnActions = createTabButton("Actions & Moves", true);
        btnSensors = createTabButton("Sensors & State", false);
        btnExamples = createTabButton("Logic & Examples", false);

        btnActions.addActionListener(e -> selectTab("ACTIONS", btnActions));
        btnSensors.addActionListener(e -> selectTab("SENSORS", btnSensors));
        btnExamples.addActionListener(e -> selectTab("EXAMPLES", btnExamples));

        tabButtonBar.add(btnActions);
        tabButtonBar.add(btnSensors);
        tabButtonBar.add(btnExamples);
        headerPanel.add(tabButtonBar, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // Card layout container for tab contents
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setBackground(new Color(24, 26, 30));

        contentCards.add(createActionsPanel(), "ACTIONS");
        contentCards.add(createSensorsPanel(), "SENSORS");
        contentCards.add(createExamplesPanel(), "EXAMPLES");

        add(contentCards, BorderLayout.CENTER);

        // Bottom footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(32, 35, 42));
        footer.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel hintLabel = new JLabel("Tip: Click [Insert] to append code into your editor directly.");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hintLabel.setForeground(new Color(134, 239, 172));
        footer.add(hintLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Consolas", Font.BOLD, 12));
        closeBtn.setBackground(new Color(55, 60, 72));
        closeBtn.setForeground(new Color(241, 245, 249));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> setVisible(false));
        footer.add(closeBtn, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);
    }

    private void selectTab(String cardName, JButton activeBtn) {
        cardLayout.show(contentCards, cardName);
        updateTabButtonState(btnActions, activeBtn == btnActions);
        updateTabButtonState(btnSensors, activeBtn == btnSensors);
        updateTabButtonState(btnExamples, activeBtn == btnExamples);
    }

    private void updateTabButtonState(JButton btn, boolean isActive) {
        btn.putClientProperty("isActive", isActive);
        btn.repaint();
    }

    private JButton createTabButton(String text, boolean isActive) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = Boolean.TRUE.equals(getClientProperty("isActive"));
                Color bg;
                Color border;
                Color fg;

                if (active) {
                    bg = new Color(72, 80, 98);
                    border = new Color(130, 142, 170);
                    fg = Color.WHITE;
                } else if (getModel().isRollover()) {
                    bg = new Color(48, 54, 66);
                    border = new Color(90, 98, 115);
                    fg = new Color(230, 235, 245);
                } else {
                    bg = new Color(36, 40, 48);
                    border = new Color(60, 66, 78);
                    fg = new Color(175, 180, 195);
                }

                g2d.setColor(bg);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(border);
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2d.setColor(fg);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };

        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(150, 30));
        btn.putClientProperty("isActive", isActive);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JScrollPane createActionsPanel() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(new Color(24, 26, 30));
        list.setBorder(new EmptyBorder(12, 12, 12, 12));

        list.add(createApiCard("harvest()", "boolean", "Harvests mature crop or grass on the current tile.", "if (canHarvest()) {\n    harvest();\n}", "harvest();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("plant(String crop)", "boolean", "Sows specified crop on tilled soil (\"WHEAT\", \"CARROT\", \"GRASS\").", "if (isTilled() && getCrop().equals(\"NONE\")) {\n    plant(\"WHEAT\");\n}", "plant(\"WHEAT\");"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("water()", "void", "Waters the current tile. Hydrated soil grows crops significantly faster.", "if (!isWatered()) {\n    water();\n}", "water();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("till()", "void", "Tills the soil on the current tile, preparing it for planting.", "if (!isTilled()) {\n    till();\n}", "till();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("move() / moveForward()", "boolean", "Moves the drone forward by 1 tile. Returns false if blocked by grid boundary.", "if (!move()) {\n    turnRight();\n}", "move();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("turnRight() / turnLeft()", "void", "Rotates the drone orientation by 90 degrees clockwise or counter-clockwise.", "turnRight();\nturnLeft();", "turnRight();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("doAFlip()", "void", "Executes an acrobatic celebratory 360-degree aerial flip!", "doAFlip();", "doAFlip();"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("sleep(int ms)", "void", "Pauses drone execution for specified milliseconds.", "sleep(200);", "sleep(200);"));

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JScrollPane createSensorsPanel() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(new Color(24, 26, 30));
        list.setBorder(new EmptyBorder(12, 12, 12, 12));

        list.add(createApiCard("canHarvest()", "boolean", "Returns true if crop or grass on the current tile is fully grown and ready to harvest.", "if (canHarvest()) {\n    harvest();\n}", "canHarvest()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("isTilled()", "boolean", "Returns true if the soil on the current tile has been tilled.", "if (!isTilled()) {\n    till();\n}", "isTilled()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("isWatered()", "boolean", "Returns true if the current tile has active water moisture.", "if (!isWatered()) {\n    water();\n}", "isWatered()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("getCrop()", "String", "Returns the current crop name (\"WHEAT\", \"CARROT\", \"GRASS\", or \"NONE\").", "if (getCrop().equals(\"NONE\")) {\n    plant(\"WHEAT\");\n}", "getCrop()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("getPosX() / getPosY()", "int", "Returns the drone's current zero-based X or Y coordinate on the island.", "int x = getPosX();\nint y = getPosY();", "getPosX()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("getWorldSize()", "int", "Returns the dimension of the island (1 for 1x1, 3 for 3x3, 4 for 4x4).", "int size = getWorldSize();", "getWorldSize()"));
        list.add(Box.createVerticalStrut(8));
        list.add(createApiCard("System.out.println(val)", "void", "Prints debug messages to the editor status bar.", "System.out.println(\"Harvested crop!\");", "System.out.println(\"Hello aeroFarm\");"));

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JScrollPane createExamplesPanel() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(new Color(24, 26, 30));
        list.setBorder(new EmptyBorder(12, 12, 12, 12));

        list.add(createPatternCard(
                "Full Autonomous Farming Loop",
                "Tills ground, plants wheat, waters the soil, and harvests when ripe in an infinite cycle.",
                "while (true) {\n" +
                "    if (!isTilled()) {\n" +
                "        till();\n" +
                "    }\n" +
                "    if (getCrop().equals(\"NONE\")) {\n" +
                "        plant(\"WHEAT\");\n" +
                "    }\n" +
                "    if (!isWatered()) {\n" +
                "        water();\n" +
                "    }\n" +
                "    if (canHarvest()) {\n" +
                "        harvest();\n" +
                "    }\n" +
                "    if (!move()) {\n" +
                "        turnRight();\n" +
                "    }\n" +
                "}"
        ));

        list.add(Box.createVerticalStrut(8));

        list.add(createPatternCard(
                "Rapid Multi-Crop Sower",
                "Alternate between planting carrots and wheat depending on the X coordinate.",
                "while (true) {\n" +
                "    if (!isTilled()) till();\n" +
                "    if (getCrop().equals(\"NONE\")) {\n" +
                "        if (getPosX() % 2 == 0) {\n" +
                "            plant(\"CARROT\");\n" +
                "        } else {\n" +
                "            plant(\"WHEAT\");\n" +
                "        }\n" +
                "        water();\n" +
                "    }\n" +
                "    if (canHarvest()) harvest();\n" +
                "    if (!move()) turnRight();\n" +
                "}"
        ));

        list.add(Box.createVerticalStrut(8));

        list.add(createPatternCard(
                "Harvest Counter with Flip Celebration",
                "Keeps track of harvested crops and does a celebratory flip every 10 harvests.",
                "int count = 0;\n" +
                "while (true) {\n" +
                "    if (canHarvest()) {\n" +
                "        harvest();\n" +
                "        count++;\n" +
                "        if (count % 10 == 0) {\n" +
                "            doAFlip();\n" +
                "        }\n" +
                "    }\n" +
                "    if (!move()) turnRight();\n" +
                "}"
        ));

        list.add(Box.createVerticalStrut(8));

        list.add(createPatternCard(
                "Do-While Loop (Search Until Harvestable)",
                "Executes movement at least once before evaluating canHarvest() condition.",
                "while (true) {\n" +
                "    do {\n" +
                "        if (!move()) turnRight();\n" +
                "    } while (!canHarvest());\n" +
                "    harvest();\n" +
                "    plant(\"WHEAT\");\n" +
                "    water();\n" +
                "}"
        ));

        list.add(Box.createVerticalStrut(8));

        list.add(createPatternCard(
                "Switch-Case (Crop Type Classifier)",
                "Branches actions cleanly depending on what crop is currently on the tile.",
                "while (true) {\n" +
                "    switch (getCrop()) {\n" +
                "        case \"WHEAT\":\n" +
                "            if (canHarvest()) harvest();\n" +
                "            break;\n" +
                "        case \"CARROT\":\n" +
                "            harvest();\n" +
                "            break;\n" +
                "        default:\n" +
                "            if (!isTilled()) till();\n" +
                "            plant(\"WHEAT\");\n" +
                "            water();\n" +
                "            break;\n" +
                "    }\n" +
                "    if (!move()) turnRight();\n" +
                "}"
        ));

        list.add(Box.createVerticalStrut(8));

        list.add(createPatternCard(
                "For-Each Array Loop (Rotating Crops)",
                "Loops through an array of crop strings using an enhanced for-each loop.",
                "String[] crops = {\"WHEAT\", \"CARROT\", \"WHEAT\"};\n" +
                "while (true) {\n" +
                "    for (String c : crops) {\n" +
                "        if (!isTilled()) till();\n" +
                "        if (canHarvest()) harvest();\n" +
                "        plant(c);\n" +
                "        water();\n" +
                "        if (!move()) turnRight();\n" +
                "    }\n" +
                "}"
        ));

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel createApiCard(String signature, String returnType, String description, String example, String insertCode) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(new Color(36, 40, 48));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Top line: Method signature & return type badge
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topRow.setOpaque(false);

        JLabel sigLabel = new JLabel(signature);
        sigLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        sigLabel.setForeground(new Color(134, 239, 172));

        JLabel typeBadge = new JLabel(" " + returnType + " ");
        typeBadge.setFont(new Font("Consolas", Font.PLAIN, 11));
        typeBadge.setOpaque(true);
        typeBadge.setBackground(new Color(50, 56, 68));
        typeBadge.setForeground(new Color(251, 191, 36));

        topRow.add(sigLabel);
        topRow.add(typeBadge);
        card.add(topRow, BorderLayout.NORTH);

        // Center: Description and code example
        JPanel centerBox = new JPanel(new GridLayout(2, 1, 0, 4));
        centerBox.setOpaque(false);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(220, 225, 235));

        JLabel codePreview = new JLabel("<html><font color='#8F96A3'>Example: </font><font color='#FCD34D'>" + example.replace("\n", " ") + "</font></html>");
        codePreview.setFont(new Font("Consolas", Font.PLAIN, 11));

        centerBox.add(descLabel);
        centerBox.add(codePreview);
        card.add(centerBox, BorderLayout.CENTER);

        // Right: Insert button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);

        JButton insertBtn = new JButton("Insert");
        insertBtn.setFont(new Font("Consolas", Font.BOLD, 11));
        insertBtn.setBackground(new Color(50, 56, 68));
        insertBtn.setForeground(Color.WHITE);
        insertBtn.setFocusPainted(false);
        insertBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        insertBtn.addActionListener(e -> {
            if (codeWindow != null) {
                String existing = codeWindow.getCode();
                if (existing == null || existing.trim().isEmpty()) {
                    codeWindow.setCode(insertCode);
                } else {
                    codeWindow.setCode(existing + "\n" + insertCode);
                }
            }
        });

        btnPanel.add(insertBtn);
        card.add(btnPanel, BorderLayout.EAST);

        return card;
    }

    private JPanel createPatternCard(String title, String description, String fullCode) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(new Color(36, 40, 48));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        titleLabel.setForeground(new Color(251, 191, 36));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(215, 220, 230));

        JPanel topBox = new JPanel(new GridLayout(2, 1, 0, 3));
        topBox.setOpaque(false);
        topBox.add(titleLabel);
        topBox.add(descLabel);
        card.add(topBox, BorderLayout.NORTH);

        JTextArea codeArea = new JTextArea(fullCode);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        codeArea.setBackground(new Color(26, 28, 34));
        codeArea.setForeground(new Color(134, 239, 172));
        codeArea.setEditable(false);
        codeArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        card.add(codeArea, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        btnPanel.setOpaque(false);

        JButton loadBtn = new JButton("Load into Editor");
        loadBtn.setFont(new Font("Consolas", Font.BOLD, 11));
        loadBtn.setBackground(new Color(50, 56, 68));
        loadBtn.setForeground(Color.WHITE);
        loadBtn.setFocusPainted(false);
        loadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadBtn.addActionListener(e -> {
            if (codeWindow != null) {
                codeWindow.setCode(fullCode);
            }
        });

        JButton copyBtn = new JButton("Copy");
        copyBtn.setFont(new Font("Consolas", Font.BOLD, 11));
        copyBtn.setBackground(new Color(50, 56, 68));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setFocusPainted(false);
        copyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fullCode), null);
            copyBtn.setText("Copied!");
            Timer t = new Timer(1500, ev -> copyBtn.setText("Copy"));
            t.setRepeats(false);
            t.start();
        });

        btnPanel.add(copyBtn);
        btnPanel.add(loadBtn);
        card.add(btnPanel, BorderLayout.SOUTH);

        return card;
    }
}
