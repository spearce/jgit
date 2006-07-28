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
package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.MissingObjectException;

public class Commit implements Treeish
{
    private final Repository objdb;

    private ObjectId commitId;

    private ObjectId treeId;

    private final List parentIds;

    private PersonIdent author;

    private PersonIdent committer;

    private String message;

    private Tree treeObj;

    public Commit(final Repository db)
    {
        objdb = db;
        parentIds = new ArrayList(2);
    }

    public Commit(
        final Repository db,
        final ObjectId id,
        final BufferedReader br) throws IOException
    {
        objdb = db;
        commitId = id;

        final StringBuffer tempMessage;
        final char[] readBuf;
        int readLen;
        String n;

        n = br.readLine();
        if (n == null || !n.startsWith("tree "))
        {
            throw new CorruptObjectException(commitId, "no tree");
        }
        treeId = new ObjectId(n.substring("tree ".length()));

        parentIds = new ArrayList(2);
        for (;;)
        {
            n = br.readLine();
            if (n == null)
            {
                throw new CorruptObjectException(commitId, "no parent(s)");
            }
            if (n.startsWith("parent "))
            {
                parentIds.add(new ObjectId(n.substring("parent ".length())));
            }
            else
            {
                break;
            }
        }

        if (n == null || !n.startsWith("author "))
        {
            throw new CorruptObjectException(commitId, "no author");
        }
        author = new PersonIdent(n.substring("author ".length()));

        n = br.readLine();
        if (n == null || !n.startsWith("committer "))
        {
            throw new CorruptObjectException(commitId, "no committer");
        }
        committer = new PersonIdent(n.substring("committer ".length()));

        n = br.readLine();
        if (n == null || !n.equals(""))
        {
            throw new CorruptObjectException(commitId, "malformed header");
        }

        tempMessage = new StringBuffer();
        readBuf = new char[128];
        while ((readLen = br.read(readBuf)) > 0)
        {
            tempMessage.append(readBuf, 0, readLen);
        }
        message = tempMessage.toString();
    }

    public ObjectId getCommitId()
    {
        return commitId;
    }

    public void setCommitId(final ObjectId id)
    {
        commitId = id;
    }

    public ObjectId getTreeId()
    {
        return treeId;
    }

    public void setTreeId(final ObjectId id)
    {
        if (!treeId.equals(id))
        {
            treeObj = null;
        }
        treeId = id;
    }

    public Tree getTree() throws IOException
    {
        if (treeObj == null)
        {
            treeObj = objdb.mapTree(getTreeId());
            if (treeObj == null)
            {
                throw new MissingObjectException(
                    getTreeId(),
                    Constants.TYPE_TREE);
            }
        }
        return treeObj;
    }

    public void setTree(final Tree t)
    {
        treeId = null;
        treeObj = t;
    }

    public PersonIdent getAuthor()
    {
        return author;
    }

    public void setAuthor(final PersonIdent a)
    {
        author = a;
    }

    public PersonIdent getCommitter()
    {
        return committer;
    }

    public void setCommitter(final PersonIdent c)
    {
        committer = c;
    }

    public List getParentIds()
    {
        return parentIds;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String m)
    {
        message = m;
    }

    public String toString()
    {
        return "Commit[" + getCommitId() + " " + getAuthor() + "]";
    }
}
