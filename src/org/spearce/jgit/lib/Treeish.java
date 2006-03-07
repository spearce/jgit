package org.spearce.jgit.lib;

import java.io.IOException;

public interface Treeish {
    public ObjectId getTreeId();

    public Tree getTree() throws IOException;
}
