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
package net.pistonmaster.serverwrecker.settings;

import net.pistonmaster.serverwrecker.gui.navigation.SettingsPanel;

import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private final List<ListenerRegistration<?>> listeners = new ArrayList<>();
    private final List<ProviderRegistration<?>> providers = new ArrayList<>();
    private final Class<? extends SettingsObject>[] registeredSettings;

    @SafeVarargs
    public SettingsManager(Class<? extends SettingsObject>... registeredSettings) {
        this.registeredSettings = registeredSettings;
    }

    public <T extends SettingsObject> void registerListener(Class<T> clazz, SettingsListener<T> listener) {
        listeners.add(new ListenerRegistration<>(clazz, listener));
    }

    public <T extends SettingsObject> void registerProvider(Class<T> clazz, SettingsProvider<T> provider) {
        providers.add(new ProviderRegistration<>(clazz, provider));
    }

    public <T extends SettingsObject> void registerDuplex(Class<T> clazz, SettingsDuplex<T> duplex) {
        registerListener(clazz, duplex);
        registerProvider(clazz, duplex);
    }

    public SettingsHolder collectSettings() {
        SettingsHolder settingsHolder = new SettingsHolder(providers.stream()
                .map(ProviderRegistration::provider)
                .map(SettingsProvider::collectSettings)
                .toList());

        for (Class<? extends SettingsObject> clazz : registeredSettings) {
            if (!settingsHolder.has(clazz)) {
                throw new IllegalArgumentException("No settings found for " + clazz.getSimpleName());
            }
        }

        return settingsHolder;
    }

    @SuppressWarnings("unchecked")
    public void onSettingsLoad(SettingsHolder settings) {
        for (SettingsObject setting : settings.settings()) {
            for (ListenerRegistration<?> listener : listeners) {
                if (listener.clazz.isInstance(setting)) {
                    ((SettingsListener<SettingsObject>) listener.listener).onSettingsChange(setting);
                }
            }
        }
    }

    private record ListenerRegistration<T extends SettingsObject>(Class<T> clazz, SettingsListener<T> listener) {
    }

    private record ProviderRegistration<T extends SettingsObject>(Class<T> clazz, SettingsProvider<T> provider) {
    }
}
