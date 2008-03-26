/*
 *  Copyright (C) 2007,2008  Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License, version 2, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.jgit.fetch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectIdMap;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.RefLock;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.lib.Tree;

/**
 * This class implements the Git fetch protocol.
 * <p>
 * The class expects that a communications channel to a serverr has been opened.
 * This can be a socket to a Git server, an SSH connection or a pipe to a
 * locally running git-upload-pack process.
 */
public class FetchClient {
	private final BufferedOutputStream toServer;
	private final BufferedInputStream fromServer;
	final Repository repository;
	private final String initialCommand;
	protected OutputStream os;

	/**
	 * Construct a FetchClient working on two streams connected to a server
	 *
	 * @param repository Git repository
	 * @param initialCommand A command to send to the server after the connection is opened, or null
	 * @param toServer stream for sending to server
	 * @param fromServer stream for receiving from server
	 * @param os stream to send pack data to
	 */
	public FetchClient(final Repository repository,
			final String initialCommand, final OutputStream toServer,
			final InputStream fromServer, final OutputStream os) {
		this.repository = repository;
		this.initialCommand = initialCommand;
		this.os = os;
		if (toServer instanceof BufferedOutputStream)
			this.toServer = (BufferedOutputStream) toServer;
		else
			this.toServer = new BufferedOutputStream(toServer);
		if (fromServer instanceof BufferedInputStream)
			this.fromServer = (BufferedInputStream) fromServer;
		else
			this.fromServer = new BufferedInputStream(fromServer);
	}

	private Map<String,ObjectId> serverHas = new HashMap<String,ObjectId>();
	private Set<ObjectId> have = new HashSet<ObjectId>();
	private String options;

	private boolean readHasLine() throws IOException {
		final byte[] lenbuf = new byte[4];
		if (readFully(fromServer,lenbuf) != lenbuf.length)
			throw new IOException("Expected has-ref-length");
		final int len = Integer.parseInt(new String(lenbuf,0),16);
		if (len == 0)
			return false;
		final byte[] objectIdBuf = new byte[Constants.OBJECT_ID_LENGTH * 2];
		if (readFully(fromServer,objectIdBuf) != objectIdBuf.length)
			throw new IOException("Expected SHA-1 of remote ref");
		final ObjectId id = ObjectId.fromString(objectIdBuf,0);

		int c = readFully(fromServer);
		if (c != ' ')
			throw new IOException("Expected blank");
		final byte[] refnameBuf = new byte[len - 4 - Constants.OBJECT_ID_LENGTH*2 - 2];
		if (readFully(fromServer,refnameBuf) != refnameBuf.length)
			throw new IOException("Expected refname");
		c = readFully(fromServer);
		if (c != '\n')
			throw new IOException("Expected newline");
		int z;
		for (z=0; z<refnameBuf.length && refnameBuf[z] != '\0';)
			++z;
		final String refName = new String(refnameBuf,0,z);
		if (z < refnameBuf.length) {
			if (options == null)
				options = new String(refnameBuf,z);
			else
				throw new IOException("Invalid state, already received options from server");
		}
		if (shouldReceiveRef(refName))
			serverHas.put(refName, id);

		return true;
	}

	private boolean shouldReceiveRef(final String refName) {
		if (!Repository.isValidRefName(refName))
			return false;
		if (refName.startsWith("refs/heads/"))
			return true;
		if (refName.startsWith("refs/tags/"))
			return true;
		return false;
	}

	private void pruneWhatWeHave() throws IOException {
		for (final Iterator<Map.Entry<String,ObjectId>> i = serverHas.entrySet().iterator(); i.hasNext(); ) {
			final Map.Entry<String, ObjectId> e = i.next();
			if (repository.hasObject(e.getValue())) {
				String key = e.getKey();
				if (key.endsWith("^{}"))
					key = key.substring(0, key.length()-3);
				final ObjectId id = repository.resolve(key);
				if (id != null && !id.equals(e.getValue())) {
					i.remove();
				}
			}
		}
	}

	private static final String defaultflags = " multi_ack side-band-64k ofs-delta";

	private String flags = defaultflags;
	private String[] fetchList;
	private ProgressMonitor monitor;
	private int scale;

	/**
	 * Request data.
	 *
	 * No thin packs so far, but we request deltas.
	 *
	 * @throws IOException
	 */
	private void request() throws IOException {
		final List<ObjectId> todo = new ArrayList<ObjectId>();
		for (final Map.Entry<String,ObjectId> e : serverHas.entrySet()) {
			if (e.getKey().endsWith("^{}"))
				continue;
			boolean match = false;
			if (fetchList == null) {
				match = true;
			} else {
				for (final String f : fetchList) {
					if (e.getKey().matches(f)) {
						match = true;
					}
				}
			}
			if (!match)
				continue;
			System.out.println("Requesting "+e);
			final ObjectId id = repository.resolve(e.getKey());
			if (id != null) {
				Object object = repository.mapObject(id, e.getKey());
				if (object instanceof Commit) {
					writeServer(("want " + e.getValue() + flags+ "\n"));
					flags = "";
					Commit commit = (Commit)object;
					todo.add(commit.getCommitId());
					have.add(commit.getCommitId());
				} else {
					System.out.println("cannot negotiate (yet) "+e.getKey());
				}
			} else {
				writeServer(("want " + e.getValue() + flags + "\n"));
				flags = "";
			}
		}
		toServer.write("0000".getBytes());
		toServer.flush();
		flags = defaultflags;
		final Collection<String> allRefs = repository.getAllRefs();
		for (final String ref : allRefs) {
			final ObjectId id = repository.resolve(ref);
			if (!have.contains(id)) {
				todo.add(id);
				have.add(id);
			}
		}
		monitor.beginTask("Negotiating commits", 1000000);

		final ObjectIdMap<Boolean> reported = new ObjectIdMap<Boolean>();

		for (Iterator<ObjectId> nextCommit = todo.iterator(); nextCommit.hasNext(); nextCommit = todo.iterator()) {

			monitor.worked(1);
			if (monitor.isCancelled())
				break;

			ObjectId id = nextCommit.next();
			nextCommit.remove();
			if (reported.containsKey(id))
				continue;
			reported.put(id, Boolean.TRUE);
			do {
				final Object object = repository.mapObject(id, "noref");
				if (!(object instanceof Commit))
					break;
				final Commit commit = repository.mapCommit(id);
				System.out.println("Have "+commit.getCommitId());
				toServer.write(("0032have "+commit.getCommitId().toString()+"\n").getBytes());
				final ObjectId[] parentIds = commit.getParentIds();
				for (int i=1; i<parentIds.length; ++i) {
					if (!have.contains(parentIds[i]))
						todo.add(parentIds[i]);
				}
				if (parentIds.length > 0)
					id = parentIds[0];
				else
					id = null;
				while (fromServer.available() >= 8)
					readWantResponse();
			} while (id != null);
			toServer.flush();
		}
		toServer.write("0009done\n".getBytes());
		toServer.flush();
		while (readWantResponse()) {
			// go on until false
		}

		final byte[] lenbuf = new byte[4];
		for(;;) {
			final int nread = readFully(fromServer,lenbuf);
			if (nread < lenbuf.length)
				throw new IOException("Unexpected end of data stream");
			final int len = Integer.parseInt(new String(lenbuf,0),16);
			if (len == 0)
				break;
			final byte[] data = new byte[len - 4];
			if (readFully(fromServer,data) != data.length)
				throw new IOException("Expected data");
			if (data[0] == '\1') {
				os.write(data,1,data.length-1);
			} else {
				System.out.println("Progress "+new String(data,1,data.length-1));
				String msg = new String(data, 1, data.length - 1);
				Matcher matcher = progress.matcher(msg);
				if (matcher.matches()) {
					String group = matcher.group(2);
					if (group != null) {
						int amount = Integer.parseInt(group);
						int stage = 0;
						if (msg.startsWith("Compressing "))
							stage = 1;
						monitor.worked(stage * scale + scale*amount / 100 - monitor.getWorked());
						monitor.setTask(matcher.group(1));
					}
				} else {
					Matcher cmatcher = counting.matcher(msg);
					if (cmatcher.matches()) {
						monitor.worked(1000);
						String scales = cmatcher.group(1);
						scale = Integer.parseInt(scales);
						monitor.setTotalWork(scale * 4);
						monitor.worked(scale - monitor.getWorked());
					}
				}
			}
			os.flush();
		}
		os.flush();
	}

	static Pattern counting = Pattern.compile(".*Counting objects: (\\d+)(, done)*\\..*", Pattern.DOTALL);
	static Pattern progress = Pattern.compile(".*?([\\w ]+): +(\\d+)%.*", Pattern.DOTALL);

	private boolean readWantResponse() throws IOException {
		final byte[] lenbuf = new byte[4];
		if (readFully(fromServer,lenbuf) != lenbuf.length)
			throw new IOException("Expected ack-nak-length");
		final int len = Integer.parseInt(new String(lenbuf,0),16);
		final byte [] type = new byte[4];
		if (readFully(fromServer,type) != lenbuf.length)
			throw new IOException("Expected ACK/NAK/done");
		final String acktype = new String(type);
		if (!acktype.equals("ACK ") && !acktype.equals("NAK\n"))
			throw new IOException("Expected ACK/NAK");
		if (acktype.equals("NAK\n")) {
			if (len != 8)
				throw new IOException("Unexpected NAK length");
			return false; // done here
		}
		final byte[] objectIdBuf = new byte[Constants.OBJECT_ID_LENGTH * 2];
		if (readFully(fromServer,objectIdBuf) != objectIdBuf.length)
			throw new IOException("Expected ack/nak SHA-1");

		final ObjectId id = ObjectId.fromString(objectIdBuf,0);
//		System.out.println("Got "+acktype+" "+id);
		if (acktype.equals("ACK"))
			have.add(id); // record that we know this one (and all older ones of course)
		int c = readFully(fromServer);
		if (c == '\n') {
			System.out.println("Server says done");
			return false;
		}
		if (c != ' ')
			throw new IOException("Expected blank");

		final byte[] cont = new byte[9];
		if (readFully(fromServer,cont) != cont.length)
			throw new IOException("Expected contine");
		if (!new String(cont).equals("continue\n"))
			throw new IOException("Expected contine");

		return true;
	}

	private int readFully(final BufferedInputStream in) throws IOException {
		while (in.available() == 0)
			;
		return in.read();
	}

	private static int readFully(final BufferedInputStream in, final byte[] b) throws IOException {
		int off=0;
		int nread = 0;
		while (off < b.length && (nread=in.read(b, off, b.length - off)) >= 0) {
			off += nread;
		}
		return off;
	}

	private void writeServer(final String data) throws IOException {
		String string = ("000"+Integer.toString(data.length()+4,16));
		string = string.substring(string.length()-4);
		toServer.write(string.getBytes());
		toServer.write(data.getBytes());
	}

	private void whatwehave() throws IOException {
		final Map<ObjectId, Boolean> weHave = new ObjectIdMap<Boolean>();
		final Collection<String> allRefs = repository.getAllRefs();
		final List<ObjectId> toList = new ArrayList<ObjectId>(20000);
		for (final String ref : allRefs) {
			final ObjectId id = repository.resolve(ref);
			if (!weHave.containsKey(id))
				toList.add(id);
		}

		for (Iterator<ObjectId> i = toList.iterator(); i.hasNext(); i = toList.iterator()) {
			ObjectId id = i.next();
			for (;;) {
				if (!weHave.containsKey(id)) {
					final Object object = repository.mapObject(id, null);
					if (object instanceof Commit) {
						final Commit commit = (Commit)object;
						final ObjectId[] parentIds = commit.getParentIds();
						for (int j = 1; j < parentIds.length; ++j)
							toList.add(parentIds[j]);
						if (parentIds.length > 0)
							id = parentIds[0];
						else
							break;
					}
					if (object instanceof Tree)
						break;
					if (object instanceof Tag)
						break;
				} else
					break;
			}
			toList.remove(0);
		}
	}

	/**
	 * Execute the fetch process.
	 *
	 * @param aMonitor progress reporting interface
	 *
	 * @throws IOException
	 */
	public void run(ProgressMonitor aMonitor) throws IOException {
		monitor = aMonitor;
		monitor.setTask("Negotiating with server");
		monitor.worked(5);
		if (initialCommand != null) {
			writeServer(initialCommand);
			toServer.flush();
		}
		while (readHasLine())
			;

		monitor.worked(10);

		pruneWhatWeHave();

		request();
		toServer.close();
		if (false)
			whatwehave();

	}

	/**
	 * Set list of branches to fetch. Default is to fetch all
	 * branches.
	 *
	 * @param fetchList
	 */
	public void setBranches(String[] fetchList) {
		this.fetchList = fetchList;
	}

	/**
	 * Save the received references into the local repository.
	 * <p>
	 * Branches are stored under <quote>remotes</quote> name space, while tags
	 * get stored (and overwritten) into the tags name space, i.e. not connected
	 * to a particular remote.
	 *
	 * @param remote
	 * @throws IOException
	 */
	void updateRemoteRefs(String remote) throws IOException {
		for(String ref : serverHas.keySet()) {
			ObjectId id = serverHas.get(ref);
			if (!repository.hasObject(id))
				throw new IllegalStateException("We should have received " + id
						+ " in order to set up remote ref " + ref);
			String remotePrefix = "refs/remotes/"+remote+"/";
			String lref;
			if (ref.startsWith("refs/heads/"))
				lref = remotePrefix + ref.substring("refs/heads/".length());
			else if (ref.equals("HEAD"))
				lref = remotePrefix + "HEAD";
			else if (ref.startsWith("refs/tags/"))
				lref = ref;
			else
				throw new IllegalStateException("Bad ref name from remote "+ref);
			RefLock lockRef = repository.lockRef(lref);
			lockRef.write(id);
			lockRef.commit();
		}
	}
}
