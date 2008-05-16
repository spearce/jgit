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

import java.util.TreeMap;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.Ref;

class ShowRef extends TextBuiltin {
	@Override
	void execute(String[] args) throws Exception {
		for (final Ref r : new TreeMap<String, Ref>(db.getAllRefs()).values()) {
			show(r.getObjectId(), r.getName());
			if (r.getPeeledObjectId() != null)
				show(r.getPeeledObjectId(), r.getName() + "^{}");
		}
	}

	private void show(final AnyObjectId id, final String name) {
		out.print(id);
		out.print('\t');
		out.print(name);
		out.println();
	}
}
