import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * aeroFarm Engine - Drone Automation & Scripting Simulation
 * Main application entry point using Java Swing with a layered architecture.
 */
public class Main {
    private static MiniJavaInterpreter interpreter;
    private static FloatingCodeWindow codeWindow;
    private static FarmPanel farmPanel;
    private static JLabel hayStatsLabel;
    private static JLabel wheatStatsLabel;
    private static int currentGridSize = 3; // Default 3x3 island
    private static JFrame mainFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("aeroFarm Engine - Java Drone Automation");
            mainFrame = frame;
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1050, 720);
            frame.setMinimumSize(new Dimension(850, 550));
            frame.setLocationRelativeTo(null);

            // Layered pane allows the floating code window to sit above the farm canvas
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setLayout(null);

            // Layer 1: Isometric farm canvas
            farmPanel = new FarmPanel();
            farmPanel.setBounds(0, 0, frame.getWidth(), frame.getHeight());
            layeredPane.add(farmPanel, JLayeredPane.DEFAULT_LAYER);

            // Layer 2: Script engine and execution callbacks
            interpreter = new MiniJavaInterpreter(farmPanel, new MiniJavaInterpreter.ExecutionListener() {
                @Override
                public void onLineExecute(int lineNumber, String lineText) {
                    codeWindow.setExecutingLine(lineNumber, lineText);
                    updateStats();
                }

                @Override
                public void onPrint(String message) {
                    System.out.println("[Drone] " + message);
                }

                @Override
                public void onError(int lineNumber, String message) {
                    codeWindow.setStatus("Line " + lineNumber + ": " + message, new Color(248, 113, 113));
                }

                @Override
                public void onFinished() {
                    codeWindow.setStatus("Idle / Completed", new Color(156, 163, 175));
                    codeWindow.setPausePlayState(false);
                    updateStats();
                }
            });

            // Layer 3: Floating code editor window
            codeWindow = new FloatingCodeWindow(new FloatingCodeWindow.ActionListenerCallback() {
                @Override
                public void onRun(String code) {
                    codeWindow.setStatus("Running...", new Color(74, 222, 128));
                    codeWindow.setPausePlayState(false);
                    interpreter.start(code);
                }

                @Override
                public void onStop() {
                    interpreter.stop();
                    codeWindow.setStatus("Stopped", new Color(251, 113, 133));
                }

                @Override
                public void onTogglePause() {
                    interpreter.togglePause();
                    codeWindow.setPausePlayState(interpreter.isPaused());
                }

                @Override
                public void onReset() {
                    interpreter.stop();
                    farmPanel.resetGrid();
                    loadDefaultPresetForGrid(currentGridSize);
                    codeWindow.setStatus("Reset to default", new Color(161, 161, 170));
                }
            });

            // Initial editor window placement (top left)
            codeWindow.setBounds(40, 40, 380, 270);
            layeredPane.add(codeWindow, JLayeredPane.PALETTE_LAYER);

            // Layer 4: Top-right HUD control bar and counters
            int hudWidth = 570;
            int hudHeight = 52;
            JPanel topHud = createHudPanel();
            topHud.setBounds(frame.getWidth() - hudWidth - 25, 16, hudWidth, hudHeight);
            layeredPane.add(topHud, JLayeredPane.PALETTE_LAYER);

            // Re-align canvas and HUD when window is resized
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    farmPanel.setBounds(0, 0, frame.getWidth(), frame.getHeight());
                    topHud.setBounds(Math.max(10, frame.getWidth() - hudWidth - 25), 16, hudWidth, hudHeight);
                }
            });

            // Refresh harvest stats periodically
            Timer statsTimer = new Timer(300, e -> updateStats());
            statsTimer.start();

            frame.setContentPane(layeredPane);
            frame.setVisible(true);
        });
    }

    // Top-right dark glass HUD panel with resource counters and action menus
    private static JPanel createHudPanel() {
        JPanel hud = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(28, 30, 34, 240));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(new Color(60, 65, 75));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        hud.setOpaque(false);
        hud.setBorder(new EmptyBorder(5, 14, 5, 12));

        // Stacked Hay and Wheat resource counters
        JPanel statsBox = new JPanel(new GridLayout(2, 1, 0, 1));
        statsBox.setOpaque(false);

        hayStatsLabel = new JLabel("Hay:   0");
        hayStatsLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        hayStatsLabel.setForeground(new Color(134, 239, 172));

        wheatStatsLabel = new JLabel("Wheat: 0");
        wheatStatsLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        wheatStatsLabel.setForeground(new Color(253, 224, 71));

        statsBox.add(hayStatsLabel);
        statsBox.add(wheatStatsLabel);
        hud.add(statsBox, BorderLayout.WEST);

        // Control buttons: Speed, Island Size, and Script Presets
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        rightControls.setOpaque(false);

        // Speed dropdown menu
        JButton speedBtn = createHudPillButton("Speed: 1x ▾", 100);
        JPopupMenu speedMenu = createDarkPopupMenu();
        addMenuItem(speedMenu, "1x Speed (Normal)", () -> {
            interpreter.setStepDelayMs(300);
            speedBtn.setText("Speed: 1x ▾");
        });
        addMenuItem(speedMenu, "2x Speed (Fast)", () -> {
            interpreter.setStepDelayMs(150);
            speedBtn.setText("Speed: 2x ▾");
        });
        addMenuItem(speedMenu, "4x Speed (Turbo)", () -> {
            interpreter.setStepDelayMs(50);
            speedBtn.setText("Speed: 4x ▾");
        });
        addMenuItem(speedMenu, "Instant Speed", new Color(251, 191, 36), () -> {
            interpreter.setStepDelayMs(10);
            speedBtn.setText("Speed: Max ▾");
        });
        speedBtn.addActionListener(e -> speedMenu.show(speedBtn, 0, speedBtn.getHeight() + 4));

        // Island grid size dropdown menu
        JButton gridBtn = createHudPillButton("Grid: 3x3 ▾", 95);
        JPopupMenu gridMenu = createDarkPopupMenu();
        addMenuItem(gridMenu, "1x1 Single Block", () -> {
            currentGridSize = 1;
            farmPanel.setGridSize(1);
            gridBtn.setText("Grid: 1x1 ▾");
            loadDefaultPresetForGrid(1);
        });
        addMenuItem(gridMenu, "3x3 Standard Island", () -> {
            currentGridSize = 3;
            farmPanel.setGridSize(3);
            gridBtn.setText("Grid: 3x3 ▾");
            loadDefaultPresetForGrid(3);
        });
        addMenuItem(gridMenu, "4x4 Large Island", () -> {
            currentGridSize = 4;
            farmPanel.setGridSize(4);
            gridBtn.setText("Grid: 4x4 ▾");
            loadDefaultPresetForGrid(4);
        });
        gridBtn.addActionListener(e -> gridMenu.show(gridBtn, 0, gridBtn.getHeight() + 4));

        // Dynamic script presets menu
        JButton presetsBtn = createHudPillButton("Presets ▾", 88);
        presetsBtn.addActionListener(e -> showDynamicPresetsMenu(presetsBtn));

        // Drone API and Automation Handbook button
        JButton handbookBtn = createHudPillButton("Handbook ▾", 98);
        handbookBtn.addActionListener(e -> showHandbookMenu(handbookBtn));

        rightControls.add(speedBtn);
        rightControls.add(gridBtn);
        rightControls.add(presetsBtn);
        rightControls.add(handbookBtn);

        hud.add(rightControls, BorderLayout.EAST);
        return hud;
    }

    // Load appropriate starter script when grid size changes
    private static void loadDefaultPresetForGrid(int size) {
        if (codeWindow != null) {
            codeWindow.setCode(CodePresets.getDefaultPresetForGrid(size));
        }
    }

    // Open dynamic categorized presets menu
    private static void showDynamicPresetsMenu(Component invoker) {
        JPopupMenu menu = createDarkPopupMenu();

        java.util.List<CodePresets.PresetCategory> categories = CodePresets.getCategorizedPresets(currentGridSize);
        for (int i = 0; i < categories.size(); i++) {
            CodePresets.PresetCategory cat = categories.get(i);
            if (i > 0) {
                addMenuSeparator(menu);
            }

            JMenu subMenu = new JMenu(cat.getName()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isArmed() || getModel().isSelected() || getModel().isRollover()) {
                        g2d.setColor(new Color(52, 58, 74));
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2d.setColor(new Color(88, 98, 122));
                        g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    }
                    g2d.setColor(getModel().isArmed() || getModel().isSelected() || getModel().isRollover() ? Color.WHITE : new Color(225, 230, 242));
                    g2d.setFont(getFont());
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = 10;
                    int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(getText(), x, y);

                    // Subtle right arrow indicator ▸
                    g2d.setColor(new Color(170, 180, 200));
                    int arrowX = getWidth() - 12;
                    int arrowY = getHeight() / 2;
                    int[] xPoints = {arrowX, arrowX + 4, arrowX};
                    int[] yPoints = {arrowY - 3, arrowY, arrowY + 3};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    g2d.dispose();
                }
            };
            subMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subMenu.setOpaque(false);
            subMenu.setContentAreaFilled(false);
            subMenu.setBorderPainted(false);
            subMenu.setPreferredSize(new Dimension(subMenu.getPreferredSize().width + 24, 26));
            subMenu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPopupMenu subPopup = subMenu.getPopupMenu();
            subPopup.setOpaque(true);
            subPopup.setBackground(new Color(28, 30, 36));
            subPopup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(65, 72, 88), 1),
                    new EmptyBorder(6, 6, 6, 6)
            ));

            for (CodePresets.Preset preset : cat.getPresets()) {
                addMenuItem(subPopup, preset.getTitle(), new Color(225, 230, 242), () -> {
                    if (codeWindow != null) {
                        codeWindow.setCode(preset.getCode());
                    }
                });
            }
            menu.add(subMenu);
        }

        menu.show(invoker, 0, invoker.getHeight() + 4);
    }

    // Interactive Handbook & Quick-insert reference menu
    private static void showHandbookMenu(Component invoker) {
        JPopupMenu menu = createDarkPopupMenu();

        addMenuItem(menu, "[*] Open API & Drone Handbook...", new Color(251, 191, 36), () -> {
            HandbookDialog dialog = new HandbookDialog(mainFrame, codeWindow);
            dialog.setVisible(true);
        });
        addMenuSeparator(menu);

        addMenuItem(menu, "+ harvest()", new Color(134, 239, 172), () -> appendCodeToEditor("harvest();"));
        addMenuItem(menu, "+ plant(\"WHEAT\")", new Color(134, 239, 172), () -> appendCodeToEditor("plant(\"WHEAT\");"));
        addMenuItem(menu, "+ water()", new Color(134, 239, 172), () -> appendCodeToEditor("water();"));
        addMenuItem(menu, "+ till()", new Color(134, 239, 172), () -> appendCodeToEditor("till();"));
        addMenuItem(menu, "+ canHarvest() check", new Color(253, 224, 71), () -> appendCodeToEditor("if (canHarvest()) {\n    harvest();\n}"));
        addMenuItem(menu, "+ move() & turnRight()", new Color(225, 230, 242), () -> appendCodeToEditor("if (!move()) {\n    turnRight();\n}"));
        addMenuItem(menu, "+ doAFlip()", new Color(251, 146, 60), () -> appendCodeToEditor("doAFlip();"));

        menu.show(invoker, 0, invoker.getHeight() + 4);
    }

    private static void appendCodeToEditor(String snippet) {
        if (codeWindow != null) {
            String current = codeWindow.getCode();
            if (current == null || current.trim().isEmpty()) {
                codeWindow.setCode(snippet);
            } else {
                codeWindow.setCode(current + "\n" + snippet);
            }
        }
    }

    private static JPopupMenu createDarkPopupMenu() {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(28, 30, 36, 250));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(new Color(65, 72, 88));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2d.dispose();
            }
        };
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(6, 6, 6, 6));
        return menu;
    }

    private static void addMenuItem(JPopupMenu menu, String text, Runnable action) {
        addMenuItem(menu, text, new Color(225, 230, 242), action);
    }

    private static void addMenuItem(JPopupMenu menu, String text, Color textColor, Runnable action) {
        JMenuItem item = new JMenuItem(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isArmed() || getModel().isRollover()) {
                    g2d.setColor(new Color(52, 58, 74));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2d.setColor(new Color(88, 98, 122));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }
                g2d.setColor(getModel().isArmed() || getModel().isRollover() ? Color.WHITE : textColor);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = 10;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.setOpaque(false);
        item.setContentAreaFilled(false);
        item.setBorderPainted(false);
        item.setPreferredSize(new Dimension(item.getPreferredSize().width + 24, 26));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private static void addMenuSeparator(JPopupMenu menu) {
        JSeparator sep = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(55, 62, 78));
                g.drawLine(6, getHeight() / 2, getWidth() - 6, getHeight() / 2);
            }
        };
        sep.setPreferredSize(new Dimension(0, 8));
        sep.setOpaque(false);
        menu.add(sep);
    }

    private static JButton createHudPillButton(String text, int width) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(68, 74, 88) : new Color(48, 52, 62);
                g2d.setColor(bg);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.setColor(new Color(85, 92, 108));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(width, 28));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static void updateStats() {
        if (farmPanel != null && hayStatsLabel != null && wheatStatsLabel != null) {
            SwingUtilities.invokeLater(() -> {
                hayStatsLabel.setText("Hay:   " + farmPanel.getHayHarvested());
                wheatStatsLabel.setText("Wheat: " + farmPanel.getWheatHarvested());
            });
        }
    }
}