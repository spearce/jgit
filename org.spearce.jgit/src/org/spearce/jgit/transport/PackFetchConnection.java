/*
 *  Copyright (C) 2007,2008  Robin Rosenberg
 *  Copyright (C) 2008  Shawn Pearce <spearce@spearce.org>
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
package org.spearce.jgit.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.MutableObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevCommitList;
import org.spearce.jgit.revwalk.RevFlag;
import org.spearce.jgit.revwalk.RevObject;
import org.spearce.jgit.revwalk.RevSort;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.revwalk.filter.CommitTimeRevFilter;
import org.spearce.jgit.revwalk.filter.RevFilter;

/**
 * Fetch implementation using the native Git pack transfer service.
 * <p>
 * This is the canonical implementation for transferring objects from the remote
 * repository to the local repository by talking to the 'git-upload-pack'
 * service. Objects are packed on the remote side into a pack file and then sent
 * down the pipe to us.
 * <p>
 * This connection requires only a bi-directional pipe or socket, and thus is
 * easily wrapped up into a local process pipe, anonymous TCP socket, or a
 * command executed through an SSH tunnel.
 */
abstract class PackFetchConnection extends FetchConnection {
	/**
	 * Maximum number of 'have' lines to send before giving up.
	 * <p>
	 * During {@link #negotiate(ProgressMonitor)} we send at most this many
	 * commits to the remote peer as 'have' lines without an ACK response before
	 * we give up.
	 */
	private static final int MAX_HAVES = 256;

	static final String OPTION_INCLUDE_TAG = "include-tag";

	static final String OPTION_MULTI_ACK = "multi_ack";

	static final String OPTION_THIN_PACK = "thin-pack";

	static final String OPTION_SIDE_BAND = "side-band";

	static final String OPTION_SIDE_BAND_64K = "side-band-64k";

	static final String OPTION_OFS_DELTA = "ofs-delta";

	static final String OPTION_SHALLOW = "shallow";

	/** The repository this transport fetches into, or pushes out of. */
	protected final Repository local;

	/** Remote repository location. */
	protected final URIish uri;

	/** Capability tokens advertised by the remote side. */
	protected final Set<String> remoteCapablities = new HashSet<String>();

	/** Buffered input stream reading from the remote. */
	protected InputStream in;

	/** Buffered output stream sending to the remote. */
	protected OutputStream out;

	/** Packet line decoder around {@link #in}. */
	protected PacketLineIn pckIn;

	/** Packet line encoder around {@link #out}. */
	protected PacketLineOut pckOut;

	private final RevWalk walk;

	/** All commits that are immediately reachable by a local ref. */
	private RevCommitList<RevCommit> reachableCommits;

	/** Marks an object as having all its dependencies. */
	final RevFlag REACHABLE;

	/** Marks a commit known to both sides of the connection. */
	final RevFlag COMMON;

	/** Marks a commit listed in the advertised refs. */
	final RevFlag ADVERTISED;

	private boolean multiAck;

	private boolean thinPack;

	private boolean sideband;

	private boolean includeTags;

	PackFetchConnection(final PackTransport packTransport) {
		local = packTransport.local;
		uri = packTransport.uri;
		includeTags = packTransport.getTagOpt() != TagOpt.NO_TAGS;

		walk = new RevWalk(local);
		reachableCommits = new RevCommitList<RevCommit>();
		REACHABLE = walk.newFlag("REACHABLE");
		COMMON = walk.newFlag("COMMON");
		ADVERTISED = walk.newFlag("ADVERTISED");

		walk.carry(COMMON);
		walk.carry(REACHABLE);
		walk.carry(ADVERTISED);
	}

	protected void init(final InputStream myIn, final OutputStream myOut) {
		in = myIn instanceof BufferedInputStream ? myIn
				: new BufferedInputStream(myIn);
		out = myOut instanceof BufferedOutputStream ? myOut
				: new BufferedOutputStream(myOut);

		pckIn = new PacketLineIn(in);
		pckOut = new PacketLineOut(out);
	}

	@Override
	public boolean didFetchIncludeTags() {
		return includeTags;
	}

	protected void readAdvertisedRefs() throws TransportException {
		try {
			readAdvertisedRefsImpl();
		} catch (TransportException err) {
			close();
			throw err;
		} catch (IOException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		} catch (RuntimeException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		}
	}

	private void readAdvertisedRefsImpl() throws IOException {
		final LinkedHashMap<String, Ref> avail = new LinkedHashMap<String, Ref>();
		for (;;) {
			String line;

			try {
				line = pckIn.readString();
			} catch (EOFException eof) {
				if (avail.isEmpty())
					throw new TransportException(uri + " not found.");
				throw eof;
			}

			if (avail.isEmpty()) {
				// The first line (if any) may contain "hidden"
				// capability values after a NUL byte.
				//
				final int nul = line.indexOf('\0');
				if (nul >= 0) {
					for (String c : line.substring(nul + 1).split(" "))
						remoteCapablities.add(c);
					line = line.substring(0, nul);
				}
			}

			if (line.length() == 0)
				break;

			String name = line.substring(41, line.length());
			final ObjectId id = ObjectId.fromString(line.substring(0, 40));
			if (name.endsWith("^{}")) {
				name = name.substring(0, name.length() - 3);
				final Ref prior = avail.get(name);
				if (prior == null)
					throw new PackProtocolException("advertisement of " + name
							+ "^{} came before " + name);

				if (prior.getPeeledObjectId() != null)
					throw duplicateAdvertisement(name + "^{}");

				avail.put(name, new Ref(name, prior.getObjectId(), id));
			} else {
				final Ref prior = avail.put(name, new Ref(name, id));
				if (prior != null)
					throw duplicateAdvertisement(name);
			}
		}
		available(avail);
	}

	private PackProtocolException duplicateAdvertisement(final String name) {
		return new PackProtocolException("duplicate advertisements of " + name);
	}

	@Override
	protected void doFetch(final ProgressMonitor monitor,
			final Collection<Ref> want) throws TransportException {
		try {
			markRefsAdvertised();
			markReachable(maxTimeWanted(want));

			if (sendWants(want)) {
				negotiate(monitor);

				walk.dispose();
				reachableCommits = null;

				receivePack(monitor);
			}
		} catch (CancelledException ce) {
			close();
			return; // Caller should test (or just know) this themselves.
		} catch (IOException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		} catch (RuntimeException err) {
			close();
			throw new TransportException(err.getMessage(), err);
		}
	}

	private int maxTimeWanted(final Collection<Ref> wants) {
		int maxTime = 0;
		for (final Ref r : wants) {
			try {
				final RevObject obj = walk.parseAny(r.getObjectId());
				if (obj instanceof RevCommit) {
					final int cTime = ((RevCommit) obj).getCommitTime();
					if (maxTime < cTime)
						maxTime = cTime;
				}
			} catch (IOException error) {
				// We don't have it, but we want to fetch (thus fixing error).
			}
		}
		return maxTime;
	}

	private void markReachable(final int maxTime) throws IOException {
		for (final Ref r : local.getAllRefs().values()) {
			try {
				final RevCommit o = walk.parseCommit(r.getObjectId());
				o.add(REACHABLE);
				reachableCommits.add(o);
			} catch (IOException readError) {
				// If we cannot read the value of the ref skip it.
			} catch (ClassCastException cce) {
				// Not a commit type.
			}
		}

		if (maxTime > 0) {
			// Mark reachable commits until we reach maxTime. These may
			// wind up later matching up against things we want and we
			// can avoid asking for something we already happen to have.
			//
			final Date maxWhen = new Date(maxTime * 1000L);
			walk.sort(RevSort.COMMIT_TIME_DESC);
			walk.markStart(reachableCommits);
			walk.setRevFilter(CommitTimeRevFilter.after(maxWhen));
			for (;;) {
				final RevCommit c = walk.next();
				if (c == null)
					break;
				if (c.has(ADVERTISED) && !c.has(COMMON)) {
					// This is actually going to be a common commit, but
					// our peer doesn't know that fact yet.
					//
					c.add(COMMON);
					c.carry(COMMON);
					reachableCommits.add(c);
				}
			}
		}
	}

	private boolean sendWants(final Collection<Ref> want) throws IOException {
		boolean first = true;
		for (final Ref r : want) {
			try {
				if (walk.parseAny(r.getObjectId()).has(REACHABLE)) {
					// We already have this object. Asking for it is
					// not a very good idea.
					//
					continue;
				}
			} catch (IOException err) {
				// Its OK, we don't have it, but we want to fix that
				// by fetching the object from the other side.
			}

			final StringBuilder line = new StringBuilder(46);
			line.append("want ");
			line.append(r.getObjectId());
			if (first) {
				line.append(enableCapabilities());
				first = false;
			}
			line.append('\n');
			pckOut.writeString(line.toString());
		}
		pckOut.end();
		return !first;
	}

	private String enableCapabilities() {
		final StringBuilder line = new StringBuilder();
		if (includeTags)
			includeTags = wantCapability(line, OPTION_INCLUDE_TAG);
		wantCapability(line, OPTION_OFS_DELTA);
		multiAck = wantCapability(line, OPTION_MULTI_ACK);
		thinPack = wantCapability(line, OPTION_THIN_PACK);
		if (wantCapability(line, OPTION_SIDE_BAND_64K))
			sideband = true;
		else if (wantCapability(line, OPTION_SIDE_BAND))
			sideband = true;
		return line.toString();
	}

	private boolean wantCapability(final StringBuilder b, final String option) {
		if (!remoteCapablities.contains(option))
			return false;
		if (b.length() > 0)
			b.append(' ');
		b.append(option);
		return true;
	}

	private void negotiate(final ProgressMonitor monitor) throws IOException,
			CancelledException {
		final MutableObjectId ackId = new MutableObjectId();
		int resultsPending = 0;
		int havesSent = 0;
		int havesSinceLastContinue = 0;
		boolean receivedContinue = false;
		boolean receivedAck = false;
		boolean sendHaves = true;

		negotiateBegin();
		while (sendHaves) {
			final RevCommit c = walk.next();
			if (c == null)
				break;

			pckOut.writeString("have " + c.getId() + "\n");
			havesSent++;
			havesSinceLastContinue++;

			if ((31 & havesSent) != 0) {
				// We group the have lines into blocks of 32, each marked
				// with a flush (aka end). This one is within a block so
				// continue with another have line.
				//
				continue;
			}

			if (monitor.isCancelled())
				throw new CancelledException();

			pckOut.end();
			resultsPending++; // Each end will cause a result to come back.

			if (havesSent == 32) {
				// On the first block we race ahead and try to send
				// more of the second block while waiting for the
				// remote to respond to our first block request.
				// This keeps us one block ahead of the peer.
				//
				continue;
			}

			while (resultsPending > 0) {
				final PacketLineIn.AckNackResult anr;

				anr = pckIn.readACK(ackId);
				resultsPending--;
				if (anr == PacketLineIn.AckNackResult.NAK) {
					// More have lines are necessary to compute the
					// pack on the remote side. Keep doing that.
					//
					break;
				}

				if (anr == PacketLineIn.AckNackResult.ACK) {
					// The remote side is happy and knows exactly what
					// to send us. There is no further negotiation and
					// we can break out immediately.
					//
					multiAck = false;
					resultsPending = 0;
					receivedAck = true;
					sendHaves = false;
					break;
				}

				if (anr == PacketLineIn.AckNackResult.ACK_CONTINUE) {
					// The server knows this commit (ackId). We don't
					// need to send any further along its ancestry, but
					// we need to continue to talk about other parts of
					// our local history.
					//
					markCommon(walk.parseAny(ackId));
					receivedAck = true;
					receivedContinue = true;
					havesSinceLastContinue = 0;
				}

				if (monitor.isCancelled())
					throw new CancelledException();
			}

			if (receivedContinue && havesSinceLastContinue > MAX_HAVES) {
				// Our history must be really different from the remote's.
				// We just sent a whole slew of have lines, and it did not
				// recognize any of them. Avoid sending our entire history
				// to them by giving up early.
				//
				break;
			}
		}

		// Tell the remote side we have run out of things to talk about.
		//
		if (monitor.isCancelled())
			throw new CancelledException();
		pckOut.writeString("done\n");
		pckOut.flush();

		if (!receivedAck) {
			// Apparently if we have never received an ACK earlier
			// there is one more result expected from the done we
			// just sent to the remote.
			//
			multiAck = false;
			resultsPending++;
		}

		while (resultsPending > 0 || multiAck) {
			final PacketLineIn.AckNackResult anr;

			anr = pckIn.readACK(ackId);
			resultsPending--;

			if (anr == PacketLineIn.AckNackResult.ACK)
				break; // commit negotiation is finished.

			if (anr == PacketLineIn.AckNackResult.ACK_CONTINUE) {
				// There must be a normal ACK following this.
				//
				multiAck = true;
			}

			if (monitor.isCancelled())
				throw new CancelledException();
		}
	}

	private void negotiateBegin() throws IOException {
		walk.resetRetain(REACHABLE, ADVERTISED);
		walk.markStart(reachableCommits);
		walk.sort(RevSort.COMMIT_TIME_DESC);
		walk.setRevFilter(new RevFilter() {
			@Override
			public RevFilter clone() {
				return this;
			}

			@Override
			public boolean include(final RevWalk walker, final RevCommit c) {
				final boolean remoteKnowsIsCommon = c.has(COMMON);
				if (c.has(ADVERTISED)) {
					// Remote advertised this, and we have it, hence common.
					// Whether or not the remote knows that fact is tested
					// before we added the flag. If the remote doesn't know
					// we have to still send them this object.
					//
					c.add(COMMON);
				}
				return !remoteKnowsIsCommon;
			}
		});
	}

	private void markRefsAdvertised() {
		for (final Ref r : getRefs()) {
			markAdvertised(r.getObjectId());
			if (r.getPeeledObjectId() != null)
				markAdvertised(r.getPeeledObjectId());
		}
	}

	private void markAdvertised(final AnyObjectId id) {
		try {
			walk.parseAny(id).add(ADVERTISED);
		} catch (IOException readError) {
			// We probably just do not have this object locally.
		}
	}

	private void markCommon(final RevObject obj) {
		obj.add(COMMON);
		if (obj instanceof RevCommit)
			((RevCommit) obj).carry(COMMON);
	}

	private void receivePack(final ProgressMonitor monitor) throws IOException {
		final IndexPack ip;

		ip = IndexPack.create(local, sideband ? pckIn.sideband(monitor) : in);
		ip.setFixThin(thinPack);
		ip.index(monitor);
		ip.renameAndOpenPack();
	}

	@Override
	public void close() {
		if (out != null) {
			try {
				pckOut.end();
				out.close();
			} catch (IOException err) {
				// Ignore any close errors.
			} finally {
				out = null;
				pckOut = null;
			}
		}

		if (in != null) {
			try {
				in.close();
			} catch (IOException err) {
				// Ignore any close errors.
			} finally {
				in = null;
				pckIn = null;
			}
		}
	}

	private static class CancelledException extends Exception {
		private static final long serialVersionUID = 1L;
	}
}