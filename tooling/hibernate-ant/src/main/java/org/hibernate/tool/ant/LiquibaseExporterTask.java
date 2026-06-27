/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;

public class LiquibaseExporterTask extends ExporterTask {

	String outputFileName = "changelog.xml";
	String changelogFormat = "xml";
	String changesetAuthor = "hibernate";
	boolean haltOnError = false;

	public LiquibaseExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	public String getName() {
		return "liquibase (Generates a Liquibase changelog)";
	}

	protected Exporter createExporter() {
		return ExporterFactory.createExporter( ExporterType.LIQUIBASE );
	}

	protected Exporter configureExporter(Exporter exp) {
		Exporter exporter = super.configureExporter( exp );
		exporter.getProperties().put( ExporterConstants.OUTPUT_FILE_NAME, outputFileName );
		exporter.getProperties().put( ExporterConstants.CHANGELOG_FORMAT, changelogFormat );
		exporter.getProperties().put( ExporterConstants.CHANGESET_AUTHOR, changesetAuthor );
		exporter.getProperties().put( ExporterConstants.HALT_ON_ERROR, haltOnError );
		return exporter;
	}

	public void setOutputFileName(String fileName) {
		this.outputFileName = fileName;
	}

	public void setChangelogFormat(String format) {
		this.changelogFormat = format;
	}

	public void setChangesetAuthor(String author) {
		this.changesetAuthor = author;
	}

	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}
}
