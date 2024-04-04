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

import com.formdev.flatlaf.extras.FlatSVGUtils;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;

@Slf4j
public class SFSwingUtils {
  private SFSwingUtils() {}

  public static JTextPane createHtmlPane(@Language("html") String text) {
    var pane = new JTextPane();
    pane.setContentType("text/html");
    pane.setText(text);
    pane.setEditable(false);
    pane.setBackground(null);
    pane.setBorder(null);
    pane.addHyperlinkListener(
      event -> {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED
          || !Desktop.isDesktopSupported()) {
          return;
        }

        try {
          Desktop.getDesktop().browse(event.getURL().toURI());
        } catch (IOException | URISyntaxException e) {
          log.error("Failed to open link!", e);
        }
      });

    return pane;
  }

  public static String htmlCenterText(String text) {
    return htmlText("<center>" + text + "</center>");
  }

  public static String htmlText(String text) {
    return "<html>" + text + "</html>";
  }

  public static UndoManager addUndoRedo(JTextComponent textComponent) {
    var undoManager = new UndoManager();
    textComponent.getDocument().addUndoableEditListener(undoManager);

    var undoAction =
      new AbstractAction("Undo") {
        @Override
        public void actionPerformed(ActionEvent event) {
          try {
            if (undoManager.canUndo()) {
              undoManager.undo();
            }
          } catch (CannotUndoException e) {
            log.error("Failed to undo!", e);
          }
        }
      };

    var redoAction =
      new AbstractAction("Redo") {
        @Override
        public void actionPerformed(ActionEvent event) {
          try {
            if (undoManager.canRedo()) {
              undoManager.redo();
            }
          } catch (CannotRedoException e) {
            log.error("Failed to redo!", e);
          }
        }
      };

    var menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    textComponent
      .getInputMap()
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcutMask), "undo");
    textComponent
      .getInputMap()
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutMask), "redo");

    textComponent.getActionMap().put("undo", undoAction);
    textComponent.getActionMap().put("redo", redoAction);

    return undoManager;
  }

  public static void setLogo(JFrame frame) {
    frame.setIconImages(FlatSVGUtils.createWindowIconImages("/icons/icon.svg"));
  }
}
