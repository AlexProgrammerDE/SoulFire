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
package com.soulfiremc.server.script.nodes.data;

import com.soulfiremc.server.script.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Data node that checks if the bot has line-of-sight to a position.
/// Performs a raytrace from the bot's eyes to the target position,
/// checking for block collisions along the path.
/// Input: target (Vec3)
/// Output: visible (boolean)
public final class CanSeePositionNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.can_see_position")
    .displayName("Can See Position")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn(),
      PortDefinition.input("target", "Target", PortType.VECTOR3, "Position to check visibility to")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("visible", "Visible", PortType.BOOLEAN, "Whether the position is visible")
    )
    .description("Checks if the bot has line-of-sight to a position (raytrace)")
    .icon("eye")
    .color("#9C27B0")
    .addKeywords("see", "visible", "sight", "raytrace", "line", "wall", "block")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var target = getInput(inputs, "target", Vec3.ZERO);

    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null) {
      return completedMono(result("visible", false));
    }

    var eye = player.getEyePosition();
    var distance = eye.distanceTo(target);
    if (distance >= 256) {
      return completedMono(result("visible", false));
    }

    var blockVec = BlockPos.containing(target);
    if (!level.isLoaded(blockVec)) {
      return completedMono(result("visible", false));
    }

    var visible = isNotIntersected(
      level.getBlockCollisions(player, new AABB(eye, target)),
      eye, target
    );

    return completedMono(result("visible", visible));
  }

  private static boolean isNotIntersected(Iterable<VoxelShape> shapes, Vec3 start, Vec3 end) {
    for (var shape : shapes) {
      var aabb = shape.bounds();
      if (AABB.clip(
        aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
        start, end
      ).isPresent()) {
        return false;
      }
    }
    return true;
  }
}
