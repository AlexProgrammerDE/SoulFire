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
package com.soulfiremc.client.gui;

import ch.jalu.injector.Injector;
import com.soulfiremc.client.gui.navigation.CardsContainer;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MainPanel extends JPanel {
  private final Injector injector;

  @PostConstruct
  public void postConstruct() {
    injector.register(MainPanel.class, this);

    setLayout(new GridLayout(1, 1));

    var cardsContainer = injector.newInstance(CardsContainer.class);
    cardsContainer.setMinimumSize(new Dimension(600, 0));

    var logPanel = injector.getSingleton(LogPanel.class);
    logPanel.setMinimumSize(new Dimension(600, 0));

    cardsContainer.setPreferredSize(new Dimension(600, cardsContainer.getPreferredSize().height));
    logPanel.setPreferredSize(new Dimension(600, logPanel.getPreferredSize().height));

    var splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, cardsContainer, logPanel);

    splitPane.setOneTouchExpandable(true);
    splitPane.setResizeWeight(0.5d);
    splitPane.setContinuousLayout(true);

    splitPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

    add(splitPane);
  }
}
