/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateFlywayMojoTest {

	private static final String CREATE_PERSON_TABLE =
			"CREATE TABLE PERSON (ID INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY (ID))";
	private static final String DROP_PERSON_TABLE =
			"DROP TABLE IF EXISTS PERSON";

	@TempDir
	private File tempDir;

	private File outputDirectory;

	@BeforeEach
	public void beforeEach() throws Exception {
		createDatabase();
		createOutputDirectory();
	}

	@AfterEach
	public void afterEach() throws Exception {
		dropDatabase();
	}

	@Test
	public void testGenerateFlywayDefault() throws Exception {
		runFlywayExporter("1", "hibernate_schema_update");
		File migrationFile = new File(outputDirectory, "V1__hibernate_schema_update.sql");
		assertTrue(migrationFile.exists(), "Expected Flyway migration file to be generated");
		String content = Files.readString(migrationFile.toPath());
		assertTrue(
				content.contains("alter table") && content.contains("PERSON"),
				"Migration should contain an alter table statement for PERSON");
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testGenerateFlywayCustomVersion() throws Exception {
		runFlywayExporter("5", "hibernate_schema_update");
		File migrationFile = new File(outputDirectory, "V5__hibernate_schema_update.sql");
		assertTrue(migrationFile.exists(), "Expected migration file with version 5");
		String content = Files.readString(migrationFile.toPath());
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testGenerateFlywayCustomDescription() throws Exception {
		runFlywayExporter("1", "add email column");
		File migrationFile = new File(outputDirectory, "V1__add_email_column.sql");
		assertTrue(migrationFile.exists(), "Expected migration file with custom description");
	}

	@Test
	public void testGenerateFlywayFileContainsSql() throws Exception {
		runFlywayExporter("1", "hibernate_schema_update");
		File migrationFile = new File(outputDirectory, "V1__hibernate_schema_update.sql");
		assertTrue(migrationFile.exists());
		String content = Files.readString(migrationFile.toPath());
		assertTrue(content.contains(";"), "Migration should contain SQL delimiter");
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	private void runFlywayExporter(String version, String description) {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, createMetadataDescriptor());
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDirectory);
		exporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, version);
		exporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, description);
		exporter.getProperties().put(ExporterConstants.DELIMITER, ";");
		exporter.getProperties().put(ExporterConstants.FORMAT, true);
		exporter.getProperties().put(ExporterConstants.HALT_ON_ERROR, true);
		exporter.start();
	}

	private void createDatabase() throws Exception {
		try (Connection connection = DriverManager.getConnection(
					constructJdbcConnectionString(), "sa", "");
			Statement statement = connection.createStatement()) {
			statement.execute(CREATE_PERSON_TABLE);
		}
	}

	private void dropDatabase() throws Exception {
		try (Connection connection = DriverManager.getConnection(
					constructJdbcConnectionString(), "sa", "");
			Statement statement = connection.createStatement()) {
			statement.execute(DROP_PERSON_TABLE);
		}
	}

	private void createOutputDirectory() {
		outputDirectory = new File(tempDir, "generated");
		if (!outputDirectory.mkdir()) {
			throw new RuntimeException("Unable to create output directory: " + outputDirectory);
		}
	}

	private MetadataDescriptor createMetadataDescriptor() {
		Properties properties = createProperties();
		return new MetadataDescriptor() {
			@Override
			public Metadata createMetadata() {
				ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
						.clearSettings()
						.applySettings(properties)
						.build();
				return new MetadataSources(serviceRegistry)
						.addAnnotatedClass(Person.class)
						.buildMetadata();
			}

			@Override
			public Properties getProperties() {
				return properties;
			}
		};
	}

	private Properties createProperties() {
		Properties result = new Properties();
		result.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		result.put("hibernate.connection.driver_class", "org.h2.Driver");
		result.put("hibernate.connection.url", constructJdbcConnectionString());
		result.put("hibernate.connection.username", "sa");
		result.put("hibernate.connection.password", "");
		return result;
	}

	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + tempDir.getAbsolutePath() + "/testdb";
	}
}
