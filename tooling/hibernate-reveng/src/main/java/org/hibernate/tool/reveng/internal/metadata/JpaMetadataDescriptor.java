/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import java.util.Properties;

public class JpaMetadataDescriptor implements MetadataDescriptor {

	private Properties properties = new Properties();
	private Metadata metadata = null;

	public JpaMetadataDescriptor(
			final String persistenceUnit,
			final Properties properties) {
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder =
				createEntityManagerFactoryBuilder(persistenceUnit, properties);
		EntityManagerFactory entityManagerFactory =
				entityManagerFactoryBuilder.build();
		metadata = entityManagerFactoryBuilder.getMetadata();
		properties.putAll(entityManagerFactory.getProperties());
	}

	public Metadata createMetadata() {
		return metadata;
	}

	public Properties getProperties() {
		return properties;
	}

	private static class PersistenceProvider extends HibernatePersistenceProvider {
		public EntityManagerFactoryBuilderImpl getEntityManagerFactoryBuilder(
				String persistenceUnit,
				Properties properties) {
			EntityManagerFactoryBuilderImpl result = (EntityManagerFactoryBuilderImpl)getEntityManagerFactoryBuilderOrNull(
					persistenceUnit,
					properties);
			if (result == null) {
				throw new HibernateException(
						"Persistence unit not found: '" + persistenceUnit + "'."
					);
			}
			return result;
		}
	}

	private static EntityManagerFactoryBuilderImpl createEntityManagerFactoryBuilder(
			final String persistenceUnit,
			final Properties properties) {
		return new PersistenceProvider().getEntityManagerFactoryBuilder(
				persistenceUnit,
				properties);
	}

}
