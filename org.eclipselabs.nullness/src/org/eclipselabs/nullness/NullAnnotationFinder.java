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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipselabs.nullness.equinox.NullnessAnnotationSet;
import org.eclipselabs.nullness.jdt.Bindings;

class NullAnnotationFinder {

	private final NullnessAnnotationSet annotationSet;

	NullAnnotationFinder(NullnessAnnotationSet annotationSet) {
		this.annotationSet = annotationSet;
	}

	private boolean isDefaultNonNullAnnotation(String typeName) {
		return annotationSet.isDefaultNonNullAnnotation(typeName);
	}

	private boolean isNullableAnnotation(String typeName) {
		return annotationSet.isNullableAnnotation(typeName);
	}

	private boolean isNonNullAnnotation(String typeName) {
		return annotationSet.isNonNullAnnotation(typeName);
	}

	private String getAnnotationTypeName(IAnnotationBinding annotation) {
		return annotation.getAnnotationType().getQualifiedName();
	}

	private String getAnnotationTypeName(AnnotationBinding annotation) {
		ReferenceBinding annotationType = annotation.getAnnotationType();
		String result = CharOperation.toString(annotationType.compoundName);
		return result;
	}

	Boolean getEffectiveNonNullState(IMethodBinding binding, IAnnotationBinding[] parameterAnnotations, Boolean defaultNonNullState) {
		Boolean result = getEffectiveNonNullState(parameterAnnotations);
		if (result != null)
			return result;
		Boolean methodDefault = getDefaultNonNullState(binding);
		if (methodDefault != null)
			return methodDefault;
		return defaultNonNullState;
	}

	private Boolean recursiveGetInheritedNonNullState(MethodBinding internalMethodBinding, ReferenceBinding declarator) {
		if (declarator == null)
			return null;
		MethodBinding[] methods = declarator.getMethods(internalMethodBinding.selector);
		for (MethodBinding method : methods) {
			if (internalMethodBinding.areParameterErasuresEqual(method)) {
				return getEffectiveNonNullState(method);
			}
		}
		Boolean result = recursiveGetInheritedNonNullState(internalMethodBinding, declarator.superclass());
		if (result != null)
			return result;
		for (ReferenceBinding intf : declarator.superInterfaces()) {
			Boolean candidate = recursiveGetInheritedNonNullState(internalMethodBinding, intf);
			if (Boolean.TRUE.equals(candidate))
				return candidate;
			if (candidate != null) {
				result = candidate;
			}
		}
		return result;
	}

	private Boolean recursiveGetDefaultNonNullState(IBinding binding) {
		Boolean result = getDefaultNonNullState(binding);
		if (result == null) {
			if (binding instanceof IMethodBinding) {
				result = recursiveGetDefaultNonNullState(((IMethodBinding) binding).getDeclaringClass());
			} else if (binding instanceof IVariableBinding) {
				result = recursiveGetDefaultNonNullState(((IVariableBinding) binding).getDeclaringClass());
			} else if (binding instanceof ITypeBinding) {
				ITypeBinding castedBinding = (ITypeBinding) binding;
				if (castedBinding.isMember()) {
					result = recursiveGetDefaultNonNullState(castedBinding.getDeclaringClass());
				} else if (castedBinding.isLocal()) {
					IMethodBinding declaringMethod = castedBinding.getDeclaringMethod();
					if (declaringMethod != null)
						result = recursiveGetDefaultNonNullState(declaringMethod);
					else
						result = recursiveGetDefaultNonNullState(castedBinding.getDeclaringClass());
				} else {
					result = recursiveGetDefaultNonNullState(castedBinding.getPackage());
				}
			}
		}
		return result;
	}

	private Boolean getEffectiveNonNullState(String typeName) {
		if (isNonNullAnnotation(typeName)) {
			return Boolean.TRUE;
		}
		if (isDefaultNonNullAnnotation(typeName)) {
			// TODO check the value of 'value'
			return Boolean.TRUE;
		}
		if (isNullableAnnotation(typeName)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private Boolean getEffectiveNonNullState(IAnnotationBinding[] annotations) {
		for (IAnnotationBinding annotation : annotations) {
			String typeName = getAnnotationTypeName(annotation);
			Boolean result = getEffectiveNonNullState(typeName);
			if (result != null)
				return result;
		}
		return null;
	}

	private Boolean getEffectiveNonNullState(AnnotationBinding[] annotations) {
		for (AnnotationBinding annotation : annotations) {
			String typeName = getAnnotationTypeName(annotation);
			Boolean result = getEffectiveNonNullState(typeName);
			if (result != null)
				return result;
		}
		return null;
	}

	private Boolean getEffectiveNonNullState(MethodBinding method) {
		AnnotationBinding[] annotations = method.getAnnotations();
		Boolean result = getEffectiveNonNullState(annotations);
		if (result != null)
			return result;
		return recursiveGetInheritedNonNullState(method);
	}

	private Boolean recursiveGetInheritedNonNullState(MethodBinding internalMethodBinding) {
		ReferenceBinding superclass = internalMethodBinding.declaringClass.superclass();
		Boolean result = recursiveGetInheritedNonNullState(internalMethodBinding, superclass);
		if (Boolean.TRUE.equals(result)) {
			return result;
		}
		for (ReferenceBinding intf : internalMethodBinding.declaringClass.superInterfaces()) {
			Boolean candidate = recursiveGetInheritedNonNullState(internalMethodBinding, intf);
			if (Boolean.TRUE.equals(candidate))
				return candidate;
			if (candidate != null) {
				result = candidate;
			}
		}
		return result;
	}

	Boolean getEffectiveNonNullState(IMethodBinding binding, Boolean defaultNonNullState) {
		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			String typeName = getAnnotationTypeName(annotation);
			Boolean result = getEffectiveNonNullState(typeName);
			if (result != null)
				return result;
		}
		if (Boolean.TRUE.equals(defaultNonNullState) || binding.isConstructor()) {
			return defaultNonNullState;
		}
		MethodBinding internalMethodBinding = Bindings.getInternalBinding(binding);
		Boolean result = recursiveGetInheritedNonNullState(internalMethodBinding);
		if (result != null) {
			return result;
		}
		// TODO apply default stuff recursively, too
		return defaultNonNullState;
	}

	private Boolean getDefaultNonNullState(IBinding binding) {
		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			if (isDefaultNonNullAnnotation(getAnnotationTypeName(annotation))) {
				// TODO check the value of 'value'
				return Boolean.TRUE;
			}
		}
		return null;
	}

	Boolean getDefaultNonNullState(ITypeBinding typeBinding) {
		Boolean result = recursiveGetDefaultNonNullState(typeBinding);
		return result;
	}

}