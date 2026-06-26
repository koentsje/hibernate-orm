/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;

/**
 * Mojo to generate Flyway versioned migration files from schema diffs.
 * Compares the Hibernate metadata model (loaded via JPA bootstrapping from
 * a {@code persistence.xml}) against the existing database and produces
 * a {@code V{version}__{description}.sql} migration file.
 */
@Mojo(
	name = "flyway",
	defaultPhase = PROCESS_CLASSES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateFlywayMojo extends AbstractGenerationMojo {

	/** The directory into which the migration file will be generated. */
	@Parameter(defaultValue = "${project.build.directory}/generated-resources/")
	private File outputDirectory;

	/** The name of the JPA persistence unit to use for entity discovery. */
	@Parameter(defaultValue = "default")
	private String persistenceUnit;

	/** The version number for the Flyway migration file. */
	@Parameter(defaultValue = "1")
	private String migrationVersion;

	/** A human-readable description for the migration. Spaces are replaced with underscores. */
	@Parameter(defaultValue = "hibernate_schema_update")
	private String migrationDescription;

	/** Set the end of statement delimiter. */
	@Parameter(defaultValue = ";")
	private String delimiter;

	/** Should we format the sql strings? */
	@Parameter(defaultValue = "true")
	private boolean format;

	/** Should we stop once an error occurs? */
	@Parameter(defaultValue = "true")
	private boolean haltOnError;

	@Override
	public void execute() throws MojoFailureException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(createExporterClassLoader(original));
			getLog().info("Starting " + this.getClass().getSimpleName() + "...");

			Properties properties = loadFlywayProperties();
			properties.put("hibernate.hbm2ddl.auto", "none");
			MetadataDescriptor descriptor = MetadataDescriptorFactory
					.createJpaDescriptor(persistenceUnit, properties);

			Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
			exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, descriptor);
			exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDirectory);
			exporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, migrationVersion);
			exporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, migrationDescription);
			exporter.getProperties().put(ExporterConstants.DELIMITER, delimiter);
			exporter.getProperties().put(ExporterConstants.FORMAT, format);
			exporter.getProperties().put(ExporterConstants.HALT_ON_ERROR, haltOnError);
			exporter.start();

			getLog().info("Finished " + this.getClass().getSimpleName() + "!");
		}
		finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	private Properties loadFlywayProperties() throws MojoFailureException {
		File propFile = getPropertyFile();
		if (propFile != null && propFile.exists()) {
			try (FileInputStream is = new FileInputStream(propFile)) {
				Properties result = new Properties();
				result.load(is);
				return result;
			}
			catch (FileNotFoundException e) {
				throw new MojoFailureException(propFile + " not found.", e);
			}
			catch (IOException e) {
				throw new MojoFailureException("Problem while loading " + propFile, e);
			}
		}
		return new Properties();
	}

	@Override
	protected void executeExporter(MetadataDescriptor metadataDescriptor) throws MojoFailureException {
		// Not used — execute() drives the FlywayExporter directly
	}
}
