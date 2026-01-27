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
package com.soulfiremc.server.script.nodes;

import com.soulfiremc.server.script.CategoryRegistry;
import com.soulfiremc.server.script.NodeCategory;
import com.soulfiremc.server.script.NodeMetadata;
import com.soulfiremc.server.script.ScriptNode;
import com.soulfiremc.server.script.nodes.action.*;
import com.soulfiremc.server.script.nodes.constant.BooleanConstantNode;
import com.soulfiremc.server.script.nodes.constant.NumberConstantNode;
import com.soulfiremc.server.script.nodes.constant.StringConstantNode;
import com.soulfiremc.server.script.nodes.constant.Vector3ConstantNode;
import com.soulfiremc.server.script.nodes.data.*;
import com.soulfiremc.server.script.nodes.flow.*;
import com.soulfiremc.server.script.nodes.list.*;
import com.soulfiremc.server.script.nodes.logic.*;
import com.soulfiremc.server.script.nodes.math.*;
import com.soulfiremc.server.script.nodes.string.*;
import com.soulfiremc.server.script.nodes.trigger.*;
import com.soulfiremc.server.script.nodes.util.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/// Registry of all available script node types.
/// Provides factory methods to create node instances by their type identifier.
public final class NodeRegistry {
  private static final Map<String, Supplier<ScriptNode>> NODES = new HashMap<>();

  static {
    // Trigger Nodes
    register(OnTickNode::new);
    register(OnJoinNode::new);
    register(OnChatNode::new);
    register(OnDamageNode::new);
    register(OnDeathNode::new);
    register(OnIntervalNode::new);

    // Math Nodes
    register(MathOperationNode::new);  // Combined node with mode-based visibility
    register(FormulaNode::new);        // Expression evaluator with variables
    register(RandomNode::new);         // Random number generation
    register(BSplineNode::new);        // B-spline interpolation
    register(DistanceNode::new);       // Vector distance calculation

    // Logic Nodes
    register(CompareNode::new);
    register(AndNode::new);
    register(OrNode::new);
    register(NotNode::new);
    register(XorNode::new);

    // String Nodes
    register(ConcatNode::new);
    register(ReplaceNode::new);
    register(SplitNode::new);
    register(SubstringNode::new);
    register(StringLengthNode::new);
    register(StartsWithNode::new);
    register(EndsWithNode::new);
    register(ToLowerCaseNode::new);
    register(ToUpperCaseNode::new);
    register(TrimNode::new);
    register(StringContainsNode::new);
    register(FormatNode::new);
    register(IndexOfNode::new);

    // List Nodes
    register(ListLengthNode::new);
    register(GetAtNode::new);
    register(FirstNode::new);
    register(LastNode::new);
    register(ListContainsNode::new);
    register(RangeNode::new);
    register(JoinToStringNode::new);
    register(ConcatListsNode::new);

    // Utility Nodes
    register(ToStringNode::new);
    register(ToNumberNode::new);
    register(IsNullNode::new);
    register(IsEmptyNode::new);
    register(CreateVector3Node::new);
    register(SplitVector3Node::new);

    // Action Nodes
    register(SetRotationNode::new);
    register(LookAtNode::new);
    register(SneakNode::new);
    register(SprintNode::new);
    register(JumpNode::new);
    register(AttackNode::new);
    register(UseItemNode::new);
    register(PathfindToNode::new);
    register(BreakBlockNode::new);
    register(PlaceBlockNode::new);
    register(SelectSlotNode::new);
    register(SendChatNode::new);
    register(WaitNode::new);
    register(PrintNode::new);

    // Data Nodes
    register(GetPositionNode::new);
    register(GetRotationNode::new);
    register(GetHealthNode::new);
    register(GetHungerNode::new);
    register(GetInventoryNode::new);
    register(GetBlockNode::new);
    register(FindEntityNode::new);
    register(FindBlockNode::new);
    register(GetBotsNode::new);
    register(FilterBotsNode::new);
    register(GetBotByNameNode::new);
    register(GetExperienceNode::new);
    register(GetArmorNode::new);
    register(GetVelocityNode::new);
    register(GetGamemodeNode::new);
    register(GetSelectedSlotNode::new);
    register(GetBotStateNode::new);
    register(GetDimensionNode::new);
    register(GetTimeNode::new);
    register(GetWeatherNode::new);
    register(GetBiomeNode::new);
    register(GetLightLevelNode::new);
    register(GetBotInfoNode::new);
    register(GetTargetBlockNode::new);
    register(GetEffectsNode::new);

    // Constant Nodes
    register(NumberConstantNode::new);
    register(StringConstantNode::new);
    register(BooleanConstantNode::new);
    register(Vector3ConstantNode::new);

    // Flow Control Nodes
    register(BranchNode::new);
    register(SwitchNode::new);
    register(LoopNode::new);
    register(ForEachNode::new);
    register(ForEachBotNode::new);
    register(SequenceNode::new);
    register(GateNode::new);
    register(DebounceNode::new);
  }

  private NodeRegistry() {
    // Prevent instantiation
  }

  /// Registers a node type with its factory.
  /// The type identifier is derived from the node's metadata.
  /// @param factory the factory to create node instances
  public static void register(Supplier<ScriptNode> factory) {
    var node = factory.get();
    var type = node.getId();
    NODES.put(type, factory);
  }

  /// Registers a node type with an explicit type identifier.
  /// @param type the node type identifier
  /// @param factory the factory to create node instances
  public static void register(String type, Supplier<ScriptNode> factory) {
    NODES.put(type, factory);
  }

  /// Creates a new instance of a node by its type.
  /// @param type the node type identifier
  /// @return a new node instance
  /// @throws IllegalArgumentException if the type is not registered
  public static ScriptNode create(String type) {
    var factory = NODES.get(type);
    if (factory == null) {
      throw new IllegalArgumentException("Unknown node type: " + type);
    }
    return factory.get();
  }

  /// Checks if a node type is registered.
  /// @param type the node type identifier
  /// @return true if the type is registered
  public static boolean isRegistered(String type) {
    return NODES.containsKey(type);
  }

  /// Gets all registered node types.
  /// @return set of all registered type identifiers
  public static Set<String> getRegisteredTypes() {
    return Set.copyOf(NODES.keySet());
  }

  /// Gets the number of registered node types.
  /// @return the count of registered types
  public static int getRegisteredCount() {
    return NODES.size();
  }

  /// Gets metadata for all registered node types.
  /// @return list of all node metadata
  public static List<NodeMetadata> getAllMetadata() {
    return NODES.values().stream()
      .map(Supplier::get)
      .map(ScriptNode::getMetadata)
      .toList();
  }

  /// Gets metadata for all registered node types, optionally filtered.
  /// @param categoryId optional category ID filter (null for all)
  /// @param includeDeprecated whether to include deprecated nodes
  /// @return list of matching node metadata
  public static List<NodeMetadata> getFilteredMetadata(String categoryId, boolean includeDeprecated) {
    return NODES.values().stream()
      .map(Supplier::get)
      .map(ScriptNode::getMetadata)
      .filter(m -> categoryId == null || categoryId.isEmpty() || m.category().id().equals(categoryId))
      .filter(m -> includeDeprecated || !m.deprecated())
      .toList();
  }

  /// Gets all categories sorted by sort order.
  /// @return sorted list of all NodeCategory values
  public static List<NodeCategory> getAllCategories() {
    return CategoryRegistry.allSorted();
  }
}
