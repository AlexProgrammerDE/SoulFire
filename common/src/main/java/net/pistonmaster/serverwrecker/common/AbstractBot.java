/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.common;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

@Getter
@Setter
public abstract class AbstractBot {
    private EntityLocation location;
    private EntityMotion motion;
    private float health = -1;
    private int food = -1;
    private float saturation = -1;
    private int entityId = -1;
    private boolean hardcore = false;
    private GameMode gameMode = null;
    private int maxPlayers = -1;

    public abstract void sendMessage(String message);

    public abstract void connect(String host, int port);

    public abstract void disconnect();

    public abstract boolean isOnline();

    public abstract void sendPositionRotation(boolean onGround, double x, double y, double z, float yaw, float pitch);

    public abstract void sendPosition(boolean onGround, double x, double y, double z);

    public abstract void sendRotation(boolean onGround, float yaw, float pitch);

    public abstract void sendGround(boolean onGround);

    public abstract void sendClientCommand(int actionId);

    public abstract Logger getLogger();

    public abstract IPacketWrapper getAccount();
}
