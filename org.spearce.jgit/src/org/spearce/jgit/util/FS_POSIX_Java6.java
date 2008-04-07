/*
 *  Copyright (C) 2007  Robin Rosenberg <robin.rosenberg@dewire.com>
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
package org.spearce.jgit.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class FS_POSIX_Java6 extends FS {
	private static final Method canExecute;

	private static final Method setExecute;

	static {
		canExecute = needMethod(File.class, "canExecute");
		setExecute = needMethod(File.class, "setExecutable", Boolean.TYPE);
	}

	static boolean detect() {
		return canExecute != null && setExecute != null;
	}

	private static Method needMethod(final Class<?> on, final String name,
			final Class<?>... args) {
		try {
			return on.getMethod(name, args);
		} catch (SecurityException e) {
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	public boolean supportsExecute() {
		return true;
	}

	public boolean canExecute(final File f) {
		try {
			final Object r = canExecute.invoke(f, (Object[]) null);
			return ((Boolean) r).booleanValue();
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e);
		}
	}

	public boolean setExecute(final File f, final boolean canExec) {
		try {
			final Object r;
			r = setExecute.invoke(f, new Object[] { Boolean.valueOf(canExec) });
			return ((Boolean) r).booleanValue();
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e);
		}
	}
}
