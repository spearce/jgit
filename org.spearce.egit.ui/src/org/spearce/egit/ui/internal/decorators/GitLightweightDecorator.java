/*******************************************************************************
 * Copyright (C) 2007, IBM Corporation and others
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/

package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.PlatformUI;
import org.spearce.egit.core.GitException;
import org.spearce.egit.core.internal.util.ExceptionCollector;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryChangeListener;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIPreferences;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.decorators.IDecoratableResource.Staged;
import org.spearce.jgit.lib.IndexChangedEvent;
import org.spearce.jgit.lib.RefsChangedEvent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryChangedEvent;
import org.spearce.jgit.lib.RepositoryListener;

/**
 * Supplies annotations for displayed resources
 *
 * This decorator provides annotations to indicate the status of each resource
 * when compared to <code>HEAD</code>, as well as the index in the relevant
 * repository.
 *
 * TODO: Add support for colors and font decoration
 */
public class GitLightweightDecorator extends LabelProvider implements
		ILightweightLabelDecorator, IPropertyChangeListener,
		IResourceChangeListener, RepositoryChangeListener, RepositoryListener {

	/**
	 * Property constant pointing back to the extension point id of the
	 * decorator
	 */
	public static final String DECORATOR_ID = "org.spearce.egit.ui.internal.decorators.GitLightweightDecorator"; //$NON-NLS-1$

	/**
	 * Bit-mask describing interesting changes for IResourceChangeListener
	 * events
	 */
	private static int INTERESTING_CHANGES = IResourceDelta.CONTENT
			| IResourceDelta.MOVED_FROM | IResourceDelta.MOVED_TO
			| IResourceDelta.OPEN | IResourceDelta.REPLACED
			| IResourceDelta.TYPE;

	/**
	 * Collector for keeping the error view from filling up with exceptions
	 */
	private static ExceptionCollector exceptions = new ExceptionCollector(
			UIText.Decorator_exceptionMessage, Activator.getPluginId(),
			IStatus.ERROR, Activator.getDefault().getLog());

	/**
	 * Constructs a new Git resource decorator
	 */
	public GitLightweightDecorator() {
		TeamUI.addPropertyChangeListener(this);
		Activator.addPropertyChangeListener(this);
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.addPropertyChangeListener(this);
		Repository.addAnyRepositoryChangedListener(this);
		GitProjectData.addRepositoryChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
				.removePropertyChangeListener(this);
		TeamUI.removePropertyChangeListener(this);
		Activator.removePropertyChangeListener(this);
		Repository.removeAnyRepositoryChangedListener(this);
		GitProjectData.removeRepositoryChangeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/**
	 * This method should only be called by the decorator thread.
	 *
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
	 *      org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) {
		final IResource resource = getResource(element);
		if (resource == null)
			return;

		// Don't decorate if the workbench is not running
		if (!PlatformUI.isWorkbenchRunning())
			return;

		// Don't decorate if UI plugin is not running
		Activator activator = Activator.getDefault();
		if (activator == null)
			return;

		// Don't decorate the workspace root
		if (resource.getType() == IResource.ROOT)
			return;

		// Don't decorate non-existing resources
		if (!resource.exists() && !resource.isPhantom())
			return;

		// Make sure we're dealing with a project under Git revision control
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(resource);
		if (mapping == null)
			return;

		// Cannot decorate linked resources
		if (mapping.getRepoRelativePath(resource) == null)
			return;

		try {
			DecorationHelper helper = new DecorationHelper(activator
					.getPreferenceStore());
			helper.decorate(decoration,
					new DecoratableResourceAdapter(resource));
		} catch (IOException e) {
			handleException(resource, GitException.wrapException(e));
		}
	}

	/**
	 * Helper class for doing resource decoration, based on the given
	 * preferences
	 *
	 * Used for real-time decoration, as well as in the decorator preview
	 * preferences page
	 */
	public static class DecorationHelper {

		/** */
		public static final String BINDING_RESOURCE_NAME = "name"; //$NON-NLS-1$

		/** */
		public static final String BINDING_BRANCH_NAME = "branch"; //$NON-NLS-1$

		/** */
		public static final String BINDING_DIRTY_FLAG = "dirty"; //$NON-NLS-1$

		/** */
		public static final String BINDING_STAGED_FLAG = "staged"; //$NON-NLS-1$

		private IPreferenceStore store;

		/**
		 * Define a cached image descriptor which only creates the image data
		 * once
		 */
		private static class CachedImageDescriptor extends ImageDescriptor {
			ImageDescriptor descriptor;

			ImageData data;

			public CachedImageDescriptor(ImageDescriptor descriptor) {
				this.descriptor = descriptor;
			}

			public ImageData getImageData() {
				if (data == null) {
					data = descriptor.getImageData();
				}
				return data;
			}
		}

		private static ImageDescriptor trackedImage;

		private static ImageDescriptor untrackedImage;

		private static ImageDescriptor stagedImage;

		private static ImageDescriptor stagedAddedImage;

		private static ImageDescriptor stagedRemovedImage;

		private static ImageDescriptor conflictImage;

		private static ImageDescriptor assumeValidImage;

		static {
			trackedImage = new CachedImageDescriptor(TeamImages
					.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
			untrackedImage = new CachedImageDescriptor(UIIcons.OVR_UNTRACKED);
			stagedImage = new CachedImageDescriptor(UIIcons.OVR_STAGED);
			stagedAddedImage = new CachedImageDescriptor(UIIcons.OVR_STAGED_ADD);
			stagedRemovedImage = new CachedImageDescriptor(
					UIIcons.OVR_STAGED_REMOVE);
			conflictImage = new CachedImageDescriptor(UIIcons.OVR_CONFLICT);
			assumeValidImage = new CachedImageDescriptor(UIIcons.OVR_ASSUMEVALID);
		}

		/**
		 * Constructs a decorator using the rules from the given
		 * <code>preferencesStore</code>
		 *
		 * @param preferencesStore
		 *            the preferences store with the preferred decorator rules
		 */
		public DecorationHelper(IPreferenceStore preferencesStore) {
			store = preferencesStore;
		}

		/**
		 * Decorates the given <code>decoration</code> based on the state of the
		 * given <code>resource</code>, using the preferences passed when
		 * constructing this decoration helper.
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param resource
		 *            the resource to retrieve state from
		 */
		public void decorate(IDecoration decoration,
				IDecoratableResource resource) {
			if (resource.isIgnored())
				return;

			decorateText(decoration, resource);
			decorateIcons(decoration, resource);
		}

		private void decorateText(IDecoration decoration,
				IDecoratableResource resource) {
			String format = "";
			switch (resource.getType()) {
			case IResource.FILE:
				format = store
						.getString(UIPreferences.DECORATOR_FILETEXT_DECORATION);
				break;
			case IResource.FOLDER:
				format = store
						.getString(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION);
				break;
			case IResource.PROJECT:
				format = store
						.getString(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION);
				break;
			}

			Map<String, String> bindings = new HashMap<String, String>();
			bindings.put(BINDING_RESOURCE_NAME, resource.getName());
			bindings.put(BINDING_BRANCH_NAME, resource.getBranch());
			bindings.put(BINDING_DIRTY_FLAG, resource.isDirty() ? ">" : null);
			bindings.put(BINDING_STAGED_FLAG,
					resource.staged() != Staged.NOT_STAGED ? "*" : null);

			decorate(decoration, format, bindings);
		}

		private void decorateIcons(IDecoration decoration,
				IDecoratableResource resource) {
			ImageDescriptor overlay = null;

			if (resource.isTracked()) {
				if (store.getBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON))
					overlay = trackedImage;

				if (store
						.getBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_VALID_ICON)
						&& resource.isAssumeValid())
					overlay = assumeValidImage;

				// Staged overrides tracked
				Staged staged = resource.staged();
				if (store.getBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON)
						&& staged != Staged.NOT_STAGED) {
					if (staged == Staged.ADDED)
						overlay = stagedAddedImage;
					else if (staged == Staged.REMOVED)
						overlay = stagedRemovedImage;
					else
						overlay = stagedImage;
				}

				// Conflicts override everything
				if (store
						.getBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON)
						&& resource.hasConflicts())
					overlay = conflictImage;

			} else if (store
					.getBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON)) {
				overlay = untrackedImage;
			}

			// Overlays can only be added once, so do it at the end
			decoration.addOverlay(overlay);
		}

		/**
		 * Decorates the given <code>decoration</code>, using the specified text
		 * <code>format</code>, and mapped using the variable bindings from
		 * <code>bindings</code>
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param format
		 *            the format to base the decoration on
		 * @param bindings
		 *            the bindings between variables in the format and actual
		 *            values
		 */
		public static void decorate(IDecoration decoration, String format,
				Map<String, String> bindings) {
			StringBuffer prefix = new StringBuffer();
			StringBuffer suffix = new StringBuffer();
			StringBuffer output = prefix;

			int length = format.length();
			int start = -1;
			int end = length;
			while (true) {
				if ((end = format.indexOf('{', start)) > -1) {
					output.append(format.substring(start + 1, end));
					if ((start = format.indexOf('}', end)) > -1) {
						String key = format.substring(end + 1, start);
						String s;

						// Allow users to override the binding
						if (key.indexOf(':') > -1) {
							String[] keyAndBinding = key.split(":", 2);
							key = keyAndBinding[0];
							if (keyAndBinding.length > 1
									&& bindings.get(key) != null)
								bindings.put(key, keyAndBinding[1]);
						}

						// We use the BINDING_RESOURCE_NAME key to determine if
						// we are doing the prefix or suffix. The name isn't
						// actually part of either.
						if (key.equals(BINDING_RESOURCE_NAME)) {
							output = suffix;
							s = null;
						} else {
							s = bindings.get(key);
						}

						if (s != null) {
							output.append(s);
						} else {
							// Support removing prefix character if binding is
							// null
							int curLength = output.length();
							if (curLength > 0) {
								char c = output.charAt(curLength - 1);
								if (c == ':' || c == '@') {
									output.deleteCharAt(curLength - 1);
								}
							}
						}
					} else {
						output.append(format.substring(end, length));
						break;
					}
				} else {
					output.append(format.substring(start + 1, length));
					break;
				}
			}

			String prefixString = prefix.toString().replaceAll("^\\s+", "");
			if (prefixString != null) {
				decoration.addPrefix(TextProcessor.process(prefixString,
						"()[].")); //$NON-NLS-1$
			}
			String suffixString = suffix.toString().replaceAll("\\s+$", "");
			if (suffixString != null) {
				decoration.addSuffix(TextProcessor.process(suffixString,
						"()[].")); //$NON-NLS-1$
			}
		}
	}

	// -------- Refresh handling --------

	/**
	 * Perform a blanket refresh of all decorations
	 */
	public static void refresh() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				Activator.getDefault().getWorkbench().getDecoratorManager()
						.update(DECORATOR_ID);
			}
		});
	}

	/**
	 * Callback for IPropertyChangeListener events
	 *
	 * If any of the relevant preferences has been changed we refresh all
	 * decorations (all projects and their resources).
	 *
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		final String prop = event.getProperty();
		// If the property is of any interest to us
		if (prop.equals(TeamUI.GLOBAL_IGNORES_CHANGED)
				|| prop.equals(TeamUI.GLOBAL_FILE_TYPES_CHANGED)
				|| prop.equals(Activator.DECORATORS_CHANGED)) {
			postLabelEvent(new LabelProviderChangedEvent(this));
		}
	}

	/**
	 * Callback for IResourceChangeListener events
	 *
	 * Schedules a refresh of the changed resource
	 *
	 * If the preference for computing deep dirty states has been set we walk
	 * the ancestor tree of the changed resource and update all parents as well.
	 *
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		final Set<IResource> resourcesToUpdate = new HashSet<IResource>();

		try { // Compute the changed resources by looking at the delta
			event.getDelta().accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {

					// If the file has changed but not in a way that we care
					// about (e.g. marker changes to files) then ignore
					if (delta.getKind() == IResourceDelta.CHANGED
							&& (delta.getFlags() & INTERESTING_CHANGES) == 0) {
						return true;
					}

					final IResource resource = delta.getResource();

					// If the resource is not part of a project under Git
					// revision control
					final RepositoryMapping mapping = RepositoryMapping
							.getMapping(resource);
					if (mapping == null) {
						// Ignore the change
						return true;
					}

					if (resource.getType() == IResource.ROOT) {
						// Continue with the delta
						return true;
					}

					if (resource.getType() == IResource.PROJECT) {
						// If the project is not accessible, don't process it
						if (!resource.isAccessible())
							return false;
					}

					// All seems good, schedule the resource for update
					resourcesToUpdate.add(resource);

					if (delta.getKind() == IResourceDelta.CHANGED
							&& (delta.getFlags() & IResourceDelta.OPEN) > 1)
						return false; // Don't recurse when opening projects
					else
						return true;
				}
			}, true /* includePhantoms */);
		} catch (final CoreException e) {
			handleException(null, e);
		}

		if (resourcesToUpdate.isEmpty())
			return;

		// If ancestor-decoration is enabled in the preferences we walk
		// the ancestor tree of each of the changed resources and add
		// their parents to the update set
		final IPreferenceStore store = Activator.getDefault()
				.getPreferenceStore();
		if (store.getBoolean(UIPreferences.DECORATOR_RECOMPUTE_ANCESTORS)) {
			final IResource[] changedResources = resourcesToUpdate
					.toArray(new IResource[resourcesToUpdate.size()]);
			for (IResource current : changedResources) {
				while (current.getType() != IResource.ROOT) {
					current = current.getParent();
					resourcesToUpdate.add(current);
				}
			}
		}

		postLabelEvent(new LabelProviderChangedEvent(this, resourcesToUpdate
				.toArray()));
	}

	/**
	 * Callback for RepositoryListener events
	 *
	 * We resolve the repository mapping for the changed repository and forward
	 * that to repositoryChanged(RepositoryMapping).
	 *
	 * @param e
	 *            The original change event
	 */
	private void repositoryChanged(RepositoryChangedEvent e) {
		final Set<RepositoryMapping> ms = new HashSet<RepositoryMapping>();
		for (final IProject p : ResourcesPlugin.getWorkspace().getRoot()
				.getProjects()) {
			final RepositoryMapping mapping = RepositoryMapping.getMapping(p);
			if (mapping != null && mapping.getRepository() == e.getRepository())
				ms.add(mapping);
		}
		for (final RepositoryMapping m : ms) {
			repositoryChanged(m);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.spearce.jgit.lib.RepositoryListener#indexChanged(org.spearce.jgit
	 * .lib.IndexChangedEvent)
	 */
	public void indexChanged(IndexChangedEvent e) {
		repositoryChanged(e);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.spearce.jgit.lib.RepositoryListener#refsChanged(org.spearce.jgit.
	 * lib.RefsChangedEvent)
	 */
	public void refsChanged(RefsChangedEvent e) {
		repositoryChanged(e);
	}

	/**
	 * Callback for RepositoryChangeListener events, as well as
	 * RepositoryListener events via repositoryChanged()
	 *
	 * @see org.spearce.egit.core.project.RepositoryChangeListener#repositoryChanged(org.spearce.egit.core.project.RepositoryMapping)
	 */
	public void repositoryChanged(RepositoryMapping mapping) {
		// Until we find a way to refresh visible labels within a project
		// we have to use this blanket refresh that includes all projects.
		postLabelEvent(new LabelProviderChangedEvent(this));
	}

	// -------- Helper methods --------

	private static IResource getResource(Object element) {
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}

		IResource resource = null;
		if (element instanceof IResource) {
			resource = (IResource) element;
		} else if (element instanceof IAdaptable) {
			final IAdaptable adaptable = (IAdaptable) element;
			resource = (IResource) adaptable.getAdapter(IResource.class);
			if (resource == null) {
				final IContributorResourceAdapter adapter = (IContributorResourceAdapter) adaptable
						.getAdapter(IContributorResourceAdapter.class);
				if (adapter != null)
					resource = adapter.getAdaptedResource(adaptable);
			}
		}

		return resource;
	}

	/**
	 * Post the label event to the UI thread
	 *
	 * @param event
	 *            The event to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				fireLabelProviderChanged(event);
			}
		});
	}

	/**
	 * Handle exceptions that occur in the decorator. Exceptions are only logged
	 * for resources that are accessible (i.e. exist in an open project).
	 *
	 * @param resource
	 *            The resource that triggered the exception
	 * @param e
	 *            The exception that occurred
	 */
	private static void handleException(IResource resource, CoreException e) {
		if (resource == null || resource.isAccessible())
			exceptions.handleException(e);
	}
}
