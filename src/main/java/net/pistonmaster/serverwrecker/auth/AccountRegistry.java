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
import net.pistonmaster.serverwrecker.auth.service.*;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
public class AccountRegistry implements SettingsDuplex<AccountList> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<MinecraftAccount> accounts = new ArrayList<>();
    private final List<Runnable> loadHooks = new ArrayList<>();
    public static final Logger LOGGER = LoggerFactory.getLogger(AccountRegistry.class);

    public void loadFromFile(Path file, AuthType authType) throws IOException {
        loadFromString(Files.readString(file), authType);
    }

    public void loadFromString(String file, AuthType authType) {
        List<MinecraftAccount> newAccounts = new ArrayList<>();

        if (isSupportedJson(file)) {
            if (isArray(file)) {
                newAccounts.addAll(Arrays.stream(GSON.fromJson(file, AccountJsonType[].class))
                        .map(account -> fromJsonType(account, authType))
                        .toList());
            } else if (isObject(file)) {
                newAccounts.add(fromJsonType(GSON.fromJson(file, AccountJsonType.class), authType));
            } else {
                throw new IllegalArgumentException("Invalid JSON!");
            }
        } else {
            file.lines()
                    .filter(line -> !line.isBlank())
                    .distinct()
                    .map(account -> fromString(account, authType))
                    .forEach(newAccounts::add);
        }

        this.accounts.addAll(newAccounts);
        LOGGER.info("Loaded {} accounts!", newAccounts.size());
        loadHooks.forEach(Runnable::run);
    }

    private boolean isSupportedJson(String file) {
        try {
            JsonElement element = GSON.fromJson(file, JsonElement.class);
            return element.isJsonArray() || element.isJsonObject();
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

    private MinecraftAccount fromString(String account, AuthType authType) {
        account = account.trim();

        if (account.isBlank()) {
            throw new IllegalArgumentException("Account cannot be empty!");
        }

        String[] split = account.split(":");

        try {
            return switch (authType) {
                case OFFLINE -> {
                    if (account.contains(":")) {
                        throw new IllegalArgumentException("Invalid account!");
                    }

                    yield new MinecraftAccount(account);
                }
                case MICROSOFT_JAVA, MICROSOFT_BEDROCK -> {
                    if (split.length < 2) {
                        throw new IllegalArgumentException("Invalid account!");
                    }

                    String email = expectEmail(split[0].trim());
                    String password = split[1].trim();
                    if (authType == AuthType.MICROSOFT_JAVA) {
                        yield new SWJavaMicrosoftAuthService().login(email, password, null);
                    } else {
                        yield new SWBedrockMicrosoftAuthService().login(email, password, null);
                    }
                }
                case THE_ALTENING -> {
                    if (split.length < 1) {
                        throw new IllegalArgumentException("Invalid account!");
                    }

                    yield new SWTheAlteningAuthService().login(split[0].trim(), null);
                }
                case EASYMC -> {
                    if (split.length < 1) {
                        throw new IllegalArgumentException("Invalid account!");
                    }

                    yield new SWEasyMCAuthService().login(split[0].trim(), null);
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEmail(String account) {
        return EmailValidator.getInstance().isValid(account);
    }

    private String expectEmail(String account) {
        if (!isEmail(account)) {
            throw new IllegalArgumentException("Invalid email!");
        }

        return account;
    }

    private MinecraftAccount fromJsonType(AccountJsonType type, AuthType authType) {
        Objects.requireNonNull(type, "Account type cannot be null");

        String authToken = type.authToken == null ? null : type.authToken.trim();
        if (authToken != null) {
            String username = Objects.requireNonNull(type.username, "Username not found").trim();
            UUID profileId = type.profileId == null ? null : UUID.fromString(type.profileId.trim());
            long tokenExpireAt = authType == AuthType.OFFLINE ? -1 : type.tokenExpireAt;

            return new MinecraftAccount(authType, username, new JavaData(profileId, authToken, tokenExpireAt), true);
        }

        try {
            return switch (authType) {
                case MICROSOFT_JAVA -> new SWJavaMicrosoftAuthService().login(
                        expectEmail(Objects.requireNonNull(type.email, "E-Mail token cannot be null").trim()),
                        Objects.requireNonNull(type.password, "Password cannot be null").trim(),
                        null
                );
                case MICROSOFT_BEDROCK -> new SWBedrockMicrosoftAuthService().login(
                        expectEmail(Objects.requireNonNull(type.email, "E-Mail token cannot be null").trim()),
                        Objects.requireNonNull(type.password, "Password cannot be null").trim(),
                        null
                );
                case THE_ALTENING -> new SWTheAlteningAuthService().login(
                        Objects.requireNonNull(type.altToken, "Alt token cannot be null").trim(),
                        null
                );
                case EASYMC -> new SWEasyMCAuthService().login(
                        Objects.requireNonNull(type.altToken, "Alt token cannot be null").trim(),
                        null);
                case OFFLINE -> new MinecraftAccount(
                        Objects.requireNonNull(type.username, "Username not found").trim()
                );
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MinecraftAccount> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void setAccounts(List<MinecraftAccount> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    public List<MinecraftAccount> getUsableAccounts() {
        return accounts.stream()
                .filter(MinecraftAccount::enabled)
                .toList();
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

    public void addLoadHook(Runnable hook) {
        loadHooks.add(hook);
    }

    public MinecraftAccount getAccount(String username, AuthType authType) {
        return accounts.stream()
                .filter(account -> account.authType() == authType)
                .filter(account -> account.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    private static class AccountJsonType {
        // Prefilled fields, username filled for offline
        private String username;
        private String profileId;
        private String authToken;
        private long tokenExpireAt;

        // Java and bedrock
        private String email;
        private String password;

        // TheAltening and EasyMC
        private String altToken;
    }
}
