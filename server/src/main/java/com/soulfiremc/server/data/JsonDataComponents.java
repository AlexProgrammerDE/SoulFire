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
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulfiremc.server.protocol.codecs.ExtraCodecs;
import com.soulfiremc.util.GsonInstance;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.tools.Tool;
import lombok.extern.slf4j.Slf4j;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.IntDataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.type.ObjectDataComponent;

@Slf4j
public record JsonDataComponents(Map<DataComponentType<?>, DataComponent<?, ?>> components) {
  public static final Codec<ToolData.Rule> TOOL_RULE_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(ToolData.Rule::getHolders),
        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("speed").forGetter(ToolData.Rule::getSpeed),
        Codec.BOOL.optionalFieldOf("correct_for_drops").forGetter(ToolData.Rule::getCorrectForDrops)
      )
      .apply(instance, ToolData.Rule::new)
  );
  public static final Codec<ToolData> TOOL_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        ToolData.Rule.CODEC.listOf().fieldOf("rules").forGetter(ToolData::getRules),
        Codec.FLOAT.optionalFieldOf("default_mining_speed", 1.0F).forGetter(ToolData::getDefaultMiningSpeed),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage_per_block", 1).forGetter(ToolData::getDamagePerBlock)
      )
      .apply(instance, Tool::new)
  );
  public static final Codec<MobEffectDetails> MOB_EFFECT_DETAILS_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("id").forGetter(MobEffectDetails::getEffect),
        MobEffectDetails.Details.MAP_CODEC.forGetter(MobEffectDetails::asDetails)
      )
      .apply(instance, MobEffectDetails::new)
  );
  public static final Codec<FoodProperties.PossibleEffect> POSSIBLE_EFFECT_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        MOB_EFFECT_DETAILS_CODEC.fieldOf("effect").forGetter(FoodProperties.PossibleEffect::getEffect),
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

          }
          case "minecraft:tool" -> {
            map.put(DataComponentType.TOOL, new ObjectDataComponent<>(DataComponentType.TOOL, TOOL_CODEC.decode(JsonOps.INSTANCE, value).result().orElseThrow().getFirst()));
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
