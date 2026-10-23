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
package com.soulfiremc.server.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatInputValidationTest {
  static IntStream illegalCharacters() {
    return IntStream.concat(IntStream.range(0, 32), IntStream.of(127, 167));
  }

  @ParameterizedTest
  @MethodSource("illegalCharacters")
  void rejectsCharactersThatVanillaDisconnects(int character) {
    var error = assertThrows(StatusRuntimeException.class,
      () -> ChatServiceImpl.requireText("valid" + (char) character + "text", "message", 256));
    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Hello world", "漢字 😀", "valid\u0085text", "valid\u009ftext"})
  void preservesAllowedUnicode(String text) {
    assertEquals(text, ChatServiceImpl.requireText(text, "message", 256));
  }

  @Test
  void preservesTheLengthBoundary() {
    var text = "a".repeat(256);
    assertEquals(text, ChatServiceImpl.requireText(text, "message", 256));
    var error = assertThrows(StatusRuntimeException.class,
      () -> ChatServiceImpl.requireText(text + "a", "message", 256));
    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
  }
}
