/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.test.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

public class AgentMixinGlobalPropertyService implements IGlobalPropertyService {

  public static final Map<String, Object> values = new HashMap<>();

  @Override
  public IPropertyKey resolveKey(String name) {
    return new AgentMixinStringPropertyKey(name);
  }

  private String keyString(IPropertyKey key) {
    return ((AgentMixinStringPropertyKey) key).key();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(IPropertyKey key) {
    return (T) values.get(keyString(key));
  }

  @Override
  public void setProperty(IPropertyKey key, Object value) {
    values.put(keyString(key), value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(IPropertyKey key, T defaultValue) {
    return (T) values.getOrDefault(keyString(key), defaultValue);
  }

  @Override
  public String getPropertyString(IPropertyKey key, String defaultValue) {
    Object o = values.get(keyString(key));
    return o != null ? o.toString() : defaultValue;
  }

}
