/*
 * Copyright (C) 2009, Google Inc.
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.spearce.jgit.errors.PackMismatchException;
import org.spearce.jgit.util.FS;

/**
 * Traditional file system based {@link ObjectDatabase}.
 * <p>
 * This is the classical object database representation for a Git repository,
 * where objects are stored loose by hashing them into directories by their
 * {@link ObjectId}, or are stored in compressed containers known as
 * {@link PackFile}s.
 */
public class ObjectDirectory extends ObjectDatabase {
	private static final PackFile[] NO_PACKS = {};

	private final File objects;

	private final File infoDirectory;

	private final File packDirectory;

	private final File alternatesFile;

	private final AtomicReference<PackFile[]> packList;

	private volatile long packDirectoryLastModified;

	/**
	 * Initialize a reference to an on-disk object directory.
	 *
	 * @param dir
	 *            the location of the <code>objects</code> directory.
	 */
	public ObjectDirectory(final File dir) {
		objects = dir;
		infoDirectory = new File(objects, "info");
		packDirectory = new File(objects, "pack");
		alternatesFile = new File(infoDirectory, "alternates");

		packList = new AtomicReference<PackFile[]>();
	}

	/**
	 * @return the location of the <code>objects</code> directory.
	 */
	public final File getDirectory() {
		return objects;
	}

	@Override
	public boolean exists() {
		return objects.exists();
	}

	@Override
	public void create() throws IOException {
		objects.mkdirs();
		infoDirectory.mkdir();
		packDirectory.mkdir();
	}

	@Override
	public void closeSelf() {
		PackFile[] packs = packList.get();
		if (packs != null) {
			packList.set(null);
			for (final PackFile p : packs) {
				p.close();
			}
		}
	}

	/**
	 * Compute the location of a loose object file.
	 *
	 * @param objectId
	 *            identity of the loose object to map to the directory.
	 * @return location of the object, if it were to exist as a loose object.
	 */
	public File fileFor(final AnyObjectId objectId) {
		return fileFor(objectId.name());
	}

	private File fileFor(final String objectName) {
		final String d = objectName.substring(0, 2);
		final String f = objectName.substring(2);
		return new File(new File(objects, d), f);
	}

	/**
	 * Add a single existing pack to the list of available pack files.
	 *
	 * @param pack
	 *            path of the pack file to open.
	 * @param idx
	 *            path of the corresponding index file.
	 * @throws IOException
	 *             index file could not be opened, read, or is not recognized as
	 *             a Git pack file index.
	 */
	public void openPack(final File pack, final File idx) throws IOException {
		final String p = pack.getName();
		final String i = idx.getName();

		if (p.length() != 50 || !p.startsWith("pack-") || !p.endsWith(".pack"))
			throw new IOException("Not a valid pack " + pack);

		if (i.length() != 49 || !i.startsWith("pack-") || !i.endsWith(".idx"))
			throw new IOException("Not a valid pack " + idx);

		if (!p.substring(0, 45).equals(i.substring(0, 45)))
			throw new IOException("Pack " + pack + "does not match index");

		insertPack(new PackFile(idx, pack));
	}

	@Override
	public String toString() {
		return "ObjectDirectory[" + getDirectory() + "]";
	}

	@Override
	protected boolean hasObject1(final AnyObjectId objectId) {
		for (final PackFile p : packs()) {
			try {
				if (p.hasObject(objectId)) {
					return true;
				}
			} catch (IOException e) {
				// The hasObject call should have only touched the index,
				// so any failure here indicates the index is unreadable
				// by this process, and the pack is likewise not readable.
				//
				removePack(p);
				continue;
			}
		}
		return false;
	}

	@Override
	protected ObjectLoader openObject1(final WindowCursor curs,
			final AnyObjectId objectId) throws IOException {
		PackFile[] pList = packs();
		SEARCH: for (;;) {
			for (final PackFile p : pList) {
				try {
					final PackedObjectLoader ldr = p.get(curs, objectId);
					if (ldr != null) {
						ldr.materialize(curs);
						return ldr;
					}
				} catch (PackMismatchException e) {
					// Pack was modified; refresh the entire pack list.
					//
					pList = scanPacks(pList);
					continue SEARCH;
				} catch (IOException e) {
					// Assume the pack is corrupted.
					//
					removePack(p);
				}
			}
			return null;
		}
	}

	@Override
	void openObjectInAllPacks1(final Collection<PackedObjectLoader> out,
			final WindowCursor curs, final AnyObjectId objectId)
			throws IOException {
		PackFile[] pList = packs();
		SEARCH: for (;;) {
			for (final PackFile p : pList) {
				try {
					final PackedObjectLoader ldr = p.get(curs, objectId);
					if (ldr != null) {
						out.add(ldr);
					}
				} catch (PackMismatchException e) {
					// Pack was modified; refresh the entire pack list.
					//
					pList = scanPacks(pList);
					continue SEARCH;
				} catch (IOException e) {
					// Assume the pack is corrupted.
					//
					removePack(p);
				}
			}
			break SEARCH;
		}
	}

	@Override
	protected boolean hasObject2(final String objectName) {
		return fileFor(objectName).exists();
	}

	@Override
	protected ObjectLoader openObject2(final WindowCursor curs,
			final String objectName, final AnyObjectId objectId)
			throws IOException {
		try {
			return new UnpackedObjectLoader(fileFor(objectName), objectId);
		} catch (FileNotFoundException noFile) {
			return null;
		}
	}

	@Override
	protected boolean tryAgain1() {
		final PackFile[] old = packList.get();
		if (packDirectoryLastModified < packDirectory.lastModified()) {
			scanPacks(old);
			return true;
		}
		return false;
	}

	private void insertPack(final PackFile pf) {
		PackFile[] o, n;
		do {
			o = packs();
			n = new PackFile[1 + o.length];
			n[0] = pf;
			System.arraycopy(o, 0, n, 1, o.length);
		} while (!packList.compareAndSet(o, n));
	}

	private void removePack(final PackFile deadPack) {
		PackFile[] o, n;
		do {
			o = packList.get();
			if (o == null || !inList(o, deadPack)) {
				break;

			} else if (o.length == 1) {
				n = NO_PACKS;

			} else {
				n = new PackFile[o.length - 1];
				int j = 0;
				for (final PackFile p : o) {
					if (p != deadPack) {
						n[j++] = p;
					}
				}
			}
		} while (!packList.compareAndSet(o, n));
		deadPack.close();
	}

	private static boolean inList(final PackFile[] list, final PackFile pack) {
		for (final PackFile p : list) {
			if (p == pack) {
				return true;
			}
		}
		return false;
	}

	private PackFile[] packs() {
		PackFile[] r = packList.get();
		if (r == null) {
			r = scanPacks(r);
		}
		return r;
	}

	private PackFile[] scanPacks(final PackFile[] original) {
		synchronized (packList) {
			PackFile[] o, n;
			do {
				o = packList.get();
				if (o != original) {
					// Another thread did the scan for us, while we
					// were blocked on the monitor above.
					//
					return o;
				}
				n = scanPacksImpl(o != null ? o : NO_PACKS);
			} while (!packList.compareAndSet(o, n));
			return n;
		}
	}

	private PackFile[] scanPacksImpl(final PackFile[] old) {
		final Map<String, PackFile> forReuse = reuseMap(old);
		final String[] idxList = listPackIdx();
		final List<PackFile> list = new ArrayList<PackFile>(idxList.length);
		for (final String indexName : idxList) {
			final String base = indexName.substring(0, indexName.length() - 4);
			final String packName = base + ".pack";

			final PackFile oldPack = forReuse.remove(packName);
			if (oldPack != null) {
				list.add(oldPack);
				continue;
			}

			final File packFile = new File(packDirectory, packName);
			if (!packFile.isFile()) {
				// Sometimes C Git's HTTP fetch transport leaves a
				// .idx file behind and does not download the .pack.
				// We have to skip over such useless indexes.
				//
				continue;
			}

			final File idxFile = new File(packDirectory, indexName);
			list.add(new PackFile(idxFile, packFile));
		}

		for (final PackFile p : forReuse.values()) {
			p.close();
		}

		if (list.isEmpty()) {
			return NO_PACKS;
		}
		final PackFile[] r = list.toArray(new PackFile[list.size()]);
		Arrays.sort(r, PackFile.SORT);
		return r;
	}

	private static Map<String, PackFile> reuseMap(final PackFile[] old) {
		final Map<String, PackFile> forReuse = new HashMap<String, PackFile>();
		for (final PackFile p : old) {
			if (p.invalid()) {
				// The pack instance is corrupted, and cannot be safely used
				// again. Do not include it in our reuse map.
				//
				p.close();
				continue;
			}

			final PackFile prior = forReuse.put(p.getPackFile().getName(), p);
			if (prior != null) {
				// This should never occur. It should be impossible for us
				// to have two pack files with the same name, as all of them
				// came out of the same directory. If it does, we promised to
				// close any PackFiles we did not reuse, so close the one we
				// just evicted out of the reuse map.
				//
				prior.close();
			}
		}
		return forReuse;
	}

	private String[] listPackIdx() {
		packDirectoryLastModified = packDirectory.lastModified();
		final String[] idxList = packDirectory.list(new FilenameFilter() {
			public boolean accept(final File baseDir, final String n) {
				// Must match "pack-[0-9a-f]{40}.idx" to be an index.
				return n.length() == 49 && n.endsWith(".idx")
						&& n.startsWith("pack-");
			}
		});
		return idxList != null ? idxList : new String[0];
	}

	@Override
	protected ObjectDatabase[] loadAlternates() throws IOException {
		final BufferedReader br = open(alternatesFile);
		final List<ObjectDirectory> l = new ArrayList<ObjectDirectory>(4);
		try {
			String line;
			while ((line = br.readLine()) != null) {
				l.add(new ObjectDirectory(FS.resolve(objects, line)));
			}
		} finally {
			br.close();
		}

		if (l.isEmpty()) {
			return NO_ALTERNATES;
		}
		return l.toArray(new ObjectDirectory[l.size()]);
	}

	private static BufferedReader open(final File f)
			throws FileNotFoundException {
		return new BufferedReader(new FileReader(f));
	}
}
