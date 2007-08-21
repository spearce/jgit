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

	private Map<ObjectId,Reference<Tree>> treeCache = new WeakHashMap<ObjectId,Reference<Tree>>(30000);
	private Map<ObjectId,Reference<Commit>> commitCache = new WeakHashMap<ObjectId,Reference<Commit>>(30000);

	private GitIndex index;

	public Repository(final File d) throws IOException {
		gitDir = d.getAbsoluteFile();
		try {
			objectsDirs = readObjectsDirs(new File(gitDir, "objects"), new ArrayList<File>()).toArray(new File[0]);
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

	private Collection<File> readObjectsDirs(File objectsDir, Collection<File> ret) throws IOException {
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
		Reference<Commit> retr = commitCache.get(id);
		if (retr != null) {
			Commit ret = retr.get();
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
			commitCache.put(id, new SoftReference<Commit>(ret));
			return ret;
		}
		throw new IncorrectObjectTypeException(id, Constants.TYPE_COMMIT);
	}

	public Tree mapTree(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTree(id) : null;
	}

	public Tree mapTree(final ObjectId id) throws IOException {
		Reference<Tree> wret = treeCache.get(id);
		if (wret != null) {
			Tree ret = wret.get();
			if (ret != null)
				return ret;
		}

		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TREE.equals(or.getType())) {
			Tree ret = new Tree(this, id, raw);
			treeCache.put(id, new SoftReference<Tree>(ret));
			return ret;
		}
		if (Constants.TYPE_COMMIT.equals(or.getType()))
			return mapTree(ObjectId.fromString(raw, 5));
		throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
	}

	public Tag mapTag(String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTag(revstr, id) : null;
	}

	public Tag mapTag(final String refName, final ObjectId id) throws IOException {
		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TAG.equals(or.getType()))
			return new Tag(this, id, refName, raw);
		return new Tag(this, id, refName, null);
	}

	public RefLock lockRef(final String ref) throws IOException {
		final Ref r = readRef(ref, true);
		final RefLock l = new RefLock(new File(gitDir, r.getName()));
		return l.lock() ? l : null;
	}

	/** Parse a git revision string and return an object id.
	 *
	 *  It is not fully implemented, so it only deals with
	 *  commits and to some extent tags. Reflogs are not
	 *  supported yet.
	 *  The plan is to implement it fully.
	 * @param revstr A git object references expression
	 * @return an ObjectId
	 * @throws IOException on serious errors
	 */
	public ObjectId parse(String revstr) throws IOException {
		char[] rev = revstr.toCharArray();
		ObjectId ret = null;
		Commit ref = null;
		for (int i = 0; i < rev.length; ++i) {
			switch (rev[i]) {
			case '^':
				if (ref == null) {
					String refstr = new String(rev,0,i);
					ObjectId refId = resolveSimple(refstr);
					ref = mapCommit(refId);
				}
				if (i + 1 < rev.length) {
					switch (rev[i + 1]) {
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						int j;
						for (j=i+1; j<rev.length; ++j) {
							if (!Character.isDigit(rev[j]))
								break;
						}
						String parentnum = new String(rev, i+1, j-i-1);
						int pnum = Integer.parseInt(parentnum);
						if (pnum != 0)
							ref = mapCommit(ref.getParentIds()[pnum - 1]);
						i = j - 1;
						break;
					case '{':
						int k;
						String item = null;
						for (k=i+2; k<rev.length; ++k) {
							if (rev[k] == '}') {
								item = new String(rev, i+2, k-i-2);
								break;
							}
						}
						i = k;
						if (item != null)
							if (item.equals("tree"))
								ret = ref.getTreeId();
							else if (item.equals("commit"))
								; // just reference self
							else
								return null; // invalid
						else
							return null; // invalid
						break;
					default:
						ref = mapCommit(ref.getParentIds()[0]);
					}
				} else {
					ref = mapCommit(ref.getParentIds()[0]);
				}
				break;
			case '~':
				if (ref == null) {
					String refstr = new String(rev,0,i);
					ObjectId refId = resolveSimple(refstr);
					ref = mapCommit(refId);
				}
				int l;
				for (l = i + 1; l < rev.length; ++l) {
					if (!Character.isDigit(rev[l]))
						break;
				}
				String distnum = new String(rev, i+1, l-i-1);
				int dist = Integer.parseInt(distnum);
				while (dist >= 0) {
					ref = mapCommit(ref.getParentIds()[0]);
					--dist;
				}
				i = l - 1;
				break;
			case '@':
				int m;
				String time = null;
				for (m=i+2; m<rev.length; ++m) {
					if (rev[m] == '}') {
						time = new String(rev, i+2, m-i-2);
						break;
					}
				}
				if (time != null)
					throw new IllegalArgumentException("reflogs not yet supprted");
				i = m - 1;
				break;
			default:
				if (ref != null)
					return null; // cannot parse, return null
			}
		}
		if (ret == null)
			if (ref != null)
				ret = ref.getCommitId();
			else
				ret = resolveSimple(revstr);
		return ret;
	}

	public ObjectId resolve(final String revstr) throws IOException {
		return parse(revstr);
	}

	public ObjectId resolveSimple(final String revstr) throws IOException {
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
		final ArrayList<PackFile> p = new ArrayList<PackFile>();
		for (int i=0; i<objectsDirs.length; ++i)
			scanForPacks(new File(objectsDirs[i], "pack"), p);
		final PackFile[] arr = new PackFile[p.size()];
		p.toArray(arr);
		packs = arr;
	}

	public void scanForPacks(final File packDir, Collection<PackFile> packList) {
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
		if (list != null) {
			for (int k = 0; k < list.length; k++) {
				try {
					packList.add(new PackFile(this, list[k]));
				} catch (IOException ioe) {
					// Whoops. That's not a pack!
					//
				}
			}
		}
	}

    public void writeSymref(final String name, final String target)
			throws IOException {
		final byte[] content = ("ref: " + target + "\n").getBytes("UTF-8");
		final RefLock lck = new RefLock(new File(gitDir, name));
		if (!lck.lock())
			throw new ObjectWritingException("Unable to lock " + name);
		try {
			lck.write(content);
		} catch (IOException ioe) {
			throw new ObjectWritingException("Unable to write " + name, ioe);
		}
		if (!lck.commit())
			throw new ObjectWritingException("Unable to write " + name);
	}

	private Ref readRef(final String revstr, final boolean missingOk)
			throws IOException {
		refreshPackredRefsCache();
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
			ObjectId id = packedRefs.get(name);
			if (id != null)
				return new Ref(null, id);

			final File f = new File(getDirectory(), name);
			if (!f.isFile())
				return new Ref(name, null);

			final BufferedReader br = new BufferedReader(new FileReader(f));
			try {
				final String line = br.readLine();
				if (line == null || line.length() == 0)
					return new Ref(name, null);
				else if (line.startsWith("ref: ")) {
					name = line.substring("ref: ".length());
					continue REF_READING;
				} else if (ObjectId.isId(line))
					return new Ref(name, new ObjectId(line));
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
		final File ptr = new File(getDirectory(),"patches/"+getBranch()+"/applied");
		final BufferedReader br = new BufferedReader(new FileReader(ptr));
		String last=null;
		try {
			String line;
			while ((line=br.readLine())!=null) {
				last = line;
			}
		} finally {
			br.close();
		}
		return last;
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

	public Collection<String> getBranches() {
		return listFilesRecursively(new File(refsDir, "heads"), null);
	}

	public Collection<String> getTags() {
		Collection<String> tags = listFilesRecursively(new File(refsDir, "tags"), null);
		refreshPackredRefsCache();
		tags.addAll(packedRefs.keySet());
		return tags;
	}

	private Map<String,ObjectId> packedRefs = new HashMap<String,ObjectId>();
	private long packedrefstime = 0;

	private void refreshPackredRefsCache() {
		File file = new File(gitDir, "packed-refs");
		if (!file.exists()) {
			if (packedRefs.size() > 0)
				packedRefs = new HashMap<String,ObjectId>();
			return;
		}
		if (file.lastModified() == packedrefstime)
			return;
		Map<String,ObjectId> newPackedRefs = new HashMap<String,ObjectId>();
		try {
			BufferedReader b=new BufferedReader(new FileReader(file));
			String p;
			while ((p = b.readLine()) != null) {
				if (p.charAt(0) == '#')
					continue;
				if (p.charAt(0) == '^') {
					continue;
				}
				int spos = p.indexOf(' ');
				ObjectId id = new ObjectId(p.substring(0,spos));
				String name = p.substring(spos+1);
				newPackedRefs.put(name, id);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		packedRefs = newPackedRefs;
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
			File currentPatches = new File(new File(new File(getDirectory(),
					"patches"), branch), "applied");
			if (!currentPatches.exists())
				return false;
			if (currentPatches.length() == 0)
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
	public Map<ObjectId,StGitPatch> getAppliedPatches() throws IOException {
		Map<ObjectId,StGitPatch> ret = new HashMap<ObjectId,StGitPatch>();
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

	private Collection<String> listFilesRecursively(File root, File start) {
		if (start == null)
			start = root;
		Collection<String> ret = new ArrayList<String>();
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
	
	/** Clean up stale caches */
	public void refreshFromDisk() {
		packedRefs = null;
	}

	public GitIndex getIndex() throws IOException {
		if (index == null) {
			index = new GitIndex(this);
			index.read();
		} else {
			index.rereadIfNecessary();
		}
		return index;
	}

	public static byte[] gitInternalSlash(byte[] bytes) {
		if (File.separatorChar == '/')
			return bytes;
		for (int i=0; i<bytes.length; ++i)
			if (bytes[i] == File.separatorChar)
				bytes[i] = '/';
		return bytes;
	}

}
