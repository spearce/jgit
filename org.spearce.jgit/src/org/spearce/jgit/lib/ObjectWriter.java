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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.spearce.jgit.errors.ObjectWritingException;

public class ObjectWriter {
	private static final byte[] htree = Constants.encodeASCII("tree");

	private static final byte[] hparent = Constants.encodeASCII("parent");

	private static final byte[] hauthor = Constants.encodeASCII("author");

	private static final byte[] hcommitter = Constants.encodeASCII("committer");

	private static final byte[] hencoding = Constants.encodeASCII("encoding");

	private final Repository r;

	private final byte[] buf;

	private final MessageDigest md;

	private final Deflater def;

	private final boolean legacyHeaders;

	public ObjectWriter(final Repository d) {
		r = d;
		buf = new byte[8192];
		md = Constants.newMessageDigest();
		def = new Deflater(r.getConfig().getCore().getCompression());
		legacyHeaders = r.getConfig().getCore().useLegacyHeaders();
	}

	public ObjectId writeBlob(final byte[] b) throws IOException {
		return writeBlob(b.length, new ByteArrayInputStream(b));
	}

	public ObjectId writeBlob(final File f) throws IOException {
		final FileInputStream is = new FileInputStream(f);
		try {
			return writeBlob(f.length(), is);
		} finally {
			is.close();
		}
	}

	public ObjectId writeBlob(final long len, final InputStream is)
			throws IOException {
		return writeObject(Constants.OBJ_BLOB, Constants.TYPE_BLOB, len, is);
	}

	public ObjectId writeTree(final Tree t) throws IOException {
		final ByteArrayOutputStream o = new ByteArrayOutputStream();
		final TreeEntry[] items = t.members();
		for (int k = 0; k < items.length; k++) {
			final TreeEntry e = items[k];
			final ObjectId id = e.getId();

			if (id == null)
				throw new ObjectWritingException("Object at path \""
						+ e.getFullName() + "\" does not have an id assigned."
						+ "  All object ids must be assigned prior"
						+ " to writing a tree.");

			e.getMode().copyTo(o);
			o.write(' ');
			o.write(e.getNameUTF8());
			o.write(0);
			o.write(id.getBytes());
		}
		return writeTree(o.toByteArray());
	}

	public ObjectId writeTree(final byte[] b) throws IOException {
		return writeTree(b.length, new ByteArrayInputStream(b));
	}

	public ObjectId writeTree(final long len, final InputStream is)
			throws IOException {
		return writeObject(Constants.OBJ_TREE, Constants.TYPE_TREE, len, is);
	}

	public ObjectId writeCommit(final Commit c) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		String encoding = c.getEncoding();
		if (encoding == null)
			encoding = Constants.CHARACTER_ENCODING;
		final OutputStreamWriter w = new OutputStreamWriter(os, encoding);

		os.write(htree);
		os.write(' ');
		c.getTreeId().copyTo(os);
		os.write('\n');

		final Iterator i = c.getParentIds().iterator();
		while (i.hasNext()) {
			os.write(hparent);
			os.write(' ');
			((ObjectId) i.next()).copyTo(os);
			os.write('\n');
		}

		os.write(hauthor);
		os.write(' ');
		w.write(c.getAuthor().toExternalString());
		w.flush();
		os.write('\n');

		os.write(hcommitter);
		os.write(' ');
		w.write(c.getCommitter().toExternalString());
		w.flush();
		os.write('\n');

		if (!encoding.equals("UTF-8")) {
			os.write(hencoding);
			os.write(' ');
			os.write(Constants.encodeASCII(encoding));
			os.write('\n');
		}

		os.write('\n');
		w.write(c.getMessage());
		w.flush();

		return writeCommit(os.toByteArray());
	}

	public ObjectId writeTag(final byte[] b) throws IOException {
		return writeTag(b.length, new ByteArrayInputStream(b));
	}

	public ObjectId writeTag(final Tag c) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final OutputStreamWriter w = new OutputStreamWriter(os,
				Constants.CHARACTER_ENCODING);

		w.write("object ");
		c.getObjId().copyTo(w);
		w.write('\n');

		w.write("type ");
		w.write(c.getType());
		w.write("\n");

		w.write("tag ");
		w.write(c.getTag());
		w.write("\n");

		w.write("tagger ");
		w.write(c.getAuthor().toExternalString());
		w.write('\n');

		w.write('\n');
		w.write(c.getMessage());
		w.close();

		return writeTag(os.toByteArray());
	}

	public ObjectId writeCommit(final byte[] b) throws IOException {
		return writeCommit(b.length, new ByteArrayInputStream(b));
	}

	public ObjectId writeCommit(final long len, final InputStream is)
			throws IOException {
		return writeObject(Constants.OBJ_COMMIT, Constants.TYPE_COMMIT, len, is);
	}

	public ObjectId writeTag(final long len, final InputStream is)
		throws IOException {
		return writeObject(Constants.OBJ_TAG, Constants.TYPE_TAG, len, is);
	}

	public ObjectId writeObject(final int typeCode, final String type,
			long len, final InputStream is) throws IOException {
		final File t;
		final DeflaterOutputStream deflateStream;
		final FileOutputStream fileStream;
		ObjectId id = null;

		t = File.createTempFile("noz", null, r.getObjectsDirectory());
		fileStream = new FileOutputStream(t);
		if (!legacyHeaders) {
			long sz = len;
			int c = ((typeCode & 7) << 4) | (int) (sz & 0xf);
			sz >>= 4;
			while (sz > 0) {
				fileStream.write(c | 0x80);
				c = (int) (sz & 0x7f);
				sz >>= 7;
			}
			fileStream.write(c);
		}

		md.reset();
		def.reset();
		deflateStream = new DeflaterOutputStream(fileStream, def);

		try {
			byte[] header;
			int r;

			header = Constants.encodeASCII(type);
			md.update(header);
			if (legacyHeaders)
				deflateStream.write(header);

			md.update((byte) ' ');
			if (legacyHeaders)
				deflateStream.write((byte) ' ');

			header = Constants.encodeASCII(len);
			md.update(header);
			if (legacyHeaders)
				deflateStream.write(header);

			md.update((byte) 0);
			if (legacyHeaders)
				deflateStream.write((byte) 0);

			while (len > 0
					&& (r = is.read(buf, 0, (int) Math.min(len, buf.length))) > 0) {
				md.update(buf, 0, r);
				deflateStream.write(buf, 0, r);
				len -= r;
			}

			if (len != 0)
				throw new IOException("Input did not match supplied length. "
						+ len + " bytes are missing.");

			deflateStream.close();
			t.setReadOnly();
			id = new ObjectId(md.digest());
		} finally {
			if (id == null) {
				try {
					deflateStream.close();
				} finally {
					t.delete();
				}
			}
		}

		if (r.hasObject(id)) {
			// Object is already in the repository so remove
			// the temporary file.
			//
			t.delete();
		} else {
			final File o = r.toFile(id);
			if (!t.renameTo(o)) {
				// Maybe the directory doesn't exist yet as the object
				// directories are always lazily created. Note that we
				// try the rename first as the directory likely does exist.
				//
				o.getParentFile().mkdir();
				if (!t.renameTo(o)) {
					if (!r.hasObject(id)) {
						// The object failed to be renamed into its proper
						// location and it doesn't exist in the repository
						// either. We really don't know what went wrong, so
						// fail.
						//
						t.delete();
						throw new ObjectWritingException("Unable to"
								+ " create new object: " + o);
					}
				}
			}
		}

		return id;
	}
}
