/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearce.jgit.lib_tst;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import junit.framework.TestCase;

import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;

public abstract class RepositoryTestCase extends TestCase
{
    protected static final File trash = new File("trash");

    protected static final File trash_git = new File(trash, ".git");

    protected static final PersonIdent jauthor;

    protected static final PersonIdent jcommitter;

    static
    {
        jauthor = new PersonIdent("J. Author", "jauthor@example.com");
        jcommitter = new PersonIdent("J. Committer", "jcommitter@example.com");
    }

    protected static void recursiveDelete(final File dir)
    {
        final File[] ls = dir.listFiles();
        if (ls != null)
        {
            for (int k = 0; k < ls.length; k++)
            {
                final File e = ls[k];
                if (e.isDirectory())
                {
                    recursiveDelete(e);
                }
                else
                {
                    e.delete();
                }
            }
        }
        dir.delete();
        if (dir.exists())
        {
            throw new IllegalStateException("Failed to delete " + dir);
        }
    }

    protected static void copyFile(final File src, final File dst)
        throws IOException
    {
        final FileInputStream fis = new FileInputStream(src);
        final FileOutputStream fos = new FileOutputStream(dst);
        final byte[] buf = new byte[4096];
        int r;
        while ((r = fis.read(buf)) > 0)
        {
            fos.write(buf, 0, r);
        }
        fis.close();
        fos.close();
    }

    protected static void writeTrashFile(final String name, final String data)
        throws IOException
    {
        final OutputStreamWriter fw = new OutputStreamWriter(
            new FileOutputStream(new File(trash, name)),
            "UTF-8");
        fw.write(data);
        fw.close();
    }

    protected Repository db;

    public void setUp() throws Exception
    {
        super.setUp();
        recursiveDelete(trash);
        db = new Repository(trash_git);
        db.create();

        final String[] packs = {"pack-34be9032ac282b11fa9babdc2b2a93ca996c9c2f"};
        final File tst = new File("tst");
        final File packDir = new File(db.getObjectsDirectory(), "pack");
        for (int k = 0; k < packs.length; k++)
        {
            copyFile(new File(tst, packs[k] + ".pack"), new File(
                packDir,
                packs[k] + ".pack"));
            copyFile(new File(tst, packs[k] + ".idx"), new File(
                packDir,
                packs[k] + ".idx"));
        }

        db.scanForPacks();
    }

    protected void tearDown() throws Exception
    {
        db.close();
        super.tearDown();
    }
}
