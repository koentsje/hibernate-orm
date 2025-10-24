/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.util;

import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;

public class ExceptionUtil {

	public static String getProblemSolutionOrCause(Throwable re) {
		if(re==null) return null;

		if(re instanceof MappingNotFoundException) {
			MappingNotFoundException mnf = (MappingNotFoundException)re;
			Origin origin = mnf.getOrigin();
			return "A " + origin.getType() + " located at " + origin.getName() + " was not found.\n" +
				"Check the following:\n" +
				"\n" +
				"1) Is the spelling/casing correct ?\n" +
				"2)	Is " + mnf.getOrigin().getName() + " available via the classpath ?\n" +
				"3) Does it actually exist ?\n";
		}

		if(re instanceof ClassNotFoundException || re instanceof NoClassDefFoundError) {

			return "A class were not found in the classpath of the Ant task.\n" +
					"Ensure that the classpath contains the classes needed for Hibernate and your code are in the classpath.\n";

		}

		if(re instanceof UnsupportedClassVersionError) {
			return "You are most likely running the ant task with a JRE that is older than the JRE required to use the classes.\n" +
					"e.g. running with JRE 1.3 or 1.4 when using JDK 1.5 annotations is not possible.\n" +
					"Ensure that you are using a correct JRE.";
		}



		if(re.getCause()!=re) {
			return getProblemSolutionOrCause( re.getCause() );
		}

		return null;
	}

}
