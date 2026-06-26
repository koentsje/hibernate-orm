/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class XmlChangelogWriter implements ChangelogWriter {

	private static final String NAMESPACE = "http://www.liquibase.org/xml/ns/dbchangelog";
	private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String SCHEMA_LOCATION = NAMESPACE
			+ " http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd";

	@Override
	public void write(List<SchemaChange> changes, File outputFile, String author) {
		outputFile.getParentFile().mkdirs();
		try (OutputStreamWriter out = new OutputStreamWriter(
				new FileOutputStream( outputFile ), StandardCharsets.UTF_8 )) {
			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter( out );
			try {
				writeDocument( writer, changes, author );
			}
			finally {
				writer.close();
			}
		}
		catch (IOException | XMLStreamException e) {
			throw new RuntimeException( "Failed to write Liquibase changelog: " + outputFile, e );
		}
	}

	private void writeDocument(XMLStreamWriter writer, List<SchemaChange> changes, String author)
			throws XMLStreamException {
		writer.writeStartDocument( "UTF-8", "1.0" );
		writeNewLine( writer );
		writer.writeStartElement( "databaseChangeLog" );
		writer.writeDefaultNamespace( NAMESPACE );
		writer.writeNamespace( "xsi", XSI_NAMESPACE );
		writer.writeAttribute( XSI_NAMESPACE, "schemaLocation", SCHEMA_LOCATION );
		writeNewLine( writer );

		int changeSetId = 1;
		for ( SchemaChange change : changes ) {
			writeNewLine( writer );
			writeChangeSet( writer, change, String.valueOf( changeSetId++ ), author );
		}

		writeNewLine( writer );
		writer.writeEndElement();
		writeNewLine( writer );
		writer.writeEndDocument();
	}

	private void writeChangeSet(XMLStreamWriter writer, SchemaChange change, String id, String author)
			throws XMLStreamException {
		writeIndent( writer, 1 );
		writer.writeStartElement( "changeSet" );
		writer.writeAttribute( "id", id );
		writer.writeAttribute( "author", author );
		writeNewLine( writer );

		if ( change instanceof SchemaChange.CreateTable ct ) {
			writeCreateTable( writer, ct );
		}
		else if ( change instanceof SchemaChange.AddColumn ac ) {
			writeAddColumn( writer, ac );
		}
		else if ( change instanceof SchemaChange.ModifyColumnType mct ) {
			writeModifyColumnType( writer, mct );
		}
		else if ( change instanceof SchemaChange.AddForeignKey afk ) {
			writeAddForeignKey( writer, afk );
		}
		else if ( change instanceof SchemaChange.CreateIndex ci ) {
			writeCreateIndex( writer, ci );
		}
		else if ( change instanceof SchemaChange.AddUniqueConstraint auc ) {
			writeAddUniqueConstraint( writer, auc );
		}

		writeIndent( writer, 1 );
		writer.writeEndElement();
		writeNewLine( writer );
	}

	private void writeCreateTable(XMLStreamWriter writer, SchemaChange.CreateTable ct)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeStartElement( "createTable" );
		writer.writeAttribute( "tableName", ct.tableName() );
		writeNewLine( writer );

		for ( SchemaChange.ColumnDef col : ct.columns() ) {
			writeColumnElement( writer, col, ct.primaryKeyColumns().contains( col.name() ) );
		}

		writeIndent( writer, 2 );
		writer.writeEndElement();
		writeNewLine( writer );
	}

	private void writeAddColumn(XMLStreamWriter writer, SchemaChange.AddColumn ac)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeStartElement( "addColumn" );
		writer.writeAttribute( "tableName", ac.tableName() );
		writeNewLine( writer );

		writeColumnElement( writer, ac.column(), false );

		writeIndent( writer, 2 );
		writer.writeEndElement();
		writeNewLine( writer );
	}

	private void writeModifyColumnType(XMLStreamWriter writer, SchemaChange.ModifyColumnType mct)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeEmptyElement( "modifyDataType" );
		writer.writeAttribute( "tableName", mct.tableName() );
		writer.writeAttribute( "columnName", mct.columnName() );
		writer.writeAttribute( "newDataType", mct.newType() );
		writeNewLine( writer );
	}

	private void writeAddForeignKey(XMLStreamWriter writer, SchemaChange.AddForeignKey afk)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeEmptyElement( "addForeignKeyConstraint" );
		writer.writeAttribute( "constraintName", afk.constraintName() );
		writer.writeAttribute( "baseTableName", afk.baseTableName() );
		writer.writeAttribute( "baseColumnNames", String.join( ",", afk.baseColumnNames() ) );
		writer.writeAttribute( "referencedTableName", afk.referencedTableName() );
		writer.writeAttribute( "referencedColumnNames", String.join( ",", afk.referencedColumnNames() ) );
		writeNewLine( writer );
	}

	private void writeCreateIndex(XMLStreamWriter writer, SchemaChange.CreateIndex ci)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeStartElement( "createIndex" );
		writer.writeAttribute( "indexName", ci.indexName() );
		writer.writeAttribute( "tableName", ci.tableName() );
		if ( ci.unique() ) {
			writer.writeAttribute( "unique", "true" );
		}
		writeNewLine( writer );

		for ( String colName : ci.columnNames() ) {
			writeIndent( writer, 3 );
			writer.writeEmptyElement( "column" );
			writer.writeAttribute( "name", colName );
			writeNewLine( writer );
		}

		writeIndent( writer, 2 );
		writer.writeEndElement();
		writeNewLine( writer );
	}

	private void writeAddUniqueConstraint(XMLStreamWriter writer, SchemaChange.AddUniqueConstraint auc)
			throws XMLStreamException {
		writeIndent( writer, 2 );
		writer.writeEmptyElement( "addUniqueConstraint" );
		writer.writeAttribute( "constraintName", auc.constraintName() );
		writer.writeAttribute( "tableName", auc.tableName() );
		writer.writeAttribute( "columnNames", String.join( ",", auc.columnNames() ) );
		writeNewLine( writer );
	}

	private void writeColumnElement(XMLStreamWriter writer, SchemaChange.ColumnDef col, boolean isPrimaryKey)
			throws XMLStreamException {
		boolean hasConstraints = isPrimaryKey || !col.nullable();
		writeIndent( writer, 3 );
		if ( col.defaultValue() == null && !hasConstraints ) {
			writer.writeEmptyElement( "column" );
		}
		else {
			writer.writeStartElement( "column" );
		}
		writer.writeAttribute( "name", col.name() );
		writer.writeAttribute( "type", col.type() );
		if ( col.defaultValue() != null ) {
			writer.writeAttribute( "defaultValue", col.defaultValue() );
		}

		if ( hasConstraints ) {
			writeNewLine( writer );
			writeIndent( writer, 4 );
			writer.writeEmptyElement( "constraints" );
			if ( isPrimaryKey ) {
				writer.writeAttribute( "primaryKey", "true" );
			}
			if ( !col.nullable() ) {
				writer.writeAttribute( "nullable", "false" );
			}
			writeNewLine( writer );
			writeIndent( writer, 3 );
			writer.writeEndElement();
		}
		else if ( col.defaultValue() != null ) {
			writer.writeEndElement();
		}
		writeNewLine( writer );
	}

	private void writeIndent(XMLStreamWriter writer, int level) throws XMLStreamException {
		for ( int i = 0; i < level; i++ ) {
			writer.writeCharacters( "    " );
		}
	}

	private void writeNewLine(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeCharacters( "\n" );
	}
}
