/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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
package net.pistonmaster.serverwrecker.protocol.bot.state;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class PlayerMetaState {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerMetaState.class);
    private boolean showDebug;
    private int opPermissionLevel;

    public void handleEntityEvent(EntityEvent event) {
        switch (event) {
            case PLAYER_ENABLE_REDUCED_DEBUG -> showDebug = true;
            case PLAYER_DISABLE_REDUCED_DEBUG -> showDebug = false;
            case PLAYER_OP_PERMISSION_LEVEL_0 -> opPermissionLevel = 0;
            case PLAYER_OP_PERMISSION_LEVEL_1 -> opPermissionLevel = 1;
            case PLAYER_OP_PERMISSION_LEVEL_2 -> opPermissionLevel = 2;
            case PLAYER_OP_PERMISSION_LEVEL_3 -> opPermissionLevel = 3;
            case PLAYER_OP_PERMISSION_LEVEL_4 -> opPermissionLevel = 4;
            default -> LOGGER.warn("Unhandled entity event for bot: {}", event.name());
        }
    }
}
