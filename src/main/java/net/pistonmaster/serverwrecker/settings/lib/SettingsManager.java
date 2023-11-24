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

import com.google.common.collect.BiMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.*;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.pistonmaster.serverwrecker.grpc.generated.ClientPluginSettingsPage;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.settings.lib.property.PropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SettingsManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(SettingsManager.class);
    private final Multimap<String, Property> propertyMap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
    // Used to read & write the settings file
    private final Gson dumpGson = new GsonBuilder().setPrettyPrinting().create();
    private final Gson baseGson = new GsonBuilder()
            .registerTypeAdapter(ProtocolVersion.class, new ProtocolVersionAdapter())
            .registerTypeHierarchyAdapter(ECPublicKey.class, new ECPublicKeyAdapter())
            .registerTypeHierarchyAdapter(ECPrivateKey.class, new ECPrivateKeyAdapter())
            .create();

    public SettingsManager(List<Class<? extends SettingsObject>> classes) {
        for (var clazz : classes) {
            addClass(clazz);
        }
    }

    public void addClass(Class<? extends SettingsObject> clazz) {
        var pluginSettings = clazz.getAnnotation(PluginSettings.class);
        var hidden = pluginSettings == null;
        var pageName = hidden ? "" : pluginSettings.pageName();
    }

    public void registerProvider(PropertyKey property, Provider<JsonElement> provider) {
        providers.add(new ProviderRegistration<>(clazz, provider));
    }

    public SettingsHolder collectSettings() {

        return settingsHolder;
    }

    public void onSettingsLoad(SettingsHolder settings) {
        for (SettingsObject setting : settings.settings()) {
            for (var listener : listeners) {
                loadSetting(setting, listener);
            }
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
        var settingsHolder = new ArrayList<JsonElement>();
        for (SettingsObject settingsObject : collectSettings().settings()) {
            settingsHolder.add(settingsTypeGson.toJsonTree(settingsObject));
        }

        return dumpGson.toJson(settingsHolder);
    }

    public List<ClientPluginSettingsPage> exportSettingsMeta() {

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

    private record ClassObjectAdapter(Gson gson, BiMap<String, Class<? extends SettingsObject>> classMap)
            implements JsonSerializer<Object>, JsonDeserializer<Object> {
        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            var serialized = gson.toJsonTree(src);
            var jsonObject = serialized.getAsJsonObject();
            var settingClass = classMap.inverse().get(src.getClass());
            Objects.requireNonNull(settingClass, "Setting name for " + src.getClass().getSimpleName() + " is null!");

            jsonObject.addProperty("settingsId", settingClass);
            return jsonObject;
        }

        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var jsonObject = json.getAsJsonObject();
            var settingType = jsonObject.get("settingId").getAsString();
            var clazz = classMap.get(settingType);
            Objects.requireNonNull(clazz, "Class for " + settingType + " is null!");

            return gson.fromJson(jsonObject, clazz);
        }
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
            var base64 = json.getAsString();
            var bytes = Base64.getDecoder().decode(base64);

            return createKey(bytes);
        }

        @Override
        public JsonElement serialize(Key src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }

        protected abstract T createKey(byte[] bytes) throws JsonParseException;
    }
}
