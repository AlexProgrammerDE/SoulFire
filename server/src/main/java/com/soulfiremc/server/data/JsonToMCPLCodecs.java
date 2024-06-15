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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soulfiremc.server.protocol.codecs.ExtraCodecs;
import com.soulfiremc.server.util.DualMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;

public class JsonToMCPLCodecs {
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
  public static final Codec<ToolData.Rule> TOOL_RULE_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        ExtraCodecs.holderSetCodec(BlockType.REGISTRY).fieldOf("blocks").forGetter(ToolData.Rule::getBlocks),
        ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("speed").forGetter(arg -> Optional.ofNullable(arg.getSpeed())),
        Codec.BOOL.optionalFieldOf("correct_for_drops").forGetter(arg -> Optional.ofNullable(arg.getCorrectForDrops()))
      )
      .apply(instance, (a, b, c) -> new ToolData.Rule(a, b.orElse(null), c.orElse(null)))
  );
  public static final Codec<ToolData> TOOL_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        TOOL_RULE_CODEC.listOf().fieldOf("rules").forGetter(ToolData::getRules),
        Codec.FLOAT.optionalFieldOf("default_mining_speed", 1.0F).forGetter(ToolData::getDefaultMiningSpeed),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage_per_block", 1).forGetter(ToolData::getDamagePerBlock)
      )
      .apply(instance, ToolData::new)
  );
  @SuppressWarnings("PatternValidation")
  private static final Codec<Effect> MCPL_EFFECT_CODEC = Codec.STRING.xmap(s -> Effect.valueOf(Key.key(s).value().toUpperCase(Locale.ROOT)), e -> Key.key(e.name().toLowerCase(Locale.ROOT)).toString());
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
  private static final Codec<ItemAttributeModifiers.EquipmentSlotGroup> MCPL_EQUIPMENT_SLOT_GROUP_CODEC = DualMap.keyCodec(
    DualMap.forEnumSwitch(ItemAttributeModifiers.EquipmentSlotGroup.class, g -> switch (g) {
      case ANY -> "any";
      case MAIN_HAND -> "mainhand";
      case OFF_HAND -> "offhand";
      case HAND -> "hand";
      case FEET -> "feet";
      case LEGS -> "legs";
      case CHEST -> "chest";
      case HEAD -> "head";
      case ARMOR -> "armor";
      case BODY -> "body";
    }));
  private static final Codec<ModifierOperation> MCPL_MODIFIER_OPERATION_CODEC = DualMap.keyCodec(
    DualMap.forEnumSwitch(ModifierOperation.class, g -> switch (g) {
      case ADD -> "add_value";
      case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
      case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
    }));
  public static final MapCodec<ItemAttributeModifiers.AttributeModifier> ATTRIBUTE_MODIFIER_MAP_CODEC = RecordCodecBuilder.mapCodec(
    instance -> instance.group(
        ExtraCodecs.KYORI_KEY_CODEC.fieldOf("id").forGetter(ItemAttributeModifiers.AttributeModifier::getId),
        Codec.DOUBLE.fieldOf("amount").forGetter(ItemAttributeModifiers.AttributeModifier::getAmount),
        MCPL_MODIFIER_OPERATION_CODEC.fieldOf("operation").forGetter(ItemAttributeModifiers.AttributeModifier::getOperation)
      )
      .apply(instance, ItemAttributeModifiers.AttributeModifier::new)
  );
  public static final Codec<ItemAttributeModifiers.Entry> Item_ATTRIBUTE_MODIFIERS_ENTRY_CODEC = RecordCodecBuilder.create(
    instance -> instance.group(
        AttributeType.REGISTRY.keyCodec().fieldOf("type").forGetter(a -> AttributeType.REGISTRY.getById(a.getAttribute())),
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
}
