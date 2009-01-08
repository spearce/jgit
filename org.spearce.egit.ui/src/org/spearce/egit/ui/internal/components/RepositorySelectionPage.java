/*******************************************************************************
 * Copyright (C) 2007, 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.URIish;
import org.spearce.jgit.util.FS;

/**
 * Wizard page that allows the user entering the location of a remote repository
 * by specifying URL manually or selecting a preconfigured remote repository.
 */
public class RepositorySelectionPage extends BaseWizardPage {
	private static final int REMOTE_CONFIG_TEXT_MAX_LENGTH = 80;

	private static final String DEFAULT_REMOTE_NAME = "origin";

	private static final int S_GIT = 0;

	private static final int S_SSH = 1;

	private static final int S_SFTP = 2;

	private static final int S_HTTP = 3;

	private static final int S_HTTPS = 4;

	private static final int S_FTP = 5;

	private static final int S_FILE = 6;

	private static final String[] DEFAULT_SCHEMES;
	static {
		DEFAULT_SCHEMES = new String[7];
		DEFAULT_SCHEMES[S_GIT] = "git";
		DEFAULT_SCHEMES[S_SSH] = "git+ssh";
		DEFAULT_SCHEMES[S_SFTP] = "sftp";
		DEFAULT_SCHEMES[S_HTTP] = "http";
		DEFAULT_SCHEMES[S_HTTPS] = "https";
		DEFAULT_SCHEMES[S_FTP] = "ftp";
		DEFAULT_SCHEMES[S_FILE] = "file";
	}

	private static void setEnabledRecursively(final Control control,
			final boolean enable) {
		control.setEnabled(enable);
		if (control instanceof Composite)
			for (final Control child : ((Composite) control).getChildren())
				setEnabledRecursively(child, enable);
	}

	private final List<RemoteConfig> configuredRemotes;

	private Group authGroup;

	private Text uriText;

	private Text hostText;

	private Text pathText;

	private Text userText;

	private Text passText;

	private Combo scheme;

	private Text portText;

	private int eventDepth;

	private URIish uri;

	private RemoteConfig remoteConfig;

	private RepositorySelection selection;

	private Composite remotePanel;

	private Button remoteButton;

	private Combo remoteCombo;

	private Composite uriPanel;

	private Button uriButton;

	/**
	 * Create repository selection page, allowing user specifying URI or
	 * (optionally) choosing from preconfigured remotes list.
	 * <p>
	 * Wizard page is created without image, just with text description.
	 *
	 * @param sourceSelection
	 *            true if dialog is used for source selection; false otherwise
	 *            (destination selection). This indicates appropriate text
	 *            messages.
	 * @param configuredRemotes
	 *            list of configured remotes that user may select as an
	 *            alternative to manual URI specification. Remotes appear in
	 *            given order in GUI, with {@value #DEFAULT_REMOTE_NAME} as the
	 *            default choice. List may be null or empty - no remotes
	 *            configurations appear in this case. Note that the provided
	 *            list may be changed by this constructor.
	 */
	public RepositorySelectionPage(final boolean sourceSelection,
			final List<RemoteConfig> configuredRemotes) {
		super(RepositorySelectionPage.class.getName());
		this.uri = new URIish();

		if (configuredRemotes != null)
			removeUnusableRemoteConfigs(configuredRemotes);
		if (configuredRemotes == null || configuredRemotes.isEmpty())
			this.configuredRemotes = null;
		else {
			this.configuredRemotes = configuredRemotes;
			this.remoteConfig = selectDefaultRemoteConfig();
		}
		selection = RepositorySelection.INVALID_SELECTION;

		if (sourceSelection) {
			setTitle(UIText.RepositorySelectionPage_sourceSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_sourceSelectionDescription);
		} else {
			setTitle(UIText.RepositorySelectionPage_destinationSelectionTitle);
			setDescription(UIText.RepositorySelectionPage_destinationSelectionDescription);
		}
	}

	/**
	 * Create repository selection page, allowing user specifying URI, with no
	 * preconfigured remotes selection.
	 *
	 * @param sourceSelection
	 *            true if dialog is used for source selection; false otherwise
	 *            (destination selection). This indicates appropriate text
	 *            messages.
	 */
	public RepositorySelectionPage(final boolean sourceSelection) {
		this(sourceSelection, null);
	}

	/**
	 * @return repository selection representing current page state.
	 */
	public RepositorySelection getSelection() {
		return selection;
	}

	/**
	 * Compare current repository selection set by user to provided one.
	 *
	 * @param s
	 *            repository selection to compare.
	 * @return true if provided selection is equal to current page selection,
	 *         false otherwise.
	 */
	public boolean selectionEquals(final RepositorySelection s) {
		return selection.equals(s);
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		panel.setLayout(new GridLayout());

		if (configuredRemotes != null)
			createRemotePanel(panel);
		createUriPanel(panel);

		updateRemoteAndURIPanels();
		setControl(panel);
		checkPage();
	}

	private void createRemotePanel(final Composite parent) {
		remoteButton = new Button(parent, SWT.RADIO);
		remoteButton
				.setText(UIText.RepositorySelectionPage_configuredRemoteChoice
						+ ":");
		remoteButton.setSelection(true);

		remotePanel = new Composite(parent, SWT.NULL);
		remotePanel.setLayout(new GridLayout());
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		remotePanel.setLayoutData(gd);

		remoteCombo = new Combo(remotePanel, SWT.READ_ONLY | SWT.DROP_DOWN);
		final String items[] = new String[configuredRemotes.size()];
		int i = 0;
		for (final RemoteConfig rc : configuredRemotes)
			items[i++] = getTextForRemoteConfig(rc);
		final int defaultIndex = configuredRemotes.indexOf(remoteConfig);
		remoteCombo.setItems(items);
		remoteCombo.select(defaultIndex);
		remoteCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int idx = remoteCombo.getSelectionIndex();
				remoteConfig = configuredRemotes.get(idx);
				checkPage();
			}
		});
	}

	private void createUriPanel(final Composite parent) {
		if (configuredRemotes != null) {
			uriButton = new Button(parent, SWT.RADIO);
			uriButton.setText(UIText.RepositorySelectionPage_uriChoice + ":");
			uriButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					// occurs either on selection or unselection event
					updateRemoteAndURIPanels();
					checkPage();
				}
			});
		}

		uriPanel = new Composite(parent, SWT.NULL);
		uriPanel.setLayout(new GridLayout());
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		uriPanel.setLayoutData(gd);

		createLocationGroup(uriPanel);
		createConnectionGroup(uriPanel);
		authGroup = createAuthenticationGroup(uriPanel);
	}

	private void createLocationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupLocation);

		newLabel(g, UIText.RepositorySelectionPage_promptURI + ":");
		uriText = new Text(g, SWT.BORDER);
		uriText.setLayoutData(createFieldGridData());
		uriText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				try {
					eventDepth++;
					if (eventDepth != 1)
						return;

					final URIish u = new URIish(uriText.getText());
					safeSet(hostText, u.getHost());
					safeSet(pathText, u.getPath());
					safeSet(userText, u.getUser());
					safeSet(passText, u.getPass());

					if (u.getPort() > 0)
						portText.setText(Integer.toString(u.getPort()));
					else
						portText.setText("");

					if (isFile(u))
						scheme.select(S_FILE);
					else if (isSSH(u))
						scheme.select(S_SSH);
					else {
						for (int i = 0; i < DEFAULT_SCHEMES.length; i++) {
							if (DEFAULT_SCHEMES[i].equals(u.getScheme())) {
								scheme.select(i);
								break;
							}
						}
					}

					updateAuthGroup();
					uri = u;
				} catch (URISyntaxException err) {
					// leave uriText as it is, but clean up underlying uri and
					// decomposed fields
					uri = new URIish();
					hostText.setText("");
					pathText.setText("");
					userText.setText("");
					passText.setText("");
					portText.setText("");
					scheme.select(0);
				} finally {
					eventDepth--;
				}
				checkPage();
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptHost + ":");
		hostText = new Text(g, SWT.BORDER);
		hostText.setLayoutData(createFieldGridData());
		hostText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setHost(nullString(hostText.getText())));
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPath + ":");
		pathText = new Text(g, SWT.BORDER);
		pathText.setLayoutData(createFieldGridData());
		pathText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setPath(nullString(pathText.getText())));
			}
		});
	}

	private Group createAuthenticationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupAuthentication);

		newLabel(g, UIText.RepositorySelectionPage_promptUser + ":");
		userText = new Text(g, SWT.BORDER);
		userText.setLayoutData(createFieldGridData());
		userText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setUser(nullString(userText.getText())));
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPassword + ":");
		passText = new Text(g, SWT.BORDER | SWT.PASSWORD);
		passText.setLayoutData(createFieldGridData());
		return g;
	}

	private void createConnectionGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.RepositorySelectionPage_groupConnection);

		newLabel(g, UIText.RepositorySelectionPage_promptScheme + ":");
		scheme = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		scheme.setItems(DEFAULT_SCHEMES);
		scheme.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final int idx = scheme.getSelectionIndex();
				if (idx < 0)
					setURI(uri.setScheme(null));
				else
					setURI(uri.setScheme(nullString(scheme.getItem(idx))));
				updateAuthGroup();
			}
		});

		newLabel(g, UIText.RepositorySelectionPage_promptPort + ":");
		portText = new Text(g, SWT.BORDER);
		portText.addVerifyListener(new VerifyListener() {
			final Pattern p = Pattern.compile("^(?:[1-9][0-9]*)?$");

			public void verifyText(final VerifyEvent e) {
				final String v = portText.getText();
				e.doit = p.matcher(
						v.substring(0, e.start) + e.text + v.substring(e.end))
						.matches();
			}
		});
		portText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				final String val = nullString(portText.getText());
				if (val == null)
					setURI(uri.setPort(-1));
				else {
					try {
						setURI(uri.setPort(Integer.parseInt(val)));
					} catch (NumberFormatException err) {
						// Ignore it for now.
					}
				}
			}
		});
	}

	private static Group createGroup(final Composite parent, final String text) {
		final Group g = new Group(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		g.setLayout(layout);
		g.setText(text);
		final GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		g.setLayoutData(gd);
		return g;
	}

	private static void newLabel(final Group g, final String text) {
		new Label(g, SWT.NULL).setText(text);
	}

	private static GridData createFieldGridData() {
		return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
	}

	private static boolean isGIT(final URIish uri) {
		return "git".equals(uri.getScheme());
	}

	private static boolean isFile(final URIish uri) {
		if ("file".equals(uri.getScheme()) || uri.getScheme() == null)
			return true;
		if (uri.getHost() != null || uri.getPort() > 0 || uri.getUser() != null
				|| uri.getPass() != null || uri.getPath() == null)
			return false;
		if (uri.getScheme() == null)
			return FS.resolve(new File("."), uri.getPath()).isDirectory();
		return false;
	}

	private static boolean isSSH(final URIish uri) {
		if (!uri.isRemote())
			return false;
		final String scheme = uri.getScheme();
		if ("ssh".equals(scheme))
			return true;
		if ("ssh+git".equals(scheme))
			return true;
		if ("git+ssh".equals(scheme))
			return true;
		if (scheme == null && uri.getHost() != null && uri.getPath() != null)
			return true;
		return false;
	}

	private static String nullString(final String value) {
		if (value == null)
			return null;
		final String v = value.trim();
		return v.length() == 0 ? null : v;
	}

	private static void safeSet(final Text text, final String value) {
		text.setText(value != null ? value : "");
	}

	private boolean isURISelected() {
		return configuredRemotes == null || uriButton.getSelection();
	}

	private void setURI(final URIish u) {
		try {
			eventDepth++;
			if (eventDepth == 1) {
				uri = u;
				uriText.setText(uri.toString());
				checkPage();
			}
		} finally {
			eventDepth--;
		}
	}

	private static void removeUnusableRemoteConfigs(
			final List<RemoteConfig> remotes) {
		final Iterator<RemoteConfig> iter = remotes.iterator();
		while (iter.hasNext()) {
			final RemoteConfig rc = iter.next();
			if (rc.getURIs().isEmpty())
				iter.remove();
		}
	}

	private RemoteConfig selectDefaultRemoteConfig() {
		for (final RemoteConfig rc : configuredRemotes)
			if (getTextForRemoteConfig(rc) == DEFAULT_REMOTE_NAME)
				return rc;
		return configuredRemotes.get(0);
	}

	private static String getTextForRemoteConfig(final RemoteConfig rc) {
		final StringBuilder sb = new StringBuilder(rc.getName());
		sb.append(": ");
		boolean first = true;
		for (final URIish u : rc.getURIs()) {
			final String uString = u.toString();
			if (first)
				first = false;
			else {
				sb.append(", ");
				if (sb.length() + uString.length() > REMOTE_CONFIG_TEXT_MAX_LENGTH) {
					sb.append("...");
					break;
				}
			}
			sb.append(uString);
		}
		return sb.toString();
	}

	private void checkPage() {
		if (isURISelected()) {
			assert uri != null;
			if (uriText.getText().length() == 0) {
				selectionIncomplete(null);
				return;
			}

			try {
				final URIish finalURI = new URIish(uriText.getText());
				String proto = finalURI.getScheme();
				if (proto == null && scheme.getSelectionIndex() >= 0)
					proto = scheme.getItem(scheme.getSelectionIndex());

				if (uri.getPath() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptPath), proto));
					return;
				}

				if (isFile(finalURI)) {
					String badField = null;
					if (uri.getHost() != null)
						badField = UIText.RepositorySelectionPage_promptHost;
					else if (uri.getUser() != null)
						badField = UIText.RepositorySelectionPage_promptUser;
					else if (uri.getPass() != null)
						badField = UIText.RepositorySelectionPage_promptPassword;
					if (badField != null) {
						selectionIncomplete(NLS
								.bind(
										UIText.RepositorySelectionPage_fieldNotSupported,
										unamp(badField), proto));
						return;
					}

					final File d = FS.resolve(new File("."), uri.getPath());
					if (!d.exists()) {
						selectionIncomplete(NLS.bind(
								UIText.RepositorySelectionPage_fileNotFound, d
										.getAbsolutePath()));
						return;
					}

					selectionComplete(finalURI, null);
					return;
				}

				if (uri.getHost() == null) {
					selectionIncomplete(NLS.bind(
							UIText.RepositorySelectionPage_fieldRequired,
							unamp(UIText.RepositorySelectionPage_promptHost), proto));
					return;
				}

				if (isGIT(finalURI)) {
					String badField = null;
					if (uri.getUser() != null)
						badField = UIText.RepositorySelectionPage_promptUser;
					else if (uri.getPass() != null)
						badField = UIText.RepositorySelectionPage_promptPassword;
					if (badField != null) {
						selectionIncomplete(NLS
								.bind(
										UIText.RepositorySelectionPage_fieldNotSupported,
										unamp(badField), proto));
						return;
					}
				}

				selectionComplete(finalURI, null);
				return;
			} catch (URISyntaxException e) {
				selectionIncomplete(e.getReason());
				return;
			} catch (Exception e) {
				Activator.logError("Error validating " + getClass().getName(),
						e);
				selectionIncomplete(UIText.RepositorySelectionPage_internalError);
				return;
			}
		} else {
			assert remoteButton.getSelection();
			selectionComplete(null, remoteConfig);
			return;
		}
	}

	private String unamp(String s) {
		return s.replace("&","");
	}

	private void selectionIncomplete(final String errorMessage) {
		setExposedSelection(null, null);
		setErrorMessage(errorMessage);
		setPageComplete(false);
	}

	private void selectionComplete(final URIish u, final RemoteConfig rc) {
		setExposedSelection(u, rc);
		setErrorMessage(null);
		setPageComplete(true);
	}

	private void setExposedSelection(final URIish u, final RemoteConfig rc) {
		final RepositorySelection newSelection = new RepositorySelection(u, rc);
		if (newSelection.equals(selection))
			return;

		selection = newSelection;
		notifySelectionChanged();
	}

	private void updateRemoteAndURIPanels() {
		setEnabledRecursively(uriPanel, isURISelected());
		if (uriPanel.getEnabled())
			updateAuthGroup();
		if (configuredRemotes != null)
			setEnabledRecursively(remotePanel, !isURISelected());
	}

	private void updateAuthGroup() {
		switch (scheme.getSelectionIndex()) {
		case S_GIT:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			setEnabledRecursively(authGroup, false);
			break;
		case S_SSH:
		case S_SFTP:
		case S_HTTP:
		case S_HTTPS:
		case S_FTP:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			setEnabledRecursively(authGroup, true);
			break;
		case S_FILE:
			hostText.setEnabled(false);
			portText.setEnabled(false);
			setEnabledRecursively(authGroup, false);
			break;
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible)
			uriText.setFocus();
	}
}
