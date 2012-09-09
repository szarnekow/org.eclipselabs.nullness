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
package org.eclipselabs.nullness;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

/**
 * A cache for the annotation state of fields.
 */
class FieldCache {

	private final NullAnnotationFinder annotationFinder;

	private final Map<FieldSelector, Boolean> nullCache;

	private final IJavaProject project;

	FieldCache(IJavaProject project, NullAnnotationFinder annotationFinder) {
		this.project = project;
		this.annotationFinder = annotationFinder;
		this.nullCache = new HashMap<FieldSelector, Boolean>();
	}

	public Boolean getEffectiveNonNullState(String owner, String name) {
		FieldSelector selector = new FieldSelector(owner, name);
		Boolean cachedResult = nullCache.get(selector);
		if (cachedResult != null) {
			return cachedResult;
		}
		Boolean result = computeNonNullState(selector);
		if (result == null) {
			result = Boolean.FALSE;
		}
		nullCache.put(selector, result);
		return result;
	}

	private Boolean computeNonNullState(FieldSelector selector) {
		try {
			IType type = project.findType(selector.owner);
			if (type != null) {
				ASTParser parser = ASTParser.newParser(AST.JLS4);
				parser.setProject(project);
				parser.setIgnoreMethodBodies(false);
				IBinding[] bindings = parser.createBindings(new IJavaElement[] { type }, null);
				if (bindings[0] != null) {
					ITypeBinding typeBinding = (ITypeBinding) bindings[0];
					return computeNonNullState(typeBinding, selector.name);
				}
			}
			return null;
		} catch (Exception e) {
			return Boolean.FALSE;
		}
	}

	private Boolean computeNonNullState(ITypeBinding typeBinding, String name) {
		for (IVariableBinding binding : typeBinding.getDeclaredFields()) {
			if (name.equals(binding.getName())) {
				Boolean defaultState = annotationFinder.getDefaultNonNullState(typeBinding);
				return annotationFinder.getEffectiveNonNullState(binding, defaultState);
			}
		}
		Boolean result = computeNonNullState(typeBinding.getSuperclass(), name);
		if (result != null)
			return result;
		return null;
	}

}
