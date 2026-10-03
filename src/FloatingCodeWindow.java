import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FloatingCodeWindow
 * Draggable in-game code editor with line numbering gutter and Java syntax highlighting.
 */
public class FloatingCodeWindow extends JPanel {
    private final JTextPane codePane;
    private final JTextArea lineNumbers;
    private final JLabel statusLabel;
    private final JButton stopBtn;
    private final JButton pausePlayBtn;
    private final JButton minBtn;
    private final JButton closeBtn;
    private final JButton executeBtn;
    private final JPanel contentPanel;

    private Point dragOffset;
    private boolean isMinimized = false;
    private int executingLineNumber = -1;

    // Action listener callback for control buttons
    public interface ActionListenerCallback {
        void onRun(String code);
        void onStop();
        void onTogglePause();
        void onReset();
    }

    private ActionListenerCallback callback;

    public FloatingCodeWindow(ActionListenerCallback callback) {
        this.callback = callback;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBounds(40, 40, 380, 270);

        // Dark rounded background container
        JPanel mainCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(32, 30, 31, 245));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(new Color(65, 60, 58));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2d.dispose();
            }
        };
        mainCard.setOpaque(false);

        // Window header bar (drag handle)
        JPanel header = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(56, 50, 48));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2d.fillRect(0, getHeight() - 6, getWidth(), 6);
                g2d.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 38));
        header.setBorder(new EmptyBorder(5, 10, 5, 10));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // Left header controls (Stop, Pause/Resume, Title)
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        leftHeader.setOpaque(false);

        stopBtn = createIconButton("■", new Color(239, 68, 68), new Color(248, 113, 113));
        stopBtn.setToolTipText("Stop Script");
        stopBtn.addActionListener(e -> {
            if (this.callback != null) this.callback.onStop();
        });

        pausePlayBtn = createIconButton("⏸", new Color(245, 158, 11), new Color(251, 191, 36));
        pausePlayBtn.setToolTipText("Pause / Resume");
        pausePlayBtn.addActionListener(e -> {
            if (this.callback != null) this.callback.onTogglePause();
        });

        JLabel titleLabel = new JLabel("Main.java");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 14));
        titleLabel.setForeground(new Color(241, 245, 249));

        leftHeader.add(stopBtn);
        leftHeader.add(pausePlayBtn);
        leftHeader.add(titleLabel);

        // Right header controls (Minimize, Reset)
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightHeader.setOpaque(false);

        minBtn = createIconButton("—", new Color(34, 197, 94), new Color(74, 222, 128));
        minBtn.setToolTipText("Minimize / Expand");
        minBtn.addActionListener(e -> toggleMinimize());

        closeBtn = createIconButton("✕", new Color(132, 204, 22), new Color(163, 230, 53));
        closeBtn.setToolTipText("Reset Code");
        closeBtn.addActionListener(e -> {
            if (this.callback != null) this.callback.onReset();
        });

        rightHeader.add(minBtn);
        rightHeader.add(closeBtn);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);

        // Window drag listeners
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point location = getLocation();
                    int newX = location.x + e.getX() - dragOffset.x;
                    int newY = location.y + e.getY() - dragOffset.y;
                    setLocation(Math.max(0, newX), Math.max(0, newY));
                }
            }
        });

        // Editor and line gutter panel
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Code text area
        codePane = new JTextPane();
        codePane.setBackground(new Color(24, 22, 23));
        codePane.setForeground(new Color(241, 245, 249));
        codePane.setCaretColor(new Color(248, 250, 252));
        codePane.setFont(new Font("Consolas", Font.PLAIN, 15));
        codePane.setMargin(new Insets(4, 8, 4, 8));

        // Starter Java code
        codePane.setText("while (true) {\n    if (canHarvest()) {\n        harvest();\n    }\n}");

        // Line numbers column
        lineNumbers = new JTextArea("1\n2\n3\n4\n5");
        lineNumbers.setBackground(new Color(20, 18, 19));
        lineNumbers.setForeground(new Color(100, 116, 139));
        lineNumbers.setFont(new Font("Consolas", Font.PLAIN, 15));
        lineNumbers.setEditable(false);
        lineNumbers.setMargin(new Insets(4, 6, 4, 6));

        // Update line gutter and syntax coloring on text change
        codePane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateLines(); highlightSyntax(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateLines(); highlightSyntax(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        JPanel editorWithGutter = new JPanel(new BorderLayout());
        editorWithGutter.setBackground(new Color(24, 22, 23));
        editorWithGutter.add(lineNumbers, BorderLayout.WEST);
        editorWithGutter.add(codePane, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(editorWithGutter);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 42, 43)));
        scrollPane.getViewport().setBackground(new Color(24, 22, 23));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom status bar and Run button
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(6, 4, 2, 4));

        statusLabel = new JLabel("● Ready");
        statusLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(148, 163, 184));

        // Custom painted Run button
        executeBtn = createCustomButton("▶ Run Script", new Color(234, 88, 12), new Color(249, 115, 22));
        executeBtn.setToolTipText("Run script (Ctrl+Enter / Ctrl+Shift+B / F5)");
        executeBtn.addActionListener(e -> {
            if (this.callback != null) {
                this.callback.onRun(codePane.getText());
            }
        });

        // Key shortcuts inside code editor to trigger Run
        Action runAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (FloatingCodeWindow.this.callback != null) {
                    FloatingCodeWindow.this.callback.onRun(codePane.getText());
                }
            }
        };
        codePane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "runScript");
        codePane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "runScript");
        codePane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "runScript");
        codePane.getActionMap().put("runScript", runAction);

        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(executeBtn, BorderLayout.EAST);

        contentPanel.add(bottomBar, BorderLayout.SOUTH);

        mainCard.add(header, BorderLayout.NORTH);
        mainCard.add(contentPanel, BorderLayout.CENTER);

        add(mainCard, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            updateLines();
            highlightSyntax();
        });
    }

    private JButton createIconButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isRollover() ? hover : bg);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(Color.WHITE);
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Run button with custom styling
    private JButton createCustomButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isRollover() ? hover : bg);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);
                g2d.dispose();
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(115, 28));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Toggle window minimize/expand
    private void toggleMinimize() {
        isMinimized = !isMinimized;
        contentPanel.setVisible(!isMinimized);
        if (isMinimized) {
            setSize(getWidth(), 38);
        } else {
            setSize(getWidth(), 270);
        }
        revalidate();
        repaint();
    }

    // Refresh line gutter numbers
    private void updateLines() {
        int lineCount = codePane.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= Math.max(1, lineCount); i++) {
            if (i == executingLineNumber) {
                sb.append("▶").append(i).append("\n"); // Active executing line marker
            } else {
                sb.append(" ").append(i).append("\n");
            }
        }
        lineNumbers.setText(sb.toString());
    }

    public void setExecutingLine(int lineNum, String lineText) {
        this.executingLineNumber = lineNum;
        SwingUtilities.invokeLater(() -> {
            updateLines();
            statusLabel.setText("● Line " + lineNum + ": " + (lineText.length() > 20 ? lineText.substring(0, 18) + "..." : lineText));
            statusLabel.setForeground(new Color(251, 191, 36));
        });
    }

    public void setStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            this.executingLineNumber = -1;
            updateLines();
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    public void setPausePlayState(boolean isPaused) {
        SwingUtilities.invokeLater(() -> {
            if (isPaused) {
                pausePlayBtn.setText("▶");
                statusLabel.setText("⏸ Paused");
                statusLabel.setForeground(new Color(251, 146, 60));
            } else {
                pausePlayBtn.setText("⏸");
            }
        });
    }

    public String getCode() {
        return codePane.getText();
    }

    public void setCode(String code) {
        codePane.setText(code);
    }

    // Regex-based Java syntax highlighter
    private boolean isHighlighting = false;

    private void highlightSyntax() {
        if (isHighlighting) return;
        isHighlighting = true;

        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = codePane.getStyledDocument();
                String text = doc.getText(0, doc.getLength());

                // Default text color
                SimpleAttributeSet defStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(defStyle, new Color(241, 245, 249));
                StyleConstants.setBold(defStyle, false);
                doc.setCharacterAttributes(0, text.length(), defStyle, true);

                // Java Keywords (Warm Golden Yellow)
                SimpleAttributeSet keywordStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(keywordStyle, new Color(253, 224, 71));
                StyleConstants.setBold(keywordStyle, true);

                String keywordPattern = "\\b(public|private|protected|static|final|class|void|int|boolean|String|double|float|long|char|while|for|if|else|return|true|false|null|break|continue|new|import|package)\\b";
                applyPattern(doc, text, keywordPattern, keywordStyle);

                // Drone API Functions (Fresh Emerald Green)
                SimpleAttributeSet funcStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(funcStyle, new Color(134, 239, 172));
                StyleConstants.setBold(funcStyle, false);

                String funcPattern = "\\b(harvest|canHarvest|move|turnLeft|turnRight|plant|water|till|getPosX|getPosY|getWorldSize|getCrop|isTilled|isWatered|doAFlip|println|print|equals|sleep)\\b";
                applyPattern(doc, text, funcPattern, funcStyle);

                // Constants & Types (Coral Orange)
                SimpleAttributeSet constStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(constStyle, new Color(251, 146, 60));
                String constPattern = "\\b(WHEAT|CARROT|GRASS|System|Math|String|Integer|Boolean)\\b";
                applyPattern(doc, text, constPattern, constStyle);

                // Strings (Warm Amber)
                SimpleAttributeSet strStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(strStyle, new Color(252, 211, 77));
                String strPattern = "(\"[^\"]*\"|'[^']*')";
                applyPattern(doc, text, strPattern, strStyle);

                // Comments (Muted Slate Italic)
                SimpleAttributeSet commentStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(commentStyle, new Color(100, 116, 139));
                StyleConstants.setItalic(commentStyle, true);
                String commentPattern = "(//.*|/\\*[\\s\\S]*?\\*/)";
                applyPattern(doc, text, commentPattern, commentStyle);

            } catch (Exception ignored) {
            } finally {
                isHighlighting = false;
            }
        });
    }

    private void applyPattern(StyledDocument doc, String text, String regex, AttributeSet attr) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while (m.find()) {
            doc.setCharacterAttributes(m.start(), m.end() - m.start(), attr, false);
        }
    }
}
