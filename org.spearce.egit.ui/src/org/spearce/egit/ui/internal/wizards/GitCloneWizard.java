/*
 *  Copyright (C) 2008  Roger C. Soares
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
package org.spearce.egit.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.wizards.datatransfer.WizardProjectsImportPage;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.jgit.fetch.FetchClient;
import org.spearce.jgit.fetch.GitProtocolFetchClient;
import org.spearce.jgit.fetch.GitSshProtocolFetchClient;
import org.spearce.jgit.fetch.LocalGitProtocolFetchClient;
import org.spearce.jgit.fetch.URIish;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.WorkDirCheckout;

import com.jcraft.jsch.JSchException;

/**
 * Import Git Repository Wizard. A front end to a git clone operation.
 */
public class GitCloneWizard extends Wizard implements IImportWizard {
	private CloneInputPage cloneInput;
	private DoClonePage cloneOutput;
	private WizardProjectsImportPage importPage;

	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		cloneInput = new CloneInputPage();
		cloneOutput = new DoClonePage(cloneInput);
		importPage = new WizardProjectsImportPage();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(cloneInput);
		addPage(cloneOutput);
		addPage(importPage);
	}

	@Override
	public boolean performFinish() {
		importPage.saveWidgetValues();
		return importPage.createProjects();
	}

	class CloneInputPage extends WizardPage {
		/** No authentication requested */
		public static final int AUTH_NONE = 0;

		/** User and password authentication requested */
		public static final int AUTH_USER_PASS = 1;

		/** SSH public key authentication requested */
		public static final int AUTH_SSH_PUBLIC_KEY = 2;

		private Composite localComposite;

		private String[] authItems = new String[] { "None", "User/Password",
				"SSH public key" };

		private Combo authCombo;

		private Text uriText;

		private Text userText;

		private Text passText;

		private Text keyText;

		/**
		 * Wizard page that allows the user entering the location of a repository to
		 * be cloned.
		 */
		public CloneInputPage() {
			super("Clone Input Page", "Import Git Repository",
					UIIcons.WIZBAN_IMPORT_REPO);
			setDescription("Enter the location of the repository to be cloned.");
		}

		public void createControl(Composite parent) {
			localComposite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			localComposite.setLayout(layout);

			Label uriLabel = new Label(localComposite, SWT.NULL);
			uriLabel.setText("Location:");

			uriText = new Text(localComposite, SWT.BORDER);
			uriText.setText("");
			GridData uriTextData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
			uriText.setLayoutData(uriTextData);

			Label authLabel = new Label(localComposite, SWT.NULL);
			authLabel.setText("Authentication:");

			authCombo = new Combo(localComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
			authCombo.setItems(authItems);
			authCombo.select(0);

			final Label userLabel = new Label(localComposite, SWT.NULL);
			userLabel.setText("User:");
			final GridData userLabelData = new GridData();
			userLabelData.exclude = true;
			userLabel.setLayoutData(userLabelData);

			userText = new Text(localComposite, SWT.BORDER);
			userText.setText("");
			final GridData userTextData = new GridData(SWT.FILL, SWT.DEFAULT, true,
					false);
			userTextData.exclude = true;
			userText.setLayoutData(userTextData);

			final Label passLabel = new Label(localComposite, SWT.NULL);
			passLabel.setText("Password:");
			final GridData passLabelData = new GridData();
			passLabelData.exclude = true;
			passLabel.setLayoutData(passLabelData);

			passText = new Text(localComposite, SWT.BORDER | SWT.PASSWORD);
			passText.setText("");
			final GridData passTextData = new GridData(SWT.FILL, SWT.DEFAULT, true,
					false);
			passTextData.exclude = true;
			passText.setLayoutData(passTextData);

			final Label keyLabel = new Label(localComposite, SWT.NULL);
			keyLabel.setText("SSH public key:");
			final GridData keyLabelData = new GridData();
			keyLabelData.exclude = true;
			keyLabel.setLayoutData(keyLabelData);

			keyText = new Text(localComposite, SWT.BORDER | SWT.WRAP);
			keyText.setText("");
			final GridData keyTextData = new GridData(SWT.FILL, SWT.DEFAULT, true,
					false);
			keyTextData.exclude = true;
			keyTextData.heightHint = 100;
			keyText.setLayoutData(keyTextData);

			KeyAdapter completeListener = new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					enableDisableFinish();
				}
			};

			uriText.addKeyListener(completeListener);
			userText.addKeyListener(completeListener);
			passText.addKeyListener(completeListener);
			keyText.addKeyListener(completeListener);

			authCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int authRequested = getAuthRequested();
					hideAll();
					if (authRequested == AUTH_USER_PASS) {
						userLabelData.exclude = false;
						userLabel.setVisible(true);
						userTextData.exclude = false;
						userText.setVisible(true);

						passLabelData.exclude = false;
						passLabel.setVisible(true);
						passTextData.exclude = false;
						passText.setVisible(true);
					} else if (authRequested == AUTH_SSH_PUBLIC_KEY) {
						keyLabelData.exclude = false;
						keyLabel.setVisible(true);
						keyTextData.exclude = false;
						keyText.setVisible(true);
					}
					localComposite.layout(false);
					enableDisableFinish();
				}

				private void hideAll() {
					userLabelData.exclude = true;
					userLabel.setVisible(false);
					userTextData.exclude = true;
					userText.setVisible(false);

					passLabelData.exclude = true;
					passLabel.setVisible(false);
					passTextData.exclude = true;
					passText.setVisible(false);

					keyLabelData.exclude = true;
					keyLabel.setVisible(false);
					keyTextData.exclude = true;
					keyText.setVisible(false);
				}
			});

			uriText.setText("/home/me/SW/EGIT");
			setControl(localComposite);
			setPageComplete(false);
		}

		private void enableDisableFinish() {
			boolean isComplete = false;
			if (uriText.getText().length() > 0) {
				isComplete = true;
			}
			int authRequested = getAuthRequested();
			if (authRequested == AUTH_USER_PASS) {
				if (userText.getText().length() == 0
						|| passText.getText().length() == 0) {
					isComplete = false;
				}
			} else if (authRequested == AUTH_SSH_PUBLIC_KEY) {
				if (keyText.getText().length() == 0) {
					isComplete = false;
				}
			}
			setPageComplete(isComplete);
		}

		@Override
		public Control getControl() {
			return localComposite;
		}

		/**
		 * Returns authentication method entered in the Wizard page.
		 *
		 * @return String the authentication method chosen: <code>AUTH_NONE</code>,
		 *         <code>AUTH_USER_PASS</code> or <code>AUTH_SSH_PUBLIC_KEY</code>.
		 */
		public int getAuthRequested() {
			String text = authCombo.getText();
			if (authItems[1].equals(text)) {
				return AUTH_USER_PASS;
			} else if (authItems[2].equals(text)) {
				return AUTH_SSH_PUBLIC_KEY;
			}

			return AUTH_NONE;
		}

		/**
		 * Returns the URI entered in the Wizard page.
		 *
		 * @return String the URI entered in the Wizard page.
		 */
		public String getUri() {
			return uriText.getText();
		}

		/**
		 * Returns the user entered in the Wizard page.
		 *
		 * @return String the user entered in the Wizard page.
		 */
		public String getUser() {
			if (getAuthRequested() == AUTH_USER_PASS)
				return userText.getText();
			return null;
		}

		/**
		 * Returns the password entered in the Wizard page.
		 *
		 * @return String the password entered in the Wizard page.
		 */
		public String getPassword() {
			if (getAuthRequested() == AUTH_USER_PASS)
				return passText.getText();
			return null;
		}

		/**
		 * Returns the SSH public key entered in the Wizard page.
		 *
		 * @return String the SSH public key entered in the Wizard page.
		 */
		public String getSshPublicKey() {
			if (getAuthRequested() == AUTH_SSH_PUBLIC_KEY)
				return keyText.getText();
			return null;
		}

		@Override
		public boolean isPageComplete() {
			try {
				URIish uri = new URIish(getUri());
				if (uri.getScheme() == null) {
					if (getAuthRequested() != AUTH_NONE) {
						setErrorMessage("local protocol does not support authentication");
						return false;
					}
					setErrorMessage(null);
					return true;
				}
				if (uri.getScheme().equals("file")) {
					if (getAuthRequested() != AUTH_NONE) {
						setErrorMessage("file protocol does not support authentication");
						return false;
					}
					setErrorMessage(null);
					return true;
				}
				if (uri.getScheme().equals("git")) {
					if (getAuthRequested() != AUTH_NONE) {
						setErrorMessage("git protocol does not support authentication");
						return false;
					}
					setErrorMessage(null);
					return true;
				}
				if (uri.getScheme().equals("git+ssh") || uri.getScheme().equals("ssh")) {
					if (uri.getUser() != null)
						if (!userText.getText().equals(""))
							userText.setText(uri.getUser());
					if (uri.getPass() != null)
						if (!passText.getText().equals(""))
							passText.setText(uri.getPass());
					if (getAuthRequested() == AUTH_NONE && (uri.getUser() == null || uri.getPass() == null)) {
						setErrorMessage("Warning: Username, password or key missing");
					} else {
						setErrorMessage(null);
					}
					return true;
				}

				setErrorMessage(uri.getScheme() + " is not a supported protocol");
				return false;

			} catch (URISyntaxException e) {
				setErrorMessage(e.getReason());
				return false;

			} catch (Exception e) {
				Activator.logError("Internal parameter problem in "+getClass().getName(), e);
				setErrorMessage("Internal error, see log");
				return false;

			}
		}
	}

	class DoClonePage extends WizardPage {

		private final CloneInputPage cloneInput;

		DoClonePage(CloneInputPage cloneInput) {
			super("Cloning");
			this.cloneInput = cloneInput;
		}

		private Composite localComposite;

		public void createControl(Composite parent) {
			localComposite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			localComposite.setLayout(layout);
			setControl(localComposite);
		}

		@Override
		public boolean canFlipToNextPage() {
			try {
				return performClone();
			} catch (CoreException e) {
				Activator.logError(e.getLocalizedMessage(), e);
				setErrorMessage(e.getLocalizedMessage());
				return false;
			}
		}

		boolean performClone() throws CoreException {
			String urish = cloneInput.getUri();
			String name = new File(urish).getName();
			if (name.endsWith(".git"))
				name = name.substring(0, name.length() - 4);
			else
				urish = urish+"/.git";
			final File into = new File(ResourcesPlugin.getWorkspace().getRoot().getRawLocation().toFile(), name);
			System.out.println("Cloning into "+into);
			setMessage("Cloning into "+into.toString(), IMessageProvider.INFORMATION);
			if (!into.exists() && !into.mkdirs()) {
				setErrorMessage("Could not create destination directory");
				return false;
			}

			Repository db = null;
			try {
				db = new Repository(new File(into, ".git"));
				db.create();
				FetchClient client = createClient(urish, db, cloneInput.getUser(), cloneInput.getPassword());
				CloneJob cloneJob = new CloneJob(client);
				getContainer().run(true, true, cloneJob);
				return true;

			} catch (InterruptedException e) {
				destroyPartialClone(db);
				return false;

			} catch (InvocationTargetException e) {
				Activator.logError("Problem in clone", e);
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				destroyPartialClone(db);
				return false;

			} catch (IOException e) {
				Activator.logError("Problem in clone", e);
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				destroyPartialClone(db);
				return false;

			} catch (URISyntaxException e) {
				Activator.logError("Problem in clone", e);
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				destroyPartialClone(db);
				return false;

			} catch (Exception e) {
				Activator.logError("Problem in clone", e);
				MessageDialog.openError(getShell(), "Error", e.getMessage());
				destroyPartialClone(db);
				return false;
			}
		}

		private void destroyPartialClone(final Repository db) throws CoreException {
			File dir = db.getDirectory();
			if (dir.equals(".git"))
				dir = dir.getParentFile();
			delete(dir);
		}

		private void delete(final File dir) throws CoreException {
			if (dir.isDirectory()) {
				for (File f : dir.listFiles()) {
					delete(f);
				}
			} else {
				if (!dir.delete()) {
					throw new CoreException(new Status(IStatus.ERROR, Activator.getPluginId(), "Could not delete directory" + dir));
				}
			}
		}

		private FetchClient createClient(final String urish, final Repository db,
				final String user, final String password)
		throws IOException, URISyntaxException, JSchException {
			URIish uri = new URIish(urish);
			if (uri.getScheme() == null)
				return LocalGitProtocolFetchClient.create(db, "origin", uri.getPath());
			if (uri.getScheme().equals("git")) {
				int port = uri.getPort();
				if (port == -1)
					port = GitProtocolFetchClient.GIT_PROTO_PORT;
				return GitProtocolFetchClient.create(db, "origin", uri.getHost(), port, uri.getPath());
			}
			if (uri.getScheme().equals("ssh") || uri.getScheme().equals("git+ssh")) {
				int port = uri.getPort();
				if (port == -1)
					port = GitSshProtocolFetchClient.GIT_SSH_PROTO_PORT;
				return GitSshProtocolFetchClient.create(db, "origin", uri.getHost(), port,
						user != null ? user : uri.getUser(),
						password != null ? password : uri.getPass(),
						uri.getPath());
			}
			throw new IOException("Unsupported fetch protocol: " + uri.getScheme());
		}

		class CloneJob implements IRunnableWithProgress {

			private final FetchClient client;

			CloneJob(final FetchClient client) {
				this.client = client;
			}

			public void run(final IProgressMonitor monitor) {
				try {
					client.run(new EclipseGitProgressTransformer(monitor));
					System.out.println("Checking out");
					monitor.setTaskName("Checking out");
					Repository repository = client.getRepository();
					WorkDirCheckout workDirCheckout = new WorkDirCheckout(repository, repository.getDirectory().getParentFile(), new GitIndex(client.getRepository()), repository.mapTree("remotes/origin/master"));
					workDirCheckout.checkout();
					System.out.println("Done");
				} catch (IOException e) {
					setErrorMessage(e.getMessage());
				} finally {
					monitor.done();
				}
			}
		}
	}

	class EclipseGitProgressTransformer implements ProgressMonitor {
		private final IProgressMonitor eclipseMonitor;
		private String task;
		private boolean done;
		private int total;
		private int worked;

		EclipseGitProgressTransformer(final IProgressMonitor eclipseMonitor) {
			this.eclipseMonitor = eclipseMonitor;
		}

		public void worked(final int work) {
			worked += work;
			eclipseMonitor.worked(work);
		}

		public void setTotalWork(final int work) {
			total = work;
			eclipseMonitor.beginTask(getMessage(), work);
		}

		public void setMessage(final String message) {
			task = message;
			eclipseMonitor.setTaskName(message);
		}

		public void setCanceled(final boolean canceled) {
			eclipseMonitor.setCanceled(true);
		}

		public boolean isDone() {
			return done;
		}

		public boolean isCancelled() {
			return eclipseMonitor.isCanceled();
		}

		public int getWorked() {
			return worked;
		}

		public int getTotal() {
			return total;
		}

		public String getMessage() {
			return task;
		}

		public void done() {
			done = true;
			eclipseMonitor.done();
		}

		public void beginTask(final String task, final int total) {
			this.task = task;
			this.total = total;
			eclipseMonitor.beginTask(task, total);
		}
	}
}

