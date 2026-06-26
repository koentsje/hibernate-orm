/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.liquibase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlChangelogWriterTest {

	@TempDir
	private File tempDir;

	@Test
	public void testCreateTableOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.CreateTable("PERSON", List.of(
						new SchemaChange.ColumnDef("ID", "int", false, null),
						new SchemaChange.ColumnDef("NAME", "varchar(50)", true, null)
				), List.of("ID"))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("databaseChangeLog"), "Should contain root element");
		assertTrue(xml.contains("liquibase.org"), "Should contain Liquibase namespace");
		assertTrue(xml.contains("<createTable tableName=\"PERSON\""), "Should contain createTable");
		assertTrue(xml.contains("name=\"ID\""), "Should contain ID column");
		assertTrue(xml.contains("type=\"int\""), "Should contain int type");
		assertTrue(xml.contains("primaryKey=\"true\""), "PK column should have primaryKey constraint");
		assertTrue(xml.contains("nullable=\"false\""), "PK column should not be nullable");
		assertTrue(xml.contains("name=\"NAME\""), "Should contain NAME column");
		assertTrue(xml.contains("type=\"varchar(50)\""), "Should contain varchar type");
	}

	@Test
	public void testAddColumnOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddColumn("PERSON",
						new SchemaChange.ColumnDef("EMAIL", "varchar(100)", true, null))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("<addColumn tableName=\"PERSON\""), "Should contain addColumn");
		assertTrue(xml.contains("name=\"EMAIL\""), "Should contain EMAIL column");
		assertTrue(xml.contains("type=\"varchar(100)\""), "Should contain varchar(100) type");
	}

	@Test
	public void testModifyColumnTypeOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.ModifyColumnType("PERSON", "NAME", "varchar(100)")
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("modifyDataType"), "Should contain modifyDataType");
		assertTrue(xml.contains("tableName=\"PERSON\""), "Should reference PERSON table");
		assertTrue(xml.contains("columnName=\"NAME\""), "Should reference NAME column");
		assertTrue(xml.contains("newDataType=\"varchar(100)\""), "Should contain new type");
	}

	@Test
	public void testAddForeignKeyOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddForeignKey("FK_PERSON_ADDR",
						"PERSON", List.of("ADDRESS_ID"),
						"ADDRESS", List.of("ID"))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("addForeignKeyConstraint"), "Should contain addForeignKeyConstraint");
		assertTrue(xml.contains("constraintName=\"FK_PERSON_ADDR\""), "Should contain constraint name");
		assertTrue(xml.contains("baseTableName=\"PERSON\""), "Should contain base table");
		assertTrue(xml.contains("baseColumnNames=\"ADDRESS_ID\""), "Should contain base columns");
		assertTrue(xml.contains("referencedTableName=\"ADDRESS\""), "Should contain ref table");
		assertTrue(xml.contains("referencedColumnNames=\"ID\""), "Should contain ref columns");
	}

	@Test
	public void testCreateIndexOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.CreateIndex("IDX_PERSON_NAME", "PERSON", List.of("NAME"), false)
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("<createIndex"), "Should contain createIndex");
		assertTrue(xml.contains("indexName=\"IDX_PERSON_NAME\""), "Should contain index name");
		assertTrue(xml.contains("tableName=\"PERSON\""), "Should contain table name");
		assertTrue(xml.contains("name=\"NAME\""), "Should contain column name");
		assertTrue(!xml.contains("unique=\"true\""), "Non-unique index should not have unique attribute");
	}

	@Test
	public void testCreateUniqueIndexOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.CreateIndex("IDX_PERSON_EMAIL", "PERSON", List.of("EMAIL"), true)
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("unique=\"true\""), "Unique index should have unique attribute");
	}

	@Test
	public void testAddUniqueConstraintOutput() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddUniqueConstraint("UK_PERSON_EMAIL", "PERSON", List.of("EMAIL"))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("addUniqueConstraint"), "Should contain addUniqueConstraint");
		assertTrue(xml.contains("constraintName=\"UK_PERSON_EMAIL\""), "Should contain constraint name");
		assertTrue(xml.contains("tableName=\"PERSON\""), "Should contain table name");
		assertTrue(xml.contains("columnNames=\"EMAIL\""), "Should contain column names");
	}

	@Test
	public void testChangeSetIds() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddColumn("T1",
						new SchemaChange.ColumnDef("C1", "int", true, null)),
				new SchemaChange.AddColumn("T2",
						new SchemaChange.ColumnDef("C2", "int", true, null)),
				new SchemaChange.AddColumn("T3",
						new SchemaChange.ColumnDef("C3", "int", true, null))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("id=\"1\""), "First changeset should have id 1");
		assertTrue(xml.contains("id=\"2\""), "Second changeset should have id 2");
		assertTrue(xml.contains("id=\"3\""), "Third changeset should have id 3");
	}

	@Test
	public void testCustomAuthor() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddColumn("T",
						new SchemaChange.ColumnDef("C", "int", true, null))
		);
		File outputFile = new File(tempDir, "changelog.xml");
		new XmlChangelogWriter().write(changes, outputFile, "koen");
		String xml = Files.readString(outputFile.toPath());
		assertTrue(xml.contains("author=\"koen\""), "Should use custom author");
	}

	@Test
	public void testColumnWithDefaultValue() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddColumn("PERSON",
						new SchemaChange.ColumnDef("STATUS", "varchar(10)", false, "'ACTIVE'"))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("defaultValue=\"'ACTIVE'\""), "Should contain default value");
		assertTrue(xml.contains("nullable=\"false\""), "Should have nullable constraint");
	}

	@Test
	public void testCompositeForeignKey() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.AddForeignKey("FK_COMPOSITE",
						"ORDER_LINE", List.of("ORDER_ID", "PRODUCT_ID"),
						"ORDER_PRODUCT", List.of("ORDER_ID", "PRODUCT_ID"))
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("baseColumnNames=\"ORDER_ID,PRODUCT_ID\""),
				"Should join composite base columns with comma");
		assertTrue(xml.contains("referencedColumnNames=\"ORDER_ID,PRODUCT_ID\""),
				"Should join composite ref columns with comma");
	}

	@Test
	public void testMultiColumnIndex() throws Exception {
		List<SchemaChange> changes = List.of(
				new SchemaChange.CreateIndex("IDX_COMPOSITE", "PERSON",
						List.of("LAST_NAME", "FIRST_NAME"), false)
		);
		String xml = writeAndRead(changes);
		assertTrue(xml.contains("name=\"LAST_NAME\""), "Should contain first index column");
		assertTrue(xml.contains("name=\"FIRST_NAME\""), "Should contain second index column");
	}

	@Test
	public void testEmptyChangeList() throws Exception {
		String xml = writeAndRead(List.of());
		assertTrue(xml.contains("databaseChangeLog"), "Should still produce valid root element");
		assertTrue(!xml.contains("changeSet"), "Should not contain any changesets");
	}

	private String writeAndRead(List<SchemaChange> changes) throws Exception {
		File outputFile = new File(tempDir, "changelog.xml");
		new XmlChangelogWriter().write(changes, outputFile, "hibernate");
		return Files.readString(outputFile.toPath());
	}
}
