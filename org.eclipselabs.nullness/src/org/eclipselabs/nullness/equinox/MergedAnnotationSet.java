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

public class MergedAnnotationSet implements NullnessAnnotationSet {

	private final NullnessAnnotationSet[] mergeUs;

	public MergedAnnotationSet(NullnessAnnotationSet... set) {
		this.mergeUs = set.clone();
	}

	public MergedAnnotationSet(NullnessAnnotationSet first, NullnessAnnotationSet... set) {
		this.mergeUs = new NullnessAnnotationSet[set.length + 1];
		this.mergeUs[0] = first;
		System.arraycopy(set, 0, mergeUs, 1, set.length);
	}

	@Override
	public boolean isDefaultNonNullAnnotation(String typeName) {
		for (int i = 0; i < mergeUs.length; i++) {
			if (mergeUs[i].isDefaultNonNullAnnotation(typeName))
				return true;
		}
		return false;
	}

	@Override
	public boolean isDefaultNullableAnnotation(String typeName) {
		for (int i = 0; i < mergeUs.length; i++) {
			if (mergeUs[i].isDefaultNullableAnnotation(typeName))
				return true;
		}
		return false;
	}

	@Override
	public boolean isNonNullAnnotation(String typeName) {
		for (int i = 0; i < mergeUs.length; i++) {
			if (mergeUs[i].isNonNullAnnotation(typeName))
				return true;
		}
		return false;
	}

	@Override
	public boolean isNullableAnnotation(String typeName) {
		for (int i = 0; i < mergeUs.length; i++) {
			if (mergeUs[i].isNullableAnnotation(typeName))
				return true;
		}
		return false;
	}

}
