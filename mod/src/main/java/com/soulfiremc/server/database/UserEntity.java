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
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public final class UserEntity {
  @Id
  private UUID id;

  @NotBlank(message = "Usernames cannot be blank")
  @Size(min = 3, max = 32, message = "Usernames must be between 3 and 32 characters")
  @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$", message = "Usernames must be lowercase, begin with an alphanumeric character, followed by more alphanumeric characters or dashes, and end with an alphanumeric character.")
  @Column(nullable = false, unique = true, length = 32)
  private String username;

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Invalid email format")
  @Size(max = 255, message = "Email must not exceed 255 characters")
  @Column(nullable = false, unique = true)
  private String email;

  @NotNull(message = "Role cannot be null")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InstanceEntity> ownedInstances = new ArrayList<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InstanceAuditLogEntity> auditLogs = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  @Nullable
  private Instant lastLoginAt;

  @Column(nullable = false)
  private Instant minIssuedAt;

  @Version
  private long version;

  public enum Role {
    USER,
    ADMIN
  }
}
