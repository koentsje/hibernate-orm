/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaComparatorTest {

	private StandardServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
				.applySetting( "hibernate.connection.driver_class", "org.h2.Driver" )
				.applySetting( "hibernate.connection.url", "jdbc:h2:mem:schema_comparator_test;DB_CLOSE_DELAY=-1" )
				.applySetting( "hibernate.connection.username", "sa" )
				.applySetting( "hibernate.connection.password", "" )
				.build();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			executeDdl( "DROP ALL OBJECTS" );
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testCreateTableWhenTableMissing() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Person.class )
				.buildMetadata();

		List<SchemaChange> changes = compareWithDatabase( metadata );

		boolean hasCreatePerson = changes.stream()
				.anyMatch( c -> c instanceof SchemaChange.CreateTable ct
						&& "PERSON".equalsIgnoreCase( ct.tableName() ) );
		assertTrue( hasCreatePerson, "Should detect missing PERSON table" );

		SchemaChange.CreateTable create = changes.stream()
				.filter( c -> c instanceof SchemaChange.CreateTable ct
						&& "PERSON".equalsIgnoreCase( ct.tableName() ) )
				.map( c -> (SchemaChange.CreateTable) c )
				.findFirst().orElseThrow();
		assertTrue( create.columns().stream().anyMatch( col -> "ID".equalsIgnoreCase( col.name() ) ),
				"Should include ID column" );
		assertTrue( create.columns().stream().anyMatch( col -> "NAME".equalsIgnoreCase( col.name() ) ),
				"Should include NAME column" );
		assertTrue( create.primaryKeyColumns().stream().anyMatch( pk -> "ID".equalsIgnoreCase( pk ) ),
				"Should have ID as primary key" );
	}

	@Test
	public void testNoChangesWhenSchemaMatches() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Person.class )
				.buildMetadata();

		executeDdl( "CREATE TABLE PERSON (ID BIGINT NOT NULL PRIMARY KEY, NAME VARCHAR(255))" );

		List<SchemaChange> changes = compareWithDatabase( metadata );

		boolean hasPersonChange = changes.stream()
				.anyMatch( c -> {
					if ( c instanceof SchemaChange.CreateTable ct ) {
						return "PERSON".equalsIgnoreCase( ct.tableName() );
					}
					if ( c instanceof SchemaChange.AddColumn ac ) {
						return "PERSON".equalsIgnoreCase( ac.tableName() );
					}
					return false;
				} );
		assertTrue( !hasPersonChange, "Should detect no changes for PERSON when schema matches" );
	}

	@Test
	public void testAddColumnWhenColumnMissing() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( PersonWithEmail.class )
				.buildMetadata();

		executeDdl( "CREATE TABLE PERSON_WITH_EMAIL (ID BIGINT NOT NULL PRIMARY KEY, NAME VARCHAR(255))" );

		List<SchemaChange> changes = compareWithDatabase( metadata );

		boolean hasAddEmail = changes.stream()
				.anyMatch( c -> c instanceof SchemaChange.AddColumn ac
						&& "EMAIL".equalsIgnoreCase( ac.column().name() ) );
		assertTrue( hasAddEmail, "Should detect missing EMAIL column" );
	}

	@Test
	public void testForeignKeyDetection() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Author.class )
				.addAnnotatedClass( Book.class )
				.buildMetadata();

		executeDdl( "CREATE TABLE AUTHOR (ID BIGINT NOT NULL PRIMARY KEY, NAME VARCHAR(255))" );
		executeDdl( "CREATE TABLE BOOK (ID BIGINT NOT NULL PRIMARY KEY, TITLE VARCHAR(255), AUTHOR_ID BIGINT)" );

		List<SchemaChange> changes = compareWithDatabase( metadata );

		boolean hasForeignKey = changes.stream()
				.anyMatch( c -> c instanceof SchemaChange.AddForeignKey afk
						&& "BOOK".equalsIgnoreCase( afk.baseTableName() )
						&& "AUTHOR".equalsIgnoreCase( afk.referencedTableName() ) );
		assertTrue( hasForeignKey, "Should detect missing FK from BOOK to AUTHOR" );
	}

	@Test
	public void testExistingForeignKeyNotDuplicated() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Author.class )
				.addAnnotatedClass( Book.class )
				.buildMetadata();

		executeDdl( "CREATE TABLE AUTHOR (ID BIGINT NOT NULL PRIMARY KEY, NAME VARCHAR(255))" );
		executeDdl( "CREATE TABLE BOOK (ID BIGINT NOT NULL PRIMARY KEY, TITLE VARCHAR(255), "
				+ "AUTHOR_ID BIGINT, CONSTRAINT FK_BOOK_AUTHOR FOREIGN KEY (AUTHOR_ID) REFERENCES AUTHOR(ID))" );

		List<SchemaChange> changes = compareWithDatabase( metadata );

		boolean hasForeignKey = changes.stream()
				.anyMatch( c -> c instanceof SchemaChange.AddForeignKey afk
						&& "BOOK".equalsIgnoreCase( afk.baseTableName() ) );
		assertTrue( !hasForeignKey, "Should not duplicate existing FK" );
	}

	@Test
	public void testMultipleTablesCreateTable() {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( Author.class )
				.addAnnotatedClass( Book.class )
				.buildMetadata();

		List<SchemaChange> changes = compareWithDatabase( metadata );

		long createTableCount = changes.stream()
				.filter( c -> c instanceof SchemaChange.CreateTable )
				.count();
		assertTrue( createTableCount >= 2, "Should create both AUTHOR and BOOK tables" );
	}

	private List<SchemaChange> compareWithDatabase(Metadata metadata) {
		final MetadataImplementor metadataImpl = (MetadataImplementor) metadata;
		final var sr = metadataImpl.getMetadataBuildingOptions().getServiceRegistry();
		final HibernateSchemaManagementTool tool =
				(HibernateSchemaManagementTool) sr.requireService( SchemaManagementTool.class );
		final JdbcContext jdbcContext = tool.resolveJdbcContext( Collections.emptyMap() );
		final DdlTransactionIsolator isolator = tool.getDdlTransactionIsolator( jdbcContext );
		try {
			final SqlStringGenerationContext sqlContext = SqlStringGenerationContextImpl.fromConfigurationMap(
					sr.requireService( JdbcEnvironment.class ),
					metadata.getDatabase(),
					Collections.emptyMap()
			);
			final DatabaseInformation dbInfo = Helper.buildDatabaseInformation( isolator, sqlContext, tool );
			try {
				final Dialect dialect = jdbcContext.getDialect();
				return new SchemaComparator().compare( metadata, dbInfo, dialect );
			}
			finally {
				dbInfo.cleanup();
			}
		}
		finally {
			isolator.release();
		}
	}

	private void executeDdl(String sql) {
		final var sr = serviceRegistry;
		final HibernateSchemaManagementTool tool =
				(HibernateSchemaManagementTool) sr.requireService( SchemaManagementTool.class );
		final JdbcContext jdbcContext = tool.resolveJdbcContext( Collections.emptyMap() );
		final DdlTransactionIsolator isolator = tool.getDdlTransactionIsolator( jdbcContext );
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

	@Entity
	@Table(name = "PERSON")
	static class Person {
		@Id
		private Long id;
		private String name;
	}

	@Entity
	@Table(name = "PERSON_WITH_EMAIL")
	static class PersonWithEmail {
		@Id
		private Long id;
		private String name;
		private String email;
	}

	@Entity
	@Table(name = "AUTHOR")
	static class Author {
		@Id
		private Long id;
		private String name;
	}

	@Entity
	@Table(name = "BOOK")
	static class Book {
		@Id
		private Long id;
		private String title;
		@ManyToOne
		private Author author;
	}
}
