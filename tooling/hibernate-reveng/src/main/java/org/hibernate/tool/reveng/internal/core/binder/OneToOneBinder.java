/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.type.ForeignKeyDirection;

import java.util.Set;

class OneToOneBinder extends AbstractBinder {

	static OneToOneBinder create(BinderContext binderContext) {
		return new OneToOneBinder(binderContext);
	}

	private final EntityPropertyBinder entityPropertyBinder;

	private OneToOneBinder(BinderContext binderContext) {
		super(binderContext);
		this.entityPropertyBinder = EntityPropertyBinder.create(binderContext);
	}

	Property bind(
			PersistentClass rc,
			Table targetTable,
			ForeignKey fk,
			Set<Column> processedColumns,
			boolean constrained,
			boolean inverseProperty) {
		OneToOne value = new OneToOne(getMetadataBuildingContext(), targetTable, rc);
		value.setReferencedEntityName(
				getRevengStrategy().tableToClassName(TableIdentifier.create(targetTable)));
		addColumns(fk, value, processedColumns);
		value.setFetchMode(FetchMode.SELECT);
		value.setConstrained(constrained);
		value.setForeignKeyType(
				constrained ?
				ForeignKeyDirection.FROM_PARENT :
				ForeignKeyDirection.TO_PARENT );
		return entityPropertyBinder
				.bind(
						getPropertyName(fk, targetTable, inverseProperty),
						true,
						targetTable,
						fk,
						value,
						inverseProperty);
	}

	private void addColumns(ForeignKey foreignKey, OneToOne oneToOne, Set<Column> processedColumns) {
		for (Column fkcolumn : foreignKey.getColumns()) {
			BinderUtils.checkColumnForMultipleBinding(fkcolumn);
			oneToOne.addColumn(fkcolumn);
			processedColumns.add(fkcolumn);
		}

	}

	private String getPropertyName(ForeignKey foreignKey, Table table, boolean inverseProperty) {
		if (inverseProperty) {
			return getForeignKeyToInverseEntityName(foreignKey, table);
		}
		else  {
			return getForeignKeyToTentityName(foreignKey, table);
		}
	}

	private String getForeignKeyToTentityName(ForeignKey foreignKey, Table table) {
		return getRevengStrategy().foreignKeyToEntityName(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getReferencedTable()),
				foreignKey.getReferencedColumns(),
				TableIdentifier.create(table),
				foreignKey.getColumns(),
				ForeignKeyUtils.isUniqueReference(foreignKey));
	}

	private String getForeignKeyToInverseEntityName(ForeignKey foreignKey, Table table) {
		return getRevengStrategy().foreignKeyToInverseEntityName(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getReferencedTable()),
				foreignKey.getReferencedColumns(),
				TableIdentifier.create(table),
				foreignKey.getColumns(),
				ForeignKeyUtils.isUniqueReference(foreignKey));
	}
}
