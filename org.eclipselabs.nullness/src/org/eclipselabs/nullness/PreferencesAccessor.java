/*******************************************************************************
 * Copyright (c) 2012 Sebastian Zarnekow (http://zarnekow.blogspot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors:
 *   Dennis Huebner - Initial implementation
 *   Sebastian Zarnekow - Renamed constants
 *******************************************************************************/
package org.eclipselabs.nullness;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class PreferencesAccessor extends AbstractPreferenceInitializer {

	public static final String ACTIVE_BOOLEAN = "active"; //$NON-NLS-1$
	public static final String NODE_ID = "org.eclipselabs.nullness.compiler"; //$NON-NLS-1$

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaultPrefs = DefaultScope.INSTANCE.getNode(PreferencesAccessor.NODE_ID);
		defaultPrefs.putBoolean(PreferencesAccessor.ACTIVE_BOOLEAN, false);
	}

	/**
	 * If Project preference is available
	 * 
	 * @param preferenceName
	 * @param project
	 * @return
	 */
	public Boolean getBoolean(String preferenceName, IProject project) {
		boolean defaultValue = defaultPreferences().getBoolean(PreferencesAccessor.ACTIVE_BOOLEAN, false);
		boolean value = InstanceScope.INSTANCE.getNode(PreferencesAccessor.NODE_ID).getBoolean(PreferencesAccessor.ACTIVE_BOOLEAN,
				defaultValue);
		if (project != null) {
			IEclipsePreferences projectScopeNode = new ProjectScope(project).getNode(PreferencesAccessor.NODE_ID);
			value = projectScopeNode.getBoolean(PreferencesAccessor.ACTIVE_BOOLEAN, value);
		}
		return Boolean.valueOf(value);
	}

	private IEclipsePreferences defaultPreferences() {
		return DefaultScope.INSTANCE.getNode(PreferencesAccessor.NODE_ID);
	}

}
