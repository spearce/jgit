package org.spearce.jgit.transport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Repository;

/**
 * This is does-it-all fetch implementation.
 */
public class FullFetchClient extends FetchClient {

	private PipedInputStream pi = new PipedInputStream();
	Throwable[] threadError = new Throwable[1];
	Thread fetchThread;
	private String remote;

	Thread indexThread;

	public void run(final ProgressMonitor monitor) throws IOException {
		try {
			indexThread = new Thread() {
				@Override
				public void run() {
					IndexPack pack;
					try {
						pack = new IndexPack(repository, pi, new File(new File(repository.getObjectsDirectory(), "pack"), "packtmp_pack"+System.currentTimeMillis()));
						pack.index(monitor);
						pack.renamePack();
					} catch (Throwable e) {
						e.printStackTrace();
						fetchThread.interrupt();
						threadError[0] = e;
					}
				}
			};
			fetchThread = Thread.currentThread();
			indexThread.start();
			super.run(monitor);
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
