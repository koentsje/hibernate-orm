/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PERSON")
public class Person {

	@Id
	@Column(name = "ID")
	private int id;

	@Column(name = "NAME", length = 20)
	private String name;

	@Column(name = "EMAIL", length = 100)
	private String email;

}
