/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui.internal.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.spearce.egit.core.Activator;
import org.spearce.egit.core.GitCorePreferences;
import org.spearce.egit.ui.UIText;

/** Preferences for our window cache. */
public class WindowCachePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {
	private static final int MB = 1024 * 1024;

	private static final int GB = 1024 * MB;

	/** */
	public WindowCachePreferencePage() {
		super(GRID);
		setTitle(UIText.WindowCachePreferencePage_title);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		addField(new StorageSizeFieldEditor(
				GitCorePreferences.core_packedGitWindowSize,
				UIText.WindowCachePreferencePage_packedGitWindowSize,
				getFieldEditorParent(), 512, 128 * MB) {
			protected boolean checkValue(final int number) {
				return super.checkValue(number)
						&& Integer.bitCount(number) == 1;
			}
		});

		addField(new StorageSizeFieldEditor(
				GitCorePreferences.core_packedGitLimit,
				UIText.WindowCachePreferencePage_packedGitLimit,
				getFieldEditorParent(), 512, 1 * GB));
		addField(new StorageSizeFieldEditor(
				GitCorePreferences.core_deltaBaseCacheLimit,
				UIText.WindowCachePreferencePage_deltaBaseCacheLimit,
				getFieldEditorParent(), 512, 1 * GB));

		addField(new BooleanFieldEditor(GitCorePreferences.core_packedGitMMAP,
				UIText.WindowCachePreferencePage_packedGitMMAP,
				getFieldEditorParent()));

		final Composite pad = new Composite(getFieldEditorParent(), SWT.NONE);
		final RowLayout rl = new RowLayout(SWT.HORIZONTAL);
		rl.marginLeft = 0;
		rl.marginTop = 20;
		pad.setLayout(rl);
		Label lbl;

		lbl = new Label(pad, SWT.NONE);
		lbl.setText(UIText.WindowCachePreferencePage_note);
		lbl.setFont(JFaceResources.getFontRegistry().getBold(
				getDialogFontName()));

		lbl = new Label(pad, SWT.NONE);
		lbl.setText(UIText.WindowCachePreferencePage_needRestart);
		lbl.setFont(getFont());
	}

	public boolean performOk() {
		Activator.getDefault().savePluginPreferences();
		return super.performOk();
	}

	public void init(IWorkbench workbench) {
		// Nothing to do
	}
}
