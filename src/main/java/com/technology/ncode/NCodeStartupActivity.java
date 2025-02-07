package com.technology.ncode;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.util.TextRange;

/**
 * This class implements a startup activity for the NCode plugin.
 * It displays a welcome notification when the IDE starts up.
 */
public class NCodeStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        // Display a welcome notification when the IDE starts up
        NotificationGroupManager.getInstance()
                .getNotificationGroup("NCode Notifications")
                .createNotification("Welcome to NCode!", NotificationType.INFORMATION)
                .notify(project);

        // Check internet connectivity
        checkInternetConnectivity(project);
    }

    private void checkInternetConnectivity(@NotNull Project project) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            private boolean wasConnected = true;

            @Override
            public void run() {
                boolean isConnected = isInternetAvailable();
                if (isConnected != wasConnected) {
                    NotificationType type = isConnected ? NotificationType.INFORMATION : NotificationType.WARNING;
                    String message = isConnected ? "You are ONLINE" : "You are OFFLINE";
                    String color = isConnected ? "background-color: green;" : "background-color: red;";
                    String content = "<html><body style=\"" + color + "\"><b>NCode:</b> " + message + "</body></html>";
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("NCode Notifications")
                            .createNotification(message, type)
                            .setContent(content)
                            .notify(project);
                    wasConnected = isConnected;
                }
            }
        }, 0, 10000); // Check every 10 seconds
    }

    private boolean isInternetAvailable() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://www.google.com").openConnection();
            urlConnection.setRequestMethod("HEAD");
            urlConnection.setConnectTimeout(3000);
            urlConnection.connect();
            return (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (IOException e) {
            return false;
        }
    }

    public static class ShowCurrentLineAction extends AnAction {
        private static final int CONTEXT_WINDOW_LINES = 10;

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            if (editor != null) {
                String surroundingLines = getSurroundingLines(editor);
                notifyUser(e.getProject(), "Surrounding Lines Code", surroundingLines);
            }
        }

        private String getSurroundingLines(Editor editor) {
            CaretModel caretModel = editor.getCaretModel();
            int currentLine = caretModel.getLogicalPosition().line;
            Document document = editor.getDocument();
            int startLine = Math.max(0, currentLine - CONTEXT_WINDOW_LINES);
            int endLine = Math.min(document.getLineCount() - 1, currentLine + CONTEXT_WINDOW_LINES);

            StringBuilder lines = new StringBuilder();
            for (int i = startLine; i <= endLine; i++) {
                lines.append(
                        document.getText(new TextRange(document.getLineStartOffset(i), document.getLineEndOffset(i))));
                lines.append("\n");
            }
            return lines.toString();
        }

        private void notifyUser(Project project, String title, String content) {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("NCode Notifications")
                    .createNotification(title, content, NotificationType.INFORMATION)
                    .notify(project);
        }
    }
}