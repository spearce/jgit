/*
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.transport;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.spearce.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/**
 * Loads known hosts and private keys from <code>$HOME/.ssh</code>.
 * <p>
 * This is the default implementation used by JGit and provides most of the
 * compatibility necessary to match OpenSSH, a popular implementation of SSH
 * used by C Git.
 * <p>
 * If user interactivity is required by SSH (e.g. to obtain a password) AWT is
 * used to display a password input field to the end-user.
 */
class DefaultSshSessionFactory extends SshSessionFactory {
	/** IANA assigned port number for SSH. */
	private static final int SSH_PORT = 22;

	private JSch userJSch;

	@Override
	public synchronized Session getSession(String user, String pass,
			String host, int port) throws JSchException {
		if (port <= 0)
			port = SSH_PORT;
		if (user == null)
			user = userName();

		final Session session = getUserJSch().getSession(user, host, port);
		if (pass != null)
			session.setPassword(pass);
		else
			session.setUserInfo(new AWT_UserInfo());
		return session;
	}

	private static String userName() {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("user.name");
			}
		});
	}

	private JSch getUserJSch() throws JSchException {
		if (userJSch == null) {
			final JSch sch = new JSch();
			knownHosts(sch);
			identities(sch);
			userJSch = sch;
		}
		return userJSch;
	}

	private void knownHosts(final JSch sch) throws JSchException {
		final File home = FS.userHome();
		if (home == null)
			return;
		final File known_hosts = new File(new File(home, ".ssh"), "known_hosts");
		try {
			final FileInputStream in = new FileInputStream(known_hosts);
			try {
				sch.setKnownHosts(in);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException none) {
			// Oh well. They don't have a known hosts in home.
		} catch (IOException err) {
			// Oh well. They don't have a known hosts in home.
		}
	}

	private void identities(final JSch sch) throws JSchException {
		final File home = FS.userHome();
		if (home == null)
			return;
		final File sshdir = new File(home, ".ssh");
		final File[] keys = sshdir.listFiles();
		if (keys == null)
			return;
		for (int i = 0; i < keys.length; i++) {
			final File pk = keys[i];
			final String n = pk.getName();
			if (!n.endsWith(".pub"))
				continue;
			final File k = new File(sshdir, n.substring(0, n.length() - 4));
			if (!k.isFile())
				continue;
			sch.addIdentity(k.getAbsolutePath());
		}
	}

	private static class AWT_UserInfo implements UserInfo,
			UIKeyboardInteractive {
		private String passwd;

		private String passphrase;

		public void showMessage(final String msg) {
			JOptionPane.showMessageDialog(null, msg);
		}

		public boolean promptYesNo(final String msg) {
			return JOptionPane.showConfirmDialog(null, msg, "Warning",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
		}

		public boolean promptPassword(final String msg) {
			passwd = null;
			final JPasswordField passwordField = new JPasswordField(20);
			final int result = JOptionPane.showConfirmDialog(null,
					new Object[] { passwordField }, msg,
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passwd = new String(passwordField.getPassword());
				return true;
			}
			return false;
		}

		public boolean promptPassphrase(final String msg) {
			passphrase = null;
			final JPasswordField passwordField = new JPasswordField(20);
			final int result = JOptionPane.showConfirmDialog(null,
					new Object[] { passwordField }, msg,
					JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passphrase = new String(passwordField.getPassword());
				return true;
			}
			return false;
		}

		public String getPassword() {
			return passwd;
		}

		public String getPassphrase() {
			return passphrase;
		}

		public String[] promptKeyboardInteractive(final String destination,
				final String name, final String instruction,
				final String[] prompt, final boolean[] echo) {
			final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1,
					1, 1, GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
			final Container panel = new JPanel();
			panel.setLayout(new GridBagLayout());

			gbc.weightx = 1.0;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.gridx = 0;
			panel.add(new JLabel(instruction), gbc);
			gbc.gridy++;

			gbc.gridwidth = GridBagConstraints.RELATIVE;

			final JTextField[] texts = new JTextField[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				gbc.fill = GridBagConstraints.NONE;
				gbc.gridx = 0;
				gbc.weightx = 1;
				panel.add(new JLabel(prompt[i]), gbc);

				gbc.gridx = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weighty = 1;
				if (echo[i]) {
					texts[i] = new JTextField(20);
				} else {
					texts[i] = new JPasswordField(20);
				}
				panel.add(texts[i], gbc);
				gbc.gridy++;
			}

			if (JOptionPane.showConfirmDialog(null, panel, destination + ": "
					+ name, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
				String[] response = new String[prompt.length];
				for (int i = 0; i < prompt.length; i++) {
					response[i] = texts[i].getText();
				}
				return response;
			}
			return null; // cancel
		}
	}
}
