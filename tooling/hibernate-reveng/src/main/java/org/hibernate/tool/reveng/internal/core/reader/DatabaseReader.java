/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.reader;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class DatabaseReader {

	public static DatabaseReader create(
			Properties properties,
			RevengStrategy revengStrategy,
			RevengDialect mdd,
			ServiceRegistry serviceRegistry) {
		ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
		return new DatabaseReader(properties, mdd, connectionProvider, revengStrategy);
	}

	private final RevengStrategy revengStrategy;

	private RevengDialect metadataDialect;

	private final ConnectionProvider provider;

	private final Properties properties;

	private DatabaseReader(
			Properties properties,
			RevengDialect dialect,
			ConnectionProvider provider,
			RevengStrategy reveng) {
		this.metadataDialect = dialect;
		this.provider = provider;
		this.revengStrategy = reveng;
		this.properties = properties;
		if (revengStrategy == null) {
			throw new IllegalStateException("Strategy cannot be null");
		}
	}

	public void readDatabaseSchema(RevengMetadataCollector revengMetadataCollector) {
		try {
			metadataDialect.configure(provider);
			TableCollector tableCollector = TableCollector.create(
					metadataDialect,
					revengStrategy,
					revengMetadataCollector,
					properties);
			for (Iterator<SchemaSelection> iter = getSchemaSelections().iterator(); iter.hasNext();) {
				tableCollector.processTables(iter.next());
			}
			revengMetadataCollector.setOneToManyCandidates(resolveForeignKeys(revengMetadataCollector));

		}
		finally {
			metadataDialect.close();
			revengStrategy.close();
		}
	}

	/**
	 * Iterates the tables and find all the foreignkeys that refers to something
	 * that is available inside the DatabaseCollector.
	 *
	 * @param revengMetadataCollector
	 * @return
	 */
	private Map<String, List<ForeignKey>> resolveForeignKeys(RevengMetadataCollector revengMetadataCollector) {
		List<ForeignKeysInfo> fks = new ArrayList<ForeignKeysInfo>();
		ForeignKeyProcessor foreignKeyProcessor = ForeignKeyProcessor.create(
				metadataDialect,
				revengStrategy,
				getDefaultCatalog(),
				getDefaultSchema(),
				revengMetadataCollector);
		for (Table table : revengMetadataCollector.getTables()) {
			// Done here after the basic process of collections as we might not have touched
			// all referenced tables (this ensure the columns are the same instances
			// througout the basic JDBC derived model.
			// after this stage it should be "ok" to divert from keeping columns in sync as
			// it can be required if the same
			// column is used with different aliases in the ORM mapping.
			ForeignKeysInfo foreignKeys = foreignKeyProcessor.processForeignKeys(table);
			fks.add(foreignKeys);
		}

		Map<String, List<ForeignKey>> oneToManyCandidates = new HashMap<String, List<ForeignKey>>();
		for (Iterator<ForeignKeysInfo> iter = fks.iterator(); iter.hasNext();) {
			ForeignKeysInfo element = iter.next();
			Map<String, List<ForeignKey>> map = element.process(revengStrategy); // the actual foreignkey is created
																					// here.
			mergeMultiMap(oneToManyCandidates, map);
		}
		return oneToManyCandidates;
	}

	private void mergeMultiMap(Map<String, List<ForeignKey>> dest, Map<String, List<ForeignKey>> src) {
		Iterator<Entry<String, List<ForeignKey>>> items = src.entrySet().iterator();

		while (items.hasNext()) {
			Entry<String, List<ForeignKey>> element = items.next();

			List<ForeignKey> existing = dest.get(element.getKey());
			if (existing == null) {
				dest.put(element.getKey(), element.getValue());
			}
			else {
				existing.addAll(element.getValue());
			}
		}

	}

	private List<SchemaSelection> getSchemaSelections() {
		List<SchemaSelection> result = revengStrategy.getSchemaSelections();
		if (result == null) {
			result = new ArrayList<SchemaSelection>();
			result.add(new SchemaSelection() {
				@Override
				public String getMatchCatalog() {
					return getDefaultCatalog();
				}
				@Override
				public String getMatchSchema() {
					return getDefaultSchema();
				}
				@Override
				public String getMatchTable() {
					return null;
				}
			});
		}
		return result;
	}

	private String getDefaultSchema() {
		return properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}

	private String getDefaultCatalog() {
		return properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
	}

}
