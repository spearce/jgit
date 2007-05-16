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
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.ObjectWritingException;

public class Repository {
	private static final String[] refSearchPaths = { "", "refs/", "refs/tags/",
			"refs/heads/", };

	private final File gitDir;

	private final File[] objectsDirs;

	private final File refsDir;

	private final RepositoryConfig config;

	private PackFile[] packs;

	private WindowCache windows;

	private Map treeCache = new WeakHashMap(30000);
	private Map commitCache = new WeakHashMap(30000);

	public Repository(final File d) throws IOException {
		gitDir = d.getAbsoluteFile();
		try {
			objectsDirs = (File[])readObjectsDirs(new File(gitDir, "objects"), new ArrayList()).toArray(new File[0]);
		} catch (IOException e) {
			IOException ex = new IOException("Cannot find all object dirs for " + gitDir);
			ex.initCause(e);
			throw ex;
		}
		refsDir = new File(gitDir, "refs");
		packs = new PackFile[0];
		config = new RepositoryConfig(this);
		if (objectsDirs[0].exists()) {
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

	private Collection readObjectsDirs(File objectsDir, Collection ret) throws IOException {
		ret.add(objectsDir);
		File alternatesFile = new File(objectsDir,"info/alternates");
		if (alternatesFile.exists()) {
			BufferedReader ar = new BufferedReader(new FileReader(alternatesFile));
			for (String alt=ar.readLine(); alt!=null; alt=ar.readLine()) {
				readObjectsDirs(new File(alt), ret);
			}
			ar.close();
		}
		return ret;
	}

	public void create() throws IOException {
		if (gitDir.exists()) {
			throw new IllegalStateException("Repository already exists: "
					+ gitDir);
		}

		gitDir.mkdirs();

		objectsDirs[0].mkdirs();
		new File(objectsDirs[0], "pack").mkdir();
		new File(objectsDirs[0], "info").mkdir();

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
		return objectsDirs[0];
	}

	public RepositoryConfig getConfig() {
		return config;
	}

	public WindowCache getWindowCache() {
		return windows;
	}

	public File toFile(final ObjectId objectId) {
		final String n = objectId.toString();
		String d=n.substring(0, 2);
		String f=n.substring(2);
		for (int i=0; i<objectsDirs.length; ++i) {
			File ret = new File(new File(objectsDirs[i], d), f);
			if (ret.exists())
				return ret;
		}
		return new File(new File(objectsDirs[0], d), f);
	}

	public boolean hasObject(final ObjectId objectId) {
		int k = packs.length;
		if (k > 0) {
			do {
				if (packs[--k].hasObject(objectId))
					return true;
			} while (k > 0);
		}
		return toFile(objectId).isFile();
	}

	public ObjectLoader openObject(final ObjectId id) throws IOException {
		int k = packs.length;
		if (k > 0) {
			do {
				try {
					final ObjectLoader ol = packs[--k].get(id);
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
						final ObjectLoader ol = packs[k].get(id);
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
//		System.out.println("commitcache.size="+commitCache.size());
		Reference retr = (Reference)commitCache.get(id);
		if (retr != null) {
			Commit ret = (Commit)retr.get();
			if (ret != null)
				return ret;
			System.out.println("Found a null id, size was "+commitCache.size());
		}

		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_COMMIT.equals(or.getType())) {
			Commit ret = new Commit(this, id, raw);
			// The key must not be the referenced strongly
			// by the value in WeakHashMaps
			commitCache.put(id, new SoftReference(ret));
			return ret;
		}
		throw new IncorrectObjectTypeException(id, Constants.TYPE_COMMIT);
	}

	public Tree mapTree(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTree(id) : null;
	}

	public Tree mapTree(final ObjectId id) throws IOException {
		Reference wret = (Reference)treeCache.get(id);
		if (wret != null) {
			Tree ret = (Tree)wret.get();
			if (ret != null)
				return ret;
		}

		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TREE.equals(or.getType())) {
			Tree ret = new Tree(this, id, raw);
			treeCache.put(id, new SoftReference(ret));
			return ret;
		}
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
		final ArrayList p = new ArrayList();
		for (int i=0; i<objectsDirs.length; ++i)
			scanForPacks(new File(objectsDirs[i], "pack"), p);
		final PackFile[] arr = new PackFile[p.size()];
		p.toArray(arr);
		packs = arr;
	}

	public void scanForPacks(final File packDir, Collection packList) {
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
		for (int k = 0; k < list.length; k++) {
			try {
				packList.add(new PackFile(this, list[k]));
			} catch (IOException ioe) {
				// Whoops. That's not a pack!
				//
			}
		}
	}

    public void writeSymref(final String name, final String target)
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

	public static class StGitPatch {
		public StGitPatch(String patchName, ObjectId id) {
			name = patchName;
			gitId = id;
		}
		public ObjectId getGitId() {
			return gitId;
		}
		public String getName() {
			return name;
		}
		private String name;
		private ObjectId gitId;
	}

	/**
	 * @return applied patches in a map indexed on current commit id
	 * @throws IOException
	 */
	public Map getAppliedPatches() throws IOException {
		Map ret = new HashMap();
		if (isStGitMode()) {
			File patchDir = new File(new File(getDirectory(),"patches"),getBranch());
			BufferedReader apr = new BufferedReader(new FileReader(new File(patchDir,"applied")));
			for (String patchName=apr.readLine(); patchName!=null; patchName=apr.readLine()) {
				File topFile = new File(new File(new File(patchDir,"patches"), patchName), "top");
				BufferedReader tfr = new BufferedReader(new FileReader(topFile));
				String objectId = tfr.readLine();
				ObjectId id = new ObjectId(objectId);
				ret.put(id, new StGitPatch(patchName, id));
				tfr.close();
			}
			apr.close();
		}
		return ret;
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
