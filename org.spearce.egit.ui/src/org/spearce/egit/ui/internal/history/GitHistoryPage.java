/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.spearce.egit.core.ResourceList;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Repository;
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
public class GitHistoryPage extends HistoryPage {
	private static final String PREF_COMMENT_WRAP = UIPreferences.RESOURCEHISTORY_SHOW_COMMENT_WRAP;

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

	/** The table showing the DAG, first "paragraph", author, author date. */
	private CommitGraphTable graph;

	/** Viewer displaying the currently selected commit of {@link #graph}. */
	private CommitMessageViewer commentViewer;

	/** Viewer displaying file difference implied by {@link #graph}'s commit. */
	private CommitFileDiffViewer fileViewer;

	/** Job that is updating our history view, if we are refreshing. */
	private GenerateHistoryJob job;

	/** Revision walker that allocated our graph's commit nodes. */
	private SWTWalk currentWalk;

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

	@Override
	public void createControl(final Composite parent) {
		GridData gd;
		final SashForm graphDetailSplit;
		final SashForm revInfoSplit;

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

		attachCommitSelectionChanged();
		createViewMenu();
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
			}
		});
		commentViewer
				.addCommitNavigationListener(new CommitNavigationListener() {
					public void showCommit(final RevCommit c) {
						graph.selectCommit(c);
					}
				});
	}

	private void createViewMenu() {
		final IActionBars actionBars = getSite().getActionBars();
		final IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(createCommentWrap());
	}

	private IAction createCommentWrap() {
		final IAction r = new Action(UIText.ResourceHistory_toggleCommentWrap) {
			public void run() {
				final boolean wrap = isChecked();
				commentViewer.getTextWidget().setWordWrap(wrap);
				prefs.setValue(PREF_COMMENT_WRAP, wrap);
			}
		};
		final boolean wrap = prefs.getBoolean(PREF_COMMENT_WRAP);
		r.setChecked(wrap);
		commentViewer.getTextWidget().setWordWrap(wrap);
		return r;
	}

	public void dispose() {
		cancelRefreshJob();
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
		cancelRefreshJob();

		if (graph == null)
			return false;

		final IResource[] in = ((ResourceList) super.getInput()).getItems();
		if (in == null || in.length == 0)
			return false;

		Repository db = null;
		final ArrayList<String> paths = new ArrayList<String>(in.length);
		for (final IResource r : in) {
			final RepositoryMapping map = RepositoryMapping.getMapping(r);
			if (map == null)
				continue;

			if (db == null)
				db = map.getRepository();
			else if (db != map.getRepository())
				return false;

			final String name = map.getRepoRelativePath(r);
			if (name != null && name.length() > 0)
				paths.add(name);
		}

		if (db == null)
			return false;

		if (currentWalk == null || currentWalk.getRepository() != db
				|| pathChange(pathFilters, paths)) {
			currentWalk = new SWTWalk(db);
			currentWalk.sort(RevSort.COMMIT_TIME_DESC, true);
			highlightFlag = currentWalk.newFlag("highlight");
		} else {
			currentWalk.reset();
		}

		try {
			final AnyObjectId headId = db.resolve("HEAD");
			if (headId == null)
				return false;
			currentWalk.markStart(currentWalk.parseCommit(headId));
		} catch (IOException e) {
			Activator.logError("Cannot parse HEAD in: "
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
		graph.setInput(highlightFlag, null, null);

		final SWTCommitList list;
		list = new SWTCommitList(graph.getControl().getDisplay());
		list.source(currentWalk);

		final GenerateHistoryJob rj = new GenerateHistoryJob(this, list);
		rj.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
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
				if (!graph.getControl().isDisposed() && job == j)
					graph.setInput(highlightFlag, list, asArray);
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
}
