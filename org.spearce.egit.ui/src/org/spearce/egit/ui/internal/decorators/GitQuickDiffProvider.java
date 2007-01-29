/*
 *  Copyright (C) 2006  Robin Rosenberg
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

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;

public class GitQuickDiffProvider implements IQuickDiffReferenceProvider {

    private String id;

    private Document document;

    private IFile file;

    public void dispose() {
	// No resources to free
    }

    public String getId() {
	return id;
    }

    public IDocument getReference(IProgressMonitor monitor)
	    throws CoreException {
	document = new Document();
	Activator.trace("(GitQuickDiffProvider) file: " + file);

	RepositoryProvider provider = RepositoryProvider
		.getProvider(file.getProject());
	if (provider != null) {
	    try {
		IFileHistoryProvider fileHistoryProvider = provider
			.getFileHistoryProvider();
		IFileHistory fileHistoryFor = fileHistoryProvider
			.getFileHistoryFor(file,
				IFileHistoryProvider.SINGLE_REVISION, null);
		IFileRevision[] revisions = fileHistoryFor.getFileRevisions();
		if (revisions != null && revisions.length > 0) {
		    IFileRevision revision = revisions[0];
		    Activator.trace("(GitQuickDiffProvider) compareTo: "
			    + revision.getContentIdentifier());
		    IStorage storage = revision.getStorage(null);
		    InputStream contents = storage.getContents();
		    BufferedReader in = new BufferedReader(
			    new InputStreamReader(contents));
		    final int DEFAULT_FILE_SIZE = 15 * 1024;

		    CharArrayWriter caw = new CharArrayWriter(DEFAULT_FILE_SIZE);
		    char[] readBuffer = new char[2048];
		    int n = in.read(readBuffer);
		    while (n > 0) {
			caw.write(readBuffer, 0, n);
			n = in.read(readBuffer);
		    }
		    String s = caw.toString();
		    document.set(s);
		} else {
		    Activator.trace("(GitQuickDiffProvider) no revision.");
		    document.set("");
		}
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
	file = ResourceUtil.getFile(editorInput);
    }

    public void setId(String id) {
	this.id = id;
    }
}
