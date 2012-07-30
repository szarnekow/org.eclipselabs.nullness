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
package org.eclipselabs.nullness.jdt;

import java.lang.reflect.Field;

import org.eclipse.jdt.core.dom.IBinding;

public final class Bindings {

	private Bindings() {
		throw new UnsupportedOperationException();
	}

	public static <T> T getInternalBinding(IBinding binding) {
		try {
			Field field = binding.getClass().getDeclaredField("binding"); //$NON-NLS-1$
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			T result = (T) field.get(binding);
			return result;
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

}
