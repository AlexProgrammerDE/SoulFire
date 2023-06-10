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
    private final List<Runnable> loadHooks = new ArrayList<>();
    private final ServerWrecker serverWrecker;

    public void loadFromFile(Path file, AuthType authType) throws IOException {
        loadFromString(Files.readString(file), authType);
    }

    public void loadFromString(String file, AuthType authType) {
        List<JavaAccount> newAccounts = new ArrayList<>();

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
            String[] accountLines = file.split("\n");

            Arrays.stream(accountLines)
                    .filter(line -> !line.isBlank())
                    .distinct()
                    .map(account -> fromString(account, authType))
                    .forEach(newAccounts::add);
        }

        this.accounts.addAll(newAccounts);
        serverWrecker.getLogger().info("Loaded {} accounts!", newAccounts.size());
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

    private JavaAccount fromString(String account, AuthType authType) {
        account = account.trim();

        if (account.isBlank()) {
            throw new IllegalArgumentException("Account cannot be empty!");
        }

        String[] split = account.split(":");

        switch (authType) {
            case OFFLINE -> {
                if (account.contains(":")) {
                    throw new IllegalArgumentException("Invalid account!");
                }

                return new JavaAccount(account);
            }
            case MICROSOFT -> {
                if (split.length < 2) {
                    throw new IllegalArgumentException("Invalid account!");
                }

                String email = split[0].trim();
                expectEmail(email);

                String password = split[1].trim();

                try {
                    return new SWMicrosoftAuthService().login(email, password, null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case THE_ALTENING -> {
                if (split.length < 1) {
                    throw new IllegalArgumentException("Invalid account!");
                }

                String altToken = split[0].trim();
                expectEmail(altToken);

                try {
                    return new SWTheAlteningAuthService().login(altToken, null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Invalid auth type!");
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

    private JavaAccount fromJsonType(AccountJsonType type, AuthType authType) {
        Objects.requireNonNull(type, "Account type cannot be null");
        String username = Objects.requireNonNull(type.username, "Username not found").trim();
        UUID profileId = type.profileId == null ? null : UUID.fromString(type.profileId.trim());

        String authToken = type.authToken == null ? null : type.authToken.trim();
        long tokenExpireAt = authType == AuthType.OFFLINE ? -1 : type.tokenExpireAt;

        String email = type.email == null ? null : type.email.trim();
        expectEmail(email);

        String password = type.password == null ? null : type.password.trim();

        if (authToken != null) {
            return new JavaAccount(authType, username, profileId, authToken, tokenExpireAt, true);
        }

        switch (authType) {
            case MICROSOFT -> {
                try {
                    return new SWMicrosoftAuthService().login(email, password, null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case THE_ALTENING -> {
                try {
                    return new SWTheAlteningAuthService().login(email, null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> throw new IllegalArgumentException("Could not create a auth token!");
        }
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

    public void addLoadHook(Runnable hook) {
        loadHooks.add(hook);
    }

    public void setAccounts(List<JavaAccount> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    private static class AccountJsonType {
        private String username;
        private String profileId;
        private String authToken;
        private long tokenExpireAt;

        private String email;
        private String password;
    }
}
