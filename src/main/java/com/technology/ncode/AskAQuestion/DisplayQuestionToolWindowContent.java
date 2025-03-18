package com.technology.ncode.AskAQuestion;

import javax.swing.*;
import javax.swing.text.*;

import com.technology.ncode.VertexAI.VertexAIChatbot;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class DisplayQuestionToolWindowContent extends JPanel {
    private JTextPane selectedCodeArea;
    private JTextPane chatOutputArea;
    private JTextField askQuestionField;
    private JButton sendButton;
    private ExecutorService executorService;
    private int waitingMessageOffset = -1; // Track waiting message position

    public DisplayQuestionToolWindowContent() {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 25, 25));
        executorService = Executors.newSingleThreadExecutor();

        // Selected Code Area
        selectedCodeArea = new JTextPane();
        selectedCodeArea.setEditable(false);
        selectedCodeArea.setBackground(new Color(40, 40, 40));
        selectedCodeArea.setForeground(Color.WHITE);
        selectedCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        selectedCodeArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane codeScrollPane = new JScrollPane(selectedCodeArea);
        codeScrollPane.setPreferredSize(new Dimension(400, 90));
        codeScrollPane.setBorder(null);
        add(codeScrollPane, BorderLayout.NORTH);

        // Chat Output Area
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);
        chatOutputArea.setBackground(Color.BLACK);
        chatOutputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane chatScrollPane = new JScrollPane(chatOutputArea);
        chatScrollPane.setPreferredSize(new Dimension(400, 300));
        chatScrollPane.setBorder(null);
        add(chatScrollPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel askPanel = new JPanel(new BorderLayout());
        askPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        askPanel.setBackground(new Color(50, 50, 50));
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.BLACK);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        askQuestionField = new JTextField();
        askQuestionField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        askQuestionField.setForeground(Color.gray);
        askQuestionField.setBackground(Color.BLACK);
        askQuestionField.setText("Ask NCode...");
        askQuestionField.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        askQuestionField.setCaretColor(Color.WHITE);

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
                if (askQuestionField.getText().isEmpty()) {
                    askQuestionField.setText("Ask NCode...");
                    askQuestionField.setForeground(Color.GRAY);
                }
            }
        });

        // Send Button
        sendButton = new JButton("➤");
        sendButton.setEnabled(false);
        sendButton.setForeground(Color.GRAY);
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        sendButton.setBorder(null);
        sendButton.setContentAreaFilled(false);

        askQuestionField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                toggleSendButton();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                toggleSendButton();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                toggleSendButton();
            }
        });

        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(askQuestionField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        askPanel.add(inputPanel, BorderLayout.CENTER);
        add(askPanel, BorderLayout.SOUTH);
    }

    private void toggleSendButton() {
        SwingUtilities.invokeLater(() -> {
            if (askQuestionField.getText().trim().isEmpty() || askQuestionField.getText().equals("Ask NCode...")) {
                sendButton.setEnabled(false);
                sendButton.setForeground(Color.GRAY);
            } else {
                sendButton.setEnabled(true);
                sendButton.setForeground(Color.WHITE);
            }
        });
    }

    private void sendMessage() {
        String questionText = askQuestionField.getText().trim();
        if (questionText.isEmpty() || askQuestionField.getText().equals("Ask NCode..."))
            return;

        String selectedCode = selectedCodeArea.getText().trim();
        if (selectedCode.isEmpty()) {
            appendNCodeMessage("No code selected to analyze.");
            appendSeparatorLine();
            return;
        }

        appendUserMessage(questionText);
        appendSpacer();
        appendWaitingMessage(); // Display waiting message

        // Disable send button while processing
        sendButton.setEnabled(false);
        sendButton.setForeground(Color.GRAY);
        askQuestionField.setEnabled(false);

        executorService.execute(() -> {
            String response = VertexAIChatbot.getVertexAIResponse(selectedCode + "\n\n" + "Question: " + questionText);
            SwingUtilities.invokeLater(() -> {
                removeWaitingMessage();
                appendNCodeMessage(response);
                appendSeparatorLine();

                // Re-enable send button after response is received
                sendButton.setEnabled(true);
                sendButton.setForeground(Color.WHITE);
                askQuestionField.setEnabled(true);
            });
        });

        SwingUtilities.invokeLater(() -> {
            askQuestionField.setText("Ask NCode...");
            toggleSendButton();
        });
    }

    private void appendUserMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();

            // Styling for user message panel
            JPanel userMessagePanel = new JPanel(new BorderLayout());
            userMessagePanel.setBackground(new Color(60, 60, 60));
            userMessagePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
                    BorderFactory.createEmptyBorder(12, 15, 12, 15)));

            // Adding small gap between "You:" and the question
            JLabel messageLabel = new JLabel("<html><b style='color:white;'>👨‍💻 You</b><br><br>" + text + "</html>");
            messageLabel.setForeground(Color.WHITE);
            messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

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
                doc.insertString(doc.getLength(), "──────────────────────────────────────────────────────────\n\n",
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
            selectedCodeArea.setText(code);
            chatOutputArea.setText("");
        });
    }

    private void appendNCodeMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatOutputArea.getStyledDocument();

            // Style for "NCode:" (Blue)
            SimpleAttributeSet ncodeLabelStyle = new SimpleAttributeSet();
            StyleConstants.setFontFamily(ncodeLabelStyle, "SansSerif");
            StyleConstants.setFontSize(ncodeLabelStyle, 14);
            StyleConstants.setForeground(ncodeLabelStyle, Color.BLUE);
            StyleConstants.setBold(ncodeLabelStyle, true);

            // Style for message text (Dim White)
            SimpleAttributeSet ncodeMessageStyle = new SimpleAttributeSet();
            StyleConstants.setFontFamily(ncodeMessageStyle, "SansSerif");
            StyleConstants.setFontSize(ncodeMessageStyle, 14);
            StyleConstants.setForeground(ncodeMessageStyle, new Color(180, 180, 180));

            try {
                doc.insertString(doc.getLength(), "\uD83E\uDD16 NCode: \n\n", ncodeLabelStyle); // "NCode:" in Blue
                doc.insertString(doc.getLength(), text + "\n\n", ncodeMessageStyle); // Message in Dim White
            } catch (BadLocationException e) {
                System.err.println("Error inserting NCode message.");
            }
            chatOutputArea.setCaretPosition(doc.getLength());
        });
    }
}
