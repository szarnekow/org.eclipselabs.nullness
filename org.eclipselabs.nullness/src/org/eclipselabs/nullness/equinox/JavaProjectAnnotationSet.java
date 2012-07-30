/*******************************************************************************
 * Copyright (c) 2012 Sebastian Zarnekow (http://zarnekow.blogspot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors:
 *   Sebastian Zarnekow - Initial implementation
 *******************************************************************************/
package org.eclipselabs.nullness.equinox;

import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class JavaProjectAnnotationSet implements NullnessAnnotationSet {

	private final String defaultNonNullAnnotation;
	private final String nonNullAnnotation;
	private final String nullableAnnotation;

	public JavaProjectAnnotationSet(IJavaProject project) {
		Map<?, ?> options = project.getOptions(true);
		defaultNonNullAnnotation = getOption(options, JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME);
		nonNullAnnotation = getOption(options, JavaCore.COMPILER_NONNULL_ANNOTATION_NAME);
		nullableAnnotation = getOption(options, JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME);
	}

	private String getOption(Map<?, ?> options, String key) {
		Object result = options.get(key);
		return result == null ? null : String.valueOf(result);
	}

	@Override
	public boolean isDefaultNonNullAnnotation(String typeName) {
		return isAnnotation(defaultNonNullAnnotation, typeName);
	}

	private boolean isAnnotation(String expected, String typeName) {
		return expected != null && expected.equals(typeName);
	}

	@Override
	public boolean isNullableAnnotation(String typeName) {
		return isAnnotation(nullableAnnotation, typeName);
	}

	@Override
	public boolean isNonNullAnnotation(String typeName) {
		return isAnnotation(nonNullAnnotation, typeName);
	}

	@Override
	public boolean isDefaultNullableAnnotation(String typeName) {
		return false;
	}

}
