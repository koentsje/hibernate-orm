/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.FlywayExporterTest;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCase {

	@TempDir
	public File outputFolder;

	private MetadataDescriptor metadataDescriptor;

	@BeforeEach
	public void setUp() throws Exception {
		try (Connection conn = DriverManager.getConnection(constructJdbcUrl(), "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute(
					"CREATE TABLE PERSON (ID INT NOT NULL, NAME VARCHAR(20), PRIMARY KEY (ID))");
		}
		Properties properties = new Properties();
		properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		properties.put("hibernate.connection.driver_class", "org.h2.Driver");
		properties.put("hibernate.connection.url", constructJdbcUrl());
		properties.put("hibernate.connection.username", "sa");
		properties.put("hibernate.connection.password", "");
		metadataDescriptor = new MetadataDescriptor() {
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

	@AfterEach
	public void tearDown() throws Exception {
		try (Connection conn = DriverManager.getConnection(constructJdbcUrl(), "sa", "");
			Statement stmt = conn.createStatement()) {
			stmt.execute("DROP TABLE IF EXISTS PERSON");
		}
	}

	@Test
	public void testFlywayExporterDefaultFileName() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.start();

		File scriptFile = new File(outputFolder, "V1__hibernate_schema_update.sql");
		assertTrue(scriptFile.exists(), "Migration file should exist with default naming");
		String content = Files.readString(scriptFile.toPath());
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testFlywayExporterCustomVersion() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, "42");
		exporter.start();

		File scriptFile = new File(outputFolder, "V42__hibernate_schema_update.sql");
		assertTrue(scriptFile.exists(), "Migration file should use custom version number");
		String content = Files.readString(scriptFile.toPath());
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testFlywayExporterCustomDescription() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, "add email column");
		exporter.start();

		File scriptFile = new File(outputFolder, "V1__add_email_column.sql");
		assertTrue(scriptFile.exists(), "Spaces in description should be replaced by underscores");
		String content = Files.readString(scriptFile.toPath());
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testFlywayExporterDottedVersion() throws Exception {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		exporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, "2.1");
		exporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, "add email");
		exporter.start();

		File scriptFile = new File(outputFolder, "V2.1__add_email.sql");
		assertTrue(scriptFile.exists(), "Migration file should support dotted version numbers");
		String content = Files.readString(scriptFile.toPath());
		assertTrue(
				content.contains("alter table if exists PERSON"),
				"Migration should alter the existing PERSON table: " + content);
		assertTrue(
				content.contains("add column EMAIL varchar(100)"),
				"Migration should add the EMAIL column: " + content);
	}

	@Test
	public void testExporterTypeRegistration() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		assertEquals(
				"org.hibernate.tool.reveng.internal.export.flyway.FlywayExporter",
				exporter.getClass().getName());
	}

	private String constructJdbcUrl() {
		return "jdbc:h2:" + outputFolder.getAbsolutePath() + "/testdb";
	}
}
