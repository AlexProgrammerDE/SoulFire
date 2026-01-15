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

import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.util.SFHelpers;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "instances")
public final class InstanceEntity {
  public static final List<String> ICON_POOL = List.of(
    "pickaxe",
    "apple",
    "shovel",
    "sword",
    "fish",
    "citrus",
    "popcorn",
    "cookie",
    "carrot",
    "croissant"
  );

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Friendly name cannot be blank")
  @Size(min = 3, max = 32, message = "Friendly name must be between 3 and 32 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Friendly name must contain only letters, numbers, and spaces")
  @Column(nullable = false, length = 32)
  private String friendlyName;

  @NotBlank(message = "Icon cannot be blank")
  @Size(min = 1, max = 64, message = "Icon name must be between 1 and 64 characters")
  @Pattern(regexp = "^[a-z-]+$", message = "Icon must contain only lowercase letters and hyphens")
  @Column(nullable = false, length = 64)
  private String icon;

  @NotNull(message = "Owner cannot be null")
  @ManyToOne
  @JoinColumn(nullable = false, foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
  private UserEntity owner;

  @NotNull(message = "Attack lifecycle cannot be null")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AttackLifecycle attackLifecycle = AttackLifecycle.STOPPED;

  @NotNull(message = "Settings cannot be null")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = InstanceSettingsConverter.class)
  @Column(nullable = false)
  private InstanceSettingsImpl settings = InstanceSettingsImpl.EMPTY;

  @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ScriptEntity> scripts = new ArrayList<>();

  @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InstanceAuditLogEntity> auditLogs = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  @Version
  private long version;

  public static String randomInstanceIcon() {
    return SFHelpers.getRandomEntry(ICON_POOL);
  }
}
