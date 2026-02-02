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
package com.soulfiremc.test.mixin;

import net.lenni0451.reflect.Agents;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.AgentMixinProxy;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public final class MixinSupportMain {
  private MixinSupportMain() {
  }

  public static void load() throws IOException {
    MixinBootstrap.init();
    Mixins.addConfiguration("soulfire.mixins.json");

    try {
      final Method m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
      m.setAccessible(true);
      m.invoke(null, MixinEnvironment.Phase.INIT);
      m.invoke(null, MixinEnvironment.Phase.DEFAULT);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
    AgentMixinProxy proxy = new AgentMixinProxy();
    Agents.getInstrumentation().addTransformer(new ClassFileTransformer() {
      @Override
      public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return proxy.transformClassBytes(className.replace("/", "."), className.replace("/", "."), classfileBuffer);
      }
    });
  }
}
