/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import java.util.List;

public sealed interface SchemaChange {

	record ColumnDef(String name, String type, boolean nullable, String defaultValue) {}

	record CreateTable(
			String tableName,
			List<ColumnDef> columns,
			List<String> primaryKeyColumns) implements SchemaChange {}

	record AddColumn(
			String tableName,
			ColumnDef column) implements SchemaChange {}

	record ModifyColumnType(
			String tableName,
			String columnName,
			String newType) implements SchemaChange {}

	record AddForeignKey(
			String constraintName,
			String baseTableName,
			List<String> baseColumnNames,
			String referencedTableName,
			List<String> referencedColumnNames) implements SchemaChange {}

	record CreateIndex(
			String indexName,
			String tableName,
			List<String> columnNames,
			boolean unique) implements SchemaChange {}

	record AddUniqueConstraint(
			String constraintName,
			String tableName,
			List<String> columnNames) implements SchemaChange {}
}
