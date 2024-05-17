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
package com.soulfiremc.server.data;

import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulfiremc.server.protocol.codecs.ExtraCodecs;
import com.soulfiremc.util.GsonInstance;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.IntDataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.ObjectDataComponent;

@Slf4j
public record JsonDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> components) {
  public static UUID uuidFromIntArray(int[] bits) {
    return new UUID((long) bits[0] << 32 | (long) bits[1] & 4294967295L, (long) bits[2] << 32 | (long) bits[3] & 4294967295L);
  }

  public static int[] uuidToIntArray(UUID uuid) {
    var l = uuid.getMostSignificantBits();
    var m = uuid.getLeastSignificantBits();
    return leastMostToIntArray(l, m);
  }

  private static int[] leastMostToIntArray(long most, long least) {
    return new int[] {(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
  }

  public static DataResult<int[]> fixedSize(IntStream stream, int size) {
    var is = stream.limit(size + 1).toArray();
    if (is.length != size) {
      Supplier<String> supplier = () -> "Input is not a list of " + size + " ints";
      return is.length >= size ? DataResult.error(supplier, Arrays.copyOf(is, size)) : DataResult.error(supplier);
    } else {
      return DataResult.success(is);
    }
  }

  public static final Codec<UUID> UUID_CODEC = Codec.INT_STREAM
    .comapFlatMap(uuids -> fixedSize(uuids, 4).map(JsonDataComponents::uuidFromIntArray), uuid -> Arrays.stream(uuidToIntArray(uuid)));
  @SuppressWarnings("PatternValidation")
  private static final Codec<Effect> MCPL_EFFECT_CODEC = Codec.STRING.xmap(s -> Effect.valueOf(Key.key(s).value().toUpperCase(Locale.ROOT)), e -> Key.key(e.name().toLowerCase(Locale.ROOT)).toString());
  @SuppressWarnings("PatternValidation")
  private static final Codec<AttributeType> ATTRIBUTE_TYPE_CODEC = Codec.STRING.xmap(s -> AttributeType.getByKey(Key.key(s)), a -> a.key().toString());
  private static final Codec<ItemAttributeModifiers.EquipmentSlotGroup> MCPL_EQUIPMENT_SLOT_GROUP_CODEC = Codec.STRING.xmap(
    s -> ItemAttributeModifiers.EquipmentSlotGroup.valueOf(s.toUpperCase(Locale.ROOT)), g -> g.name().toLowerCase(Locale.ROOT));
  private static final Codec<ModifierOperation> MCPL_MODIFIER_OPERATION_CODEC = Codec.STRING.xmap(
    s -> ModifierOperation.valueOf(s.toUpperCase(Locale.ROOT)), g -> g.name().toLowerCase(Locale.ROOT));
  public static final MapCodec<ItemAttributeModifiers.AttributeModifier> ATTRIBUTE_MODIFIER_MAP_CODEC = RecordCodecBuilder.mapCodec(
    instance -> instance.group(
        UUID_CODEC.fieldOf("uuid").forGetter(ItemAttributeModifiers.AttributeModifier::getId),
        Codec.STRING.fieldOf("name").forGetter(ItemAttributeModifiers.AttributeModifier::getName),
        Codec.DOUBLE.fieldOf("amount").forGetter(ItemAttributeModifiers.AttributeModifier::getAmount),
        MCPL_MODIFIER_OPERATION_CODEC.fieldOf("operation").forGetter(ItemAttributeModifiers.AttributeModifier::getOperation)
      )
      .apply(instance, ItemAttributeModifiers.AttributeModifier::new)
  );
  public static final Codec<ItemAttributeModifiers.Entry> Item_ATTRIBUTE_MODIFIERS_ENTRY_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        ATTRIBUTE_TYPE_CODEC.fieldOf("type").forGetter(a -> AttributeType.getById(a.getAttribute())),
        ATTRIBUTE_MODIFIER_MAP_CODEC.forGetter(ItemAttributeModifiers.Entry::getModifier),
        MCPL_EQUIPMENT_SLOT_GROUP_CODEC.optionalFieldOf("slot", ItemAttributeModifiers.EquipmentSlotGroup.ANY).forGetter(ItemAttributeModifiers.Entry::getSlot)
      )
      .apply(instance, (a, b, c) -> new ItemAttributeModifiers.Entry(a.id(), b, c))
  );
  private static final Codec<ItemAttributeModifiers> ITEM_ATTRIBUTE_MODIFIERS_FULL_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        Item_ATTRIBUTE_MODIFIERS_ENTRY_CODEC.listOf().fieldOf("modifiers").forGetter(ItemAttributeModifiers::getModifiers),
        Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(ItemAttributeModifiers::isShowInTooltip)
      )
      .apply(instance, ItemAttributeModifiers::new)
  );
  public static final Codec<ItemAttributeModifiers> ITEM_ATTRIBUTE_MODIFIERS_CODEC = Codec.withAlternative(
    ITEM_ATTRIBUTE_MODIFIERS_FULL_CODEC, Item_ATTRIBUTE_MODIFIERS_ENTRY_CODEC.listOf(), list -> new ItemAttributeModifiers(list, true)
  );
  public static final MapCodec<MobEffectDetails> MOB_EFFECT_DETAILS_MAP_CODEC = MapCodec.recursive(
    "MobEffectInstance.Details",
    codec -> RecordCodecBuilder.mapCodec(
      instance -> instance.group(
          ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectDetails::getAmplifier),
          Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectDetails::getDuration),
          Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectDetails::isAmbient),
          Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectDetails::isShowParticles),
          Codec.BOOL.optionalFieldOf("show_icon").forGetter(arg -> Optional.of(arg.isShowIcon())),
          codec.optionalFieldOf("hidden_effect").forGetter(d -> Optional.ofNullable(d.getHiddenEffect()))
        )
        .apply(instance, (a, b, c, d, e, f) -> new MobEffectDetails(a, b, c, d, e.orElse(d), f.orElse(null)))
    )
  );
  public static final Codec<MobEffectInstance> MOB_EFFECT_INSTANCE_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        MCPL_EFFECT_CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect),
        MOB_EFFECT_DETAILS_MAP_CODEC.forGetter(MobEffectInstance::getDetails)
      )
      .apply(instance, MobEffectInstance::new)
  );
  public static final Codec<FoodProperties.PossibleEffect> POSSIBLE_EFFECT_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        MOB_EFFECT_INSTANCE_CODEC.fieldOf("effect").forGetter(FoodProperties.PossibleEffect::getEffect),
        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(FoodProperties.PossibleEffect::getProbability)
      )
      .apply(instance, FoodProperties.PossibleEffect::new)
  );
  public static final Codec<FoodProperties> FOOD_PROPERTIES_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("nutrition").forGetter(FoodProperties::getNutrition),
        Codec.FLOAT.fieldOf("saturation").forGetter(FoodProperties::getSaturationModifier),
        Codec.BOOL.optionalFieldOf("can_always_eat", false).forGetter(FoodProperties::isCanAlwaysEat),
        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("eat_seconds", 1.6F).forGetter(FoodProperties::getEatSeconds),
        POSSIBLE_EFFECT_CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(FoodProperties::getEffects)
      )
      .apply(instance, FoodProperties::new)
  );
  public static final TypeAdapter<JsonDataComponents> SERIALIZER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, JsonDataComponents value) {
      throw new UnsupportedOperationException("JsonDataComponents serialization is not supported");
    }

    @Override
    public JsonDataComponents read(JsonReader in) {
      var parsedJson = GsonInstance.GSON.<JsonObject>fromJson(in, JsonObject.class);
      var map = new HashMap<DataComponentType<?>, DataComponent<?, ?>>();
      for (var entry : parsedJson.entrySet()) {
        var value = entry.getValue();
        switch (entry.getKey()) {
          case "minecraft:max_stack_size" -> {
            var maxStackSize = value.getAsInt();
            map.put(DataComponentType.MAX_STACK_SIZE, new IntDataComponent(DataComponentType.MAX_STACK_SIZE, maxStackSize));
          }
          case "minecraft:rarity" -> {
            var rarity = value.getAsString();
            map.put(DataComponentType.RARITY, new IntDataComponent(DataComponentType.RARITY, Rarity.valueOf(rarity.toUpperCase(Locale.ROOT)).ordinal()));
          }
          case "minecraft:attribute_modifiers" -> {
            map.put(DataComponentType.ATTRIBUTE_MODIFIERS, new ObjectDataComponent<>(DataComponentType.ATTRIBUTE_MODIFIERS, ITEM_ATTRIBUTE_MODIFIERS_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
          }
          case "minecraft:tool" -> {
            // TODO: Implement
          }
          case "minecraft:food" -> {
            map.put(DataComponentType.FOOD, new ObjectDataComponent<>(DataComponentType.FOOD, FOOD_PROPERTIES_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
          }
          default -> log.trace("Unknown DataComponentType: {}", entry.getKey());
        }
      }

      return new JsonDataComponents(map);
    }
  };
}
