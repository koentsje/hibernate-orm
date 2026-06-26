/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlywayTestIT extends TestTemplate {

	@Test
	public void testFlywayDefault() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated-sources'>                             \s
							<classpath>                                                        \s
								<pathelement location='.'/>                                    \s
							</classpath>                                                       \s
							<jpaconfiguration persistenceunit='default'/>                      \s
							<flyway/>                                                          \s
						</hibernatetool>                                                        \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
		createProjectAndBuildWithPersistenceXml();
		assertFolderExists("generated-sources", 1);
		assertFileExists("generated-sources/V1__hibernate_schema_update.sql");
		String content = getFileContents("generated-sources/V1__hibernate_schema_update.sql");
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testFlywayCustomVersionAndDescription() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated-sources'>                             \s
							<classpath>                                                        \s
								<pathelement location='.'/>                                    \s
							</classpath>                                                       \s
							<jpaconfiguration persistenceunit='default'/>                      \s
							<flyway migrationVersion='3' migrationDescription='add_person'/>   \s
						</hibernatetool>                                                        \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
		createProjectAndBuildWithPersistenceXml();
		assertFileExists("generated-sources/V3__add_person.sql");
		String content = getFileContents("generated-sources/V3__add_person.sql");
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	private void createProjectAndBuildWithPersistenceXml() throws Exception {
		createBuildXmlFile();
		createDatabase();
		createPersistenceXml();
		runAntBuild();
	}

	private void createPersistenceXml() throws Exception {
		File metaInfDir = new File(getProjectDir(), "META-INF");
		metaInfDir.mkdirs();
		File persistenceXmlFile = new File(metaInfDir, "persistence.xml");
		String persistenceXmlContents =
				"""
				<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
					<persistence-unit name="default">
						<class>org.hibernate.tool.ant.Person</class>
						<exclude-unlisted-classes>true</exclude-unlisted-classes>
						<properties>
							<property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
							<property name="jakarta.persistence.jdbc.url" value="%s"/>
							<property name="jakarta.persistence.jdbc.user" value=""/>
							<property name="jakarta.persistence.jdbc.password" value=""/>
						</properties>
					</persistence-unit>
				</persistence>
				""".formatted(constructJdbcConnectionString());
		Files.writeString(persistenceXmlFile.toPath(), persistenceXmlContents);
	}
}
