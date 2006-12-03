    package org.spearce.jgit.pgm;

    import java.io.File;
    import java.io.IOException;
import java.util.Iterator;

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Repository;

    public class Log {
        public static void main(String[] args) throws IOException {
            Repository db = new Repository(new File(".git"));
            Commit commit = db.mapCommit(args[0]);
            System.out.println("commit "+commit.getCommitId());
            System.out.println("tree "+commit.getTreeId());
            for (Iterator ci=commit.getParentIds().iterator(); ci.hasNext(); ) {
                System.out.println("parent "+ci.next());
            }
            System.out.println("author "+commit.getAuthor());
            System.out.println();
            System.out.println(commit.getMessage());
        }
    }
