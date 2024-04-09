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
package com.soulfiremc.client.gui.libs;

import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.soulfiremc.client.gui.ThemeUtil;
import com.soulfiremc.util.XtermPalette256;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pk.ansi4j.core.DefaultFunctionFinder;
import pk.ansi4j.core.DefaultParserFactory;
import pk.ansi4j.core.DefaultTextHandler;
import pk.ansi4j.core.api.Environment;
import pk.ansi4j.core.api.Fragment;
import pk.ansi4j.core.api.FragmentType;
import pk.ansi4j.core.api.FunctionFragment;
import pk.ansi4j.core.api.ParserFactory;
import pk.ansi4j.core.api.TextFragment;
import pk.ansi4j.core.api.iso6429.C0ControlFunction;
import pk.ansi4j.core.api.iso6429.ControlSequenceFunction;
import pk.ansi4j.core.iso6429.C0ControlFunctionHandler;
import pk.ansi4j.core.iso6429.C1ControlFunctionHandler;
import pk.ansi4j.core.iso6429.ControlSequenceHandler;
import pk.ansi4j.core.iso6429.ControlStringHandler;
import pk.ansi4j.core.iso6429.IndependentControlFunctionHandler;

/**
 * Modified version of: <a
 * href="https://github.com/SKCraft/Launcher/blob/master/launcher/src/main/java/com/skcraft/launcher/swing/MessageLog.java">SKCraft/Launcher
 * Log Panel</a>.
 */
@Slf4j
public class MessageLogPanel extends JPanel {
  private final SimpleAttributeSet defaultAttributes = new SimpleAttributeSet();
  private final NoopDocumentFilter noopDocumentFilter = new NoopDocumentFilter();
  private final List<String> toInsert = Collections.synchronizedList(new ArrayList<>());
  private final JTextPane textComponent;
  private final Document document;
  private final ParserFactory factory =
    new DefaultParserFactory.Builder()
      .environment(Environment._7_BIT)
      .textHandler(new DefaultTextHandler())
      .functionFinder(new DefaultFunctionFinder())
      .functionHandlers(
        new C0ControlFunctionHandler(),
        new C1ControlFunctionHandler(),
        new ControlSequenceHandler(),
        new IndependentControlFunctionHandler(),
        new ControlStringHandler())
      .build();
  private TerminalTheme theme = ThemeUtil.getTerminal();
  private boolean clearText;

  public MessageLogPanel(int numLines) {
    setLayout(new BorderLayout());

    this.textComponent = new JTextPane();

    textComponent.setFont(
      new Font(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, new JLabel().getFont().getSize()));
    textComponent.setEditable(true);
    textComponent.setBackground(theme.backgroundColor());
    StyleConstants.setForeground(defaultAttributes, theme.getDefaultTextColor());

    var caret = (DefaultCaret) textComponent.getCaret();
    caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    document = textComponent.getDocument();
    document.addDocumentListener(new LimitLinesDocumentListener(numLines, true));
    ((AbstractDocument) document).setDocumentFilter(noopDocumentFilter);

    updatePopup();

    textComponent.addCaretListener(e -> updatePopup());

    var scrollText = new JScrollPane(textComponent);
    scrollText.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    scrollText.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollText.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    new SmartScroller(scrollText);

    add(scrollText, BorderLayout.CENTER);

    var executorService =
      Executors.newSingleThreadScheduledExecutor(
        r -> Thread.ofPlatform().name("MessageLogPanel").daemon().unstarted(r));
    executorService.scheduleWithFixedDelay(
      this::updateTextComponent, 100, 100, TimeUnit.MILLISECONDS);
  }

  private void updateTextComponent() {
    try {
      SwingUtilities.invokeAndWait(
        () -> {
          synchronized (toInsert) {
            if (!clearText && toInsert.isEmpty()) {
              return;
            }

            noopDocumentFilter.filter(false);
            try {
              if (clearText) {
                textComponent.setText("");
                clearText = false;
              } else {
                try {
                  var parser = factory.createParser(String.join("", toInsert));

                  Fragment fragment;
                  while ((fragment = parser.parse()) != null) {
                    if (fragment.getType() == FragmentType.TEXT) {
                      var textFragment = (TextFragment) fragment;
                      document.insertString(
                        document.getLength(), textFragment.getText(), defaultAttributes);
                    } else if (fragment.getType() == FragmentType.FUNCTION) {
                      var functionFragment = (FunctionFragment) fragment;
                      if (functionFragment.getFunction()
                        == ControlSequenceFunction.SGR_SELECT_GRAPHIC_RENDITION) {
                        var sgr = (int) functionFragment.getArguments().getFirst().getValue();

                        switch (sgr) {
                          case 0 -> {
                            for (var attributes = defaultAttributes.copyAttributes().getAttributeNames();
                                 attributes.hasMoreElements(); ) {
                              var name = attributes.nextElement();
                              defaultAttributes.removeAttribute(name);
                            }
                            StyleConstants.setForeground(
                              defaultAttributes, theme.getDefaultTextColor());
                          }
                          case 1 -> StyleConstants.setBold(defaultAttributes, true);
                          case 3 -> StyleConstants.setItalic(defaultAttributes, true);
                          case 4 -> StyleConstants.setUnderline(defaultAttributes, true);
                          case 9 -> StyleConstants.setStrikeThrough(defaultAttributes, true);
                          case 22 -> StyleConstants.setBold(defaultAttributes, false);
                          case 23 -> StyleConstants.setItalic(defaultAttributes, false);
                          case 24 -> StyleConstants.setUnderline(defaultAttributes, false);
                          case 29 -> StyleConstants.setStrikeThrough(defaultAttributes, false);
                          case 30 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(0));
                          case 31 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(1));
                          case 32 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(2));
                          case 33 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(3));
                          case 34 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(4));
                          case 35 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(5));
                          case 36 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(6));
                          case 37, 39 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(7));
                          case 38 -> {
                            var secondArgument =
                              (int) functionFragment.getArguments().get(1).getValue();
                            StyleConstants.setForeground(
                              defaultAttributes,
                              switch (secondArgument) {
                                case 2 -> {
                                  var r = (int) functionFragment.getArguments().get(2).getValue();
                                  var g = (int) functionFragment.getArguments().get(3).getValue();
                                  var b =
                                    (int) functionFragment.getArguments().getLast().getValue();
                                  yield new Color(r, g, b);
                                }
                                case 5 -> {
                                  var id = (int) functionFragment.getArguments().get(2).getValue();
                                  yield XtermPalette256.getColor(id);
                                }
                                default -> Color.decode("#FFFFFF");
                              });
                          }
                          case 90 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(8));
                          case 91 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(9));
                          case 92 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(10));
                          case 93 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(11));
                          case 94 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(12));
                          case 95 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(13));
                          case 96 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(14));
                          case 97 -> StyleConstants.setForeground(defaultAttributes, theme.getANSIColor(15));
                        }
                      } else if (functionFragment.getFunction() == C0ControlFunction.LF_LINE_FEED) {
                        document.insertString(document.getLength(), "\n", defaultAttributes);
                      }
                    }
                  }
                } catch (BadLocationException e) {
                  log.debug("Failed to insert text!", e);
                }
                toInsert.clear();
              }
            } catch (Throwable t) {
              log.error("Failed to update text component!", t);
            } finally {
              noopDocumentFilter.filter(true);
            }
          }
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
    }

    // Add divider
    if (popupMenu.getComponentCount() > 0) {
      popupMenu.addSeparator();
    }

    var uploadItem = new JMenuItem("Upload logs");
    uploadItem.addActionListener(
      event -> {
        try {
          var url = LogsUploadService.upload(textComponent.getText()).url();
          JOptionPane.showMessageDialog(
            this,
            SFSwingUtils.createHtmlPane("Uploaded to: <a href='" + url + "'>" + url + "</a>"),
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
          log.error("Failed to upload!", e);
          JOptionPane.showMessageDialog(
            this, "Failed to upload!", "Error", JOptionPane.ERROR_MESSAGE);
        }
      });
    popupMenu.add(uploadItem);

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

  public void refreshTheme() {
    theme = ThemeUtil.getTerminal();
    textComponent.setBackground(theme.backgroundColor());
    StyleConstants.setForeground(defaultAttributes, theme.getDefaultTextColor());
  }

  @Setter
  private static class NoopDocumentFilter extends DocumentFilter {
    private boolean filter = true;

    @Override
    public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
      throws BadLocationException {
      if (filter) {
        return;
      }

      super.remove(fb, offset, length);
    }

    @Override
    public void insertString(
      DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr)
      throws BadLocationException {
      if (filter) {
        return;
      }

      super.insertString(fb, offset, string, attr);
    }

    @Override
    public void replace(
      DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
      throws BadLocationException {
      if (filter) {
        return;
      }

      super.replace(fb, offset, length, text, attrs);
    }
  }
}
