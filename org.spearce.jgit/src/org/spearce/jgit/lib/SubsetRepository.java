package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

public class SubsetRepository implements Repository {
    private final Repository parent;

    private final String subset;

    public SubsetRepository(final Repository p, final String s) {
        parent = p;
        subset = s;
    }

    public File getDirectory() {
        return parent.getDirectory();
    }

    public File getObjectsDirectory() {
        return parent.getObjectsDirectory();
    }

    public File toFile(final ObjectId id) {
        return parent.toFile(id);
    }

    public boolean hasObject(final ObjectId id) {
        return parent.hasObject(id);
    }

    public ObjectReader openBlob(final ObjectId id) throws IOException {
        return parent.openBlob(id);
    }

    public ObjectReader openTree(final ObjectId id) throws IOException {
        return parent.openTree(id);
    }

    public Commit mapCommit(final ObjectId id) throws IOException {
        return parent.mapCommit(id);
    }

    public Tree mapTree(final ObjectId id) throws IOException {
        return parent.mapTree(id);
    }

    public ObjectId resolveRevision(final String r) throws IOException {
        return parent.resolveRevision(r);
    }

    public String toString() {
        return "SubsetRepository[" + getDirectory() + ", " + subset + "]";
    }
}
