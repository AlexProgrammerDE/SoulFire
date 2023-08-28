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
package net.pistonmaster.serverwrecker.gui;

import ch.jalu.injector.Injector;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogRequest;
import net.pistonmaster.serverwrecker.grpc.generated.logs.LogResponse;
import net.pistonmaster.serverwrecker.gui.libs.MessageLogPanel;
import net.pistonmaster.serverwrecker.gui.navigation.CardsContainer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainPanel extends JPanel {
    private final Injector injector;
    private final CardsContainer cardsContainer;

    @PostConstruct
    public void postConstruct() {
        injector.register(MainPanel.class, this);

        JPanel logPanel = injector.getSingleton(LogPanel.class);
        cardsContainer.create();

        setLayout(new GridLayout(1, 1));

        cardsContainer.setMinimumSize(new Dimension(600, 0));
        logPanel.setMinimumSize(new Dimension(600, 0));

        cardsContainer.setPreferredSize(new Dimension(600,
                cardsContainer.getPreferredSize().height));
        logPanel.setPreferredSize(new Dimension(600,
                logPanel.getPreferredSize().height));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cardsContainer, logPanel);

        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5d);
        splitPane.setContinuousLayout(true);

        splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        add(splitPane);
    }
}
