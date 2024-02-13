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
package net.pistonmaster.soulfire.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class GsonAdapters {
    public static class ECPublicKeyAdapter extends AbstractKeyAdapter<ECPublicKey> {
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

    public static class ECPrivateKeyAdapter extends AbstractKeyAdapter<ECPrivateKey> {
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
}
