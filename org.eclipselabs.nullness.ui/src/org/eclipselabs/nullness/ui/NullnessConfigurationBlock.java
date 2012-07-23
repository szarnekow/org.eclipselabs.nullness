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
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.eclipselabs.nullness.PreferencesAccessor;

public class NullnessConfigurationBlock extends OptionsConfigurationBlock {
	private static final Key KEY_ACTIVE_BOOLEAN = NullnessConfigurationBlock.getRuntimeKey();

	public NullnessConfigurationBlock(IStatusChangeListener statusChangedListener, IProject project, Key[] allKeys,
			IWorkbenchPreferenceContainer container) {
		super(statusChangedListener, project, allKeys, container);
	}

	public NullnessConfigurationBlock(IStatusChangeListener statusChangedListener, IProject project, IWorkbenchPreferenceContainer container) {
		this(statusChangedListener, project, new Key[] { NullnessConfigurationBlock.KEY_ACTIVE_BOOLEAN }, container);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#
	 * createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		Composite fieldEditorParent = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		fieldEditorParent.setLayout(layout);
		fieldEditorParent.setFont(parent.getFont());

		addCheckBox(fieldEditorParent, "Generate runtime assertions", NullnessConfigurationBlock.KEY_ACTIVE_BOOLEAN, new String[] {
				IPreferenceStore.TRUE, IPreferenceStore.FALSE }, 0);

		Dialog.applyDialogFont(fieldEditorParent);
		return fieldEditorParent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#
	 * getFullBuildDialogStrings(boolean)
	 */
	@Override
	protected String[] getFullBuildDialogStrings(boolean workspaceSettings) {
		return new String[] { "Project rebuild needed", "Rebuild project now?" };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock#
	 * validateSettings
	 * (org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	protected void validateSettings(Key changedKey, String oldValue, String newValue) {
		// TODO Auto-generated method stub

	}

	private static Key getRuntimeKey() {
		return new Key(PreferencesAccessor.NODE_ID, PreferencesAccessor.ACTIVE_BOOLEAN);
	}

}
