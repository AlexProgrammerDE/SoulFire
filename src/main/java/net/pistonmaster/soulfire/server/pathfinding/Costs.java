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
package net.pistonmaster.soulfire.server.pathfinding;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import net.pistonmaster.soulfire.server.data.BlockState;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.data.ItemType;
import net.pistonmaster.soulfire.server.data.ToolSpeedType;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.protocol.bot.state.EntityEffectState;
import net.pistonmaster.soulfire.server.protocol.bot.state.TagsState;
import net.pistonmaster.soulfire.server.util.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public class Costs {
    public static final double STRAIGHT = 1;
    public static final double DIAGONAL = 1.4142135623730951;
    public static final double JUMP = 0.3;
    public static final double ONE_GAP_JUMP = STRAIGHT + JUMP;
    public static final double FALL_1 = 0.1;
    public static final double FALL_2 = 0.2;
    public static final double FALL_3 = 0.3;
    // 4.317 blocks per second converted to rough estimation of ticks per block
    // Multiply calculated ticks using this number to get a good relative heuristic
    public static final double TICKS_PER_BLOCK = 5;
    // We don't want a bot that tries to break blocks instead of walking around them
    public static final double BREAK_BLOCK_ADDITION = 2;
    public static final double PLACE_BLOCK = 5;
    public static final double JUMP_UP_AND_PLACE_BELOW = JUMP + PLACE_BLOCK;
    // Sliding around a corner is roughly like walking two blocks
    public static final double CORNER_SLIDE = 2 - DIAGONAL;
    /**
     * For performance reasons, we do not want to calculate new costs for every possible block placed.
     * This is the state every placed block on the graph has.
     * This allows the inventory to just store the number of blocks and tools instead of the actual items.
     * Although this decreases the result "quality" a bit, it is a good tradeoff for performance.
     */
    public static final BlockState SOLID_PLACED_BLOCK_STATE = BlockState.forDefaultBlockType(BlockType.STONE);

    private Costs() {
    }

    public static BlockMiningCosts calculateBlockBreakCost(TagsState tagsState, ProjectedInventory inventory, BlockType blockType) {
        var lowestMiningTicks = Integer.MAX_VALUE;
        SWItemStack bestItem = null;
        var correctToolUsed = false;
        for (var slot : inventory.usableToolsAndNull()) {
            var miningTicks = getRequiredMiningTicks(tagsState, null, true, slot, blockType);
            if (miningTicks.ticks() < lowestMiningTicks) {
                lowestMiningTicks = miningTicks.ticks();
                bestItem = slot;
                correctToolUsed = miningTicks.willDrop();
            }
        }

        if (lowestMiningTicks == Integer.MAX_VALUE) {
            // We would expect there is at least a cost to break a block without a tool
            throw new IllegalStateException("No way found to break block!");
        }

        return new BlockMiningCosts(
                (lowestMiningTicks / TICKS_PER_BLOCK) + BREAK_BLOCK_ADDITION,
                bestItem,
                correctToolUsed
        );
    }

    // Time in ticks
    public static TickResult getRequiredMiningTicks(TagsState tagsState,
                                                    @Nullable EntityEffectState effectState,
                                                    boolean onGround,
                                                    @Nullable SWItemStack itemStack,
                                                    BlockType blockType) {
        float speedMultiplier;
        if (itemStack == null) {
            speedMultiplier = 1;
        } else {
            speedMultiplier = ToolSpeedType.getBlockToolSpeed(tagsState, itemStack.type(), blockType);
        }

        if (itemStack != null && speedMultiplier > 1) {
            var efficiency = itemStack.enchantments().getOrDefault("minecraft:efficiency", (short) 0);

            if (efficiency > 0) {
                // Efficiency is capped at 255
                efficiency = MathHelper.shortClamp(efficiency, (short) 0, (short) 255);
                speedMultiplier += (float) (efficiency * efficiency + 1);
            }
        }

        if (effectState != null) {
            var digSpeedAmplifier = getDigSpeedAmplifier(effectState);
            if (digSpeedAmplifier.isPresent()) {
                speedMultiplier *= 1.0F + (float) (digSpeedAmplifier.getAsInt() + 1) * 0.2F;
            }

            var digSlowdownAmplifier = getDigSlowdownAmplifier(effectState);
            if (digSlowdownAmplifier.isPresent()) {
                speedMultiplier *= switch (digSlowdownAmplifier.getAsInt()) {
                    case 0 -> 0.3F;
                    case 1 -> 0.09F;
                    case 2 -> 0.0027F;
                    default -> 8.1E-4F;
                };
            }
        }

        // TODO: Add support for digging underwater without aqua affinity

        if (!onGround) {
            speedMultiplier /= 5.0F;
        }

        var damage = speedMultiplier / blockType.destroyTime();

        var correctToolUsed = isCorrectToolUsed(tagsState, itemStack == null ? null : itemStack.type(), blockType);
        damage /= correctToolUsed ? 30 : 100;

        // Insta mine
        if (damage > 1) {
            return new TickResult(0, correctToolUsed);
        }

        return new TickResult((int) Math.ceil(1 / damage), correctToolUsed);
    }

    private static boolean isCorrectToolUsed(TagsState tagsState, ItemType itemType, BlockType blockType) {
        if (!blockType.requiresCorrectToolForDrops()) {
            return true;
        }

        if (itemType == null) {
            return false;
        }

        return ToolSpeedType.isRightToolFor(tagsState, itemType, blockType);
    }

    private static OptionalInt getDigSpeedAmplifier(EntityEffectState effectState) {
        var hasteEffect = effectState.getEffect(Effect.HASTE);
        var conduitPowerEffect = effectState.getEffect(Effect.CONDUIT_POWER);

        if (hasteEffect.isPresent() && conduitPowerEffect.isPresent()) {
            return OptionalInt.of(Math.max(hasteEffect.get().amplifier(), conduitPowerEffect.get().amplifier()));
        } else {
            return hasteEffect.map(effectData -> OptionalInt.of(effectData.amplifier()))
                    .orElseGet(() -> conduitPowerEffect.map(effectData -> OptionalInt.of(effectData.amplifier()))
                            .orElseGet(OptionalInt::empty));
        }
    }

    private static OptionalInt getDigSlowdownAmplifier(EntityEffectState effectState) {
        var miningFatigueEffect = effectState.getEffect(Effect.MINING_FATIGUE);

        return miningFatigueEffect.map(effectData -> OptionalInt.of(effectData.amplifier()))
                .orElseGet(OptionalInt::empty);
    }

    public record BlockMiningCosts(double miningCost, @Nullable SWItemStack usedTool, boolean willDrop) {
    }

    public record TickResult(int ticks, boolean willDrop) {
    }
}
