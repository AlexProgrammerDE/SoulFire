package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractClientPlayer extends Player {
  @Nullable
  private PlayerInfo playerInfo;
  protected Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
  public float elytraRotX;
  public float elytraRotY;
  public float elytraRotZ;
  public final ClientLevel clientLevel;

  public AbstractClientPlayer(ClientLevel arg, GameProfile gameProfile) {
    super(arg, arg.getSharedSpawnPos(), arg.getSharedSpawnAngle(), gameProfile);
    this.clientLevel = arg;
  }

  @Override
  public boolean isSpectator() {
    PlayerInfo lv = this.getPlayerInfo();
    return lv != null && lv.getGameMode() == GameType.SPECTATOR;
  }

  @Override
  public boolean isCreative() {
    PlayerInfo lv = this.getPlayerInfo();
    return lv != null && lv.getGameMode() == GameType.CREATIVE;
  }

  @Nullable
  protected PlayerInfo getPlayerInfo() {
    if (this.playerInfo == null) {
      this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
    }

    return this.playerInfo;
  }

  @Override
  public void tick() {
    this.deltaMovementOnPreviousTick = this.getDeltaMovement();
    super.tick();
  }
}
