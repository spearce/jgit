/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.ObjectWritingException;

public class Repository {
	private static final String[] refSearchPaths = { "", "refs/", "refs/tags/",
			"refs/heads/", };

	private final File gitDir;

	private final File objectsDir;

	private final File refsDir;

	private final RepositoryConfig config;

	private PackFile[] packs;

	private WindowCache windows;

	private Map cache = new WeakHashMap(30000); 

	public Repository(final File d) throws IOException {
		gitDir = d.getAbsoluteFile();
		objectsDir = new File(gitDir, "objects");
		refsDir = new File(gitDir, "refs");
		packs = new PackFile[0];
		config = new RepositoryConfig(this);
		if (objectsDir.exists()) {
			getConfig().load();
			final String repositoryFormatVersion = getConfig().getString(
					"core", "repositoryFormatVersion");
			if (!"0".equals(repositoryFormatVersion)) {
				throw new IOException("Unknown repository format \""
						+ repositoryFormatVersion + "\"; expected \"0\".");
			}
			initializeWindowCache();
			scanForPacks();
		}
	}

	public void create() throws IOException {
		if (gitDir.exists()) {
			throw new IllegalStateException("Repository already exists: "
					+ gitDir);
		}

		gitDir.mkdirs();

		objectsDir.mkdirs();
		new File(objectsDir, "pack").mkdir();
		new File(objectsDir, "info").mkdir();

		refsDir.mkdir();
		new File(refsDir, "heads").mkdir();
		new File(refsDir, "tags").mkdir();

		new File(gitDir, "branches").mkdir();
		new File(gitDir, "remotes").mkdir();
		writeSymref("HEAD", "refs/heads/master");

		getConfig().create();
		getConfig().save();
		initializeWindowCache();
	}

	private void initializeWindowCache() {
		// FIXME these should be configurable...
		windows = new WindowCache(256 * 1024 * 1024, 4);
	}

	public File getDirectory() {
		return gitDir;
	}

	public File getObjectsDirectory() {
		return objectsDir;
	}

	public RepositoryConfig getConfig() {
		return config;
	}

	public WindowCache getWindowCache() {
		return windows;
	}

	public File toFile(final ObjectId objectId) {
		final String n = objectId.toString();
		return new File(new File(objectsDir, n.substring(0, 2)), n.substring(2));
	}

	public boolean hasObject(final ObjectId objectId) {
		int k = packs.length;
		if (k > 0) {
			final byte[] tmp = new byte[Constants.OBJECT_ID_LENGTH];
			do {
				try {
					if (packs[--k].hasObject(objectId, tmp))
						return true;
				} catch (IOException ioe) {
					// This shouldn't happen unless the pack was corrupted
					// after we opened it. We'll ignore the error as though
					// the object does not exist in this pack.
					//
				}
			} while (k > 0);
		}
		return toFile(objectId).isFile();
	}

	public ObjectLoader openObject(final ObjectId id) throws IOException {
		int k = packs.length;
		if (k > 0) {
			final byte[] tmp = new byte[Constants.OBJECT_ID_LENGTH];
			do {
				try {
					final ObjectLoader ol = packs[--k].get(id, tmp);
					if (ol != null)
						return ol;
				} catch (IOException ioe) {
					// This shouldn't happen unless the pack was corrupted
					// after we opened it or the VM runs out of memory. This is
					// a know problem with memory mapped I/O in java and have
					// been noticed with JDK < 1.6. Tell the gc that now is a good
					// time to collect and try once more.
					try {
						System.gc();
						final ObjectLoader ol = packs[k].get(id, tmp);
						if (ol != null)
							return ol;
					} catch (IOException ioe2) {
						ioe2.printStackTrace();
						ioe.printStackTrace();
						// Still fails.. that's BAD, maybe the pack has
						// been corrupted after all, or the gc didn't manage
						// to release enough previously mmaped areas.
					}
				}
			} while (k > 0);
		}
		try {
			return new UnpackedObjectLoader(this, id);
		} catch (FileNotFoundException fnfe) {
			return null;
		}
	}

	public ObjectLoader openBlob(final ObjectId id) throws IOException {
		return openObject(id);
	}

	public ObjectLoader openTree(final ObjectId id) throws IOException {
		return openObject(id);
	}

	public Commit mapCommit(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapCommit(id) : null;
	}

	public Commit mapCommit(final ObjectId id) throws IOException {
		Commit ret = (Commit)cache.get(id);
		if (ret != null)
			return ret;

		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_COMMIT.equals(or.getType())) {
			ret = new Commit(this, id, raw);
			// The key must not be the referenced strongly
			// by the value in WeakHashMaps
			cache.put(new ObjectId(id.getBytes()), ret);
			return ret;
		}
		throw new IncorrectObjectTypeException(id, Constants.TYPE_COMMIT);
	}

	public Tree mapTree(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTree(id) : null;
	}

	public Tree mapTree(final ObjectId id) throws IOException {
		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TREE.equals(or.getType()))
			return new Tree(this, id, raw);
		if (Constants.TYPE_COMMIT.equals(or.getType()))
			return mapTree(ObjectId.fromString(raw, 5));
		throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
	}

	public Tag mapTag(String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTag(id) : null;
	}

	public Tag mapTag(final ObjectId id) throws IOException {
		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TAG.equals(or.getType()))
			return new Tag(this, id, raw);
		throw new IncorrectObjectTypeException(id, Constants.TYPE_TAG);
	}

	public RefLock lockRef(final String ref) throws IOException {
		final RefLock l = new RefLock(readRef(ref, true));
		return l.lock() ? l : null;
	}

	public ObjectId resolve(final String revstr) throws IOException {
		ObjectId id = null;

		if (ObjectId.isId(revstr)) {
			id = new ObjectId(revstr);
		}

		if (id == null) {
			final Ref r = readRef(revstr, false);
			if (r != null) {
				id = r.getObjectId();
			}
		}

		return id;
	}

	public void close() throws IOException {
		closePacks();
	}

	public void closePacks() throws IOException {
		for (int k = packs.length - 1; k >= 0; k--) {
			packs[k].close();
		}
		packs = new PackFile[0];
	}

	public void scanForPacks() {
		final File packDir = new File(objectsDir, "pack");
		final File[] list = packDir.listFiles(new FileFilter() {
			public boolean accept(final File f) {
				final String n = f.getName();
				if (!n.endsWith(".pack")) {
					return false;
				}
				final String nBase = n.substring(0, n.lastIndexOf('.'));
				final File idx = new File(packDir, nBase + ".idx");
				return f.isFile() && f.canRead() && idx.isFile()
						&& idx.canRead();
			}
		});
		final ArrayList p = new ArrayList(list.length);
		for (int k = 0; k < list.length; k++) {
			try {
				p.add(new PackFile(this, list[k]));
			} catch (IOException ioe) {
				// Whoops. That's not a pack!
				//
			}
		}
		final PackFile[] arr = new PackFile[p.size()];
		p.toArray(arr);
		packs = arr;
	}

	private void writeSymref(final String name, final String target)
			throws IOException {
		final File s = new File(gitDir, name);
		final File t = File.createTempFile("srf", null, gitDir);
		FileWriter w = new FileWriter(t);
		try {
			w.write("ref: ");
			w.write(target);
			w.write('\n');
			w.close();
			w = null;
			if (!t.renameTo(s)) {
				s.getParentFile().mkdirs();
				if (!t.renameTo(s)) {
					t.delete();
					throw new ObjectWritingException("Unable to"
							+ " write symref " + name + " to point to "
							+ target);
				}
			}
		} finally {
			if (w != null) {
				w.close();
				t.delete();
			}
		}
	}

	private Ref readRef(final String revstr, final boolean missingOk)
			throws IOException {
		for (int k = 0; k < refSearchPaths.length; k++) {
			final Ref r = readRefBasic(refSearchPaths[k] + revstr);
			if (missingOk || r.getObjectId() != null) {
				return r;
			}
		}
		return null;
	}

	private Ref readRefBasic(String name) throws IOException {
		int depth = 0;
		REF_READING: do {
			final File f = new File(getDirectory(), name);
			if (!f.isFile()) {
				return new Ref(f, null);
			}

			final BufferedReader br = new BufferedReader(new FileReader(f));
			try {
				final String line = br.readLine();
				if (line == null || line.length() == 0) {
					return new Ref(f, null);
				} else if (line.startsWith("ref: ")) {
					name = line.substring("ref: ".length());
					continue REF_READING;
				} else if (ObjectId.isId(line)) {
					return new Ref(f, new ObjectId(line));
				}
				throw new IOException("Not a ref: " + name + ": " + line);
			} finally {
				br.close();
			}
		} while (depth++ < 5);
		throw new IOException("Exceed maximum ref depth.  Circular reference?");
	}

	public String toString() {
		return "Repository[" + getDirectory() + "]";
	}

	public String getPatch() throws IOException {
		final File ptr = new File(getDirectory(),"patches/"+getBranch()+"/current");
		final BufferedReader br = new BufferedReader(new FileReader(ptr));
		try {
			return br.readLine();
		} finally {
			br.close();
		}
	}

	public String getBranch() throws IOException {
		final File ptr = new File(getDirectory(),"HEAD");
		final BufferedReader br = new BufferedReader(new FileReader(ptr));
		String ref;
		try {
			ref = br.readLine();
		} finally {
			br.close();
		}
		if (ref.startsWith("ref: "))
			ref = ref.substring(5);
		if (ref.startsWith("refs/heads/"))
			ref = ref.substring(11);
		return ref;
	}

	public Collection getBranches() {
		return listFilesRecursively(new File(refsDir, "heads"), null);
	}

	public Collection getTags() {
		return listFilesRecursively(new File(refsDir, "tags"), null);
	}

	/**
	 * @return true if HEAD points to a StGit patch.
	 */
	public boolean isStGitMode() {
		try {
			File file = new File(getDirectory(), "HEAD");
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String string = reader.readLine();
			if (!string.startsWith("ref: refs/heads/"))
				return false;
			String branch = string.substring("ref: refs/heads/".length());
			File currentPatch = new File(new File(new File(getDirectory(),
					"patches"), branch), "current");
			if (!currentPatch.exists())
				return false;
			if (currentPatch.length() == 0)
				return false;
			return true;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private Collection listFilesRecursively(File root, File start) {
		if (start == null)
			start = root;
		Collection ret = new ArrayList();
		File[] files = start.listFiles();
		for (int i = 0; i < files.length; ++i) {
			if (files[i].isDirectory())
				ret.addAll(listFilesRecursively(root, files[i]));
			else if (files[i].length() == 41) {
				String name = files[i].toString().substring(
						root.toString().length() + 1);
				ret.add(name);
			}
		}
		return ret;
	}
}
