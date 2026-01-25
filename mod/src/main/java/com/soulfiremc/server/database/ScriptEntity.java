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

import com.soulfiremc.grpc.generated.ScriptScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Database entity for storing visual scripts.
 * Scripts contain a graph of nodes and edges that define automation workflows.
 */
@Getter
@Setter
@Entity
@Table(name = "scripts")
public final class ScriptEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank(message = "Script name cannot be blank")
  @Size(min = 1, max = 100, message = "Script name must be between 1 and 100 characters")
  @Column(nullable = false, length = 100)
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Column(length = 1000)
  private String description;

  @NotNull(message = "Instance cannot be null")
  @ManyToOne
  @JoinColumn(nullable = false, foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT))
  private InstanceEntity instance;

  @NotNull(message = "Scope cannot be null")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ScriptScope scope = ScriptScope.SCRIPT_SCOPE_INSTANCE;

  /**
   * JSON array of script nodes.
   * Each node contains: id, type, position (x, y), and data (map).
   */
  @NotNull(message = "Nodes cannot be null")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "TEXT")
  private String nodesJson = "[]";

  /**
   * JSON array of script edges.
   * Each edge contains: id, source, sourceHandle, target, targetHandle, edgeType.
   */
  @NotNull(message = "Edges cannot be null")
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "TEXT")
  private String edgesJson = "[]";

  /**
   * Whether this script should automatically start when the instance starts.
   */
  @Column(nullable = false)
  private boolean autoStart = false;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  @Version
  private long version;
}
