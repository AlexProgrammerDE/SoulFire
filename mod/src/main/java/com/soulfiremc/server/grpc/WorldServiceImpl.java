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

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotThreadExecution;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.SFEntityHelpers;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/// Structured, bounded queries over the world currently observed by a bot.
@RequiredArgsConstructor
public final class WorldServiceImpl extends WorldServiceGrpc.WorldServiceImplBase {
  private static final int MAX_BLOCK_RADIUS = 128;
  private static final long MAX_BLOCK_VOLUME = 4_194_304;
  private static final float MAX_ENTITY_RADIUS = 256;
  private static final float MAX_EXPLOSION_POWER = 128;
  private static final int MAX_PAGE_SIZE = 500;

  private final SoulFireServer server;

  @Override
  public void getPlayerSnapshot(
    GetPlayerSnapshotRequest request,
    StreamObserver<GetPlayerSnapshotResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> {
        var level = requireLevel(bot);
        var player = Objects.requireNonNull(
          bot.minecraft().player,
          "Bot player is not available"
        );
        return GetPlayerSnapshotResponse.newBuilder()
          .setPlayer(MinecraftDomainMapper.player(
            bot,
            player,
            level,
            Math.max(0, level.getGameTime())
          ))
          .build();
      });
    });
  }

  @Override
  public void getWorldBlock(
    GetWorldBlockRequest request,
    StreamObserver<GetWorldBlockResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> {
        var level = requireLevel(bot);
        var position = blockPosition(request.getPosition(), level);
        requireLoaded(level, position);
        return GetWorldBlockResponse.newBuilder()
          .setBlock(MinecraftDomainMapper.block(
            level,
            position,
            level.getBlockState(position),
            request.getIncludeBlockEntity(),
            request.getIncludeShapes()
          ))
          .build();
      });
    });
  }

  @Override
  public void queryBlocks(
    QueryBlocksRequest request,
    StreamObserver<QueryBlocksResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> queryBlocks(bot, request));
    });
  }

  @Override
  public void getWorldEntity(
    GetWorldEntityRequest request,
    StreamObserver<GetWorldEntityResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> {
        requireEpoch(bot, request.getEntity());
        var entity = requireLevel(bot).getEntity(request.getEntity().getNetworkId());
        if (entity == null
          || request.getEntity().hasUuid()
          && !entity.getUUID().toString().equals(request.getEntity().getUuid())) {
          throw Status.NOT_FOUND
            .withDescription("Entity is no longer observable")
            .asRuntimeException();
        }
        return GetWorldEntityResponse.newBuilder()
          .setEntity(MinecraftDomainMapper.entity(bot, entity))
          .build();
      });
    });
  }

  @Override
  public void queryEntities(
    QueryEntitiesRequest request,
    StreamObserver<QueryEntitiesResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> queryEntities(bot, request));
    });
  }

  @Override
  public void raycast(
    RaycastRequest request,
    StreamObserver<RaycastResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> raycast(bot, request));
    });
  }

  @Override
  public void canSeeBlock(
    CanSeeBlockRequest request,
    StreamObserver<CanSeeBlockResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> canSeeBlock(bot, request));
    });
  }

  @Override
  public void estimateDigTime(
    EstimateDigTimeRequest request,
    StreamObserver<EstimateDigTimeResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> estimateDigTime(bot, request));
    });
  }

  @Override
  public void estimateExplosionDamage(
    EstimateExplosionDamageRequest request,
    StreamObserver<EstimateExplosionDamageResponse> responseObserver
  ) {
    unary(responseObserver, () -> {
      var bot = requireReadableBot(request.getInstanceId(), request.getBotId());
      return inBot(bot, () -> estimateExplosionDamage(bot, request));
    });
  }

  private QueryBlocksResponse queryBlocks(
    BotConnection bot,
    QueryBlocksRequest request
  ) {
    var level = requireLevel(bot);
    var player = Objects.requireNonNull(bot.minecraft().player);
    var bounds = bounds(request.getRegion(), level, player.position());
    var selector = request.getSelector();
    var page = blockPage(
      bounds,
      request.getSort(),
      request.getPageSize(),
      request.getPageToken(),
      position -> SFBlockHelpers.isChunkLoaded(level, position)
        && matchesBlock(level, position, level.getBlockState(position), selector, player)
    );
    return QueryBlocksResponse.newBuilder()
      .addAllBlocks(page.values.stream()
        .map(match -> MinecraftDomainMapper.block(
          level,
          match.position,
          level.getBlockState(match.position),
          selector.getIncludeBlockEntity(),
          false
        ))
        .toList())
      .setNextPageToken(page.nextToken)
      .setWorldRevision(Math.max(0, level.getGameTime()))
      .build();
  }

  private QueryEntitiesResponse queryEntities(
    BotConnection bot,
    QueryEntitiesRequest request
  ) {
    var level = requireLevel(bot);
    var player = Objects.requireNonNull(bot.minecraft().player);
    var origin = request.hasOrigin()
      ? worldPosition(request.getOrigin(), level)
      : player.position();
    var radius = request.getRadius() <= 0
      ? 32
      : Math.min(request.getRadius(), MAX_ENTITY_RADIUS);
    var selector = request.getSelector();
    var matches = StreamSupport.stream(
        level.entitiesForRendering().spliterator(),
        false
      )
      .filter(entity -> entity != player)
      .filter(entity -> entity.position().distanceToSqr(origin) <= radius * radius)
      .filter(entity -> matchesEntity(bot, entity, selector, origin))
      .map(entity -> new EntityMatch(
        entity,
        entity.position().distanceToSqr(origin)
      ))
      .sorted(entityComparator(request.getSort()))
      .toList();
    var page = page(matches, request.getPageSize(), request.getPageToken());
    return QueryEntitiesResponse.newBuilder()
      .addAllEntities(page.values.stream()
        .map(match -> MinecraftDomainMapper.entity(bot, match.entity))
        .toList())
      .setNextPageToken(page.nextToken)
      .setWorldRevision(Math.max(0, level.getGameTime()))
      .build();
  }

  private RaycastResponse raycast(BotConnection bot, RaycastRequest request) {
    var level = requireLevel(bot);
    var player = Objects.requireNonNull(bot.minecraft().player);
    var origin = request.hasOrigin()
      ? worldPosition(request.getOrigin(), level)
      : player.getEyePosition();
    var direction = request.hasDirection()
      ? new Vec3(
      request.getDirection().getX(),
      request.getDirection().getY(),
      request.getDirection().getZ()
    )
      : player.getViewVector(1.0F);
    if (direction.lengthSqr() < 1.0E-12) {
      throw Status.INVALID_ARGUMENT
        .withDescription("direction must not be zero")
        .asRuntimeException();
    }
    var distance = request.getMaximumDistance() <= 0
      ? 6
      : Math.min(request.getMaximumDistance(), MAX_ENTITY_RADIUS);
    var end = origin.add(direction.normalize().scale(distance));
    var blockHit = level.clip(new ClipContext(
      origin,
      end,
      ClipContext.Block.OUTLINE,
      request.getIncludeFluids() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
      player
    ));
    var blockDistance = blockHit.getType() == HitResult.Type.MISS
      ? Double.POSITIVE_INFINITY
      : blockHit.getLocation().distanceTo(origin);
    var entityHit = request.getIncludeEntities()
      ? ProjectileUtil.getEntityHitResult(
        level,
        player,
        origin,
        end,
        new AABB(origin, end).inflate(1),
        entity -> entity != player && entity.isPickable(),
        (float) distance
      )
      : null;
    var entityDistance = entityHit == null
      ? Double.POSITIVE_INFINITY
      : entityHit.getLocation().distanceTo(origin);
    var builder = RaycastResponse.newBuilder();
    if (entityDistance < blockDistance) {
      builder
        .setEntity(MinecraftDomainMapper.entity(bot, entityHit.getEntity()))
        .setHitPosition(MinecraftDomainMapper.worldPosition(
          entityHit.getLocation(),
          level.dimension().identifier().toString()
        ))
        .setDistance((float) entityDistance);
    } else if (blockDistance != Double.POSITIVE_INFINITY) {
      builder
        .setBlock(MinecraftDomainMapper.block(
          level,
          blockHit.getBlockPos(),
          level.getBlockState(blockHit.getBlockPos()),
          false,
          true
        ))
        .setHitPosition(MinecraftDomainMapper.worldPosition(
          blockHit.getLocation(),
          level.dimension().identifier().toString()
        ))
        .setBlockFace(blockFace(blockHit))
        .setDistance((float) blockDistance);
    }
    return builder.build();
  }

  private static CanSeeBlockResponse canSeeBlock(
    BotConnection bot,
    CanSeeBlockRequest request
  ) {
    var level = requireLevel(bot);
    var player = Objects.requireNonNull(bot.minecraft().player);
    var position = blockPosition(request.getPosition(), level);
    requireLoaded(level, position);
    return CanSeeBlockResponse.newBuilder()
      .setVisible(hasLineOfSight(level, player, position))
      .setDistance((float) player.getEyePosition().distanceTo(Vec3.atCenterOf(position)))
      .setBlock(MinecraftDomainMapper.block(
        level,
        position,
        level.getBlockState(position),
        false,
        true
      ))
      .build();
  }

  private static EstimateDigTimeResponse estimateDigTime(
    BotConnection bot,
    EstimateDigTimeRequest request
  ) {
    var level = requireLevel(bot);
    var player = Objects.requireNonNull(bot.minecraft().player);
    var position = blockPosition(request.getPosition(), level);
    requireLoaded(level, position);
    var state = level.getBlockState(position);
    var diggable = !state.isAir() && state.getDestroySpeed(level, position) >= 0;
    var progress = diggable
      ? state.getDestroyProgress(player, level, position)
      : 0;
    var ticks = progress > 0
      ? Math.max(1, (int) Math.ceil(1.0 / progress))
      : 0;
    return EstimateDigTimeResponse.newBuilder()
      .setDiggable(diggable && progress > 0)
      .setInstant(progress >= 1)
      .setTicks(ticks)
      .setDurationMs(ticks * 50L)
      .setProgressPerTick(progress)
      .setCorrectToolForDrops(player.getMainHandItem().isCorrectToolForDrops(state))
      .setBlock(MinecraftDomainMapper.block(
        level,
        position,
        state,
        false,
        true
      ))
      .build();
  }

  private static EstimateExplosionDamageResponse estimateExplosionDamage(
    BotConnection bot,
    EstimateExplosionDamageRequest request
  ) {
    var level = requireLevel(bot);
    requireEpoch(bot, request.getTarget());
    var target = level.getEntity(request.getTarget().getNetworkId());
    if (target == null
      || request.getTarget().hasUuid()
      && !target.getUUID().toString().equals(request.getTarget().getUuid())) {
      throw Status.NOT_FOUND
        .withDescription("Explosion target is no longer observable")
        .asRuntimeException();
    }
    var center = worldPosition(request.getCenter(), level);
    requireLoaded(level, BlockPos.containing(center));
    var power = request.getPower();
    if (!Float.isFinite(power) || power <= 0 || power > MAX_EXPLOSION_POWER) {
      throw Status.INVALID_ARGUMENT
        .withDescription(
          "Explosion power must be finite and between zero and " + MAX_EXPLOSION_POWER)
        .asRuntimeException();
    }

    var damageRadius = power * 2;
    var distance = (float) Math.sqrt(target.distanceToSqr(center));
    var exposure = distance <= damageRadius
      ? ServerExplosion.getSeenPercent(center, target)
      : 0;
    var impact = distance <= damageRadius
      ? (1 - distance / damageRadius) * exposure
      : 0;
    var rawDamage = distance <= damageRadius
      ? ((impact * impact + impact) / 2 * 7 * damageRadius + 1)
      : 0;

    var armorPoints = 0;
    var armorToughness = 0.0F;
    var resistanceLevel = 0;
    var explosionProtection = 0;
    var absorption = 0.0F;
    var damageAfterArmor = rawDamage;
    var damageAfterResistance = rawDamage;
    var damageAfterEnchantments = rawDamage;
    if (target instanceof LivingEntity living) {
      armorPoints = living.getArmorValue();
      armorToughness = (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      damageAfterArmor = damageAfterArmor(
        rawDamage,
        armorPoints,
        armorToughness);
      var resistance = living.getEffect(MobEffects.RESISTANCE);
      if (resistance != null) {
        resistanceLevel = resistance.getAmplifier() + 1;
      }
      damageAfterResistance = Math.max(
        0,
        damageAfterArmor * (1 - Math.min(resistanceLevel, 5) * 0.2F));
      explosionProtection = explosionProtection(living);
      damageAfterEnchantments = damageAfterResistance
        * (1 - Math.min(explosionProtection, 20) / 25.0F);
      absorption = Math.min(
        damageAfterEnchantments,
        Math.max(0, living.getAbsorptionAmount()));
    }
    var invulnerable = target.isInvulnerable()
      || target instanceof Player player
      && (player.isSpectator() || player.getAbilities().invulnerable);
    var healthDamage = invulnerable
      ? 0
      : Math.max(0, damageAfterEnchantments - absorption);

    return EstimateExplosionDamageResponse.newBuilder()
      .setDistance(distance)
      .setDamageRadius(damageRadius)
      .setExposure(exposure)
      .setRawDamage(rawDamage)
      .setDamageAfterArmor(damageAfterArmor)
      .setDamageAfterResistance(damageAfterResistance)
      .setDamageAfterEnchantments(damageAfterEnchantments)
      .setAbsorbedDamage(absorption)
      .setEstimatedHealthDamage(healthDamage)
      .setInvulnerable(invulnerable)
      .setArmorPoints(armorPoints)
      .setArmorToughness(armorToughness)
      .setResistanceLevel(resistanceLevel)
      .setExplosionProtection(explosionProtection)
      .build();
  }

  static float damageAfterArmor(
    float damage,
    float armor,
    float toughness
  ) {
    var divisor = 2 + toughness / 4;
    var effectiveArmor = Math.max(
      armor * 0.2F,
      Math.min(20, armor - damage / divisor));
    return damage * (1 - effectiveArmor / 25);
  }

  static int explosionProtection(LivingEntity entity) {
    var protection = 0;
    for (var slot : EquipmentSlot.values()) {
      if (!slot.isArmor()) {
        continue;
      }
      var stack = entity.getItemBySlot(slot);
      for (var enchantment : stack.getEnchantments().entrySet()) {
        var id = enchantment.getKey().unwrapKey()
          .map(key -> key.identifier().toString())
          .orElse("");
        protection += switch (id) {
          case "minecraft:protection" -> enchantment.getIntValue();
          case "minecraft:blast_protection" -> enchantment.getIntValue() * 2;
          default -> 0;
        };
      }
    }
    return Math.min(protection, 20);
  }

  private static boolean matchesBlock(
    ClientLevel level,
    BlockPos position,
    BlockState state,
    BlockSelector selector,
    LivingEntity viewer
  ) {
    var blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
      .getKey(state.getBlock())
      .toString();
    if (!selector.getBlockIdsList().isEmpty()
      && !selector.getBlockIdsList().contains(blockId)) {
      return false;
    }
    for (var tag : selector.getTagsList()) {
      if (!state.is(TagKey.create(Registries.BLOCK, Identifier.parse(tag)))) {
        return false;
      }
    }
    for (var property : selector.getPropertiesMap().entrySet()) {
      var actual = state.getProperties().stream()
        .filter(candidate -> candidate.getName().equals(property.getKey()))
        .findFirst();
      if (actual.isEmpty()
        || !propertyValue(state, actual.orElseThrow()).equals(property.getValue())) {
        return false;
      }
    }
    if (selector.hasSolid() && selector.getSolid() != state.isSolidRender()
      || selector.hasReplaceable() && selector.getReplaceable() != state.canBeReplaced()
      || selector.hasInteractive()
      && selector.getInteractive()
      != MinecraftDomainMapper.isInteractive(level, position, state)
      || selector.hasDiggable()
      && selector.getDiggable() != (state.getDestroySpeed(level, position) >= 0)) {
      return false;
    }
    for (var tag : selector.getEffectiveToolTagsList()) {
      if (!state.is(TagKey.create(Registries.BLOCK, Identifier.parse(tag)))) {
        return false;
      }
    }
    if (!selector.getBiomeIdsList().isEmpty()) {
      var biome = level.getBiome(position).unwrapKey()
        .map(key -> key.identifier().toString())
        .orElse("");
      if (!selector.getBiomeIdsList().contains(biome)) {
        return false;
      }
    }
    if (!within(level.getBrightness(LightLayer.SKY, position), selector.getSkyLight())
      || !within(level.getBrightness(LightLayer.BLOCK, position), selector.getBlockLight())) {
      return false;
    }
    return !selector.getRequireLineOfSight()
      || hasLineOfSight(level, viewer, position);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String propertyValue(
    BlockState state,
    net.minecraft.world.level.block.state.properties.Property property
  ) {
    return property.getName(state.getValue(property));
  }

  public static boolean matchesEntity(
    BotConnection bot,
    Entity entity,
    EntitySelector selector,
    Vec3 origin
  ) {
    var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
      .getKey(entity.getType())
      .toString();
    if (!selector.getEntityTypesList().isEmpty()
      && !selector.getEntityTypesList().contains(type)
      || !selector.getCategoriesList().isEmpty()
      && !selector.getCategoriesList().contains(MinecraftDomainMapper.category(entity))
      || selector.hasUuid()
      && !selector.getUuid().equals(entity.getUUID().toString())
      || selector.hasNetworkId()
      && selector.getNetworkId() != entity.getId()
      || selector.hasAlive()
      && selector.getAlive()
      != SFEntityHelpers.isAliveAndTargetable(entity)) {
      return false;
    }
    if (!selector.getTagsList().isEmpty()
      && !entity.entityTags().containsAll(selector.getTagsList())) {
      return false;
    }
    if (selector.hasPlayerName()
      && (!(entity instanceof Player player)
      || !player.getGameProfile().name().equalsIgnoreCase(selector.getPlayerName()))) {
      return false;
    }
    if (selector.hasHealth()) {
      if (!(entity instanceof LivingEntity living)
        || !within(living.getHealth(), selector.getHealth())) {
        return false;
      }
    }
    if (selector.hasCustomName()) {
      var customName = entity.getCustomName();
      if (customName == null
        || !customName.getString().contains(selector.getCustomName())) {
        return false;
      }
    }
    if (!selector.getEquippedItemIdsList().isEmpty()) {
      if (!(entity instanceof LivingEntity living)) {
        return false;
      }
      var equipped = new HashSet<String>();
      for (var slot : EquipmentSlot.values()) {
        var stack = living.getItemBySlot(slot);
        if (!stack.isEmpty()) {
          equipped.add(stack.typeHolder().getRegisteredName());
        }
      }
      if (!equipped.containsAll(selector.getEquippedItemIdsList())) {
        return false;
      }
    }
    if (!selector.getEffectIdsList().isEmpty()) {
      if (!(entity instanceof LivingEntity living)) {
        return false;
      }
      var effects = living.getActiveEffects().stream()
        .map(effect -> effect.getEffect().getRegisteredName())
        .collect(java.util.stream.Collectors.toSet());
      if (!effects.containsAll(selector.getEffectIdsList())) {
        return false;
      }
    }
    if (selector.hasOwnerUuid()
      && !selector.getOwnerUuid().equals(ownerUuid(entity).orElse(null))) {
      return false;
    }
    if (selector.getRequireLineOfSight()) {
      var level = requireLevel(bot);
      var hit = level.clip(new ClipContext(
        origin,
        entity.getEyePosition(),
        ClipContext.Block.COLLIDER,
        ClipContext.Fluid.NONE,
        entity
      ));
      if (hit.getType() != HitResult.Type.MISS) {
        return false;
      }
    }
    return true;
  }

  private static Optional<String> ownerUuid(Entity entity) {
    if (entity instanceof OwnableEntity ownable) {
      var reference = ownable.getOwnerReference();
      return reference == null
        ? Optional.empty()
        : Optional.of(reference.getUUID().toString());
    }
    if (entity instanceof Projectile projectile && projectile.getOwner() != null) {
      return Optional.of(projectile.getOwner().getUUID().toString());
    }
    return Optional.empty();
  }

  private BotConnection requireReadableBot(String instanceValue, String botValue) {
    var instanceId = parseUuid(instanceValue, "instance_id");
    var botId = parseUuid(botValue, "bot_id");
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(
      PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId)
    );
    var instance = server.getInstance(instanceId)
      .orElseThrow(() -> Status.NOT_FOUND
        .withDescription("Instance not found: " + instanceId)
        .asRuntimeException());
    var bot = instance.botConnections().get(botId);
    if (bot == null || bot.isDisconnected()) {
      throw Status.FAILED_PRECONDITION
        .withDescription("Bot is not online: " + botId)
        .asRuntimeException();
    }
    return bot;
  }

  private static ClientLevel requireLevel(BotConnection bot) {
    var level = bot.minecraft().level;
    if (level == null) {
      throw Status.FAILED_PRECONDITION
        .withDescription("Bot level is not loaded")
        .asRuntimeException();
    }
    return level;
  }

  private static void requireEpoch(BotConnection bot, EntityReference reference) {
    if (!reference.getConnectionEpoch().equals(bot.connectionEpoch().toString())) {
      throw Status.FAILED_PRECONDITION
        .withDescription("Entity reference belongs to an earlier connection")
        .asRuntimeException();
    }
  }

  private static BlockPos blockPosition(
    com.soulfiremc.grpc.generated.BlockPosition value,
    ClientLevel level
  ) {
    validateDimension(value.getDimension(), level);
    return new BlockPos(value.getX(), value.getY(), value.getZ());
  }

  private static Vec3 worldPosition(WorldPosition value, ClientLevel level) {
    validateDimension(value.getDimension(), level);
    return new Vec3(value.getX(), value.getY(), value.getZ());
  }

  private static void validateDimension(String dimension, ClientLevel level) {
    var current = level.dimension().identifier().toString();
    if (!dimension.isBlank() && !dimension.equals(current)) {
      throw Status.INVALID_ARGUMENT
        .withDescription("Position dimension does not match the bot's dimension")
        .asRuntimeException();
    }
  }

  private static void requireLoaded(ClientLevel level, BlockPos position) {
    if (!SFBlockHelpers.isChunkLoaded(level, position)) {
      throw Status.FAILED_PRECONDITION
        .withDescription("Target chunk is not loaded")
        .asRuntimeException();
    }
  }

  private static QueryBounds bounds(
    QueryRegion region,
    ClientLevel level,
    Vec3 defaultOrigin
  ) {
    return switch (region.getRegionCase()) {
      case SPHERE -> {
        var sphere = region.getSphere();
        var center = sphere.hasCenter()
          ? worldPosition(sphere.getCenter(), level)
          : defaultOrigin;
        var radius = sphere.getRadius() <= 0
          ? 16
          : Math.min(sphere.getRadius(), MAX_BLOCK_RADIUS);
        yield new QueryBounds(
          (int) Math.floor(center.x - radius),
          (int) Math.floor(center.y - radius),
          (int) Math.floor(center.z - radius),
          (int) Math.ceil(center.x + radius),
          (int) Math.ceil(center.y + radius),
          (int) Math.ceil(center.z + radius),
          center,
          radius * radius
        );
      }
      case BOX -> {
        var box = region.getBox();
        var minimum = blockPosition(box.getMinimum(), level);
        var maximum = blockPosition(box.getMaximum(), level);
        var minX = Math.min(minimum.getX(), maximum.getX());
        var minY = Math.min(minimum.getY(), maximum.getY());
        var minZ = Math.min(minimum.getZ(), maximum.getZ());
        var maxX = Math.max(minimum.getX(), maximum.getX());
        var maxY = Math.max(minimum.getY(), maximum.getY());
        var maxZ = Math.max(minimum.getZ(), maximum.getZ());
        var volume = (long) (maxX - minX + 1)
          * (maxY - minY + 1)
          * (maxZ - minZ + 1);
        if (volume > MAX_BLOCK_VOLUME) {
          throw Status.RESOURCE_EXHAUSTED
            .withDescription("Block query region is too large")
            .asRuntimeException();
        }
        yield new QueryBounds(
          minX,
          minY,
          minZ,
          maxX,
          maxY,
          maxZ,
          new Vec3(
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
          ),
          Double.POSITIVE_INFINITY
        );
      }
      case REGION_NOT_SET -> {
        var radius = 16;
        yield new QueryBounds(
          (int) Math.floor(defaultOrigin.x - radius),
          (int) Math.floor(defaultOrigin.y - radius),
          (int) Math.floor(defaultOrigin.z - radius),
          (int) Math.ceil(defaultOrigin.x + radius),
          (int) Math.ceil(defaultOrigin.y + radius),
          (int) Math.ceil(defaultOrigin.z + radius),
          defaultOrigin,
          radius * radius
        );
      }
    };
  }

  private static boolean hasLineOfSight(
    ClientLevel level,
    LivingEntity viewer,
    BlockPos position
  ) {
    var hit = level.clip(new ClipContext(
      viewer.getEyePosition(),
      Vec3.atCenterOf(position),
      ClipContext.Block.COLLIDER,
      ClipContext.Fluid.NONE,
      viewer
    ));
    return hit.getType() == HitResult.Type.MISS
      || hit instanceof BlockHitResult blockHit
      && blockHit.getBlockPos().equals(position);
  }

  private static boolean within(int value, IntRange range) {
    return (!range.hasMinimum() || value >= range.getMinimum())
      && (!range.hasMaximum() || value <= range.getMaximum());
  }

  private static boolean within(float value, FloatRange range) {
    return (!range.hasMinimum() || value >= range.getMinimum())
      && (!range.hasMaximum() || value <= range.getMaximum());
  }

  /// A total order, so a page token can resume right after the last block it returned.
  static Comparator<BlockMatch> blockComparator(QuerySort sort) {
    var xyz = Comparator
      .comparingInt((BlockMatch match) -> match.position.getX())
      .thenComparingInt(match -> match.position.getY())
      .thenComparingInt(match -> match.position.getZ());
    return switch (sort) {
      case QUERY_SORT_FARTHEST ->
        Comparator.comparingDouble(BlockMatch::distanceSquared).reversed().thenComparing(xyz);
      case QUERY_SORT_XYZ -> xyz;
      case QUERY_SORT_NEAREST, QUERY_SORT_UNSPECIFIED, UNRECOGNIZED ->
        Comparator.comparingDouble(BlockMatch::distanceSquared).thenComparing(xyz);
    };
  }

  /// One page of the blocks in `bounds` that `matches` accepts, after the block `pageToken` names.
  static Page<BlockMatch> blockPage(
    QueryBounds bounds,
    QuerySort sort,
    int requestedSize,
    String pageToken,
    Predicate<BlockPos> matches
  ) {
    var page = new BlockPage(
      blockComparator(sort),
      pageSize(requestedSize),
      decodeBlockCursor(pageToken, bounds.origin)
    );
    for (var x = bounds.minX; x <= bounds.maxX; x++) {
      for (var y = bounds.minY; y <= bounds.maxY; y++) {
        for (var z = bounds.minZ; z <= bounds.maxZ; z++) {
          var position = new BlockPos(x, y, z);
          var distanceSquared = position.distToCenterSqr(bounds.origin.x, bounds.origin.y, bounds.origin.z);
          if (page.wants(position, distanceSquared) && bounds.contains(position) && matches.test(position)) {
            page.add(new BlockMatch(position, distanceSquared));
          }
        }
      }
    }
    return page.result();
  }

  static BlockMatch blockMatch(BlockPos position, Vec3 origin) {
    return new BlockMatch(position, position.distToCenterSqr(origin.x, origin.y, origin.z));
  }

  private static Comparator<EntityMatch> entityComparator(QuerySort sort) {
    return switch (sort) {
      case QUERY_SORT_FARTHEST ->
        Comparator.comparingDouble(EntityMatch::distanceSquared).reversed();
      case QUERY_SORT_XYZ -> Comparator
        .comparingDouble((EntityMatch match) -> match.entity.getX())
        .thenComparingDouble(match -> match.entity.getY())
        .thenComparingDouble(match -> match.entity.getZ())
        .thenComparingInt(match -> match.entity.getId());
      case QUERY_SORT_NEAREST, QUERY_SORT_UNSPECIFIED, UNRECOGNIZED ->
        Comparator.comparingDouble(EntityMatch::distanceSquared)
          .thenComparingInt(match -> match.entity.getId());
    };
  }

  private static <T> Page<T> page(
    List<T> values,
    int requestedSize,
    String pageToken
  ) {
    var offset = decodeOffset(pageToken);
    if (offset > values.size()) {
      throw Status.INVALID_ARGUMENT
        .withDescription("page_token is outside the result set")
        .asRuntimeException();
    }
    var size = pageSize(requestedSize);
    var end = Math.min(values.size(), offset + size);
    return new Page<>(
      List.copyOf(values.subList(offset, end)),
      end < values.size() ? encodeOffset(end) : ""
    );
  }

  static int pageSize(int requestedSize) {
    return requestedSize <= 0 ? 100 : Math.min(requestedSize, MAX_PAGE_SIZE);
  }

  /// A block page token holds the position of the last block returned.
  static @Nullable BlockMatch decodeBlockCursor(String token, Vec3 origin) {
    if (token.isBlank()) {
      return null;
    }
    try {
      var parts = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8).split(",", -1);
      if (parts.length != 3) {
        throw new IllegalArgumentException("Expected x,y,z");
      }
      return blockMatch(
        new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
        origin
      );
    } catch (RuntimeException exception) {
      throw Status.INVALID_ARGUMENT
        .withDescription("Invalid page_token")
        .withCause(exception)
        .asRuntimeException();
    }
  }

  static String encodeBlockCursor(BlockMatch last) {
    var position = last.position;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
      (position.getX() + "," + position.getY() + "," + position.getZ()).getBytes(StandardCharsets.UTF_8)
    );
  }

  private static int decodeOffset(String token) {
    if (token.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(new String(
        Base64.getUrlDecoder().decode(token),
        StandardCharsets.UTF_8
      ));
    } catch (RuntimeException exception) {
      throw Status.INVALID_ARGUMENT
        .withDescription("Invalid page_token")
        .withCause(exception)
        .asRuntimeException();
    }
  }

  private static String encodeOffset(int offset) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(
      Integer.toString(offset).getBytes(StandardCharsets.UTF_8)
    );
  }

  private static UUID parseUuid(String value, String field) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw Status.INVALID_ARGUMENT
        .withDescription(field + " must be a UUID")
        .withCause(exception)
        .asRuntimeException();
    }
  }

  private static BlockFace blockFace(BlockHitResult hit) {
    return switch (hit.getDirection()) {
      case DOWN -> BlockFace.BLOCK_FACE_DOWN;
      case UP -> BlockFace.BLOCK_FACE_UP;
      case NORTH -> BlockFace.BLOCK_FACE_NORTH;
      case SOUTH -> BlockFace.BLOCK_FACE_SOUTH;
      case WEST -> BlockFace.BLOCK_FACE_WEST;
      case EAST -> BlockFace.BLOCK_FACE_EAST;
    };
  }

  private static <T> T inBot(BotConnection bot, Callable<T> callable) throws Exception {
    return BotThreadExecution.call(bot, callable);
  }

  private static <T> void unary(
    StreamObserver<T> observer,
    Callable<T> call
  ) {
    try {
      observer.onNext(call.call());
      observer.onCompleted();
    } catch (Throwable throwable) {
      observer.onError(toGrpcError(throwable));
    }
  }

  private static Throwable toGrpcError(Throwable throwable) {
    if (throwable instanceof io.grpc.StatusRuntimeException) {
      return throwable;
    }
    return Status.INTERNAL
      .withDescription(Objects.requireNonNullElse(
        throwable.getMessage(),
        throwable.getClass().getSimpleName()
      ))
      .withCause(throwable)
      .asRuntimeException();
  }

  record BlockMatch(BlockPos position, double distanceSquared) {}

  /// The first `size` blocks after `after` in `order`, kept while scanning a region, so a page
  /// costs one scan and no sort of every match.
  static final class BlockPage {
    private final Comparator<BlockMatch> order;
    private final int size;
    private final @Nullable BlockMatch after;
    /// Up to `size + 1` blocks, the last in `order` at the head; the extra one means more follow.
    private final PriorityQueue<BlockMatch> kept;

    BlockPage(Comparator<BlockMatch> order, int size, @Nullable BlockMatch after) {
      this.order = order;
      this.size = size;
      this.after = after;
      this.kept = new PriorityQueue<>(order.reversed());
    }

    /// Whether `match` would be on this page if it matches, checked before the costlier matching.
    boolean wants(BlockPos position, double distanceSquared) {
      if (after == null && kept.size() <= size) {
        return true;
      }
      var match = new BlockMatch(position, distanceSquared);
      return (after == null || order.compare(match, after) > 0)
        && (kept.size() <= size || order.compare(match, Objects.requireNonNull(kept.peek())) < 0);
    }

    void add(BlockMatch match) {
      kept.add(match);
      if (kept.size() > size + 1) {
        kept.poll();
      }
    }

    Page<BlockMatch> result() {
      var values = new ArrayList<>(kept);
      values.sort(order);
      if (values.size() <= size) {
        return new Page<>(List.copyOf(values), "");
      }
      var page = List.copyOf(values.subList(0, size));
      return new Page<>(page, encodeBlockCursor(page.getLast()));
    }
  }

  private record EntityMatch(Entity entity, double distanceSquared) {}

  record Page<T>(List<T> values, String nextToken) {}

  record QueryBounds(
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    Vec3 origin,
    double radiusSquared
  ) {
    private boolean contains(BlockPos position) {
      return radiusSquared == Double.POSITIVE_INFINITY
        || position.distToCenterSqr(origin.x, origin.y, origin.z) <= radiusSquared;
    }
  }
}
