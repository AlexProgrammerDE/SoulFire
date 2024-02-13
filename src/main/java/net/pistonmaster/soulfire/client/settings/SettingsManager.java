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
package net.pistonmaster.soulfire.client.settings;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.settings.lib.property.PropertyKey;
import net.pistonmaster.soulfire.util.GsonAdapters;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class SettingsManager {
    // Used to read & write the settings file
    private static final Gson serializeGson = new GsonBuilder()
            .registerTypeHierarchyAdapter(ECPublicKey.class, new GsonAdapters.ECPublicKeyAdapter())
            .registerTypeHierarchyAdapter(ECPrivateKey.class, new GsonAdapters.ECPrivateKeyAdapter())
            .setPrettyPrinting()
            .create();
    private final Multimap<PropertyKey, Consumer<JsonElement>> listeners = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
    private final Map<PropertyKey, Provider<JsonElement>> providers = new LinkedHashMap<>();
    @Getter
    private final AccountRegistry accountRegistry = new AccountRegistry();
    @Getter
    private final ProxyRegistry proxyRegistry = new ProxyRegistry();

    public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
        providers.put(property, provider);
    }

    public void registerListener(PropertyKey property, Consumer<JsonElement> listener) {
        listeners.put(property, listener);
    }

    public void loadProfile(Path path) throws IOException {
        try {
            SettingsHolder.createSettingsHolder(Files.readString(path),
                    listeners, accounts -> {
                        accountRegistry.setAccounts(accounts);
                        accountRegistry.callLoadHooks();
                    }, proxies -> {
                        proxyRegistry.setProxies(proxies);
                        proxyRegistry.callLoadHooks();
                    });
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void saveProfile(Path path) throws IOException {
        Files.createDirectories(path.getParent());

        try {
            Files.writeString(path, exportSettings());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String exportSettings() {
        var settingsData = new JsonObject();
        for (var providerEntry : providers.entrySet()) {
            var property = providerEntry.getKey();
            var provider = providerEntry.getValue();

            var namespace = property.namespace();
            var settingId = property.key();
            var value = provider.get();

            var namespaceData = settingsData.getAsJsonObject(namespace);
            if (namespaceData == null) {
                namespaceData = new JsonObject();
                settingsData.add(namespace, namespaceData);
            }

            namespaceData.add(settingId, value);
        }

        var settingsSerialized = new SettingsHolder.RootDataStructure(
                settingsData,
                accountRegistry.getAccounts(),
                proxyRegistry.getProxies()
        );

        return serializeGson.toJson(settingsSerialized);
    }
}
