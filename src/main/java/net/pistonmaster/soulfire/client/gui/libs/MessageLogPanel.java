/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.client.gui.libs;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Modified version of: <a href="https://github.com/SKCraft/Launcher/blob/master/launcher/src/main/java/com/skcraft/launcher/swing/MessageLog.java">SKCraft/Launcher Log Panel</a>
 */
@Slf4j
public class MessageLogPanel extends JPanel {
    private final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
    private final NoopDocumentFilter noopDocumentFilter = new NoopDocumentFilter();
    private final List<String> toInsert = Collections.synchronizedList(new ArrayList<>());
    private final JTextArea textComponent;
    private final AbstractDocument document;
    private boolean clearText;

    public MessageLogPanel(int numLines) {
        setLayout(new BorderLayout());

        this.textComponent = new JTextArea();

        textComponent.setLineWrap(true);
        textComponent.setWrapStyleWord(true);

        textComponent.setFont(new JLabel().getFont());
        textComponent.setEditable(true);
        var caret = (DefaultCaret) textComponent.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        document = (AbstractDocument) textComponent.getDocument();
        document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));
        document.setDocumentFilter(noopDocumentFilter);

        updatePopup();

        textComponent.addCaretListener(e -> updatePopup());

        var scrollText = new JScrollPane(textComponent);
        scrollText.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        scrollText.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        new SmartScroller(scrollText);

        add(scrollText, BorderLayout.CENTER);

        var executorService = Executors.newSingleThreadScheduledExecutor((r) -> {
            var thread = new Thread(r);
            thread.setName("MessageLogPanel");
            thread.setDaemon(true);
            return thread;
        });
        executorService.scheduleWithFixedDelay(
                this::updateTextComponent,
                100,
                100,
                TimeUnit.MILLISECONDS
        );
    }

    private void updateTextComponent() {
        if (!clearText && toInsert.isEmpty()) {
            return;
        }

        try {
            SwingUtilities.invokeAndWait(() -> {
                noopDocumentFilter.filter(false);
                if (clearText) {
                    textComponent.setText("");
                    clearText = false;
                } else {
                    try {
                        var offset = document.getLength();
                        document.insertString(
                                offset,
                                String.join("", toInsert),
                                defaultAttributes
                        );
                    } catch (BadLocationException ignored) {
                    }
                    toInsert.clear();
                }
                noopDocumentFilter.filter(true);
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePopup() {
        var popupMenu = new JPopupMenu();
        if (textComponent.getSelectedText() != null) {
            var copyItem = new JMenuItem("Copy");
            copyItem.addActionListener(e -> textComponent.copy());
            popupMenu.add(copyItem);

            var uploadItem = new JMenuItem("Upload to pastes.dev");
            uploadItem.addActionListener(e -> {
                try {
                    var url = "https://pastes.dev/" + PastesDevService.upload(textComponent.getSelectedText());
                    JOptionPane.showMessageDialog(this,
                            SwingTextUtils.createHtmlPane("Uploaded to: <a href='" + url + "'>" + url + "</a>"),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    log.error("Failed to upload!", ex);
                    JOptionPane.showMessageDialog(this, "Failed to upload!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            popupMenu.add(uploadItem);
        }

        // Add divider
        if (popupMenu.getComponentCount() > 0) {
            popupMenu.addSeparator();
        }

        var clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clear());
        popupMenu.add(clearItem);
        textComponent.setComponentPopupMenu(popupMenu);
    }

    public void clear() {
        clearText = true;
    }

    public void log(final String line) {
        toInsert.add(line);
    }

    public String getLogs() {
        return textComponent.getText();
    }

    @Setter
    private static class NoopDocumentFilter extends DocumentFilter {
        private boolean filter = true;

        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
            if (filter) {
                return;
            }

            super.remove(fb, offset, length);
        }

        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (filter) {
                return;
            }

            super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (filter) {
                return;
            }

            super.replace(fb, offset, length, text, attrs);
        }
    }
}
