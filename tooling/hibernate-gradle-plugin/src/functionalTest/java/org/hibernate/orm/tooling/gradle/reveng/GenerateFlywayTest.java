/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateFlywayTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("generateFlyway");
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
	}

	@Test
	public void testDefaultFlyway() throws Exception {
		createProjectWithEntityAndPersistenceXml();
		executeGradleCommand(getGradleTaskToPerform());
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		File migrationFile = new File(generatedOutputFolder, "V1__hibernate_schema_update.sql");
		assertTrue(migrationFile.exists(), "Expected default Flyway migration file");
		String content = Files.readString(migrationFile.toPath());
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testCustomVersionAndDescription() throws Exception {
		setRevengExtensionSection(
				"    migrationVersion = '3'\n" +
				"    migrationDescription = 'add_person'"
		);
		createProjectWithEntityAndPersistenceXml();
		executeGradleCommand(getGradleTaskToPerform());
		File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(generatedOutputFolder.exists());
		File migrationFile = new File(generatedOutputFolder, "V3__add_person.sql");
		assertTrue(migrationFile.exists(), "Expected migration file with custom version and description");
		String content = Files.readString(migrationFile.toPath());
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	private void createProjectWithEntityAndPersistenceXml() throws Exception {
		initGradleProject();
		editGradleBuildFile();
		editGradlePropertiesFile();
		createDatabase();
		createPersonEntity();
		createHibernatePropertiesForFlyway();
		createPersistenceXml();
	}

	private void createPersonEntity() throws Exception {
		File entityDir = new File(getProjectDir(), "app/src/main/java/org/example");
		entityDir.mkdirs();
		File entityFile = new File(entityDir, "Person.java");
		Files.writeString(entityFile.toPath(),
				"""
				package org.example;

				import jakarta.persistence.Column;
				import jakarta.persistence.Entity;
				import jakarta.persistence.Id;
				import jakarta.persistence.Table;

				@Entity
				@Table(name = "PERSON")
				public class Person {

					@Id
					@Column(name = "ID")
					private int id;

					@Column(name = "NAME", length = 20)
					private String name;

					@Column(name = "EMAIL", length = 100)
					private String email;

				}
				""");
	}

	private void createHibernatePropertiesForFlyway() throws Exception {
		File hibernatePropertiesFile = new File(getProjectDir(), "app/src/main/resources/hibernate.properties");
		String contents =
				"hibernate.connection.driver_class=org.h2.Driver\n" +
				"hibernate.connection.url=" + constructJdbcConnectionString() + "\n" +
				"hibernate.connection.username=\n" +
				"hibernate.connection.password=\n";
		Files.writeString(hibernatePropertiesFile.toPath(), contents);
	}

	private void createPersistenceXml() throws Exception {
		File metaInfDir = new File(getProjectDir(), "app/src/main/resources/META-INF");
		metaInfDir.mkdirs();
		File persistenceXmlFile = new File(metaInfDir, "persistence.xml");
		Files.writeString(persistenceXmlFile.toPath(),
				"""
				<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
					<persistence-unit name="default">
						<class>org.example.Person</class>
						<exclude-unlisted-classes>true</exclude-unlisted-classes>
						<properties>
							<property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
							<property name="jakarta.persistence.jdbc.url" value="%s"/>
							<property name="jakarta.persistence.jdbc.user" value=""/>
							<property name="jakarta.persistence.jdbc.password" value=""/>
						</properties>
					</persistence-unit>
				</persistence>
				""".formatted(constructJdbcConnectionString()));
	}
}
