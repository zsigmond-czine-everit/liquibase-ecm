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
package org.everit.persistence.liquibase.ecm.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.BooleanAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.osgi.liquibase.bundle.OSGiResourceAccessor;
import org.everit.persistence.liquibase.ecm.DatabaseMaintenanceException;
import org.everit.persistence.liquibase.ecm.LiquibaseConstants;
import org.everit.persistence.liquibase.ecm.LiquibaseService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.headers.ProvideCapability;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;

/**
 * Component for {@link LiquibaseService}.
 */
@Component(componentId = LiquibaseConstants.SERVICE_PID,
    configurationPolicy = ConfigurationPolicy.OPTIONAL, label = "Liquibase Component",
    description = "A component that makes it possible to call Liquibase functionality during "
        + "activating bundles that rely on database schema.")
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(attributeId = Constants.SERVICE_DESCRIPTION,
        defaultValue = LiquibaseConstants.DEFAULT_SERVICE_DESCRIPTION,
        label = "Service Description",
        description = "The description of this component configuration. It is used to easily "
            + "identify the service registered by this component.") })
@Service
public class LiquibaseComponent implements LiquibaseService {

  public static final int PRIORITY_01_UPDATE = 1;

  public static final int PRIORITY_02_SQL_DUMB_FOLDER = 2;

  public static final int PRIORITY_03_LOG_SERVICE = 3;

  private LogService logService;

  private String sqlDumpFolder;

  private boolean update = true;

  private void dumpSQL(final Liquibase liquibase, final Bundle bundle, final String changeLogFile)
      throws LiquibaseException {
    if (sqlDumpFolder != null) {
      File folderFile = new File(sqlDumpFolder);
      if (!folderFile.exists() && !folderFile.mkdirs()) {
        // not created folder.
        return;
      }
      String symbolicName = bundle.getSymbolicName();
      String fileName = symbolicName + "_" + new Date().getTime() + ".sql";
      File outputFile = new File(folderFile, fileName);

      try (FileOutputStream fos = new FileOutputStream(outputFile);
          OutputStreamWriter oswriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
        liquibase.update((String) null, oswriter);
      } catch (IOException e) {
        logService.log(LogService.LOG_ERROR, "Cannot dump SQL to " + outputFile.getAbsolutePath()
            + " during processing '" + changeLogFile
            + "' from the bundle " + bundle.toString(), e);
      }
    }
  }

  @Override
  public void process(final DataSource dataSource, final Bundle bundle,
      final String changeLogFile) {
    Database database = null;
    try {

      database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
          new JdbcConnection(dataSource.getConnection()));
      Liquibase liquibase =
          new Liquibase(changeLogFile, new OSGiResourceAccessor(bundle), database);

      List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets((String) null);

      if (unrunChangeSets.size() > 0) {
        dumpSQL(liquibase, bundle, changeLogFile);

        if (update) {
          liquibase.update((String) null);
        }
      } else {
        logService.log(LogService.LOG_INFO, "Nothing to change in the database for bundle "
            + bundle.toString());
      }
    } catch (LiquibaseException e) {
      throw new DatabaseMaintenanceException(
          "Error during processing '" + changeLogFile + "' from the bundle "
              + bundle.toString(),
          e);
    } catch (SQLException e) {
      throw new DatabaseMaintenanceException(
          "Error during processing '" + changeLogFile + "' from the bundle "
              + bundle.toString(),
          e);
    } finally {
      if (database != null) {
        try {
          database.close();
        } catch (DatabaseException e) {
          logService.log(LogService.LOG_ERROR,
              "Cannot close database during processing '" + changeLogFile
                  + "' from the bundle " + bundle.toString(),
              e);
        }
      }
    }

  }

  @ServiceRef(attributeId = LiquibaseConstants.ATTR_LOG_SERVICE_TARGET, defaultValue = "",
      label = "LogService filter", description = "The OSGi filter expression of LogService.")
  public void setLogService(final LogService logService) {
    this.logService = logService;
  }

  @StringAttribute(attributeId = LiquibaseConstants.ATTR_SQL_DUMP_FOLDER, optional = true,
      priority = PRIORITY_02_SQL_DUMB_FOLDER, label = "SQL Dump Folder",
      description = "When provided, the update SQL scripts will be dumped into the specified folder"
          + " with the \"bundleSymbolicName_timestamp.sql\" name. The SQL scripts are dumped before"
          + " an update runs. SQL scripts are dumped even if the update is not done on the database"
          + " as the \"Update\" configuration setting is false.")
  public void setSqlDumpFolder(final String sqlDumpFolder) {
    this.sqlDumpFolder = sqlDumpFolder;
  }

  @BooleanAttribute(attributeId = LiquibaseConstants.ATTR_UPDATE, defaultValue = true,
      priority = LiquibaseComponent.PRIORITY_01_UPDATE, label = "Update",
      description = "In case this setting is true, Liquibase will try to update the database "
          + "schema on the db server during calling the process function.")
  public void setUpdate(final boolean update) {
    this.update = update;
  }

}
