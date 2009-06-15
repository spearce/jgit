/*******************************************************************************
 * Copyright (C) 2003, 2006 Subclipse project and others.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.SWTUtils;
import org.spearce.egit.ui.internal.decorators.IDecoratableResource;
import org.spearce.egit.ui.internal.decorators.GitLightweightDecorator.DecorationHelper;
import org.spearce.egit.ui.internal.decorators.IDecoratableResource.Staged;


/**
 * Preference page for customizing Git label decorations
 */
public class GitDecoratorPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Text fileTextFormat;

	private Text folderTextFormat;

	private Text projectTextFormat;

	private Button recomputeAncestorDecorations;

	private Scale containerRecurseLimit;

	private Button showTracked;

	private Button showUntracked;

	private Preview preview;

	private Button showStaged;

	private Button showConflicts;

	private Button showAssumeValid;

	private static final Collection PREVIEW_FILESYSTEM_ROOT;

	private static IPropertyChangeListener themeListener;

	static {
		final PreviewResource project = new PreviewResource(
				"Project", IResource.PROJECT, "master", true, false, true, Staged.NOT_STAGED, false, false); //$NON-NLS-1$1
		final ArrayList<PreviewResource> children = new ArrayList<PreviewResource>();

		children.add(new PreviewResource(
						"folder", IResource.FOLDER, null, true, false, true, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"tracked.txt", IResource.FILE, null, true, false, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"untracked.txt", IResource.FILE, null, false, false, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"ignored.txt", IResource.FILE, null, false, true, false, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"dirty.txt", IResource.FILE, null, true, false, true, Staged.NOT_STAGED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"staged.txt", IResource.FILE, null, true, false, false, Staged.MODIFIED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"partially-staged.txt", IResource.FILE, null, true, false, true, Staged.MODIFIED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"added.txt", IResource.FILE, null, true, false, false, Staged.ADDED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"removed.txt", IResource.FILE, null, true, false, false, Staged.REMOVED, false, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"conflict.txt", IResource.FILE, null, true, false, true, Staged.NOT_STAGED, true, false)); //$NON-NLS-1$
		children.add(new PreviewResource(
						"assume-valid.txt", IResource.FILE, null, true, false, false, Staged.NOT_STAGED, false, true)); //$NON-NLS-1$
		project.children = children;
		PREVIEW_FILESYSTEM_ROOT = Collections.singleton(project);
	}

	/**
	 * Constructs a decorator preference page
	 */
	public GitDecoratorPreferencePage() {
		setDescription(UIText.DecoratorPreferencesPage_description);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_NONE);

		SWTUtils.createPreferenceLink(
				(IWorkbenchPreferenceContainer) getContainer(), composite,
				"org.eclipse.ui.preferencePages.Decorators", //$NON-NLS-1$
				UIText.DecoratorPreferencesPage_labelDecorationsLink);

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setLayoutData(SWTUtils.createHVFillGridData());

		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(UIText.DecoratorPreferencesPage_generalTabFolder);
		tabItem.setControl(createGeneralDecoratorPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(UIText.DecoratorPreferencesPage_textLabel);
		tabItem.setControl(createTextDecoratorPage(tabFolder));

		tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(UIText.DecoratorPreferencesPage_iconLabel);
		tabItem.setControl(createIconDecoratorPage(tabFolder));

		initializeValues();

		preview = new Preview(composite);
		preview.refresh();

		// TODO: Add help text for this preference page

		themeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				preview.refresh();
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(
				themeListener);

		Dialog.applyDialogFont(parent);

		return composite;
	}

	private Control createGeneralDecoratorPage(Composite parent) {
		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_DEFAULT, 1);

		recomputeAncestorDecorations = SWTUtils.createCheckBox(composite,
				UIText.DecoratorPreferencesPage_recomputeAncestorDecorations);
		recomputeAncestorDecorations
				.setToolTipText(UIText.DecoratorPreferencesPage_recomputeAncestorDecorationsTooltip);

		SWTUtils.createLabel(composite,
				UIText.DecoratorPreferencesPage_computeRecursiveLimit);
		containerRecurseLimit = createLabeledScaleControl(composite);
		containerRecurseLimit
				.setToolTipText(UIText.DecoratorPreferencesPage_computeRecursiveLimitTooltip);

		return composite;
	}

	private Scale createLabeledScaleControl(Composite parent) {

		final int[] values = new int[] { 0, 1, 2, 3, 5, 10, 15, 20, 50, 100,
				Integer.MAX_VALUE };

		Composite composite = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_DEFAULT);

		Composite labels = SWTUtils.createHVFillComposite(composite,
				SWTUtils.MARGINS_NONE, values.length);
		GridLayout labelsLayout = (GridLayout) labels.getLayout();
		labelsLayout.makeColumnsEqualWidth = true;
		labelsLayout.horizontalSpacing = 0;
		labels.setLayoutData(SWTUtils.createGridData(-1, -1, SWT.FILL,
				SWT.FILL, false, false));

		for (int i = 0; i < values.length; ++i) {
			Label label = SWTUtils.createLabel(labels, "" + values[i]);
			if (i == 0) {
				label.setAlignment(SWT.LEFT);
				label.setText("Off");
			} else if (i == values.length - 1) {
				label.setAlignment(SWT.RIGHT);
				label.setText("Inf.");
			} else {
				label.setAlignment(SWT.CENTER);
			}
		}

		final Scale scale = new Scale(composite, SWT.HORIZONTAL);
		scale.setLayoutData(SWTUtils.createHVFillGridData());
		scale.setMaximum(values.length - 1);
		scale.setMinimum(0);
		scale.setIncrement(1);
		scale.setPageIncrement(1);

		scale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// Workaround for GTK treating the slider as stepless
				scale.setSelection(scale.getSelection());
			}
		});

		return scale;
	}

	/**
	 * Creates the controls for the first tab folder
	 *
	 * @param parent
	 *
	 * @return the control
	 */
	private Control createTextDecoratorPage(Composite parent) {
		Composite fileTextGroup = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_DEFAULT, 3);

		int labelWidth = convertWidthInCharsToPixels(Math.max(
				UIText.DecoratorPreferencesPage_fileFormatLabel.length(),
				Math.max(UIText.DecoratorPreferencesPage_folderFormatLabel
						.length(),
						UIText.DecoratorPreferencesPage_projectFormatLabel
								.length())));

		TextPair format = createFormatEditorControl(fileTextGroup,
				UIText.DecoratorPreferencesPage_fileFormatLabel,
				UIText.DecoratorPreferencesPage_addVariablesAction,
				getFileBindingDescriptions(), labelWidth);
		fileTextFormat = format.t1;

		format = createFormatEditorControl(fileTextGroup,
				UIText.DecoratorPreferencesPage_folderFormatLabel,
				UIText.DecoratorPreferencesPage_addVariablesAction,
				getFolderBindingDescriptions(), labelWidth);
		folderTextFormat = format.t1;

		format = createFormatEditorControl(fileTextGroup,
				UIText.DecoratorPreferencesPage_projectFormatLabel,
				UIText.DecoratorPreferencesPage_addVariablesAction,
				getProjectBindingDescriptions(), labelWidth);
		projectTextFormat = format.t1;

		return fileTextGroup;
	}

	private Control createIconDecoratorPage(Composite parent) {
		Composite imageGroup = SWTUtils.createHVFillComposite(parent,
				SWTUtils.MARGINS_DEFAULT, 2);

		showTracked = SWTUtils.createCheckBox(imageGroup,
				UIText.DecoratorPreferencesPage_iconsShowTracked);
		showUntracked = SWTUtils.createCheckBox(imageGroup,
				UIText.DecoratorPreferencesPage_iconsShowUntracked);
		showStaged = SWTUtils.createCheckBox(imageGroup,
				UIText.DecoratorPreferencesPage_iconsShowStaged);
		showConflicts = SWTUtils.createCheckBox(imageGroup,
				UIText.DecoratorPreferencesPage_iconsShowConflicts);
		showAssumeValid = SWTUtils.createCheckBox(imageGroup,
				UIText.DecoratorPreferencesPage_iconsShowAssumeValid);

		return imageGroup;
	}

	private TextPair createFormatEditorControl(Composite composite,
			String title, String buttonText, final Map supportedBindings,
			int labelWidth) {

		Label label = SWTUtils.createLabel(composite, title);
		GridData labelGridData = new GridData();
		labelGridData.widthHint = labelWidth;
		label.setLayoutData(labelGridData);

		Text format = new Text(composite, SWT.BORDER);
		GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
		textGridData.widthHint = 200;
		format.setLayoutData(textGridData);
		format.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updatePreview();
			}
		});
		Button b = new Button(composite, SWT.NONE);
		b.setText(buttonText);
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, b.computeSize(SWT.DEFAULT,
				SWT.DEFAULT, true).x);
		b.setLayoutData(data);
		final Text formatToInsert = format;
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addVariables(formatToInsert, supportedBindings);
			}
		});

		return new TextPair(format, null);
	}

	/**
	 * Initializes states of the controls from the preference store.
	 */
	private void initializeValues() {
		final IPreferenceStore store = getPreferenceStore();

		recomputeAncestorDecorations.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
		containerRecurseLimit.setSelection(store
				.getInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT));

		fileTextFormat.setText(store
				.getString(UIPreferences.DECORATOR_FILETEXT_DECORATION));
		folderTextFormat.setText(store
				.getString(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION));
		projectTextFormat.setText(store
				.getString(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION));

		showTracked.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON));
		showUntracked.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON));
		showStaged.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON));
		showConflicts.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON));
		showAssumeValid.setSelection(store
				.getBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON));

		SelectionListener selectionListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				preview.refresh();
			}
		};

		showTracked.addSelectionListener(selectionListener);
		showUntracked.addSelectionListener(selectionListener);
		showStaged.addSelectionListener(selectionListener);
		showConflicts.addSelectionListener(selectionListener);
		showAssumeValid.addSelectionListener(selectionListener);

		setValid(true);
	}

	/**
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// No-op
	}

	/**
	 * OK was clicked. Store the preferences to the plugin store
	 *
	 * @return whether it is okay to close the preference page
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		final boolean okToClose = performOk(store);
		if (store.needsSaving()) {
			Activator.getDefault().savePluginPreferences();
			Activator.broadcastPropertyChange(new PropertyChangeEvent(this,
					Activator.DECORATORS_CHANGED, null, null));
		}
		return okToClose;
	}

	/**
	 * Store the preferences to the given preference store
	 *
	 * @param store
	 *            the preference store to store the preferences to
	 *
	 * @return whether it operation succeeded
	 */
	private boolean performOk(IPreferenceStore store) {

		store.setValue(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS,
				recomputeAncestorDecorations.getSelection());
		store.setValue(UIPreferences.DECORATOR_RECURSIVE_LIMIT,
				containerRecurseLimit.getSelection());

		store.setValue(UIPreferences.DECORATOR_FILETEXT_DECORATION,
				fileTextFormat.getText());
		store.setValue(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION,
				folderTextFormat.getText());
		store.setValue(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION,
				projectTextFormat.getText());

		store.setValue(UIPreferences.DECORATOR_SHOW_TRACKED_ICON, showTracked
				.getSelection());
		store.setValue(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON,
				showUntracked.getSelection());
		store.setValue(UIPreferences.DECORATOR_SHOW_STAGED_ICON, showStaged
				.getSelection());
		store.setValue(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON,
				showConflicts.getSelection());
		store.setValue(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON,
				showAssumeValid.getSelection());

		return true;
	}

	/**
	 * Defaults was clicked. Restore the Git decoration preferences to their
	 * default values
	 */
	protected void performDefaults() {
		super.performDefaults();
		IPreferenceStore store = getPreferenceStore();

		recomputeAncestorDecorations
				.setSelection(store
						.getDefaultBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS));
		containerRecurseLimit.setSelection(store
				.getDefaultInt(UIPreferences.DECORATOR_RECURSIVE_LIMIT));

		fileTextFormat.setText(store
				.getDefaultString(UIPreferences.DECORATOR_FILETEXT_DECORATION));
		folderTextFormat
				.setText(store
						.getDefaultString(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION));
		projectTextFormat
				.setText(store
						.getDefaultString(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION));

		showTracked.setSelection(store
				.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON));
		showUntracked
				.setSelection(store
						.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON));
		showStaged.setSelection(store
				.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON));
		showConflicts
				.setSelection(store
						.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON));
		showAssumeValid
				.setSelection(store
						.getDefaultBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON));
	}

	/**
	 * Returns the preference store that belongs to the our plugin.
	 *
	 * This is important because we want to store our preferences separately
	 * from the desktop.
	 *
	 * @return the preference store for this plugin
	 */
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	public void dispose() {
		PlatformUI.getWorkbench().getThemeManager()
				.removePropertyChangeListener(themeListener);
		super.dispose();
	}

	/**
	 * Adds another variable to the given target text
	 *
	 * A ListSelectionDialog pops up and allow the user to choose the variable,
	 * which is then inserted at current position in <code>text</code>
	 *
	 * @param target
	 *            the target to add the variable to
	 * @param bindings
	 *            the map of bindings
	 */
	private void addVariables(Text target, Map bindings) {

		final List<StringPair> variables = new ArrayList<StringPair>(bindings
				.size());

		ILabelProvider labelProvider = new LabelProvider() {
			public String getText(Object element) {
				return ((StringPair) element).s1
						+ " - " + ((StringPair) element).s2; //$NON-NLS-1$
			}
		};

		IStructuredContentProvider contentsProvider = new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				return variables.toArray(new StringPair[variables.size()]);
			}

			public void dispose() {
				// No-op
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// No-op
			}
		};

		for (Iterator it = bindings.keySet().iterator(); it.hasNext();) {
			StringPair variable = new StringPair();
			variable.s1 = (String) it.next(); // variable
			variable.s2 = (String) bindings.get(variable.s1); // description
			variables.add(variable);
		}

		ListSelectionDialog dialog = new ListSelectionDialog(this.getShell(),
				this, contentsProvider, labelProvider,
				UIText.DecoratorPreferencesPage_selectVariablesToAdd);
		dialog.setTitle(UIText.DecoratorPreferencesPage_addVariablesTitle);
		if (dialog.open() != Window.OK)
			return;

		Object[] result = dialog.getResult();

		for (int i = 0; i < result.length; i++) {
			target.insert("{" + ((StringPair) result[i]).s1 + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	class StringPair {
		String s1;

		String s2;
	}

	class TextPair {
		TextPair(Text t1, Text t2) {
			this.t1 = t1;
			this.t2 = t2;
		}

		Text t1;

		Text t2;
	}

	/**
	 * Gets the map of bindings between variables and description, to use for
	 * the format editors for files
	 *
	 * @return the bindings
	 */
	private Map getFileBindingDescriptions() {
		Map<String, String> bindings = new HashMap<String, String>();
		bindings.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		bindings.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		bindings.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);
		return bindings;
	}

	/**
	 * Gets the map of bindings between variables and description, to use for
	 * the format editors for folders
	 *
	 * @return the bindings
	 */
	private Map getFolderBindingDescriptions() {
		Map<String, String> bindings = new HashMap<String, String>();
		bindings.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		bindings.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		bindings.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);
		return bindings;
	}

	/**
	 * Gets the map of bindings between variables and description, to use for
	 * the format editors for projects
	 *
	 * @return the bindings
	 */
	private Map getProjectBindingDescriptions() {
		Map<String, String> bindings = new HashMap<String, String>();
		bindings.put(DecorationHelper.BINDING_RESOURCE_NAME,
				UIText.DecoratorPreferencesPage_bindingResourceName);
		bindings.put(DecorationHelper.BINDING_DIRTY_FLAG,
				UIText.DecoratorPreferencesPage_bindingDirtyFlag);
		bindings.put(DecorationHelper.BINDING_STAGED_FLAG,
				UIText.DecoratorPreferencesPage_bindingStagedFlag);
		bindings.put(DecorationHelper.BINDING_BRANCH_NAME,
				UIText.DecoratorPreferencesPage_bindingBranchName);
		return bindings;
	}

	private void updatePreview() {
		if (preview != null)
			preview.refresh();
	}

	/**
	 * Preview control for showing how changes in the dialog will affect
	 * decoration
	 */
	private class Preview extends LabelProvider implements Observer,
			ITreeContentProvider {

		private final ResourceManager fImageCache;

		private final TreeViewer fViewer;

		private DecorationHelper fHelper;

		public Preview(Composite composite) {
			// Has to happen before the tree control is constructed
			reloadDecorationHelper();
			SWTUtils.createLabel(composite,
					UIText.DecoratorPreferencesPage_preview);
			fImageCache = new LocalResourceManager(JFaceResources
					.getResources());

			fViewer = new TreeViewer(composite);
			fViewer.getControl().setLayoutData(SWTUtils.createHVFillGridData());
			fViewer.setContentProvider(this);
			fViewer.setLabelProvider(this);
			fViewer.setInput(PREVIEW_FILESYSTEM_ROOT);
			fViewer.expandAll();
			fHelper = new DecorationHelper(new PreferenceStore());
		}

		private void reloadDecorationHelper() {
			PreferenceStore store = new PreferenceStore();
			performOk(store);
			fHelper = new DecorationHelper(store);
		}

		public void refresh() {
			reloadDecorationHelper();
			fViewer.refresh(true);
			setColorsAndFonts(fViewer.getTree().getItems());
		}

		@SuppressWarnings("unused")
		private void setColorsAndFonts(TreeItem[] items) {
			// TODO: Implement colors and fonts
		}

		public void update(Observable o, Object arg) {
			refresh();
		}

		public Object[] getChildren(Object parentElement) {
			return ((PreviewResource) parentElement).children.toArray();
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return !((PreviewResource) element).children.isEmpty();
		}

		public Object[] getElements(Object inputElement) {
			return ((Collection) inputElement).toArray();
		}

		public void dispose() {
			fImageCache.dispose();
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// No-op
		}

		public String getText(Object element) {
			final PreviewDecoration decoration = getDecoration(element);
			final StringBuffer buffer = new StringBuffer();
			final String prefix = decoration.getPrefix();
			if (prefix != null)
				buffer.append(prefix);
			buffer.append(((PreviewResource) element).getName());
			final String suffix = decoration.getSuffix();
			if (suffix != null)
				buffer.append(suffix);
			return buffer.toString();
		}

		public Image getImage(Object element) {
			final String s;
			switch (((PreviewResource) element).type) {
			case IResource.PROJECT:
				s = SharedImages.IMG_OBJ_PROJECT;
				break;
			case IResource.FOLDER:
				s = ISharedImages.IMG_OBJ_FOLDER;
				break;
			default:
				s = ISharedImages.IMG_OBJ_FILE;
				break;
			}
			final Image baseImage = PlatformUI.getWorkbench().getSharedImages()
					.getImage(s);
			final ImageDescriptor overlay = getDecoration(element).getOverlay();
			if (overlay == null)
				return baseImage;
			try {
				return fImageCache.createImage(new DecorationOverlayIcon(
						baseImage, overlay, IDecoration.BOTTOM_RIGHT));
			} catch (Exception e) {
				Activator.logError(e.getMessage(), e);
			}

			return null;
		}

		private PreviewDecoration getDecoration(Object element) {
			PreviewDecoration decoration = new PreviewDecoration();
			fHelper.decorate(decoration, (PreviewResource) element);
			return decoration;
		}
	}

	private static class PreviewResource implements IDecoratableResource {
		private final String name;

		private final String branch;

		private final int type;

		private Collection children;

		private boolean tracked;

		private boolean ignored;

		private boolean dirty;

		private boolean conflicts;

		private Staged staged;

		private boolean assumeValid;

		public PreviewResource(String name, int type, String branch,
				boolean tracked, boolean ignored, boolean dirty, Staged staged,
				boolean conflicts, boolean assumeValid) {

			this.name = name;
			this.branch = branch;
			this.type = type;
			this.children = Collections.EMPTY_LIST;
			this.tracked = tracked;
			this.ignored = ignored;
			this.dirty = dirty;
			this.staged = staged;
			this.conflicts = conflicts;
			this.assumeValid = assumeValid;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public String getBranch() {
			return branch;
		}

		public boolean isTracked() {
			return tracked;
		}

		public boolean isIgnored() {
			return ignored;
		}

		public boolean isDirty() {
			return dirty;
		}

		public Staged staged() {
			return staged;
		}

		public boolean hasConflicts() {
			return conflicts;
		}

		public boolean isAssumeValid() {
			return assumeValid;
		}
	}

	private class PreviewDecoration implements IDecoration {

		private List<String> prefixes = new ArrayList<String>();

		private List<String> suffixes = new ArrayList<String>();

		private ImageDescriptor overlay = null;

		/**
		 * Adds an icon overlay to the decoration
		 * <p>
		 * Copies the behavior of <code>DecorationBuilder</code> of only
		 * allowing the overlay to be set once.
		 */
		public void addOverlay(ImageDescriptor overlayImage) {
			if (overlay == null)
				overlay = overlayImage;
		}

		public void addOverlay(ImageDescriptor overlayImage, int quadrant) {
			addOverlay(overlayImage);
		}

		public void addPrefix(String prefix) {
			prefixes.add(prefix);
		}

		public void addSuffix(String suffix) {
			suffixes.add(suffix);
		}

		public IDecorationContext getDecorationContext() {
			return new DecorationContext();
		}

		public void setBackgroundColor(Color color) {
		}

		public void setForegroundColor(Color color) {
		}

		public void setFont(Font font) {
		}

		public ImageDescriptor getOverlay() {
			return overlay;
		}

		public String getPrefix() {
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> iter = prefixes.iterator(); iter.hasNext();) {
				sb.append(iter.next());
			}
			return sb.toString();
		}

		public String getSuffix() {
			StringBuffer sb = new StringBuffer();
			for (Iterator<String> iter = suffixes.iterator(); iter.hasNext();) {
				sb.append(iter.next());
			}
			return sb.toString();
		}

	}
}
