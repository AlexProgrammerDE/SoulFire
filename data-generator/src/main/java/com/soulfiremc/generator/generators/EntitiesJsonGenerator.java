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
package com.soulfiremc.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soulfiremc.generator.util.FieldGenerationHelper;
import com.soulfiremc.generator.util.MCHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import java.util.ArrayList;
import java.util.Locale;

@Slf4j
public class EntitiesJsonGenerator implements IDataGenerator {

  @SneakyThrows
  public static JsonObject generateEntity(EntityType<?> entityType) {
    var entityDesc = new JsonObject();

    entityDesc.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType));
    entityDesc.addProperty("key", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());

    var dimensionsDesc = new JsonObject();
    var dimensions = entityType.getDimensions();

    dimensionsDesc.addProperty("width", dimensions.width());
    dimensionsDesc.addProperty("height", dimensions.height());
    dimensionsDesc.addProperty("eyeHeight", dimensions.eyeHeight());
    if (dimensions.fixed()) {
      dimensionsDesc.addProperty("fixed", true);
    }

    entityDesc.add("dimensions", dimensionsDesc);

    entityDesc.addProperty("updateInterval", entityType.updateInterval());
    entityDesc.addProperty("clientTrackingRange", entityType.clientTrackingRange());

    var category = entityType.getCategory();
    entityDesc.addProperty("category", category.getName());
    if (category.isFriendly()) {
      entityDesc.addProperty("friendly", true);
    }

    if (entityType.canSummon()) {
      entityDesc.addProperty("summonable", true);
    }

    if (entityType.fireImmune()) {
      entityDesc.addProperty("fireImmune", true);
    }

    var defaultEntity = MCHelper.createEntity(entityType);
    if (defaultEntity.isAttackable()) {
      entityDesc.addProperty("attackable", true);
    }
    if (defaultEntity instanceof LivingEntity le && le.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)) {
      entityDesc.addProperty("defaultFollowRange", le.getAttributeValue(Attributes.FOLLOW_RANGE));
    }
    if (defaultEntity instanceof Player) {
      entityDesc.addProperty("playerEntity", true);
    }
    if (defaultEntity instanceof LivingEntity) {
      entityDesc.addProperty("livingEntity", true);
    }
    if (defaultEntity instanceof AbstractBoat) {
      entityDesc.addProperty("boatEntity", true);
    }
    if (defaultEntity instanceof AbstractMinecart) {
      entityDesc.addProperty("minecartEntity", true);
    }
    if (defaultEntity instanceof AbstractWindCharge) {
      entityDesc.addProperty("windChargeEntity", true);
    }
    if (defaultEntity instanceof Shulker) {
      entityDesc.addProperty("shulkerEntity", true);
    }

    var inheritedClasses = new JsonArray();
    Class<?> clazz = defaultEntity.getClass();
    do {
      inheritedClasses.add(FieldGenerationHelper.toSnakeCase(clazz.getSimpleName()).toLowerCase(Locale.ROOT));
    } while ((clazz = clazz.getSuperclass()) != null && clazz != Object.class);
    entityDesc.add("inheritedClasses", inheritedClasses);

    entityDesc.addProperty("defaultEntityMetadata", MCHelper.serializeToBase64(registryBuf -> {
      try {
        var itemsByIdField = SynchedEntityData.class.getDeclaredField("itemsById");
        itemsByIdField.setAccessible(true);
        var itemsById = (SynchedEntityData.DataItem<?>[]) itemsByIdField.get(defaultEntity.getEntityData());
        var list = new ArrayList<SynchedEntityData.DataValue<?>>();
        var defaultField = SynchedEntityData.DataItem.class.getDeclaredField("initialValue");
        defaultField.setAccessible(true);
        for (var item : itemsById) {
          list.add(createCasted(item.getAccessor(), defaultField.get(item)));
        }
        var packet = new ClientboundSetEntityDataPacket(-1, list);

        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(registryBuf, packet);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }));

    return entityDesc;
  }

  @Override
  public String getDataName() {
    return "data/entities.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultArray = new JsonArray();
    BuiltInRegistries.ENTITY_TYPE.forEach(entity -> resultArray.add(generateEntity(entity)));
    return resultArray;
  }

  @SuppressWarnings("unchecked")
  private static <T> SynchedEntityData.DataValue<T> createCasted(EntityDataAccessor<T> accessor, Object value) {
    return SynchedEntityData.DataValue.create(accessor, (T) value);
  }
}
