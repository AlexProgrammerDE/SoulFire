package com.github.games647.lambdaattack.logging;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class LogHandler extends Handler {

    private final JTextArea logArea;
    private final Formatter formatter = new LogFormatter();

    public LogHandler(JTextArea logArea) {
        this.logArea = logArea;
    }

    @Override
    public void publish(LogRecord record) {
        SwingUtilities.invokeLater(() -> logArea.append(formatter.format(record)));
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
