/*
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.spearce.jgit.errors.ObjectWritingException;
import org.spearce.jgit.util.FS;
import org.spearce.jgit.util.NB;

class RefDatabase {
	private static final String CHAR_ENC = Constants.CHARACTER_ENCODING;

	private static final String REFS_SLASH = "refs/";

	private static final String HEADS_SLASH = Constants.HEADS_PREFIX + "/";

	private static final String TAGS_SLASH = Constants.TAGS_PREFIX + "/";

	private static final String[] refSearchPaths = { "", REFS_SLASH,
			TAGS_SLASH, HEADS_SLASH, Constants.REMOTES_PREFIX + "/" };

	private final Repository db;

	private final File gitDir;

	private final File refsDir;

	private Map<String, CachedRef> looseRefs;

	private final File packedRefsFile;

	private Map<String, Ref> packedRefs;

	private long packedRefsLastModified;

	private long packedRefsLength;

	long lastRefModification;

	long lastNotifiedRefModification;

	static int refModificationCounter;

	RefDatabase(final Repository r) {
		db = r;
		gitDir = db.getDirectory();
		refsDir = FS.resolve(gitDir, "refs");
		packedRefsFile = FS.resolve(gitDir, "packed-refs");
		clearCache();
	}

	void clearCache() {
		looseRefs = new HashMap<String, CachedRef>();
		packedRefs = new HashMap<String, Ref>();
		packedRefsLastModified = 0;
		packedRefsLength = 0;
	}

	Repository getRepository() {
		return db;
	}

	void create() {
		refsDir.mkdir();
		new File(refsDir, "heads").mkdir();
		new File(refsDir, "tags").mkdir();
	}

	ObjectId idOf(final String name) throws IOException {
		final Ref r = readRefBasic(name, 0);
		return r != null ? r.getObjectId() : null;
	}

	/**
	 * Create a command to update (or create) a ref in this repository.
	 * 
	 * @param name
	 *            name of the ref the caller wants to modify.
	 * @return an update command. The caller must finish populating this command
	 *         and then invoke one of the update methods to actually make a
	 *         change.
	 * @throws IOException
	 *             a symbolic ref was passed in and could not be resolved back
	 *             to the base ref, as the symbolic ref could not be read.
	 */
	RefUpdate newUpdate(final String name) throws IOException {
		Ref r = readRefBasic(name, 0);
		if (r == null)
			r = new Ref(Ref.Storage.NEW, name, null);
		return new RefUpdate(this, r, fileForRef(r.getName()));
	}

	void stored(final String name, final ObjectId id, final long time) {
		looseRefs.put(name, new CachedRef(Ref.Storage.LOOSE, name, id, time));
		setModified();
		db.fireRefsMaybeChanged();
	}

	/**
	 * Writes a symref (e.g. HEAD) to disk
	 * 
	 * @param name
	 *            symref name
	 * @param target
	 *            pointed to ref
	 * @throws IOException
	 */
	void link(final String name, final String target) throws IOException {
		final byte[] content = ("ref: " + target + "\n").getBytes(CHAR_ENC);
		final LockFile lck = new LockFile(fileForRef(name));
		if (!lck.lock())
			throw new ObjectWritingException("Unable to lock " + name);
		try {
			lck.write(content);
		} catch (IOException ioe) {
			throw new ObjectWritingException("Unable to write " + name, ioe);
		}
		if (!lck.commit())
			throw new ObjectWritingException("Unable to write " + name);
		setModified();
		db.fireRefsMaybeChanged();
	}

	void setModified() {
		lastRefModification = refModificationCounter++;
	}

	Ref readRef(final String partialName) throws IOException {
		refreshPackedRefs();
		for (int k = 0; k < refSearchPaths.length; k++) {
			final Ref r = readRefBasic(refSearchPaths[k] + partialName, 0);
			if (r != null && r.getObjectId() != null)
				return r;
		}
		return null;
	}

	/**
	 * @return all known refs (heads, tags, remotes).
	 */
	Map<String, Ref> getAllRefs() {
		return readRefs();
	}

	/**
	 * @return all tags; key is short tag name ("v1.0") and value of the entry
	 *         contains the ref with the full tag name ("refs/tags/v1.0").
	 */
	Map<String, Ref> getTags() {
		final Map<String, Ref> tags = new HashMap<String, Ref>();
		for (final Ref r : readRefs().values()) {
			if (r.getName().startsWith(TAGS_SLASH))
				tags.put(r.getName().substring(TAGS_SLASH.length()), r);
		}
		return tags;
	}

	private Map<String, Ref> readRefs() {
		final HashMap<String, Ref> avail = new HashMap<String, Ref>();
		readPackedRefs(avail);
		readLooseRefs(avail, REFS_SLASH, refsDir);
		readOneLooseRef(avail, Constants.HEAD, new File(gitDir, Constants.HEAD));
		db.fireRefsMaybeChanged();
		return avail;
	}

	private void readPackedRefs(final Map<String, Ref> avail) {
		refreshPackedRefs();
		avail.putAll(packedRefs);
	}

	private void readLooseRefs(final Map<String, Ref> avail,
			final String prefix, final File dir) {
		final File[] entries = dir.listFiles();
		if (entries == null)
			return;

		for (final File ent : entries) {
			final String entName = ent.getName();
			if (".".equals(entName) || "..".equals(entName))
				continue;
			readOneLooseRef(avail, prefix + entName, ent);
		}
	}

	private void readOneLooseRef(final Map<String, Ref> avail,
			final String refName, final File ent) {
		// Unchanged and cached? Don't read it again.
		//
		CachedRef ref = looseRefs.get(refName);
		if (ref != null) {
			if (ref.lastModified == ent.lastModified()) {
				avail.put(ref.getName(), ref);
				return;
			}
			looseRefs.remove(refName);
		}

		// Recurse into the directory.
		//
		if (ent.isDirectory()) {
			readLooseRefs(avail, refName + "/", ent);
			return;
		}

		// Assume its a valid loose reference we need to cache.
		//
		try {
			final FileInputStream in = new FileInputStream(ent);
			try {
				final ObjectId id;
				try {
					final byte[] str = new byte[Constants.OBJECT_ID_LENGTH * 2];
					NB.readFully(in, str, 0, str.length);
					id = ObjectId.fromString(str, 0);
				} catch (EOFException tooShortToBeRef) {
					// Its below the minimum length needed. It could
					// be a symbolic reference.
					//
					return;
				} catch (IllegalArgumentException notRef) {
					// It is not a well-formed ObjectId. It may be
					// a symbolic reference ("ref: ").
					//
					return;
				}

				ref = new CachedRef(Ref.Storage.LOOSE, refName, id, ent
						.lastModified());
				looseRefs.put(ref.getName(), ref);
				avail.put(ref.getName(), ref);
			} finally {
				in.close();
			}
		} catch (FileNotFoundException noFile) {
			// Deleted while we were reading? Its gone now!
			//
		} catch (IOException err) {
			// Whoops.
			//
			throw new RuntimeException("Cannot read ref " + ent, err);
		}
	}

	private File fileForRef(final String name) {
		if (name.startsWith(REFS_SLASH))
			return new File(refsDir, name.substring(REFS_SLASH.length()));
		return new File(gitDir, name);
	}

	private Ref readRefBasic(final String name, final int depth)
			throws IOException {
		// Prefer loose ref to packed ref as the loose
		// file can be more up-to-date than a packed one.
		//
		CachedRef ref = looseRefs.get(name);
		final File loose = fileForRef(name);
		final long mtime = loose.lastModified();
		if (ref != null) {
			if (ref.lastModified == mtime)
				return ref;
			looseRefs.remove(name);
		}

		if (mtime == 0) {
			// If last modified is 0 the file does not exist.
			// Try packed cache.
			//
			return packedRefs.get(name);
		}

		final String line;
		try {
			line = readLine(loose);
		} catch (FileNotFoundException notLoose) {
			return packedRefs.get(name);
		}

		if (line == null || line.length() == 0)
			return new Ref(Ref.Storage.LOOSE, name, null);

		if (line.startsWith("ref: ")) {
			if (depth >= 5) {
				throw new IOException("Exceeded maximum ref depth of " + depth
						+ " at " + name + ".  Circular reference?");
			}

			final String target = line.substring("ref: ".length());
			final Ref r = readRefBasic(target, depth + 1);
			return r != null ? r : new Ref(Ref.Storage.LOOSE, target, null);
		}

		setModified();

		final ObjectId id;
		try {
			id = ObjectId.fromString(line);
		} catch (IllegalArgumentException notRef) {
			throw new IOException("Not a ref: " + name + ": " + line);
		}

		ref = new CachedRef(Ref.Storage.LOOSE, name, id, mtime);
		looseRefs.put(name, ref);
		return ref;
	}

	private void refreshPackedRefs() {
		final long currTime = packedRefsFile.lastModified();
		final long currLen = currTime == 0 ? 0 : packedRefsFile.length();
		if (currTime == packedRefsLastModified && currLen == packedRefsLength)
			return;
		if (currTime == 0) {
			packedRefsLastModified = 0;
			packedRefsLength = 0;
			packedRefs = new HashMap<String, Ref>();
			return;
		}

		final Map<String, Ref> newPackedRefs = new HashMap<String, Ref>();
		try {
			final BufferedReader b = openReader(packedRefsFile);
			try {
				String p;
				Ref last = null;
				while ((p = b.readLine()) != null) {
					if (p.charAt(0) == '#')
						continue;

					if (p.charAt(0) == '^') {
						if (last == null)
							throw new IOException("Peeled line before ref.");

						final ObjectId id = ObjectId.fromString(p.substring(1));
						last = new Ref(Ref.Storage.PACKED, last.getName(), last
								.getObjectId(), id);
						newPackedRefs.put(last.getName(), last);
						continue;
					}

					final int sp = p.indexOf(' ');
					final ObjectId id = ObjectId.fromString(p.substring(0, sp));
					final String name = new String(p.substring(sp + 1));
					last = new Ref(Ref.Storage.PACKED, name, id);
					newPackedRefs.put(last.getName(), last);
				}
			} finally {
				b.close();
			}
			packedRefsLastModified = currTime;
			packedRefsLength = currLen;
			packedRefs = newPackedRefs;
			setModified();
		} catch (FileNotFoundException noPackedRefs) {
			// Ignore it and leave the new map empty.
			//
			packedRefsLastModified = 0;
			packedRefsLength = 0;
			packedRefs = newPackedRefs;
		} catch (IOException e) {
			throw new RuntimeException("Cannot read packed refs", e);
		}
	}

	private static String readLine(final File file)
			throws FileNotFoundException, IOException {
		final BufferedReader br = openReader(file);
		try {
			return br.readLine();
		} finally {
			br.close();
		}
	}

	private static BufferedReader openReader(final File fileLocation)
			throws UnsupportedEncodingException, FileNotFoundException {
		return new BufferedReader(new InputStreamReader(new FileInputStream(
				fileLocation), CHAR_ENC));
	}

	private static class CachedRef extends Ref {
		final long lastModified;

		CachedRef(final Storage st, final String refName, final ObjectId id,
				final long mtime) {
			super(st, refName, id);
			lastModified = mtime;
		}
	}

}
