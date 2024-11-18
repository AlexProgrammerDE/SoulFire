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
package com.soulfiremc.server.util.pf4j;

import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;

import java.util.jar.Manifest;

public class SFManifestPluginDescriptorFinder extends ManifestPluginDescriptorFinder {
  public static final String PLUGIN_WEBSITE = "Plugin-Website";

  protected PluginDescriptor createPluginDescriptor(Manifest manifest) {
    var descriptor = (SFPluginDescriptor) super.createPluginDescriptor(manifest);
    var attributes = manifest.getMainAttributes();

    descriptor.website(attributes.getValue(PLUGIN_WEBSITE));

    return descriptor;
  }

  @Override
  protected SFPluginDescriptor createPluginDescriptorInstance() {
    return new SFPluginDescriptor();
  }
}
