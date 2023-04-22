package net.pistonmaster.serverwrecker.gui.libs;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Modified version of: https://github.com/SKCraft/Launcher/blob/master/launcher/src/main/java/com/skcraft/launcher/swing/MessageLog.java
 */
public class MessageLogPanel extends JPanel {
    private final int numLines;
    private final boolean colorEnabled;

    protected JTextComponent textComponent;
    protected Document document;

    protected final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();

    public MessageLogPanel(int numLines, boolean colorEnabled) {
        this.numLines = numLines;
        this.colorEnabled = colorEnabled;

        setLayout(new BorderLayout());

        initComponents();
    }

    private void initComponents() {
        if (colorEnabled) {
            this.textComponent = new JTextPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
            };
        } else {
            JTextArea text = new JTextArea();
            this.textComponent = text;
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
        }

        textComponent.setFont(new JLabel().getFont());
        textComponent.setEditable(false);
        DefaultCaret caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        document = textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));

        JScrollPane scrollText = new JScrollPane();
        scrollText.setBorder(null);
        scrollText.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollText.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        scrollText.setViewportView(textComponent);

        add(scrollText, BorderLayout.CENTER);
    }

    public void clear() {
        textComponent.setText("");
    }

    /**
     * Log a message given the {@link javax.swing.text.AttributeSet}.
     *
     * @param line line
     */
    public void log(final String line) {
        final Document d = document;
        final JTextComponent t = textComponent;

        SwingUtilities.invokeLater(() -> {
            try {
                int offset = d.getLength();
                d.insertString(offset, line, defaultAttributes);
                t.setCaretPosition(0);
            } catch (BadLocationException ignored) {
            }
        });
    }
}
