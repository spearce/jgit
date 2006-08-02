/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.ObjectWritingException;

public class Repository
{
    private static final String[] refSearchPaths = {
        "",
        "refs/",
        "refs/tags/",
        "refs/heads/",};

    private final File gitDir;

    private final File objectsDir;

    private final File refsDir;

    private final List packs;

    private final RepositoryConfig config;

    public Repository(final File d) throws IOException
    {
        gitDir = d.getAbsoluteFile();
        objectsDir = new File(gitDir, "objects");
        refsDir = new File(gitDir, "refs");
        packs = new ArrayList();
        config = new RepositoryConfig(this);
        if (objectsDir.exists())
        {
            getConfig().load();
            final String repositoryFormatVersion = getConfig().getString(
                "core",
                "repositoryFormatVersion");
            if (!"0".equals(repositoryFormatVersion))
            {
                throw new IOException("Unknown repository format \""
                    + repositoryFormatVersion
                    + "\"; expected \"0\".");
            }
            scanForPacks();
        }
    }

    public void create() throws IOException
    {
        if (gitDir.exists())
        {
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
    }

    public File getDirectory()
    {
        return gitDir;
    }

    public File getObjectsDirectory()
    {
        return objectsDir;
    }

    public RepositoryConfig getConfig()
    {
        return config;
    }

    public File toFile(final ObjectId objectId)
    {
        final String n = objectId.toString();
        return new File(new File(objectsDir, n.substring(0, 2)), n.substring(2));
    }

    public boolean hasObject(final ObjectId objectId)
    {
        if (toFile(objectId).isFile())
        {
            return true;
        }

        final Iterator i = packs.iterator();
        while (i.hasNext())
        {
            final PackReader p = (PackReader) i.next();
            try
            {
                final ObjectReader o = p.get(objectId);
                if (o != null)
                {
                    o.close();
                    return true;
                }
            }
            catch (IOException ioe)
            {
                // This shouldn't happen unless the pack was corrupted after we
                // opened it. We'll ignore the error as though the object does
                // not exist in this pack.
                //
            }
        }
        return false;
    }

    public ObjectReader openObject(final ObjectId id) throws IOException
    {
        final XInputStream fis = openObjectStream(id);
        if (fis != null)
        {
            try
            {
                return new UnpackedObjectReader(id, fis);
            }
            catch (IOException ioe)
            {
                fis.close();
                throw ioe;
            }
        }
        else
        {
            return objectInPack(id);
        }
    }

    public ObjectReader openBlob(final ObjectId id) throws IOException
    {
        final ObjectReader or = openObject(id);
        if (or == null)
        {
            return null;
        }
        else if (Constants.TYPE_BLOB.equals(or.getType()))
        {
            return or;
        }
        else
        {
            or.close();
            throw new IncorrectObjectTypeException(id, Constants.TYPE_BLOB);
        }
    }

    public ObjectReader openTree(final ObjectId id) throws IOException
    {
        final ObjectReader or = openObject(id);
        if (or == null)
        {
            return null;
        }
        else if (Constants.TYPE_TREE.equals(or.getType()))
        {
            return or;
        }
        else
        {
            or.close();
            throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
        }
    }

    public Commit mapCommit(final String revstr) throws IOException
    {
        final ObjectId id = resolve(revstr);
        return id != null ? mapCommit(id) : null;
    }

    public Commit mapCommit(final ObjectId id) throws IOException
    {
        final ObjectReader or = openObject(id);
        if (or == null)
        {
            return null;
        }
        else if (Constants.TYPE_COMMIT.equals(or.getType()))
        {
            return new Commit(this, id, or.getBufferedReader());
        }
        else
        {
            or.close();
            throw new IncorrectObjectTypeException(id, Constants.TYPE_COMMIT);
        }
    }

    public Tree mapTree(final String revstr) throws IOException
    {
        final ObjectId id = resolve(revstr);
        return id != null ? mapTree(id) : null;
    }

    public Tree mapTree(final ObjectId id) throws IOException
    {
        final ObjectReader or = openObject(id);
        if (or == null)
        {
            return null;
        }
        else if (Constants.TYPE_TREE.equals(or.getType()))
        {
            return new Tree(this, id, or.getInputStream());
        }
        else if (Constants.TYPE_COMMIT.equals(or.getType()))
        {
            return new Commit(this, id, or.getBufferedReader()).getTree();
        }
        else
        {
            or.close();
            throw new IncorrectObjectTypeException(id, Constants.TYPE_TREE);
        }
    }

    public RefLock lockRef(final String ref) throws IOException
    {
        final RefLock l = new RefLock(readRef(ref, true));
        return l.lock() ? l : null;
    }

    public ObjectId resolve(final String revstr) throws IOException
    {
        ObjectId id = null;

        if (ObjectId.isId(revstr))
        {
            id = new ObjectId(revstr);
        }

        if (id == null)
        {
            final Ref r = readRef(revstr, false);
            if (r != null)
            {
                id = r.getObjectId();
            }
        }

        return id;
    }

    public void close() throws IOException
    {
        closePacks();
    }

    public void closePacks() throws IOException
    {
        final Iterator i = packs.iterator();
        while (i.hasNext())
        {
            final PackReader pr = (PackReader) i.next();
            pr.close();
        }
        packs.clear();
    }

    public void scanForPacks()
    {
        final File packDir = new File(objectsDir, "pack");
        final File[] list = packDir.listFiles(new FileFilter()
        {
            public boolean accept(final File f)
            {
                final String n = f.getName();
                if (!n.endsWith(".pack"))
                {
                    return false;
                }
                final String nBase = n.substring(0, n.lastIndexOf('.'));
                final File idx = new File(packDir, nBase + ".idx");
                return f.isFile()
                    && f.canRead()
                    && idx.isFile()
                    && idx.canRead();
            }
        });
        for (int k = 0; k < list.length; k++)
        {
            try
            {
                packs.add(new PackReader(this, list[k]));
            }
            catch (IOException ioe)
            {
                // Whoops. That's not a pack!
                //
            }
        }
    }

    private XInputStream openObjectStream(final ObjectId objectId)
        throws IOException
    {
        try
        {
            return new XInputStream(new FileInputStream(toFile(objectId)));
        }
        catch (FileNotFoundException fnfe)
        {
            return null;
        }
    }

    private ObjectReader objectInPack(final ObjectId objectId)
    {
        final Iterator i = packs.iterator();
        while (i.hasNext())
        {
            final PackReader p = (PackReader) i.next();
            try
            {
                final ObjectReader o = p.get(objectId);
                if (o != null)
                {
                    return o;
                }
            }
            catch (IOException ioe)
            {
                // This shouldn't happen unless the pack was corrupted after we
                // opened it. We'll ignore the error as though the object does
                // not exist in this pack.
                //
            }
        }
        return null;
    }

    private void writeSymref(final String name, final String target)
        throws IOException
    {
        final File s = new File(gitDir, name);
        final File t = File.createTempFile("srf", null, gitDir);
        FileWriter w = new FileWriter(t);
        try
        {
            w.write("ref: ");
            w.write(target);
            w.write('\n');
            w.close();
            w = null;
            if (!t.renameTo(s))
            {
                s.getParentFile().mkdirs();
                if (!t.renameTo(s))
                {
                    t.delete();
                    throw new ObjectWritingException("Unable to"
                        + " write symref "
                        + name
                        + " to point to "
                        + target);
                }
            }
        }
        finally
        {
            if (w != null)
            {
                w.close();
                t.delete();
            }
        }
    }

    private Ref readRef(final String revstr, final boolean missingOk)
        throws IOException
    {
        for (int k = 0; k < refSearchPaths.length; k++)
        {
            final Ref r = readRefBasic(refSearchPaths[k] + revstr);
            if (missingOk || r.getObjectId() != null)
            {
                return r;
            }
        }
        return null;
    }

    private Ref readRefBasic(String name) throws IOException
    {
        int depth = 0;
        REF_READING: do
        {
            final File f = new File(getDirectory(), name);
            if (!f.isFile())
            {
                return new Ref(f, null);
            }

            final BufferedReader br = new BufferedReader(new FileReader(f));
            try
            {
                final String line = br.readLine();
                if (line == null || line.length() == 0)
                {
                    return new Ref(f, null);
                }
                else if (line.startsWith("ref: "))
                {
                    name = line.substring("ref: ".length());
                    continue REF_READING;
                }
                else if (ObjectId.isId(line))
                {
                    return new Ref(f, new ObjectId(line));
                }
                throw new IOException("Not a ref: " + name + ": " + line);
            }
            finally
            {
                br.close();
            }
        }
        while (depth++ < 5);
        throw new IOException("Exceed maximum ref depth.  Circular reference?");
    }

    public String toString()
    {
        return "Repository[" + getDirectory() + "]";
    }
}
