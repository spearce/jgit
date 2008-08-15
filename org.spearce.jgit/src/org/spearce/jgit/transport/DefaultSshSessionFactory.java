/*
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

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
	static final int SSH_PORT = 22;

	private Set<String> loadedIdentities;

	private JSch userJSch;

	private OpenSshConfig config;

	@Override
	public synchronized Session getSession(String user, String pass,
			String host, int port) throws JSchException {
		final OpenSshConfig.Host hc = getConfig().lookup(host);
		host = hc.getHostName();
		if (port <= 0)
			port = hc.getPort();
		if (user == null)
			user = hc.getUser();

		final Session session = getUserJSch().getSession(user, host, port);
		if (hc.getIdentityFile() != null)
			addIdentity(hc.getIdentityFile());
		if (pass != null)
			session.setPassword(pass);
		else
			session.setUserInfo(new AWT_UserInfo());
		return session;
	}

	static String userName() {
		return AccessController.doPrivileged(new PrivilegedAction<String>() {
			public String run() {
				return System.getProperty("user.name");
			}
		});
	}

	private JSch getUserJSch() throws JSchException {
		if (userJSch == null) {
			loadedIdentities = new HashSet<String>();
			userJSch = new JSch();
			knownHosts(userJSch);
			identities();
		}
		return userJSch;
	}

	private OpenSshConfig getConfig() {
		if (config == null)
			config = OpenSshConfig.get();
		return config;
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

	private void identities() throws JSchException {
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
			addIdentity(k);
		}
	}

	private void addIdentity(final File identityFile) throws JSchException {
		final String path = identityFile.getAbsolutePath();
		if (loadedIdentities.add(path))
			userJSch.addIdentity(path);
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

	@Override
	public OutputStream getErrorStream() {
		return new OutputStream() {
			private StringBuilder all = new StringBuilder();

			private StringBuilder sb = new StringBuilder();

			public String toString() {
				String r = all.toString();
				while (r.endsWith("\n"))
					r = r.substring(0, r.length() - 1);
				return r;
			}

			@Override
			public void write(final int b) throws IOException {
				if (b == '\r') {
					System.err.print('\r');
					return;
				}

				sb.append((char) b);

				if (b == '\n') {
					final String line = sb.toString();
					System.err.print(line);
					all.append(line);
					sb = new StringBuilder();
				}
			}
		};
	}
}
