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

import junit.framework.Assert;

import org.eclipselabs.nullness.tests.jdtdisabled.classes.TestHarness;
import org.junit.Test;

public abstract class AbstractNonNullOnClassTest {

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_01() {
		try {
			createTestHarness(null, null);
		} catch (IllegalArgumentException e) {
			ExceptionMessageAsserter.assertMessage(e, "first", 0, getTestHarnessName(), "<init>");
			throw e;
		}
	}

	protected String getTestHarnessName() {
		return createTestHarness().getClass().getSimpleName();
	}

	protected abstract TestHarness createTestHarness(String s1, String s2);

	protected abstract TestHarness createTestHarness();

	@Test
	public void testConstructor_02() {
		Assert.assertNotNull(createTestHarness("", null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParameter_01() {
		TestHarness testMe = createTestHarness();
		try {
			testMe.methodWithArguments(null, null);
		} catch (IllegalArgumentException e) {
			ExceptionMessageAsserter.assertMessage(e, "first", 0, getTestHarnessName(), "methodWithArguments");
			throw e;
		}
	}

	@Test
	public void testParameter_02() {
		TestHarness testMe = createTestHarness();
		Assert.assertEquals("ok", testMe.methodWithArguments("", null));
	}

	protected String getTestHarnessNameForReturnValue() {
		return getTestHarnessName();
	}

	@Test(expected = IllegalStateException.class)
	public void testReturnValue_01() {
		TestHarness testMe = createTestHarness();
		try {
			testMe.methodWithReturnValue();
		} catch (IllegalStateException e) {
			ExceptionMessageAsserter.assertMessage(e, getTestHarnessNameForReturnValue(), "methodWithReturnValue");
			throw e;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testReturnValue_02() {
		TestHarness testMe = createTestHarness();
		try {
			testMe.methodWithDeclaredNonNullReturnValue("");
		} catch (IllegalStateException e) {
			ExceptionMessageAsserter.assertMessage(e, getTestHarnessNameForReturnValue(), "methodWithDeclaredNonNullReturnValue");
			throw e;
		}
	}

}
