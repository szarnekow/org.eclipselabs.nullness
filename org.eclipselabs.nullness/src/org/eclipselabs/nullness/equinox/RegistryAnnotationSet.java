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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

public class RegistryAnnotationSet implements NullnessAnnotationSet {

	private static final String EXTENSION_POINT_ID = "org.eclipselabs.nullness.annotations";

	public static NullnessAnnotationSet[] getAnnotationSets() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);
		NullnessAnnotationSet[] result = new NullnessAnnotationSet[elements.length];
		for (int i = 0; i < elements.length; i++) {
			result[i] = new RegistryAnnotationSet(elements[i]);
		}
		return result;
	}

	private final String[] defaultNonNullAnnotations;
	private final String[] defaultNullableAnnotations;
	private final String[] nonNullAnnotations;
	private final String[] nullableAnnotations;

	private RegistryAnnotationSet(IConfigurationElement element) {
		defaultNonNullAnnotations = getTypeNames(element, "defaultNonNull");
		defaultNullableAnnotations = getTypeNames(element, "defaultNullable");
		nonNullAnnotations = getTypeNames(element, "nonNull");
		nullableAnnotations = getTypeNames(element, "nullable");
	}

	private String[] getTypeNames(IConfigurationElement parent, String childName) {
		IConfigurationElement[] children = parent.getChildren(childName);
		String[] result = new String[children.length];
		for (int i = 0; i < children.length; i++) {
			result[i] = children[i].getAttribute("typeName");
		}
		return result;
	}

	private boolean containsTypeName(String[] array, String typeName) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(typeName))
				return true;
		}
		return false;
	}

	@Override
	public boolean isDefaultNonNullAnnotation(String typeName) {
		return containsTypeName(defaultNonNullAnnotations, typeName);
	}

	@Override
	public boolean isDefaultNullableAnnotation(String typeName) {
		return containsTypeName(defaultNullableAnnotations, typeName);
	}

	@Override
	public boolean isNonNullAnnotation(String typeName) {
		return containsTypeName(nonNullAnnotations, typeName);
	}

	@Override
	public boolean isNullableAnnotation(String typeName) {
		return containsTypeName(nullableAnnotations, typeName);
	}

}
