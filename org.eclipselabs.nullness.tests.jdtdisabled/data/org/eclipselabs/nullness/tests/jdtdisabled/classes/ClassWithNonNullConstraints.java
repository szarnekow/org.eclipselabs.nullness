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
package org.eclipselabs.nullness.tests.jdtdisabled.classes;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class ClassWithNonNullConstraints extends TestHarness {

	public ClassWithNonNullConstraints() {
	}

	public ClassWithNonNullConstraints(@NonNull String first, @Nullable String second) {
	}

	@Override
	public String methodWithArguments(@NonNull String first, @Nullable String second) {
		return "ok";
	}

	@Override
	@NonNull
	public String methodWithReturnValue() {
		return null;
	}

	@Override
	public String methodWithDeclaredNonNullReturnValue(String param) {
		return null;
	}

}
