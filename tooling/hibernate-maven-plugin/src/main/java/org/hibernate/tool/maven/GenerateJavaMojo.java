/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import java.io.File;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * Mojo to generate Java JPA Entities from an existing database.
 * <p>
 * See: https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821
 */
@Mojo(
	name = "hbm2java",
	defaultPhase = GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateJavaMojo extends AbstractGenerationMojo {

	/** The directory into which the JPA entities will be generated. */
	@Parameter(defaultValue = "${project.build.directory}/generated-sources/")
	private File outputDirectory;

	/** Code will contain JPA features, e.g. using annotations from jakarta.persistence
	 * and org.hibernate.annotations. */
	@Parameter(defaultValue = "true")
	private boolean ejb3;

	/** Code will contain JDK 5 constructs such as generics and static imports. */
	@Parameter(defaultValue = "true")
	private boolean jdk5;

	/** A path used for looking up user-edited templates. */
	@Parameter
	private String templatePath;

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.JAVA);
		pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		pojoExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDirectory);
		if (templatePath != null) {
			getLog().info("Setting template path to: " + templatePath);
			pojoExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] {templatePath});
		}
		pojoExporter.getProperties().setProperty("ejb3", String.valueOf(ejb3));
		pojoExporter.getProperties().setProperty("jdk5", String.valueOf(jdk5));
		getLog().info("Starting POJO export to directory: " + outputDirectory + "...");
		pojoExporter.start();
	}




}
