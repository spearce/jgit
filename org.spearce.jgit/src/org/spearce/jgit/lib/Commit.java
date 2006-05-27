package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commit implements Treeish {
    private final Repository objdb;

    private final ObjectId commitId;

    private final ObjectId treeId;

    private final List parentIds;

    private final PersonIdent author;

    private final PersonIdent committer;

    private final String message;

    private Tree treeObj;

    public Commit(final Repository db, final ObjectId id,
            final BufferedReader br) throws IOException {
        objdb = db;
        commitId = id;

        final ArrayList tempParents;
        final StringBuffer tempMessage;
        final char[] readBuf;
        int readLen;
        String n;

        n = br.readLine();
        if (n == null || !n.startsWith("tree ")) {
            throw new CorruptObjectException(commitId, "no tree");
        }
        treeId = new ObjectId(n.substring("tree ".length()));

        tempParents = new ArrayList(2);
        for (;;) {
            n = br.readLine();
            if (n == null) {
                throw new CorruptObjectException(commitId, "early eof");
            }
            if (n.startsWith("parent ")) {
                tempParents.add(new ObjectId(n.substring("parent ".length())));
            } else {
                break;
            }
        }
        parentIds = Collections.unmodifiableList(tempParents);

        if (n == null || !n.startsWith("author ")) {
            throw new CorruptObjectException(commitId, "no author");
        }
        author = new PersonIdent(n.substring("author ".length()));

        n = br.readLine();
        if (n == null || !n.startsWith("committer ")) {
            throw new CorruptObjectException(commitId, "no committer");
        }
        committer = new PersonIdent(n.substring("committer ".length()));

        n = br.readLine();
        if (n == null || !n.equals("")) {
            throw new CorruptObjectException(commitId, "malformed header");
        }

        tempMessage = new StringBuffer();
        readBuf = new char[128];
        while ((readLen = br.read(readBuf)) > 0) {
            tempMessage.append(readBuf, 0, readLen);
        }
        message = tempMessage.toString();
    }

    public ObjectId getCommitId() {
        return commitId;
    }

    public ObjectId getTreeId() {
        return treeId;
    }

    public Tree getTree() throws IOException {
        if (treeObj == null) {
            treeObj = objdb.mapTree(getTreeId());
            if (treeObj == null) {
                throw new MissingObjectException("tree", getTreeId());
            }
        }
        return treeObj;
    }

    public PersonIdent getAuthor() {
        return author;
    }

    public PersonIdent getCommitter() {
        return committer;
    }

    public List getParentIds() {
        return parentIds;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "Commit[" + getCommitId() + " " + getAuthor() + "]";
    }
}
