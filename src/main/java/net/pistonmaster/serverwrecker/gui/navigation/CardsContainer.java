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
package net.pistonmaster.serverwrecker.gui.navigation;

import ch.jalu.injector.Injector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.grpc.generated.ClientPluginSettingsPage;
import net.pistonmaster.serverwrecker.gui.GUIManager;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CardsContainer extends JPanel {
    public static final String NAVIGATION_MENU = "navigation-menu";
    @Getter
    private final List<NavigationItem> panels = new ArrayList<>();
    private final Injector injector;
    private final GUIManager guiManager;
    @Getter
    private final List<ClientPluginSettingsPage> pluginPages = new ArrayList<>();
    private final CardLayout cardLayout = new CardLayout();

    public void create() {
        setLayout(cardLayout);

        // Add bot settings
        panels.add(new GeneratedPanel(guiManager.getSettingsManager(), getByNamespace("bot")));
        var pluginPanel = injector.getSingleton(PluginListPanel.class);
        panels.add(pluginPanel);
        panels.add(injector.getSingleton(AccountPanel.class));
        panels.add(injector.getSingleton(ProxyPanel.class));
        panels.add(injector.getSingleton(DeveloperPanel.class));

        var navigationPanel = injector.getSingleton(NavigationPanel.class);
        add(navigationPanel, NAVIGATION_MENU);

        // Add the main page cards
        for (var item : panels) {
            add(NavigationWrapper.createBackWrapper(this, NAVIGATION_MENU, item), item.getNavigationId());
        }

        // Add the plugin page cards
        for (var item : pluginPages) {
            add(NavigationWrapper.createBackWrapper(this, pluginPanel.getNavigationId(),
                    new GeneratedPanel(guiManager.getSettingsManager(), item)), item.getNamespace());
        }

        setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
    }

    public ClientPluginSettingsPage getByNamespace(String namespace) {
        for (var page : pluginPages) {
            if (page.getNamespace().equals(namespace)) {
                return page;
            }
        }

        throw new IllegalArgumentException("No page found with namespace " + namespace);
    }

    public void show(String id) {
        cardLayout.show(this, id);
    }
}
