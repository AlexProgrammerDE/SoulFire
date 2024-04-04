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

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import lombok.extern.slf4j.Slf4j;

/**
 * From <a href="http://tips4java.wordpress.com/2008/10/15/limit-lines-in-document/">tips4java</a>.
 *
 * @author Rob Camick
 */
@Slf4j
public class LimitLinesDocumentListener implements DocumentListener {
  private final boolean isRemoveFromStart;
  private int maximumLines;
  private volatile boolean isRemoving;

  /**
   * Specify the number of lines to be stored in the Document. Extra lines will be removed from the
   * start or end of the Document, depending on the boolean value specified.
   *
   * @param maximumLines      number of lines
   * @param isRemoveFromStart true to remove from the start
   */
  public LimitLinesDocumentListener(int maximumLines, boolean isRemoveFromStart) {
    setLimitLines(maximumLines);
    this.isRemoveFromStart = isRemoveFromStart;
    this.isRemoving = false;
  }

  /**
   * Set the maximum number of lines to be stored in the Document.
   *
   * @param maximumLines number of lines
   */
  public void setLimitLines(int maximumLines) {
    if (maximumLines < 1) {
      throw new IllegalArgumentException("Maximum lines must be greater than 0");
    }

    this.maximumLines = maximumLines;
  }

  @Override
  public void insertUpdate(final DocumentEvent e) {
    // Changes to the Document can not be done within the listener
    // so we need to add the processing to the end of the EDT

    if (!this.isRemoving) {
      this.isRemoving = true;
      SwingUtilities.invokeLater(() -> removeLines(e));
    }
  }

  @Override
  public void removeUpdate(DocumentEvent e) {}

  @Override
  public void changedUpdate(DocumentEvent e) {}

  private void removeLines(DocumentEvent e) {
    // The root Element of the Document will tell us the total number
    // of line in the Document.

    try {
      var document = e.getDocument();
      var root = document.getDefaultRootElement();
      var excess = root.getElementCount() - maximumLines;

      if (excess > 0) {
        if (isRemoveFromStart) {
          removeFromStart(document, root, excess);
        } else {
          removeFromEnd(document, root);
        }
      }
    } finally {
      this.isRemoving = false;
    }
  }

  private void removeFromStart(Document document, Element root, int excess) {
    var line = root.getElement(excess - 1);
    var end = line.getEndOffset();

    try {
      document.remove(0, end);
    } catch (BadLocationException ble) {
      log.error("{}", ble.getMessage(), ble);
    }
  }

  private void removeFromEnd(Document document, Element root) {
    // We use start minus 1 to make sure we remove the newline
    // character of the previous line

    var line = root.getElement(maximumLines);
    var start = line.getStartOffset();
    var end = root.getEndOffset();

    try {
      document.remove(start - 1, end - start);
    } catch (BadLocationException ble) {
      log.error("{}", ble.getMessage(), ble);
    }
  }
}
