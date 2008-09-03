/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Ref.Storage;
import org.spearce.jgit.transport.FetchConnection;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;

/**
 * This class provides universal panel for editing list of {@link RefSpec} -
 * specifications for both push or fetch, depending on panel configuration.
 * <p>
 * It is intended to allow user easily edit specifications, supporting user with
 * content assistant and giving feedback with error information as soon as
 * possible. Component uses editable specifications table and panels for easy
 * creation of new specifications basing on typical push/fetch schemes (like
 * branch update, deletion, all branches update, saved configuration etc.).
 * <p>
 * The model of specifications list behind panel is accessible by public methods
 * - giving both read and write access. Listener interface for handling changes
 * in model is provided by {@link SelectionChangeListener}.
 * <p>
 * Typical class usage:
 *
 * <pre>
 * // create panel for editing push-specifications
 * RefSpecPanel panel = new RefSpecPanel(parent, true);
 * // register model listener
 * panel.addRefSpecPanelListener(listener);
 *
 * // provide information about local and remote refs
 * panel.setRefsData(localRepo, remoteRefs, remoteName);
 *
 * // get result data
 * List&lt;RefSpec&gt; result = panel.getRefSpecs();
 * // further processing: push or save configuration...
 * </pre>
 *
 * @see SelectionChangeListener
 */
public class RefSpecPanel {
	private static final String IMAGE_ADD = "ADD"; //$NON-NLS-1$

	private static final String IMAGE_DELETE = "DELETE"; //$NON-NLS-1$

	private static final String IMAGE_TRASH = "TRASH"; //$NON-NLS-1$

	private static final String IMAGE_CLEAR = "CLEAR"; //$NON-NLS-1$

	private static final int TABLE_PREFERRED_HEIGHT = 165;

	private static final int TABLE_PREFERRED_WIDTH = 560;

	private static final int COLUMN_MODE_WEIGHT = 23;

	private static final int COLUMN_SRC_WEIGHT = 40;

	private static final int COLUMN_DST_WEIGHT = 40;

	private static final int COLUMN_FORCE_WEIGHT = 30;

	private static final int COLUMN_REMOVE_WEIGHT = 20;

	private static boolean isDeleteRefSpec(final Object element) {
		return ((RefSpec) element).getSource() == null;
	}

	private static boolean isValidRefExpression(final String s) {
		if (RefSpec.isWildcard(s)) {
			// replace wildcard with some legal name just for checking
			return Repository
					.isValidRefName(s.substring(0, s.length() - 1) + 'X');
		} else
			return Repository.isValidRefName(s);
	}

	private static RefSpec setRefSpecSource(final RefSpec spec, final String src) {
		final String dst;
		if (RefSpec.isWildcard(src))
			dst = wildcardSpecComponent(spec.getDestination());
		else
			dst = unwildcardSpecComponent(spec.getDestination(), src);
		return spec.setSourceDestination(src, dst);
	}

	private static RefSpec setRefSpecDestination(final RefSpec spec,
			final String dst) {
		final String src;
		if (RefSpec.isWildcard(dst))
			src = wildcardSpecComponent(spec.getSource());
		else
			src = unwildcardSpecComponent(spec.getSource(), dst);
		return spec.setSourceDestination(src, dst);
	}

	private static String wildcardSpecComponent(final String comp) {
		final int i;
		if (RefSpec.isWildcard(comp))
			return comp;
		if (comp == null || (i = comp.lastIndexOf('/')) == -1) {
			// That's somewhat ugly. What better can we do here?
			return UIText.RefSpecPanel_refChooseSomeWildcard;
		}
		return comp.substring(0, i + 1) + '*';
	}

	private static String unwildcardSpecComponent(final String comp,
			final String other) {
		if (!RefSpec.isWildcard(comp))
			return comp;
		if (other == null || other.length() == 0)
			return ""; //$NON-NLS-1$
		final int i = other.lastIndexOf('/');
		return comp.substring(0, comp.length() - 1) + other.substring(i + 1);
	}

	private static List<RefContentProposal> createProposalsFilteredRemote(
			final List<RefContentProposal> proposals) {
		final List<RefContentProposal> result = new ArrayList<RefContentProposal>();
		for (final RefContentProposal p : proposals) {
			final String content = p.getContent();
			if (content.equals(Constants.HEAD)
					|| content.startsWith(Constants.R_HEADS))
				result.add(p);
		}
		return result;
	}

	private static Image getDecorationImage(final String key) {
		return FieldDecorationRegistry.getDefault().getFieldDecoration(key)
				.getImage();
	}

	private static void setControlDecoration(final ControlDecoration control,
			final String imageKey, final String description) {
		control.setImage(getDecorationImage(imageKey));
		control.setDescriptionText(description);
		control.show();
	}

	private final List<RefSpec> specs = new ArrayList<RefSpec>();

	private final Composite panel;

	private TableViewer tableViewer;

	private CellEditor modeCellEditor;

	private CellEditor localRefCellEditor;

	private CellEditor remoteRefCellEditor;

	private CellEditor forceUpdateCellEditor;

	private CellEditor removeSpecCellEditor;

	private int srcColumnIndex;

	private Button removeAllSpecButton;

	private Button forceUpdateAllButton;

	private Button creationButton;

	private Button addConfiguredButton;

	private Button addTagsButton;

	private Button addBranchesButton;

	private ControlDecoration creationSrcDecoration;

	private ControlDecoration creationDstDecoration;

	private ControlDecoration deleteRefDecoration;

	private Combo creationSrcCombo;

	private Combo creationDstCombo;

	private Combo deleteRefCombo;

	private Button deleteButton;

	private Repository localDb;

	private String remoteName;

	private Set<String> localRefNames = Collections.emptySet();

	private Set<String> remoteRefNames = Collections.emptySet();

	private List<RefSpec> predefinedConfigured = Collections.emptyList();

	private RefSpec predefinedBranches = null;

	private final RefContentProposalProvider remoteProposalProvider;

	private final RefContentProposalProvider localProposalProvider;

	private ComboLabelingSupport creationSrcComboSupport;

	private ComboLabelingSupport creationDstComboSupport;

	private ComboLabelingSupport deleteRefComboSupport;

	private final boolean pushSpecs;

	private final List<SelectionChangeListener> listeners = new LinkedList<SelectionChangeListener>();

	private final ImageRegistry imageRegistry;

	private boolean matchingAnyRefs;

	private RefSpec invalidSpec;

	private RefSpec invalidSpecSameDst;

	private String errorMessage;

	private Color errorBackgroundColor;

	private Color errorTextColor;

	/**
	 * Create a new panel and install it on a provided composite. Panel is
	 * created either for editing push or fetch specifications - this setting
	 * can't be changed later, after constructing object.
	 * <p>
	 * Panel is created with an empty model, with no provided assistant. It
	 * can't be used by user until
	 * {@link #setAssistanceData(Repository, Collection, String)} method is
	 * called, and to this time is disabled.
	 *
	 * @param parent
	 *            parent control for panel.
	 * @param pushSpecs
	 *            true if panel is used for editing push specifications, false
	 *            if panel is used for editing fetch specifications.
	 */
	public RefSpecPanel(final Composite parent, final boolean pushSpecs) {
		this.pushSpecs = pushSpecs;
		this.localProposalProvider = new RefContentProposalProvider(pushSpecs);
		this.remoteProposalProvider = new RefContentProposalProvider(false);
		this.imageRegistry = new ImageRegistry(parent.getDisplay());

		panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());

		safeCreateResources();

		createCreationPanel();
		if (pushSpecs)
			createDeleteCreationPanel();
		createPredefinedCreationPanel();
		createTableGroup();

		addRefSpecTableListener(new SelectionChangeListener() {
			public void selectionChanged() {
				validateSpecs();
			}
		});
		setEnable(false);
	}

	/**
	 * Enable or disable panel controls.
	 *
	 * @param enable
	 *            true to enable panel, false to disable.
	 */
	public void setEnable(final boolean enable) {
		getControl().setEnabled(enable);
	}

	/**
	 * Set information needed for assisting user with entering data and
	 * validating user input. This method automatically enables the panel.
	 *
	 * @param localRepo
	 *            local repository where specifications will be applied.
	 * @param remoteRefs
	 *            collection of remote refs as advertised by remote repository.
	 *            Typically they are collected by {@link FetchConnection}
	 *            implementation.
	 * @param remoteName
	 *            optional name for remote configuration, if edited
	 *            specification list is related to this remote configuration.
	 *            Can be null. When not null, panel is filled with default
	 *            fetch/push specifications for this remote configuration.
	 */
	public void setAssistanceData(final Repository localRepo,
			final Collection<Ref> remoteRefs, final String remoteName) {
		this.localDb = localRepo;
		this.remoteName = remoteName;

		final List<RefContentProposal> remoteProposals = createContentProposals(
				remoteRefs, null);
		remoteProposalProvider.setProposals(remoteProposals);
		remoteRefNames = new HashSet<String>();
		for (final RefContentProposal p : remoteProposals)
			remoteRefNames.add(p.getContent());

		Ref HEAD = null;
		try {
			final ObjectId id = localDb.resolve(Constants.HEAD);
			if (id != null)
				HEAD = new Ref(Storage.LOOSE, Constants.HEAD, id);
		} catch (IOException e) {
			Activator.logError("Couldn't read HEAD from local repository", e); //$NON-NLS-1$
		}
		final List<RefContentProposal> localProposals = createContentProposals(
				localDb.getAllRefs().values(), HEAD);
		localProposalProvider.setProposals(localProposals);
		localRefNames = new HashSet<String>();
		for (final RefContentProposal ref : localProposals)
			localRefNames.add(ref.getContent());

		final List<RefContentProposal> localFilteredProposals = createProposalsFilteredLocal(localProposals);
		final List<RefContentProposal> remoteFilteredProposals = createProposalsFilteredRemote(remoteProposals);

		if (pushSpecs) {
			creationSrcComboSupport.setProposals(localFilteredProposals);
			creationDstComboSupport.setProposals(remoteFilteredProposals);
		} else {
			creationSrcComboSupport.setProposals(remoteFilteredProposals);
			creationDstComboSupport.setProposals(localFilteredProposals);
		}
		validateCreationPanel();

		if (pushSpecs) {
			deleteRefComboSupport.setProposals(remoteFilteredProposals);
			validateDeleteCreationPanel();
		}

		try {
			if (remoteName == null)
				predefinedConfigured = Collections.emptyList();
			else {
				final RemoteConfig rc = new RemoteConfig(localDb.getConfig(),
						remoteName);
				if (pushSpecs)
					predefinedConfigured = rc.getPushRefSpecs();
				else
					predefinedConfigured = rc.getFetchRefSpecs();
				for (final RefSpec spec : predefinedConfigured)
					addRefSpec(spec);
			}
		} catch (URISyntaxException e) {
			predefinedConfigured = null;
			ErrorDialog.openError(panel.getShell(),
					UIText.RefSpecPanel_errorRemoteConfigTitle,
					UIText.RefSpecPanel_errorRemoteConfigDescription,
					new Status(IStatus.ERROR, Activator.getPluginId(), 0, e
							.getMessage(), e));
		}
		updateAddPredefinedButton(addConfiguredButton, predefinedConfigured);
		if (pushSpecs)
			predefinedBranches = Transport.REFSPEC_PUSH_ALL;
		else {
			final String r;
			if (remoteName == null)
				r = UIText.RefSpecPanel_refChooseRemoteName;
			else
				r = remoteName;
			predefinedBranches = new RefSpec("refs/heads/*:refs/remotes/" //$NON-NLS-1$
					+ r + "/*"); //$NON-NLS-1$
		}
		updateAddPredefinedButton(addBranchesButton, predefinedBranches);
		setEnable(true);
	}

	/**
	 * @return underlying control for this panel.
	 */
	public Control getControl() {
		return panel;
	}

	/**
	 * Return current list of specifications of this panel.
	 * <p>
	 * This method should be called only from the UI thread.
	 *
	 * @return unmodifiable view of specifications list as edited by user in
	 *         this panel. Note that this view underlying model may change -
	 *         create a copy if needed.
	 */
	public List<RefSpec> getRefSpecs() {
		return Collections.unmodifiableList(specs);
	}

	/**
	 * @return true if specifications list is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return getRefSpecs().isEmpty();
	}

	/**
	 * @return true if specifications match any ref(s) in source repository -
	 *         resolve to concrete ref updates, false otherwise. For non empty
	 *         specifications list, false value is possible only in case of
	 *         specifications with wildcards.
	 */
	public boolean isMatchingAnyRefs() {
		return matchingAnyRefs;
	}

	/**
	 * Add provided specification to this panel. Panel view is automatically
	 * refreshed, model is revalidated.
	 * <p>
	 * Note that the same reference can't be added twice to the panel, while two
	 * or more equals RefSpec (in terms of equals method) can be - likely
	 * causing validation error.
	 * <p>
	 * This method should be called only from the UI thread.
	 *
	 * @param spec
	 *            specification to add.
	 * @throws IllegalArgumentException
	 *             if specification with same reference already exists in panel.
	 */
	public void addRefSpec(final RefSpec spec) {
		final int i = indexOfSpec(spec);
		if (i != -1)
			throw new IllegalArgumentException("RefSpec " + spec //$NON-NLS-1$
					+ " already exists."); //$NON-NLS-1$
		specs.add(spec);
		tableViewer.add(spec);
		notifySpecsChanged();
	}

	/**
	 * Remove provided specification from this panel. Panel view is
	 * automatically refreshed, model is revalidated.
	 * <p>
	 * Provided specification must be equals with existing one in terms of
	 * reference equality, not an equals method.
	 * <p>
	 * This method should be called only from the UI thread.
	 *
	 * @param spec
	 *            specification to remove.
	 * @throws IllegalArgumentException
	 *             if specification with this reference doesn't exist in this
	 *             panel.
	 */
	public void removeRefSpec(final RefSpec spec) {
		final int i = indexOfSpec(spec);
		if (i == -1)
			throw new IllegalArgumentException("RefSpec " + spec //$NON-NLS-1$
					+ " not found."); //$NON-NLS-1$
		specs.remove(i);
		tableViewer.remove(spec);
		notifySpecsChanged();
	}

	/**
	 * Change some specification to the new one.
	 * <p>
	 * Old specification must exist in the panel, while new specification can't
	 * exist before (both in terms of reference equality).
	 * <p>
	 * This method should be called only from the UI thread.
	 *
	 * @param oldSpec
	 *            specification to change. Can't be null.
	 * @param newSpec
	 *            new specification to override existing one. Can't be null.
	 */
	public void setRefSpec(final RefSpec oldSpec, final RefSpec newSpec) {
		final int oldI = indexOfSpec(oldSpec);
		if (oldI == -1)
			throw new IllegalArgumentException("RefSpec " + oldSpec //$NON-NLS-1$
					+ " not found."); //$NON-NLS-1$
		final int newI = indexOfSpec(newSpec);
		if (newI != -1)
			throw new IllegalArgumentException("RefSpec " + newSpec //$NON-NLS-1$
					+ " already exists."); //$NON-NLS-1$
		specs.set(oldI, newSpec);

		// have to refresh whole table as we are operating on immutable objects
		// (this shouldn't be an issue)
		tableViewer.refresh();
		notifySpecsChanged();
	}

	/**
	 * Clear all specifications from this panel.
	 * <p>
	 * This method should be called only from the UI thread.
	 */
	public void clearRefSpecs() {
		final RefSpec toRemove[] = specs.toArray(new RefSpec[0]);
		specs.clear();
		tableViewer.remove(toRemove);
		notifySpecsChanged();
	}

	/**
	 * Add listener of changes in panel model.
	 * <p>
	 * Listeners are notified on events caused by both operations invoked by
	 * external calls and user interaction. Listener method(s) is always called
	 * from UI thread and shouldn't perform long computations.
	 * <p>
	 * Order of adding listeners is significant. This method is not thread-safe.
	 * Listeners should be set up before panel usage.
	 *
	 * @param listener
	 *            listener to add.
	 */
	public void addRefSpecTableListener(final SelectionChangeListener listener) {
		listeners.add(listener);
	}

	/**
	 * Get user-friendly error message regarding invalid specification.
	 *
	 * @return user-readable information about invalid specification.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Return information about validity of specifications.
	 * <p>
	 * Specifications are considered valid if pushing/fetching (depending on
	 * panel configuration) shouldn't cause any error except for
	 * non-fast-forward or server-related errors complaint. I.e. specifications
	 * destinations don't overlap and every specification is correctly
	 * formulated, preferably none is referring to non-existing ref etc.
	 *
	 * @return true if all specifications in panel are valid, false if at least
	 *         one specification is invalid (in this case
	 *         {@link #getErrorMessage()} gives detailed information for user).
	 */
	public boolean isValid() {
		return errorMessage == null;
	}

	private int indexOfSpec(final RefSpec spec) {
		int i;
		for (i = 0; i < specs.size(); i++) {
			// we have to compare references, not use List#indexOf,
			// as equals is implemented in RefSpec
			if (specs.get(i) == spec)
				break;
		}
		if (i == specs.size())
			return -1;
		return i;
	}

	private void notifySpecsChanged() {
		for (final SelectionChangeListener listener : listeners)
			listener.selectionChanged();
	}

	private void safeCreateResources() {
		imageRegistry.put(IMAGE_ADD, UIIcons.ELCL16_ADD);
		imageRegistry.put(IMAGE_DELETE, UIIcons.ELCL16_DELETE);
		imageRegistry.put(IMAGE_TRASH, UIIcons.ELCL16_TRASH);
		imageRegistry.put(IMAGE_CLEAR, UIIcons.ELCL16_CLEAR);
		errorBackgroundColor = new Color(panel.getDisplay(), 255, 150, 150);
		errorTextColor = new Color(panel.getDisplay(), 255, 0, 0);

		panel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				imageRegistry.dispose();
				errorBackgroundColor.dispose();
				errorTextColor.dispose();
			}
		});
	}

	private RefContentProposalProvider getRefsProposalProvider(
			final boolean local) {
		return (local ? localProposalProvider : remoteProposalProvider);
	}

	private void createCreationPanel() {
		final Group creationPanel = new Group(panel, SWT.NONE);
		creationPanel.setText(UIText.RefSpecPanel_creationGroup);
		creationPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 2;
		creationPanel.setLayout(layout);

		new Label(creationPanel, SWT.NONE)
				.setText(UIText.RefSpecPanel_creationSrc);
		new Label(creationPanel, SWT.NONE)
				.setText(UIText.RefSpecPanel_creationDst);
		creationButton = new Button(creationPanel, SWT.PUSH);
		creationButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false,
				false, 1, 2));
		creationButton.setImage(imageRegistry.get(IMAGE_ADD));
		creationButton.setText(UIText.RefSpecPanel_creationButton);
		creationButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String src = creationSrcCombo.getText();
				final String dst = creationDstCombo.getText();
				RefSpec spec = new RefSpec(src + ':' + dst);
				addRefSpec(spec);
				creationSrcCombo.setText(""); //$NON-NLS-1$
				creationDstCombo.setText(""); //$NON-NLS-1$
			}
		});
		creationButton.setToolTipText(NLS.bind(
				UIText.RefSpecPanel_creationButtonDescription, typeString()));

		creationSrcDecoration = createAssistedDecoratedCombo(creationPanel,
				getRefsProposalProvider(pushSpecs),
				new IContentProposalListener() {
					public void proposalAccepted(IContentProposal proposal) {
						tryAutoCompleteSrcToDst();
					}
				});
		creationSrcCombo = (Combo) creationSrcDecoration.getControl();
		creationSrcCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		creationSrcCombo.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				// SWT.TRAVERSE_RETURN may be also reasonable here, but
				// it can be confused with RETURN for content proposal
				if (e.detail == SWT.TRAVERSE_TAB_NEXT)
					tryAutoCompleteSrcToDst();
			}
		});
		if (pushSpecs)
			creationSrcCombo
					.setToolTipText(UIText.RefSpecPanel_srcPushDescription);
		else
			creationSrcCombo
					.setToolTipText(UIText.RefSpecPanel_srcFetchDescription);
		creationSrcComboSupport = new ComboLabelingSupport(creationSrcCombo,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						tryAutoCompleteSrcToDst();
					}
				});

		creationDstDecoration = createAssistedDecoratedCombo(creationPanel,
				getRefsProposalProvider(!pushSpecs),
				new IContentProposalListener() {
					public void proposalAccepted(IContentProposal proposal) {
						tryAutoCompleteDstToSrc();
					}
				});
		creationDstCombo = (Combo) creationDstDecoration.getControl();
		creationDstCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		creationDstCombo.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				// SWT.TRAVERSE_RETURN may be also reasonable here, but
				// it can be confused with RETURN for content proposal
				if (e.detail == SWT.TRAVERSE_TAB_NEXT)
					tryAutoCompleteDstToSrc();
			}
		});
		if (pushSpecs)
			creationDstCombo
					.setToolTipText(UIText.RefSpecPanel_dstPushDescription);
		else
			creationDstCombo
					.setToolTipText(UIText.RefSpecPanel_dstFetchDescription);
		creationDstComboSupport = new ComboLabelingSupport(creationDstCombo,
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						tryAutoCompleteDstToSrc();
					}
				});

		validateCreationPanel();
		final ModifyListener validator = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateCreationPanel();
			}
		};
		creationSrcCombo.addModifyListener(validator);
		creationDstCombo.addModifyListener(validator);
	}

	private void createDeleteCreationPanel() {
		final Group deletePanel = new Group(panel, SWT.NONE);
		deletePanel.setText(UIText.RefSpecPanel_deletionGroup);
		deletePanel
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 10;
		deletePanel.setLayout(layout);

		final Label label = new Label(deletePanel, SWT.NONE);
		label.setText(UIText.RefSpecPanel_deletionRef);
		label.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		deleteRefDecoration = createAssistedDecoratedCombo(deletePanel,
				getRefsProposalProvider(false), null);
		deleteRefCombo = (Combo) deleteRefDecoration.getControl();
		deleteRefCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		deleteRefCombo
				.setToolTipText(UIText.RefSpecPanel_dstDeletionDescription);
		deleteRefComboSupport = new ComboLabelingSupport(deleteRefCombo, null);

		deleteButton = new Button(deletePanel, SWT.PUSH);
		deleteButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false));
		deleteButton.setImage(imageRegistry.get(IMAGE_DELETE));
		deleteButton.setText(UIText.RefSpecPanel_deletionButton);
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpec spec = new RefSpec(':' + deleteRefCombo.getText());
				addRefSpec(spec);
				deleteRefCombo.setText(""); //$NON-NLS-1$
			}
		});
		deleteButton
				.setToolTipText(UIText.RefSpecPanel_deletionButtonDescription);
		validateDeleteCreationPanel();

		deleteRefCombo.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateDeleteCreationPanel();
			}
		});
	}

	private void createPredefinedCreationPanel() {
		final Group predefinedPanel = new Group(panel, SWT.NONE);
		predefinedPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		predefinedPanel.setText(UIText.RefSpecPanel_predefinedGroup);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		predefinedPanel.setLayout(layout);

		addConfiguredButton = new Button(predefinedPanel, SWT.PUSH);
		addConfiguredButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false));
		addConfiguredButton.setText(NLS.bind(
				UIText.RefSpecPanel_predefinedConfigured, typeString()));
		addConfiguredButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addPredefinedRefSpecs(predefinedConfigured);
			}
		});
		addConfiguredButton
				.setToolTipText(UIText.RefSpecPanel_predefinedConfiguredDescription);
		updateAddPredefinedButton(addConfiguredButton, predefinedConfigured);

		addBranchesButton = new Button(predefinedPanel, SWT.PUSH);
		addBranchesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				true, false));
		addBranchesButton.setText(UIText.RefSpecPanel_predefinedAll);
		addBranchesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addPredefinedRefSpecs(predefinedBranches);
			}
		});
		addBranchesButton
				.setToolTipText(UIText.RefSpecPanel_predefinedAllDescription);
		updateAddPredefinedButton(addBranchesButton, predefinedBranches);

		addTagsButton = new Button(predefinedPanel, SWT.PUSH);
		addTagsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));
		addTagsButton.setText(UIText.RefSpecPanel_predefinedTags);
		addTagsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addPredefinedRefSpecs(Transport.REFSPEC_TAGS);
			}
		});
		addTagsButton
				.setToolTipText(UIText.RefSpecPanel_predefinedTagsDescription);
		updateAddPredefinedButton(addTagsButton, Transport.REFSPEC_TAGS);

		addRefSpecTableListener(new SelectionChangeListener() {
			public void selectionChanged() {
				updateAddPredefinedButton(addConfiguredButton,
						predefinedConfigured);
				updateAddPredefinedButton(addBranchesButton, predefinedBranches);
				updateAddPredefinedButton(addTagsButton, Transport.REFSPEC_TAGS);
			}
		});
	}

	private ControlDecoration createAssistedDecoratedCombo(
			final Composite parent,
			final IContentProposalProvider proposalProvider,
			final IContentProposalListener listener) {
		// FIXME: VERY ANNOYING! reported as 243991 in eclipse bugzilla
		// when typing, pressing arrow-down key opens combo box drop-down
		// instead of moving within autocompletion list (Mac 10.4&10.5, Eclipse
		// 3.4)
		final Combo combo = new Combo(parent, SWT.DROP_DOWN);
		final ControlDecoration decoration = new ControlDecoration(combo,
				SWT.BOTTOM | SWT.LEFT);
		final ContentAssistCommandAdapter proposal = new ContentAssistCommandAdapter(
				combo, new ComboContentAdapter(), proposalProvider, null, null,
				true);
		proposal
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		if (listener != null)
			proposal.addContentProposalListener(listener);
		return decoration;
	}

	private void createTableGroup() {
		final Group tableGroup = new Group(panel, SWT.NONE);
		tableGroup.setText(NLS.bind(UIText.RefSpecPanel_specifications,
				typeString()));
		tableGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableGroup.setLayout(new GridLayout());

		createTable(tableGroup);
		createSpecsButtonsPanel(tableGroup);
	}

	private void createTable(final Group tableGroup) {
		final Composite tablePanel = new Composite(tableGroup, SWT.NONE);
		final GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.heightHint = TABLE_PREFERRED_HEIGHT;
		layoutData.widthHint = TABLE_PREFERRED_WIDTH;
		tablePanel.setLayoutData(layoutData);

		tableViewer = new TableViewer(tablePanel, SWT.FULL_SELECTION
				| SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		ColumnViewerToolTipSupport.enableFor(tableViewer);
		final Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		createTableColumns(tablePanel);
		createCellEditors(table);

		tableViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(final Object inputElement) {
				return ((List) inputElement).toArray();
			}

			public void dispose() {
				// nothing to dispose
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// input is hard coded
			}
		});
		tableViewer.setInput(specs);

		tableViewer.setComparer(new IElementComparer() {
			public boolean equals(Object a, Object b) {
				// need that as viewers are not designed to support 2 equals
				// object, while we have RefSpec#equals implemented
				return a == b;
			}

			public int hashCode(Object element) {
				return element.hashCode();
			}
		});
	}

	private void createTableColumns(final Composite tablePanel) {
		final TableColumnLayout columnLayout = new TableColumnLayout();
		tablePanel.setLayout(columnLayout);

		createDummyColumn(columnLayout);
		if (pushSpecs)
			createModeColumn(columnLayout);
		createSrcColumn(columnLayout);
		createDstColumn(columnLayout);
		createForceColumn(columnLayout);
		createRemoveColumn(columnLayout);
	}

	private void createDummyColumn(final TableColumnLayout columnLayout) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, SWT.LEFT);
		final TableColumn column = viewerColumn.getColumn();
		columnLayout.setColumnData(column, new ColumnWeightData(0, 0, false));
		viewerColumn.setLabelProvider(new ColumnLabelProvider());
		// FIXME: first cell is left aligned on Mac OS X 10.4, Eclipse 3.4
	}

	private void createModeColumn(final TableColumnLayout columnLayout) {
		final TableViewerColumn column = createColumn(columnLayout,
				UIText.RefSpecPanel_columnMode, COLUMN_MODE_WEIGHT, SWT.CENTER);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return (isDeleteRefSpec(element) ? UIText.RefSpecPanel_modeDelete
						: UIText.RefSpecPanel_modeUpdate);
			}

			@Override
			public Image getImage(Object element) {
				return (isDeleteRefSpec(element) ? imageRegistry
						.get(IMAGE_DELETE) : imageRegistry.get(IMAGE_ADD));
			}

			@Override
			public String getToolTipText(Object element) {
				if (isDeleteRefSpec(element))
					return UIText.RefSpecPanel_modeDeleteDescription + '\n'
							+ UIText.RefSpecPanel_clickToChange;
				return UIText.RefSpecPanel_modeUpdateDescription + '\n'
						+ UIText.RefSpecPanel_clickToChange;
			}
		});
		column.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(final Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(final Object element) {
				return modeCellEditor;
			}

			@Override
			protected Object getValue(final Object element) {
				return isDeleteRefSpec(element);
			}

			@Override
			protected void setValue(final Object element, final Object value) {
				final RefSpec oldSpec = (RefSpec) element;
				final RefSpec newSpec;
				if ((Boolean) value) {
					newSpec = setRefSpecSource(oldSpec, null);
					setRefSpec(oldSpec, newSpec);
				} else {
					newSpec = setRefSpecSource(oldSpec,
							UIText.RefSpecPanel_refChooseSome);
					setRefSpec(oldSpec, newSpec);
					tableViewer.getControl().getDisplay().asyncExec(
							new Runnable() {
								public void run() {
									tableViewer.editElement(newSpec,
											srcColumnIndex);
								}
							});
				}
			}
		});
	}

	private void createSrcColumn(final TableColumnLayout columnLayout) {
		final TableViewerColumn column = createColumn(columnLayout,
				UIText.RefSpecPanel_columnSrc, COLUMN_SRC_WEIGHT, SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return ((RefSpec) element).getSource();
			}

			@Override
			public String getToolTipText(Object element) {
				if (isInvalidSpec(element))
					return errorMessage;
				if (isDeleteRefSpec(element))
					return UIText.RefSpecPanel_srcDeleteDescription;
				if (pushSpecs)
					return UIText.RefSpecPanel_srcPushDescription;
				return UIText.RefSpecPanel_srcFetchDescription;
			}

			@Override
			public Color getBackground(final Object element) {
				if (isInvalidSpec(element))
					return errorBackgroundColor;
				return null;
			}

			@Override
			public Color getToolTipForegroundColor(Object element) {
				if (isInvalidSpec(element))
					return errorTextColor;
				return null;
			}
		});
		column.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(final Object element) {
				return !isDeleteRefSpec(element);
			}

			@Override
			protected CellEditor getCellEditor(final Object element) {
				return (pushSpecs ? localRefCellEditor : remoteRefCellEditor);
			}

			@Override
			protected Object getValue(final Object element) {
				return ((RefSpec) element).getSource();
			}

			@Override
			protected void setValue(final Object element, final Object value) {
				if (value == null || ((String) value).length() == 0
						|| ObjectId.zeroId().name().equals(value)) {
					// Ignore empty strings or null objects - do not set them in
					// model.User won't loose any information if we just fall
					// back to the old value.
					// If user want to delete ref, let change the mode.
					return;
				}

				final RefSpec oldSpec = (RefSpec) element;
				final RefSpec newSpec = setRefSpecSource(oldSpec,
						(String) value);
				setRefSpec(oldSpec, newSpec);
			}
		});

		// find index of this column - for later usage
		final TableColumn[] columns = tableViewer.getTable().getColumns();
		for (srcColumnIndex = 0; srcColumnIndex < columns.length; srcColumnIndex++)
			if (columns[srcColumnIndex] == column.getColumn())
				break;

	}

	private void createDstColumn(final TableColumnLayout columnLayout) {
		final TableViewerColumn column = createColumn(columnLayout,
				UIText.RefSpecPanel_columnDst, COLUMN_DST_WEIGHT, SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(final Object element) {
				return ((RefSpec) element).getDestination();
			}

			@Override
			public String getToolTipText(Object element) {
				if (isInvalidSpec(element))
					return errorMessage;
				if (isDeleteRefSpec(element))
					return UIText.RefSpecPanel_dstDeletionDescription;
				if (pushSpecs)
					return UIText.RefSpecPanel_dstPushDescription;
				return UIText.RefSpecPanel_dstFetchDescription;
			}

			@Override
			public Color getBackground(final Object element) {
				if (isInvalidSpec(element))
					return errorBackgroundColor;
				return null;
			}

			@Override
			public Color getToolTipForegroundColor(Object element) {
				if (isInvalidSpec(element))
					return errorTextColor;
				return null;
			}
		});
		column.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(final Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(final Object element) {
				return (pushSpecs ? remoteRefCellEditor : localRefCellEditor);
			}

			@Override
			protected Object getValue(final Object element) {
				return ((RefSpec) element).getDestination();
			}

			@Override
			protected void setValue(final Object element, final Object value) {
				if (value == null || ((String) value).length() == 0) {
					// Ignore empty strings - do not set them in model.
					// User won't loose any information if we just fall back
					// to the old value.
					return;
				}

				final RefSpec oldSpec = (RefSpec) element;
				final RefSpec newSpec = setRefSpecDestination(oldSpec,
						(String) value);
				setRefSpec(oldSpec, newSpec);
			}
		});
	}

	private void createForceColumn(final TableColumnLayout columnLayout) {
		final TableViewerColumn column = createColumn(columnLayout,
				UIText.RefSpecPanel_columnForce, COLUMN_FORCE_WEIGHT,
				SWT.CENTER);
		column.setLabelProvider(new CheckboxLabelProvider(tableViewer
				.getControl()) {
			@Override
			protected boolean isChecked(final Object element) {
				return ((RefSpec) element).isForceUpdate();
			}

			@Override
			protected boolean isEnabled(Object element) {
				return !isDeleteRefSpec(element);
			}

			@Override
			public String getToolTipText(Object element) {
				if (!isEnabled(element))
					return UIText.RefSpecPanel_forceDeleteDescription;
				if (isChecked(element))
					return UIText.RefSpecPanel_forceTrueDescription + '\n'
							+ UIText.RefSpecPanel_clickToChange;
				return UIText.RefSpecPanel_forceFalseDescription + '\n'
						+ UIText.RefSpecPanel_clickToChange;
			}
		});
		column.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(final Object element) {
				return !isDeleteRefSpec(element);
			}

			@Override
			protected CellEditor getCellEditor(final Object element) {
				return forceUpdateCellEditor;
			}

			@Override
			protected Object getValue(final Object element) {
				return ((RefSpec) element).isForceUpdate();
			}

			@Override
			protected void setValue(final Object element, final Object value) {
				final RefSpec oldSpec = (RefSpec) element;
				final RefSpec newSpec = oldSpec.setForceUpdate((Boolean) value);
				setRefSpec(oldSpec, newSpec);
			}
		});
	}

	private void createRemoveColumn(TableColumnLayout columnLayout) {
		final TableViewerColumn column = createColumn(columnLayout,
				UIText.RefSpecPanel_columnRemove, COLUMN_REMOVE_WEIGHT,
				SWT.CENTER);
		column.setLabelProvider(new CenteredImageLabelProvider() {
			@Override
			public Image getImage(Object element) {
				return imageRegistry.get(IMAGE_TRASH);
			}

			@Override
			public String getToolTipText(Object element) {
				return NLS.bind(UIText.RefSpecPanel_removeDescription,
						typeString());
			}
		});
		column.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return removeSpecCellEditor;
			}

			@Override
			protected Object getValue(Object element) {
				return null;
			}

			@Override
			protected void setValue(Object element, Object value) {
				removeRefSpec((RefSpec) element);
			}
		});
	}

	private TableViewerColumn createColumn(
			final TableColumnLayout columnLayout, final String text,
			final int weight, final int style) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, style);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(text);
		columnLayout.setColumnData(column, new ColumnWeightData(weight));
		return viewerColumn;
	}

	private void createCellEditors(final Table table) {
		if (pushSpecs)
			modeCellEditor = new CheckboxCellEditor(table);
		localRefCellEditor = createLocalRefCellEditor(table);
		remoteRefCellEditor = createRemoteRefCellEditor(table);
		forceUpdateCellEditor = new CheckboxCellEditor(table);
		removeSpecCellEditor = new ClickableCellEditor(table);
	}

	private CellEditor createLocalRefCellEditor(final Table table) {
		return createRefCellEditor(table, getRefsProposalProvider(true));
	}

	private CellEditor createRemoteRefCellEditor(final Table table) {
		return createRefCellEditor(table, getRefsProposalProvider(false));
	}

	private CellEditor createRefCellEditor(final Table table,
			final IContentProposalProvider proposalProvider) {
		final CellEditor cellEditor = new TextCellEditor(table);

		final Text text = (Text) cellEditor.getControl();
		final ContentAssistCommandAdapter assist = new ContentAssistCommandAdapter(
				text, new TextContentAdapter(), proposalProvider, null, null,
				true);
		assist
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		return cellEditor;
	}

	private void createSpecsButtonsPanel(final Composite parent) {
		final Composite specsPanel = new Composite(parent, SWT.NONE);
		specsPanel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true,
				false));
		final RowLayout layout = new RowLayout();
		layout.spacing = 10;
		specsPanel.setLayout(layout);

		forceUpdateAllButton = new Button(specsPanel, SWT.PUSH);
		forceUpdateAllButton.setText(UIText.RefSpecPanel_forceAll);
		forceUpdateAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final List<RefSpec> specsCopy = new ArrayList<RefSpec>(specs);
				for (final RefSpec spec : specsCopy) {
					if (!isDeleteRefSpec(spec))
						setRefSpec(spec, spec.setForceUpdate(true));
				}
			}
		});
		forceUpdateAllButton
				.setToolTipText(UIText.RefSpecPanel_forceAllDescription);
		updateForceUpdateAllButton();

		removeAllSpecButton = new Button(specsPanel, SWT.PUSH);
		removeAllSpecButton.setImage(imageRegistry.get(IMAGE_CLEAR));
		removeAllSpecButton.setText(UIText.RefSpecPanel_removeAll);
		removeAllSpecButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clearRefSpecs();
			}
		});
		removeAllSpecButton
				.setToolTipText(UIText.RefSpecPanel_removeAllDescription);
		updateRemoveAllSpecButton();

		addRefSpecTableListener(new SelectionChangeListener() {
			public void selectionChanged() {
				updateForceUpdateAllButton();
				updateRemoveAllSpecButton();
			}
		});
	}

	private void tryAutoCompleteSrcToDst() {
		final String src = creationSrcCombo.getText();
		final String dst = creationDstCombo.getText();

		if (src == null || src.length() == 0)
			return;

		if (dst != null && dst.length() > 0) {
			// dst is already there, just fix wildcards if needed
			final String newDst;
			if (RefSpec.isWildcard(src))
				newDst = wildcardSpecComponent(dst);
			else
				newDst = unwildcardSpecComponent(dst, src);
			creationDstCombo.setText(newDst);
			return;
		}

		if (!isValidRefExpression(src)) {
			// no way to be smarter than user here
			return;
		}

		// dst is empty, src is ref or wildcard, so we can rewrite it as user
		// would perhaps
		if (pushSpecs)
			creationDstCombo.setText(src);
		else {
			for (final RefSpec spec : predefinedConfigured) {
				if (spec.matchSource(src)) {
					final String newDst = spec.expandFromSource(src)
							.getDestination();
					creationDstCombo.setText(newDst);
					return;
				}
			}
			if (remoteName != null && src.startsWith(Constants.R_HEADS)) {
				final String newDst = Constants.R_REMOTES + remoteName + '/'
						+ src.substring(Constants.R_HEADS.length());
				creationDstCombo.setText(newDst);
			}
		}
	}

	private void tryAutoCompleteDstToSrc() {
		final String src = creationSrcCombo.getText();
		final String dst = creationDstCombo.getText();

		if (dst == null || dst.length() == 0)
			return;

		if (src != null && src.length() > 0) {
			// src is already there, fix wildcards if needed
			final String newSrc;
			if (RefSpec.isWildcard(dst))
				newSrc = wildcardSpecComponent(src);
			else
				newSrc = unwildcardSpecComponent(src, dst);
			creationSrcCombo.setText(newSrc);
			return;
		}
	}

	private void validateCreationPanel() {
		final String src = creationSrcCombo.getText();
		final String dst = creationDstCombo.getText();

		// check src ref field
		boolean srcOk = false;
		final boolean srcWildcard = RefSpec.isWildcard(src);
		if (src == null || src.length() == 0)
			setControlDecoration(creationSrcDecoration,
					FieldDecorationRegistry.DEC_REQUIRED,
					UIText.RefSpecPanel_validationSrcUpdateRequired);
		else if (pushSpecs) {
			if (!srcWildcard && !isLocalRef(src))
				setControlDecoration(creationSrcDecoration,
						FieldDecorationRegistry.DEC_ERROR, NLS.bind(
								UIText.RefSpecPanel_validationRefInvalidLocal,
								src));
			else if (srcWildcard && !isValidRefExpression(src))
				setControlDecoration(
						creationSrcDecoration,
						FieldDecorationRegistry.DEC_ERROR,
						NLS
								.bind(
										UIText.RefSpecPanel_validationRefInvalidExpression,
										src));
			else {
				srcOk = true;
				if (srcWildcard && !isMatchingAny(src, localRefNames))
					setControlDecoration(
							creationSrcDecoration,
							FieldDecorationRegistry.DEC_WARNING,
							NLS
									.bind(
											UIText.RefSpecPanel_validationRefNonMatchingLocal,
											src));
				else
					creationSrcDecoration.hide();
			}
		} else {
			if (!srcWildcard && !isRemoteRef(src))
				setControlDecoration(
						creationSrcDecoration,
						FieldDecorationRegistry.DEC_ERROR,
						NLS
								.bind(
										UIText.RefSpecPanel_validationRefNonExistingRemote,
										src));
			else if (srcWildcard && !isMatchingAny(src, remoteRefNames)) {
				setControlDecoration(
						creationSrcDecoration,
						FieldDecorationRegistry.DEC_WARNING,
						NLS
								.bind(
										UIText.RefSpecPanel_validationRefNonMatchingRemote,
										src));
				srcOk = true;
			} else {
				srcOk = true;
				creationSrcDecoration.hide();
			}
		}

		// check dst ref field
		boolean dstOk = false;
		if (dst == null || dst.length() == 0)
			setControlDecoration(creationDstDecoration,
					FieldDecorationRegistry.DEC_REQUIRED,
					UIText.RefSpecPanel_validationDstRequired);
		else if (!isValidRefExpression(dst))
			setControlDecoration(creationDstDecoration,
					FieldDecorationRegistry.DEC_ERROR, NLS.bind(
							UIText.RefSpecPanel_validationDstInvalidExpression,
							dst));
		else {
			creationDstDecoration.hide();
			dstOk = true;
		}
		// leave duplicates dst checking for validateSpecs()

		// check the wildcard synergy
		boolean wildcardOk = true;
		if (srcOk && dstOk && (srcWildcard ^ RefSpec.isWildcard(dst))) {
			setControlDecoration(creationSrcDecoration,
					FieldDecorationRegistry.DEC_ERROR,
					UIText.RefSpecPanel_validationWildcardInconsistent);
			setControlDecoration(creationDstDecoration,
					FieldDecorationRegistry.DEC_ERROR,
					UIText.RefSpecPanel_validationWildcardInconsistent);
			wildcardOk = false;
		}

		creationButton.setEnabled(srcOk && dstOk && wildcardOk);
	}

	private void validateDeleteCreationPanel() {
		final String ref = deleteRefCombo.getText();

		deleteButton.setEnabled(false);
		if (ref == null || ref.length() == 0)
			setControlDecoration(deleteRefDecoration,
					FieldDecorationRegistry.DEC_REQUIRED,
					UIText.RefSpecPanel_validationRefDeleteRequired);
		else if (!isValidRefExpression(ref))
			setControlDecoration(deleteRefDecoration,
					FieldDecorationRegistry.DEC_ERROR, NLS.bind(
							UIText.RefSpecPanel_validationRefInvalidExpression,
							ref));
		else if (RefSpec.isWildcard(ref))
			setControlDecoration(deleteRefDecoration,
					FieldDecorationRegistry.DEC_ERROR,
					UIText.RefSpecPanel_validationRefDeleteWildcard);
		else if (!isRemoteRef(ref))
			setControlDecoration(
					deleteRefDecoration,
					FieldDecorationRegistry.DEC_ERROR,
					NLS
							.bind(
									UIText.RefSpecPanel_validationRefNonExistingRemoteDelete,
									ref));
		else {
			deleteRefDecoration.hide();
			deleteButton.setEnabled(true);
		}
	}

	private void validateSpecs() {
		// validate spec; display max. 1 error message for user at time
		final RefSpec oldInvalidSpec = invalidSpec;
		final RefSpec oldInvalidSpecSameDst = invalidSpecSameDst;
		errorMessage = null;
		invalidSpec = null;
		invalidSpecSameDst = null;
		for (final RefSpec spec : specs) {
			errorMessage = validateSpec(spec);
			if (errorMessage != null) {
				invalidSpec = spec;
				break;
			}
		}
		if (errorMessage == null)
			validateSpecsCrossDst();
		if (invalidSpec != oldInvalidSpec
				|| invalidSpecSameDst != oldInvalidSpecSameDst)
			tableViewer.refresh();
	}

	private String validateSpec(final RefSpec spec) {
		final String src = spec.getSource();
		final String dst = spec.getDestination();
		final boolean wildcard = spec.isWildcard();

		// check src
		if (pushSpecs) {
			if (!isDeleteRefSpec(spec)) {
				if (src.length() == 0)
					return UIText.RefSpecPanel_validationSrcUpdateRequired;
				else if (!wildcard && !isLocalRef(src))
					return NLS.bind(
							UIText.RefSpecPanel_validationRefInvalidLocal, src);
				else if (wildcard && !isValidRefExpression(src))
					return NLS.bind(
							UIText.RefSpecPanel_validationRefInvalidExpression,
							src);
				// ignore non-matching wildcard specs
			}
		} else {
			if (src == null || src.length() == 0)
				return UIText.RefSpecPanel_validationSrcUpdateRequired;
			else if (!wildcard && !isRemoteRef(src))
				return NLS
						.bind(
								UIText.RefSpecPanel_validationRefNonExistingRemote,
								src);
			// ignore non-matching wildcard specs
		}

		// check dst
		if (dst == null || dst.length() == 0) {
			if (isDeleteRefSpec(spec))
				return UIText.RefSpecPanel_validationRefDeleteRequired;
			return UIText.RefSpecPanel_validationDstRequired;
		} else if (!isValidRefExpression(dst))
			return NLS.bind(UIText.RefSpecPanel_validationRefInvalidExpression,
					dst);
		else if (isDeleteRefSpec(spec) && !isRemoteRef(dst))
			return NLS.bind(
					UIText.RefSpecPanel_validationRefNonExistingRemoteDelete,
					dst);

		return null;
	}

	private boolean isInvalidSpec(Object element) {
		return element == invalidSpec || element == invalidSpecSameDst;
	}

	private void validateSpecsCrossDst() {
		final Map<String, RefSpec> dstsSpecsMap = new HashMap<String, RefSpec>();
		try {
			for (final RefSpec spec : specs) {
				if (!spec.isWildcard()) {
					if (!tryAddDestination(dstsSpecsMap, spec.getDestination(),
							spec))
						return;
				} else {
					final Collection<String> srcNames;
					if (pushSpecs)
						srcNames = localRefNames;
					else
						srcNames = remoteRefNames;

					for (final String src : srcNames) {
						if (spec.matchSource(src)) {
							final String dst = spec.expandFromSource(src)
									.getDestination();
							if (!tryAddDestination(dstsSpecsMap, dst, spec))
								return;
						}
					}
				}
			}
		} finally {
			matchingAnyRefs = !dstsSpecsMap.isEmpty();
		}
	}

	private boolean tryAddDestination(final Map<String, RefSpec> dstsSpecsMap,
			final String dst, final RefSpec spec) {
		final RefSpec other = dstsSpecsMap.put(dst, spec);
		if (other != null) {
			errorMessage = NLS
					.bind(
							UIText.RefSpecPanel_validationSpecificationsOverlappingDestination,
							dst);
			invalidSpec = other;
			invalidSpecSameDst = spec;
			return false;
		}
		return true;
	}

	private void updateAddPredefinedButton(final Button button,
			final List<RefSpec> predefined) {
		boolean enable = false;
		for (final RefSpec pre : predefined) {
			if (!specs.contains(pre)) {
				enable = true;
				break;
			}
		}
		button.setEnabled(enable);
	}

	private void updateAddPredefinedButton(Button button,
			final RefSpec predefined) {
		button.setEnabled(!specs.contains(predefined));
	}

	private void updateForceUpdateAllButton() {
		boolean enable = false;
		for (final RefSpec spec : specs) {
			if (!isDeleteRefSpec(spec) && !spec.isForceUpdate()) {
				enable = true;
				break;
			}
		}
		forceUpdateAllButton.setEnabled(enable);
	}

	private void updateRemoveAllSpecButton() {
		removeAllSpecButton.setEnabled(!specs.isEmpty());
	}

	private String typeString() {
		return (pushSpecs ? UIText.RefSpecPanel_push
				: UIText.RefSpecPanel_fetch);
	}

	private void addPredefinedRefSpecs(final RefSpec predefined) {
		addPredefinedRefSpecs(Collections.singletonList(predefined));
	}

	private void addPredefinedRefSpecs(final List<RefSpec> predefined) {
		for (final RefSpec pre : predefined) {
			if (!specs.contains(pre))
				addRefSpec(pre);
		}
	}

	private List<RefContentProposal> createContentProposals(
			final Collection<Ref> refs, final Ref HEAD) {
		final TreeSet<Ref> set = new TreeSet<Ref>(new Comparator<Ref>() {
			public int compare(Ref o1, Ref o2) {
				// lexicographical ordering by name seems to be fine
				return o1.getName().compareTo(o2.getName());
			}
		});
		set.addAll(refs);
		if (HEAD != null)
			set.add(HEAD);

		final List<RefContentProposal> result = new ArrayList<RefContentProposal>(
				set.size());
		for (final Ref r : set)
			result.add(new RefContentProposal(localDb, r));
		return result;
	}

	private List<RefContentProposal> createProposalsFilteredLocal(
			final List<RefContentProposal> proposals) {
		final List<RefContentProposal> result = new ArrayList<RefContentProposal>();
		for (final RefContentProposal p : proposals) {
			final String content = p.getContent();
			if (pushSpecs) {
				if (content.equals(Constants.HEAD)
						|| content.startsWith(Constants.R_HEADS))
					result.add(p);
			} else {
				if (content.startsWith(Constants.R_REMOTES))
					result.add(p);
			}
		}
		return result;
	}

	private boolean isRemoteRef(String ref) {
		return remoteRefNames.contains(ref);
	}

	private boolean isLocalRef(final String ref) {
		return tryResolveLocalRef(ref) != null;
	}

	private boolean isMatchingAny(final String ref,
			final Collection<String> names) {
		// strip wildcard sign
		final String prefix = ref.substring(0, ref.length() - 1);
		for (final String name : names)
			if (name.startsWith(prefix))
				return true;
		return false;
	}

	private ObjectId tryResolveLocalRef(final String ref) {
		try {
			return localDb.resolve(ref);
		} catch (final IOException e) {
			Activator.logError(
					"I/O error occurred during resolving expression: " //$NON-NLS-1$
							+ ref, e);
			return null;
		}
	}

	private class RefContentProposalProvider implements
			IContentProposalProvider {
		private List<RefContentProposal> proposals = Collections.emptyList();

		private final boolean tryResolvingLocally;

		private RefContentProposalProvider(final boolean tryResolvingLocally) {
			this.tryResolvingLocally = tryResolvingLocally;
		}

		private void setProposals(final List<RefContentProposal> proposals) {
			this.proposals = proposals;
		}

		public IContentProposal[] getProposals(final String contents,
				int position) {
			final List<RefContentProposal> result = new ArrayList<RefContentProposal>();

			if (contents.indexOf('*') != -1 || contents.indexOf('?') != -1) {
				// contents contains wildcards

				// check if contents can be safely added as wildcard spec
				if (isValidRefExpression(contents))
					result.add(new RefContentProposal(localDb, contents, null));

				// let's expand wildcards
				final String regex = ".*" //$NON-NLS-1$
						+ contents.replace("*", ".*").replace("?", ".?") + ".*"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				final Pattern p = Pattern.compile(regex);
				for (final RefContentProposal prop : proposals)
					if (p.matcher(prop.getContent()).matches())
						result.add(prop);
			} else {
				for (final RefContentProposal prop : proposals)
					if (prop.getContent().contains(contents))
						result.add(prop);

				if (tryResolvingLocally && result.isEmpty()) {
					final ObjectId id = tryResolveLocalRef(contents);
					if (id != null)
						result
								.add(new RefContentProposal(localDb, contents,
										id));
				}
			}
			return result.toArray(new IContentProposal[0]);
		}
	}
}
