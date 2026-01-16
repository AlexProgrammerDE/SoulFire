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
package com.soulfiremc.server.settings.instance;

import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableIntProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathfindingSettings implements SettingsObject {
  private static final String NAMESPACE = "pathfinding";
  public static final BooleanProperty<InstanceSettingsSource> ALLOW_BREAKING_UNDIGGABLE =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("allow-breaking-undiggable")
      .uiName("Allow Breaking Undiggable")
      .description("Allow the bot to attempt breaking blocks that are normally undiggable (like bedrock)")
      .defaultValue(false)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> AVOID_DIAGONAL_SQUEEZE =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("avoid-diagonal-squeeze")
      .uiName("Avoid Diagonal Squeeze")
      .description("Prevent the bot from squeezing through diagonal gaps between blocks")
      .defaultValue(false)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> AVOID_HARMFUL_ENTITIES =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("avoid-harmful-entities")
      .uiName("Avoid Harmful Entities")
      .description("Add a penalty to paths that go near harmful entities like hostile mobs")
      .defaultValue(true)
      .build();
  public static final IntProperty<InstanceSettingsSource> MAX_ENEMY_PENALTY =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("max-enemy-penalty")
      .uiName("Max Enemy Penalty")
      .description("Maximum cost penalty applied when pathfinding near hostile entities")
      .defaultValue(50)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<InstanceSettingsSource> BREAK_BLOCK_PENALTY =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("break-block-penalty")
      .uiName("Break Block Penalty")
      .description("Cost penalty for breaking a block during pathfinding (higher values discourage breaking)")
      .defaultValue(2)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<InstanceSettingsSource> PLACE_BLOCK_PENALTY =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("place-block-penalty")
      .uiName("Place Block Penalty")
      .description("Cost penalty for placing a block during pathfinding (higher values discourage placing)")
      .defaultValue(5)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<InstanceSettingsSource> EXPIRE_TIMEOUT =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("expire-timeout")
      .uiName("Expire Timeout")
      .description("Maximum time in seconds before pathfinding gives up")
      .defaultValue(180)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> DISABLE_PRUNING =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("disable-pruning")
      .uiName("Disable Pruning")
      .description("Disable periodic pruning of the pathfinding search space (may use more memory)")
      .defaultValue(false)
      .build();
}
