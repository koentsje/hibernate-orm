/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.tool.reveng.internal.export.common.GenericExporter;

public class HbmLintExporter extends GenericExporter {

	private static final String TEXT_REPORT_FTL = "lint/text-report.ftl";

	public void start() {
		getProperties().put(TEMPLATE_NAME, TEXT_REPORT_FTL);
		getProperties().put(FILE_PATTERN, "hbmlint-result.txt");
		super.start();
	}
	protected void setupContext() {
		HbmLint hbmlint = HbmLint.createInstance();
		hbmlint.analyze( getMetadata() );
		getProperties().put("lintissues", hbmlint.getResults());
		super.setupContext();
	}

	public String getName() {
		return "hbmlint";
	}


}
