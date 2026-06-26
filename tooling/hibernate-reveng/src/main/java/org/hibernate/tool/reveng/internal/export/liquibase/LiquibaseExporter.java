/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.internal.Helper;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class LiquibaseExporter extends AbstractExporter {

	@Override
	protected void doStart() {
		final Metadata metadata = getMetadata();
		final MetadataImplementor metadataImpl = (MetadataImplementor) metadata;
		final ServiceRegistry sr = metadataImpl.getMetadataBuildingOptions().getServiceRegistry();
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
				final List<SchemaChange> changes = new SchemaComparator().compare(
						metadata, dbInfo, jdbcContext.getDialect() );
				if ( !changes.isEmpty() ) {
					writeChangelog( changes );
				}
			}
			finally {
				dbInfo.cleanup();
			}
		}
		catch ( RuntimeException e ) {
			if ( getHaltOnError() ) {
				throw e;
			}
			log.error( "Error generating Liquibase changelog", e );
		}
		finally {
			isolator.release();
		}
	}

	private void writeChangelog(List<SchemaChange> changes) {
		final String outputFileName = getOutputFileName();
		final File outputFile = new File( getOutputDirectory(), outputFileName );
		final String author = getChangesetAuthor();
		final ChangelogWriter writer = createChangelogWriter();
		writer.write( changes, outputFile, author );
		getArtifactCollector().addFile( outputFile, "changelog" );
		log.info( "Liquibase changelog written to " + outputFile.getAbsolutePath() );
	}

	private ChangelogWriter createChangelogWriter() {
		final String format = getChangelogFormat();
		if ( "xml".equalsIgnoreCase( format ) ) {
			return new XmlChangelogWriter();
		}
		throw new IllegalArgumentException( "Unsupported changelog format: " + format
				+ ". Supported formats: xml" );
	}

	private String getOutputFileName() {
		final String fileName = getProperties().getProperty( OUTPUT_FILE_NAME );
		return fileName != null ? fileName : "changelog.xml";
	}

	private String getChangelogFormat() {
		final String format = getProperties().getProperty( CHANGELOG_FORMAT );
		return format != null ? format : "xml";
	}

	private String getChangesetAuthor() {
		final String author = getProperties().getProperty( CHANGESET_AUTHOR );
		return author != null ? author : "hibernate";
	}

	private boolean getHaltOnError() {
		if ( !getProperties().containsKey( HALT_ON_ERROR ) ) {
			return false;
		}
		return (boolean) getProperties().get( HALT_ON_ERROR );
	}
}
