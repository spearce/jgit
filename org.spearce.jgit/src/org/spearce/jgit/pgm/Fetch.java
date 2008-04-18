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

import java.io.File;

import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.FetchClient;
import org.spearce.jgit.transport.LocalGitProtocolFetchClient;

class Fetch extends TextBuiltin {
	@Override
	void execute(String[] args) throws Exception {
		final FetchClient fp;

		if (args.length == 0)
			args = new String[] { "origin" };

		final String rn = args[0];
		if (new File(rn).isDirectory())
			fp = LocalGitProtocolFetchClient.create(db, rn, new File(rn));
		else
			throw die("unsupported URL: " + rn);

		if (args.length > 1) {
			final String[] branches = new String[args.length - 1];
			System.arraycopy(args, 1, branches, 0, args.length - 1);
			fp.setBranches(branches);
		}

		fp.run(new TextProgressMonitor());
	}
}
