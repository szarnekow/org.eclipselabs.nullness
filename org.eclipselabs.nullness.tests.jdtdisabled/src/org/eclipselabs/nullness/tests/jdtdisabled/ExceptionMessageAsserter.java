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

public class ExceptionMessageAsserter {

	public static void assertMessage(IllegalArgumentException iae, String argumentName, int idx, String type, String method) {
		Assert.assertEquals(
				String.format("Argument for non-null parameter %s at index %d of %s#%s must not be null", argumentName, idx, type, method),
				iae.getMessage());
	}

	public static void assertMessage(IllegalStateException ise, String type, String method) {
		Assert.assertEquals(String.format("Non-null method %s#%s must not return null", type, method), ise.getMessage());
	}

}
