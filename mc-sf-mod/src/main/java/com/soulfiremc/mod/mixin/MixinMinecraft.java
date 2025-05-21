package com.soulfiremc.mod.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.soulfiremc.mod.util.SFModThreadLocals;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(Minecraft.class)
public class MixinMinecraft {
  @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
  private static void getInstance(CallbackInfoReturnable<Minecraft> cir) {
    var currentInstance = SFModThreadLocals.MINECRAFT_INSTANCE.get();
    if (currentInstance == null) {
      // new RuntimeException().printStackTrace();
    } else {
      cir.setReturnValue(currentInstance);
    }
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
    SFModThreadLocals.MINECRAFT_INSTANCE.set((Minecraft) (Object) this);
  }

  @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
  private void preventDestroy(CallbackInfo ci) {
    ci.cancel();
  }

  @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
  private void preventStop(CallbackInfo ci) {
    ci.cancel();
  }
}
