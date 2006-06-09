package org.spearce.jgit.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class ObjectWriter
{
    private static final TreeNameComparator TNC = new TreeNameComparator();

    private static final byte[] TREE_MODE;

    private static final byte[] SYMLINK_MODE;

    private static final byte[] PLAIN_FILE_MODE;

    private static final byte[] EXECUTABLE_FILE_MODE;

    static
    {
        try
        {
            TREE_MODE = "40000".getBytes("UTF-8");
            SYMLINK_MODE = "120000".getBytes("UTF-8");
            PLAIN_FILE_MODE = "100644".getBytes("UTF-8");
            EXECUTABLE_FILE_MODE = "100755".getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uue)
        {
            throw new ExceptionInInitializerError(uue);
        }
    }

    private final Repository r;

    private final byte[] buf;

    private final MessageDigest md;

    public ObjectWriter(final Repository d)
    {
        r = d;
        buf = new byte[8192];
        md = Constants.newMessageDigest();
    }

    public ObjectId writeBlob(final byte[] b) throws IOException
    {
        return writeBlob(b.length, new ByteArrayInputStream(b));
    }

    public ObjectId writeBlob(final File f) throws IOException
    {
        final FileInputStream is = new FileInputStream(f);
        try
        {
            return writeBlob(f.length(), is);
        }
        finally
        {
            is.close();
        }
    }

    public ObjectId writeBlob(final long len, final InputStream is)
        throws IOException
    {
        return writeObject(Constants.TYPE_BLOB, len, is);
    }

    public ObjectId writeTree(final Tree t) throws IOException
    {
        final ByteArrayOutputStream o = new ByteArrayOutputStream();
        final ArrayList r = new ArrayList();
        Iterator i;

        i = t.entryIterator();
        while (i.hasNext())
        {
            r.add(i.next());
        }
        Collections.sort(r, TNC);

        i = r.iterator();
        while (i.hasNext())
        {
            final TreeEntry e = (TreeEntry) i.next();
            final byte[] mode;

            if (e instanceof Tree)
            {
                mode = TREE_MODE;
            }
            else if (e instanceof SymlinkTreeEntry)
            {
                mode = SYMLINK_MODE;
            }
            else if (e instanceof FileTreeEntry)
            {
                mode = ((FileTreeEntry) e).isExecutable()
                    ? EXECUTABLE_FILE_MODE
                    : PLAIN_FILE_MODE;
            }
            else
            {
                throw new WritingNotSupportedException("Object type not"
                    + " supported as member of Tree:"
                    + e);
            }

            o.write(mode);
            o.write(' ');
            o.write(e.getNameUTF8());
            o.write(0);
            o.write(e.getId().getBytes());
        }

        return writeTree(o.toByteArray());
    }

    public ObjectId writeTree(final byte[] b) throws IOException
    {
        return writeTree(b.length, new ByteArrayInputStream(b));
    }

    public ObjectId writeTree(final long len, final InputStream is)
        throws IOException
    {
        return writeObject(Constants.TYPE_TREE, len, is);
    }

    public ObjectId writeCommit(final Commit c) throws IOException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");

        w.write("tree ");
        w.write(c.getTreeId().toString());
        w.write('\n');

        final Iterator i = c.getParentIds().iterator();
        while (i.hasNext())
        {
            w.write("parent ");
            w.write(i.next().toString());
            w.write('\n');
        }

        w.write("author ");
        w.write(c.getAuthor().toString());
        w.write('\n');

        w.write("committer ");
        w.write(c.getCommitter().toString());
        w.write('\n');

        w.write('\n');
        w.write(c.getMessage());
        w.close();

        return writeCommit(os.toByteArray());
    }

    public ObjectId writeCommit(final byte[] b) throws IOException
    {
        return writeCommit(b.length, new ByteArrayInputStream(b));
    }

    public ObjectId writeCommit(final long len, final InputStream is)
        throws IOException
    {
        return writeObject(Constants.TYPE_COMMIT, len, is);
    }

    public ObjectId writeObject(
        final String type,
        long len,
        final InputStream is) throws IOException
    {
        final File t;
        final DeflaterOutputStream ts;
        ObjectId id = null;
        t = File.createTempFile("noz", null, r.getObjectsDirectory());
        ts = new DeflaterOutputStream(new FileOutputStream(t), new Deflater(
            Deflater.BEST_COMPRESSION));
        try
        {
            final byte[] header = (type + ' ' + len + '\0').getBytes("UTF-8");
            int r;

            md.update(header);
            ts.write(header);

            while (len > 0
                && (r = is.read(buf, 0, (int) Math.min(len, buf.length))) > 0)
            {
                md.update(buf, 0, r);
                ts.write(buf, 0, r);
                len -= r;
            }
            if (len != 0)
            {
                throw new IOException("Input did not match supplied length. "
                    + len
                    + " bytes are missing.");
            }

            ts.close();
            t.setReadOnly();
            id = new ObjectId(md.digest());
        }
        finally
        {
            if (id == null)
            {
                md.reset();
                try
                {
                    ts.close();
                }
                finally
                {
                    t.delete();
                }
            }
        }

        if (r.hasObject(id))
        {
            // Object is already in the repository so remove the temporary file.
            //
            t.delete();
        }
        else
        {
            final File o = r.toFile(id);
            if (!t.renameTo(o))
            {
                // Maybe the directory doesn't exist yet as the object
                // directories are always lazilly created. Note that we try the
                // rename first as the directory likely does exist.
                //
                o.getParentFile().mkdir();
                if (!t.renameTo(o))
                {
                    if (!r.hasObject(id))
                    {
                        // The object failed to be renamed into its proper
                        // location and it doesn't exist in the repository
                        // either. We really don't know what went wrong, so
                        // fail.
                        //
                        t.delete();
                        throw new WritingNotSupportedException("Unable to"
                            + " create new object: "
                            + o);
                    }
                }
            }
        }

        return id;
    }
}
