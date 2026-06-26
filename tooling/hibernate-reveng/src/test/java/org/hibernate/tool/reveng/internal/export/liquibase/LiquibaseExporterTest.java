/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LiquibaseExporterTest {

	private static int dbCounter = 0;

	private StandardServiceRegistry serviceRegistry;

	@TempDir
	private File tempDir;

	@BeforeEach
	public void setUp() {
		dbCounter++;
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
				.applySetting( "hibernate.connection.driver_class", "org.h2.Driver" )
				.applySetting( "hibernate.connection.url",
						"jdbc:h2:mem:liquibase_exporter_test_" + dbCounter + ";DB_CLOSE_DELAY=-1" )
				.applySetting( "hibernate.connection.username", "sa" )
				.applySetting( "hibernate.connection.password", "" )
				.build();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testExporterTypeRegistration() {
		var exporter = ExporterFactory.createExporter( ExporterType.LIQUIBASE );
		assertNotNull( exporter );
		assertInstanceOf( LiquibaseExporter.class, exporter );
	}

	@Test
	public void testGenerateChangelogForNewTable() throws Exception {
		final LiquibaseExporter exporter = new LiquibaseExporter();
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR,
				createMetadataDescriptor( Person.class ) );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, tempDir );
		exporter.start();

		File changelog = new File( tempDir, "changelog.xml" );
		assertTrue( changelog.exists(), "Changelog file should be created" );

		String xml = Files.readString( changelog.toPath() );
		assertTrue( xml.contains( "databaseChangeLog" ), "Should contain root element" );
		assertTrue( xml.contains( "createTable" ), "Should contain createTable for new entity" );
		assertTrue( xml.contains( "PERSON" ), "Should reference PERSON table" );
	}

	@Test
	public void testCustomOutputFileName() throws Exception {
		final LiquibaseExporter exporter = new LiquibaseExporter();
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR,
				createMetadataDescriptor( Person.class ) );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, tempDir );
		exporter.getProperties().put( ExporterConstants.OUTPUT_FILE_NAME, "migrations.xml" );
		exporter.start();

		File changelog = new File( tempDir, "migrations.xml" );
		assertTrue( changelog.exists(), "Changelog should use custom filename" );
	}

	@Test
	public void testCustomAuthor() throws Exception {
		final LiquibaseExporter exporter = new LiquibaseExporter();
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR,
				createMetadataDescriptor( Person.class ) );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, tempDir );
		exporter.getProperties().put( ExporterConstants.CHANGESET_AUTHOR, "koen" );
		exporter.start();

		File changelog = new File( tempDir, "changelog.xml" );
		String xml = Files.readString( changelog.toPath() );
		assertTrue( xml.contains( "author=\"koen\"" ), "Should use custom author" );
	}

	@Test
	public void testNoChangelogWhenSchemaMatches() {
		executeDdl( "CREATE TABLE PERSON (ID BIGINT NOT NULL PRIMARY KEY, NAME VARCHAR(255))" );

		final LiquibaseExporter exporter = new LiquibaseExporter();
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR,
				createMetadataDescriptor( Person.class ) );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, tempDir );
		exporter.start();

		File changelog = new File( tempDir, "changelog.xml" );
		assertTrue( !changelog.exists(), "No changelog when schema is up to date" );
	}

	private void executeDdl(String sql) {
		final HibernateSchemaManagementTool tool =
				(HibernateSchemaManagementTool) serviceRegistry.requireService( SchemaManagementTool.class );
		final JdbcContext jdbcContext = tool.resolveJdbcContext( Collections.emptyMap() );
		final var isolator = tool.getDdlTransactionIsolator( jdbcContext );
		try {
			Connection connection = isolator.getIsolatedConnection();
			try ( Statement stmt = connection.createStatement() ) {
				stmt.execute( sql );
			}
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		finally {
			isolator.release();
		}
	}

	private MetadataDescriptor createMetadataDescriptor(Class<?>... entityClasses) {
		return new MetadataDescriptor() {
			@Override
			public Metadata createMetadata() {
				MetadataSources sources = new MetadataSources( serviceRegistry );
				for ( Class<?> entityClass : entityClasses ) {
					sources.addAnnotatedClass( entityClass );
				}
				return sources.buildMetadata();
			}

			@Override
			public Properties getProperties() {
				return new Properties();
			}
		};
	}

	@Entity
	@Table(name = "PERSON")
	static class Person {
		@Id
		private Long id;
		private String name;
	}
}
