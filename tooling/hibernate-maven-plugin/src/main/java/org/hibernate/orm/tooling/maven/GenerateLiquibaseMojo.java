/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.metadata.JpaMetadataDescriptor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_CLASSES;

@Mojo(
		name = "liquibase",
		defaultPhase = PROCESS_CLASSES,
		requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateLiquibaseMojo extends AbstractMojo {

	@Parameter(defaultValue = "default")
	private String persistenceUnit;

	@Parameter(defaultValue = "${project.build.directory}/generated-resources/")
	private File outputDirectory;

	@Parameter(defaultValue = "changelog.xml")
	private String outputFileName;

	@Parameter(defaultValue = "xml")
	private String changelogFormat;

	@Parameter(defaultValue = "hibernate")
	private String changesetAuthor;

	@Parameter(defaultValue = "false")
	private boolean haltOnError;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader( createClassLoader( original ) );
			getLog().info( "Generating Liquibase changelog for persistence unit: " + persistenceUnit );
			executeExporter( new JpaMetadataDescriptor( persistenceUnit, new Properties() ) );
			getLog().info( "Liquibase changelog generation complete." );
		}
		catch ( Exception e ) {
			throw new MojoExecutionException( "Failed to generate Liquibase changelog", e );
		}
		finally {
			Thread.currentThread().setContextClassLoader( original );
		}
	}

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		Exporter exporter = ExporterFactory.createExporter( ExporterType.LIQUIBASE );
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, outputDirectory );
		exporter.getProperties().put( ExporterConstants.OUTPUT_FILE_NAME, outputFileName );
		exporter.getProperties().put( ExporterConstants.CHANGELOG_FORMAT, changelogFormat );
		exporter.getProperties().put( ExporterConstants.CHANGESET_AUTHOR, changesetAuthor );
		exporter.getProperties().put( ExporterConstants.HALT_ON_ERROR, haltOnError );
		exporter.start();
	}

	private ClassLoader createClassLoader(ClassLoader parent) {
		ArrayList<URL> urls = new ArrayList<>();
		try {
			for ( String element : project.getRuntimeClasspathElements() ) {
				urls.add( new File( element ).toURI().toURL() );
			}
		}
		catch ( DependencyResolutionRequiredException | MalformedURLException e ) {
			throw new RuntimeException( "Problem while constructing project classloader", e );
		}
		return new URLClassLoader( urls.toArray( new URL[0] ), parent );
	}
}
