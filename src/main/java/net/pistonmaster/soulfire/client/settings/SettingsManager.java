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
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.account.service.AccountData;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.settings.lib.property.PropertyKey;

import javax.annotation.Nullable;
import javax.inject.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SettingsManager {
    // Used to read & write the settings file
    private static final Gson serializeGson = new GsonBuilder()
            .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
            .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
            .setPrettyPrinting()
            .create();
    private static final Gson deserializeGson = new GsonBuilder()
            .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
            .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
            .registerTypeAdapter(MinecraftAccount.class, new MinecraftAccountAdapter())
            .create();
    private final Multimap<PropertyKey, Consumer<JsonElement>> listeners = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
    private final Map<PropertyKey, Provider<JsonElement>> providers = new LinkedHashMap<>();
    @Getter
    private final AccountRegistry accountRegistry = new AccountRegistry();
    @Getter
    private final ProxyRegistry proxyRegistry = new ProxyRegistry();

    public static SettingsHolder createSettingsHolder(String json, @Nullable SettingsManager settingsManager) {
        try {
            var settingsSerialized = deserializeGson.fromJson(json, RootDataStructure.class);
            var settingsData = settingsSerialized.settings();
            var intProperties = new Object2IntArrayMap<PropertyKey>();
            var booleanProperties = new Object2BooleanArrayMap<PropertyKey>();
            var stringProperties = new Object2ObjectArrayMap<PropertyKey, String>();

            for (var entry : settingsData.entrySet()) {
                var namespace = entry.getKey();
                for (var setting : entry.getValue().getAsJsonObject().entrySet()) {
                    var key = setting.getKey();
                    var settingData = setting.getValue();

                    var propertyKey = new PropertyKey(namespace, key);

                    if (settingsManager != null) {
                        // Notify all listeners that this setting has been loaded
                        settingsManager.listeners.get(propertyKey)
                                .forEach(listener -> listener.accept(settingData));
                    }

                    if (settingData.isJsonPrimitive()) {
                        var primitive = settingData.getAsJsonPrimitive();
                        if (primitive.isBoolean()) {
                            booleanProperties.put(propertyKey, primitive.getAsBoolean());
                        } else if (primitive.isNumber()) {
                            intProperties.put(propertyKey, primitive.getAsInt());
                        } else if (primitive.isString()) {
                            stringProperties.put(propertyKey, primitive.getAsString());
                        } else {
                            throw new IllegalArgumentException("Unknown primitive type: " + primitive);
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown type: " + settingData);
                    }
                }
            }

            // Apply loaded accounts & proxies
            if (settingsManager != null) {
                settingsManager.accountRegistry.setAccounts(settingsSerialized.accounts());
                settingsManager.accountRegistry.callLoadHooks();

                settingsManager.proxyRegistry.setProxies(settingsSerialized.proxies());
                settingsManager.proxyRegistry.callLoadHooks();
            }

            return new SettingsHolder(
                    intProperties,
                    booleanProperties,
                    stringProperties,
                    settingsSerialized.accounts(),
                    settingsSerialized.proxies()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
        providers.put(property, provider);
    }

    public void registerListener(PropertyKey property, Consumer<JsonElement> listener) {
        listeners.put(property, listener);
    }

    public void loadProfile(Path path) throws IOException {
        try {
            createSettingsHolder(Files.readString(path), this);
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

        var settingsSerialized = new RootDataStructure(
                settingsData,
                accountRegistry.getAccounts(),
                proxyRegistry.getProxies()
        );

        return serializeGson.toJson(settingsSerialized);
    }

    private record RootDataStructure(
            JsonObject settings,
            List<MinecraftAccount> accounts,
            List<SWProxy> proxies
    ) {
    }

    private static class ECPublicKeyAdapter extends AbstractKeyAdapter<ECPublicKey> {
        @Override
        protected ECPublicKey createKey(byte[] bytes) throws JsonParseException {
            try {
                var keyFactory = KeyFactory.getInstance("EC");
                return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
            } catch (GeneralSecurityException e) {
                throw new JsonParseException(e);
            }
        }
    }

    private static class ECPrivateKeyAdapter extends AbstractKeyAdapter<ECPrivateKey> {
        @Override
        protected ECPrivateKey createKey(byte[] bytes) throws JsonParseException {
            try {
                var keyFactory = KeyFactory.getInstance("EC");
                return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
            } catch (GeneralSecurityException e) {
                throw new JsonParseException(e);
            }
        }
    }

    private static abstract class AbstractKeyAdapter<T> implements JsonSerializer<Key>, JsonDeserializer<T> {
        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return createKey(Base64.getDecoder().decode(json.getAsString()));
        }

        @Override
        public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }

        protected abstract T createKey(byte[] bytes) throws JsonParseException;
    }

    private static class MinecraftAccountAdapter implements JsonDeserializer<MinecraftAccount> {
        @Override
        public MinecraftAccount deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var authType = context.<AuthType>deserialize(json.getAsJsonObject().get("authType"), AuthType.class);

            return new GsonBuilder()
                    .registerTypeAdapter(AccountData.class, new AccountDataAdapter(authType))
                    .create()
                    .fromJson(json, MinecraftAccount.class);
        }

        private record AccountDataAdapter(AuthType authType) implements JsonDeserializer<AccountData> {
            @Override
            public AccountData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                return context.deserialize(json, authType.accountDataClass());
            }
        }
    }
}
