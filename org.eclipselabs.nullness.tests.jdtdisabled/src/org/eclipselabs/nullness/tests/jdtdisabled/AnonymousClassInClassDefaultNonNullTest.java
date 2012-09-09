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
import org.eclipselabs.nullness.tests.jdtdisabled.classes.ClassWithDefaultNonNullConstraints;
import org.eclipselabs.nullness.tests.jdtdisabled.classes.TestHarness;
import org.junit.Test;

public class AnonymousClassInClassDefaultNonNullTest extends AbstractNonNullOnClassTest {

	static class TestHarnessDelegate {
		String s1;
		String s2;

		TestHarnessDelegate(String s1, String s2) {
			this.s1 = s1;
			this.s2 = s2;
		}
	}

	static class TestHarnessDelegateSub extends TestHarnessDelegate {
		TestHarness testHarness = new ClassWithDefaultNonNullConstraints(s1, s2) {
			// anonymous class defined in method
		};

		TestHarnessDelegateSub(String s1, String s2) {
			super(s1, s2);
		}
	}

	@Override
	protected TestHarness createTestHarness(String s1, String s2) {
		return new TestHarnessDelegateSub(s1, s2).testHarness;
	}

	private final TestHarness testHarness = new ClassWithDefaultNonNullConstraints() {
		// anonymous class defined in method
	};

	@Override
	protected TestHarness createTestHarness() {
		return testHarness;
	}

	@Override
	protected String getTestHarnessName() {
		return ClassWithDefaultNonNullConstraints.class.getSimpleName();
	}

	private final TestHarness testHarnessForParameter_03 = new ClassWithDefaultNonNullConstraints() {
		@Override
		public String methodWithArguments(@Nullable String first, @Nullable String second) {
			return methodWithArgumentsImpl(first, second);
		}

		private String methodWithArgumentsImpl(@Nullable String first, @NonNull String second) {
			return "ok";
		}
	};

	@Test(expected = IllegalArgumentException.class)
	@NonNullByDefault
	public void testParameter_03() {
		try {
			testHarnessForParameter_03.methodWithArguments(null, null);
		} catch (IllegalArgumentException e) {
			ExceptionMessageAsserter.assertMessage(e, "second", 1, "anonymous " + getTestHarnessName(), "methodWithArgumentsImpl");
			throw e;
		}
	}
}
