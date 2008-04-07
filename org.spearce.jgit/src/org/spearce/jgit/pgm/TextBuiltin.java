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
package org.spearce.jgit.pgm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;

abstract class TextBuiltin {
	protected PrintWriter out;

	protected Repository db;

	TextBuiltin() {
		try {
			out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					System.out, "UTF-8")));
		} catch (IOException e) {
			throw die("cannot create output stream");
		}
	}

	abstract void execute(String[] args) throws Exception;

	protected ObjectId resolve(final String s) throws IOException {
		final ObjectId r = db.resolve(s);
		if (r == null)
			throw die("Not a revision: " + s);
		return r;
	}

	protected static Die die(final String why) {
		return new Die(why);
	}
}
