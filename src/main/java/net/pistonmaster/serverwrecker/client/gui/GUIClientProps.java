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
package net.pistonmaster.serverwrecker.client.gui;

import net.pistonmaster.serverwrecker.util.SWPathConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class GUIClientProps {
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIClientProps.class);
    private static final Path SETTINGS_PATH = SWPathConstants.DATA_FOLDER.resolve("gui-data.properties");
    private static final Properties SETTINGS = new Properties();

    private GUIClientProps() {
    }

    public static void loadSettings() {
        if (!Files.exists(SETTINGS_PATH)) {
            return;
        }

        try (var is = Files.newInputStream(SETTINGS_PATH)) {
            SETTINGS.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Failed to load settings!", e);
        }
    }

    public static void saveSettings() {
        try (var os = Files.newOutputStream(SETTINGS_PATH, StandardOpenOption.CREATE)) {
            SETTINGS.store(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)), "ServerWrecker GUI Settings");
        } catch (IOException e) {
            LOGGER.error("Failed to save settings!", e);
        }
    }

    public static boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(getString(key, String.valueOf(def)));
    }

    public static void setBoolean(String key, boolean value) {
        setString(key, String.valueOf(value));
    }

    public static String getString(String key, String def) {
        return SETTINGS.getProperty(key, def);
    }

    public static void setString(String key, String value) {
        SETTINGS.setProperty(key, value);
        saveSettings();
    }
}
