package com.technology.ncode.GenerateTestCases;

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
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.intellij.openapi.project.Project;
import com.technology.ncode.UsageMetricsReporter;
import com.technology.ncode.VertexAI.TestCaseCodeVertexAi;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class GenerateTestCaseFactoryContent extends JPanel {
    private JTextPane chatOutputArea;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel waitingMessageLabel;
    private String lastSelectedCode = "";
    private static final String PLACEHOLDER_TEXT = "Ask NCode...";
    private List<UserConversation> conversationHistory = new ArrayList<>();
    private final Project project;

    public GenerateTestCaseFactoryContent(Project project) {
        this.project = project;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // Chat Output Area
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);
        chatOutputArea.setBackground(Color.BLACK);
        chatOutputArea.setForeground(Color.WHITE);
        chatOutputArea.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JScrollPane outputScrollPane = new JScrollPane(chatOutputArea);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder());
        outputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        outputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(outputScrollPane, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.BLACK);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input Field Setup
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

    private void insertMessagePanel(String message, Color bgColor) {
        JTextPane messageLabel = new JTextPane();
        messageLabel.setContentType("text/html");

        messageLabel.setText(message); // Set the formatted message
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

    private static class UserConversation {
        String author;
        String message;

        public UserConversation(String author, String message) {
            this.author = author;
            this.message = message;
        }
    }

    private String buildFullPrompt(String latestMessage) {
        Map<String, String> userTypes = Map.of(
                "user", "User",
                "assistant", "AI");

        String context = !lastSelectedCode.isEmpty() ? "Code Context: " + lastSelectedCode + "\n\n" : "";

        String conversation = conversationHistory.stream()
                .map(obj -> userTypes.get(obj.author) + ": " + obj.message)
                .collect(Collectors.joining("\n"));

        return "Instruction: You are an intelligent assistant generating unit test cases. " +
                "Ensure that all function calls to external dependencies or complex objects are properly mocked. " +
                "Also, provide clear explanations where necessary.\n\n" +
                context + conversation + "\n\nAI:";
    }

    public void setSelectedCode(String code) {
        lastSelectedCode = code;
        conversationHistory.clear(); // Start fresh

        if (code != null && !code.isEmpty()) {
            // If code is selected, auto-send unit test prompt
            appendUserMessage("Generate Test Case", true);
            appendWaitingMessage();
            disableInput();

            String userPrompt = "Write unit test cases and mock all function calls properly. Ensure that no external dependencies are directly used.";

            // ✅ Store user message
            conversationHistory.add(new UserConversation("user", userPrompt));

            new Thread(() -> {
                String prompt = buildFullPrompt(userPrompt);

                try {
                    TestCaseCodeVertexAi vertexAi = new TestCaseCodeVertexAi();
                    String testCase = vertexAi.generateContent(prompt);

                    // Report metrics for test case generation
                    UsageMetricsReporter.reportMetricsAsync(
                            UsageMetricsReporter.getUserInfo().email,
                            UsageMetricsReporter.getUserInfo().projectId,
                            0, // linesOfCodeSuggested
                            0, // linesOfCodeAccepted
                            project, // Pass the project reference
                            "testcase");

                    // ✅ Store AI response
                    conversationHistory.add(new UserConversation("assistant", testCase));

                    String markdownResponse = renderMarkdown(testCase);

                    SwingUtilities.invokeLater(() -> {
                        removeWaitingMessage();
                        appendAssistantMessage(markdownResponse);
                        enableInput();
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        removeWaitingMessage();
                        appendAssistantMessage("Error generating test cases: " + e.getMessage());
                        enableInput();
                    });
                }
            }).start();
        } else {
            // If no code is selected, just inform and allow manual input
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

        // ✅ Store user message before sending
        conversationHistory.add(new UserConversation("user", userQuery));

        new Thread(() -> {
            String prompt = buildFullPrompt(userQuery);

            try {
                TestCaseCodeVertexAi vertexAi = new TestCaseCodeVertexAi();
                String response = vertexAi.generateContent(prompt);

                // Report metrics for test case generation
                UsageMetricsReporter.reportMetricsAsync(
                        UsageMetricsReporter.getUserInfo().email,
                        UsageMetricsReporter.getUserInfo().projectId,
                        0, // linesOfCodeSuggested
                        0, // linesOfCodeAccepted
                        project, // Pass the project reference
                        "testcase");

                // ✅ Store assistant response
                conversationHistory.add(new UserConversation("assistant", response));

                String markdownResponse = renderMarkdown(response);

                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendAssistantMessage(markdownResponse);
                    enableInput();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendAssistantMessage("Error generating test cases: " + e.getMessage());
                    enableInput();
                });
            }
        }).start();
    }

    public void clearChatHistory() {
        chatOutputArea.setText(""); // Clear chat visually
        conversationHistory.clear(); // Clear internal memory
    }

    public JPanel getPanel() {
        return this;
    }

    private void appendUserMessage(String text, boolean isNewChat) {
        SwingUtilities.invokeLater(() -> {
            // Create the message container
            StringBuilder message = new StringBuilder("<html><b style='color:white;'>👨‍💻 You</b><br><br>");

            // Append previously selected code if it's a new chat and lastSelectedCode
            // exists
            if (isNewChat && !lastSelectedCode.isEmpty()) {
                message.append(
                        "<pre style='color:gray; background:#282828; padding:10px; font-family:Courier New; font-size:12px; white-space:pre-wrap; word-wrap:break-word;'>")
                        .append(lastSelectedCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")) // Escape
                                                                                                                  // HTML
                                                                                                                  // characters
                        .append("</pre><br>");
            }

            // Append the new selected code along with the new user message
            message.append("<div style='white-space:pre-wrap; word-wrap:break-word;'>")
                    .append(text)
                    .append("</div></html>");

            // Insert the combined message into the display window
            insertMessagePanel(message.toString(), new Color(60, 60, 60));
        });
    }

    private void appendAssistantMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();

            // Track position of the waiting message (before assistant's message)
            int waitingMessageOffset = doc.getLength();

            // Styles
            SimpleAttributeSet assistantStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(assistantStyle, Color.white);
            StyleConstants.setFontSize(assistantStyle, 16);
            StyleConstants.setBold(assistantStyle, true);

            SimpleAttributeSet titleStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(titleStyle, Color.WHITE);
            StyleConstants.setFontSize(titleStyle, 14);
            StyleConstants.setBold(titleStyle, true);

            SimpleAttributeSet responseStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(responseStyle, Color.WHITE);
            StyleConstants.setFontSize(responseStyle, 13);

            // Clean the text
            final String cleanedText = text.replaceAll("<pre><code.*?>", "```")
                    .replaceAll("</code></pre>", "```")
                    .replaceAll("<code>", "")
                    .replaceAll("</code>", "")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("<[^>]*>", "");

            try {
                // Insert Assistant Name
                doc.insertString(doc.getLength(), "\nncode\n", assistantStyle);

                String[] parts = cleanedText.split("```");
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (i % 2 == 1) {
                        insertCodeBlock(part); // Insert code block
                    } else {
                        String[] lines = part.split("\n");
                        for (String line : lines) {
                            if (line.trim().equalsIgnoreCase("Unit Test Cases:") ||
                                    line.trim().equalsIgnoreCase("Mocking Function Calls:")) {
                                doc.insertString(doc.getLength(), "\n" + line + "\n\n", titleStyle); // Title-style
                            } else {
                                doc.insertString(doc.getLength(), line + "\n", responseStyle); // Normal text
                            }
                        }
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            // Scroll back to the waiting message position (not bottom)
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

    private void insertCodeBlock(String code) {
        // Outer panel for the code block
        JPanel codeBox = new JPanel(new BorderLayout());
        codeBox.setBackground(new Color(40, 40, 40));
        codeBox.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true));

        // Code display pane
        JTextPane codePane = new JTextPane();
        codePane.setEditable(false);
        codePane.setText(code);
        codePane.setBackground(new Color(40, 40, 40));
        codePane.setForeground(Color.WHITE);
        codePane.setFont(new Font("Monospaced", Font.PLAIN, 11));
        codePane.setMargin(new Insets(2, 4, 2, 2)); // Top, Left, Bottom, Right
        codePane.setCaretPosition(0);

        // Scrollable wrapper for the code
        JScrollPane codeScrollPane = new JScrollPane(codePane);
        codeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Smooth scrolling for touchpads
        codeScrollPane.addMouseWheelListener(e -> {
            JScrollBar bar = codeScrollPane.getVerticalScrollBar();
            if (bar.isVisible()) {
                int scrollAmount = e.getUnitsToScroll() * bar.getUnitIncrement();
                bar.setValue(bar.getValue() + scrollAmount);
            } else {
                Container parent = codeScrollPane.getParent();
                while (parent != null) {
                    if (parent instanceof JScrollPane) {
                        ((JScrollPane) parent).dispatchEvent(e);
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        });

        // Top bar for the copy button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(40, 40, 40));

        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        iconPanel.setBackground(new Color(40, 40, 40));

        CopyIcon copyIcon = new CopyIcon(14, 14);
        JButton copyButton = new JButton(copyIcon);
        copyButton.setPreferredSize(new Dimension(22, 22));
        copyButton.setFocusable(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setBorderPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("Copy");

        // Copy functionality
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(codePane.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

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

        // Combine all parts
        codeBox.add(topPanel, BorderLayout.NORTH);
        codeBox.add(codeScrollPane, BorderLayout.CENTER);

        // Insert into the chat output
        StyledDocument chatDoc = chatOutputArea.getStyledDocument();
        try {
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
            chatOutputArea.insertComponent(codeBox);
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
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

    private String renderMarkdown(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(parser.parse(markdown));
    }
}