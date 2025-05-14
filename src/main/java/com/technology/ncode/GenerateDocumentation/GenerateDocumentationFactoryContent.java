package com.technology.ncode.GenerateDocumentation;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.intellij.openapi.project.Project;
import com.technology.ncode.UsageMetricsReporter;
import com.technology.ncode.vertexai.DocumentationVertexAi;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class GenerateDocumentationFactoryContent extends JPanel {
    private JTextPane chatOutputArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel waitingMessageLabel;
    private String lastSelectedCode = "";
    private static final String PLACEHOLDER_TEXT = "Ask NCode...";
    private List<UserConversation> conversationHistory = new ArrayList<>();
    private JPanel chatPanel;
    private final Project project;

    public GenerateDocumentationFactoryContent(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // Documentation Output Area
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);
        chatOutputArea.setBackground(Color.BLACK);
        chatOutputArea.setForeground(Color.WHITE);
        chatOutputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JScrollPane outputScrollPane = new JScrollPane(chatOutputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder());
        outputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(outputScrollPane, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.BLACK);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Input Field with placeholder
        inputField = new JTextField(PLACEHOLDER_TEXT);
        inputField.setCaretColor(Color.WHITE);
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBackground(Color.BLACK); // Black background for input field
        inputField.setForeground(Color.WHITE); // White text color
        inputField.setCaretColor(Color.WHITE); // Caret color white
        inputField.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15)); // Padding inside the input field
        inputField.setPreferredSize(new Dimension(800, 50)); // Full width input field

        // Placeholder Logic for Input Field
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals(PLACEHOLDER_TEXT)) {
                    inputField.setText("");
                    inputField.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputField.getText().trim().isEmpty()) {
                    inputField.setText(PLACEHOLDER_TEXT);
                    inputField.setForeground(Color.GRAY);
                }
            }
        });
        inputField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
        inputField.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendUserQuery();
            }
        });

        // Send Button Setup
        sendButton = new JButton("➤");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        sendButton.setForeground(Color.WHITE); // White icon color
        sendButton.setContentAreaFilled(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setBackground(new Color(50, 50, 50)); // Grey background for send button
        sendButton.setOpaque(true);
        sendButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        // Send Button Action
        sendButton.addActionListener(e -> sendUserQuery());

        // Dropdown Setup
        String[] options = { "gemini-1.5-flash-002", "claude 3.5 Sonnet", "claude 3.7 Sonnet" };
        JComboBox<String> modelDropdown = new JComboBox<>(options);
        modelDropdown.setBackground(Color.DARK_GRAY); // Black background for dropdown
        modelDropdown.setForeground(Color.WHITE); // White text color
        modelDropdown.setFont(new Font("Times new Roman", Font.PLAIN, 14)); // Smaller font for dropdown
        modelDropdown.setPreferredSize(new Dimension(200, 30));
        modelDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index > 0) { // Disable Claude models
                    component.setForeground(Color.GRAY);
                    component.setEnabled(false);
                } else {
                    component.setForeground(Color.WHITE);
                }
                return component;
            }
        });

        // Prevent selection of disabled items
        modelDropdown.addActionListener(e -> {
            if (modelDropdown.getSelectedIndex() > 0) {
                modelDropdown.setSelectedIndex(0); // Force selection back to Gemini
            }
        });// Size for the dropdown

        // Send button container (aligned to right)
        JPanel sendButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sendButtonContainer.setBackground(new Color(50, 50, 50));
        sendButtonContainer.add(sendButton);
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));

        // Main container for dropdown and send button
        JPanel sendButtonAndDropdownContainer = new JPanel(new BorderLayout());
        sendButtonAndDropdownContainer.setBackground(new Color(50, 50, 50));
        sendButtonAndDropdownContainer.add(modelDropdown, BorderLayout.WEST);
        sendButtonAndDropdownContainer.add(sendButtonContainer, BorderLayout.EAST);

        // Container for input, send button, and dropdown
        JPanel inputPanelContainer = new JPanel(new BorderLayout());
        inputPanelContainer.setBackground(Color.BLACK);

        // Input field container
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBackground(Color.BLACK);
        inputContainer.setBorder(BorderFactory.createLineBorder(Color.gray)); // Border around input field
        inputContainer.add(inputField, BorderLayout.CENTER);
        inputPanelContainer.add(inputContainer, BorderLayout.NORTH);

        // Add send button and dropdown to the bottom of the input panel
        inputPanelContainer.add(sendButtonAndDropdownContainer, BorderLayout.SOUTH);

        // Add the inputPanelContainer to the main panel (bottom section)
        inputPanel.add(inputPanelContainer, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void clearChatHistory() {
        chatOutputArea.setText(""); // Clear chat visually
        conversationHistory.clear(); // Clear internal memory
    }

    public void setSelectedCode(String code) {
        lastSelectedCode = code;

        // Clear old conversation history
        conversationHistory.clear();

        // Always add system instruction
        conversationHistory.add(new UserConversation("system",
                "Instruction: You are an intelligent assistant helping the user with programming queries. " +
                        "Please refer to the code context to provide relevant and accurate responses."));

        // If code is selected
        if (code != null && !code.trim().isEmpty()) {
            appendUserMessage("Generate Documentation", true);
            appendWaitingMessage();
            disableInput();

            conversationHistory.add(new UserConversation("system", "Code Context:\n" + code));

            String userPrompt = "Give me the document for the code, which explains the workflow and execution process separately "
                    +
                    "and the output in the paragraph first and then execution steps in point-wise in brief in the README markdown file format\n";

            conversationHistory.add(new UserConversation("user", userPrompt));

            new Thread(() -> {
                String prompt = buildPrompt(userPrompt);

                DocumentationVertexAi docVertexAi = new DocumentationVertexAi();
                try {
                    GenerateContentResponse response = docVertexAi.generateContent(prompt);
                    String documentation = DocumentationVertexAi.extractGeneratedDocumentation(response);

                    // Report metrics for documentation generation
                    UsageMetricsReporter.reportMetricsAsync(
                            UsageMetricsReporter.getUserInfo().email,
                            UsageMetricsReporter.getUserInfo().projectId,
                            0, // linesOfCodeSuggested
                            0, // linesOfCodeAccepted
                            project, // Pass the project reference
                            "documentation");

                    String markdownResponse = renderMarkdown(documentation);

                    SwingUtilities.invokeLater(() -> {
                        removeWaitingMessage();
                        appendAssistantMessage(markdownResponse);
                        conversationHistory.add(new UserConversation("assistant", documentation));
                        enableInput();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        removeWaitingMessage();
                        appendAssistantMessage("Error generating documentation: " + e.getMessage());
                        enableInput();
                    });
                }
            }).start();
        } else {
            // No code selected: do not append the "Generate Documentation" label, allow
            // input
            enableInput();
        }
    }

    private void sendUserQuery() {
        String userQuery = inputField.getText().trim();
        if (userQuery.isEmpty() || userQuery.equals(PLACEHOLDER_TEXT)) {
            return;
        }

        appendUserMessage(userQuery, false);
        inputField.setText("");
        appendWaitingMessage();
        disableInput();

        // Store the user's query in history
        conversationHistory.add(new UserConversation("user", userQuery));

        new Thread(() -> {
            String prompt = buildPrompt(userQuery);

            DocumentationVertexAi docVertexAi = new DocumentationVertexAi();
            try {
                GenerateContentResponse response = docVertexAi.generateContent(prompt);
                String documentation = DocumentationVertexAi.extractGeneratedDocumentation(response);

                // Report metrics for documentation generation
                UsageMetricsReporter.reportMetricsAsync(
                        UsageMetricsReporter.getUserInfo().email,
                        UsageMetricsReporter.getUserInfo().projectId,
                        0, // linesOfCodeSuggested
                        0, // linesOfCodeAccepted
                        project, // Pass the project reference
                        "documentation");

                // Store assistant response in history
                conversationHistory.add(new UserConversation("assistant", documentation));

                String markdownResponse = renderMarkdown(documentation);

                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendAssistantMessage(markdownResponse);
                    enableInput();
                });
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendAssistantMessage("Error generating documentation: " + e.getMessage());
                    enableInput();
                });
            }
        }).start();
    }

    public class UserConversation {
        public String author;
        public String message;

        public UserConversation(String author, String message) {
            this.author = author;
            this.message = message;
        }
    }

    private String buildPrompt(String latestMessage) {
        long userMessageCount = conversationHistory.stream()
                .filter(c -> c.author.equals("user"))
                .count();

        // First message — include instruction and code context
        if (userMessageCount == 1) {
            String instruction = "Instruction: You are an intelligent assistant helping the user with programming queries. "
                    +
                    "Please refer to the code context to provide relevant and accurate responses.\n\n";
            String context = !lastSelectedCode.isEmpty() ? "Code Context:\n" + lastSelectedCode + "\n\n" : "";
            String userMessage = "User: " + latestMessage + "\nAI:";
            return instruction + context + userMessage;
        }

        // Follow-up: format all conversation history with proper User/AI labels
        StringBuilder promptBuilder = new StringBuilder();
        for (UserConversation uc : conversationHistory) {
            if (uc.author.equals("user")) {
                promptBuilder.append("User: ");
            } else if (uc.author.equals("assistant")) {
                promptBuilder.append("AI: ");
            }
            promptBuilder.append(uc.message).append("\n");
        }

        promptBuilder.append("AI:");
        return promptBuilder.toString();
    }

    private String renderMarkdown(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(parser.parse(markdown));
    }

    private void appendUserMessage(String text, boolean isNewChat) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder message = new StringBuilder("<html><b style='color:white;'>👨‍💻 You</b><br><br>");

            if (isNewChat && !lastSelectedCode.isEmpty()) {
                message.append(
                        "<pre style='color:gray; background:#282828; padding:5px; white-space:pre-wrap; word-wrap:break-word;'>")
                        .append(lastSelectedCode.replace("\n", "<br>"))
                        .append("</pre><br>");
            }

            message.append("<div style='white-space:pre-wrap; word-wrap:break-word;'>")
                    .append(text)
                    .append("</div></html>");

            insertMessagePanel(message.toString(), new Color(60, 60, 60));
        });
    }

    private void appendAssistantMessage(String htmlText) {
        SwingUtilities.invokeLater(() -> {

            // Message box with border
            JPanel messagePanel = new JPanel();
            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(new Color(30, 30, 30));
            messagePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(10, 10, 10, 10),
                    BorderFactory.createLineBorder(new Color(60, 60, 60))));
            messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Split htmlText into text/code parts
            String[] parts = htmlText.split("(?=<pre>)|(?<=</pre>)");
            for (String part : parts) {
                JComponent component;
                if (part.startsWith("<pre>") || part.startsWith("<pre ")) {
                    component = createCodeBlock(part, part);
                } else {
                    component = createTextBlock(part);
                }
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
                messagePanel.add(component);
            }

            // ⬅️ Create container with heading outside the box
            JPanel containerPanel = new JPanel();
            containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
            containerPanel.setBackground(new Color(20, 20, 20)); // Optional: different from messagePanel
            containerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel headerLabel = new JLabel("<html><b style='color: white; font-size:12px;'>ncode</b></html>");
            headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 0)); // spacing below
            headerLabel.setForeground(Color.WHITE);

            containerPanel.add(headerLabel); // Add heading
            containerPanel.add(messagePanel); // Then the box

            StyledDocument doc = chatOutputArea.getStyledDocument();
            int messageStartOffset = doc.getLength(); // ✅ Get offset BEFORE inserting

            try {
                doc.insertString(messageStartOffset, "\n", null);
                chatOutputArea.insertComponent(containerPanel);
                doc.insertString(doc.getLength(), "\n", null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            // Scroll to the beginning of the inserted assistant message
            SwingUtilities.invokeLater(() -> {
                try {
                    Rectangle view = chatOutputArea.modelToView(messageStartOffset);
                    if (view != null) {
                        chatOutputArea.scrollRectToVisible(view); // ✅ Scroll to start
                    }
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });

        });
    }

    class CopyIcon implements Icon {
        private final int width;
        private final int height;

        public CopyIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int offset = 3;
            g2.drawRoundRect(x + offset, y, width - offset, height - offset, 3, 3);
            g2.drawRoundRect(x, y + offset, width - offset, height - offset, 3, 3);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    private JComponent createTextBlock(String html) {
        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setText("<html><div style='color:white; font-size:11px;'>" + html + "</div></html>");
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBorder(null);
        return textPane;
    }

    private JPanel createCodeBlock(String markdown, String plainCode) {
        JPanel blockPanel = new JPanel(new BorderLayout());
        blockPanel.setBackground(new Color(30, 30, 30));
        blockPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true));

        // --- Code display pane ---
        JTextPane codePane = new JTextPane();
        codePane.setEditable(false);
        codePane.setText(stripCodeAndPreTags(markdown));
        codePane.setBackground(new Color(30, 30, 30));
        codePane.setForeground(Color.WHITE);
        codePane.setFont(new Font("Monospaced", Font.PLAIN, 11));
        codePane.setMargin(new Insets(4, 6, 4, 6));
        codePane.setCaretPosition(0);

        // --- Scrollable wrapper ---
        JScrollPane scrollPane = new JScrollPane(codePane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.addMouseWheelListener(e -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            if (bar.isVisible()) {
                int scrollAmount = e.getUnitsToScroll() * bar.getUnitIncrement();
                bar.setValue(bar.getValue() + scrollAmount);
            } else {
                Container parent = scrollPane.getParent();
                while (parent != null) {
                    if (parent instanceof JScrollPane) {
                        ((JScrollPane) parent).dispatchEvent(e);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        });

        // --- TOP BAR COPY BUTTON PANEL ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(30, 30, 30));

        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        iconPanel.setBackground(new Color(30, 30, 30));

        CopyIcon copyIcon = new CopyIcon(14, 14);
        JButton copyButton = new JButton(copyIcon);
        copyButton.setPreferredSize(new Dimension(22, 22));
        copyButton.setFocusable(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setBorderPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("Copy");

        // --- Copy functionality ---
        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(plainCode), null);

            copyButton.setText("✔");
            copyButton.setIcon(null);

            Timer timer = new Timer(1500, evt -> {
                copyButton.setText("");
                copyButton.setIcon(copyIcon);
            });
            timer.setRepeats(false);
            timer.start();
        });

        iconPanel.add(copyButton);
        topPanel.add(iconPanel, BorderLayout.EAST);

        // --- Add components to the block panel ---
        blockPanel.add(topPanel, BorderLayout.NORTH);
        blockPanel.add(scrollPane, BorderLayout.CENTER);

        return blockPanel;
    }

    private String stripCodeAndPreTags(String text) {
        // Removes <pre>, </pre>, <code>, </code>, and <code ...> (with attributes)
        return text.replaceAll("(?i)</?(pre|code)([^>]*)?>", "");
    }

    private void appendWaitingMessage() {
        SwingUtilities.invokeLater(() -> {
            waitingMessageLabel = new JLabel("⏳ Waiting for response...");
            waitingMessageLabel.setForeground(Color.GRAY);
            waitingMessageLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));

            JPanel waitingPanel = new JPanel(new BorderLayout());
            waitingPanel.setBackground(Color.BLACK);
            waitingPanel.add(waitingMessageLabel, BorderLayout.WEST);

            chatOutputArea.insertComponent(waitingPanel);
            // Track the position of the waiting message
            int waitingMessageOffset = chatOutputArea.getDocument().getLength();

            // Scroll to the waiting message position
            SwingUtilities.invokeLater(() -> {
                try {
                    Rectangle view = chatOutputArea.modelToView(waitingMessageOffset); // Get waiting message position
                    if (view != null) {
                        chatOutputArea.scrollRectToVisible(view); // Scroll to that position
                    }
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void removeWaitingMessage() {
        SwingUtilities.invokeLater(() -> {
            if (waitingMessageLabel != null) {
                waitingMessageLabel.setText("");
                waitingMessageLabel.getParent().remove(waitingMessageLabel);
                chatOutputArea.revalidate();
                chatOutputArea.repaint();
            }
        });
    }

    private void disableInput() {
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
    }

    private void enableInput() {
        inputField.setEnabled(true);
        sendButton.setEnabled(true);
    }

    private void insertMessagePanel(String message, Color bgColor) {
        JTextPane messageLabel = new JTextPane();
        messageLabel.setContentType("text/html");
        messageLabel.setText(message);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageLabel.setEditable(false);
        messageLabel.setOpaque(false);
        messageLabel.setBorder(null);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(bgColor);
        messagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        messagePanel.add(messageLabel, BorderLayout.CENTER);

        StyledDocument doc = chatOutputArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "\n", null);
            chatOutputArea.insertComponent(messagePanel);
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            System.err.println("Error inserting message panel.");
        }
        chatOutputArea.setCaretPosition(doc.getLength());
    }

    public JPanel getPanel() {
        return this;
    }
}