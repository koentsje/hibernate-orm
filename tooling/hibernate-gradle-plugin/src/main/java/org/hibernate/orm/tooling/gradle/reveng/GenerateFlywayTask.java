/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;

@DisableCachingByDefault(because = "Flyway migration generation compares entity classes against a database")
public class GenerateFlywayTask extends RevengTask {

	@TaskAction
	public void performTask() {
		super.perform();
	}

	void doWork() {
		getLogger().lifecycle("Creating Flyway exporter");

		Properties properties = getHibernateProperties();
		properties.put("hibernate.hbm2ddl.auto", "none");

		Exporter flywayExporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		File outputFolder = getOutputFolder();
		flywayExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR,
				MetadataDescriptorFactory.createJpaDescriptor(
						getRevengSpec().persistenceUnit, properties));
		flywayExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputFolder);
		String migrationVersion = getRevengSpec().migrationVersion;
		if (migrationVersion != null) {
			flywayExporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, migrationVersion);
		}
		String migrationDescription = getRevengSpec().migrationDescription;
		if (migrationDescription != null) {
			flywayExporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, migrationDescription);
		}
		getLogger().lifecycle("Starting Flyway migration export to directory: " + outputFolder + "...");
		flywayExporter.start();
		getLogger().lifecycle("Flyway migration export finished");
	}

	@Override
	URL[] resolveProjectClassPath() {
		try {
			List<URL> urls = new ArrayList<>(Arrays.asList(super.resolveProjectClassPath()));
			SourceSetContainer ssc = getProject().getExtensions().getByType(SourceSetContainer.class);
			SourceSet mainSourceSet = ssc.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			for (File classesDir : mainSourceSet.getOutput().getClassesDirs()) {
				urls.add(classesDir.toURI().toURL());
			}
			File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
			if (resourcesDir != null) {
				urls.add(resourcesDir.toURI().toURL());
			}
			return urls.toArray(new URL[0]);
		}
		catch (MalformedURLException e) {
			throw new BuildException(e);
		}
	}

}
