/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.persistence.liquibase.ecm.tests;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.everit.osgi.ecm.annotation.Activate;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.persistence.liquibase.ecm.DatabaseMaintenanceException;
import org.everit.persistence.liquibase.ecm.LiquibaseService;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.headers.ProvideCapability;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;

/**
 * Test component that tests {@link LiquibaseService} functionlity.
 */
@Component(componentId = "LiquibaseTest", configurationPolicy = ConfigurationPolicy.IGNORE)
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(attributeId = "eosgi.testId", defaultValue = "liquibaseTest"),
    @StringAttribute(attributeId = "eosgi.testEngine", defaultValue = "junit4") })
@Service(value = LiquibaseTestComponent.class)
public class LiquibaseTestComponent {

  private BundleContext bundleContext;

  private DataSource dataSource;

  private LiquibaseService liquibaseService;

  private LogService logService;

  @Activate
  public void activate(final BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  private void dropAll() {
    Database database = null;
    try (Connection connection = dataSource.getConnection()) {
      database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
          new JdbcConnection(connection));
      database.setDefaultCatalogName("TEST");
      database.setDefaultSchemaName("public");
      Liquibase liquibase =
          new Liquibase((String) null, (ResourceAccessor) null, database);
      liquibase.dropAll();
    } catch (SQLException | LiquibaseException e) {
      throw new RuntimeException(e);
    } finally {
      if (database != null) {
        try {
          database.close();
        } catch (DatabaseException e) {
          logService.log(LogService.LOG_WARNING, "", e);
        }
      }
    }
  }

  @ServiceRef(defaultValue = "")
  public void setDataSource(final DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @ServiceRef(defaultValue = "")
  public void setLiquibaseService(final LiquibaseService liquibaseService) {
    this.liquibaseService = liquibaseService;
  }

  @ServiceRef(defaultValue = "")
  public void setLogService(final LogService logService) {
    this.logService = logService;
  }

  @Test
  public void testProcessTwiceCreation() {
    liquibaseService.process(dataSource, bundleContext.getBundle(),
        "META-INF/liquibase/changelog.xml");

    try (Connection connection = dataSource.getConnection();
        Statement insertStatement = connection.createStatement()) {
      insertStatement
          .executeUpdate("insert into person (firstName, lastName) values ('John', 'Doe')");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    liquibaseService.process(dataSource, bundleContext.getBundle(),
        "META-INF/liquibase/changelog.xml");

    try (Connection connection = dataSource.getConnection();
        Statement queryStatement = connection.createStatement();
        ResultSet resultSet =
            queryStatement.executeQuery("select firstName from person where lastName = 'Doe'")) {
      Assert.assertEquals(true, resultSet.first());
      String firstName = resultSet.getString(1);
      Assert.assertEquals("John", firstName);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    dropAll();
  }

  @Test
  public void testWrongChangelogSQL() {

    try {
      liquibaseService.process(dataSource, bundleContext.getBundle(),
          "META-INF/liquibase/wrongSQL.xml");
      Assert.assertTrue(false);
    } catch (DatabaseMaintenanceException e) {
      Assert.assertTrue(true);
    }
    dropAll();
  }
}
