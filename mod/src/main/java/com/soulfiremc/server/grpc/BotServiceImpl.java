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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.*;

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

  @Override
  public void clickInventorySlot(BotInventoryClickRequest request, StreamObserver<BotInventoryClickResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

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
      var gameMode = minecraft.gameMode;
      if (player == null || gameMode == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot player or gameMode is not available"));
      }

      // Map proto ClickType to Minecraft ClickType and mouse button
      var container = player.containerMenu;
      var slotId = request.getSlot();
      int mouseButton;
      net.minecraft.world.inventory.ClickType clickType;

      switch (request.getClickType()) {
        case LEFT_CLICK -> {
          mouseButton = 0;
          clickType = net.minecraft.world.inventory.ClickType.PICKUP;
        }
        case RIGHT_CLICK -> {
          mouseButton = 1;
          clickType = net.minecraft.world.inventory.ClickType.PICKUP;
        }
        case SHIFT_LEFT_CLICK -> {
          mouseButton = 0;
          clickType = net.minecraft.world.inventory.ClickType.QUICK_MOVE;
        }
        case DROP_ONE -> {
          mouseButton = 0;
          clickType = net.minecraft.world.inventory.ClickType.THROW;
        }
        case DROP_ALL -> {
          mouseButton = 1;
          clickType = net.minecraft.world.inventory.ClickType.THROW;
        }
        case SWAP_HOTBAR -> {
          mouseButton = request.getHotbarSlot(); // 0-8 for hotbar slots
          clickType = net.minecraft.world.inventory.ClickType.SWAP;
        }
        case MIDDLE_CLICK -> {
          mouseButton = 2;
          clickType = net.minecraft.world.inventory.ClickType.CLONE;
        }
        default -> {
          responseObserver.onNext(BotInventoryClickResponse.newBuilder()
            .setSuccess(false)
            .setError("Invalid click type")
            .build());
          responseObserver.onCompleted();
          return;
        }
      }

      // Perform the click
      gameMode.handleInventoryMouseClick(container.containerId, slotId, mouseButton, clickType, player);

      responseObserver.onNext(BotInventoryClickResponse.newBuilder()
        .setSuccess(true)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error clicking inventory slot", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  /**
   * Builds a ContainerLayout describing the slot regions for the given menu.
   * This allows the frontend to render any container type dynamically.
   */
  private static ContainerLayout buildContainerLayout(AbstractContainerMenu menu, String title) {
    var layoutBuilder = ContainerLayout.newBuilder()
      .setTitle(title)
      .setTotalSlots(menu.slots.size());

    // Player inventory always has these standard regions at the end
    // The slot indices vary based on container type, but layout is consistent
    int playerInvStart;
    int hotbarStart;

    if (menu instanceof InventoryMenu) {
      // Player inventory screen layout:
      // 0: crafting output
      // 1-4: crafting grid (2x2)
      // 5-8: armor slots
      // 9-35: main inventory (27 slots, 3 rows of 9)
      // 36-44: hotbar (9 slots)
      // 45: offhand

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("crafting_output")
        .setLabel("Crafting")
        .setStartIndex(0)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("crafting_grid")
        .setLabel("Crafting Grid")
        .setStartIndex(1)
        .setSlotCount(4)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("armor")
        .setLabel("Armor")
        .setStartIndex(5)
        .setSlotCount(4)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_ARMOR)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("offhand")
        .setLabel("Offhand")
        .setStartIndex(45)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 9;
      hotbarStart = 36;

    } else if (menu instanceof ChestMenu chestMenu) {
      // Chest layout: container slots first, then player inventory
      int containerSize = chestMenu.getRowCount() * 9;
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("container")
        .setLabel(title)
        .setStartIndex(0)
        .setSlotCount(containerSize)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = containerSize;
      hotbarStart = containerSize + 27;

    } else if (menu instanceof DispenserMenu) {
      // Dispenser/Dropper: 9 slots in 3x3 grid
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("container")
        .setLabel(title)
        .setStartIndex(0)
        .setSlotCount(9)
        .setColumns(3)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 9;
      hotbarStart = 36;

    } else if (menu instanceof HopperMenu) {
      // Hopper: 5 slots in a row
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("container")
        .setLabel(title)
        .setStartIndex(0)
        .setSlotCount(5)
        .setColumns(5)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 5;
      hotbarStart = 32;

    } else if (menu instanceof AbstractFurnaceMenu) {
      // Furnace/Blast Furnace/Smoker: input, fuel, output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Input")
        .setStartIndex(0)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("fuel")
        .setLabel("Fuel")
        .setStartIndex(1)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Output")
        .setStartIndex(2)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 3;
      hotbarStart = 30;

    } else if (menu instanceof CraftingMenu) {
      // Crafting table: output + 3x3 grid
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("crafting_output")
        .setLabel("Result")
        .setStartIndex(0)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("crafting_grid")
        .setLabel("Crafting Grid")
        .setStartIndex(1)
        .setSlotCount(9)
        .setColumns(3)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 10;
      hotbarStart = 37;

    } else if (menu instanceof AnvilMenu) {
      // Anvil: 2 inputs + 1 output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Items")
        .setStartIndex(0)
        .setSlotCount(2)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(2)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 3;
      hotbarStart = 30;

    } else if (menu instanceof EnchantmentMenu) {
      // Enchanting table: item + lapis
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("enchant")
        .setLabel("Enchant")
        .setStartIndex(0)
        .setSlotCount(2)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 2;
      hotbarStart = 29;

    } else if (menu instanceof BrewingStandMenu) {
      // Brewing stand: 3 potions + ingredient + fuel
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("potions")
        .setLabel("Potions")
        .setStartIndex(0)
        .setSlotCount(3)
        .setColumns(3)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("ingredient")
        .setLabel("Ingredient")
        .setStartIndex(3)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("fuel")
        .setLabel("Blaze Powder")
        .setStartIndex(4)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 5;
      hotbarStart = 32;

    } else if (menu instanceof BeaconMenu) {
      // Beacon: 1 payment slot
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("payment")
        .setLabel("Payment")
        .setStartIndex(0)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 1;
      hotbarStart = 28;

    } else if (menu instanceof ShulkerBoxMenu) {
      // Shulker box: 27 slots like a small chest
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("container")
        .setLabel(title)
        .setStartIndex(0)
        .setSlotCount(27)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      playerInvStart = 27;
      hotbarStart = 54;

    } else if (menu instanceof GrindstoneMenu) {
      // Grindstone: 2 inputs + 1 output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Items")
        .setStartIndex(0)
        .setSlotCount(2)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(2)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 3;
      hotbarStart = 30;

    } else if (menu instanceof StonecutterMenu) {
      // Stonecutter: 1 input + 1 output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Input")
        .setStartIndex(0)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(1)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 2;
      hotbarStart = 29;

    } else if (menu instanceof LoomMenu) {
      // Loom: banner + dye + pattern + output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Materials")
        .setStartIndex(0)
        .setSlotCount(3)
        .setColumns(3)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(3)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 4;
      hotbarStart = 31;

    } else if (menu instanceof CartographyTableMenu) {
      // Cartography table: map + material + output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Materials")
        .setStartIndex(0)
        .setSlotCount(2)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(2)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 3;
      hotbarStart = 30;

    } else if (menu instanceof SmithingMenu) {
      // Smithing table: template + base + addition + output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("input")
        .setLabel("Materials")
        .setStartIndex(0)
        .setSlotCount(3)
        .setColumns(3)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("output")
        .setLabel("Result")
        .setStartIndex(3)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 4;
      hotbarStart = 31;

    } else if (menu instanceof MerchantMenu) {
      // Villager trading: 2 inputs + 1 output
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("trade_input")
        .setLabel("Trade")
        .setStartIndex(0)
        .setSlotCount(2)
        .setColumns(2)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("trade_output")
        .setLabel("Result")
        .setStartIndex(2)
        .setSlotCount(1)
        .setColumns(1)
        .setType(SlotRegionType.SLOT_REGION_OUTPUT)
        .build());

      playerInvStart = 3;
      hotbarStart = 30;

    } else {
      // Generic fallback for unknown containers
      // Just show all container slots, then player inventory
      int containerSlots = menu.slots.size() - 36; // Assume 36 player slots at end
      if (containerSlots > 0) {
        layoutBuilder.addRegions(SlotRegion.newBuilder()
          .setId("container")
          .setLabel(title)
          .setStartIndex(0)
          .setSlotCount(containerSlots)
          .setColumns(9) // Default to 9 columns
          .setType(SlotRegionType.SLOT_REGION_NORMAL)
          .build());
      }
      playerInvStart = Math.max(0, containerSlots);
      hotbarStart = playerInvStart + 27;
    }

    // Add player inventory regions (common to all containers except pure player inventory)
    if (!(menu instanceof InventoryMenu)) {
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("player_inventory")
        .setLabel("Inventory")
        .setStartIndex(playerInvStart)
        .setSlotCount(27)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("player_hotbar")
        .setLabel("Hotbar")
        .setStartIndex(hotbarStart)
        .setSlotCount(9)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_HOTBAR)
        .build());
    } else {
      // For player inventory, add main and hotbar
      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("player_inventory")
        .setLabel("Inventory")
        .setStartIndex(playerInvStart)
        .setSlotCount(27)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_NORMAL)
        .build());

      layoutBuilder.addRegions(SlotRegion.newBuilder()
        .setId("player_hotbar")
        .setLabel("Hotbar")
        .setStartIndex(hotbarStart)
        .setSlotCount(9)
        .setColumns(9)
        .setType(SlotRegionType.SLOT_REGION_HOTBAR)
        .build());
    }

    return layoutBuilder.build();
  }

  /**
   * Gets the display title for a container menu.
   */
  private static String getContainerTitle(AbstractContainerMenu menu, Minecraft minecraft) {
    // For player inventory, use a standard title
    if (menu instanceof InventoryMenu) {
      return "Inventory";
    }

    // Try to get the title from the screen if available
    if (minecraft.screen instanceof AbstractContainerScreen<?> containerScreen) {
      var title = containerScreen.getTitle();
      if (title != null) {
        return title.getString();
      }
    }

    // Fallback to class name
    return menu.getClass().getSimpleName().replace("Menu", "");
  }

  @Override
  public void getInventoryState(BotInventoryStateRequest request, StreamObserver<BotInventoryStateResponse> responseObserver) {
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
      if (player == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot player is not available"));
      }

      var container = player.containerMenu;
      var title = getContainerTitle(container, minecraft);
      var layout = buildContainerLayout(container, title);

      var responseBuilder = BotInventoryStateResponse.newBuilder()
        .setLayout(layout)
        .setSelectedHotbarSlot(player.getInventory().getSelectedSlot());

      // Add all non-empty slots
      for (var slot : container.slots) {
        var item = slot.getItem();
        if (!item.isEmpty()) {
          var slotBuilder = InventorySlot.newBuilder()
            .setSlot(slot.index)
            .setItemId(item.getItemHolder().getRegisteredName())
            .setCount(item.getCount());

          if (item.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            slotBuilder.setDisplayName(item.getHoverName().getString());
          }

          responseBuilder.addSlots(slotBuilder.build());
        }
      }

      // Add carried item if present
      var carried = container.getCarried();
      if (!carried.isEmpty()) {
        var carriedBuilder = InventorySlot.newBuilder()
          .setSlot(-1) // -1 indicates carried item
          .setItemId(carried.getItemHolder().getRegisteredName())
          .setCount(carried.getCount());

        if (carried.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
          carriedBuilder.setDisplayName(carried.getHoverName().getString());
        }

        responseBuilder.setCarriedItem(carriedBuilder.build());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting inventory state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void closeContainer(BotCloseContainerRequest request, StreamObserver<BotCloseContainerResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

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
      if (player == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot player is not available"));
      }

      // Close any open container
      player.closeContainer();

      responseObserver.onNext(BotCloseContainerResponse.newBuilder()
        .setSuccess(true)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error closing container", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void openInventory(BotOpenInventoryRequest request, StreamObserver<BotOpenInventoryResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getInstanceId());
    var botId = UUID.fromString(request.getBotId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_BOT_CONFIG, instanceId));

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
      if (player == null) {
        throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Bot player is not available"));
      }

      // Open player inventory (sends inventory open packet to server)
      player.sendOpenInventory();

      responseObserver.onNext(BotOpenInventoryResponse.newBuilder()
        .setSuccess(true)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error opening inventory", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
