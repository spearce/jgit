/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.spearce.egit.core.ResourceList;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.revplot.PlotCommit;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevFlag;
import org.spearce.jgit.revwalk.RevSort;
import org.spearce.jgit.revwalk.filter.RevFilter;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.filter.AndTreeFilter;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;
import org.spearce.jgit.treewalk.filter.TreeFilter;

/** Graphical commit history viewer. */
public class GitHistoryPage extends HistoryPage implements RepositoryListener {
	private static final String PREF_COMMENT_WRAP = UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP;

	private static final String PREF_COMMENT_FILL = UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_FILL;

	private static final String SHOW_COMMENT = UIPreferences.RESOURCEHISTORY_SHOW_REV_COMMENT;

	private static final String SHOW_FILES = UIPreferences.RESOURCEHISTORY_SHOW_REV_DETAIL;

	private static final String SPLIT_GRAPH = UIPreferences.RESOURCEHISTORY_GRAPH_SPLIT;

	private static final String SPLIT_INFO = UIPreferences.RESOURCEHISTORY_REV_SPLIT;

	private static final String SHOW_FIND_TOOLBAR = UIPreferences.RESOURCEHISTORY_SHOW_FINDTOOLBAR;

	private static final String POPUP_ID = "org.spearce.egit.ui.historyPageContributions";

	/**
	 * Determine if the input can be shown in this viewer.
	 * 
	 * @param object
	 *            an object that is hopefully of type ResourceList or IResource,
	 *            but may be anything (including null).
	 * @return true if the input is a ResourceList or an IResource of type FILE,
	 *         FOLDER or PROJECT and we can show it; false otherwise.
	 */
	public static boolean canShowHistoryFor(final Object object) {
		if (object instanceof ResourceList) {
			final IResource[] array = ((ResourceList) object).getItems();
			if (array.length == 0)
				return false;
			for (final IResource r : array) {
				if (!typeOk(r))
					return false;
			}
			return true;

		}

		if (object instanceof IResource) {
			return typeOk((IResource) object);
		}

		return false;
	}

	private static boolean typeOk(final IResource object) {
		switch (object.getType()) {
		case IResource.FILE:
		case IResource.FOLDER:
		case IResource.PROJECT:
			return true;
		}
		return false;
	}

	/** Plugin private preference store for the current workspace. */
	private Preferences prefs;

	/** Overall composite hosting all of our controls. */
	private Composite ourControl;

	/** Split between {@link #graph} and {@link #revInfoSplit}. */
	private SashForm graphDetailSplit;

	/** Split between {@link #commentViewer} and {@link #fileViewer}. */
	private SashForm revInfoSplit;

	/** The table showing the DAG, first "paragraph", author, author date. */
	private CommitGraphTable graph;

	/** Viewer displaying the currently selected commit of {@link #graph}. */
	private CommitMessageViewer commentViewer;

	/** Viewer displaying file difference implied by {@link #graph}'s commit. */
	private CommitFileDiffViewer fileViewer;

	/** Toolbar to find commits in the history view. */
	private FindToolbar findToolbar;

	/** Our context menu manager for the entire page. */
	private MenuManager popupMgr;

	/** Job that is updating our history view, if we are refreshing. */
	private GenerateHistoryJob job;

	/** Revision walker that allocated our graph's commit nodes. */
	private SWTWalk currentWalk;

	/** Last HEAD */
	private AnyObjectId currentHeadId;

	/** We need to remember the current repository */
	private Repository db;

	/**
	 * Highlight flag that can be applied to commits to make them stand out.
	 * <p>
	 * Allocated at the same time as {@link #currentWalk}. If the walk
	 * rebuilds, so must this flag.
	 */
	private RevFlag highlightFlag;

	/**
	 * List of paths we used to limit {@link #currentWalk}; null if no paths.
	 * <p>
	 * Note that a change in this list requires that {@link #currentWalk} and
	 * all of its associated commits.
	 */
	private List<String> pathFilters;

	/**
	 * The selection provider tracks the selected revisions for the context menu
	 */
	private RevObjectSelectionProvider revObjectSelectionProvider;

	private static final String PREF_SHOWALLFILTER = "org.spearce.egit.ui.githistorypage.showallfilter"; //$NON-NLS-1$

	enum ShowFilter {
		SHOWALLRESOURCE,
		SHOWALLFOLDER,
		SHOWALLPROJECT,
		SHOWALLREPO,
	}

	class ShowFilterAction extends Action {
		private final ShowFilter filter;

		ShowFilterAction(ShowFilter filter, ImageDescriptor icon, String toolTipText) {
			super(null, IAction.AS_CHECK_BOX);
			this.filter = filter;
			setImageDescriptor(icon);
			setToolTipText(toolTipText);
		}
		@Override
		public void run() {
			if (!isChecked()) {
				if (showAllFilter == filter) {
					showAllFilter = ShowFilter.SHOWALLRESOURCE;
					refresh();
				}
			}
			if (isChecked() && showAllFilter != filter) {
				showAllFilter = filter;
				if (this != showAllRepoVersionsAction)
					showAllRepoVersionsAction.setChecked(false);
				if (this != showAllProjectVersionsAction)
					showAllProjectVersionsAction.setChecked(false);
				if (this != showAllFolderVersionsAction)
					showAllFolderVersionsAction.setChecked(false);
				refresh();
			}
			Activator.getDefault().getPreferenceStore().setValue(
					PREF_SHOWALLFILTER, showAllFilter.toString());
		}
		@Override
		public String toString() {
			return "ShowFilter[" + filter.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private ShowFilter showAllFilter = ShowFilter.SHOWALLRESOURCE;

	private ShowFilterAction showAllRepoVersionsAction;

	private ShowFilterAction showAllProjectVersionsAction;

	private ShowFilterAction showAllFolderVersionsAction;

	private void createResourceFilterActions() {
		try {
			showAllFilter = ShowFilter.valueOf(Activator.getDefault()
					.getPreferenceStore().getString(PREF_SHOWALLFILTER));
		} catch (IllegalArgumentException e) {
			showAllFilter = ShowFilter.SHOWALLRESOURCE;
		}

		showAllRepoVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLREPO, UIIcons.FILTERREPO,
				UIText.HistoryPage_ShowAllVersionsForRepo);

		showAllProjectVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLPROJECT, UIIcons.FILTERPROJECT,
				UIText.HistoryPage_ShowAllVersionsForProject);

		showAllFolderVersionsAction = new ShowFilterAction(
				ShowFilter.SHOWALLFOLDER, UIIcons.FILTERFOLDER,
				UIText.HistoryPage_ShowAllVersionsForFolder);

		showAllRepoVersionsAction
				.setChecked(showAllFilter == showAllRepoVersionsAction.filter);
		showAllProjectVersionsAction
				.setChecked(showAllFilter == showAllProjectVersionsAction.filter);
		showAllFolderVersionsAction
				.setChecked(showAllFilter == showAllFolderVersionsAction.filter);

		getSite().getActionBars().getToolBarManager().add(new Separator());

		getSite().getActionBars().getToolBarManager().add(
				showAllRepoVersionsAction);

		getSite().getActionBars().getToolBarManager().add(
				showAllProjectVersionsAction);

		getSite().getActionBars().getToolBarManager().add(
				showAllFolderVersionsAction);
	}

	@Override
	public void createControl(final Composite parent) {
		GridData gd;

		prefs = Activator.getDefault().getPluginPreferences();
		ourControl = createMainPanel(parent);
		gd = new GridData();
		gd.verticalAlignment = SWT.FILL;
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		ourControl.setLayoutData(gd);

		gd = new GridData();
		gd.verticalAlignment = SWT.FILL;
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		graphDetailSplit = new SashForm(ourControl, SWT.VERTICAL);
		graphDetailSplit.setLayoutData(gd);

		graph = new CommitGraphTable(graphDetailSplit);
		revInfoSplit = new SashForm(graphDetailSplit, SWT.HORIZONTAL);
		commentViewer = new CommitMessageViewer(revInfoSplit);
		fileViewer = new CommitFileDiffViewer(revInfoSplit);
		findToolbar = new FindToolbar(ourControl);

		layoutSashForm(graphDetailSplit, SPLIT_GRAPH);
		layoutSashForm(revInfoSplit, SPLIT_INFO);

		revObjectSelectionProvider = new RevObjectSelectionProvider();
		popupMgr = new MenuManager(null, POPUP_ID);
		attachCommitSelectionChanged();
		createLocalToolbarActions();
		createResourceFilterActions();
		createStandardActions();
		createViewMenu();

		finishContextMenu();
		attachContextMenu(graph.getControl());
		attachContextMenu(commentViewer.getControl());
		attachContextMenu(fileViewer.getControl());
		layout();

		Repository.addAnyRepositoryChangedListener(this);
	}

	private Runnable refschangedRunnable;

	public void refsChanged(final RefsChangedEvent e) {
		if (e.getRepository() != db)
			return;

		if (getControl().isDisposed())
			return;

		synchronized (this) {
			if (refschangedRunnable == null) {
				refschangedRunnable = new Runnable() {
					public void run() {
						if (!getControl().isDisposed()) {
							Activator.trace("Executing async repository changed event");
							refschangedRunnable = null;
							inputSet();
						}
					}
				};
				getControl().getDisplay().asyncExec(refschangedRunnable);
			}
		}
	}

	public void indexChanged(final IndexChangedEvent e) {
		// We do not use index information here now
	}

	private void finishContextMenu() {
		popupMgr.add(new Separator());
		popupMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		getSite().registerContextMenu(POPUP_ID, popupMgr,
				revObjectSelectionProvider);
		getHistoryPageSite().getPart().getSite().setSelectionProvider(
				revObjectSelectionProvider);
	}

	private void attachContextMenu(final Control c) {
		c.setMenu(popupMgr.createContextMenu(c));
	}

	private void layoutSashForm(final SashForm sf, final String key) {
		sf.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				final int[] w = sf.getWeights();
				UIPreferences.setValue(prefs, key, w);
			}
		});
		sf.setWeights(UIPreferences.getIntArray(prefs, key, 2));
	}

	private Composite createMainPanel(final Composite parent) {
		final Composite c = new Composite(parent, SWT.NULL);
		final GridLayout parentLayout = new GridLayout();
		parentLayout.marginHeight = 0;
		parentLayout.marginWidth = 0;
		parentLayout.verticalSpacing = 0;
		c.setLayout(parentLayout);
		return c;
	}

	private void layout() {
		final boolean showComment = prefs.getBoolean(SHOW_COMMENT);
		final boolean showFiles = prefs.getBoolean(SHOW_FILES);
		final boolean showFindToolbar = prefs.getBoolean(SHOW_FIND_TOOLBAR);

		if (showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(null);
		} else if (showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(commentViewer.getControl());
		} else if (!showComment && showFiles) {
			graphDetailSplit.setMaximizedControl(null);
			revInfoSplit.setMaximizedControl(fileViewer.getControl());
		} else if (!showComment && !showFiles) {
			graphDetailSplit.setMaximizedControl(graph.getControl());
		}
		if (showFindToolbar) {
			((GridData) findToolbar.getLayoutData()).heightHint = SWT.DEFAULT;
		} else {
			((GridData) findToolbar.getLayoutData()).heightHint = 0;
			findToolbar.clear();
		}
		ourControl.layout();
	}

	private void attachCommitSelectionChanged() {
		graph.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final ISelection s = event.getSelection();
				if (s.isEmpty() || !(s instanceof IStructuredSelection)) {
					commentViewer.setInput(null);
					fileViewer.setInput(null);
					return;
				}

				final IStructuredSelection sel;
				final PlotCommit<?> c;

				sel = ((IStructuredSelection) s);
				c = (PlotCommit<?>) sel.getFirstElement();
				commentViewer.setInput(c);
				fileViewer.setInput(c);
				revObjectSelectionProvider.setSelection(s);
			}
		});
		commentViewer
				.addCommitNavigationListener(new CommitNavigationListener() {
					public void showCommit(final RevCommit c) {
						graph.selectCommit(c);
					}
				});
		findToolbar.addSelectionListener(new Listener() {
			public void handleEvent(Event event) {
				graph.selectCommit((RevCommit) event.data);
			}
		});
	}

	private void createLocalToolbarActions() {
		final IToolBarManager barManager = getSite().getActionBars()
				.getToolBarManager();
		IAction a;

		a = createFindToolbarAction();
		barManager.add(a);
	}

	private IAction createFindToolbarAction() {
		final IAction r = new Action("Fi", UIIcons.ELCL16_FIND) {
			public void run() {
				prefs.setValue(SHOW_FIND_TOOLBAR, isChecked());
				layout();
			}
		};
		r.setChecked(prefs.getBoolean(SHOW_FIND_TOOLBAR));
		r.setToolTipText(UIText.HistoryPage_findbar_findTooltip);
		return r;
	}

	private void createViewMenu() {
		final IActionBars actionBars = getSite().getActionBars();
		final IMenuManager menuManager = actionBars.getMenuManager();
		IAction a;

		a = createCommentWrap();
		menuManager.add(a);
		popupMgr.add(a);

		a = createCommentFill();
		menuManager.add(a);
		popupMgr.add(a);

		menuManager.add(new Separator());
		popupMgr.add(new Separator());

		a = createShowComment();
		menuManager.add(a);
		popupMgr.add(a);

		a = createShowFiles();
		menuManager.add(a);
		popupMgr.add(a);

		menuManager.add(new Separator());
		popupMgr.add(new Separator());
	}

	private IAction createCommentWrap() {
		final BooleanPrefAction a = new BooleanPrefAction(PREF_COMMENT_WRAP,
				UIText.ResourceHistory_toggleCommentWrap) {
			void apply(boolean wrap) {
				commentViewer.setWrap(wrap);
			}
		};
		a.apply(a.isChecked());
		return a;
	}

	private IAction createCommentFill() {
		final BooleanPrefAction a = new BooleanPrefAction(PREF_COMMENT_FILL,
				UIText.ResourceHistory_toggleCommentFill) {
			void apply(boolean fill) {
				commentViewer.setFill(fill);
			}
		};
		a.apply(a.isChecked());
		return a;
	}

	private IAction createShowComment() {
		return new BooleanPrefAction(SHOW_COMMENT,
				UIText.ResourceHistory_toggleRevComment) {
			void apply(final boolean value) {
				layout();
			}
		};
	}

	private IAction createShowFiles() {
		return new BooleanPrefAction(SHOW_FILES,
				UIText.ResourceHistory_toggleRevDetail) {
			void apply(final boolean value) {
				layout();
			}
		};
	}

	private void createStandardActions() {
		final TextAction copy = new TextAction(ITextOperationTarget.COPY);
		final TextAction sAll = new TextAction(ITextOperationTarget.SELECT_ALL);

		graph.getControl().addFocusListener(copy);
		graph.getControl().addFocusListener(sAll);
		graph.addSelectionChangedListener(copy);
		graph.addSelectionChangedListener(sAll);

		commentViewer.getControl().addFocusListener(copy);
		commentViewer.getControl().addFocusListener(sAll);
		commentViewer.addSelectionChangedListener(copy);
		commentViewer.addSelectionChangedListener(sAll);

		fileViewer.getControl().addFocusListener(copy);
		fileViewer.getControl().addFocusListener(sAll);
		fileViewer.addSelectionChangedListener(copy);
		fileViewer.addSelectionChangedListener(sAll);

		final IActionBars b = getSite().getActionBars();
		b.setGlobalActionHandler(ActionFactory.COPY.getId(), copy);
		b.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), sAll);

		popupMgr.add(createStandardAction(ActionFactory.COPY));
		popupMgr.add(createStandardAction(ActionFactory.SELECT_ALL));
		popupMgr.add(new Separator());
	}

	private IAction createStandardAction(final ActionFactory af) {
		final IPageSite s = getSite();
		final IWorkbenchAction a = af.create(s.getWorkbenchWindow());
		if (af instanceof IPartListener)
			((IPartListener) a).partActivated(s.getPage().getActivePart());
		return a;
	}

	public void dispose() {
		Repository.removeAnyRepositoryChangedListener(this);
		cancelRefreshJob();
		if (popupMgr != null) {
			for (final IContributionItem i : popupMgr.getItems()) {
				if (i instanceof ActionFactory.IWorkbenchAction)
					((ActionFactory.IWorkbenchAction) i).dispose();
			}
			for (final IContributionItem i : getSite().getActionBars()
					.getMenuManager().getItems()) {
				if (i instanceof ActionFactory.IWorkbenchAction)
					((ActionFactory.IWorkbenchAction) i).dispose();
			}
		}
		Activator.getDefault().savePluginPreferences();
		super.dispose();
	}

	public void refresh() {
		inputSet();
	}

	@Override
	public void setFocus() {
		graph.getControl().setFocus();
	}

	@Override
	public Control getControl() {
		return ourControl;
	}

	public Object getInput() {
		final ResourceList r = (ResourceList) super.getInput();
		if (r == null)
			return null;
		final IResource[] in = r.getItems();
		if (in == null || in.length == 0)
			return null;
		if (in.length == 1)
			return in[0];
		return r;
	}

	public boolean setInput(final Object o) {
		final Object in;
		if (o instanceof IResource)
			in = new ResourceList(new IResource[] { (IResource) o });
		else if (o instanceof ResourceList)
			in = o;
		else
			in = null;
		return super.setInput(in);
	}

	@Override
	public boolean inputSet() {
		if (revObjectSelectionProvider != null)
			revObjectSelectionProvider.setActiveRepository(null);
		cancelRefreshJob();

		if (graph == null)
			return false;

		final IResource[] in = ((ResourceList) super.getInput()).getItems();
		if (in == null || in.length == 0)
			return false;

		db = null;

		final ArrayList<String> paths = new ArrayList<String>(in.length);
		for (final IResource r : in) {
			final RepositoryMapping map = RepositoryMapping.getMapping(r);
			if (map == null)
				continue;

			if (db == null)
				db = map.getRepository();
			else if (db != map.getRepository())
				return false;

			if (showAllFilter == ShowFilter.SHOWALLFOLDER) {
				final String name = map.getRepoRelativePath(r.getParent());
				if (name != null && name.length() > 0)
					paths.add(name);
			} else if (showAllFilter == ShowFilter.SHOWALLPROJECT) {
				final String name = map.getRepoRelativePath(r.getProject());
				if (name != null && name.length() > 0)
					paths.add(name);
			} else if (showAllFilter == ShowFilter.SHOWALLREPO) {
				// nothing
			} else /*if (showAllFilter == ShowFilter.SHOWALLRESOURCE)*/ {
				final String name = map.getRepoRelativePath(r);
				if (name != null && name.length() > 0)
					paths.add(name);
			}
		}

		if (db == null)
			return false;

		final AnyObjectId headId;
		try {
			headId = db.resolve(Constants.HEAD);
		} catch (IOException e) {
			Activator.logError("Cannot parse HEAD in: "
					+ db.getDirectory().getAbsolutePath(), e);
			return false;
		}

		if (currentWalk == null || currentWalk.getRepository() != db
				|| pathChange(pathFilters, paths) || headId != null
				&& !headId.equals(currentHeadId)) {
			// TODO Do not dispose SWTWalk just because HEAD changed
			// In theory we should be able to update the graph and
			// not dispose of the SWTWalk, even if HEAD was reset to
			// HEAD^1 and the old HEAD commit should not be visible.
			//
			currentWalk = new SWTWalk(db);
			currentWalk.sort(RevSort.COMMIT_TIME_DESC, true);
			currentWalk.sort(RevSort.BOUNDARY, true);
			highlightFlag = currentWalk.newFlag("highlight");
		} else {
			currentWalk.reset();
		}

		if (headId == null)
			return false;
		try {
			currentWalk.markStart(currentWalk.parseCommit(headId));
		} catch (IOException e) {
			Activator.logError("Cannot read HEAD commit " + headId + " in: "
					+ db.getDirectory().getAbsolutePath(), e);
			return false;
		}

		final TreeWalk fileWalker = new TreeWalk(db);
		fileWalker.setRecursive(true);
		if (paths.size() > 0) {
			pathFilters = paths;
			currentWalk.setTreeFilter(AndTreeFilter.create(PathFilterGroup
					.createFromStrings(paths), TreeFilter.ANY_DIFF));
			fileWalker.setFilter(currentWalk.getTreeFilter().clone());

		} else {
			pathFilters = null;
			currentWalk.setTreeFilter(TreeFilter.ALL);
			fileWalker.setFilter(TreeFilter.ANY_DIFF);
		}
		fileViewer.setTreeWalk(fileWalker);
		findToolbar.clear();
		graph.setInput(highlightFlag, null, null);

		final SWTCommitList list;
		list = new SWTCommitList(graph.getControl().getDisplay());
		list.source(currentWalk);

		final GenerateHistoryJob rj = new GenerateHistoryJob(this, list);
		final Repository fdb = db;
		rj.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				revObjectSelectionProvider.setActiveRepository(fdb);
				final Control graphctl = graph.getControl();
				if (job != rj || graphctl.isDisposed())
					return;
				graphctl.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (job == rj)
							job = null;
					}
				});
			}
		});
		job = rj;
		schedule(rj);
		return true;
	}

	private void cancelRefreshJob() {
		if (job != null && job.getState() != Job.NONE) {
			job.cancel();

			// As the job had to be canceled but was working on
			// the data connected with the currentWalk we cannot
			// be sure it really finished. Since the walk is not
			// thread safe we must throw it away and build a new
			// one to start another walk. Clearing our field will
			// ensure that happens.
			//
			job = null;
			currentWalk = null;
			highlightFlag = null;
			pathFilters = null;
		}
	}

	private boolean pathChange(final List<String> o, final List<String> n) {
		if (o == null)
			return !n.isEmpty();
		return !o.equals(n);
	}

	private void schedule(final Job j) {
		final IWorkbenchPartSite site = getWorkbenchSite();
		if (site != null) {
			final IWorkbenchSiteProgressService p;
			p = (IWorkbenchSiteProgressService) site
					.getAdapter(IWorkbenchSiteProgressService.class);
			if (p != null) {
				p.schedule(j, 0, true /* use half-busy cursor */);
				return;
			}
		}
		j.schedule();
	}

	void showCommitList(final Job j, final SWTCommitList list,
			final SWTCommit[] asArray) {
		if (job != j || graph.getControl().isDisposed())
			return;

		graph.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!graph.getControl().isDisposed() && job == j) {
					graph.setInput(highlightFlag, list, asArray);
					findToolbar.setInput(highlightFlag, graph.getTable(),
							asArray);
				}
			}
		});
	}

	private IWorkbenchPartSite getWorkbenchSite() {
		final IWorkbenchPart part = getHistoryPageSite().getPart();
		return part != null ? part.getSite() : null;
	}

	public boolean isValidInput(final Object object) {
		return canShowHistoryFor(object);
	}

	public Object getAdapter(final Class adapter) {
		return null;
	}

	public String getName() {
		final ResourceList in = (ResourceList) super.getInput();
		if (currentWalk == null || in == null)
			return "";
		final IResource[] items = in.getItems();
		if (items.length == 0)
			return "";

		final StringBuilder b = new StringBuilder();
		b.append(items[0].getProject().getName());
		if (currentWalk.getRevFilter() != RevFilter.ALL) {
			b.append(": ");
			b.append(currentWalk.getRevFilter());
		}
		if (currentWalk.getTreeFilter() != TreeFilter.ALL) {
			b.append(":");
			for (final String p : pathFilters) {
				b.append(' ');
				b.append(p);
			}
		}
		return b.toString();
	}

	public String getDescription() {
		return getName();
	}

	private abstract class BooleanPrefAction extends Action implements
			IPropertyChangeListener, ActionFactory.IWorkbenchAction {
		private final String prefName;

		BooleanPrefAction(final String pn, final String text) {
			setText(text);
			prefName = pn;
			prefs.addPropertyChangeListener(this);
			setChecked(prefs.getBoolean(prefName));
		}

		public void run() {
			prefs.setValue(prefName, isChecked());
			apply(isChecked());
		}

		abstract void apply(boolean value);

		public void propertyChange(final PropertyChangeEvent event) {
			if (prefName.equals(event.getProperty())) {
				setChecked(prefs.getBoolean(prefName));
				apply(isChecked());
			}
		}

		public void dispose() {
			prefs.removePropertyChangeListener(this);
		}
	}

	private class TextAction extends Action implements FocusListener,
			ISelectionChangedListener {
		private final int op;

		TextAction(final int operationCode) {
			op = operationCode;
			setEnabled(false);
		}

		public void run() {
			if (commentViewer.getTextWidget().isFocusControl()) {
				if (commentViewer.canDoOperation(op))
					commentViewer.doOperation(op);
			} else if (fileViewer.getTable().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					fileViewer.doCopy();
					break;
				case ITextOperationTarget.SELECT_ALL:
					fileViewer.doSelectAll();
					break;
				}
			} else if (graph.getControl().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					graph.doCopy();
					break;
				}
			}
		}

		private void update() {
			if (commentViewer.getTextWidget().isFocusControl()) {
				setEnabled(commentViewer.canDoOperation(op));
			} else if (fileViewer.getTable().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					setEnabled(!fileViewer.getSelection().isEmpty());
					break;
				case ITextOperationTarget.SELECT_ALL:
					setEnabled(fileViewer.getTable().getItemCount() > 0);
					break;
				}
			} else if (graph.getControl().isFocusControl()) {
				switch (op) {
				case ITextOperationTarget.COPY:
					setEnabled(graph.canDoCopy());
					break;
				case ITextOperationTarget.SELECT_ALL:
					setEnabled(false);
					break;
				}
			}
		}

		public void focusGained(final FocusEvent e) {
			update();
		}

		public void selectionChanged(final SelectionChangedEvent event) {
			update();
		}

		public void focusLost(final FocusEvent e) {
			// Ignore lost events. If focus leaves our page then the
			// workbench will update the global action away from us.
			// If focus stays in our page someone else should have
			// gained it from us.
		}
	}
}
