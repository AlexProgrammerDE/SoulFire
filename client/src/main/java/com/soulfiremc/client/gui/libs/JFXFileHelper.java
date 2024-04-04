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

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_CANCEL;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_FreePath;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Init;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OKAY;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_OpenDialog;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_Quit;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_SaveDialog;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.nfd.NFDFilterItem;

public class JFXFileHelper {
  private JFXFileHelper() {}

  public static Optional<Path> showOpenDialog(
    @Nullable Path initialDirectory, Map<String, String> filterMap) {
    NFD_Init();
    try (var stack = stackPush()) {
      var filters = NFDFilterItem.malloc(filterMap.size());
      var i = 0;
      for (var entry : filterMap.entrySet()) {
        filters.get(i).name(stack.UTF8(entry.getKey())).spec(stack.UTF8(entry.getValue()));
        i++;
      }

      ByteBuffer initialPathBuf = null;
      if (initialDirectory != null) {
        initialPathBuf = stack.UTF8(initialDirectory.toString());
      }

      var pp = stack.mallocPointer(1);
      return checkResult(NFD_OpenDialog(pp, filters, initialPathBuf), pp);
    } finally {
      NFD_Quit();
    }
  }

  public static Optional<Path> showSaveDialog(
    Path initialDirectory, Map<String, String> filterMap, String defaultName) {
    NFD_Init();
    try (var stack = stackPush()) {
      var filters = NFDFilterItem.malloc(filterMap.size());
      var i = 0;
      for (var entry : filterMap.entrySet()) {
        filters.get(i).name(stack.UTF8(entry.getKey())).spec(stack.UTF8(entry.getValue()));
        i++;
      }

      var pp = stack.mallocPointer(1);
      return checkResult(NFD_SaveDialog(pp, filters, initialDirectory.toString(), defaultName), pp);
    } finally {
      NFD_Quit();
    }
  }

  private static Optional<Path> checkResult(int result, PointerBuffer path) {
    switch (result) {
      case NFD_OKAY -> {
        var pathStr = path.getStringUTF8(0);
        NFD_FreePath(path.get(0));
        return Optional.of(Path.of(pathStr));
      }
      case NFD_CANCEL -> {
        return Optional.empty();
      }
      default -> throw new IllegalStateException("Unexpected value: " + result);
    }
  }
}
