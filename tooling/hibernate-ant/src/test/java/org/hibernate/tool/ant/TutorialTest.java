/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;


import org.junit.jupiter.api.Test;

public class TutorialTest extends TestTemplate {

	@Test
	public void testTutorial() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated-sources'>                         \s
							<jdbcconfiguration propertyfile='hibernate.properties'/>\s
							<hbm2java/>                                             \s
						</hibernatetool>                                            \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
		createProjectAndBuild();
		assertFolderExists("generated-sources", 1);
		assertFileExists("generated-sources/Person.java");
	}


}
