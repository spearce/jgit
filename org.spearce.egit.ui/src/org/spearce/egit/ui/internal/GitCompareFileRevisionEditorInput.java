/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Robin Rosenberg - Git interface
 *******************************************************************************/
package org.spearce.egit.ui.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.IWorkbenchPage;

public class GitCompareFileRevisionEditorInput extends CompareEditorInput {

	private ITypedElement left;
	private ITypedElement right;

	/**
	 * Creates a new CompareFileRevisionEditorInput.
	 * @param left
	 * @param right
	 * @param page
	 */
	public GitCompareFileRevisionEditorInput(ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		super(new CompareConfiguration());
		this.left = left;
		this.right = right;
	}

	protected FileRevisionTypedElement getRightRevision() {
		if (right instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) right;
		}
		return null;
	}

	protected FileRevisionTypedElement getLeftRevision() {
		if (left instanceof FileRevisionTypedElement) {
			return (FileRevisionTypedElement) left;
		}
		return null;
	}

	private static void ensureContentsCached(FileRevisionTypedElement left, FileRevisionTypedElement right,
			IProgressMonitor monitor) {
		if (left != null) {
			try {
				left.cacheContents(monitor);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		if (right != null) {
			try {
				right.cacheContents(monitor);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
	}

	private boolean isLeftEditable(ICompareInput input) {
		Object left = input.getLeft();
		if (left instanceof IEditableContent) {
			return ((IEditableContent) left).isEditable();
		}
		return false;
	}

	private IResource getResource(ICompareInput input) {
		if (getLocalElement() != null) {
			return ((IResourceProvider) getLocalElement()).getResource();
		}
		return null;
	}

	private ICompareInput createCompareInput() {
		return compare(left, right);
	}

	private DiffNode compare(ITypedElement left, ITypedElement right) {
		if (left.getType().equals(ITypedElement.FOLDER_TYPE)) {
			//			return new MyDiffContainer(null, left,right);
			DiffNode diffNode = new DiffNode(null,Differencer.CHANGE,null,left,right);
			ITypedElement[] lc = (ITypedElement[])((IStructureComparator)left).getChildren();
			ITypedElement[] rc = (ITypedElement[])((IStructureComparator)right).getChildren();
			int li=0;
			int ri=0;
			while (li<lc.length && ri<rc.length) {
				ITypedElement ln = lc[li];
				ITypedElement rn = rc[ri];
				// TODO: Git ordering!
				if (ln.getName().compareTo(rn.getName()) < 0) {
					diffNode.add(new DiffNode(Differencer.ADDITION,null, ln, null));
					++li;
				}
				if (ln.getName().compareTo(rn.getName()) > 0) {
					diffNode.add(new DiffNode(Differencer.DELETION,null, null, rn));
					++ri;
				}
				if (ln.getName().compareTo(rn.getName()) == 0) {
					if (!ln.equals(rn))
						diffNode.add(compare(ln,rn));
					++li;
					++ri;
				}
			}
			while (li<lc.length) {
				ITypedElement ln = lc[li];
				diffNode.add(new DiffNode(Differencer.ADDITION,null, ln, null));
				++li;
			}
			while (ri<rc.length) {
				ITypedElement rn = rc[ri];
				diffNode.add(new DiffNode(Differencer.ADDITION,null, null, rn));
				++ri;
			}
			return diffNode;
		} else {
			return new DiffNode(left, right);
		}
	}

	private void initLabels(ICompareInput input) {
		CompareConfiguration cc = getCompareConfiguration();
		if (getLeftRevision() != null) {
			String leftLabel = getFileRevisionLabel(getLeftRevision());
			cc.setLeftLabel(leftLabel);
		} else if (getResource(input) != null) {
			String label = NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_workspace, new Object[]{ input.getLeft().getName() });
			cc.setLeftLabel(label);
		}
		if (getRightRevision() != null) {
			String rightLabel = getFileRevisionLabel(getRightRevision());
			cc.setRightLabel(rightLabel);
		}
	}

	private String getFileRevisionLabel(FileRevisionTypedElement element) {
		Object fileObject = element.getFileRevision();
		if (fileObject instanceof LocalFileRevision){
			return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_localRevision, new Object[]{element.getName(), element.getTimestamp()});
		} else {
			return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_repository, new Object[]{ element.getName(), element.getContentIdentifier()});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getLongName(left);
		titleObject[1] = getContentIdentifier(getLeftRevision());
		titleObject[2] = getContentIdentifier(getRightRevision());
		return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_compareResourceAndVersions, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getTitle()
	 */
	public String getTitle() {
		Object[] titleObject = new Object[3];
		titleObject[0] = getShortName(left);
		titleObject[1] = getContentIdentifier(getLeftRevision());
		titleObject[2] = getContentIdentifier(getRightRevision());
		return NLS.bind(TeamUIMessages.CompareFileRevisionEditorInput_compareResourceAndVersions, titleObject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.CompareEditorInput#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IFile.class || adapter == IResource.class) {
			if (getLocalElement() != null) {
				return getLocalElement().getResource();
			}
			return null;
		}
		return super.getAdapter(adapter);
	}

	private String getShortName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getName();
		}
		else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getName();
		}
		return element.getName();
	}

	private String getLongName(ITypedElement element) {
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			return fileRevisionElement.getPath();
		}
		else if (element instanceof LocalResourceTypedElement){
			LocalResourceTypedElement typedContent = (LocalResourceTypedElement) element;
			return typedContent.getResource().getFullPath().toString();
		}
		return element.getName();
	}

	private String getContentIdentifier(ITypedElement element){
		if (element instanceof FileRevisionTypedElement){
			FileRevisionTypedElement fileRevisionElement = (FileRevisionTypedElement) element;
			Object fileObject = fileRevisionElement.getFileRevision();
			if (fileObject instanceof LocalFileRevision){
				try {
					IStorage storage = ((LocalFileRevision) fileObject).getStorage(new NullProgressMonitor());
					if (Utils.getAdapter(storage, IFileState.class) != null){
						//local revision
						return TeamUIMessages.CompareFileRevisionEditorInput_0;
					} else if (Utils.getAdapter(storage, IFile.class) != null) {
						//current revision
						return TeamUIMessages.CompareFileRevisionEditorInput_1;
					}
				} catch (CoreException e) {
				}
			} else {
				return fileRevisionElement.getContentIdentifier();
			}
		}
		return TeamUIMessages.CompareFileRevisionEditorInput_2;
	}

//	/* (non-Javadoc)
//	 * @see org.eclipse.team.ui.synchronize.LocalResourceCompareEditorInput#fireInputChange()
//	 */
//	protected void fireInputChange() {
//		((DiffNode)getCompareResult()).fireChange();
//	}
//
//	/* (non-Javadoc)
//	 * @see org.eclipse.team.ui.synchronize.SaveableCompareEditorInput#contentsCreated()
//	 */
//	protected void contentsCreated() {
//		super.contentsCreated();
//		notifier.initialize();
//	}
//
//	/* (non-Javadoc)
//	 * @see org.eclipse.team.ui.synchronize.SaveableCompareEditorInput#handleDispose()
//	 */
//	protected void handleDispose() {
//		super.handleDispose();
//		notifier.dispose();
//		if (getLocalElement() != null) {
//			getLocalElement().discardBuffer();
//		}
//	}
//
	public LocalResourceTypedElement getLocalElement() {
		if (left instanceof LocalResourceTypedElement) {
			return (LocalResourceTypedElement) left;
		}
		return null;
	}

	public ITypedElement getLeft() {
		return left;
	}

	protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		ICompareInput input = createCompareInput();
		getCompareConfiguration().setLeftEditable(isLeftEditable(input));
		getCompareConfiguration().setRightEditable(false);
		ensureContentsCached(getLeftRevision(), getRightRevision(), monitor);
		initLabels(input);
		setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { input.getName() }));

		// The compare editor (Structure Compare) will show the diff filenames
		// with their project relative path. So, no need to also show directory entries.
		DiffNode flatDiffNode = new DiffNode(null,Differencer.CHANGE,null,left,right);
		flatDiffView(flatDiffNode, (DiffNode) input);

		return flatDiffNode;
	}

	private void flatDiffView(DiffNode rootNode, DiffNode currentNode) {
		if(currentNode != null) {
			IDiffElement[] dElems = currentNode.getChildren();
			if(dElems != null) {
				for(IDiffElement dElem : dElems) {
					DiffNode dNode = (DiffNode) dElem;
					if(dNode.getChildren() != null && dNode.getChildren().length > 0) {
						flatDiffView(rootNode, dNode);
					} else {
						rootNode.add(dNode);
					}
				}
			}
		}
	}

}
