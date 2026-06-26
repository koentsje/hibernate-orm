/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

public class FlywayExporterTask extends ExporterTask {

	String migrationVersion = "1";
	String migrationDescription = "hibernate_schema_update";
	String delimiter = ";";
	boolean format = true;
	private boolean haltOnError = false;

	public FlywayExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public String getName() {
		return "flyway (Generates Flyway versioned migration)";
	}

	protected Exporter configureExporter(Exporter exp) {
		Exporter exporter = super.configureExporter( exp );
		exporter.getProperties().put(ExporterConstants.MIGRATION_VERSION, migrationVersion);
		exporter.getProperties().put(ExporterConstants.MIGRATION_DESCRIPTION, migrationDescription);
		exporter.getProperties().put(ExporterConstants.DELIMITER, delimiter);
		exporter.getProperties().put(ExporterConstants.FORMAT, format);
		exporter.getProperties().put(ExporterConstants.HALT_ON_ERROR, haltOnError);
		return exporter;
	}

	protected Exporter createExporter() {
		Exporter exporter = ExporterFactory.createExporter(ExporterType.FLYWAY);
		exporter.getProperties().putAll(parent.getProperties());
		exporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, parent.getProperties());
		exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, parent.getDestDir());
		return exporter;
	}

	public void setMigrationVersion(String migrationVersion) {
		this.migrationVersion = migrationVersion;
	}

	public void setMigrationDescription(String migrationDescription) {
		this.migrationDescription = migrationDescription;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setFormat(boolean format) {
		this.format = format;
	}

	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}
}
