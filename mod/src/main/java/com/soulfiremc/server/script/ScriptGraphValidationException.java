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
package com.soulfiremc.server.script;

import java.util.List;

/// Thrown when a ScriptGraph fails validation during construction.
public final class ScriptGraphValidationException extends RuntimeException {
  private final List<ScriptGraph.ValidationDiagnostic> diagnostics;

  public ScriptGraphValidationException(List<ScriptGraph.ValidationDiagnostic> diagnostics) {
    super("Script graph validation failed:\n" + formatDiagnostics(diagnostics));
    this.diagnostics = List.copyOf(diagnostics);
  }

  /// Returns all diagnostics (both errors and warnings).
  public List<ScriptGraph.ValidationDiagnostic> diagnostics() {
    return diagnostics;
  }

  /// Returns error messages as strings for backward compatibility.
  public List<String> errors() {
    return diagnostics.stream()
      .filter(d -> d.severity() == ScriptGraph.Severity.ERROR)
      .map(ScriptGraph.ValidationDiagnostic::message)
      .toList();
  }

  private static String formatDiagnostics(List<ScriptGraph.ValidationDiagnostic> diagnostics) {
    return String.join("\n", diagnostics.stream()
      .map(d -> "[" + d.severity() + "] " + d.message())
      .toList());
  }
}
