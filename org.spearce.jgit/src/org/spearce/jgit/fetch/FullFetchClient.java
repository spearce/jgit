package org.spearce.jgit.fetch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.spearce.jgit.fetch.FetchClient;
import org.spearce.jgit.fetch.IndexPack;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.TextProgressMonitor;

/**
 * This is does-it-all fetch implementation.
 */
public class FullFetchClient extends FetchClient {

	private PipedInputStream pi = new PipedInputStream();
	Throwable[] threadError = new Throwable[1];
	Thread fetchThread;
	private String remote;

	Thread indexThread = new Thread() {
		@Override
		public void run() {
			IndexPack pack;
			try {
				pack = new IndexPack(pi, new File("tmp_pack1"));
				pack.index(new TextProgressMonitor());
				pack.renamePack(repository);
			} catch (Throwable e) {
				e.printStackTrace();
				fetchThread.interrupt();
				threadError[0] = e;
			}
		}
	};

	public void run() throws IOException {
		try {
			fetchThread = Thread.currentThread();
			indexThread.start();
			super.run();
			os.close();
			indexThread.join();
			repository.scanForPacks();
			updateRemoteRefs(remote);
		} catch (InterruptedException e) {
			if (threadError[0] != null)
				if (threadError[0] instanceof IOException)
					throw (IOException)threadError[0];
				throw new Error(threadError[0]);
		} finally {
			if (indexThread.isAlive()) {
				indexThread.interrupt();
				try {
					indexThread.join();
				} catch (InterruptedException e) {
					// nothing here
				}
			}
		}
	}

	/**
	 * Construct a {@link FullFetchClient}
	 *
	 * @param repository
	 * @param remoteName
	 * @param initialCommand
	 * @param toServer
	 * @param fromServer
	 * @throws IOException
	 */
	public FullFetchClient(Repository repository, String remoteName, String initialCommand,
			OutputStream toServer, InputStream fromServer) throws IOException {
		super(repository, initialCommand, toServer, fromServer, null);
		remote = remoteName;
		os = new PipedOutputStream(pi);
	}
}
