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
package org.eclipselabs.nullness.tests.jdtenabled;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipselabs.nullness.tests.jdtenabled.classes.ClassWithNonNullConstraints;
import org.eclipselabs.nullness.tests.jdtenabled.classes.TestHarness;
import org.junit.Test;

@SuppressWarnings("null")
public class AnonymousClassNonNullTest extends AbstractNonNullOnClassTest {

	@Override
	protected TestHarness createTestHarness(String s1, String s2) {
		return new ClassWithNonNullConstraints(s1, s2) {
		};
	}

	@Override
	protected TestHarness createTestHarness() {
		return new ClassWithNonNullConstraints() {
		};
	}

	@Override
	protected String getTestHarnessName() {
		return ClassWithNonNullConstraints.class.getSimpleName();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParameter_03() {
		TestHarness testMe = new ClassWithNonNullConstraints() {
			@Override
			public String methodWithArguments(@Nullable String first, @Nullable String second) {
				return methodWithArgumentsImpl(first, second);
			}

			private String methodWithArgumentsImpl(@Nullable String first, @NonNull String second) {
				return "ok";
			}
		};
		try {
			testMe.methodWithArguments(null, null);
		} catch (IllegalArgumentException e) {
			ExceptionMessageAsserter.assertMessage(e, "second", 1, "anonymous " + getTestHarnessName(), "methodWithArgumentsImpl");
			throw e;
		}
	}
}
