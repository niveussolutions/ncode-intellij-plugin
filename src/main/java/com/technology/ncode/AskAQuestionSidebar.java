package com.technology.ncode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AskAQuestionSidebar {
    public JPanel createSidebar() {
        // Create the main container for the sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout(10, 10));
        sidebar.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create and add the header
        JLabel header = new JLabel("Ask a Question", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 16));
        sidebar.add(header, BorderLayout.NORTH);

        // Create the question section
        JPanel questionPanel = new JPanel();
        questionPanel.setLayout(new BorderLayout(5, 5));
        questionPanel.setBorder(BorderFactory.createTitledBorder("Question"));
        JLabel questionLabel = new JLabel("Your Question:");
        JTextArea questionArea = new JTextArea("What is AI?");
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionPanel.add(questionLabel, BorderLayout.NORTH);
        questionPanel.add(new JScrollPane(questionArea), BorderLayout.CENTER);

        // Create the response section
        JPanel responsePanel = new JPanel();
        responsePanel.setLayout(new BorderLayout(5, 5));
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
        JLabel responseLabel = new JLabel("LLM Response:");
        JTextArea responseArea = new JTextArea("Artificial Intelligence is the simulation of human intelligence by machines.");
        responseArea.setEditable(false);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responsePanel.add(responseLabel, BorderLayout.NORTH);
        responsePanel.add(new JScrollPane(responseArea), BorderLayout.CENTER);

        // Add the question and response sections to the sidebar
        sidebar.add(questionPanel, BorderLayout.CENTER);
        sidebar.add(responsePanel, BorderLayout.SOUTH);

        return sidebar;
    }
}
