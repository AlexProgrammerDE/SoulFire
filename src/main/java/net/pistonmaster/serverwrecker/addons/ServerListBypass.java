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
package net.pistonmaster.serverwrecker.addons;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.EventHandler;
import net.pistonmaster.serverwrecker.api.event.bot.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.JMinMaxHelper;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.protocol.BotConnectionFactory;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import net.pistonmaster.serverwrecker.util.RandomUtil;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class ServerListBypass implements InternalAddon {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(this);
    }

    @EventHandler
    public void onPreConnect(PreBotConnectEvent event) {
        if (event.connection().meta().getTargetState() == ProtocolState.STATUS) {
            return;
        }

        BotConnectionFactory factory = event.connection().factory();
        if (!factory.settingsHolder().has(ServerListBypassSettings.class)) {
            return;
        }

        ServerListBypassSettings settings = factory.settingsHolder().get(ServerListBypassSettings.class);

        if (!settings.serverListBypass()) {
            return;
        }

        factory.prepareConnectionInternal(ProtocolState.STATUS).connect().join();
        TimeUtil.waitTime(RandomUtil.getRandomInt(settings.minDelay(), settings.maxDelay()), TimeUnit.SECONDS);
    }

    @EventHandler
    public void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ServerListBypassPanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ServerListBypassSettings.class, new ServerListBypassCommand());
    }

    private static class ServerListBypassPanel extends NavigationItem implements SettingsDuplex<ServerListBypassSettings> {
        private final JCheckBox serverListBypass;
        private final JSpinner minDelay;
        private final JSpinner maxDelay;

        public ServerListBypassPanel(ServerWrecker serverWrecker) {
            super();
            serverWrecker.getSettingsManager().registerDuplex(ServerListBypassSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Do Server List Bypass?"));
            serverListBypass = new PresetJCheckBox(ServerListBypassSettings.DEFAULT_SERVER_LIST_BYPASS);
            add(serverListBypass);

            add(new JLabel("Min Delay (Seconds)"));
            minDelay = new JSpinner(new SpinnerNumberModel(ServerListBypassSettings.DEFAULT_MIN_DELAY, 1, 1000, 1));
            add(minDelay);

            add(new JLabel("Max Delay (Seconds)"));
            maxDelay = new JSpinner(new SpinnerNumberModel(ServerListBypassSettings.DEFAULT_MAX_DELAY, 1, 1000, 1));
            add(maxDelay);

            JMinMaxHelper.applyLink(minDelay, maxDelay);
        }

        @Override
        public String getNavigationName() {
            return "Server List Bypass";
        }

        @Override
        public String getNavigationId() {
            return "server-list-bypass";
        }

        @Override
        public void onSettingsChange(ServerListBypassSettings settings) {
            serverListBypass.setSelected(settings.serverListBypass());
            minDelay.setValue(settings.minDelay());
            maxDelay.setValue(settings.maxDelay());
        }

        @Override
        public ServerListBypassSettings collectSettings() {
            return new ServerListBypassSettings(
                    serverListBypass.isSelected(),
                    (int) minDelay.getValue(),
                    (int) maxDelay.getValue()
            );
        }
    }

    private static class ServerListBypassCommand implements SettingsProvider<ServerListBypassSettings> {
        @CommandLine.Option(names = {"--server-list-bypass"}, description = "Do server list bypass?")
        private boolean serverListBypass = ServerListBypassSettings.DEFAULT_SERVER_LIST_BYPASS;
        @CommandLine.Option(names = {"--server-list-bypass-min-delay"}, description = "Minimum join delay after pinging the server")
        private int minDelay = ServerListBypassSettings.DEFAULT_MIN_DELAY;
        @CommandLine.Option(names = {"--server-list-bypass-max-delay"}, description = "Maximum join delay after pinging the server")
        private int maxDelay = ServerListBypassSettings.DEFAULT_MAX_DELAY;

        @Override
        public ServerListBypassSettings collectSettings() {
            return new ServerListBypassSettings(
                    serverListBypass,
                    minDelay,
                    maxDelay
            );
        }
    }

    private record ServerListBypassSettings(
            boolean serverListBypass,
            int minDelay,
            int maxDelay
    ) implements SettingsObject {
        public static final boolean DEFAULT_SERVER_LIST_BYPASS = false;
        public static final int DEFAULT_MIN_DELAY = 1;
        public static final int DEFAULT_MAX_DELAY = 3;
    }
}
