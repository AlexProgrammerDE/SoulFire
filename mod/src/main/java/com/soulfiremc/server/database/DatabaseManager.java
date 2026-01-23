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

import com.soulfiremc.server.util.SFPathConstants;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.mariadb.jdbc.Driver;
import org.sqlite.JDBC;

import java.nio.file.Path;
import java.sql.DriverManager;

@Slf4j
public final class DatabaseManager {
  private DatabaseManager() {
  }

  public static SessionFactory select() {
    return switch (System.getProperty("database", "sqlite")) {
      case "sqlite" -> forSqlite(SFPathConstants.BASE_DIR.resolve("soulfire.sqlite"));
      case "mysql" -> forMysql();
      default -> throw new IllegalArgumentException("Invalid database type");
    };
  }

  private static SessionFactory forSqlite(Path dbFile) {
    try {
      var jdbcUrl = "jdbc:sqlite:%s".formatted(dbFile);

      // Run migrations before Hibernate initializes
      runMigrations(jdbcUrl, true);

      var configuration = new Configuration();

      configuration.setProperty("hibernate.dialect", SQLiteDialect.class);
      configuration.setProperty("hibernate.connection.driver_class", JDBC.class);
      configuration.setProperty("hibernate.connection.url", jdbcUrl);
      configuration.setProperty("hibernate.connection.pool_size", 1);
      // configuration.setProperty("hibernate.show_sql", true);
      configuration.setProperty("hibernate.hbm2ddl.auto", "update");
      configuration.setProperty("hibernate.hikari.minimumIdle", 1);
      configuration.setProperty("hibernate.hikari.maximumPoolSize", 1);
      configuration.setProperty("hibernate.hikari.idleTimeout", 0);
      configuration.setProperty("hibernate.hikari.connectionTimeout", 30_000);

      return fromConfiguration(configuration);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static SessionFactory fromConfiguration(Configuration configuration) {
    var metadataSources = new MetadataSources(new StandardServiceRegistryBuilder()
      .applySettings(configuration.getProperties())
      .build());
    metadataSources.addPackage("com.soulfiremc.server.database");
    metadataSources.addAnnotatedClasses(
      UserEntity.class,
      InstanceEntity.class,
      InstanceAuditLogEntity.class,
      ScriptEntity.class,
      ServerConfigEntity.class
    );

    return metadataSources.getMetadataBuilder()
      .build()
      .buildSessionFactory();
  }

  private static SessionFactory forMysql() {
    try {
      var jdbcUrl = "jdbc:mysql://%s:%s/%s".formatted(
        System.getProperty("mysql.host", "localhost"),
        Integer.getInteger("mysql.port", 3306),
        System.getProperty("mysql.database", "soulfire")
      );
      var username = System.getProperty("mysql.user");
      var password = System.getProperty("mysql.password");

      // Run migrations before Hibernate initializes
      runMigrations(jdbcUrl, username, password, false);

      var configuration = new Configuration();

      configuration.setProperty("hibernate.dialect", MariaDBDialect.class);
      configuration.setProperty("hibernate.connection.driver_class", Driver.class);
      configuration.setProperty("hibernate.connection.url", jdbcUrl);
      configuration.setProperty("hibernate.connection.username", username);
      configuration.setProperty("hibernate.connection.password", password);
      configuration.setProperty("hibernate.connection.pool_size", Integer.getInteger("mysql.pool_size", 10));
      // configuration.setProperty("hibernate.show_sql", true);
      configuration.setProperty("hibernate.hbm2ddl.auto", "update");
      configuration.setProperty("hibernate.hikari.minimumIdle", Integer.getInteger("mysql.minimumIdle", 1));
      configuration.setProperty("hibernate.hikari.maximumPoolSize", Integer.getInteger("mysql.maximumPoolSize", 10));
      configuration.setProperty("hibernate.hikari.idleTimeout", 0);
      configuration.setProperty("hibernate.hikari.connectionTimeout", 30_000);

      return fromConfiguration(configuration);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static void runMigrations(String jdbcUrl, boolean isSqlite) {
    try (var connection = DriverManager.getConnection(jdbcUrl)) {
      DatabaseMigrations.runMigrations(connection, isSqlite);
    } catch (Exception e) {
      log.warn("Could not run database migrations (database may not exist yet)", e);
    }
  }

  private static void runMigrations(String jdbcUrl, String username, String password, boolean isSqlite) {
    try (var connection = DriverManager.getConnection(jdbcUrl, username, password)) {
      DatabaseMigrations.runMigrations(connection, isSqlite);
    } catch (Exception e) {
      log.warn("Could not run database migrations (database may not exist yet)", e);
    }
  }
}
