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

import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SFItemHelpersTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void recognizesPiercingWeaponsWithUnarmedBaseDamage() {
    var stack = itemStack(Items.STICK);
    assertTrue(SFItemHelpers.meleeWeaponStats(stack).isEmpty());

    stack.set(DataComponents.PIERCING_WEAPON,
      new PiercingWeapon(true, false, Optional.empty(), Optional.empty()));
    var stats = SFItemHelpers.meleeWeaponStats(stack).orElseThrow();
    assertEquals(1, stats.attackDamage());
    assertTrue(stats.attackSpeed() > 0);

    stack.remove(DataComponents.PIERCING_WEAPON);
    assertTrue(SFItemHelpers.meleeWeaponStats(stack).isEmpty());
  }

  @Test
  void acceptsDisposableTerrainBlocks() {
    assertTrue(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.DIRT)));
    assertTrue(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.COBBLESTONE)));
    assertTrue(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.NETHERRACK)));
    assertTrue(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.END_STONE)));
  }

  @Test
  void preservesCraftingAndProgressionResources() {
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.OAK_LOG)));
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.OAK_PLANKS)));
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.CRAFTING_TABLE)));
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.FURNACE)));
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.OBSIDIAN)));
    assertFalse(SFItemHelpers.isDisposableFullBlockItem(itemStack(Items.IRON_BLOCK)));
  }

  @Test
  void preservesProgressionWoodDuringPathBuilding() {
    assertTrue(SFItemHelpers.isPathBuildingBlockItem(
      itemStack(Items.COBBLESTONE)
    ));
    assertFalse(SFItemHelpers.isPathBuildingBlockItem(
      itemStack(Items.OAK_LOG)
    ));
    assertFalse(SFItemHelpers.isPathBuildingBlockItem(
      itemStack(Items.OAK_PLANKS)
    ));
    assertFalse(SFItemHelpers.isPathBuildingBlockItem(
      itemStack(Items.CRAFTING_TABLE)
    ));
    assertFalse(SFItemHelpers.isPathBuildingBlockItem(
      itemStack(Items.OBSIDIAN)
    ));
  }

  @Test
  void classifiesProjectedBlockDropsWithTheSamePlacementPolicy() {
    assertTrue(SFItemHelpers.isDisposableFullBlock(Blocks.COBBLESTONE));
    assertFalse(SFItemHelpers.isDisposableFullBlock(Blocks.OAK_LOG));
    assertFalse(SFItemHelpers.isPathBuildingBlock(Blocks.OAK_LOG));
    assertFalse(SFItemHelpers.isPathBuildingBlock(Blocks.OAK_PLANKS));
  }

  @Test
  void ranksArmorUpgradesUsingProtectionAttributes() {
    var leather = armor(1, 0, 0);
    var diamond = armor(3, 2, 0);
    var netherite = armor(3, 3, 0.1);
    assertTrue(SFItemHelpers.armorScore(diamond, EquipmentSlot.HEAD)
      > SFItemHelpers.armorScore(leather, EquipmentSlot.HEAD));
    assertTrue(SFItemHelpers.armorScore(netherite, EquipmentSlot.HEAD)
      > SFItemHelpers.armorScore(diamond, EquipmentSlot.HEAD));
    assertEquals(new SFItemHelpers.ArmorStats(3, 3, 0.1, 37),
      SFItemHelpers.armorStats(netherite, EquipmentSlot.HEAD).orElseThrow());
  }

  @Test
  void ignoresOtherAttributesAndEquipmentSlots() {
    var helmet = armor(3, 0, 0);
    helmet.set(DataComponents.ATTRIBUTE_MODIFIERS, helmet.get(DataComponents.ATTRIBUTE_MODIFIERS)
      .withModifierAdded(Attributes.ATTACK_DAMAGE,
        new AttributeModifier(Identifier.parse("soulfire:test_damage"), 100, AttributeModifier.Operation.ADD_VALUE),
        EquipmentSlotGroup.HEAD)
      .withModifierAdded(Attributes.ARMOR,
        new AttributeModifier(Identifier.parse("soulfire:test_chest"), 100, AttributeModifier.Operation.ADD_VALUE),
        EquipmentSlotGroup.CHEST));
    assertEquals(30, SFItemHelpers.armorScore(helmet, EquipmentSlot.HEAD));
    assertTrue(SFItemHelpers.armorStats(helmet, EquipmentSlot.CHEST).isEmpty());
    assertEquals(0, SFItemHelpers.armorScore(helmet, EquipmentSlot.CHEST));
  }

  @Test
  void calculatesCustomArmorModifiersWithoutAnArtificialBasePoint() {
    var helmet = armor(3, 0, 0);
    helmet.set(DataComponents.ATTRIBUTE_MODIFIERS, helmet.get(DataComponents.ATTRIBUTE_MODIFIERS)
      .withModifierAdded(Attributes.ARMOR,
        new AttributeModifier(Identifier.parse("soulfire:test_multiplier"), 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
        EquipmentSlotGroup.HEAD));
    assertEquals(6, SFItemHelpers.armorStats(helmet, EquipmentSlot.HEAD).orElseThrow().armor());
  }

  @Test
  void doesNotTreatEmptySlotsOrCosmeticEquipmentAsArmor() {
    var cosmetic = itemStack(Items.CARVED_PUMPKIN);
    cosmetic.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build());
    assertEquals(0, SFItemHelpers.armorScore(ItemStack.EMPTY, EquipmentSlot.HEAD));
    assertEquals(0, SFItemHelpers.armorScore(cosmetic, EquipmentSlot.HEAD));
    assertTrue(SFItemHelpers.armorStats(itemStack(Items.DIRT), EquipmentSlot.HEAD).isEmpty());
  }

  private static ItemStack armor(double armor, double toughness, double knockback) {
    var stack = itemStack(Items.LEATHER_HELMET);
    stack.set(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build());
    stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
      .add(Attributes.ARMOR,
        new AttributeModifier(Identifier.parse("soulfire:test_armor"), armor, AttributeModifier.Operation.ADD_VALUE),
        EquipmentSlotGroup.HEAD)
      .add(Attributes.ARMOR_TOUGHNESS,
        new AttributeModifier(Identifier.parse("soulfire:test_toughness"), toughness, AttributeModifier.Operation.ADD_VALUE),
        EquipmentSlotGroup.HEAD)
      .add(Attributes.KNOCKBACK_RESISTANCE,
        new AttributeModifier(Identifier.parse("soulfire:test_knockback"), knockback, AttributeModifier.Operation.ADD_VALUE),
        EquipmentSlotGroup.HEAD)
      .build());
    return stack;
  }

  private static ItemStack itemStack(Item item) {
    return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), 1);
  }
}
