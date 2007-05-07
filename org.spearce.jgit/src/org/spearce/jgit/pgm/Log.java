package org.spearce.jgit.pgm;

import java.io.File;
import java.io.IOException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;

public class Log {
	public static void main(String[] args) throws IOException {
		Repository db = new Repository(new File(".git"));
		Commit commit = db.mapCommit(args[0]);
		System.out.println("commit " + commit.getCommitId());
		System.out.println("tree " + commit.getTreeId());
		ObjectId[] ps=commit.getParentIds();
		for (int ci=0; ci<ps.length; ++ci) {
			System.out.println("parent " + ps[ci]);
		}
		System.out.println("author " + commit.getAuthor());
		System.out.println();
		System.out.println(commit.getMessage());
	}
}
