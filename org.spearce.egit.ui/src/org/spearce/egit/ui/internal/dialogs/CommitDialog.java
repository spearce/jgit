/*
 *  Copyright (C) 2007 David Watson <dwatson@mimvista.com>
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

package org.spearce.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.lib.GitIndex.Entry;

/**
 * @author dwatson Dialog is shown to user when they request to commit files.
 *         Changes in the selected portion of the tree are shown.
 */
public class CommitDialog extends Dialog {

	class CommitLabelProvider extends WorkbenchLabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int columnIndex) {
			IFile file = (IFile) obj;
			if (columnIndex == 1)
				return file.getProject().getName() + ": "
						+ file.getProjectRelativePath();

			else if (columnIndex == 0) {
				String prefix = "Unknown";

				final GitProjectData projectData = GitProjectData.get(file
						.getProject());
				try {
					RepositoryMapping repositoryMapping = projectData
							.getRepositoryMapping(file.getProject());

					Repository repo = repositoryMapping.getRepository();
					GitIndex index = repo.getIndex();
					Tree headTree = repo.mapTree("HEAD");

					String repoPath = repositoryMapping
							.getRepoRelativePath(file);
					TreeEntry headEntry = headTree.findBlobMember(repoPath);
					boolean headExists = headTree.existsBlob(repoPath);

					Entry indexEntry = index.getEntry(repoPath);
					if (headEntry == null) {
						prefix = "Added";
					} else if (indexEntry == null) {
						prefix = "Removed(?)";
					} else if (headExists
							&& !headEntry.getId().equals(
									indexEntry.getObjectId())) {
						prefix = "Modified";
					} else
						prefix = "Mod., index diff";

				} catch (Exception e) {
				}

				return prefix;
			}
			return null;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0)
				return getImage(element);
			return null;
		}
	}

	ArrayList<IFile> files;

	/**
	 * @param parentShell
	 */
	public CommitDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.SELECT_ALL_ID, "Select All", false);
		createButton(parent, IDialogConstants.DESELECT_ALL_ID, "Deselect All", false);

		createButton(parent, IDialogConstants.OK_ID, "Commit", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, true);
	}

	Text commitText;

	CheckboxTableViewer filesViewer;

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		parent.getShell().setText("Commit Changes");

		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);

		Label label = new Label(container, SWT.LEFT);
		label.setText("Commit Message:");
		label.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).create());

		commitText = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		commitText.setLayoutData(GridDataFactory.fillDefaults().span(1, 1)
				.hint(600, 200).create());

		Table resourcesTable = new Table(container, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.BORDER);
		resourcesTable.setLayoutData(GridDataFactory.fillDefaults().hint(600,
				200).create());

		resourcesTable.setHeaderVisible(true);
		TableColumn statCol = new TableColumn(resourcesTable, SWT.LEFT);
		statCol.setText("Status");
		statCol.setWidth(150);

		TableColumn resourceCol = new TableColumn(resourcesTable, SWT.LEFT);
		resourceCol.setText("File");
		resourceCol.setWidth(415);

		filesViewer = new CheckboxTableViewer(resourcesTable);
		filesViewer.setContentProvider(new IStructuredContentProvider() {

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				return files.toArray();
			}

		});
		filesViewer.setLabelProvider(new CommitLabelProvider());
		filesViewer.setInput(files);
		filesViewer.setAllChecked(true);

		container.pack();
		return container;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String s) {
		this.commitMessage = s;
	}

	private String commitMessage = "";

	private ArrayList<IFile> selectedItems = new ArrayList<IFile>();

	public void setSelectedItems(IFile[] items) {
		Collections.addAll(selectedItems, items);
	}

	public IFile[] getSelectedItems() {
		return selectedItems.toArray(new IFile[0]);
	}

	@Override
	protected void okPressed() {
		commitMessage = commitText.getText();
		Object[] checkedElements = filesViewer.getCheckedElements();
		selectedItems.clear();
		for (Object obj : checkedElements)
			selectedItems.add((IFile) obj);

		if (commitMessage.trim().length() == 0) {
			MessageDialog.openWarning(getShell(), "No message", "You must enter a commit message");
			return;
		}

		if (selectedItems.isEmpty()) {
			MessageDialog.openWarning(getShell(), "No items selected", "No items are currently selected to be committed.");
			return;
		}
		super.okPressed();
	}

	public void setFileList(ArrayList<IFile> files) {
		this.files = files;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.SELECT_ALL_ID == buttonId) {
			filesViewer.setAllChecked(true);
		}
		if (IDialogConstants.DESELECT_ALL_ID == buttonId) {
			filesViewer.setAllChecked(false);
		}
		super.buttonPressed(buttonId);
	}

}
