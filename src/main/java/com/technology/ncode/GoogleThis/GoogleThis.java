package com.technology.ncode.GoogleThis;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleThis extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        if (selectedText == null || selectedText.isBlank()) {
            return; // Ensure null or blank selections are handled
        }
        String encodedText = URLEncoder.encode(selectedText, StandardCharsets.UTF_8); // Encode the text
        BrowserUtil.browse("https://www.google.com/search?q=" + encodedText);
    }

    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        e.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
    }
}