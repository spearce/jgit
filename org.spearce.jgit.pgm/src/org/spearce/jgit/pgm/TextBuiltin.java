/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
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

package org.spearce.jgit.pgm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;

/**
 * Abstract command which can be invoked from the command line.
 * <p>
 * Commands are configured with a single "current" repository and then the
 * {@link #execute(String[])} method is invoked with the arguments that appear
 * on the command line after the command name.
 * <p>
 * Command constructors should perform as little work as possible as they may be
 * invoked very early during process loading, and the command may not execute
 * even though it was constructed.
 */
public abstract class TextBuiltin {
	protected static final String REFS_HEADS = Constants.HEADS_PREFIX + "/";

	protected static final String REFS_REMOTES = Constants.REMOTES_PREFIX + "/";

	protected static final String REFS_TAGS = Constants.TAGS_PREFIX + "/";

	private String commandName;

	/** Stream to output to, typically this is standard output. */
	protected PrintWriter out;

	/** Git repository the command was invoked within. */
	protected Repository db;

	/**
	 * Set the name this command can be invoked as on the command line.
	 *
	 * @param name
	 *            the name of the command.
	 */
	public void setCommandName(final String name) {
		commandName = name;
	}

	void init(final Repository repo) {
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					System.out, "UTF-8")));
		} catch (IOException e) {
			throw die("cannot create output stream");
		}
		db = repo;
	}

	/**
	 * Perform the action(s) of this command.
	 *
	 * @param args
	 *            command line arguments passed after the command name.
	 * @throws Exception
	 *             an error occurred while processing the command. The main
	 *             framework will catch the exception and print a message on
	 *             standard error.
	 */
	public abstract void execute(String[] args) throws Exception;

	/**
	 * @return the repository this command accesses.
	 */
	public Repository getRepository() {
		return db;
	}

	protected ObjectId resolve(final String s) throws IOException {
		final ObjectId r = db.resolve(s);
		if (r == null)
			throw die("Not a revision: " + s);
		return r;
	}

	protected static Die die(final String why) {
		return new Die(why);
	}

	protected static String abbreviateObject(final ObjectId id) {
		return id.toString().substring(0, 7);
	}

	protected String abbreviateRef(String dst, boolean abbreviateRemote) {
		if (dst.startsWith(REFS_HEADS))
			dst = dst.substring(REFS_HEADS.length());
		else if (dst.startsWith(REFS_TAGS))
			dst = dst.substring(REFS_TAGS.length());
		else if (abbreviateRemote && dst.startsWith(REFS_REMOTES))
			dst = dst.substring(REFS_REMOTES.length());
		return dst;
	}
}
