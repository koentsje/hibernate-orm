/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaChangeTest {

	@Test
	public void testColumnDef() {
		var col = new SchemaChange.ColumnDef("NAME", "varchar(50)", true, null);
		assertEquals("NAME", col.name());
		assertEquals("varchar(50)", col.type());
		assertTrue(col.nullable());
		assertNull(col.defaultValue());
	}

	@Test
	public void testColumnDefWithDefault() {
		var col = new SchemaChange.ColumnDef("STATUS", "varchar(10)", false, "'ACTIVE'");
		assertEquals("STATUS", col.name());
		assertEquals("varchar(10)", col.type());
		assertFalse(col.nullable());
		assertEquals("'ACTIVE'", col.defaultValue());
	}

	@Test
	public void testCreateTable() {
		var columns = List.of(
				new SchemaChange.ColumnDef("ID", "int", false, null),
				new SchemaChange.ColumnDef("NAME", "varchar(50)", true, null)
		);
		SchemaChange change = new SchemaChange.CreateTable("PERSON", columns, List.of("ID"));
		assertInstanceOf(SchemaChange.CreateTable.class, change);
		var ct = (SchemaChange.CreateTable) change;
		assertEquals("PERSON", ct.tableName());
		assertEquals(2, ct.columns().size());
		assertEquals(List.of("ID"), ct.primaryKeyColumns());
	}

	@Test
	public void testAddColumn() {
		var col = new SchemaChange.ColumnDef("EMAIL", "varchar(100)", true, null);
		SchemaChange change = new SchemaChange.AddColumn("PERSON", col);
		assertInstanceOf(SchemaChange.AddColumn.class, change);
		var ac = (SchemaChange.AddColumn) change;
		assertEquals("PERSON", ac.tableName());
		assertEquals("EMAIL", ac.column().name());
	}

	@Test
	public void testModifyColumnType() {
		SchemaChange change = new SchemaChange.ModifyColumnType("PERSON", "NAME", "varchar(100)");
		assertInstanceOf(SchemaChange.ModifyColumnType.class, change);
		var mct = (SchemaChange.ModifyColumnType) change;
		assertEquals("PERSON", mct.tableName());
		assertEquals("NAME", mct.columnName());
		assertEquals("varchar(100)", mct.newType());
	}

	@Test
	public void testAddForeignKey() {
		SchemaChange change = new SchemaChange.AddForeignKey(
				"FK_PERSON_ADDRESS",
				"PERSON", List.of("ADDRESS_ID"),
				"ADDRESS", List.of("ID"));
		assertInstanceOf(SchemaChange.AddForeignKey.class, change);
		var afk = (SchemaChange.AddForeignKey) change;
		assertEquals("FK_PERSON_ADDRESS", afk.constraintName());
		assertEquals("PERSON", afk.baseTableName());
		assertEquals(List.of("ADDRESS_ID"), afk.baseColumnNames());
		assertEquals("ADDRESS", afk.referencedTableName());
		assertEquals(List.of("ID"), afk.referencedColumnNames());
	}

	@Test
	public void testCreateIndex() {
		SchemaChange change = new SchemaChange.CreateIndex(
				"IDX_PERSON_NAME", "PERSON", List.of("NAME"), false);
		assertInstanceOf(SchemaChange.CreateIndex.class, change);
		var ci = (SchemaChange.CreateIndex) change;
		assertEquals("IDX_PERSON_NAME", ci.indexName());
		assertEquals("PERSON", ci.tableName());
		assertEquals(List.of("NAME"), ci.columnNames());
		assertFalse(ci.unique());
	}

	@Test
	public void testAddUniqueConstraint() {
		SchemaChange change = new SchemaChange.AddUniqueConstraint(
				"UK_PERSON_EMAIL", "PERSON", List.of("EMAIL"));
		assertInstanceOf(SchemaChange.AddUniqueConstraint.class, change);
		var auc = (SchemaChange.AddUniqueConstraint) change;
		assertEquals("UK_PERSON_EMAIL", auc.constraintName());
		assertEquals("PERSON", auc.tableName());
		assertEquals(List.of("EMAIL"), auc.columnNames());
	}

	@Test
	public void testAllChangeTypesAreSchemaChanges() {
		List<SchemaChange> changes = List.of(
				new SchemaChange.CreateTable("T", List.of(), List.of()),
				new SchemaChange.AddColumn("T", new SchemaChange.ColumnDef("C", "int", true, null)),
				new SchemaChange.ModifyColumnType("T", "C", "bigint"),
				new SchemaChange.AddForeignKey("FK", "T", List.of("C"), "T2", List.of("ID")),
				new SchemaChange.CreateIndex("IDX", "T", List.of("C"), false),
				new SchemaChange.AddUniqueConstraint("UK", "T", List.of("C"))
		);
		assertEquals(6, changes.size());
		assertInstanceOf(SchemaChange.CreateTable.class, changes.get(0));
		assertInstanceOf(SchemaChange.AddColumn.class, changes.get(1));
		assertInstanceOf(SchemaChange.ModifyColumnType.class, changes.get(2));
		assertInstanceOf(SchemaChange.AddForeignKey.class, changes.get(3));
		assertInstanceOf(SchemaChange.CreateIndex.class, changes.get(4));
		assertInstanceOf(SchemaChange.AddUniqueConstraint.class, changes.get(5));
	}
}
