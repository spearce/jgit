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

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.transport.FetchConnection;
import org.spearce.jgit.transport.Transport;

class LsRemote extends TextBuiltin {
	@Override
	void execute(final String[] args) throws Exception {
		int argi = 0;
		for (; argi < args.length; argi++) {
			final String a = args[argi];
			if ("--".equals(a)) {
				argi++;
				break;
			} else
				break;
		}

		if (argi == args.length)
			throw die("usage: url");
		else if (argi + 1 < args.length)
			throw die("too many arguments");

		final Transport tn = Transport.open(db, args[argi]);
		final FetchConnection c = tn.openFetch();
		try {
			for (final Ref r : c.getRefs()) {
				show(r.getObjectId(), r.getName());
				if (r.getPeeledObjectId() != null)
					show(r.getPeeledObjectId(), r.getName() + "^{}");
			}
		} finally {
			c.close();
		}
	}

	private void show(final AnyObjectId id, final String name) {
		out.print(id);
		out.print('\t');
		out.print(name);
		out.println();
	}
}
