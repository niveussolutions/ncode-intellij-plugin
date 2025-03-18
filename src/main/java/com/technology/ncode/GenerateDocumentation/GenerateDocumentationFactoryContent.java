package com.technology.ncode.GenerateDocumentation;

import javax.swing.*;

import com.technology.ncode.VertexAI.VertexAIChatbot;

import java.awt.*;

public class GenerateDocumentationFactoryContent extends JPanel {
    private JTextPane selectedCodeArea;
    private JTextPane chatOutputArea;

    public GenerateDocumentationFactoryContent() {
        setLayout(new BorderLayout(15, 15)); // Enhanced spacing for a refined look
        setBackground(new Color(45, 45, 45)); // Slightly darker background for a sleek UI
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Selected Code Area
        selectedCodeArea = new JTextPane();
        selectedCodeArea.setEditable(false);
        selectedCodeArea.setBackground(new Color(25, 25, 25));
        selectedCodeArea.setForeground(Color.WHITE);
        selectedCodeArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane codeScrollPane = new JScrollPane(selectedCodeArea);
        codeScrollPane.setPreferredSize(new Dimension(450, 100));
        codeScrollPane.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Selected Code"));
        codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(codeScrollPane, BorderLayout.NORTH);

        // Chat Output Area
        chatOutputArea = new JTextPane();
        chatOutputArea.setEditable(false);
        chatOutputArea.setBackground(new Color(35, 35, 35));
        chatOutputArea.setForeground(Color.WHITE);
        chatOutputArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane chatScrollPane = new JScrollPane(chatOutputArea);
        chatScrollPane.setPreferredSize(new Dimension(450, 260));
        chatScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                "Documentation Output"));
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(chatScrollPane, BorderLayout.CENTER);
    }

    public void setSelectedCode(String code) {
        selectedCodeArea.setText(code);
        chatOutputArea.setText("\n\n\u2728 Generating documentation...\n\n");

        // Fetch documentation from Vertex AI
        SwingUtilities.invokeLater(() -> {
            String documentation = VertexAIChatbot.getVertexAIResponse("Generate documentation for: \n" + code);
            chatOutputArea.setText(documentation);
            selectedCodeArea.setCaretPosition(0);
            chatOutputArea.setCaretPosition(0);
        });
    }

    public JPanel getPanel() {
        return this;
    }
}
