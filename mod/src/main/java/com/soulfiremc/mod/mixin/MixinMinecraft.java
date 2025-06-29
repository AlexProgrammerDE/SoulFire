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
package com.soulfiremc.mod.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.soulfiremc.mod.access.IMinecraft;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.protocol.BotConnection;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.Objects;

@Mixin(Minecraft.class)
public class MixinMinecraft implements IMinecraft {
  @Unique
  public BotConnection soulfire$botConnection;

  @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
  private static void getInstance(CallbackInfoReturnable<Minecraft> cir) {
    var currentInstance = SFConstants.MINECRAFT_INSTANCE.get();
    if (currentInstance == null) {
      // new RuntimeException().printStackTrace();
    } else {
      cir.setReturnValue(currentInstance);
    }
  }

  @Inject(method = "<init>", at = @At("RETURN"))
  private void closeRenderer(GameConfig arg, CallbackInfo ci) {
    ((Minecraft) (Object) this).gameRenderer.close();
  }

  @Inject(method = "crash", at = @At("HEAD"), cancellable = true)
  private static void preventCrash(Minecraft minecraft, File gameDirectory, CrashReport crashReport, CallbackInfo ci) {
    Bootstrap.realStdoutPrintln(crashReport.getFriendlyReport(ReportType.CRASH));
    ci.cancel();
  }

  @Inject(method = "createUserApiService", at = @At("HEAD"), cancellable = true)
  private void createUserApiServiceHook(CallbackInfoReturnable<UserApiService> cir) {
    cir.setReturnValue(UserApiService.OFFLINE);
  }

  @Inject(method = "updateLevelInEngines", at = @At("HEAD"), cancellable = true)
  private void updateLevelEngineHook(ClientLevel level, CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "fillSystemReport", at = @At("HEAD"), cancellable = true)
  private static void preventFillSystemReport(SystemReport report, Minecraft minecraft, LanguageManager languageManager, String launchVersion, Options options, CallbackInfoReturnable<SystemReport> cir) {
    cir.setReturnValue(report);
  }

  @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;<init>(Ljava/lang/String;)V", shift = At.Shift.AFTER))
  private void injectLocalHook(GameConfig arg, CallbackInfo ci) {
    SFConstants.MINECRAFT_INSTANCE.set((Minecraft) (Object) this);
  }

  @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
  private void preventDestroy(CallbackInfo ci) {
    ci.cancel();
  }

  @Redirect(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetData()V"))
  private void preventResetData(GameRenderer instance) {
    // Prevent resetting because it causes race conditions
  }

  @Override
  public BotConnection soulfire$getConnection() {
    return Objects.requireNonNull(soulfire$botConnection, "BotConnection is null");
  }

  @Override
  public void soulfire$setConnection(BotConnection connection) {
    if (soulfire$botConnection != null) {
      throw new IllegalStateException("BotConnection is already set");
    }

    this.soulfire$botConnection = connection;
  }
}
