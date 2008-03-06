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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.ObjectWritingException;
import org.spearce.jgit.stgit.StGitPatch;

/**
 * Represents a Git repository. A repository holds all objects and refs used for
 * managing source code (could by any type of file, but source code is what
 * SCM's are typically used for).
 *
 * In Git terms all data is stored in GIT_DIR, typically a directory called
 * .git. A work tree is maintained unless the repository is a bare repository.
 * Typically the .git directory is located at the root of the work dir.
 *
 * <ul>
 * <li>GIT_DIR
 * 	<ul>
 * 		<li>objects/ - objects</li>
 * 		<li>refs/ - tags and heads</li>
 * 		<li>config - configuration</li>
 * 		<li>info/ - more configurations</li>
 * 	</ul>
 * </li>
 * </ul>
 *
 * This implementation only handles a subtly undocumented subset of git features.
 *
 */
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

	/**
	 * Construct a representation of this git repo managing a Git repository.
	 *
	 * @param d
	 *            GIT_DIR
	 * @throws IOException
	 */
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
					"core", null, "repositoryFormatVersion");
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

	/**
	 * Create a new Git repository initializing the necessary files and
	 * directories.
	 *
	 * @throws IOException
	 */
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
		windows = new WindowCache(getConfig().getCore().getPackedGitLimit(), 4);
	}

	/**
	 * @return GIT_DIR
	 */
	public File getDirectory() {
		return gitDir;
	}

	/**
	 * @return the directory containg the objects owned by this repository.
	 */
	public File getObjectsDirectory() {
		return objectsDirs[0];
	}

	/**
	 * @return the configuration of this repository
	 */
	public RepositoryConfig getConfig() {
		return config;
	}

	/**
	 * @return the cache needed for accessing packed objects in this repository.
	 */
	public WindowCache getWindowCache() {
		return windows;
	}

	/**
	 * Construct a filename where the loose object having a specified SHA-1
	 * should be stored. If the object is stored in a shared repository the path
	 * to the alternative repo will be returned. If the object is not yet store
	 * a usable path in this repo will be returned. It is assumed that callers
	 * will look for objects in a pack first.
	 *
	 * @param objectId
	 * @return suggested file name
	 */
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

	/**
	 * @param objectId
	 * @return true if the specified object is stored in this repo or any of the
	 *         known shared repositories.
	 */
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

	/**
	 * @param id SHA-1 of an object.
	 *
	 * @return a {@link ObjectLoader} for accessing the data of the named
	 *         object, or null if the object does not exist.
	 * @throws IOException
	 */
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

	/**
	 * @param id
	 *            SHA'1 of a blob
	 * @return an {@link ObjectLoader} for accessing the data of a named blob
	 * @throws IOException
	 */
	public ObjectLoader openBlob(final ObjectId id) throws IOException {
		return openObject(id);
	}

	/**
	 * @param id
	 *            SHA'1 of a tree
	 * @return an {@link ObjectLoader} for accessing the data of a named tree
	 * @throws IOException
	 */
	public ObjectLoader openTree(final ObjectId id) throws IOException {
		return openObject(id);
	}

	/**
	 * Access a Commit object using a symbolic reference. This reference may
	 * be a SHA-1 or ref in combination with a number of symbols translating
	 * from one ref or SHA1-1 to another, such as HEAD^ etc.
	 *
	 * @param revstr a reference to a git commit object
	 * @return a Commit named by the specified string
	 * @throws IOException for I/O error or unexpected object type.
	 *
	 * @see #resolve(String)
	 */
	public Commit mapCommit(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapCommit(id) : null;
	}

	/**
	 * Access a Commit by SHA'1 id.
	 * @param id
	 * @return Commit or null
	 * @throws IOException for I/O error or unexpected object type.
	 */
	public Commit mapCommit(final ObjectId id) throws IOException {
		Reference<Commit> retr = commitCache.get(id);
		if (retr != null) {
			Commit ret = retr.get();
			if (ret != null)
				return ret;
//			System.out.println("Found a null id, size was "+commitCache.size());
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

	/**
	 * Access a Tree object using a symbolic reference. This reference may
	 * be a SHA-1 or ref in combination with a number of symbols translating
	 * from one ref or SHA1-1 to another, such as HEAD^{tree} etc.
	 *
	 * @param revstr a reference to a git commit object
	 * @return a Tree named by the specified string
	 * @throws IOException
	 *
	 * @see #resolve(String)
	 */
	public Tree mapTree(final String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTree(id) : null;
	}

	/**
	 * Access a Tree by SHA'1 id.
	 * @param id
	 * @return Tree or null
	 * @throws IOException for I/O error or unexpected object type.
	 */
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

	/**
	 * Access a tag by symbolic name.
	 *
	 * @param revstr
	 * @return a Tag or null
	 * @throws IOException on I/O error or unexpected type
	 */
	public Tag mapTag(String revstr) throws IOException {
		final ObjectId id = resolve(revstr);
		return id != null ? mapTag(revstr, id) : null;
	}

	/**
	 * Access a Tag by SHA'1 id
	 * @param refName
	 * @param id
	 * @return Commit or null
	 * @throws IOException for I/O error or unexpected object type.
	 */
	public Tag mapTag(final String refName, final ObjectId id) throws IOException {
		final ObjectLoader or = openObject(id);
		if (or == null)
			return null;
		final byte[] raw = or.getBytes();
		if (Constants.TYPE_TAG.equals(or.getType()))
			return new Tag(this, id, refName, raw);
		return new Tag(this, id, refName, null);
	}

	/**
	 * Get a locked handle to a ref suitable for updating or creating.
	 *
	 * @param ref name to lock
	 * @return a locked ref
	 * @throws IOException
	 */
	public RefLock lockRef(final String ref) throws IOException {
		final Ref r = readRef(ref, true);
		final RefLock l = new RefLock(new File(gitDir, r.getName()));
		return l.lock() ? l : null;
	}

	/**
	 * Parse a git revision string and return an object id.
	 *
	 * Currently supported is combinations of these.
	 * <ul>
	 *  <li>SHA-1 - a SHA-1</li>
	 *  <li>refs/... - a ref name</li>
	 *  <li>ref^n - nth parent reference</li>
	 *  <li>ref~n - distance via parent reference</li>
	 *  <li>ref@{n} - nth version of ref</li>
	 *  <li>ref^{tree} - tree references by ref</li>
	 *  <li>ref^{commit} - commit references by ref</li>
	 * </ul>
	 *
	 * Not supported is
	 * <ul>
	 * <li>timestamps in reflogs, ref@{full or relative timestamp}</li>
	 * <li>abbreviated SHA-1's</li>
	 * </ul>
	 *
	 * @param revstr A git object references expression
	 * @return an ObjectId
	 * @throws IOException on serious errors
	 */
	public ObjectId resolve(final String revstr) throws IOException {
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

	private ObjectId resolveSimple(final String revstr) throws IOException {
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

	/**
	 * Close all resources used by this repository
	 */
	public void close() {
		closePacks();
	}

	void closePacks() {
		for (int k = packs.length - 1; k >= 0; k--) {
			packs[k].close();
		}
		packs = new PackFile[0];
	}

	/**
	 * Scan the object dirs, including alternates for packs
	 * to use.
	 */
	public void scanForPacks() {
		final ArrayList<PackFile> p = new ArrayList<PackFile>();
		for (int i=0; i<objectsDirs.length; ++i)
			scanForPacks(new File(objectsDirs[i], "pack"), p);
		final PackFile[] arr = new PackFile[p.size()];
		p.toArray(arr);
		packs = arr;
	}

	private void scanForPacks(final File packDir, Collection<PackFile> packList) {
		final String[] idxList = packDir.list(new FilenameFilter() {
			public boolean accept(final File baseDir, final String n) {
				// Must match "pack-[0-9a-f]{40}.idx" to be an index.
				return n.length() == 49 && n.endsWith(".idx")
						&& n.startsWith("pack-");
			}
		});
		if (idxList != null) {
			for (final String indexName : idxList) {
				final String n = indexName.substring(0, indexName.length() - 4);
				final File idxFile = new File(packDir, n + ".idx");
				final File packFile = new File(packDir, n + ".pack");
				try {
					packList.add(new PackFile(this, idxFile, packFile));
				} catch (IOException ioe) {
					// Whoops. That's not a pack!
					//
					ioe.printStackTrace();
				}
			}
		}
	}

    /**
     * Writes a symref (e.g. HEAD) to disk
     *
     * @param name symref name
     * @param target pointed to ref
     * @throws IOException
     */
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
		refreshPackedRefsCache();
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
			// prefer unpacked ref to packed ref
			final File f = new File(getDirectory(), name);
			if (!f.isFile()) {
				// look for packed ref, since this one doesn't exist
				ObjectId id = packedRefs.get(name);
				if (id != null)
					return new Ref(name, id);
				
				// no packed ref found, return blank one
				return new Ref(name, null);
			}

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

	/**
	 * @return name of topmost Stacked Git patch.
	 * @throws IOException
	 */
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

	/**
	 * @return name of current branch
	 * @throws IOException
	 */
	public String getFullBranch() throws IOException {
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
		return ref;
	}
	
	/**
	 * @return name of current branch.
	 * @throws IOException
	 */
	public String getBranch() throws IOException {
		try {
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
		} catch (FileNotFoundException e) {
			final File ptr = new File(getDirectory(),"head-name");
			final BufferedReader br = new BufferedReader(new FileReader(ptr));
			String ref;
			try {
				ref = br.readLine();
			} finally {
				br.close();
			}
			return ref;
		}
	}

	/**
	 * @return names of all local branches
	 */
	public Collection<String> getBranches() {
		return listRefs("heads");
	}
	
	/**
	 * @return the names of all refs (local and remotes branches, tags)
	 */
	public Collection<String> getAllRefs() {
		return listRefs("");
	}
	
	private Collection<String> listRefs(String refSubDir) {
		// add / to end, unless empty
		if (refSubDir.length() > 0 && refSubDir.charAt(refSubDir.length() -1 ) != '/')
			refSubDir += "/";
		
		Collection<String> branchesRaw = listFilesRecursively(new File(refsDir, refSubDir), null);
		ArrayList<String> branches = new ArrayList<String>();
		for (String b : branchesRaw) {
			branches.add("refs/" + refSubDir + b);
		}
		
		refreshPackedRefsCache();
		Set<String> keySet = packedRefs.keySet();
		for (String s : keySet)
			if (s.startsWith("refs/" + refSubDir) && !branches.contains(s))
				branches.add(s);
		return branches;
	}

	/**
	 * @return all git tags
	 */
	public Collection<String> getTags() {
		return listRefs("tags");
	}

	private Map<String,ObjectId> packedRefs = new HashMap<String,ObjectId>();
	private long packedrefstime = 0;

	private void refreshPackedRefsCache() {
		File file = new File(gitDir, "packed-refs");
		if (!file.exists()) {
			if (packedRefs.size() > 0)
				packedRefs = new HashMap<String,ObjectId>();
			return;
		}
		if (file.lastModified() == packedrefstime)
			return;
		Map<String,ObjectId> newPackedRefs = new HashMap<String,ObjectId>();
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(file);
			BufferedReader b=new BufferedReader(fileReader);
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
			throw new Error("Cannot read packed refs",e);
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					// Cannot do anything more here
					e.printStackTrace();
				}
			}
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
				if (File.separatorChar != '/')
					name = name.replace(File.separatorChar, '/');
				ret.add(name);
			}
		}
		return ret;
	}
	
	/** Clean up stale caches */
	public void refreshFromDisk() {
		packedRefs = null;
	}

	/**
	 * @return a representation of the index associated with this repo
	 * @throws IOException
	 */
	public GitIndex getIndex() throws IOException {
		if (index == null) {
			index = new GitIndex(this);
			index.read();
		} else {
			index.rereadIfNecessary();
		}
		return index;
	}

	static byte[] gitInternalSlash(byte[] bytes) {
		if (File.separatorChar == '/')
			return bytes;
		for (int i=0; i<bytes.length; ++i)
			if (bytes[i] == File.separatorChar)
				bytes[i] = '/';
		return bytes;
	}

	/**
	 * @return an important state
	 */
	public RepositoryState getRepositoryState() {
		if (new File(gitDir.getParentFile(), ".dotest").exists())
			return RepositoryState.REBASING;
		if (new File(gitDir,".dotest-merge").exists())
			return RepositoryState.REBASING_INTERACTIVE;
		if (new File(gitDir,"MERGE_HEAD").exists())
			return RepositoryState.MERGING;
		if (new File(gitDir,"BISECT_LOG").exists())
			return RepositoryState.BISECTING;
		return RepositoryState.SAFE;
	}

	/**
	 * String work dir and return normalized repository path
	 *
	 * @param wd Work dir
	 * @param f File whose path shall be stripp off it's workdir
	 * @return normalized repository relative path
	 */
	public static String stripWorkDir(File wd, File f) {
		String relName = f.getPath().substring(wd.getPath().length() + 1);
		relName = relName.replace(File.separatorChar, '/');
		return relName;
	}
}
