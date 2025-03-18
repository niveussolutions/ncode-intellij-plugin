package com.technology.ncode.GoogleThis;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public class GoogleThis extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        String selectedText = caretModel.getCurrentCaret().getSelectedText();
        BrowserUtil.browse("https://www.google.com/search?q=" + selectedText);
    }

    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final CaretModel caretModel = editor.getCaretModel();
        e.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
    }
}