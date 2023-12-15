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
package net.pistonmaster.serverwrecker.api;

import org.pf4j.ExtensionPoint;

import java.util.Set;

/**
 * This interface is used to load mixins from third-party plugins.
 * Use the direct class name for a single transformer <i>(e.g. <b>package.Transformer</b>)</i><br>
 * Use the package ending with '*'
 * for all transformers in the packet (not sub packages) <i>(e.g. <b>package.*</b>)</i><br>
 * Use the package ending with '**'
 * for all transformers in the package and sub packages <i>(e.g. <b>package.**</b>)</i><br>
 */
public interface MixinExtension extends ExtensionPoint {
    /**
     * This method is used to inject into ServerWrecker classes.
     *
     * @return A list of mixin paths.
     */
    Set<String> getMixinPaths();
}
