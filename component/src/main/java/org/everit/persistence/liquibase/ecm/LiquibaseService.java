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

import javax.sql.DataSource;

import org.osgi.framework.Bundle;

/**
 * A very simple OSGi service that can be used to do Liquibase database migration.
 */
public interface LiquibaseService {

  /**
   * Processes a Liquibase changelog.
   *
   * @param dataSource
   *          The dataSource that the changeLog will be processed on.
   * @param bundle
   *          The bundle that contains the changeLog file.
   * @param changeLogFile
   *          The location of the changeLog file withing the bundle.
   */
  void process(DataSource dataSource, Bundle bundle, String changeLogFile);
}
