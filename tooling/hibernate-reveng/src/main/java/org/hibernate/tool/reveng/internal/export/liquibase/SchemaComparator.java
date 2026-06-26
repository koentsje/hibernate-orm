/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SchemaComparator {

	public List<SchemaChange> compare(Metadata metadata, DatabaseInformation databaseInformation, Dialect dialect) {
		final List<SchemaChange> changes = new ArrayList<>();
		for ( var namespace : metadata.getDatabase().getNamespaces() ) {
			for ( var table : namespace.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}
				final var tableInfo = databaseInformation.getTableInformation( table.getQualifiedTableName() );
				if ( tableInfo == null ) {
					addCreateTable( changes, table, metadata );
				}
				else {
					addColumnChanges( changes, table, tableInfo, metadata, dialect );
					addIndexChanges( changes, table, tableInfo );
					addUniqueKeyChanges( changes, table, tableInfo );
				}
			}
		}
		for ( var namespace : metadata.getDatabase().getNamespaces() ) {
			for ( var table : namespace.getTables() ) {
				if ( !table.isPhysicalTable() ) {
					continue;
				}
				final var tableInfo = databaseInformation.getTableInformation( table.getQualifiedTableName() );
				addForeignKeyChanges( changes, table, tableInfo );
			}
		}
		return changes;
	}

	private void addCreateTable(List<SchemaChange> changes, Table table, Metadata metadata) {
		final List<SchemaChange.ColumnDef> columnDefs = new ArrayList<>();
		final List<String> pkColumns = new ArrayList<>();

		final PrimaryKey pk = table.getPrimaryKey();
		if ( pk != null ) {
			for ( var col : pk.getColumns() ) {
				pkColumns.add( col.getName() );
			}
		}

		for ( var column : table.getColumns() ) {
			columnDefs.add( toColumnDef( column, metadata ) );
		}

		changes.add( new SchemaChange.CreateTable( table.getName(), columnDefs, pkColumns ) );
	}

	private void addColumnChanges(
			List<SchemaChange> changes,
			Table table,
			TableInformation tableInfo,
			Metadata metadata,
			Dialect dialect) {
		for ( var column : table.getColumns() ) {
			final var columnInfo = tableInfo.getColumn(
					Identifier.toIdentifier( column.getName(), column.isQuoted() ) );
			if ( columnInfo == null ) {
				changes.add( new SchemaChange.AddColumn( table.getName(), toColumnDef( column, metadata ) ) );
			}
			else if ( !hasMatchingType( column, columnInfo, metadata, dialect ) ) {
				changes.add( new SchemaChange.ModifyColumnType(
						table.getName(), column.getName(), column.getSqlType( metadata ) ) );
			}
		}
	}

	private void addIndexChanges(List<SchemaChange> changes, Table table, TableInformation tableInfo) {
		for ( var entry : table.getIndexes().entrySet() ) {
			final Index index = entry.getValue();
			if ( index.getName() == null || index.getName().isEmpty() ) {
				continue;
			}
			final var existingIndex = tableInfo.getIndex( Identifier.toIdentifier( index.getName() ) );
			if ( existingIndex == null ) {
				final List<String> columnNames = new ArrayList<>();
				for ( Selectable selectable : index.getSelectables() ) {
					if ( selectable instanceof Column col ) {
						columnNames.add( col.getName() );
					}
				}
				if ( !columnNames.isEmpty() ) {
					changes.add( new SchemaChange.CreateIndex(
							index.getName(), table.getName(), columnNames, false ) );
				}
			}
		}
	}

	private void addUniqueKeyChanges(List<SchemaChange> changes, Table table, TableInformation tableInfo) {
		for ( var entry : table.getUniqueKeys().entrySet() ) {
			final UniqueKey uk = entry.getValue();
			if ( uk.getName() == null || uk.getName().isEmpty() ) {
				continue;
			}
			final var existingIndex = tableInfo.getIndex( Identifier.toIdentifier( uk.getName() ) );
			if ( existingIndex == null ) {
				final List<String> columnNames = new ArrayList<>();
				for ( var col : uk.getColumns() ) {
					columnNames.add( col.getName() );
				}
				changes.add( new SchemaChange.AddUniqueConstraint(
						uk.getName(), table.getName(), columnNames ) );
			}
		}
	}

	private void addForeignKeyChanges(List<SchemaChange> changes, Table table, TableInformation tableInfo) {
		for ( ForeignKey fk : table.getForeignKeyCollection() ) {
			if ( !fk.isPhysicalConstraint() || !fk.isCreationEnabled() ) {
				continue;
			}
			if ( fk.getName() == null || fk.getName().isEmpty() ) {
				continue;
			}
			if ( tableInfo != null && foreignKeyExists( fk, tableInfo ) ) {
				continue;
			}
			final List<String> baseColumns = new ArrayList<>();
			for ( var col : fk.getColumns() ) {
				baseColumns.add( col.getName() );
			}
			final List<String> refColumns = new ArrayList<>();
			if ( fk.isReferenceToPrimaryKey() ) {
				final PrimaryKey refPk = fk.getReferencedTable().getPrimaryKey();
				if ( refPk != null ) {
					for ( var col : refPk.getColumns() ) {
						refColumns.add( col.getName() );
					}
				}
			}
			else {
				for ( var col : fk.getReferencedColumns() ) {
					refColumns.add( col.getName() );
				}
			}
			changes.add( new SchemaChange.AddForeignKey(
					fk.getName(),
					table.getName(),
					baseColumns,
					fk.getReferencedTable().getName(),
					refColumns ) );
		}
	}

	private boolean foreignKeyExists(ForeignKey fk, TableInformation tableInfo) {
		if ( tableInfo.getForeignKey( Identifier.toIdentifier( fk.getName() ) ) != null ) {
			return true;
		}
		final String referencingColumn = fk.getColumn( 0 ).getName();
		final String referencedTable = fk.getReferencedTable().getName();
		for ( var fkInfo : tableInfo.getForeignKeys() ) {
			for ( var mapping : fkInfo.getColumnReferenceMappings() ) {
				final String existingRefCol = mapping.getReferencingColumnMetadata()
						.getColumnIdentifier().getText();
				final String existingRefTable = mapping.getReferencedColumnMetadata()
						.getContainingTableInformation().getName().getTableName().getCanonicalName();
				if ( referencingColumn.equalsIgnoreCase( existingRefCol )
						&& referencedTable.equalsIgnoreCase( existingRefTable ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasMatchingType(Column column, ColumnInformation columnInfo, Metadata metadata, Dialect dialect) {
		final boolean typesMatch =
				dialect.equivalentTypes( column.getSqlTypeCode( metadata ), columnInfo.getTypeCode() )
				|| normalize( stripArgs( column.getSqlType( metadata ) ) )
						.equals( normalize( columnInfo.getTypeName() ) );
		if ( typesMatch ) {
			return true;
		}
		final JdbcType jdbcType = dialect.resolveSqlTypeDescriptor(
				columnInfo.getTypeName(),
				columnInfo.getTypeCode(),
				columnInfo.getColumnSize(),
				columnInfo.getDecimalDigits(),
				metadata.getDatabase().getTypeConfiguration().getJdbcTypeRegistry()
		);
		return dialect.equivalentTypes( column.getSqlTypeCode( metadata ), jdbcType.getDefaultSqlTypeCode() );
	}

	private SchemaChange.ColumnDef toColumnDef(Column column, Metadata metadata) {
		return new SchemaChange.ColumnDef(
				column.getName(),
				column.getSqlType( metadata ),
				column.isNullable(),
				column.getDefaultValue()
		);
	}

	private static String normalize(String typeName) {
		if ( typeName == null ) {
			return null;
		}
		final String lower = typeName.toLowerCase( Locale.ROOT );
		return switch ( lower ) {
			case "int" -> "integer";
			case "character" -> "char";
			case "character varying" -> "varchar";
			case "binary varying" -> "varbinary";
			case "character large object" -> "clob";
			case "binary large object" -> "blob";
			case "interval second" -> "interval";
			case "double precision" -> "double";
			default -> lower;
		};
	}

	private static String stripArgs(String typeExpression) {
		if ( typeExpression == null ) {
			return null;
		}
		final int i = typeExpression.indexOf( '(' );
		return i > 0 ? typeExpression.substring( 0, i ).trim() : typeExpression;
	}
}
