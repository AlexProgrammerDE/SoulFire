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
package com.soulfiremc.mod;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class HolderComponentsIsolationTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void bindingSharedComponentsKeepsEachConnectionAndBootstrapIndependent() throws ReflectiveOperationException {
    var registry = ResourceKey.<Object>createRegistryKey(Identifier.withDefaultNamespace("component_test"));
    var holder = Holder.Reference.createStandAlone(new HolderOwner<Object>() {},
      ResourceKey.create(registry, Identifier.withDefaultNamespace("shared")));
    var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 16).build();
    var firstComponents = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 32).build();
    var secondComponents = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
    var first = mock(BotConnection.class);
    var second = mock(BotConnection.class);
    when(first.registryComponents()).thenReturn(new ConcurrentHashMap<>());
    when(second.registryComponents()).thenReturn(new ConcurrentHashMap<>());
    var field = BotConnection.class.getDeclaredField("CURRENT");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    var current = (ScopedValue<BotConnection>) field.get(null);

    holder.bindComponents(defaults);
    ScopedValue.where(current, first).run(() -> {
      assertSame(defaults, holder.components());
      holder.bindComponents(firstComponents);
      ScopedValue.where(current, second).run(() -> {
        holder.bindComponents(secondComponents);
        assertTrue(holder.areComponentsBound());
        assertSame(secondComponents, holder.components());
      });
      assertSame(firstComponents, holder.components());
    });
    assertSame(defaults, holder.components());
  }
}
