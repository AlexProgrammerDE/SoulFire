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
package com.soulfiremc.server.database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.nio.file.Path;

public class DatabaseManager {
  public static SessionFactory forSqlite(Path dbFile) {
    try {
      var configuration = new Configuration();

      configuration.setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
      configuration.setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC");
      configuration.setProperty("hibernate.connection.url", "jdbc:sqlite:" + dbFile.toString());
      configuration.setProperty("hibernate.connection.pool_size", 1);
      // configuration.setProperty("hibernate.show_sql", true);
      configuration.setProperty("hibernate.hbm2ddl.auto", "update");
      configuration.setProperty("hibernate.hikari.minimumIdle", 1);
      configuration.setProperty("hibernate.hikari.maximumPoolSize", 1);
      configuration.setProperty("hibernate.hikari.idleTimeout", 0);
      configuration.setProperty("hibernate.hikari.connectionTimeout", 30_000);

      var metadataSources = new MetadataSources(new StandardServiceRegistryBuilder()
        .applySettings(configuration.getProperties())
        .build());
      metadataSources.addPackage("com.soulfiremc.server.database");
      metadataSources.addAnnotatedClasses(
        UserEntity.class,
        InstanceEntity.class,
        ServerConfigEntity.class
      );

      return metadataSources.getMetadataBuilder()
        .build()
        .buildSessionFactory();
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }
}
