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
package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Manages mostly block and interaction related stuff that requires to keep track of sequence numbers.
 */
@Data
@RequiredArgsConstructor
public class BotActionManager {
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private int sequenceNumber = 0;

    public void incrementSequenceNumber() {
        sequenceNumber++;
    }

    public void useItemInHand(Hand hand) {
        incrementSequenceNumber();
        dataManager.getSession().send(new ServerboundUseItemPacket(hand, sequenceNumber));
    }
}
