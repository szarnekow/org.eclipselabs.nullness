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
package org.eclipselabs.nullness.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */
public class NullnessPreferencePage extends PropertyAndPreferencePage {

	public static final String PREF_ID = "org.eclipselabs.nullness.ui.PreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID = "org.eclipselabs.nullness.ui.PropertyPage"; //$NON-NLS-1$

	private NullnessConfigurationBlock confBlock;

	@Override
	public void createControl(Composite parent) {
		IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
		confBlock = new NullnessConfigurationBlock(getNewStatusChangedListener(), getProject(), container);

		super.createControl(parent);
	}

	@Override
	protected boolean hasProjectSpecificOptions(IProject project) {
		return confBlock.hasProjectSpecificOptions(project);
	}

	@Override
	protected Control createPreferenceContent(Composite parent) {
		return confBlock.createContents(parent);

	}

	@Override
	protected String getPreferencePageID() {
		return NullnessPreferencePage.PREF_ID;
	}

	@Override
	protected String getPropertyPageID() {
		return NullnessPreferencePage.PROP_ID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage#
	 * enableProjectSpecificSettings(boolean)
	 */
	@Override
	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings) {
		super.enableProjectSpecificSettings(useProjectSpecificSettings);
		if (confBlock != null) {
			confBlock.useProjectSpecificSettings(useProjectSpecificSettings);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		if (confBlock != null) {
			confBlock.dispose();
		}
		super.dispose();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		super.performDefaults();
		if (confBlock != null) {
			confBlock.performDefaults();
		}
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (confBlock != null && !confBlock.performOk()) {
			return false;
		}
		return super.performOk();
	}

	/*
	 * @see org.eclipse.jface.preference.IPreferencePage#performApply()
	 */
	@Override
	public void performApply() {
		if (confBlock != null) {
			confBlock.performApply();
		}
	}

}