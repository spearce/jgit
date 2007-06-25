package org.spearce.jgit.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.spearce.jgit.errors.CorruptObjectException;

public class GitIndex {

	/** Stage 0 represents merged entries. */
	public static final int STAGE_0 = 0;

	private RandomAccessFile cache;

	private File cacheFile;

	// Index is modified
	private boolean changed;

	// Stat information updated
	private boolean statDirty;

	private Header header;

	private long lastCacheTime;

	private final Repository db;

	private Map entries = new TreeMap(new Comparator() {
		public int compare(Object arg0, Object arg1) {
			byte[] a = (byte[]) arg0;
			byte[] b = (byte[]) arg1;
			for (int i = 0; i < a.length && i < b.length; ++i) {
				int c = a[i] - b[i];
				if (c != 0)
					return c;
			}
			if (a.length < b.length)
				return -1;
			else if (a.length > b.length)
				return 1;
			return 0;
		}
	});

	public GitIndex(Repository db) {
		this.db = db;
		this.cacheFile = new File(db.getDirectory(), "index");
	}

	public boolean isChanged() {
		return changed || statDirty;
	}

	public void rereadIfNecessary() throws IOException {
		if (cacheFile.exists() && cacheFile.lastModified() != lastCacheTime) {
			read();
		}
	}

	public void add(File wd, File f) throws IOException {
		byte[] key = Entry.makeKey(wd, f);
		Entry e = (Entry) entries.get(key);
		if (e == null) {
			e = new Entry(key, f, 0, this);
			entries.put(key, e);
		} else {
			e.update(f, db);
		}
	}

	public void remove(File wd, File f) {
		byte[] key = Entry.makeKey(wd, f);
		entries.remove(key);
	}

	public void read() throws IOException {
		long t0 = System.currentTimeMillis();
		changed = false;
		statDirty = false;
		cache = new RandomAccessFile(cacheFile, "r");
		try {
			MappedByteBuffer map = cache.getChannel().map(MapMode.READ_ONLY, 0,
					cacheFile.length());
			map.order(ByteOrder.BIG_ENDIAN);
			header = new Header(map);
			entries.clear();
			for (int i = 0; i < header.entries; ++i) {
				Entry entry = new Entry(this, map);
				entries.put(entry.name, entry);
			}
			long t1 = System.currentTimeMillis();
			lastCacheTime = cacheFile.lastModified();
			System.out.println("Read index "+cacheFile+" in "+((t1-t0)/1000.0)+"s");
		} finally {
			cache.close();
		}
	}

	public void write() throws IOException {
		checkWriteOk();
		File tmpIndex = new File(cacheFile.getAbsoluteFile() + ".tmp");
		File lock = new File(cacheFile.getAbsoluteFile() + ".lock");
		if (!lock.createNewFile())
			throw new IOException("Index file is in use");
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(tmpIndex);
			FileChannel fc = fileOutputStream.getChannel();
			ByteBuffer buf = ByteBuffer.allocate(4096);
			MessageDigest newMessageDigest = Constants.newMessageDigest();
			header = new Header(entries);
			header.write(buf);
			buf.flip();
			newMessageDigest
					.update(buf.array(), buf.arrayOffset(), buf.limit());
			fc.write(buf);
			buf.flip();
			buf.clear();
			for (Iterator i = entries.values().iterator(); i.hasNext();) {
				Entry e = (Entry) i.next();
				e.write(buf);
				buf.flip();
				newMessageDigest.update(buf.array(), buf.arrayOffset(), buf
						.limit());
				fc.write(buf);
				buf.flip();
				buf.clear();
			}
			buf.put(newMessageDigest.digest());
			buf.flip();
			fc.write(buf);
			fc.close();
			fileOutputStream.close();
			if (!tmpIndex.renameTo(cacheFile))
				throw new IOException(
						"Could not rename temporary index file to index");
			changed = false;
			statDirty = false;
		} finally {
			if (!lock.delete())
				throw new IOException(
						"Could not delete lock file. Should not happen");
			if (tmpIndex.exists() && !tmpIndex.delete())
				throw new IOException(
						"Could not delete temporary index file. Should not happen");
		}
	}

	private void checkWriteOk() throws IOException {
		for (Iterator i = entries.values().iterator(); i.hasNext();) {
			Entry e = (Entry) i.next();
			if (e.stage != 0) {
				throw new IOException("Cannot work with other stages than zero right now. Won't write corrupt index.");
			}
		}
	}

	public static class Entry {
		private long ctime;

		private long mtime;

		private int dev;

		private int ino;

		private int mode;

		private int uid;

		private int gid;

		private int size;

		private ObjectId sha1;

		private short flags;

		private byte[] name;

		private int stage;

		private GitIndex theIndex;

		static byte[] makeKey(File wd, File f) {
			if (!f.getPath().startsWith(wd.getPath()))
				throw new Error("Path is not in working dir");
			String relName = f.getPath().substring(wd.getPath().length() + 1)
					.replace(File.separatorChar, '/');
			return relName.getBytes();
		}

		public Entry(byte[] key, File f, int stage, GitIndex index)
				throws IOException {
			theIndex = index;
			ctime = f.lastModified() * 1000000L;
			mtime = ctime; // we use same here
			dev = -1;
			ino = -1;
			mode = FileMode.REGULAR_FILE.getBits();
			uid = -1;
			gid = -1;
			size = (int) f.length();
			ObjectWriter writer = new ObjectWriter(theIndex.db);
			sha1 = writer.writeBlob(f);
			name = key;
			flags = (short) ((stage << 12) | name.length); // TODO: fix flags
		}

		public Entry(TreeEntry f, int stage, GitIndex index)
				throws UnsupportedEncodingException {
			theIndex = index;
			ctime = -1; // hmm
			mtime = -1;
			dev = -1;
			ino = -1;
			mode = f.getMode().getBits();
			uid = -1;
			gid = -1;
			size = -1;
			sha1 = f.getId();
			name = f.getFullName().getBytes("UTF-8");
			flags = (short) ((stage << 12) | name.length); // TODO: fix flags
		}

		Entry(GitIndex index, ByteBuffer b) {
			theIndex = index;
			int startposition = b.position();
			ctime = b.getInt() * 1000000000L + (b.getInt() % 1000000000L);
			mtime = b.getInt() * 1000000000L + (b.getInt() % 1000000000L);
			dev = b.getInt();
			ino = b.getInt();
			mode = b.getInt();
			uid = b.getInt();
			gid = b.getInt();
			size = b.getInt();
			byte[] sha1bytes = new byte[Constants.OBJECT_ID_LENGTH];
			b.get(sha1bytes);
			sha1 = new ObjectId(sha1bytes);
			flags = b.getShort();
			stage = (flags & 0x3000) >> 12;
			name = new byte[flags & 0xFFF];
			b.get(name);
			b
					.position(startposition
							+ ((8 + 8 + 4 + 4 + 4 + 4 + 4 + 4 + 20 + 2
									+ name.length + 8) & ~7));
		}

		public boolean update(File f, Repository db) throws IOException {
			boolean modified = false;
			long lm = f.lastModified() * 1000000L;
			if (mtime != lm)
				modified = true;
			mtime = f.lastModified() * 1000000L;
			if (size != f.length())
				modified = true;
			if (File_canExecute(f) != FileMode.EXECUTABLE_FILE.equals(mode)) {
				mode = FileMode.EXECUTABLE_FILE.getBits();
				modified = true;
			}
			if (modified) {
				size = (int) f.length();
				ObjectWriter writer = new ObjectWriter(db);
				ObjectId newsha1 = sha1 = writer.writeBlob(f);
				if (!newsha1.equals(sha1))
					modified = true;
				sha1 = newsha1;
			}
			return modified;
		}

		public void write(ByteBuffer buf) {
			int startposition = buf.position();
			buf.putInt((int) (ctime / 1000000000L));
			buf.putInt((int) (ctime % 1000000000L));
			buf.putInt((int) (mtime / 1000000000L));
			buf.putInt((int) (mtime % 1000000000L));
			buf.putInt(dev);
			buf.putInt(ino);
			buf.putInt(mode);
			buf.putInt(uid);
			buf.putInt(gid);
			buf.putInt(size);
			buf.put(sha1.getBytes());
			buf.putShort(flags);
			buf.put(name);
			int end = startposition
					+ ((8 + 8 + 4 + 4 + 4 + 4 + 4 + 4 + 20 + 2 + name.length + 8) & ~7);
			int remain = end - buf.position();
			while (remain-- > 0)
				buf.put((byte) 0);
		}

		static Method canExecute;
		static {
			try {
				canExecute = File.class.getMethod("canExecute", (Class[]) null);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/*
		 * JDK1.6 has file.canExecute
		 * 
		 * if (file.canExecute() != FileMode.EXECUTABLE_FILE.equals(mode))
		 * return true;
		 */
		boolean File_canExecute(File f) {
			if (canExecute != null) {
				try {
					return ((Boolean) canExecute.invoke(f, (Object[]) null))
							.booleanValue();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
			} else
				return false;
		}

		/**
		 * Check if an entry's content is different from the cache, 
		 * 
		 * File status information is used and status is same we
		 * consider the file identical to the state in the working
		 * directory. Native git uses more stat fields than we
		 * have accessible in Java.
		 * 
		 * @param wd working directory to compare content with
		 * @return true if content is most likely different.
		 */
		public boolean isModified(File wd) {
			return isModified(wd, false);
		}

		/**
		 * Check if an entry's content is different from the cache, 
		 * 
		 * File status information is used and status is same we
		 * consider the file identical to the state in the working
		 * directory. Native git uses more stat fields than we
		 * have accessible in Java.
		 * 
		 * @param wd working directory to compare content with
		 * @param forceContentCheck True if the actual file content
		 * should be checked if modification time differs.
		 * 
		 * @return true if content is most likely different.
		 */
		public boolean isModified(File wd, boolean forceContentCheck) {
			File file = getFile(wd);
			if (!file.exists())
				return true;

			// JDK1.6 has file.canExecute
			// if (file.canExecute() != FileMode.EXECUTABLE_FILE.equals(mode))
			// return true;
			if (FileMode.EXECUTABLE_FILE.equals(mode)) {
				if (!File_canExecute(file))
					return true;
			} else {
				if (FileMode.REGULAR_FILE.equals(mode)) {
					if (!file.isFile())
						return true;
					if (File_canExecute(file))
						return true;
				} else {
					if (FileMode.SYMLINK.equals(mode)) {
						return true;
					} else {
						if (FileMode.TREE.equals(mode)) {
							if (!file.isDirectory())
								return true;
						} else {
							System.out.println("Does not handle mode "+mode+" ("+file+")");
							return true;
						}
					}
				}
			}

			long javamtime = mtime / 1000000L;
			long lastm = file.lastModified();
			if (file.length() != size)
				return true;
			if (lastm != javamtime) {
				if (!forceContentCheck)
					return true;

				try {
					InputStream is = new FileInputStream(file);
					ObjectWriter objectWriter = new ObjectWriter(theIndex.db);
					try {
						ObjectId newId = objectWriter.computeBlobSha1(file
								.length(), is);
						boolean ret = !newId.equals(sha1);
						theIndex.statDirty = true;
						return ret;
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							// can't happen, but if it does we ignore it
							e.printStackTrace();
						}
					}
				} catch (FileNotFoundException e) {
					// should not happen because we already checked this
					e.printStackTrace();
					throw new Error(e);
				}
			}
			return false;
		}

		private File getFile(File wd) {
			return new File(wd, getName());
		}

		public String toString() {
			return new String(name) + "/SHA-1(" + sha1 + ")/M:"
					+ new Date(ctime / 1000000L) + "/C:"
					+ new Date(mtime / 1000000L) + "/d" + dev + "/i" + ino
					+ "/m" + Integer.toString(mode, 8) + "/u" + uid + "/g"
					+ gid + "/s" + size + "/f" + flags + "/@" + stage;
		}

		public String getName() {
			return new String(name);
		}

		public ObjectId getObjectId() {
			return sha1;
		}

		public int getStage() {
			return stage;
		}
	}

	static class Header {
		private int signature;

		private int version;

		int entries;

		public Header(ByteBuffer map) throws CorruptObjectException {
			read(map);
		}

		private void read(ByteBuffer buf) throws CorruptObjectException {
			signature = buf.getInt();
			version = buf.getInt();
			entries = buf.getInt();
			if (signature != 0x44495243)
				throw new CorruptObjectException("Index signature is invalid: "
						+ signature);
			if (version != 2)
				throw new CorruptObjectException(
						"Unknow index version (or corrupt index):" + version);
		}

		public void write(ByteBuffer buf) {
			buf.order(ByteOrder.BIG_ENDIAN);
			buf.putInt(signature);
			buf.putInt(version);
			buf.putInt(entries);
		}

		public Header(Map entryset) {
			signature = 0x44495243;
			version = 2;
			entries = entryset.size();
		}
	}

	public void readTree(Tree t) throws IOException {
		readTree("", t);
	}

	public void readTree(String prefix, Tree t) throws IOException {
		TreeEntry[] members = t.members();
		for (int i = 0; i < members.length; ++i) {
			TreeEntry te = members[i];
			String name;
			if (prefix.length() > 0)
				name = prefix + "/" + te.getName();
			else
				name = te.getName();
			if (te instanceof Tree) {
				readTree(name, (Tree) te);
			} else {
				Entry e = new Entry(te, 0, this);
				entries.put(name.getBytes("UTF-8"), e);
			}
		}
	}

	public void checkout(File wd) throws IOException {
		for (Iterator i = entries.values().iterator(); i.hasNext();) {
			Entry e = (Entry) i.next();
			if (e.stage != 0)
				continue;
			ObjectLoader ol = db.openBlob(e.sha1);
			byte[] bytes = ol.getBytes();
			File file = new File(wd, e.getName());
			file.delete();
			file.getParentFile().mkdirs();
			FileChannel channel = new FileOutputStream(file).getChannel();
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			int j = channel.write(buffer);
			if (j != bytes.length)
				throw new IOException("Could not write file " + file);
			channel.close();
		}
	}

	public ObjectId writeTree() throws IOException {
		checkWriteOk();
		ObjectWriter writer = new ObjectWriter(db);
		Tree current = new Tree(db);
		Stack trees = new Stack();
		trees.push(current);
		String[] prevName = new String[0];
		for (Iterator i = entries.values().iterator(); i.hasNext();) {
			Entry e = (Entry) i.next();
			if (e.stage != 0)
				continue;
			String[] newName = splitDirPath(e.getName());
			int c = longestCommonPath(prevName, newName);
			while (c < trees.size() - 1) {
				current.setId(writer.writeTree(current));
				trees.pop();
				current = trees.isEmpty() ? null : (Tree) trees.peek();
			}
			while (trees.size() < newName.length) {
				if (!current.existsTree(newName[trees.size() - 1])) {
					current = new Tree(current, newName[trees.size() - 1]
							.getBytes());
					current.getParent().addEntry(current);
					trees.push(current);
				} else {
					current = (Tree) current.findTreeMember(newName[trees
							.size() - 1]);
					trees.push(current);
				}
			}
			FileTreeEntry ne = new FileTreeEntry(current, e.sha1,
					newName[newName.length - 1].getBytes(),
					(e.mode & FileMode.EXECUTABLE_FILE.getBits()) == FileMode.EXECUTABLE_FILE.getBits());
			current.addEntry(ne);
		}
		while (!trees.isEmpty()) {
			current.setId(writer.writeTree(current));
			trees.pop();
			if (!trees.isEmpty())
				current = (Tree) trees.peek();
		}
		return current.getTreeId();
	}

	String[] splitDirPath(String name) {
		String[] tmp = new String[name.length() / 2 + 1];
		int p0 = -1;
		int p1;
		int c = 0;
		while ((p1 = name.indexOf('/', p0 + 1)) != -1) {
			tmp[c++] = name.substring(p0 + 1, p1);
			p0 = p1;
		}
		tmp[c++] = name.substring(p0 + 1);
		String[] ret = new String[c];
		for (int i = 0; i < c; ++i) {
			ret[i] = tmp[i];
		}
		return ret;
	}

	int longestCommonPath(String[] a, String[] b) {
		int i;
		for (i = 0; i < a.length && i < b.length; ++i)
			if (!a[i].equals(b[i]))
				return i;
		return i;
	}

	public Entry[] getMembers() {
		return (Entry[]) entries.values().toArray(new Entry[entries.size()]);
	}

	public Entry getEntry(String path) throws UnsupportedEncodingException {
		return (Entry) entries.get(path.getBytes("ISO-8859-1"));
	}

	public Repository getRepository() {
		return db;
	}
}
