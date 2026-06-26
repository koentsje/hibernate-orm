/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.flyway;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.hibernate.tool.schema.TargetType;

import java.io.File;
import java.util.EnumSet;

/**
 * Generates Flyway versioned migration files from schema diffs.
 * Uses {@link SchemaUpdate} to compare Hibernate metadata against the
 * existing database and produces a {@code V{version}__{description}.sql} file.
 */
public class FlywayExporter extends AbstractExporter {

	@Override
	protected void doStart() {
		Metadata metadata = getMetadata();
		String version = getMigrationVersion();
		String description = getMigrationDescription().replace( ' ', '_' );
		String fileName = "V" + version + "__" + description + ".sql";

		SchemaUpdate update = new SchemaUpdate();
		File outputFile = new File( getOutputDirectory(), fileName );
		update.setOutputFile( outputFile.getPath() );
		update.setDelimiter( getDelimiter() );
		update.setFormat( getFormat() );
		if ( getHaltOnError() ) {
			update.setHaltOnError( true );
		}

		EnumSet<TargetType> targetTypes = EnumSet.of( TargetType.SCRIPT );
		update.execute( targetTypes, metadata );

		if ( !update.getExceptions().isEmpty() ) {
			int i = 1;
			for ( Throwable element : update.getExceptions() ) {
				log.warn( "Error #" + i + ": ", element );
				i++;
			}
			log.error( ( i - 1 ) + " errors occurred while generating Flyway migration." );
			if ( getHaltOnError() ) {
				throw new RuntimeException( "Errors while generating Flyway migration" );
			}
		}
	}

	private String getMigrationVersion() {
		if ( !getProperties().containsKey( MIGRATION_VERSION ) ) {
			return "1";
		}
		return (String) getProperties().get( MIGRATION_VERSION );
	}

	private String getMigrationDescription() {
		if ( !getProperties().containsKey( MIGRATION_DESCRIPTION ) ) {
			return "hibernate_schema_update";
		}
		return (String) getProperties().get( MIGRATION_DESCRIPTION );
	}

	private String getDelimiter() {
		if ( !getProperties().containsKey( DELIMITER ) ) {
			return ";";
		}
		return (String) getProperties().get( DELIMITER );
	}

	private boolean getFormat() {
		if ( !getProperties().containsKey( FORMAT ) ) {
			return true;
		}
		return (boolean) getProperties().get( FORMAT );
	}

	private boolean getHaltOnError() {
		if ( !getProperties().containsKey( HALT_ON_ERROR ) ) {
			return false;
		}
		return (boolean) getProperties().get( HALT_ON_ERROR );
	}
}
