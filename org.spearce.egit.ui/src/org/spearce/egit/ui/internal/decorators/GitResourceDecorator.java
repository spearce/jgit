/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IDecoratorManager;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryChangeListener;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryChangedEvent;
import org.spearce.jgit.lib.RepositoryListener;
import org.spearce.jgit.lib.RepositoryState;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * Supplies annotations for displayed resources.
 * <p>
 * This decorator provides annotations to indicate the status of each resource
 * when compared to <code>HEAD</code> as well as the index in the relevant
 * repository.
 * 
 * When either the index or the working directory is different from HEAD an
 * indicator is set.
 * 
 * </p>
 */
public class GitResourceDecorator extends LabelProvider implements
		ILightweightLabelDecorator {

	static final String decoratorId = "org.spearce.egit.ui.internal.decorators.GitResourceDecorator";
	static class ResCL extends Job implements IResourceChangeListener, RepositoryChangeListener, RepositoryListener {

		ResCL() {
			super("Git resource decorator trigger");
		}

		GitResourceDecorator getActiveDecorator() {
			IDecoratorManager decoratorManager = Activator.getDefault()
					.getWorkbench().getDecoratorManager();
			if (decoratorManager.getEnabled(decoratorId))
				return (GitResourceDecorator) decoratorManager
						.getLightweightLabelDecorator(decoratorId);
			return null;
		}

		private Set<IResource> resources = new LinkedHashSet<IResource>();

		public void refsChanged(RefsChangedEvent e) {
			repositoryChanged(e);
		}

		public void indexChanged(IndexChangedEvent e) {
			repositoryChanged(e);
		}

		private void repositoryChanged(RepositoryChangedEvent e) {
			Set<RepositoryMapping> ms = new HashSet<RepositoryMapping>();
			for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				RepositoryMapping mapping = RepositoryMapping.getMapping(p);
				if (mapping != null && mapping.getRepository() == e.getRepository())
					ms.add(mapping);
			}
			for (RepositoryMapping m : ms) {
				repositoryChanged(m);
			}
		}

		public void repositoryChanged(final RepositoryMapping which) {
			synchronized (resources) {
				resources.add(which.getContainer());
			}
			schedule();
		}

		@Override
		protected IStatus run(IProgressMonitor arg0) {
			try {
				if (resources.size() > 0) {
					IResource m;
					synchronized(resources) {
						Iterator<IResource> i = resources.iterator();
						m = i.next();
						i.remove();
						if (resources.size() > 0)
							schedule();
					}
					ISchedulingRule markerRule = m.getWorkspace().getRuleFactory().markerRule(m);
					getJobManager().beginRule(markerRule, arg0);
					try {
						m.accept(new IResourceVisitor() {
							public boolean visit(IResource resource) throws CoreException {
								getActiveDecorator().clearDecorationState(resource);
								return true;
							}
						},
						IResource.DEPTH_INFINITE,
						true);
					} finally {
						getJobManager().endRule(markerRule);
					}
				}
				return Status.OK_STATUS;
			} catch (Exception e) {
				// We must be silent here or the UI will panic with lots of error messages
				Activator.logError("Failed to trigger resource re-decoration", e);
				return Status.OK_STATUS;
			}
		}

		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
				return;
			}
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {
					public boolean visit(IResourceDelta delta)
							throws CoreException {
						for (IResource r = delta.getResource(); r.getType() != IResource.ROOT; r = r
								.getParent()) {
							synchronized (resources) {
								resources.add(r);
							}
						}
						return true;
					}
				},
				true
				);
			} catch (Exception e) {
				Activator.logError("Problem during decorations. Stopped", e);
			}
			schedule();
		}

		void force() {
			for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
				synchronized (resources) {
					resources.add(p);
				}
			}
			schedule();
		}
	} // End ResCL

	void clearDecorationState(IResource r) throws CoreException {
		if (r.isAccessible())
			r.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, null);
		fireLabelProviderChanged(new LabelProviderChangedEvent(this, r));
	}

	static ResCL myrescl = new ResCL();

	static {
		Repository.addAnyRepositoryChangedListener(myrescl);
		GitProjectData.addRepositoryChangeListener(myrescl);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(myrescl,
				IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Request that the decorator be updated, to reflect any recent changes.
	 * <p>
	 * Can be invoked any any thread. If the current thread is not the UI
	 * thread, an async update will be scheduled.
	 * </p>
	 */
	public static void refresh() {
		myrescl.force();
	}

	private static IResource toIResource(final Object e) {
		if (e instanceof IResource)
			return (IResource) e;
		if (e instanceof IAdaptable) {
			final Object c = ((IAdaptable) e).getAdapter(IResource.class);
			if (c instanceof IResource)
				return (IResource) c;
		}
		return null;
	}

	static QualifiedName GITFOLDERDIRTYSTATEPROPERTY = new QualifiedName(
			"org.spearce.egit.ui.internal.decorators.GitResourceDecorator",
			"dirty");

	static final int UNCHANGED = 0;

	static final int CHANGED = 1;

	private Boolean isDirty(IResource rsrc) {
		try {
			if (rsrc.getType() == IResource.FILE && Team.isIgnored((IFile)rsrc))
				return Boolean.FALSE;

			RepositoryMapping mapped = RepositoryMapping.getMapping(rsrc);
			if (mapped != null) {
				if (rsrc instanceof IContainer) {
					for (IResource r : ((IContainer) rsrc)
							.members(IContainer.EXCLUDE_DERIVED)) {
						Boolean f = isDirty(r);
						if (f == null || f.booleanValue())
							return Boolean.TRUE;
					}
					return Boolean.FALSE;
				}

				return Boolean.valueOf(mapped.isResourceChanged(rsrc));
			}
			return null; // not mapped
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void decorate(final Object element, final IDecoration decoration) {
		final IResource rsrc = toIResource(element);
		if (rsrc == null)
			return;

		// If the workspace has not been refreshed properly a resource might
		// not actually exist, so we ignore these and do not decorate them
		if (!rsrc.exists() && !rsrc.isPhantom()) {
			Activator.trace("Tried to decorate non-existent resource "+rsrc);
			return;
		}

		RepositoryMapping mapped = RepositoryMapping.getMapping(rsrc);

		// TODO: How do I see a renamed resource?
		// TODO: Even trickier: when a path change from being blob to tree?
		try {
			if (mapped != null) {
				Repository repository = mapped.getRepository();
				GitIndex index = repository.getIndex();
				String repoRelativePath = mapped.getRepoRelativePath(rsrc);
				Tree headTree = repository.mapTree("HEAD");
				TreeEntry blob = headTree!=null ? headTree.findBlobMember(repoRelativePath) : null;
				Entry entry = index.getEntry(repoRelativePath);
				if (entry == null) {
					if (blob == null) {
						if (rsrc instanceof IContainer) {
							Integer df = (Integer) rsrc
									.getSessionProperty(GITFOLDERDIRTYSTATEPROPERTY);
							Boolean f = df == null ? isDirty(rsrc)
									: Boolean.valueOf(df.intValue() == CHANGED);
							if (f != null) {
								if (f.booleanValue()) {
									decoration.addPrefix(">"); // Have not
									// seen
									orState(rsrc, CHANGED);
								} else {
									orState(rsrc, UNCHANGED);
									// decoration.addSuffix("=?");
								}
							} else {
								decoration.addSuffix(" ?* ");
							}

							if (rsrc instanceof IProject) {
								Repository repo = mapped.getRepository();
								try {
									String branch = repo.getBranch();
									if (repo.isStGitMode()) {
										String patch = repo.getPatch();
										decoration.addSuffix(" [StGit " + patch + "@" + branch
												+ "]");
									} else {
										RepositoryState repositoryState = repo.getRepositoryState();
										String statename;
										if (repositoryState.equals(RepositoryState.SAFE))
											statename = "";
										else
											statename = repositoryState.getDescription() + " ";
										decoration.addSuffix(" [Git " + statename + "@ " + branch + "]");
									}
								} catch (IOException e) {
									e.printStackTrace();
									decoration.addSuffix(" [Git ?]");
								}
								decoration.addOverlay(UIIcons.OVR_SHARED);
							}

						} else {
							if (Team.isIgnoredHint(rsrc)) {
								decoration.addSuffix("(ignored)");
							} else {
								decoration.addPrefix(">");
								decoration.addSuffix("(untracked)");
								orState(rsrc.getParent(), CHANGED);
							}
						}
					} else {
						if (!(rsrc instanceof IContainer)) {
							decoration.addSuffix("(deprecated)"); // Will drop on
							// commit
							decoration.addOverlay(UIIcons.OVR_PENDING_REMOVE);
							orState(rsrc.getParent(), CHANGED);
						}
					}
				} else {
					if (entry.getStage() != GitIndex.STAGE_0) {
						decoration.addSuffix("(conflict)");
						decoration.addOverlay(UIIcons.OVR_CONFLICT);
						orState(rsrc.getParent(), CHANGED);
						return;
					}

					if (blob == null) {
						decoration.addOverlay(UIIcons.OVR_PENDING_ADD);
						orState(rsrc.getParent(), CHANGED);
					} else {

						if (entry.isAssumedValid()) {
							decoration.addOverlay(UIIcons.OVR_ASSUMEVALID);
							return;
						}

						decoration.addOverlay(UIIcons.OVR_SHARED);

						if (entry.isModified(mapped.getWorkDir(), true)) {
							decoration.addPrefix(">");
							decoration.addSuffix("(not updated)");
							orState(rsrc.getParent(), CHANGED);
						} else {
							if (!entry.getObjectId().equals(blob.getId()))
								decoration.addPrefix(">");
							else
								decoration.addPrefix(""); // set it to avoid further calls
						}
					}
				}
			}
		} catch (IOException e) {
			decoration.addSuffix("?");
			// If we throw an exception Eclipse will log the error and
			// unregister us thereby preventing us from dragging down the
			// entire workbench because we are crashing.
			//
			throw new RuntimeException(UIText.Decorator_failedLazyLoading, e);
		} catch (CoreException e) {
			throw new RuntimeException(UIText.Decorator_failedLazyLoading, e);
		}
	}

	private void orState(final IResource rsrc, int flag) {
		if (rsrc == null || rsrc.getType() == IResource.ROOT) {
			return;
		}

		try {
			Integer dirty = (Integer) rsrc.getSessionProperty(GITFOLDERDIRTYSTATEPROPERTY);
			Runnable runnable = new Runnable() {
				public void run() {
					// Async could be called after a
					// project is closed or a
					// resource is deleted
					if (!rsrc.isAccessible())
						return;
					fireLabelProviderChanged(new LabelProviderChangedEvent(
							GitResourceDecorator.this, rsrc));
				}
			};
			if (dirty == null) {
				rsrc.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, new Integer(flag));
				orState(rsrc.getParent(), flag);
//				if (Thread.currentThread() == Display.getDefault().getThread())
//					runnable.run();
//				else
					Display.getDefault().asyncExec(runnable);
			} else {
				if ((dirty.intValue() | flag) != dirty.intValue()) {
					dirty = new Integer(dirty.intValue() | flag);
					rsrc.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, dirty);
					orState(rsrc.getParent(), dirty.intValue());
//					if (Thread.currentThread() == Display.getDefault().getThread())
//						runnable.run();
//					else
						Display.getDefault().asyncExec(runnable);
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return super.isLabelProperty(element, property);
	}
}
