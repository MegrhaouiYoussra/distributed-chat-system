package chat;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ChatClient — Java Swing GUI
 * ============================
 * A graphical chat client that connects to the Server via TCP.
 * Features:
 *   - Login dialog (username entry)
 *   - Chat window with message bubbles
 *   - Online users panel
 *   - Real-time message receiving (background thread)
 */
public class ChatClient extends JFrame {

    // ── Colors ────────────────────────────────────────────────────────────
    private static final Color BG_DARK     = new Color(245, 245, 250);
    private static final Color BG_SIDEBAR  = new Color(255, 255, 255);
    private static final Color BUBBLE_OWN  = new Color(74, 108, 247);
    private static final Color BUBBLE_OTHER= new Color(255, 255, 255);
    private static final Color TEXT_OWN    = Color.WHITE;
    private static final Color TEXT_OTHER  = new Color(30, 30, 30);
    private static final Color BORDER_CLR  = new Color(230, 230, 235);
    private static final Color HEADER_BG   = new Color(255, 255, 255);
    private static final Color SEND_BTN    = new Color(74, 108, 247);
    private static final Color ONLINE_DOT  = new Color(57, 153, 34);
    private static final Color TEXT_MUTED  = new Color(140, 140, 150);
    private static final Color SYSTEM_CLR  = new Color(150, 150, 160);

    // ── Network ───────────────────────────────────────────────────────────
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    // ── UI Components ─────────────────────────────────────────────────────
    private JPanel  messagesPanel;
    private JScrollPane scrollPane;
    private JTextField  inputField;
    private JPanel  usersPanel;
    private JLabel  statusLabel;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════
    public ChatClient(String username) {
        this.username = username;
        buildUI();
        connectToServer();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BUILD UI
    // ══════════════════════════════════════════════════════════════════════
    private void buildUI() {
        setTitle("ChatApp — " + username);
        setSize(850, 600);
        setMinimumSize(new Dimension(600, 450));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ── Root layout ───────────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        // ── Left sidebar ──────────────────────────────────────────────────
        JPanel sidebar = buildSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // ── Main chat area ────────────────────────────────────────────────
        JPanel chatArea = buildChatArea();
        root.add(chatArea, BorderLayout.CENTER);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_CLR));

        // Header
        JPanel sideHeader = new JPanel(new BorderLayout());
        sideHeader.setBackground(BG_SIDEBAR);
        sideHeader.setBorder(new EmptyBorder(16, 14, 12, 14));
        JLabel appTitle = new JLabel("ChatApp");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        appTitle.setForeground(new Color(30, 30, 30));
        JLabel serverInfo = new JLabel("Server: localhost:" + Server.PORT);
        serverInfo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        serverInfo.setForeground(TEXT_MUTED);
        sideHeader.add(appTitle, BorderLayout.NORTH);
        sideHeader.add(serverInfo, BorderLayout.SOUTH);
        sidebar.add(sideHeader, BorderLayout.NORTH);

        // Users label
        JPanel usersSection = new JPanel(new BorderLayout());
        usersSection.setBackground(BG_SIDEBAR);
        JLabel usersLabel = new JLabel("ONLINE");
        usersLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        usersLabel.setForeground(TEXT_MUTED);
        usersLabel.setBorder(new EmptyBorder(8, 14, 4, 14));
        usersSection.add(usersLabel, BorderLayout.NORTH);

        usersPanel = new JPanel();
        usersPanel.setBackground(BG_SIDEBAR);
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        addUserToSidebar(username + " (You)", true);
        usersSection.add(new JScrollPane(usersPanel) {{
            setBorder(null);
            setBackground(BG_SIDEBAR);
            getViewport().setBackground(BG_SIDEBAR);
        }}, BorderLayout.CENTER);
        sidebar.add(usersSection, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        sidebar.add(statusLabel, BorderLayout.SOUTH);

        return sidebar;
    }

    private void addUserToSidebar(String name, boolean isYou) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        item.setBackground(BG_SIDEBAR);
        item.setMaximumSize(new Dimension(200, 40));

        // Avatar circle
        JLabel avatar = new JLabel(name.substring(0, Math.min(2, name.length())).toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isYou ? new Color(238, 241, 254) : new Color(253, 238, 245));
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        avatar.setPreferredSize(new Dimension(28, 28));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 10));
        avatar.setForeground(isYou ? new Color(42, 63, 143) : new Color(153, 53, 86));
        avatar.setOpaque(false);

        JLabel nameLabel = new JLabel(name.length() > 12 ? name.substring(0, 12) + "..." : name);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        nameLabel.setForeground(new Color(40, 40, 40));

        // Online dot
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.PLAIN, 8));
        dot.setForeground(ONLINE_DOT);

        item.add(avatar);
        item.add(nameLabel);
        item.add(dot);

        SwingUtilities.invokeLater(() -> {
            usersPanel.add(item);
            usersPanel.revalidate();
        });
    }

    // ── Chat area ─────────────────────────────────────────────────────────
    private JPanel buildChatArea() {
        JPanel chatArea = new JPanel(new BorderLayout());
        chatArea.setBackground(BG_DARK);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(HEADER_BG);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR),
            new EmptyBorder(12, 16, 12, 16)
        ));
        JLabel chatTitle = new JLabel("Group Chat");
        chatTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel chatSub = new JLabel("TCP port " + Server.PORT + " · Java Sockets + Multithreading");
        chatSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        chatSub.setForeground(TEXT_MUTED);
        topBar.add(chatTitle, BorderLayout.NORTH);
        topBar.add(chatSub, BorderLayout.SOUTH);
        chatArea.add(topBar, BorderLayout.NORTH);

        // Messages area
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_DARK);
        messagesPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(BG_DARK);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatArea.add(scrollPane, BorderLayout.CENTER);

        // Input area
        JPanel inputArea = buildInputArea();
        chatArea.add(inputArea, BorderLayout.SOUTH);

        return chatArea;
    }

    private JPanel buildInputArea() {
        JPanel inputArea = new JPanel(new BorderLayout(8, 0));
        inputArea.setBackground(HEADER_BG);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR),
            new EmptyBorder(10, 16, 10, 16)
        ));

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_CLR, 1, true),
            new EmptyBorder(8, 14, 8, 14)
        ));
        inputField.addActionListener(e -> sendMessage());
        inputField.setBackground(new Color(248, 248, 252));

        JButton sendBtn = new JButton("Send") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(42, 63, 143) : SEND_BTN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setPreferredSize(new Dimension(80, 36));
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendBtn.addActionListener(e -> sendMessage());

        inputArea.add(inputField, BorderLayout.CENTER);
        inputArea.add(sendBtn, BorderLayout.EAST);
        return inputArea;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NETWORK
    // ══════════════════════════════════════════════════════════════════════
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", Server.PORT);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Send username immediately
                out.println(username);
                SwingUtilities.invokeLater(() -> statusLabel.setText("● Connected"));

                // Listen for messages
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> handleIncoming(msg));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("✗ Disconnected");
                    appendSystemMessage("Could not connect to server. Is it running?");
                });
            }
        }).start();
    }

    private void handleIncoming(String raw) {
        if (raw.startsWith("MSG:")) {
            String[] parts = raw.substring(4).split(":", 2);
            if (parts.length == 2) appendMessage(parts[0], parts[1], false);
        } else if (raw.startsWith("SYSTEM:")) {
            appendSystemMessage(raw.substring(7));
            // Add user to sidebar if they joined
            if (raw.contains("joined the chat")) {
                String name = raw.substring(7).replace(" joined the chat", "").trim();
                addUserToSidebar(name, false);
            }
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null) return;
        out.println(text);
        appendMessage(username, text, true);
        inputField.setText("");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MESSAGE RENDERING
    // ══════════════════════════════════════════════════════════════════════
    private void appendMessage(String sender, String text, boolean isOwn) {
        JPanel row = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        row.setBackground(BG_DARK);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel bubble = new JPanel(new BorderLayout(0, 2));
        bubble.setBackground(isOwn ? BUBBLE_OWN : BUBBLE_OTHER);
        bubble.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(isOwn ? BUBBLE_OWN : BORDER_CLR, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));

        if (!isOwn) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            senderLabel.setForeground(new Color(74, 108, 247));
            bubble.add(senderLabel, BorderLayout.NORTH);
        }

        JTextArea msgText = new JTextArea(text);
        msgText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        msgText.setForeground(isOwn ? TEXT_OWN : TEXT_OTHER);
        msgText.setBackground(isOwn ? BUBBLE_OWN : BUBBLE_OTHER);
        msgText.setEditable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setOpaque(false);
        msgText.setColumns(25);
        msgText.setMaximumSize(new Dimension(320, Integer.MAX_VALUE));
        bubble.add(msgText, BorderLayout.CENTER);

        JLabel timeLabel = new JLabel(TIME_FMT.format(new Date()));
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(isOwn ? new Color(200, 210, 255) : TEXT_MUTED);
        timeLabel.setHorizontalAlignment(isOwn ? SwingConstants.RIGHT : SwingConstants.LEFT);
        bubble.add(timeLabel, BorderLayout.SOUTH);

        row.add(bubble);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_DARK);
        wrapper.setBorder(new EmptyBorder(3, isOwn ? 60 : 0, 3, isOwn ? 0 : 60));
        wrapper.add(row, BorderLayout.CENTER);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        messagesPanel.add(wrapper);
        messagesPanel.add(Box.createVerticalStrut(4));
        messagesPanel.revalidate();
        scrollToBottom();
    }

    private void appendSystemMessage(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.ITALIC, 11));
        label.setForeground(SYSTEM_CLR);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(new EmptyBorder(4, 0, 4, 0));
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setBackground(BG_DARK);
        wrapper.add(label);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.add(wrapper);
        messagesPanel.revalidate();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAIN — Login dialog then launch
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // Login dialog
            JPanel loginPanel = new JPanel(new GridLayout(2, 1, 4, 4));
            loginPanel.add(new JLabel("Enter your username:"));
            JTextField usernameField = new JTextField(15);
            loginPanel.add(usernameField);

            int result = JOptionPane.showConfirmDialog(
                null, loginPanel, "ChatApp — Login",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String name = usernameField.getText().trim();
                if (name.isEmpty()) name = "User" + (int)(Math.random() * 100);
                ChatClient client = new ChatClient(name);
                client.setVisible(true);
            }
        });
    }
}
