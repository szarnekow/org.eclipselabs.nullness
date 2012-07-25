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
package org.eclipselabs.nullness.tests.jdtenabled.classes;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

public class ClassWithSpacesInLocation extends TestHarness {

	public ClassWithSpacesInLocation() {
	}

	public ClassWithSpacesInLocation(@NonNull String first, @Nullable String second) {
	}

	@NonNullByDefault
	@Override
	public String methodWithArguments(String first, @Nullable String second) {
		return "ok";
	}

	@NonNull
	@Override
	public String methodWithReturnValue() {
		return null;
	}

	@Override
	public String methodWithDeclaredNonNullReturnValue(String param) {
		return null;
	}

}
