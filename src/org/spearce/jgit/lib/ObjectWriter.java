package org.spearce.jgit.lib;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.DeflaterOutputStream;

public class ObjectWriter {
    private static final String HASH_FUNCTION = "SHA-1";

    private final Repository r;

    private final byte[] copybuf;

    private final MessageDigest md;

    public ObjectWriter(final Repository d) {
        r = d;
        copybuf = new byte[8192];
        try {
            md = MessageDigest.getInstance(HASH_FUNCTION);
        } catch (NoSuchAlgorithmException nsae) {
            throw new WritingNotSupportedException("Required hash function "
                    + HASH_FUNCTION + " not available.", nsae);
        }
    }

    public ObjectId writeBlob(final byte[] b) throws IOException {
        return writeBlob(b.length, new ByteArrayInputStream(b));
    }

    public ObjectId writeBlob(final File f) throws IOException {
        final FileInputStream is = new FileInputStream(f);
        try {
            return writeBlob((int) f.length(), is);
        } finally {
            is.close();
        }
    }

    public ObjectId writeBlob(final int len, final InputStream is)
            throws IOException {
        return writeObject("blob", len, is);
    }

    public ObjectId writeTree(final byte[] b) throws IOException {
        return writeTree(b.length, new ByteArrayInputStream(b));
    }

    public ObjectId writeTree(final int len, final InputStream is)
            throws IOException {
        return writeObject("tree", len, is);
    }

    public ObjectId writeObject(final String type, int len, final InputStream is)
            throws IOException {
        final File t;
        final DeflaterOutputStream ts;
        ObjectId id = null;
        t = File.createTempFile("noz", null, r.getObjectsDirectory());
        ts = new DeflaterOutputStream(new FileOutputStream(t));
        try {
            final byte[] header = (type + ' ' + len + '\0').getBytes("UTF-8");
            int r;

            md.update(header);
            ts.write(header);

            while ((r = is.read(copybuf)) > 0 && len > 0) {
                if (r > len) {
                    r = len;
                }
                md.update(copybuf, 0, r);
                ts.write(copybuf, 0, r);
                len -= r;
            }
            if (len != 0) {
                throw new IOException("Input did not match supplied length. "
                        + len + " bytes are missing.");
            }

            ts.close();
            t.setReadOnly();
            id = new ObjectId(md.digest());
        } finally {
            if (id == null) {
                md.reset();
                try {
                    ts.close();
                } finally {
                    t.delete();
                }
            }
        }

        if (r.hasObject(id)) {
            // Object is already in the repository so remove the temporary file.
            //
            t.delete();
        } else {
            final File o = r.toFile(id);
            if (!t.renameTo(o)) {
                // Maybe the directory doesn't exist yet as the object
                // directories are always lazilly created. Note that we try the
                // rename first as the directory likely does exist.
                //
                o.getParentFile().mkdir();
                if (!t.renameTo(o)) {
                    if (!r.hasObject(id)) {
                        // The object failed to be renamed into its proper
                        // location and it doesn't exist in the repository
                        // either. We really don't know what went wrong, so
                        // abort.
                        //
                        t.delete();
                        throw new WritingNotSupportedException("Unable to"
                                + " create new object: " + o);
                    }
                }
            }
        }

        return id;
    }
}
