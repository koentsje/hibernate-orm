/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlywayExporterTaskTest {

	@Test
	public void testGetName() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		assertEquals("flyway (Generates Flyway versioned migration)", task.getName());
	}

	@Test
	public void testDefaultValues() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		assertEquals("1", task.migrationVersion);
		assertEquals("hibernate_schema_update", task.migrationDescription);
		assertEquals(";", task.delimiter);
		assertTrue(task.format);
	}

	@Test
	public void testSetMigrationVersion() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		task.setMigrationVersion("5");
		assertEquals("5", task.migrationVersion);
	}

	@Test
	public void testSetMigrationDescription() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		task.setMigrationDescription("add person table");
		assertEquals("add person table", task.migrationDescription);
	}

	@Test
	public void testSetDelimiter() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		task.setDelimiter("GO");
		assertEquals("GO", task.delimiter);
	}

	@Test
	public void testSetFormat() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		task.setFormat(false);
		assertFalse(task.format);
	}

	@Test
	public void testSetHaltonerror() {
		HibernateToolTask parent = new HibernateToolTask();
		FlywayExporterTask task = new FlywayExporterTask(parent);
		task.setHaltonerror(true);
	}
}
