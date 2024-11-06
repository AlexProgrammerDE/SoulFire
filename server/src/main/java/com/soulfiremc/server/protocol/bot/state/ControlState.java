package com.soulfiremc.server.protocol.bot.state;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;

@Setter
@Getter
@ToString
public class ControlState {
  private int activelyControlling;
  private boolean forward;
  private boolean backward;
  private boolean left;
  private boolean right;
  private boolean sprinting;
  private boolean jumping;
  private boolean sneaking;
  private boolean flying;

  public void incrementActivelyControlling() {
    activelyControlling++;
  }

  public void decrementActivelyControlling() {
    activelyControlling--;
  }

  public boolean isActivelyControlling() {
    return activelyControlling > 0;
  }

  public void resetWasd() {
    forward = false;
    backward = false;
    left = false;
    right = false;
  }

  public void resetAll() {
    resetWasd();
    sprinting = false;
    jumping = false;
    sneaking = false;
    flying = false;
  }

  public ServerboundPlayerInputPacket toServerboundPlayerInputPacket() {
    return new ServerboundPlayerInputPacket(forward, backward, left, right, jumping, sneaking, sprinting);
  }
}
