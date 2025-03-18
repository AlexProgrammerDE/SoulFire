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
package com.soulfiremc.server.script.api;

import com.soulfiremc.server.protocol.BotConnection;
import org.graalvm.polyglot.HostAccess;

public record ScriptMovementAPI(BotConnection connection) {
  @HostAccess.Export
  public boolean isFlying() {
    return connection.botControl().isFlying();
  }

  @HostAccess.Export
  public void setFlying(boolean flying) {
    connection.botControl().setFlying(flying);
  }

  @HostAccess.Export
  public boolean isSneaking() {
    return connection.botControl().isSneaking();
  }

  @HostAccess.Export
  public void setSneaking(boolean sneaking) {
    connection.botControl().setSneaking(sneaking);
  }

  @HostAccess.Export
  public boolean isSprinting() {
    return connection.botControl().isSprinting();
  }

  @HostAccess.Export
  public void setSprinting(boolean sprinting) {
    connection.botControl().setSprinting(sprinting);
  }

  @HostAccess.Export
  public boolean isForward() {
    return connection.controlState().forward();
  }

  @HostAccess.Export
  public void setForward(boolean forward) {
    connection.controlState().forward(forward);
  }

  @HostAccess.Export
  public boolean isBackward() {
    return connection.controlState().backward();
  }

  @HostAccess.Export
  public void setBackward(boolean backward) {
    connection.controlState().backward(backward);
  }

  @HostAccess.Export
  public boolean isLeft() {
    return connection.controlState().left();
  }

  @HostAccess.Export
  public void setLeft(boolean left) {
    connection.controlState().left(left);
  }

  @HostAccess.Export
  public boolean isRight() {
    return connection.controlState().right();
  }

  @HostAccess.Export
  public void setRight(boolean right) {
    connection.controlState().right(right);
  }

  @HostAccess.Export
  public boolean isJumping() {
    return connection.controlState().jumping();
  }

  @HostAccess.Export
  public void setJumping(boolean jumping) {
    connection.controlState().jumping(jumping);
  }

  @HostAccess.Export
  public void resetWasd() {
    connection.controlState().resetWasd();
  }

  @HostAccess.Export
  public void resetAll() {
    connection.controlState().resetAll();
  }
}
