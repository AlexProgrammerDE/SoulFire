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
package com.soulfiremc.server.database;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "scripts")
public final class ScriptEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull(message = "Script type cannot be null")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ScriptEntity.ScriptType type;

  @ManyToOne
  @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
  private InstanceEntity instance;

  @NotBlank(message = "Script names cannot be blank")
  @Size(min = 3, max = 32, message = "Script names must be between 3 and 32 characters")
  @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$", message = "Script names must be lowercase, begin with an alphanumeric character, followed by more alphanumeric characters or dashes, and end with an alphanumeric character.")
  @Column(nullable = false, length = 32)
  private String scriptName;

  @Column(nullable = false)
  private boolean elevatedPermissions;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  @Version
  private long version;

  public enum ScriptType {
    INSTANCE,
    GLOBAL
  }
}
