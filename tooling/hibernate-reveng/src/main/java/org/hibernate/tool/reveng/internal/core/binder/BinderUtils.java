/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.FetchMode;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.reveng.api.core.AssociationInfo;
import org.hibernate.tool.reveng.api.core.RevengStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BinderUtils {

	public static Logger LOGGER = Logger.getLogger(BinderUtils.class.getName());

	public static String makeUnique(
			Iterator<Property> props,
			String originalPropertyName) {
		int cnt = 0;
		String propertyName = originalPropertyName;
		Set<String> uniqueNames = new HashSet<String>();
		while ( props.hasNext() ) {
			Property element = props.next();
			uniqueNames.add( element.getName() );
		}
		while( uniqueNames.contains(propertyName) ) {
			cnt++;
			propertyName = originalPropertyName + "_" + cnt;
		}
		return propertyName;
	}

	public static String makeUnique(PersistentClass clazz, String propertyName) {
		List<Property> list = new ArrayList<Property>();
		if( clazz.hasIdentifierProperty() ) {
			list.add( clazz.getIdentifierProperty() );
		}
		if( clazz.isVersioned() ) {
			list.add( clazz.getVersion() );
		}
	JoinedList<Property> joinedList =
				new JoinedList<Property>(
						list,
						clazz.getProperties());
		return BinderUtils.makeUnique(joinedList.iterator(), propertyName);
	}

	public static String makeUnique(Component clazz, String propertyName) {
		return BinderUtils.makeUnique(clazz.getProperties().iterator(), propertyName);
	}

	public static void checkColumnForMultipleBinding(Column column) {
		if(column.getValue()!=null) {
			LOGGER.log(Level.WARNING, "Binding column twice should not happen. " + column);
// TODO enable this next line and investigate why the tests fail
//			throw new RuntimeException("Binding column twice should not happen. " + column);
		}
	}

	static void updateFetchMode(Fetchable value, String fetchMode) {
		if(FetchMode.JOIN.toString().equalsIgnoreCase(fetchMode)) {
			value.setFetchMode(FetchMode.JOIN);
		}
		else {
			value.setFetchMode(FetchMode.SELECT);
		}
	}


	static AssociationInfo getAssociationInfo(
			RevengStrategy revengStrategy,
			ForeignKey foreignKey,
			boolean inverseProperty) {
		if (inverseProperty) {
			return revengStrategy.foreignKeyToInverseAssociationInfo(foreignKey);
		}
		else {
			return revengStrategy.foreignKeyToAssociationInfo(foreignKey);
		}
	}

}
