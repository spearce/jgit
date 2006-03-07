package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Commit implements Treeish {
    private final ObjectDatabase objdb;

    private final ObjectId commitId;

    private final ObjectId treeId;

    private final List parentIds;

    private final String author;

    private final String committer;

    private final String message;

    private Tree treeObj;

    public Commit(final ObjectDatabase db, final ObjectId id,
            final BufferedReader br) throws IOException {
        objdb = db;
        commitId = id;

        final ArrayList tempParents;
        final StringBuffer tempMessage;
        final char[] readBuf;
        int readLen;
        String line;

        line = br.readLine();
        if (line == null || !line.startsWith("tree ")) {
            throw new CorruptObjectException("No tree found in commit " + id);
        }
        treeId = new ObjectId(line.substring("tree ".length()));

        tempParents = new ArrayList(2);
        for (;;) {
            line = br.readLine();
            if (line == null) {
                throw new CorruptObjectException("Commit header corrupt " + id);
            }
            if (line.startsWith("parent ")) {
                tempParents
                        .add(new ObjectId(line.substring("parent ".length())));
            } else {
                break;
            }
        }
        parentIds = Collections.unmodifiableList(tempParents);

        if (line == null || !line.startsWith("author ")) {
            throw new CorruptObjectException("No author found in commit " + id);
        }
        author = line.substring("author ".length());

        line = br.readLine();
        if (line == null || !line.startsWith("committer ")) {
            throw new CorruptObjectException("No committer found in commit "
                    + id);
        }
        committer = line.substring("committer ".length());

        line = br.readLine();
        if (line == null || !line.equals("")) {
            throw new CorruptObjectException(
                    "No blank line after header in commit " + id);
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

    public List getTreeEntries() throws IOException {
        if (treeObj == null) {
            treeObj = objdb.openTree(getTreeId());
        }
        return treeObj.getTreeEntries();
    }

    public String getAuthor() {
        return author;
    }

    public String getCommitter() {
        return committer;
    }

    public List getParentIds() {
        return parentIds;
    }

    public String getMessage() {
        return message;
    }
}
