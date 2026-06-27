/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.reveng.internal.metadata.JpaMetadataDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateLiquibaseTaskTest {

	@TempDir
	private File tempDir;

	private File outputDirectory;
	private GenerateLiquibaseTask task;

	@BeforeEach
	public void beforeEach() {
		outputDirectory = new File( tempDir, "generated" );
		if ( !outputDirectory.mkdir() ) {
			throw new RuntimeException( "Unable to create output directory: " + outputDirectory );
		}
		Project project = ProjectBuilder.builder().build();
		task = project.getTasks().create( "generateLiquibase", GenerateLiquibaseTask.class );
		task.outputDirectory = outputDirectory;
	}

	@AfterEach
	public void afterEach() {
		try ( Connection conn = DriverManager.getConnection( constructJdbcConnectionString() ) ) {
			try ( Statement stmt = conn.createStatement() ) {
				stmt.execute( "DROP ALL OBJECTS" );
			}
		}
		catch ( Exception ignored ) {
		}
	}

	@Test
	public void testGenerateChangelog() throws Exception {
		task.executeExporter( createJpaDescriptor() );
		File changelog = new File( outputDirectory, "changelog.xml" );
		assertTrue( changelog.exists(), "Expected changelog.xml to be generated" );
		String xml = Files.readString( changelog.toPath() );
		assertTrue( xml.contains( "databaseChangeLog" ), "Should contain root element" );
		assertTrue( xml.contains( "createTable" ), "Should contain createTable" );
		assertTrue( xml.contains( "PERSON" ), "Should reference PERSON table" );
	}

	@Test
	public void testCustomAuthor() throws Exception {
		task.changesetAuthor = "koen";
		task.executeExporter( createJpaDescriptor() );
		File changelog = new File( outputDirectory, "changelog.xml" );
		String xml = Files.readString( changelog.toPath() );
		assertTrue( xml.contains( "author=\"koen\"" ), "Should use custom author" );
	}

	@Test
	public void testCustomOutputFileName() throws Exception {
		task.outputFileName = "migrations.xml";
		task.executeExporter( createJpaDescriptor() );
		File changelog = new File( outputDirectory, "migrations.xml" );
		assertTrue( changelog.exists(), "Expected migrations.xml to be generated" );
	}

	private JpaMetadataDescriptor createJpaDescriptor() throws Exception {
		writePersistenceXml();
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		URLClassLoader cl = new URLClassLoader(
				new URL[]{ tempDir.toURI().toURL() }, original );
		Thread.currentThread().setContextClassLoader( cl );
		try {
			Properties props = new Properties();
			props.put( "hibernate.connection.url", constructJdbcConnectionString() );
			return new JpaMetadataDescriptor( "liquibase-gradle-test", props );
		}
		finally {
			Thread.currentThread().setContextClassLoader( original );
		}
	}

	private void writePersistenceXml() throws Exception {
		File metaInf = new File( tempDir, "META-INF" );
		metaInf.mkdirs();
		Files.writeString( new File( metaInf, "persistence.xml" ).toPath(),
				"""
				<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0">
					<persistence-unit name="liquibase-gradle-test">
						<class>org.hibernate.orm.tooling.gradle.GenerateLiquibaseTaskTest$Person</class>
						<exclude-unlisted-classes>true</exclude-unlisted-classes>
						<properties>
							<property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
							<property name="hibernate.connection.driver_class" value="org.h2.Driver"/>
							<property name="hibernate.connection.username" value="sa"/>
							<property name="hibernate.connection.password" value=""/>
						</properties>
					</persistence-unit>
				</persistence>
				""" );
	}

	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + tempDir.getAbsolutePath() + "/database/liquibase_test;AUTO_SERVER=TRUE";
	}

	@Entity
	@Table(name = "PERSON")
	public static class Person {
		@Id
		private Long id;
		private String name;
	}
}
