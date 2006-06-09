package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

public interface Repository
{
    public File getDirectory();

    public String getSubsetPath();

    public File getObjectsDirectory();

    public RepositoryConfig getConfig();

    public Repository subset(final String path);

    public File toFile(final ObjectId id);

    public boolean hasObject(final ObjectId id);

    public ObjectReader openObject(final ObjectId id) throws IOException;

    public ObjectReader openBlob(final ObjectId id) throws IOException;

    public ObjectReader openTree(final ObjectId id) throws IOException;

    public Commit mapCommit(final ObjectId id) throws IOException;

    public Tree mapTree(final ObjectId id) throws IOException;

    public ObjectId resolve(final String revStr) throws IOException;
}