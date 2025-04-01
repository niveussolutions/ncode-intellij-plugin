package com.technology.ncode.GenerateTestCases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTestCaseFactoryContentTest {

    private GenerateTestCaseFactoryContent factoryContent;

    @BeforeEach
    void setUp() {
        factoryContent = new GenerateTestCaseFactoryContent();
    }

    @Test
    void testSetSelectedCode_updatesSelectedCodeArea() {
        String sampleCode = "public class Test {}";

        SwingUtilities.invokeLater(() -> {
            factoryContent.setSelectedCode(sampleCode);

            JTextPane selectedCodeArea = (JTextPane) ((JScrollPane) factoryContent.getComponent(0)).getViewport().getView();
            assertEquals(sampleCode, selectedCodeArea.getText());
        });
    }

    @Test
    void testSetSelectedCode_displaysFetchingMessage() {
        String sampleCode = "public class Test {}";

        SwingUtilities.invokeLater(() -> {
            factoryContent.setSelectedCode(sampleCode);

            JScrollPane scrollPane = (JScrollPane) factoryContent.getComponent(1);
            JTextPane chatOutputArea = (JTextPane) scrollPane.getViewport().getView();
            assertEquals("Fetching test cases, please wait...", chatOutputArea.getText());
        });
    }

    @Test
    void testSetSelectedCode_handlesNullCode() {
        SwingUtilities.invokeLater(() -> {
            factoryContent.setSelectedCode(null);

            JScrollPane scrollPane = (JScrollPane) factoryContent.getComponent(0);
            JTextPane selectedCodeArea = (JTextPane) scrollPane.getViewport().getView();
            assertEquals("", selectedCodeArea.getText());
        });
    }
}
