/*
 *  Copyright (C) 2007  Robin Rosenberg
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
package org.spearce.jgit.lib;

/**
 * A simple progress reporter printing on stderr
 */
public class TextProgressMonitor extends AbstractProgressMonitor {

	int lastWorked;

	@Override
	protected void report() {
		int tot = getTotal() + 1;
		if ((lastWorked+1)*100/tot != (getWorked()+1)*100/tot)
			System.err.println(getMessage() + " " + (getWorked()*100 / tot) + "%");
		lastWorked = getWorked();
	}

}
