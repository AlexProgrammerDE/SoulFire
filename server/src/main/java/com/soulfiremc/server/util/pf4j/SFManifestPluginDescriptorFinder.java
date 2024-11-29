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

import com.google.common.base.Strings;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;

import java.util.jar.Manifest;

public class SFManifestPluginDescriptorFinder extends ManifestPluginDescriptorFinder {
  public static final String PLUGIN_WEBSITE = "Plugin-Website";

  protected PluginDescriptor createPluginDescriptor(Manifest manifest) {
    var descriptor = (SFPluginDescriptor) super.createPluginDescriptor(manifest);
    var attributes = manifest.getMainAttributes();

    descriptor.website(attributes.getValue(PLUGIN_WEBSITE));

    // Validate required fields
    checkNotNull(descriptor.getPluginId(), ManifestPluginDescriptorFinder.PLUGIN_ID);
    checkNotNull(descriptor.getPluginDescription(), ManifestPluginDescriptorFinder.PLUGIN_DESCRIPTION);
    checkNotNull(descriptor.getVersion(), ManifestPluginDescriptorFinder.PLUGIN_VERSION);
    checkNotNull(descriptor.getProvider(), ManifestPluginDescriptorFinder.PLUGIN_PROVIDER);
    checkNotNull(descriptor.getRequires(), ManifestPluginDescriptorFinder.PLUGIN_REQUIRES);
    checkNotNull(descriptor.getLicense(), ManifestPluginDescriptorFinder.PLUGIN_LICENSE);
    checkNotNull(descriptor.website(), SFManifestPluginDescriptorFinder.PLUGIN_WEBSITE);

    // Dependencies and class can be null

    return descriptor;
  }

  private void checkNotNull(String value, String name) {
    if (Strings.isNullOrEmpty(value)) {
      throw new IllegalArgumentException(name + " cannot be null or empty");
    }
  }

  @Override
  protected SFPluginDescriptor createPluginDescriptorInstance() {
    return new SFPluginDescriptor();
  }
}
