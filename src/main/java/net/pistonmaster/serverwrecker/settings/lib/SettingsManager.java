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
package net.pistonmaster.serverwrecker.settings.lib;

import com.google.gson.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.pistonmaster.serverwrecker.auth.service.AccountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SettingsManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(SettingsManager.class);
    private final List<ListenerRegistration<?>> listeners = new ArrayList<>();
    private final List<ProviderRegistration<?>> providers = new ArrayList<>();
    private final Class<? extends SettingsObject>[] registeredSettings;
    // Used to read & write the settings file
    private final Gson dumpGson = new GsonBuilder().setPrettyPrinting().create();
    private final Gson baseGson = new GsonBuilder()
            .registerTypeAdapter(ProtocolVersion.class, new ProtocolVersionAdapter())
            .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
            .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
            .create();
    private final Gson normalGson = baseGson.newBuilder()
            .registerTypeHierarchyAdapter(AccountData.class, new ClassObjectAdapter(baseGson))
            .create();
    private final Gson settingsTypeGson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Object.class, new ClassObjectAdapter(normalGson))
            .create();

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
        var settingsHolder = new SettingsHolder(providers.stream()
                .map(ProviderRegistration::provider)
                .map(SettingsProvider::collectSettings)
                .toList());

        for (var clazz : registeredSettings) {
            if (!settingsHolder.has(clazz)) {
                throw new IllegalArgumentException("No settings found for " + clazz.getSimpleName());
            }
        }

        return settingsHolder;
    }

    public void onSettingsLoad(SettingsHolder settings) {
        for (SettingsObject setting : settings.settings()) {
            for (var listener : listeners) {
                loadSetting(setting, listener);
            }
        }
    }

    private <T extends SettingsObject> void loadSetting(SettingsObject setting, ListenerRegistration<T> registration) {
        if (registration.clazz.isInstance(setting)) {
            var castedSetting = registration.clazz.cast(setting);
            registration.listener.onSettingsChange(castedSetting);
        }
    }

    public void loadProfile(Path path) throws IOException {
        try {
            onSettingsLoad(createSettingsHolder(Files.readString(path)));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public SettingsHolder createSettingsHolder(String json) {
        try {
            var settingsHolder = dumpGson.fromJson(json, JsonArray.class);
            List<SettingsObject> settingsObjects = new ArrayList<>();
            for (var jsonElement : settingsHolder) {
                settingsObjects.add(settingsTypeGson.fromJson(jsonElement, SettingsObject.class));
            }

            return new SettingsHolder(settingsObjects);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
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
        List<JsonElement> settingsHolder = new ArrayList<>();
        for (SettingsObject settingsObject : collectSettings().settings()) {
            settingsHolder.add(settingsTypeGson.toJsonTree(settingsObject));
        }

        return dumpGson.toJson(settingsHolder);
    }

    private record ListenerRegistration<T extends SettingsObject>(Class<T> clazz, SettingsListener<T> listener) {
    }

    private record ProviderRegistration<T extends SettingsObject>(Class<T> clazz, SettingsProvider<T> provider) {
    }

    private static class ProtocolVersionAdapter implements JsonSerializer<ProtocolVersion>, JsonDeserializer<ProtocolVersion> {
        @Override
        public ProtocolVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ProtocolVersion.getClosest(json.getAsString());
        }

        @Override
        public JsonElement serialize(ProtocolVersion src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getName());
        }
    }

    private record ClassObjectAdapter(Gson gson) implements JsonSerializer<Object>, JsonDeserializer<Object> {
        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            var serialized = gson.toJsonTree(src);
            var jsonObject = serialized.getAsJsonObject();
            jsonObject.addProperty("class", src.getClass().getName());
            return jsonObject;
        }

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var jsonObject = json.getAsJsonObject();
            var className = jsonObject.get("class").getAsString();
            try {
                var clazz = Class.forName(className);
                return gson.fromJson(jsonObject, clazz);
            } catch (ClassNotFoundException e) {
                return null; // Some extension might not be loaded, so we just ignore it
            }
        }
    }

    private static class ECPublicKeyAdapter implements JsonSerializer<ECPublicKey>, JsonDeserializer<ECPublicKey> {
        @Override
        public ECPublicKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var base64 = json.getAsString();
            var bytes = Base64.getDecoder().decode(base64);

            try {
                var keyFactory = KeyFactory.getInstance("EC");
                return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(ECPublicKey src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }
    }

    private static class ECPrivateKeyAdapter implements JsonSerializer<ECPrivateKey>, JsonDeserializer<ECPrivateKey> {
        @Override
        public ECPrivateKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var base64 = json.getAsString();
            var bytes = Base64.getDecoder().decode(base64);

            try {
                var keyFactory = KeyFactory.getInstance("EC");
                return (ECPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(ECPrivateKey src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }
    }
}
