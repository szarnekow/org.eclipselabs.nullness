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
package org.eclipselabs.nullness.tests.jdtdisabled;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipselabs.nullness.tests.jdtdisabled.classes.TestHarness;

@SuppressWarnings("null")
class WithNonNullConstraints extends TestHarness {

	public WithNonNullConstraints() {
	}

	public WithNonNullConstraints(@NonNull String first, @Nullable String second) {
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

@SuppressWarnings("null")
@NonNullByDefault
class WithDefaultNonNullConstraints extends TestHarness {

	public WithDefaultNonNullConstraints() {
	}

	public WithDefaultNonNullConstraints(String first, @Nullable String second) {
	}

	@Override
	public String methodWithArguments(String first, @Nullable String second) {
		return "ok";
	}

	@Override
	public String methodWithReturnValue() {
		return null;
	}

	@Override
	public String methodWithDeclaredNonNullReturnValue(String param) {
		return null;
	}

}