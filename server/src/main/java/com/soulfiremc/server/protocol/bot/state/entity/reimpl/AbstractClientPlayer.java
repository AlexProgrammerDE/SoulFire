package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractClientPlayer extends Player {
  @Nullable
  private PlayerInfo playerInfo;

  public AbstractClientPlayer(ClientLevel arg, GameProfile gameProfile) {
    super(arg, arg.getSharedSpawnPos(), arg.getSharedSpawnAngle(), gameProfile);
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
}
