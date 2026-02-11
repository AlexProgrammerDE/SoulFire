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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;

@Slf4j
public final class DatabaseManager {
  private DatabaseManager() {
  }

  public static DatabaseContext select() {
    return switch (System.getProperty("database", "sqlite")) {
      case "sqlite" -> forSqlite();
      case "mysql" -> forMysql();
      default -> throw new IllegalArgumentException("Invalid database type");
    };
  }

  private static DatabaseContext forSqlite() {
    var dbFile = SFPathConstants.BASE_DIR.resolve("soulfire.sqlite");
    var jdbcUrl = "jdbc:sqlite:%s".formatted(dbFile);

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setMinimumIdle(1);
    hikariConfig.setMaximumPoolSize(1);
    hikariConfig.setIdleTimeout(0);
    hikariConfig.setConnectionTimeout(30_000);

    var dataSource = new HikariDataSource(hikariConfig);

    runMigrations(dataSource);

    var dsl = createDslContext(dataSource, SQLDialect.SQLITE);
    return new DatabaseContext(dsl, dataSource);
  }

  private static DatabaseContext forMysql() {
    var jdbcUrl = "jdbc:mysql://%s:%s/%s".formatted(
      System.getProperty("mysql.host", "localhost"),
      Integer.getInteger("mysql.port", 3306),
      System.getProperty("mysql.database", "soulfire")
    );

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(System.getProperty("mysql.user"));
    hikariConfig.setPassword(System.getProperty("mysql.password"));
    hikariConfig.setMinimumIdle(Integer.getInteger("mysql.minimumIdle", 1));
    hikariConfig.setMaximumPoolSize(Integer.getInteger("mysql.maximumPoolSize", 10));
    hikariConfig.setIdleTimeout(0);
    hikariConfig.setConnectionTimeout(30_000);

    var dataSource = new HikariDataSource(hikariConfig);

    runMigrations(dataSource);

    var dsl = createDslContext(dataSource, SQLDialect.MARIADB);
    return new DatabaseContext(dsl, dataSource);
  }

  private static void runMigrations(DataSource dataSource) {
    var flyway = Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .load();
    flyway.migrate();
  }

  private static DSLContext createDslContext(DataSource dataSource, SQLDialect dialect) {
    var config = new DefaultConfiguration()
      .set(dataSource)
      .set(dialect)
      .set(new DefaultConfiguration().settings()
        .withExecuteWithOptimisticLocking(true));
    return DSL.using(config);
  }

  public record DatabaseContext(DSLContext dsl, HikariDataSource dataSource) implements AutoCloseable {
    @Override
    public void close() {
      dataSource.close();
    }
  }
}
