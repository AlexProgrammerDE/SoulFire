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

import com.soulfiremc.server.script.ScriptNode;
import com.soulfiremc.server.script.nodes.action.*;
import com.soulfiremc.server.script.nodes.constant.*;
import com.soulfiremc.server.script.nodes.data.*;
import com.soulfiremc.server.script.nodes.flow.*;
import com.soulfiremc.server.script.nodes.list.*;
import com.soulfiremc.server.script.nodes.logic.*;
import com.soulfiremc.server.script.nodes.math.*;
import com.soulfiremc.server.script.nodes.string.*;
import com.soulfiremc.server.script.nodes.trigger.*;
import com.soulfiremc.server.script.nodes.util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/// Registry of all available script node types.
/// Provides factory methods to create node instances by their type identifier.
public final class NodeRegistry {
  private static final Map<String, Supplier<ScriptNode>> NODES = new HashMap<>();

  static {
    // Trigger Nodes
    register(OnTickNode.TYPE, OnTickNode::new);
    register(OnJoinNode.TYPE, OnJoinNode::new);
    register(OnChatNode.TYPE, OnChatNode::new);
    register(OnDamageNode.TYPE, OnDamageNode::new);
    register(OnDeathNode.TYPE, OnDeathNode::new);
    register(OnIntervalNode.TYPE, OnIntervalNode::new);

    // Math Nodes
    register(AddNode.TYPE, AddNode::new);
    register(SubtractNode.TYPE, SubtractNode::new);
    register(MultiplyNode.TYPE, MultiplyNode::new);
    register(DivideNode.TYPE, DivideNode::new);
    register(ModuloNode.TYPE, ModuloNode::new);
    register(FormulaNode.TYPE, FormulaNode::new);
    register(RandomNode.TYPE, RandomNode::new);
    register(ClampNode.TYPE, ClampNode::new);
    register(LerpNode.TYPE, LerpNode::new);
    register(BSplineNode.TYPE, BSplineNode::new);
    register(AbsNode.TYPE, AbsNode::new);
    register(FloorNode.TYPE, FloorNode::new);
    register(CeilNode.TYPE, CeilNode::new);
    register(RoundNode.TYPE, RoundNode::new);
    register(MinNode.TYPE, MinNode::new);
    register(MaxNode.TYPE, MaxNode::new);
    register(PowNode.TYPE, PowNode::new);
    register(SqrtNode.TYPE, SqrtNode::new);
    register(SinNode.TYPE, SinNode::new);
    register(CosNode.TYPE, CosNode::new);
    register(TanNode.TYPE, TanNode::new);
    register(DistanceNode.TYPE, DistanceNode::new);

    // Logic Nodes
    register(CompareNode.TYPE, CompareNode::new);
    register(AndNode.TYPE, AndNode::new);
    register(OrNode.TYPE, OrNode::new);
    register(NotNode.TYPE, NotNode::new);
    register(XorNode.TYPE, XorNode::new);

    // String Nodes
    register(ConcatNode.TYPE, ConcatNode::new);
    register(ReplaceNode.TYPE, ReplaceNode::new);
    register(SplitNode.TYPE, SplitNode::new);
    register(SubstringNode.TYPE, SubstringNode::new);
    register(StringLengthNode.TYPE, StringLengthNode::new);
    register(StartsWithNode.TYPE, StartsWithNode::new);
    register(EndsWithNode.TYPE, EndsWithNode::new);
    register(ToLowerCaseNode.TYPE, ToLowerCaseNode::new);
    register(ToUpperCaseNode.TYPE, ToUpperCaseNode::new);
    register(TrimNode.TYPE, TrimNode::new);
    register(StringContainsNode.TYPE, StringContainsNode::new);
    register(FormatNode.TYPE, FormatNode::new);
    register(IndexOfNode.TYPE, IndexOfNode::new);

    // List Nodes
    register(ListLengthNode.TYPE, ListLengthNode::new);
    register(GetAtNode.TYPE, GetAtNode::new);
    register(FirstNode.TYPE, FirstNode::new);
    register(LastNode.TYPE, LastNode::new);
    register(ListContainsNode.TYPE, ListContainsNode::new);
    register(RangeNode.TYPE, RangeNode::new);
    register(JoinToStringNode.TYPE, JoinToStringNode::new);

    // Utility Nodes
    register(ToStringNode.TYPE, ToStringNode::new);
    register(ToNumberNode.TYPE, ToNumberNode::new);
    register(IsNullNode.TYPE, IsNullNode::new);
    register(IsEmptyNode.TYPE, IsEmptyNode::new);
    register(CreateVector3Node.TYPE, CreateVector3Node::new);
    register(SplitVector3Node.TYPE, SplitVector3Node::new);

    // Action Nodes
    register(SetRotationNode.TYPE, SetRotationNode::new);
    register(LookAtNode.TYPE, LookAtNode::new);
    register(SneakNode.TYPE, SneakNode::new);
    register(SprintNode.TYPE, SprintNode::new);
    register(JumpNode.TYPE, JumpNode::new);
    register(AttackNode.TYPE, AttackNode::new);
    register(UseItemNode.TYPE, UseItemNode::new);
    register(PathfindToNode.TYPE, PathfindToNode::new);
    register(BreakBlockNode.TYPE, BreakBlockNode::new);
    register(PlaceBlockNode.TYPE, PlaceBlockNode::new);
    register(SelectSlotNode.TYPE, SelectSlotNode::new);
    register(SendChatNode.TYPE, SendChatNode::new);
    register(WaitNode.TYPE, WaitNode::new);
    register(PrintNode.TYPE, PrintNode::new);
    register(SetVariableNode.TYPE, SetVariableNode::new);

    // Data Nodes
    register(GetPositionNode.TYPE, GetPositionNode::new);
    register(GetRotationNode.TYPE, GetRotationNode::new);
    register(GetHealthNode.TYPE, GetHealthNode::new);
    register(GetHungerNode.TYPE, GetHungerNode::new);
    register(GetInventoryNode.TYPE, GetInventoryNode::new);
    register(GetBlockNode.TYPE, GetBlockNode::new);
    register(FindEntityNode.TYPE, FindEntityNode::new);
    register(FindBlockNode.TYPE, FindBlockNode::new);
    register(GetVariableNode.TYPE, GetVariableNode::new);
    register(GetBotsNode.TYPE, GetBotsNode::new);
    register(FilterBotsNode.TYPE, FilterBotsNode::new);
    register(GetBotByNameNode.TYPE, GetBotByNameNode::new);

    // Constant Nodes
    register(NumberConstantNode.TYPE, NumberConstantNode::new);
    register(StringConstantNode.TYPE, StringConstantNode::new);
    register(BooleanConstantNode.TYPE, BooleanConstantNode::new);
    register(Vector3ConstantNode.TYPE, Vector3ConstantNode::new);

    // Flow Control Nodes
    register(BranchNode.TYPE, BranchNode::new);
    register(SwitchNode.TYPE, SwitchNode::new);
    register(LoopNode.TYPE, LoopNode::new);
    register(ForEachNode.TYPE, ForEachNode::new);
    register(ForEachBotNode.TYPE, ForEachBotNode::new);
    register(SequenceNode.TYPE, SequenceNode::new);
    register(GateNode.TYPE, GateNode::new);
    register(DebounceNode.TYPE, DebounceNode::new);
  }

  private NodeRegistry() {
    // Prevent instantiation
  }

  /// Registers a node type with its factory.
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
}
