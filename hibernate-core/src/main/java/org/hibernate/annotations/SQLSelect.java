/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.SqlResultSetMapping;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a custom SQL query to be used in place of the default SQL
 * generated by Hibernate when an entity or collection is loaded from the
 * database by id. This occurs when:
 * <ul>
 * <li>an association to an entity is fetched lazily,
 * <li>a collection is fetched lazily, or
 * <li>when an entity is retrieved using {@link org.hibernate.Session#get}
 *     or {@link org.hibernate.Session#find}.
 * </ul>
 * <p>
 * The given {@linkplain #sql SQL statement} must have exactly the number
 * of JDBC {@code ?} parameters that Hibernate expects, that is, one for
 * each column of:
 * <ol>
 * <li>the {@linkplain jakarta.persistence.Id primary key}, in the case of
 *     an entity, or
 * <li>the foreign key, in the case of a collection.
 * </ol>
 * <p>
 * If the {@linkplain #querySpaces tables which affect the query results}
 * are specified, then changes to those tables will be flushed before
 * execution of the query.
 * <p>
 * For example:
 * <pre>
 * &#64;SQLSelect(sql = """
 *                  SELECT id, created, text
 *                  FROM records
 *                  WHERE id = ? and deleted is null
 *                  """
 *            querySpaces = "records")
 * &#64;Entity
 * public class Record { ... }
 * </pre>
 * <p>
 * Optionally, an explicit {@linkplain #resultSetMapping result set mapping}
 * may be specified. It should have:
 * <ol>
 * <li>a single {@link jakarta.persistence.EntityResult}, if the SQL query
 *     loads an {@linkplain jakarta.persistence.Entity entity},
 *     {@linkplain jakarta.persistence.OneToMany one-to-many} association,
 *     or {@linkplain jakarta.persistence.ManyToMany many-to-many} association,
 *     or
 * <li>a single {@link jakarta.persistence.ColumnResult} or
 *     {@link jakarta.persistence.ConstructorResult}, if the SQL query
 *     loads an {@linkplain jakarta.persistence.ElementCollection collection}
 *     of basic-typed values.
 * </ol>
 *
 * @see HQLSelect
 * @see DialectOverride.SQLSelect
 *
 * @author Gavin King
 *
 * @since 6.2
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
public @interface SQLSelect {
	/**
	 * The SQL {@code SELECT} statement.
	 */
	String sql();

	/**
	 * A {@link SqlResultSetMapping} with a single
	 * {@link jakarta.persistence.ColumnResult} or
	 * {@link jakarta.persistence.EntityResult}.
	 */
	SqlResultSetMapping resultSetMapping() default @SqlResultSetMapping(name="");

	/**
	 * The query spaces involved in this query.
	 *
	 * @see org.hibernate.query.SynchronizeableQuery
	 */
	String[] querySpaces() default {};

}
