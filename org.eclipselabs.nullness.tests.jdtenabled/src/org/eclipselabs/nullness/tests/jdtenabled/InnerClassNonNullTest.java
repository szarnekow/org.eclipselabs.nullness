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

import org.eclipselabs.nullness.tests.jdtenabled.classes.Outer;
import org.eclipselabs.nullness.tests.jdtenabled.classes.TestHarness;
import org.eclipselabs.nullness.tests.jdtenabled.classes.Outer.Mid;

public class InnerClassNonNullTest extends AbstractNonNullOnClassTest {

	@SuppressWarnings("null")
	@Override
	protected TestHarness createTestHarness(String s1, String s2) {
		Mid mid = new Outer().new Mid();
		return mid.new InnerWithNonNullConstraints(s1, s2);
	}

	@Override
	protected TestHarness createTestHarness() {
		Mid mid = new Outer().new Mid();
		return mid.new InnerWithNonNullConstraints();
	}

}