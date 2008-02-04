/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *  Copyright (C) 2007  Robin Rosenberg <robin.rosenberg@dewire.com>
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
package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;

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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.Team;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryChangeListener;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;
import org.spearce.jgit.lib.Repository.RepositoryState;

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

	private static final RCL myrcl = new RCL();

	static class RCL implements RepositoryChangeListener, Runnable {
		private boolean requested;

		public synchronized void run() {
			Activator.trace("Invoking decorator");
			requested = false;
			PlatformUI.getWorkbench().getDecoratorManager().update(
					GitResourceDecorator.class.getName());
		}

		public void repositoryChanged(final RepositoryMapping which) {
			try {
				which.getContainer().accept(new IResourceVisitor() {
					public boolean visit(IResource resource) throws CoreException {
						if (resource instanceof IContainer)
							clearDecorationState(resource);
						return true;
					}
				});
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			start();
		}

		synchronized void start() {
			if (requested)
				return;
			final Display d = PlatformUI.getWorkbench().getDisplay();
			if (d.getThread() == Thread.currentThread())
				run();
			else {
				requested = true;
				d.asyncExec(this);
			}
		}
	}

	static class ResCL implements IResourceChangeListener {
		public void resourceChanged(IResourceChangeEvent event) {
			Activator.trace("resourceChanged(buildKind="
					+ event.getBuildKind() + ",type=" + event.getType()
					+ ",source=" + event.getSource());
			if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
				return;
			}
			Activator.trace("CLEARING:"+event.getDelta().getResource().getFullPath().toOSString());
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {

					public boolean visit(IResourceDelta delta)
							throws CoreException {
						Activator.trace("VCLEARING:"+delta.getResource().getFullPath().toOSString());
						for (IResource r = delta.getResource(); r.getType() != IResource.ROOT; r = r
								.getParent()) {
							try {
								// Activator.trace("VCLEARING:"+r.getFullPath().toOSString());
								clearDecorationState(r);
							} catch (CoreException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						return true;
					}

				});
			} catch (CoreException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				return;
			}
			myrcl.start();
		}
	}

	static void clearDecorationState(IResource r) throws CoreException {
		if (r.isAccessible())
			r.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, null);
	}

	static ResCL myrescl = new ResCL();

	static {
		GitProjectData.addRepositoryChangeListener(myrcl);
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
		myrcl.start();
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
//		Activator.trace("isDirty(" + rsrc.getFullPath().toOSString() +")");
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

				return new Boolean(mapped.isResourceChanged(rsrc));
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

		RepositoryMapping mapped = RepositoryMapping.getMapping(rsrc);

		Activator.trace("decorate: " + element);

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
									: new Boolean(df.intValue() == CHANGED);
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
							if (rsrc.getType() == IResource.FILE
									&& Team.isIgnored((IFile) rsrc)) {
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

						if (entry.isModified(mapped.getWorkDir())) {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(UIText.Decorator_failedLazyLoading, e);
		}
	}

	private void orState(final IResource rsrc, int flag) {
		// Activator.trace("orState "+rsrc.getFullPath().toOSString()+
		// ","+flag);
		if (rsrc == null || rsrc.getType() == IResource.ROOT) {
			return;
		}

		try {
			Integer dirty = (Integer) rsrc.getSessionProperty(GITFOLDERDIRTYSTATEPROPERTY);
			if (dirty == null) {
				rsrc.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, new Integer(flag));
				Activator.trace("SETTING:"+rsrc.getFullPath().toOSString()+" => "+flag);
				orState(rsrc.getParent(), flag);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						Activator.trace("firing on " + rsrc);
						// Async could be called after a
						// project is closed or a
						// resource is deleted
						if (!rsrc.isAccessible())
							return;
						fireLabelProviderChanged(new LabelProviderChangedEvent(
								GitResourceDecorator.this, rsrc));
					}
				});
			} else {
				if ((dirty.intValue() | flag) != dirty.intValue()) {
					dirty = new Integer(dirty.intValue() | flag);
					rsrc.setSessionProperty(GITFOLDERDIRTYSTATEPROPERTY, dirty);
					Activator.trace("SETTING:"+rsrc.getFullPath().toOSString()+" => "+dirty);
					orState(rsrc.getParent(), dirty.intValue());
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							Activator.trace("firing on " + rsrc);
							// Async could be called after a
							// project is closed or a
							// resource is deleted
							if (!rsrc.isAccessible())
								return;
							fireLabelProviderChanged(new LabelProviderChangedEvent(
									GitResourceDecorator.this, rsrc));
						}
					});
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		Activator.trace("isLabelProperty("+element+","+property+")");
		return super.isLabelProperty(element, property);
	}
}
