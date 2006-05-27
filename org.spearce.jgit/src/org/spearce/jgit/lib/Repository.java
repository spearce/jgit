package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

public interface Repository {
    public File getDirectory();

    public File getObjectsDirectory();

    public File toFile(final ObjectId id);

    public boolean hasObject(final ObjectId id);

    public ObjectReader openBlob(final ObjectId id) throws IOException;

    public ObjectReader openTree(final ObjectId id) throws IOException;

    public Commit mapCommit(final ObjectId id) throws IOException;

    public Tree mapTree(final ObjectId id) throws IOException;

    public ObjectId resolveRevision(final String revStr) throws IOException;
}