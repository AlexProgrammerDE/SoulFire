/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server;

import com.google.common.util.concurrent.Striped;
import com.google.gson.JsonElement;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.api.PluginSettingsPageRegistration;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotConnectionRemovedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.api.metrics.InstancePluginStats;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.database.AuditLogType;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.metrics.InstanceMetricsCollector;
import com.soulfiremc.server.settings.instance.AISettings;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.PathfindingSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsPageRegistry;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.soulfiremc.shared.SFLogAppender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/// Represents a single instance.
/// An instance persists settings and provides a compartment for independently controlled bots.
@Slf4j
@Getter
public final class InstanceManager {
  private static final ScopedValue<InstanceManager> CURRENT = ScopedValue.newInstance();
  private final Striped<Lock> accountRefreshLocks = Striped.lock(64);
  private final Map<UUID, BotConnection> botConnections = new ConcurrentHashMap<>();
  private final BotControlLeaseManager botControlLeaseManager = new BotControlLeaseManager();
  private final MetadataHolder<Object> metadata = new MetadataHolder<>();
  private final MetadataHolder<JsonElement> persistentMetadata = new MetadataHolder<>();
  private final UUID id;
  private final SoulFireScheduler scheduler;
  private final InstanceSettingsDelegate settingsSource;
  private final SoulFireServer soulFireServer;
  private final DSLContext dsl;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;
  private final CachedLazyObject<String> friendlyNameCache;
  private final SettingsPageRegistry instanceSettingsPageRegistry;
  private final InstanceMetricsCollector metricsCollector;
  private final InstancePluginStats pluginStats;
  private final BotStateManager botStateManager;

  public static InstanceManager current() {
    var current = CURRENT.isBound() ? CURRENT.get() : null;
    if (!SFConstants.NOT_REGISTRY_INIT_PHASE) {
      return current;
    }

    if (current == null) {
      new RuntimeException().printStackTrace();
    }
    return Objects.requireNonNull(current, "No instance manager in current thread");
  }

  public static Optional<InstanceManager> currentOptional() {
    return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
  }

  public InstanceManager(SoulFireServer soulFireServer, DSLContext dsl, UUID id) {
    this.id = id;
    this.runnableWrapper = soulFireServer.runnableWrapper().with(new InstanceRunnableWrapper(this));
    this.scheduler = new SoulFireScheduler(runnableWrapper);
    this.soulFireServer = soulFireServer;
    this.dsl = dsl;
    this.settingsSource = new InstanceSettingsDelegate(new CachedLazyObject<>(this::fetchSettingsSource, 1, TimeUnit.SECONDS));
    this.friendlyNameCache = new CachedLazyObject<>(this::fetchFriendlyName, 1, TimeUnit.SECONDS);
    initPersistentMetadata();

    this.metricsCollector = new InstanceMetricsCollector(this);
    SoulFireAPI.registerListenersOfObject(metricsCollector);
    this.pluginStats = new InstancePluginStats();

    try {
      Files.createDirectories(getInstanceObjectStoragePath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    this.instanceSettingsPageRegistry = scheduler.supplyAsync(() -> {
      var registry = new SettingsPageRegistry()
        // Needs Via loaded to have all protocol versions
        .addInternalPage(BotSettings.class, "bot", "Bot Settings", "bot")
        .addInternalPage(AccountSettings.class, "account", "Account Settings", "users")
        .addInternalPage(ProxySettings.class, "proxy", "Proxy Settings", "waypoints")
        .addInternalPage(AISettings.class, "ai", "AI Settings", "sparkles")
        .addInternalPage(PathfindingSettings.class, "pathfinding", "Pathfinding Settings", "route");

      SoulFireAPI.pluginApis().applySettingsPages(
        PluginSettingsPageRegistration.Scope.INSTANCE,
        registry
      );
      SoulFireAPI.postEvent(new InstanceSettingsRegistryInitEvent(this, registry));

      return registry;
    }).join();

    this.botStateManager = new BotStateManager(this);
    this.scheduler.scheduleWithFixedDelay(this::tick, 0, 500, TimeUnit.MILLISECONDS);
    this.scheduler.scheduleWithFixedDelay(this::refreshExpiredAccounts, 0, 1, TimeUnit.HOURS);
    this.botStateManager.restoreDesiredBots();
  }

  private void initPersistentMetadata() {
    var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
    if (record != null) {
      var stem = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      persistentMetadata.resetFrom(stem.persistentMetadata());
    }
  }

  private InstanceSettingsSource fetchSettingsSource() {
    var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();

    if (record == null) {
      throw new IllegalStateException("Instance not found");
    } else {
      var stem = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      return new InstanceSettingsImpl(stem, soulFireServer.settingsSource());
    }
  }

  public void invalidateSettingsCache() {
    settingsSource.invalidate();
    friendlyNameCache.invalidate();
    for (var bot : botConnections.values()) {
      bot.invalidateSettingsCache();
    }
  }

  public void updateInstanceSetting(Property<SettingsSource.Instance> property, JsonElement value) {
    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
      if (record == null) {
        throw new IllegalStateException("Instance not found");
      }

      var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      var updatedSettings = SettingsSource.Stem.withUpdatedEntry(
        currentSettings.settings(),
        property.namespace(),
        property.key(),
        value);
      var updatedStem = currentSettings.withSettings(updatedSettings);

      ctx.update(Tables.INSTANCES)
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.GSON.toJson(updatedStem.serializeToTree()))
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
        .where(Tables.INSTANCES.ID.eq(id.toString()))
        .execute();
    });

    invalidateSettingsCache();
  }

  public void updateBotSetting(UUID botId, Property<SettingsSource.Bot> property, JsonElement value) {
    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
      if (record == null) {
        throw new IllegalStateException("Instance not found");
      }

      var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      var updatedStem = currentSettings.withAccounts(currentSettings.accounts().stream()
        .map(account -> {
          if (!account.profileId().equals(botId)) {
            return account;
          }

          var currentBotSettings = account.settings() == null ? Map.<String, Map<String, JsonElement>>of() : account.settings();
          var updatedBotSettings = SettingsSource.Stem.withUpdatedEntry(
            currentBotSettings,
            property.namespace(),
            property.key(),
            value);
          return account.withSettings(updatedBotSettings);
        })
        .toList());

      ctx.update(Tables.INSTANCES)
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.GSON.toJson(updatedStem.serializeToTree()))
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
        .where(Tables.INSTANCES.ID.eq(id.toString()))
        .execute();
    });

    invalidateSettingsCache();
  }

  private String fetchFriendlyName() {
    var record = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();

    if (record == null) {
      return "Unknown";
    } else {
      return record.getFriendlyName();
    }
  }

  private void tick() {
    metricsCollector.tick();
    persistDirtyMetadata();
    evictBots();
  }

  private void persistDirtyMetadata() {
    var dirtyBots = new ArrayList<Map.Entry<UUID, Map<String, Map<String, JsonElement>>>>();
    for (var entry : botConnections.entrySet()) {
      var bot = entry.getValue();
      bot.persistentMetadata().exportIfDirty().ifPresent(metadata -> {
        dirtyBots.add(Map.entry(entry.getKey(), metadata));
        bot.persistentMetadata().markClean();
      });
    }

    var dirtyInstanceMetadata = persistentMetadata.exportIfDirty();
    dirtyInstanceMetadata.ifPresent(_ -> persistentMetadata.markClean());

    if (dirtyBots.isEmpty() && dirtyInstanceMetadata.isEmpty()) {
      return;
    }

    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
      if (record == null) {
        return;
      }

      var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));

      if (!dirtyBots.isEmpty()) {
        var newAccounts = currentSettings.accounts().stream()
          .map(account -> {
            for (var dirtyEntry : dirtyBots) {
              if (account.profileId().equals(dirtyEntry.getKey())) {
                return account.withPersistentMetadata(dirtyEntry.getValue());
              }
            }
            return account;
          })
          .toList();
        currentSettings = currentSettings.withAccounts(newAccounts);
      }

      if (dirtyInstanceMetadata.isPresent()) {
        currentSettings = currentSettings.withPersistentMetadata(dirtyInstanceMetadata.get());
      }

      ctx.update(Tables.INSTANCES)
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.GSON.toJson(currentSettings.serializeToTree()))
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
        .where(Tables.INSTANCES.ID.eq(id.toString()))
        .execute();
    });
  }

  private void evictBots() {
    // Remove botConnections from the map that are closed
    for (var bot : List.copyOf(botConnections.values())) {
      if (bot.isDisconnected()) {
        removeBot(bot);
      }
    }
  }

  private void refreshExpiredAccounts() {
    for (var account : settingsSource.accounts().values()) {
      try {
        refreshAccount(account);
      } catch (Exception e) {
        // One expired or rate-limited account must not prevent other accounts
        // from refreshing, or discard credentials refreshed earlier in the batch.
        log.warn("Failed to refresh account {}", account.profileId(), e);
      }
    }
  }

  void persistAccountMetadataUpdates(Map<UUID, Map<String, Map<String, JsonElement>>> metadataUpdates) {
    if (metadataUpdates.isEmpty()) {
      return;
    }

    dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
      if (record == null) {
        return;
      }

      var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      var newAccounts = currentSettings.accounts().stream()
        .map(account -> {
          var updatedMetadata = metadataUpdates.get(account.profileId());
          return updatedMetadata != null ? account.withPersistentMetadata(updatedMetadata) : account;
        })
        .toList();

      ctx.update(Tables.INSTANCES)
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.GSON.toJson(currentSettings.withAccounts(newAccounts).serializeToTree()))
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
        .where(Tables.INSTANCES.ID.eq(id.toString()))
        .execute();
    });
  }

  MinecraftAccount refreshAccount(MinecraftAccount account) {
    var lock = accountRefreshLocks.get(account.profileId());
    lock.lock();
    try {
      // A concurrent connection or the periodic refresh may have refreshed this
      // account already. Read persisted credentials after acquiring its lock.
      var currentAccount = fetchSettingsSource().accounts().get(account.profileId());
      if (currentAccount == null) {
        throw new IllegalStateException("Account was removed before authentication");
      }
      var authService = MCAuthService.convertService(currentAccount.authType());
      if (!authService.isExpired(currentAccount)) {
        return currentAccount;
      }

      log.info("Refreshing expired account {}", currentAccount.lastKnownName());
      var refreshedAccount = authService.refresh(
        currentAccount,
        settingsSource.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH)
          ? SFHelpers.getRandomEntry(settingsSource.proxies()) : null,
        scheduler
      ).join();
      var persistedAccount = dsl.transactionResult(cfg -> {
        var ctx = DSL.using(cfg);
        var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id.toString())).fetchOne();
        if (record == null) {
          throw new IllegalStateException("Instance was removed during authentication");
        }

        var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
        var accounts = mergeRefreshedAccount(currentSettings.accounts(), currentAccount, refreshedAccount);
        var result = accounts.stream()
          .filter(a -> a.profileId().equals(account.profileId()))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Account was removed during authentication"));
        ctx.update(Tables.INSTANCES)
          .set(Tables.INSTANCES.SETTINGS, GsonInstance.GSON.toJson(currentSettings.withAccounts(accounts).serializeToTree()))
          .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
          .where(Tables.INSTANCES.ID.eq(id.toString()))
          .execute();
        return result;
      });
      invalidateSettingsCache();
      return persistedAccount;
    } finally {
      lock.unlock();
    }
  }

  /// Applies refreshed credentials without restoring removed accounts or overwriting
  /// account edits made while the authentication request was in flight.
  static List<MinecraftAccount> mergeRefreshedAccount(
    List<MinecraftAccount> accounts,
    MinecraftAccount original,
    MinecraftAccount refreshed
  ) {
    if (!original.profileId().equals(refreshed.profileId()) || original.authType() != refreshed.authType()) {
      throw new IllegalArgumentException("Refreshed account identity does not match");
    }
    return accounts.stream().map(account -> {
      if (!account.profileId().equals(original.profileId())
        || account.authType() != original.authType()
        || !account.accountData().equals(original.accountData())) {
        return account;
      }
      return account.withAccountData(refreshed.accountData()).withLastKnownName(refreshed.lastKnownName());
    }).toList();
  }

  public CompletableFuture<?> deleteInstance() {
    return botStateManager.shutdown()
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler())
      .thenRunAsync(() -> {
        try {
          SFHelpers.deleteDirectory(getInstanceObjectStoragePath());
        } catch (IOException e) {
          log.error("Error while deleting instance storage", e);
        }
      }, soulFireServer.scheduler());
  }

  public CompletableFuture<?> shutdownHook() {
    return botStateManager.shutdown()
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler());
  }

  void removeBot(BotConnection bot) {
    if (botConnections.remove(bot.accountProfileId(), bot)) {
      log.debug("Removing bot {}", bot.accountName());
      botStateManager.connectionRemoved(bot);
      SoulFireAPI.postEvent(new BotConnectionRemovedEvent(bot));
    }
  }

  public void addAuditLog(SoulFireUser source, AuditLogType logType, @Nullable String data) {
    scheduler.execute(() -> dsl.transaction(cfg -> {
      var ctx = DSL.using(cfg);
      // Verify instance and user exist
      var instanceExists = ctx.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(id.toString()));
      var userExists = ctx.fetchExists(Tables.USERS, Tables.USERS.ID.eq(source.getUniqueId().toString()));
      if (!instanceExists || !userExists) {
        return;
      }

      var now = LocalDateTime.now(ZoneOffset.UTC);
      ctx.insertInto(Tables.INSTANCE_AUDIT_LOGS)
        .set(Tables.INSTANCE_AUDIT_LOGS.ID, UUID.randomUUID().toString())
        .set(Tables.INSTANCE_AUDIT_LOGS.TYPE, logType.name())
        .set(Tables.INSTANCE_AUDIT_LOGS.DATA, data)
        .set(Tables.INSTANCE_AUDIT_LOGS.INSTANCE_ID, id.toString())
        .set(Tables.INSTANCE_AUDIT_LOGS.USER_ID, source.getUniqueId().toString())
        .set(Tables.INSTANCE_AUDIT_LOGS.CREATED_AT, now)
        .set(Tables.INSTANCE_AUDIT_LOGS.UPDATED_AT, now)
        .set(Tables.INSTANCE_AUDIT_LOGS.VERSION, 0L)
        .execute();
    }));
  }

  public Path getInstanceObjectStoragePath() {
    return soulFireServer.getObjectStoragePath().resolve("instance-" + id.toString());
  }

  public List<BotConnection> getConnectedBots() {
    return new ArrayList<>(botConnections.values());
  }

  public boolean storeNewBot(BotConnection connection) {
    return botConnections.putIfAbsent(connection.accountProfileId(), connection) == null;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof InstanceManager that)) {
      return false;
    }

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  private record InstanceRunnableWrapper(InstanceManager instanceManager) implements SoulFireScheduler.RunnableWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
      return () -> ScopedValue.where(CURRENT, instanceManager).run(() -> {
        try (
          var _ = SFHelpers.smartMDCCloseable(SFLogAppender.SF_INSTANCE_ID, instanceManager.id().toString());
          var _ = SFHelpers.smartMDCCloseable(SFLogAppender.SF_INSTANCE_NAME, instanceManager.friendlyNameCache().get())) {
          runnable.run();
        }
      });
    }
  }
}
