/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.decorators;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

/**
 * This class provides input for the Eclipse Quick Diff feature.
 */
public class GitQuickDiffProvider implements IQuickDiffReferenceProvider {

	private String id;

	private GitDocument document;

	private IResource resource;

	public void dispose() {
		Activator.trace("(GitQuickDiffProvider) dispose");
		if (document != null)
			document.dispose();
	}

	public String getId() {
		return id;
	}

	public IDocument getReference(IProgressMonitor monitor)
			throws CoreException {
		Activator.trace("(GitQuickDiffProvider) file: " + resource);
		RepositoryProvider provider = RepositoryProvider.getProvider(resource
				.getProject());
		if (provider != null) {
			try {
				document = GitDocument.create(resource);
			} catch (CoreException e) {
				Activator.error(UIText.QuickDiff_failedLoading, e);
			} catch (IOException e) {
				Activator.error(UIText.QuickDiff_failedLoading, e);
			}
			return document;
		} else {
			return null;
		}
	}

	public boolean isEnabled() {
		return true;
	}

	public void setActiveEditor(ITextEditor editor) {
		IEditorInput editorInput = editor.getEditorInput();
		resource = ResourceUtil.getResource(editorInput);
	}

	public void setId(String id) {
		this.id = id;
	}
}
