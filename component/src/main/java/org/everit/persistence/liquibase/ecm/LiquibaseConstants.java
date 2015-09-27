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
package org.everit.persistence.liquibase.ecm;

/**
 * Constants of the Liquibase component.
 */
public final class LiquibaseConstants {

  public static final String ATTR_LOG_SERVICE_TARGET = "logService.target";

  /**
   * In case this configuration property is defined, the SQL update scripts will be dumped to the
   * specified folder.
   */
  public static final String ATTR_SQL_DUMP_FOLDER = "sqlDumpFolder";

  /**
   * Configuration property of the component that indicates if the database itself should be updated
   * or not.
   */
  public static final String ATTR_UPDATE = "update";

  public static final String DEFAULT_SERVICE_DESCRIPTION = "Default Liquibase Component";

  public static final String SERVICE_PID =
      "org.everit.persistence.liquibase.ecm.LiquibaseComponent";

  private LiquibaseConstants() {
  }

}
