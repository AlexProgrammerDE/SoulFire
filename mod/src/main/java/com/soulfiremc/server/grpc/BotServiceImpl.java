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
package com.soulfiremc.server.grpc;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.renderer.RenderConstants;
import com.soulfiremc.server.renderer.SoftwareRenderer;
import com.soulfiremc.server.settings.lib.BotSettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.player.LocalPlayer;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class BotServiceImpl extends BotServiceGrpc.BotServiceImplBase {
  private final SoulFireServer soulFireServer;

  /**
   * Builds a BotLiveState from a LocalPlayer.
   *
   * @param player           The player to extract data from
   * @param includeInventory Whether to include full inventory data (more expensive)
   * @return The built BotLiveState
   */
  private static BotLiveState buildLiveState(LocalPlayer player, boolean includeInventory) {
    var builder = BotLiveState.newBuilder()
      .setX(player.getX())
      .setY(player.getY())
      .setZ(player.getZ())
      .setXRot(player.getXRot())
      .setYRot(player.getYRot())
      .setHealth(player.getHealth())
      .setMaxHealth(player.getMaxHealth())
      .setFoodLevel(player.getFoodData().getFoodLevel())
      .setSaturationLevel(player.getFoodData().getSaturationLevel())
      .setSelectedHotbarSlot(player.getInventory().getSelectedSlot())
      .setExperienceLevel(player.experienceLevel)
      .setExperienceProgress(player.experienceProgress);

    // Add dimension
    var dimension = player.level().dimension().identifier().toString();
    builder.setDimension(dimension);

    // Add skin texture hash if available
    var skinHash = extractSkinTextureHash(player.getGameProfile());
    if (skinHash != null) {
      builder.setSkinTextureHash(skinHash);
    }

    // Add inventory items if requested (only non-empty slots)
    if (includeInventory) {
      var container = player.inventoryMenu;
      for (var slot : container.slots) {
        var item = slot.getItem();
        if (!item.isEmpty()) {
          var slotBuilder = InventorySlot.newBuilder()
            .setSlot(slot.index)
            .setItemId(item.getItemHolder().getRegisteredName())
            .setCount(item.getCount());

          // Check for custom display name
          var displayName = item.getHoverName();
          if (item.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            slotBuilder.setDisplayName(displayName.getString());
          }

          builder.addInventory(slotBuilder.build());
        }
      }
    }

    return builder.build();
  }

  private static String toBase64PNG(java.awt.image.BufferedImage image) {
    try (var os = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", os);
      return Base64.getEncoder().encodeToString(os.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to convert image to base64", e);
    }
  }

  /**
   * Extracts the skin texture hash from a GameProfile.
   * The hash is the ID at the end of the texture URL like:
   * http://textures.minecraft.net/texture/abc123def456...
   *
   * @param profile The player's game profile
   * @return The texture hash, or null if not available
   */
  private static String extractSkinTextureHash(GameProfile profile) {
    try {
      var properties = profile.properties();
      if (properties == null) {
        return null;
      }

      var texturesProperty = properties.get("textures");
      if (texturesProperty == null || texturesProperty.isEmpty()) {
        return null;
      }

      var property = texturesProperty.iterator().next();
      var base64Value = property.value();
      if (base64Value == null || base64Value.isEmpty()) {
        return null;
      }

      // Decode base64
      var decoded = new String(Base64.getDecoder().decode(base64Value));

      // Parse JSON to get the skin URL
      var json = JsonParser.parseString(decoded).getAsJsonObject();
      var textures = json.getAsJsonObject("textures");
      if (textures == null) {
        return null;
      }

      var skin = textures.getAsJsonObject("SKIN");
      if (skin == null) {
        return null;
      }

      var url = skin.get("url").getAsString();
      if (url == null || url.isEmpty()) {
        return null;
      }

      // Extract hash from URL (last segment after /texture/)
      var lastSlash = url.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < url.length() - 1) {
        return url.substring(lastSlash + 1);
      }

      return null;
    } catch (Exception e) {
      log.debug("Failed to extract skin texture hash", e);
      return null;
    }
  }

  @Override
  public void updateBotConfig(BotUpdateConfigRequest request, StreamObserver<BotUpdateConfigResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        instanceEntity.settings(instanceEntity.settings().withAccounts(instanceEntity.settings().accounts().stream()
          .map(minecraftAccount -> {
            if (minecraftAccount.profileId().equals(botId)) {
              return minecraftAccount.withSettingsStem(BotSettingsImpl.Stem.fromProto(request.getConfig()));
            } else {
              return minecraftAccount;
            }
          })
          .toList()));

        session.merge(instanceEntity);
      });

      responseObserver.onNext(BotUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot config", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateBotConfigEntry(BotUpdateConfigEntryRequest request, StreamObserver<BotUpdateConfigEntryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        instanceEntity.settings(instanceEntity.settings().withAccounts(instanceEntity.settings().accounts().stream()
          .map(minecraftAccount -> {
            if (minecraftAccount.profileId().equals(botId)) {
              var currentStem = minecraftAccount.settingsStem() == null ? BotSettingsImpl.Stem.EMPTY : minecraftAccount.settingsStem();
              var newSettings = SettingsSource.Stem.withUpdatedEntry(
                currentStem.settings(),
                request.getNamespace(),
                request.getKey(),
                SettingsSource.Stem.valueToJsonElement(request.getValue())
              );
              return minecraftAccount.withSettingsStem(currentStem.withSettings(newSettings));
            } else {
              return minecraftAccount;
            }
          })
          .toList()));

        session.merge(instanceEntity);
      });

      responseObserver.onNext(BotUpdateConfigEntryResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating bot config entry", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getBotList(BotListRequest request, StreamObserver<BotListResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      var botConnections = instance.botConnections();

      var responseBuilder = BotListResponse.newBuilder();
      for (var account : instance.settingsSource().accounts().values()) {
        var profileId = account.profileId();
        var entryBuilder = BotListEntry.newBuilder()
          .setProfileId(profileId.toString())
          .setIsOnline(botConnections.containsKey(profileId));

        var activeBot = botConnections.get(profileId);
        if (activeBot != null) {
          var player = activeBot.minecraft().player;
          if (player != null) {
            // Don't include inventory for list view (too expensive)
            entryBuilder.setLiveState(buildLiveState(player, false));
          }
        }
        responseBuilder.addBots(entryBuilder.build());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting bot list", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getBotInfo(BotInfoRequest request, StreamObserver<BotInfoResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      var account = instance.settingsSource().accounts().get(botId);
      if (account == null) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Bot '%s' not found in instance '%s'".formatted(botId, instanceId)));
      }

      var settingsStem = account.settingsStem() == null ? BotSettingsImpl.Stem.EMPTY : account.settingsStem();
      var botInfoResponseBuilder = BotInfoResponse.newBuilder()
        .setConfig(settingsStem.toProto());
      var activeBot = instance.botConnections().get(botId);
      if (activeBot != null) {
        var minecraft = activeBot.minecraft();
        var player = minecraft.player;
        if (player != null) {
          // Include full inventory data for detail view
          botInfoResponseBuilder.setLiveState(buildLiveState(player, true));
        }
      }

      responseObserver.onNext(botInfoResponseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void renderBotPov(BotRenderPovRequest request, StreamObserver<BotRenderPovResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      var activeBot = instance.botConnections().get(botId);
      if (activeBot == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot '%s' is not online".formatted(botId)));
      }

      var minecraft = activeBot.minecraft();
      var player = minecraft.player;
      var level = minecraft.level;
      if (player == null || level == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot player or level is not available"));
      }

      // Use provided dimensions or defaults
      var width = request.getWidth() > 0 ? request.getWidth() : RenderConstants.DEFAULT_WIDTH;
      var height = request.getHeight() > 0 ? request.getHeight() : RenderConstants.DEFAULT_HEIGHT;

      // Clamp dimensions to reasonable values
      width = Math.min(Math.max(width, 1), 1920);
      height = Math.min(Math.max(height, 1), 1080);

      // Get render distance from settings (in chunks) and convert to blocks
      var renderDistanceChunks = minecraft.options.getEffectiveRenderDistance();
      var maxDistance = renderDistanceChunks * 16;

      // Render the POV
      var image = SoftwareRenderer.render(
        level,
        player,
        width,
        height,
        RenderConstants.DEFAULT_FOV,
        maxDistance
      );

      // Convert to base64 PNG
      var base64Image = toBase64PNG(image);

      responseObserver.onNext(BotRenderPovResponse.newBuilder()
        .setImageBase64(base64Image)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error rendering bot POV", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
