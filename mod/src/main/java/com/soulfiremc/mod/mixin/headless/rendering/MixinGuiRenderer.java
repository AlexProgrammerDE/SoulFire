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
package com.soulfiremc.mod.mixin.headless.rendering;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.soulfiremc.server.renderer.VulkanRenderer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GuiRenderer.class)
public abstract class MixinGuiRenderer {
  @WrapMethod(method = "draw")
  private void preserveViewport(Operation<Void> original) {
    VulkanRenderer.withGuiViewport(original::call);
  }

  @WrapMethod(method = "enableScissor")
  private void scaleClipping(ScreenRectangle rectangle, RenderPass pass, Operation<Void> original) {
    if (!VulkanRenderer.renderGuiScissor(rectangle, pass)) original.call(rectangle, pass);
  }
}
