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
package org.spearce.egit.ui.internal.clone;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIIcons;
import org.spearce.egit.ui.UIText;
import org.spearce.jgit.transport.URIish;
import org.spearce.jgit.util.FS;

/**
 * Wizard page that allows the user entering the location of a repository to be
 * cloned.
 */
class CloneSourcePage extends WizardPage {
	private static final String[] DEFAULT_SCHEMES = { "git", "git+ssh", "file" };

	private final List<URIishChangeListener> uriishChangeListeners;

	private Group authGroup;

	private Text uriText;

	private Text hostText;

	private Text pathText;

	private Text userText;

	private Text passText;

	private Combo scheme;

	private Text portText;

	private int eventDepth;

	private URIish uri = new URIish();

	CloneSourcePage() {
		super(CloneSourcePage.class.getName());
		setTitle(UIText.CloneSourcePage_title);
		setDescription(UIText.CloneSourcePage_description);
		setImageDescriptor(UIIcons.WIZBAN_IMPORT_REPO);
		uriishChangeListeners = new ArrayList<URIishChangeListener>(4);
	}

	void addURIishChangeListener(final URIishChangeListener l) {
		uriishChangeListeners.add(l);
	}

	public void createControl(final Composite parent) {
		final Composite panel = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		createLocationGroup(panel);
		createConnectionGroup(panel);
		authGroup = createAuthenticationGroup(panel);

		updateAuthGroup();
		setControl(panel);
		setPageComplete(false);
	}

	private void createLocationGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneSourcePage_groupLocation);

		newLabel(g, UIText.CloneSourcePage_promptURI + ":");
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
						scheme.select(2);
					else if (isSSH(u))
						scheme.select(1);
					else
						scheme.select(0);
					updateAuthGroup();
					uri = u;
					for (final URIishChangeListener l : uriishChangeListeners)
						l.uriishChanged(u);
					setPageComplete(isPageComplete());
				} catch (URISyntaxException err) {
					uriInvalid();
					setErrorMessage(err.getMessage());
					setPageComplete(false);
				} finally {
					eventDepth--;
				}
			}
		});

		newLabel(g, UIText.CloneSourcePage_promptHost + ":");
		hostText = new Text(g, SWT.BORDER);
		hostText.setLayoutData(createFieldGridData());
		hostText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setHost(nullString(hostText.getText())));
			}
		});

		newLabel(g, UIText.CloneSourcePage_promptPath + ":");
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
				UIText.CloneSourcePage_groupAuthentication);

		newLabel(g, UIText.CloneSourcePage_promptUser + ":");
		userText = new Text(g, SWT.BORDER);
		userText.setLayoutData(createFieldGridData());
		userText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				setURI(uri.setUser(nullString(userText.getText())));
			}
		});

		newLabel(g, UIText.CloneSourcePage_promptPassword + ":");
		passText = new Text(g, SWT.BORDER | SWT.PASSWORD);
		passText.setLayoutData(createFieldGridData());
		return g;
	}

	private void createConnectionGroup(final Composite parent) {
		final Group g = createGroup(parent,
				UIText.CloneSourcePage_groupConnection);

		newLabel(g, UIText.CloneSourcePage_promptScheme + ":");
		scheme = new Combo(g, SWT.DROP_DOWN | SWT.READ_ONLY);
		scheme.setItems(DEFAULT_SCHEMES);
		scheme.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(final SelectionEvent e) {
				// Nothing
			}

			public void widgetSelected(final SelectionEvent e) {
				final int idx = scheme.getSelectionIndex();
				if (idx < 0)
					setURI(uri.setScheme(null));
				else
					setURI(uri.setScheme(nullString(scheme.getItem(idx))));
				updateAuthGroup();
			}
		});

		newLabel(g, UIText.CloneSourcePage_promptPort + ":");
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
						uriInvalid();
					}
				}
			}
		});
	}

	private static Group createGroup(final Composite parent, final String text) {
		final Group g = new Group(parent, SWT.BORDER);
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

	/**
	 * Returns the URI entered in the Wizard page.
	 * 
	 * @return the URI entered in the Wizard page.
	 * @throws URISyntaxException
	 */
	public URIish getURI() throws URISyntaxException {
		return new URIish(uriText.getText());
	}

	@Override
	public boolean isPageComplete() {
		if (uriText.getText().length() == 0) {
			setErrorMessage(null);
			return false;
		}

		try {
			final URIish finalURI = getURI();
			String proto = finalURI.getScheme();
			if (proto == null && scheme.getSelectionIndex() >= 0)
				proto = scheme.getItem(scheme.getSelectionIndex());

			if (uri.getPath() == null) {
				uriInvalid();
				setErrorMessage(NLS.bind(UIText.CloneSourcePage_fieldRequired,
						UIText.CloneSourcePage_promptPath, proto));
				return false;
			}

			if (isFile(finalURI)) {
				String badField = null;
				if (uri.getHost() != null)
					badField = UIText.CloneSourcePage_promptHost;
				else if (uri.getUser() != null)
					badField = UIText.CloneSourcePage_promptUser;
				else if (uri.getPass() != null)
					badField = UIText.CloneSourcePage_promptPassword;
				if (badField != null) {
					uriInvalid();
					setErrorMessage(NLS.bind(
							UIText.CloneSourcePage_fieldNotSupported, badField,
							proto));
					return false;
				}

				final File d = FS.resolve(new File("."), uri.getPath());
				if (!d.exists()) {
					setErrorMessage(NLS.bind(
							UIText.CloneSourcePage_fileNotFound, d
									.getAbsolutePath()));
					return false;
				}
				setErrorMessage(null);
				return true;
			}

			if (uri.getHost() == null) {
				uriInvalid();
				setErrorMessage(NLS.bind(UIText.CloneSourcePage_fieldRequired,
						UIText.CloneSourcePage_promptHost, proto));
				return false;
			}

			if (!isSSH(finalURI)) {
				String badField = null;
				if (uri.getUser() != null)
					badField = UIText.CloneSourcePage_promptUser;
				else if (uri.getPass() != null)
					badField = UIText.CloneSourcePage_promptPassword;
				if (badField != null) {
					uriInvalid();
					setErrorMessage(NLS.bind(
							UIText.CloneSourcePage_fieldNotSupported, badField,
							proto));
					return false;
				}
			}

			setErrorMessage(null);
			return true;
		} catch (URISyntaxException e) {
			uriInvalid();
			setErrorMessage(e.getReason());
			return false;
		} catch (Exception e) {
			uriInvalid();
			Activator.logError("Error validating " + getClass().getName(), e);
			setErrorMessage(UIText.CloneSourcePage_internalError);
			return false;
		}
	}

	private static boolean isFile(final URIish uri) {
		if ("file".equals(uri.getScheme()))
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

	private void setURI(final URIish u) {
		try {
			eventDepth++;
			if (eventDepth == 1) {
				for (final URIishChangeListener l : uriishChangeListeners)
					l.uriishChanged(u);
				uri = u;
				uriText.setText(uri.toString());
				setPageComplete(isPageComplete());
			}
		} finally {
			eventDepth--;
		}
	}

	private void updateAuthGroup() {
		switch (scheme.getSelectionIndex()) {
		case 0:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			authGroup.setEnabled(false);
			break;
		case 1:
			hostText.setEnabled(true);
			portText.setEnabled(true);
			authGroup.setEnabled(true);
			break;
		case 2:
			hostText.setEnabled(false);
			portText.setEnabled(false);
			authGroup.setEnabled(false);
			break;
		}
	}

	private void uriInvalid() {
		for (final URIishChangeListener l : uriishChangeListeners)
			l.uriishChanged(null);
	}
}