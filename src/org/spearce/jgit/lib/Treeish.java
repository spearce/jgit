package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.List;

public interface Treeish {
    public ObjectId getTreeId();

    public List getTreeEntries() throws IOException;
}
