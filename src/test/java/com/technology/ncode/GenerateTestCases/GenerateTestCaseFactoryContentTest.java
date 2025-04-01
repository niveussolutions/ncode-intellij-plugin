package com.technology.ncode.GenerateTestCases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class GenerateTestCaseFactoryContentTest {

    private GenerateTestCaseFactoryContent factoryContent;

    @BeforeEach
    void setUp() {
        factoryContent = new GenerateTestCaseFactoryContent();
    }

    @Test
    void testSetSelectedCode_updatesSelectedCodeArea() throws InvocationTargetException, InterruptedException {
        String sampleCode = "public class Test {}";

        SwingUtilities.invokeAndWait(() -> factoryContent.setSelectedCode(sampleCode));

        SwingUtilities.invokeAndWait(() -> {
            JTextPane selectedCodeArea = (JTextPane) ((JScrollPane) factoryContent.getComponent(0)).getViewport()
                    .getView();
            assertEquals(sampleCode, selectedCodeArea.getText());
        });
    }

    @Test
    void testSetSelectedCode_displaysFetchingMessage() throws InvocationTargetException, InterruptedException {
        String sampleCode = "public class Test {}";

        SwingUtilities.invokeAndWait(() -> factoryContent.setSelectedCode(sampleCode));

        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scrollPane = (JScrollPane) factoryContent.getComponent(1);
            JTextPane chatOutputArea = (JTextPane) scrollPane.getViewport().getView();
            assertEquals("Fetching test cases, please wait...", chatOutputArea.getText());
        });
    }

    @Test
    void testSetSelectedCode_handlesNullCode() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> factoryContent.setSelectedCode(null));

        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scrollPane = (JScrollPane) factoryContent.getComponent(0);
            JTextPane selectedCodeArea = (JTextPane) scrollPane.getViewport().getView();
            assertEquals("", selectedCodeArea.getText());
        });
    }
}
