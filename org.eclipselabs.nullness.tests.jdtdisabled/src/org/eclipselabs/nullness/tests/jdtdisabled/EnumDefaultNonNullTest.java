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

import org.eclipselabs.nullness.tests.jdtdisabled.enums.EnumWithDefaultNonNullConstraint1;
import org.eclipselabs.nullness.tests.jdtdisabled.enums.EnumWithDefaultNonNullConstraint2;
import org.eclipselabs.nullness.tests.jdtdisabled.enums.EnumWithDefaultNonNullConstraint3;
import org.junit.Assert;
import org.junit.Test;

public class EnumDefaultNonNullTest {

	@Test(expected = IllegalArgumentException.class)
	public void testEnum1() throws Throwable {
		try {
			Assert.assertNotNull(EnumWithDefaultNonNullConstraint1.VALUE);
		} catch (ExceptionInInitializerError e) {
			ExceptionMessageAsserter.assertMessage((IllegalArgumentException) e.getCause(), "value", 0,
					"EnumWithDefaultNonNullConstraint1", "<init>");
			throw e.getCause();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEnum2() throws Throwable {
		try {
			Assert.assertNotNull(EnumWithDefaultNonNullConstraint2.VALUE);
		} catch (ExceptionInInitializerError e) {
			ExceptionMessageAsserter.assertMessage((IllegalArgumentException) e.getCause(), "second", 1,
					"EnumWithDefaultNonNullConstraint2", "<init>");
			throw e.getCause();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testMethodOnEnum() {
		try {
			EnumWithDefaultNonNullConstraint3.VALUE.getString();
		} catch (IllegalStateException e) {
			ExceptionMessageAsserter.assertMessage(e, "EnumWithDefaultNonNullConstraint3", "getString");
			throw e;
		}
	}

}
