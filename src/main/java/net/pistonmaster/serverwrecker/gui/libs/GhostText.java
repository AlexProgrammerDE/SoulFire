/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.gui.libs;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class GhostText implements FocusListener, DocumentListener, PropertyChangeListener {
    private final JTextField textfield;
    private final String ghostText;
    private boolean isEmpty;
    private Color ghostColor;
    private Color foregroundColor;

    public GhostText(final JTextField textfield, String ghostText) {
        super();
        this.textfield = textfield;
        this.ghostText = ghostText;
        this.ghostColor = Color.GRAY;
        textfield.addFocusListener(this);
        registerListeners();
        updateState();
        if (!this.textfield.hasFocus()) {
            focusLost(null);
        }
    }

    public void delete() {
        unregisterListeners();
        textfield.removeFocusListener(this);
    }

    private void registerListeners() {
        textfield.getDocument().addDocumentListener(this);
        textfield.addPropertyChangeListener("foreground", this);
    }

    private void unregisterListeners() {
        textfield.getDocument().removeDocumentListener(this);
        textfield.removePropertyChangeListener("foreground", this);
    }

    public Color getGhostColor() {
        return ghostColor;
    }

    public void setGhostColor(Color ghostColor) {
        this.ghostColor = ghostColor;
    }

    private void updateState() {
        isEmpty = textfield.getText().length() == 0;
        foregroundColor = textfield.getForeground();
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (isEmpty) {
            unregisterListeners();
            try {
                textfield.setText("");
                textfield.setForeground(foregroundColor);
            } finally {
                registerListeners();
            }
        }

    }

    @Override
    public void focusLost(FocusEvent e) {
        if (isEmpty) {
            unregisterListeners();
            try {
                textfield.setText(ghostText);
                textfield.setForeground(ghostColor);
            } finally {
                registerListeners();
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateState();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateState();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateState();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateState();
    }
}
