package com.technology.ncode.GenerateTestCases;

import javax.swing.*;
import javax.swing.border.*;

import com.technology.ncode.VertexAI.TestCaseCodeVertexAi; // Import new VertexAI class
import com.technology.ncode.VertexAI.VertexAIChatbot; // Import old VertexAI class

import java.awt.*;
import java.io.IOException;

public class GenerateTestCaseFactoryContent extends JPanel {
    private JTextPane selectedCodeArea;
    private JTextPane chatOutputArea;

    public GenerateTestCaseFactoryContent() {
        setLayout(new BorderLayout(15, 15)); // Enhanced spacing for refined look
        setBackground(new Color(45, 45, 45)); // Sleek dark background
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Selected Code Area
        selectedCodeArea = createStyledTextPane();
        JScrollPane codeScrollPane = createStyledScrollPane(selectedCodeArea, "Selected Code", 450, 80);
        add(codeScrollPane, BorderLayout.NORTH);

        // Chat Output Area
        chatOutputArea = createStyledTextPane();
        JScrollPane chatScrollPane = createStyledScrollPane(chatOutputArea, "Generated Test Cases", 450, 250);
        add(chatScrollPane, BorderLayout.CENTER);
    }

    private JTextPane createStyledTextPane() {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(new Color(25, 25, 25)); // Darker shade for contrast
        textPane.setForeground(Color.WHITE);
        textPane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textPane.setCaretPosition(0);
        return textPane;
    }

    private JScrollPane createStyledScrollPane(JTextPane textPane, String title, int width, int height) {
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                new CompoundBorder(
                        new LineBorder(Color.LIGHT_GRAY, 1, true), // Smooth border
                        new EmptyBorder(5, 5, 5, 5) // Padding inside the box
                ), title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), Color.WHITE));
        scrollPane.setPreferredSize(new Dimension(width, height));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    public void setSelectedCode(String code) {
        SwingUtilities.invokeLater(() -> {
            selectedCodeArea.setText(code != null ? code : ""); // Handle null input
            selectedCodeArea.setCaretPosition(0);
        });

        // Immediately set the waiting message
        chatOutputArea.setText("Fetching test cases, please wait...");
        chatOutputArea.setForeground(Color.GRAY);
        chatOutputArea.setFont(new Font("Monospaced", Font.ITALIC, 14));

        if (code == null || code.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> chatOutputArea.setText("No code provided."));
            return;
        }

        // Fetch test case output dynamically in a separate thread
        new Thread(() -> {
            String testCaseOutput = VertexAIChatbot.getVertexAIResponse(
                    "Generate 5 structured test cases for the following Java code:\n" + code
                            + "\n\nFormat the response as:\n"
                            + "Generated Test Cases:\n"
                            + "\nTest Case 1: <Title>\n"
                            + "- Input: <Describe input>\n"
                            + "- Expected Output:\n"
                            + "  <List each output line>\n"
                            + "\nTest Case 2: <Title>\n"
                            + "- Input: <Describe input>\n"
                            + "- Expected Output:\n"
                            + "  <List each output line>\n"
                            + "\nTest Case 3: <Title>\n"
                            + "- Input: <Describe input>\n"
                            + "- Expected Output:\n"
                            + "  <List each output line>\n"
                            + "\nTest Case 4: <Title>\n"
                            + "- Input: <Describe input>\n"
                            + "- Expected Output:\n"
                            + "  <List each output line>\n"
                            + "\nTest Case 5: <Title>\n"
                            + "- Input: <Describe input>\n"
                            + "- Expected Output:\n"
                            + "  <List each output line>");

            SwingUtilities.invokeLater(() -> {
                chatOutputArea.setText(testCaseOutput);
                chatOutputArea.setForeground(Color.WHITE);
                chatOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
                chatOutputArea.setCaretPosition(0);
            });
        }).start();
    }

    public JPanel getPanel() {
        return this;
    }
}