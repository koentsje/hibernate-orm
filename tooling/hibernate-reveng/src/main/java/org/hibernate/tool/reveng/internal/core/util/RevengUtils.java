/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.AssociationInfo;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;

import java.util.List;

public class RevengUtils {

	public static List<String> getPrimaryKeyInfoInRevengStrategy(
			RevengStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema) {
		List<String> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = TableIdentifier.create(catalog, schema, table.getName());
			result = revengStrat.getPrimaryKeyColumnNames(tableIdentifier);
		}
		return result;
	}

	public static String getTableIdentifierStrategyNameInRevengStrategy(
			RevengStrategy revengStrat,
			TableIdentifier tableIdentifier,
			String defaultCatalog,
			String defaultSchema) {
		String result = null;
		result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
		if (result == null) {
			String catalog = getCatalogForModel(tableIdentifier.getCatalog(), defaultCatalog);
			String schema = getSchemaForModel(tableIdentifier.getSchema(), defaultSchema);
			tableIdentifier = TableIdentifier.create(catalog, schema, tableIdentifier.getName());
			result = revengStrat.getTableIdentifierStrategyName(tableIdentifier);
		}
		return result;
	}

	public static TableIdentifier createTableIdentifier(
			Table table,
			String defaultCatalog,
			String defaultSchema) {
		String tableName = table.getName();
		String tableCatalog = getCatalogForModel(table.getCatalog(), defaultCatalog);
		String tableSchema = getSchemaForModel(table.getSchema(), defaultSchema);
		return TableIdentifier.create(tableCatalog, tableSchema, tableName);
	}

	public static AssociationInfo createAssociationInfo(
			String cascade,
			String fetch,
			Boolean insert,
			Boolean update) {
		return new AssociationInfo() {
			@Override
			public String getCascade() {
				return cascade;
			}
			@Override
			public String getFetch() {
				return fetch;
			}
			@Override
			public Boolean getUpdate() {
				return update;
			}
			@Override
			public Boolean getInsert() {
				return insert;
			}

		};
	}

	/** If catalog is equal to defaultCatalog then we return null so it will be null in the generated code. */
	private static String getCatalogForModel(String catalog, String defaultCatalog) {
		if(catalog==null) return null;
		if(catalog.equals(defaultCatalog)) return null;
		return catalog;
	}

	/** If catalog is equal to defaultSchema then we return null so it will be null in the generated code. */
	private static String getSchemaForModel(String schema, String defaultSchema) {
		if(schema==null) return null;
		if(schema.equals(defaultSchema)) return null;
		return schema;
	}

}
