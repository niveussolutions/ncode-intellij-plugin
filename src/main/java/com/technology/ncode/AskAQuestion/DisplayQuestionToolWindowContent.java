package com.technology.ncode.AskAQuestion;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout; // Remove the old import
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.technology.ncode.VertexAI.AskAQuestionVertexAi;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class DisplayQuestionToolWindowContent extends JPanel {
    private JTextPane selectedCodeArea;
    private JTextPane chatOutputArea;
    private JTextField askQuestionField;
    private JButton sendButton;
    private ExecutorService executorService;
    private int waitingMessageOffset = -1; // Track waiting message position
    private AskAQuestionVertexAi askAQuestionVertexAi; // Add VertexAI instance
    private final List<UserConversation> conversationHistory = new ArrayList<>();
    private JPanel chatPanel; // Wraps the chat output area
    private JScrollPane scrollPane;

    public DisplayQuestionToolWindowContent() {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));
        executorService = Executors.newSingleThreadExecutor();
        askAQuestionVertexAi = new AskAQuestionVertexAi(); // Initialize

        // Chat Output Area
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);
        chatOutputArea.setBackground(Color.BLACK);
        chatOutputArea.setForeground(Color.WHITE);
        chatOutputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatOutputArea.setContentType("text/html"); // Enables proper word wrapping
        chatOutputArea.setEditorKit(new HTMLEditorKit()); // Ensures correct text wrapping

        // Wrap JTextPane inside a JScrollPane
        JScrollPane chatScrollPane = new JScrollPane(chatOutputArea);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Prevent horizontal
                                                                                             // scrolling

        // Ensure JTextPane resizes dynamically within the window
        chatOutputArea.setPreferredSize(new Dimension(400, 300));
        chatScrollPane.setPreferredSize(new Dimension(400, 300));
        chatScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        chatOutputArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Add component to layout
        // add(chatScrollPane, BorderLayout.CENTER);
        chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(Color.BLACK);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        add(chatPanel, BorderLayout.CENTER);

        JPanel askPanel = new JPanel(new BorderLayout());
        askPanel.setBackground(new Color(50, 50, 50));

        // Input Field Setup
        askQuestionField = new JTextField("Ask NCode...");
        askQuestionField.setCaretColor(Color.WHITE);
        askQuestionField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        askQuestionField.setBackground(Color.BLACK);
        askQuestionField.setForeground(Color.GRAY);
        askQuestionField.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        askQuestionField.setPreferredSize(new Dimension(800, 50));

        // Placeholder Logic for Input Field
        askQuestionField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (askQuestionField.getText().equals("Ask NCode...")) {
                    askQuestionField.setText("");
                    askQuestionField.setForeground(Color.WHITE);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (askQuestionField.getText().trim().isEmpty()) {
                    askQuestionField.setText("Ask NCode...");
                    askQuestionField.setForeground(Color.GRAY);
                }
            }
        });
        askQuestionField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
        askQuestionField.getActionMap().put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Send Button
        sendButton = new JButton("➤");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        sendButton.setForeground(Color.WHITE);
        sendButton.setContentAreaFilled(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.setBackground(new Color(50, 50, 50));
        sendButton.setOpaque(true);
        sendButton.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        // Add ActionListener to send button
        sendButton.addActionListener(e -> sendMessage()); // Ensure this function is defined

        // Dropdown Model Selection
        String[] options = { "gemini-1.5-flash-002", "claude 3.5 Sonnet", "claude 3.7 Sonnet" };
        JComboBox<String> modelDropdown = new JComboBox<>(options);
        modelDropdown.setBackground(Color.DARK_GRAY);
        modelDropdown.setForeground(Color.WHITE);
        modelDropdown.setFont(new Font("Times New Roman", Font.PLAIN, 14));
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
        });

        // Send Button Container (aligned to right)
        JPanel sendButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        sendButtonContainer.setBackground(new Color(50, 50, 50));
        sendButtonContainer.add(sendButton);

        // Main container for dropdown and send button
        JPanel sendButtonAndDropdownContainer = new JPanel(new BorderLayout());
        sendButtonAndDropdownContainer.setBackground(new Color(50, 50, 50));
        sendButtonAndDropdownContainer.add(modelDropdown, BorderLayout.WEST);
        sendButtonAndDropdownContainer.add(sendButtonContainer, BorderLayout.EAST);

        // Input field container
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBackground(Color.BLACK);
        inputContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        inputContainer.add(askQuestionField, BorderLayout.CENTER);

        // Main container for input, dropdown, and send button
        JPanel inputPanelContainer = new JPanel(new BorderLayout());
        inputPanelContainer.setBackground(Color.BLACK);
        inputPanelContainer.add(inputContainer, BorderLayout.NORTH);
        inputPanelContainer.add(sendButtonAndDropdownContainer, BorderLayout.SOUTH);

        // Add input panel to askPanel
        askPanel.add(inputPanelContainer, BorderLayout.CENTER);

        // Add to main layout
        add(askPanel, BorderLayout.SOUTH);

    }

    public void clearChatHistory() {
        chatOutputArea.setText(""); // Clear chat visually
        conversationHistory.clear(); // Clear internal memory
    }

    private boolean isFirstQuestion = true;
    private String lastSelectedCode = "";

    private String previousSelectedCode = "";

    private void appendUserMessage(String text, boolean isNewChat) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();
            StringBuilder message = new StringBuilder("<html><b style='color:white;'>👨‍💻 You</b><br><br>");

            // Reset first question flag if the selected code changes
            if (!lastSelectedCode.isEmpty() && !lastSelectedCode.equals(previousSelectedCode)) {
                isFirstQuestion = true;
                previousSelectedCode = lastSelectedCode; // Store the last selected code
            }

            // Ensure first question flag is set for a new chat
            if (isNewChat) {
                isFirstQuestion = true;
            }

            // Show selected code only for the first message of a new chat or when code is
            // changed
            if (isFirstQuestion && !lastSelectedCode.isEmpty()) {
                message.append(
                        "<pre style='color:gray; background:#282828; padding:5px; white-space:pre-wrap; word-wrap:break-word; max-width: 400px;'>")
                        .append(lastSelectedCode.replace("\n", "<br>"))
                        .append("</pre><br>");
                isFirstQuestion = false; // Prevent showing again in the same chat
            }

            message.append("<div style='white-space:pre-wrap; word-wrap:break-word; max-width: 400px;'>")
                    .append(text)
                    .append("</div></html>");

            JTextPane messageLabel = new JTextPane();
            messageLabel.setContentType("text/html");
            messageLabel.setText(message.toString());
            messageLabel.setForeground(Color.WHITE);
            messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            messageLabel.setEditable(false);
            messageLabel.setOpaque(false);
            messageLabel.setBorder(null);

            JPanel userMessagePanel = new JPanel(new BorderLayout());
            userMessagePanel.setBackground(new Color(60, 60, 60));
            userMessagePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));
            userMessagePanel.add(messageLabel, BorderLayout.CENTER);

            try {
                doc.insertString(doc.getLength(), "\n", null);
                chatOutputArea.insertComponent(userMessagePanel);
                doc.insertString(doc.getLength(), "\n", null);
            } catch (BadLocationException e) {
                System.err.println("Error inserting user message.");
            }

            chatOutputArea.setCaretPosition(doc.getLength());
        });
    }

    private void appendWaitingMessage() {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();
            SimpleAttributeSet waitingStyle = new SimpleAttributeSet();
            StyleConstants.setFontFamily(waitingStyle, "SansSerif");
            StyleConstants.setFontSize(waitingStyle, 14);
            StyleConstants.setForeground(waitingStyle, Color.GRAY);
            StyleConstants.setItalic(waitingStyle, true);

            try {
                waitingMessageOffset = doc.getLength();
                doc.insertString(doc.getLength(), "⏳ Thinking... brewing the best answer for you!\n\n", waitingStyle);
            } catch (BadLocationException e) {
                System.err.println("Error inserting waiting message.");
            }
            chatOutputArea.setCaretPosition(doc.getLength());
        });
    }

    private void removeWaitingMessage() {
        SwingUtilities.invokeLater(() -> {
            if (waitingMessageOffset != -1) {
                try {
                    StyledDocument doc = chatOutputArea.getStyledDocument();
                    doc.remove(waitingMessageOffset, doc.getLength() - waitingMessageOffset);
                } catch (BadLocationException e) {
                    System.err.println("Error removing waiting message.");
                }
                waitingMessageOffset = -1;
            }
        });
    }

    private void appendSeparatorLine() {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), "────────────────────────────────────────────────────────\n\n",
                        null);
            } catch (BadLocationException e) {
                System.err.println("Error inserting separator line.");
            }
        });
    }

    public JPanel getPanel() {
        return this;
    }

    private void appendSpacer() {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), "\n", null);
            } catch (BadLocationException e) {
                System.err.println("Error inserting spacer.");
            }
        });
    }

    public void setSelectedCode(String code) {
        SwingUtilities.invokeLater(() -> {
            if (code == null || code.trim().isEmpty())
                return;

            // Update only the latest selected code for prompt
            lastSelectedCode = code;

            System.out.println("[Selected Code]");
            System.out.println(lastSelectedCode);

            // Just show the new question in chat UI (no previous code)
            StyledDocument doc = chatOutputArea.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), "", null);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }

            // Send the new message using the latest selection only
            sendMessageToAPI("Explain?");
        });
    }

    private void sendMessageToAPI(String message) {
        askQuestionField.setText("Ask NCode...");
        askQuestionField.setForeground(Color.GRAY);
        askQuestionField.setEnabled(false);

        if (message.isEmpty() || message.equals("Ask NCode..."))
            return;

        boolean isNewChat = chatOutputArea.getText().trim().isEmpty();
        appendUserMessage(message, isNewChat);
        appendSpacer();
        appendWaitingMessage();

        sendButton.setEnabled(false);
        sendButton.setForeground(Color.GRAY);
        askQuestionField.setEnabled(false);

        boolean isAutoExplain = message.equals("Explain?");

        executorService.execute(() -> {
            try {
                System.out.println("[User Query]");
                System.out.println(message);

                String prompt;
                if (isAutoExplain) {
                    prompt = "Please explain the following code:\n\n" + lastSelectedCode;
                } else {
                    conversationHistory.add(new UserConversation("user", message));
                    prompt = buildPrompt();
                }

                System.out.println("[Prompt Sent to VertexAI]");
                System.out.println(prompt);

                String response = AskAQuestionVertexAi
                        .extractGeneratedText(askAQuestionVertexAi.generateContent(prompt));

                System.out.println("[Response from VertexAI]");
                System.out.println(response);

                if (!isAutoExplain) {
                    conversationHistory.add(new UserConversation("assistant", response));
                }

                String finalResponse = (response != null && !response.trim().isEmpty())
                        ? response
                        : "⚠ No response received.";

                SwingUtilities.invokeLater(() -> {
                    try {
                        int currentScrollValue = 0;
                        JScrollBar verticalScrollBar = null;
                        if (scrollPane != null) {
                            verticalScrollBar = scrollPane.getVerticalScrollBar();
                            currentScrollValue = verticalScrollBar.getValue();
                        }

                        removeWaitingMessage();
                        appendNCodeMessage(finalResponse);
                        appendSeparatorLine();

                        if (verticalScrollBar != null) {
                            verticalScrollBar.setValue(currentScrollValue);
                        }

                        sendButton.setEnabled(true);
                        sendButton.setForeground(Color.WHITE);
                        askQuestionField.setEnabled(true);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendNCodeMessage("⚠ Error: Unable to retrieve response. Please try again.");
                    appendSeparatorLine();

                    sendButton.setEnabled(true);
                    sendButton.setForeground(Color.WHITE);
                    askQuestionField.setEnabled(true);
                });
                e.printStackTrace();
            }
        });
    }

    // Example init method where scrollPane is created:
    public JPanel createContent() {
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);

        scrollPane = new JScrollPane(chatOutputArea); // ✅ assign to field

        askQuestionField = new JTextField("Ask NCode...");
        sendButton = new JButton("Send");

        // setup listeners, layout etc.

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;

        // return scrollPane; // or panel containing all components
    }

    private String buildPrompt() {
        Map<String, String> userTypes = Map.of(
                "user", "User",
                "assistant", "AI");

        String context = !lastSelectedCode.isEmpty()
                ? "Code Context:\n" + lastSelectedCode + "\n\n"
                : "";

        StringBuilder conversation = new StringBuilder();
        for (UserConversation uc : conversationHistory) {
            conversation.append(userTypes.getOrDefault(uc.author, uc.author))
                    .append(": ")
                    .append(uc.message)
                    .append("\n\n");
        }

        // Add "AI:" at the end to prompt the model to reply
        conversation.append("AI:");

        return "Instruction: You are an intelligent assistant helping the user with programming queries. " +
                "Please refer to the previous conversation history and code context to provide relevant and accurate responses.\n\n"
                +
                context + conversation.toString().trim();
    }

    // Helper class for conversation history
    private static class UserConversation {
        String author;
        String message;

        public UserConversation(String author, String message) {
            this.author = author;
            this.message = message;
        }
    }

    private void sendMessage() {
        String questionText = askQuestionField.getText().trim();
        askQuestionField.setText("Ask NCode..."); // Placeholder text
        askQuestionField.setForeground(Color.GRAY);
        askQuestionField.setEnabled(false);

        if (questionText.isEmpty() || questionText.equals("Ask NCode..."))
            return;

        boolean isNewChat = (chatOutputArea.getText().trim().isEmpty());
        appendUserMessage(questionText, isNewChat);
        appendSpacer();
        appendWaitingMessage();

        sendButton.setEnabled(false);
        sendButton.setForeground(Color.GRAY);
        askQuestionField.setEnabled(false);

        executorService.execute(() -> {
            try {
                System.out.println("[User Query]");
                System.out.println(questionText);
                conversationHistory.add(new UserConversation("user", questionText));

                String prompt = buildPrompt();
                System.out.println("[Prompt Sent to VertexAI]");
                System.out.println(prompt);

                // Always include conversation history
                String response = AskAQuestionVertexAi
                        .extractGeneratedText(askAQuestionVertexAi.generateContent(prompt));
                System.out.println("[Response from VertexAI]");
                System.out.println(response);

                conversationHistory.add(new UserConversation("assistant", response));

                String finalResponse = (response != null && !response.trim().isEmpty())
                        ? response
                        : "⚠ No response received. Please try again.";

                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendNCodeMessage(finalResponse);
                    appendSeparatorLine();

                    sendButton.setEnabled(true);
                    sendButton.setForeground(Color.WHITE);
                    askQuestionField.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    removeWaitingMessage();
                    appendNCodeMessage("⚠ Error: Unable to retrieve response. Please try again.");
                    appendSeparatorLine();

                    sendButton.setEnabled(true);
                    sendButton.setForeground(Color.WHITE);
                    askQuestionField.setEnabled(true);
                });
                e.printStackTrace();
            }
        });
    }

    public void appendNCodeMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatOutputArea);
            if (scrollPane == null)
                return;

            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            int currentScrollValue = verticalScrollBar.getValue(); // Lock scroll

            StyledDocument doc = chatOutputArea.getStyledDocument();
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            Node document = parser.parse(text);

            try {
                HTMLEditorKit editorKit = (HTMLEditorKit) chatOutputArea.getEditorKit();
                String ncodeLabel = "<b><span style='font-size:16px;'>ncode:</span></b> ";
                editorKit.insertHTML((HTMLDocument) doc, doc.getLength(), ncodeLabel, 0, 0, null);

                for (Node node : document.getChildren()) {
                    if (node instanceof FencedCodeBlock) {
                        FencedCodeBlock codeBlock = (FencedCodeBlock) node;
                        insertCodeBlock(codeBlock.getContentChars().toString());
                    } else if (node instanceof OrderedList || node instanceof BulletList) {
                        for (Node listItem : node.getChildren()) {
                            if (listItem instanceof ListItem) {
                                String listItemText = renderer.render(listItem);
                                editorKit.insertHTML((HTMLDocument) doc, doc.getLength(), listItemText, 0, 0, null);
                            }
                        }
                    } else {
                        String htmlText = renderer.render(node);
                        editorKit.insertHTML((HTMLDocument) doc, doc.getLength(), htmlText, 0, 0, null);
                    }
                }

                // Double-delay scroll lock to counter insertComponent() jump
                SwingUtilities.invokeLater(
                        () -> SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(currentScrollValue)));

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Prevent caret from jumping to bottom
            // chatOutputArea.setCaretPosition(doc.getLength());
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

    JLabel waitingLabel; // defined earlier and added to chatOutputArea

    private void insertCodeBlock(String code) {
        JPanel codeBox = new JPanel(new BorderLayout());
        codeBox.setBackground(new Color(40, 40, 40));
        codeBox.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true));

        JTextPane codePane = new JTextPane();
        codePane.setEditable(false);
        codePane.setText(code);
        codePane.setBackground(new Color(40, 40, 40));
        codePane.setForeground(Color.WHITE);
        codePane.setFont(new Font("Monospaced", Font.PLAIN, 11));
        codePane.setMargin(new Insets(2, 4, 2, 2));
        codePane.setCaretPosition(0);

        JScrollPane codeScrollPane = new JScrollPane(codePane);
        codeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.getVerticalScrollBar().setUnitIncrement(16);

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

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(40, 40, 40));

        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        iconPanel.setBackground(new Color(40, 40, 40));

        DisplayQuestionToolWindowContent.CopyIcon copyIcon = new DisplayQuestionToolWindowContent.CopyIcon(14, 14);
        JButton copyButton = new JButton(copyIcon);
        copyButton.setPreferredSize(new Dimension(22, 22));
        copyButton.setFocusable(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setBorderPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("Copy");

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

        codeBox.add(topPanel, BorderLayout.NORTH);
        codeBox.add(codeScrollPane, BorderLayout.CENTER);

        StyledDocument chatDoc = chatOutputArea.getStyledDocument();
        try {
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
            chatOutputArea.insertComponent(codeBox);
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Scroll to waiting label instead of bottom
        if (waitingLabel != null) {
            SwingUtilities.invokeLater(() -> {
                Rectangle bounds = waitingLabel.getBounds();
                chatOutputArea.scrollRectToVisible(new Rectangle(0, bounds.y, 1, bounds.height));
            });
        }
    }

}