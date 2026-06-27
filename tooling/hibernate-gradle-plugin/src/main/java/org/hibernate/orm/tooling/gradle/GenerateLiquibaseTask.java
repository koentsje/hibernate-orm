/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.metadata.JpaMetadataDescriptor;

@DisableCachingByDefault(because = "Liquibase migration generation performs JDBC operations and is not cacheable")
public class GenerateLiquibaseTask extends DefaultTask {

	public String persistenceUnit = "default";
	public File outputDirectory;
	public String outputFileName = "changelog.xml";
	public String changelogFormat = "xml";
	public String changesetAuthor = "hibernate";
	public boolean haltOnError = false;

	@TaskAction
	public void performTask() {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					new URLClassLoader( resolveProjectClassPath(), oldLoader ) );
			getLogger().lifecycle( "Generating Liquibase changelog for persistence unit: " + persistenceUnit );
			executeExporter( new JpaMetadataDescriptor( persistenceUnit, new Properties() ) );
			getLogger().lifecycle( "Liquibase changelog generation complete." );
		}
		finally {
			Thread.currentThread().setContextClassLoader( oldLoader );
		}
	}

	void executeExporter(MetadataDescriptor metadataDescriptor) {
		Exporter exporter = ExporterFactory.createExporter( ExporterType.LIQUIBASE );
		exporter.getProperties().put( ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor );
		exporter.getProperties().put( ExporterConstants.DESTINATION_FOLDER, getResolvedOutputDirectory() );
		exporter.getProperties().put( ExporterConstants.OUTPUT_FILE_NAME, outputFileName );
		exporter.getProperties().put( ExporterConstants.CHANGELOG_FORMAT, changelogFormat );
		exporter.getProperties().put( ExporterConstants.CHANGESET_AUTHOR, changesetAuthor );
		exporter.getProperties().put( ExporterConstants.HALT_ON_ERROR, haltOnError );
		exporter.start();
	}

	@Internal
	File getResolvedOutputDirectory() {
		if ( outputDirectory != null ) {
			return outputDirectory;
		}
		return new File( getProject().getProjectDir(), "build/generated-resources" );
	}

	URL[] resolveProjectClassPath() {
		try {
			ConfigurationContainer cc = getProject().getConfigurations();
			Configuration defaultConf = cc.getByName( "compileClasspath" );
			ResolvedConfiguration resolvedConf = defaultConf.getResolvedConfiguration();
			Set<ResolvedArtifact> ras = resolvedConf.getResolvedArtifacts();
			ResolvedArtifact[] resolvedArtifacts = ras.toArray( new ResolvedArtifact[ras.size()] );
			URL[] urls = new URL[ras.size()];
			for ( int i = 0; i < ras.size(); i++ ) {
				urls[i] = resolvedArtifacts[i].getFile().toURI().toURL();
			}
			return urls;
		}
		catch ( MalformedURLException e ) {
			getLogger().error( "MalformedURLException while compiling project classpath" );
			throw new BuildException( e );
		}
	}
}
