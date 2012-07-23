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

import org.eclipselabs.nullness.tests.jdtenabled.enums.EnumWithNonNullConstraint1;
import org.eclipselabs.nullness.tests.jdtenabled.enums.EnumWithNonNullConstraint2;
import org.eclipselabs.nullness.tests.jdtenabled.enums.EnumWithNonNullConstraint3;
import org.junit.Assert;
import org.junit.Test;

public class EnumNonNullTest {

	@Test(expected = ExceptionInInitializerError.class)
	public void testEnum1() {
		try {
			Assert.assertNotNull(EnumWithNonNullConstraint1.VALUE);
		} catch (ExceptionInInitializerError e) {
			ExceptionMessageAsserter.assertMessage((IllegalArgumentException) e.getCause(), "value", 0, "EnumWithNonNullConstraint1",
					"<init>");
			throw e;
		}
	}

	@Test(expected = ExceptionInInitializerError.class)
	public void testEnum2() {
		try {
			Assert.assertNotNull(EnumWithNonNullConstraint2.VALUE);
		} catch (ExceptionInInitializerError e) {
			ExceptionMessageAsserter.assertMessage((IllegalArgumentException) e.getCause(), "second", 1, "EnumWithNonNullConstraint2",
					"<init>");
			throw e;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testMethodOnEnum() {
		try {
			EnumWithNonNullConstraint3.VALUE.getValue();
		} catch (IllegalStateException e) {
			ExceptionMessageAsserter.assertMessage(e, "EnumWithNonNullConstraint3", "getValue");
			throw e;
		}
	}

}
