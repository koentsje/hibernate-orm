/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.maven.cli.MavenCli;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;

public class EnhancerMojoTestIT {

	@TempDir
	private File projectDir;

	@Test
	public void testEnhanceDefault() throws Exception {
		copyPomXml();
		copyJavaFiles();
		MavenCli cli = new MavenCli();
		System.setProperty("maven.multiModuleProjectDirectory", projectDir.getAbsolutePath());
		File barClassFile = new File(projectDir, "target/classes/org/foo/Bar.class");
		assertFalse(barClassFile.exists());
//		cli.doMain(
//				new String[]{"compile"},
//				projectDir.getAbsolutePath(),
//				null,
//				null);
//		assertTrue(barClassFile.exists());
//		assertFalse(isEnhanced("org.foo.Bar"));
//		cli.doMain(
//				new String[]{"process-classes"},
//				projectDir.getAbsolutePath(),
//				null,
//				null);
//		assertTrue(isEnhanced("org.foo.Bar"));
	}

	private void copyPomXml() throws Exception {
		URL url = getClass().getClassLoader().getResource("pom.xm_");
		File source = new File(url.toURI());
		File destination = new File(projectDir, "pom.xml");
		Files.copy(source.toPath(), destination.toPath());
	}

	private void copyJavaFiles() throws Exception {
		URL url = getClass().getClassLoader().getResource("Bar.jav_");
		File source = new File(url.toURI());
		File destinationFolder = new File(projectDir, "src/main/java/org/foo");
		destinationFolder.mkdirs();
		File destination = new File(destinationFolder, "Bar.java");
		Files.copy(source.toPath(), destination.toPath());
	}

	private ClassLoader getTestClassLoader() throws Exception {
		return new URLClassLoader( new URL[] { new File(projectDir, "target/classes").toURI().toURL() } );
	}


	private boolean isEnhanced(String className) throws Exception {
		return getTestClassLoader().loadClass( className ).isAnnotationPresent( EnhancementInfo.class );
	}

}
