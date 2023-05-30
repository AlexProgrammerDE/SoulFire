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
package net.pistonmaster.serverwrecker.api.event.lifecycle;

import net.pistonmaster.serverwrecker.api.event.ServerWreckerEvent;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;

import java.util.List;

/**
 * Add yourself to the addon panel by adding a {@link NavigationItem} to the list.
 *
 * @param navigationItems The list of navigation items.
 */
public record AddonPanelInitEvent(List<NavigationItem> navigationItems) implements ServerWreckerEvent {
}
