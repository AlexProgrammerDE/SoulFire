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
package com.soulfiremc.client.gui.libs;

import java.awt.Color;

/**
 * Represents a terminal theme. Defaults are from the <a
 * href="https://github.com/catppuccin/blackbox">catppuccin themes</a>.
 *
 * @param name            The name of the theme
 * @param backgroundColor The background color of the theme
 * @param colors          The ANSI colors of the theme
 */
public record TerminalTheme(String name, Color backgroundColor, Color[] colors) {
  public static final TerminalTheme[] THEMES = {
    new TerminalTheme(
      "Catppuccin-Frappe",
      "#303446",
      new String[] {
        "#51576D", "#E78284", "#A6D189", "#E5C890", "#8CAAEE", "#F4B8E4", "#81C8BE", "#B5BFE2",
        "#626880", "#E78284", "#A6D189", "#E5C890", "#8CAAEE", "#F4B8E4", "#81C8BE", "#A5ADCE"
      }),
    new TerminalTheme(
      "Catppuccin-Latte",
      "#EFF1F5",
      new String[] {
        "#5C5F77", "#D20F39", "#40A02B", "#DF8E1D", "#1E66F5", "#EA76CB", "#179299", "#ACB0BE",
        "#6C6F85", "#D20F39", "#40A02B", "#DF8E1D", "#1E66F5", "#EA76CB", "#179299", "#BCC0CC"
      }),
    new TerminalTheme(
      "Catppuccin-Macchiato",
      "#24273A",
      new String[] {
        "#494D64", "#ED8796", "#A6DA95", "#EED49F", "#8AADF4", "#F5BDE6", "#8BD5CA", "#B8C0E0",
        "#5B6078", "#ED8796", "#A6DA95", "#EED49F", "#8AADF4", "#F5BDE6", "#8BD5CA", "#A5ADCB"
      }),
    new TerminalTheme(
      "Catppuccin-Mocha",
      "#303446",
      new String[] {
        "#45475A", "#F38BA8", "#A6E3A1", "#F9E2AF", "#89B4FA", "#F5C2E7", "#94E2D5", "#BAC2DE",
        "#585B70", "#F38BA8", "#A6E3A1", "#F9E2AF", "#89B4FA", "#F5C2E7", "#94E2D5", "#A6ADC8"
      })
  };

  public TerminalTheme(String name, String backgroundColor, String[] colors) {
    this(name, Color.decode(backgroundColor), new Color[colors.length]);
    for (var i = 0; i < colors.length; i++) {
      this.colors[i] = Color.decode(colors[i]);
    }
  }

  public TerminalTheme {
    if (colors.length != 16) {
      throw new IllegalArgumentException("Colors must be 16 long!");
    }
  }

  public Color getANSIColor(int index) {
    if (index < 0 || index > 15) {
      throw new IllegalArgumentException("Index must be between 0 and 15!");
    }
    return colors[index];
  }

  public Color getDefaultTextColor() {
    return colors[7];
  }
}
