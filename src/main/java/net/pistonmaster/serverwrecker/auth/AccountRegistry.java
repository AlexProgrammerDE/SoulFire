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
package net.pistonmaster.serverwrecker.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import org.apache.commons.validator.routines.EmailValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
public class AccountRegistry implements SettingsDuplex<AccountList> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<JavaAccount> accounts = new ArrayList<>();
    private final ServerWrecker serverWrecker;

    public void loadFromFile(Path file) throws IOException {
        loadFromString(Files.readString(file));
    }

    public void loadFromString(String file) {
        List<JavaAccount> newAccounts = new ArrayList<>();

        if (isJson(file)) {
            if (isArray(file)) {
                newAccounts.addAll(Arrays.stream(GSON.fromJson(file, AccountJsonType[].class))
                        .map(this::fromJsonType)
                        .toList());
            } else if (isObject(file)) {
                newAccounts.add(fromJsonType(GSON.fromJson(file, AccountJsonType.class)));
            } else {
                throw new IllegalArgumentException("Invalid JSON!");
            }
        } else {
            String[] accountLines = file.split("\n");

            Arrays.stream(accountLines)
                    .filter(line -> !line.isBlank())
                    .distinct()
                    .map(this::fromString)
                    .forEach(newAccounts::add);
        }

        this.accounts.addAll(newAccounts);
        serverWrecker.getLogger().info("Loaded {} accounts!", newAccounts.size());
    }

    private boolean isJson(String file) {
        try {
            GSON.fromJson(file, JsonElement.class);
            return true;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }

    private boolean isArray(String file) {
        return GSON.fromJson(file, JsonElement.class).isJsonArray();
    }

    private boolean isObject(String file) {
        return GSON.fromJson(file, JsonElement.class).isJsonObject();
    }

    private JavaAccount fromString(String account) {
        account = account.trim();

        if (account.isBlank()) {
            throw new IllegalArgumentException("Account cannot be empty!");
        }

        if (!account.contains(":")) {
            return new JavaAccount(account);
        }

        String[] split = account.split(":");
        if (split.length < 2) {
            throw new IllegalArgumentException("Invalid account!");
        }

        String email = split[0].trim();
        expectEmail(email);

        String password = split[1].trim();

        try {
            return serverWrecker.authenticate(AuthType.MICROSOFT, email, password, null); // TODO: Implement proxy and auth type
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEmail(String account) {
        return EmailValidator.getInstance().isValid(account);
    }

    private void expectEmail(String account) {
        if (!isEmail(account)) {
            throw new IllegalArgumentException("Invalid email!");
        }
    }

    private JavaAccount fromJsonType(AccountJsonType type) {
        Objects.requireNonNull(type, "Account type cannot be null");
        String username = Objects.requireNonNull(type.username, "Username not found").trim();
        UUID profileId = type.profileId == null ? null : UUID.fromString(type.profileId.trim());
        AuthType authType;
        if (type.authType == null) {
            authType = profileId == null ? AuthType.OFFLINE : AuthType.MICROSOFT;
        } else {
            authType = AuthType.valueOf(type.authType.trim().toUpperCase(Locale.ENGLISH));
        }

        String authToken = type.authToken == null ? null : type.authToken.trim();
        long tokenExpireAt = authType == AuthType.OFFLINE ? -1 : type.tokenExpireAt;

        String email = type.email == null ? null : type.email.trim();
        expectEmail(email);

        String password = type.password == null ? null : type.password.trim();

        if (email != null && password != null && authType != AuthType.OFFLINE && authToken == null) {
            try {
                return serverWrecker.authenticate(authType, email, password, null); // TODO: Implement proxy
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new JavaAccount(authType, username, profileId, authToken, tokenExpireAt);
    }

    public List<JavaAccount> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    @Override
    public void onSettingsChange(AccountList settings) {
        accounts.clear();
        accounts.addAll(settings.accounts());
    }

    @Override
    public AccountList collectSettings() {
        return new AccountList(List.copyOf(accounts));
    }

    @AllArgsConstructor
    @NoArgsConstructor
    private static class AccountJsonType {
        private String username;
        private String profileId;
        private String authType;
        private String authToken;
        private long tokenExpireAt;

        private String email;
        private String password;
    }
}
