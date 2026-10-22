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
package com.soulfiremc.server.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;

import java.util.Optional;
import java.util.Set;

public final class SFItemHelpers {
  private static final Set<Block> DISPOSABLE_PATH_BLOCKS = Set.of(
    Blocks.DIRT,
    Blocks.COARSE_DIRT,
    Blocks.ROOTED_DIRT,
    Blocks.GRASS_BLOCK,
    Blocks.PODZOL,
    Blocks.MYCELIUM,
    Blocks.MUD,
    Blocks.MOSS_BLOCK,
    Blocks.CLAY,
    Blocks.STONE,
    Blocks.COBBLESTONE,
    Blocks.GRANITE,
    Blocks.DIORITE,
    Blocks.ANDESITE,
    Blocks.DEEPSLATE,
    Blocks.COBBLED_DEEPSLATE,
    Blocks.TUFF,
    Blocks.CALCITE,
    Blocks.DRIPSTONE_BLOCK,
    Blocks.SANDSTONE,
    Blocks.RED_SANDSTONE,
    Blocks.TERRACOTTA,
    Blocks.NETHERRACK,
    Blocks.BASALT,
    Blocks.SMOOTH_BASALT,
    Blocks.BLACKSTONE,
    Blocks.END_STONE,
    Blocks.SNOW_BLOCK
  );

  private SFItemHelpers() {}

  public static boolean isSafeFullBlockItem(ItemStack itemStack) {
    var blockType = BlockItems.getBlock(itemStack.getItem());
    return blockType.isPresent() && isSafeFullBlock(blockType.get());
  }

  public static boolean isSafeFullBlock(Block block) {
    return BlockItems.hasItem(block) && !(block instanceof FallingBlock);
  }

  public static boolean isDisposableFullBlockItem(ItemStack itemStack) {
    var blockType = BlockItems.getBlock(itemStack.getItem());
    return blockType.isPresent() && isDisposableFullBlock(blockType.get());
  }

  public static boolean isPathBuildingBlockItem(ItemStack itemStack) {
    var blockType = BlockItems.getBlock(itemStack.getItem());
    return blockType.isPresent() && isPathBuildingBlock(blockType.get());
  }

  public static boolean isPathBuildingBlock(Block block) {
    return isDisposableFullBlock(block);
  }

  public static boolean isDisposableFullBlock(Block block) {
    if (block instanceof FallingBlock) {
      return false;
    }
    var state = block.defaultBlockState();
    return DISPOSABLE_PATH_BLOCKS.contains(block)
      || state.is(BlockTags.DIRT)
      || state.is(BlockTags.MUD)
      || state.is(BlockTags.MOSS_BLOCKS)
      || state.is(BlockTags.GRASS_BLOCKS)
      || state.is(BlockTags.TERRACOTTA)
      || state.is(BlockTags.BASE_STONE_OVERWORLD)
      || state.is(BlockTags.BASE_STONE_NETHER);
  }

  public static boolean isTool(ItemStack itemStack) {
    return itemStack.getComponents().get(DataComponents.TOOL) != null;
  }

  public static boolean isEdibleFood(ItemStack itemStack) {
    var components = itemStack.getComponents();
    return components.get(DataComponents.FOOD) != null
      && components.get(DataComponents.CONSUMABLE) != null;
  }

  public static boolean isGoodEdibleFood(ItemStack itemStack) {
    var components = itemStack.getComponents();
    if (!isEdibleFood(itemStack)) {
      return false;
    }
    return Optional.ofNullable(components.get(DataComponents.CONSUMABLE)).map(f -> {
      for (var consumeEffects : f.onConsumeEffects()) {
        if (!(consumeEffects instanceof ApplyStatusEffectsConsumeEffect applyEffects)) {
          continue;
        }

        for (var mobEffect : applyEffects.effects()) {
          if (mobEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return false;
          }
        }
      }

      return true;
    }).orElse(false);
  }

  public static Optional<MeleeWeaponStats> meleeWeaponStats(
    ItemStack itemStack
  ) {
    var modifiers = Optional.ofNullable(
      itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS)
    ).orElse(ItemAttributeModifiers.EMPTY);
    var damage = modifiers.compute(
      Attributes.ATTACK_DAMAGE,
      1,
      EquipmentSlot.MAINHAND
    );
    if (damage <= 1) {
      return Optional.empty();
    }
    var speed = modifiers.compute(
      Attributes.ATTACK_SPEED,
      4,
      EquipmentSlot.MAINHAND
    );
    return Optional.of(new MeleeWeaponStats(
      damage,
      speed,
      damage * 10 + Math.max(0, speed)
    ));
  }

  public static Optional<ArmorStats> armorStats(ItemStack itemStack, EquipmentSlot slot) {
    var equippable = itemStack.get(DataComponents.EQUIPPABLE);
    if (equippable == null || equippable.slot() != slot) {
      return Optional.empty();
    }
    var modifiers = Optional.ofNullable(itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS))
      .orElse(ItemAttributeModifiers.EMPTY);
    var armor = modifiers.compute(Attributes.ARMOR, 0, slot);
    var toughness = modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0, slot);
    var knockback = modifiers.compute(Attributes.KNOCKBACK_RESISTANCE, 0, slot);
    return Optional.of(new ArmorStats(armor, toughness, knockback, armor * 10 + toughness * 2 + knockback * 10));
  }

  public static double armorScore(ItemStack itemStack, EquipmentSlot slot) {
    return armorStats(itemStack, slot).map(ArmorStats::score).orElse(0.0);
  }

  public record ArmorStats(double armor, double toughness, double knockbackResistance, double score) {}

  public record MeleeWeaponStats(
    double attackDamage,
    double attackSpeed,
    double score
  ) {
  }
}
