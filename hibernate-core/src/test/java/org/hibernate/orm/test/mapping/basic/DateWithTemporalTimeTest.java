/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.SkipForDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class DateWithTemporalTimeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DateEvent.class
		};
	}

	@Test
	@SkipForDialect(value = SybaseDialect.class, comment = "The jTDS driver doesn't allow setting a timestamp through setTime")
	public void testLifecycle() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			DateEvent dateEvent = new DateEvent(new Date());
			entityManager.persist(dateEvent);
		});
	}

	@Entity(name = "DateEvent")
	public static class DateEvent {

		@Id
		@GeneratedValue
		private Long id;

		//tag::basic-datetime-temporal-time-example[]
		@Column(name = "`timestamp`")
		@Temporal(TemporalType.TIME)
		private Date timestamp;
		//end::basic-datetime-temporal-time-example[]

		public DateEvent() {
		}

		public DateEvent(Date timestamp) {
			this.timestamp = timestamp;
		}

		public Long getId() {
			return id;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}
}
