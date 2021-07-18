package com.github.games647.lambdaattack.logging;

import javax.swing.*;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

    private final JTextArea logArea;
    private final Formatter formatter = new LogFormatter();

    public LogHandler(JTextArea logArea) {
        this.logArea = logArea;
    }

    @Override
    public void publish(LogRecord record) {
        String formatted = formatter.format(record);
        SwingUtilities.invokeLater(() -> logArea.append(formatted));
    }

    @Override
    public void flush() {
        //do nothing
    }

    @Override
    public void close() throws SecurityException {
        //do nothing
    }
}
