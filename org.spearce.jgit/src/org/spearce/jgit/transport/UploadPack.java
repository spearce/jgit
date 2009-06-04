/*
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.transport;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.NullProgressMonitor;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PackWriter;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.RefComparator;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevFlag;
import org.spearce.jgit.revwalk.RevFlagSet;
import org.spearce.jgit.revwalk.RevObject;
import org.spearce.jgit.revwalk.RevTag;
import org.spearce.jgit.revwalk.RevWalk;

/**
 * Implements the server side of a fetch connection, transmitting objects.
 */
public class UploadPack {
	static final String OPTION_INCLUDE_TAG = BasePackFetchConnection.OPTION_INCLUDE_TAG;

	static final String OPTION_MULTI_ACK = BasePackFetchConnection.OPTION_MULTI_ACK;

	static final String OPTION_THIN_PACK = BasePackFetchConnection.OPTION_THIN_PACK;

	static final String OPTION_SIDE_BAND = BasePackFetchConnection.OPTION_SIDE_BAND;

	static final String OPTION_SIDE_BAND_64K = BasePackFetchConnection.OPTION_SIDE_BAND_64K;

	static final String OPTION_OFS_DELTA = BasePackFetchConnection.OPTION_OFS_DELTA;

	static final String OPTION_NO_PROGRESS = BasePackFetchConnection.OPTION_NO_PROGRESS;

	/** Database we read the objects from. */
	private final Repository db;

	/** Revision traversal support over {@link #db}. */
	private final RevWalk walk;

	private InputStream rawIn;

	private OutputStream rawOut;

	private PacketLineIn pckIn;

	private PacketLineOut pckOut;

	/** The refs we advertised as existing at the start of the connection. */
	private Map<String, Ref> refs;

	/** Capabilities requested by the client. */
	private final Set<String> options = new HashSet<String>();

	/** Objects the client wants to obtain. */
	private final List<RevObject> wantAll = new ArrayList<RevObject>();

	/** Objects the client wants to obtain. */
	private final List<RevCommit> wantCommits = new ArrayList<RevCommit>();

	/** Objects on both sides, these don't have to be sent. */
	private final List<RevObject> commonBase = new ArrayList<RevObject>();

	/** null if {@link #commonBase} should be examined again. */
	private Boolean okToGiveUp;

	/** Marked on objects we sent in our advertisement list. */
	private final RevFlag ADVERTISED;

	/** Marked on objects the client has asked us to give them. */
	private final RevFlag WANT;

	/** Marked on objects both we and the client have. */
	private final RevFlag PEER_HAS;

	/** Marked on objects in {@link #commonBase}. */
	private final RevFlag COMMON;

	private final RevFlagSet SAVE;

	private boolean multiAck;

	/**
	 * Create a new pack upload for an open repository.
	 *
	 * @param copyFrom
	 *            the source repository.
	 */
	public UploadPack(final Repository copyFrom) {
		db = copyFrom;
		walk = new RevWalk(db);
		walk.setRetainBody(false);

		ADVERTISED = walk.newFlag("ADVERTISED");
		WANT = walk.newFlag("WANT");
		PEER_HAS = walk.newFlag("PEER_HAS");
		COMMON = walk.newFlag("COMMON");
		walk.carry(PEER_HAS);

		SAVE = new RevFlagSet();
		SAVE.add(ADVERTISED);
		SAVE.add(WANT);
		SAVE.add(PEER_HAS);
	}

	/** @return the repository this receive completes into. */
	public final Repository getRepository() {
		return db;
	}

	/** @return the RevWalk instance used by this connection. */
	public final RevWalk getRevWalk() {
		return walk;
	}

	/**
	 * Execute the upload task on the socket.
	 *
	 * @param input
	 *            raw input to read client commands from. Caller must ensure the
	 *            input is buffered, otherwise read performance may suffer.
	 * @param output
	 *            response back to the Git network client, to write the pack
	 *            data onto. Caller must ensure the output is buffered,
	 *            otherwise write performance may suffer.
	 * @param messages
	 *            secondary "notice" channel to send additional messages out
	 *            through. When run over SSH this should be tied back to the
	 *            standard error channel of the command execution. For most
	 *            other network connections this should be null.
	 * @throws IOException
	 */
	public void upload(final InputStream input, final OutputStream output,
			final OutputStream messages) throws IOException {
		rawIn = input;
		rawOut = output;

		pckIn = new PacketLineIn(rawIn);
		pckOut = new PacketLineOut(rawOut);
		service();
	}

	private void service() throws IOException {
		sendAdvertisedRefs();
		recvWants();
		if (wantAll.isEmpty())
			return;
		multiAck = options.contains(OPTION_MULTI_ACK);
		negotiate();
		sendPack();
	}

	private void sendAdvertisedRefs() throws IOException {
		refs = db.getAllRefs();

		final StringBuilder m = new StringBuilder(100);
		final char[] idtmp = new char[2 * Constants.OBJECT_ID_LENGTH];
		final Iterator<Ref> i = RefComparator.sort(refs.values()).iterator();
		if (i.hasNext()) {
			final Ref r = i.next();
			final RevObject o = safeParseAny(r.getObjectId());
			if (o != null) {
				advertise(m, idtmp, o, r.getOrigName());
				m.append('\0');
				m.append(' ');
				m.append(OPTION_INCLUDE_TAG);
				m.append(' ');
				m.append(OPTION_MULTI_ACK);
				m.append(' ');
				m.append(OPTION_OFS_DELTA);
				m.append(' ');
				m.append(OPTION_SIDE_BAND);
				m.append(' ');
				m.append(OPTION_SIDE_BAND_64K);
				m.append(' ');
				m.append(OPTION_THIN_PACK);
				m.append(' ');
				m.append(OPTION_NO_PROGRESS);
				m.append(' ');
				writeAdvertisedRef(m);
				if (o instanceof RevTag)
					writeAdvertisedTag(m, idtmp, o, r.getName());
			}
		}
		while (i.hasNext()) {
			final Ref r = i.next();
			final RevObject o = safeParseAny(r.getObjectId());
			if (o != null) {
				advertise(m, idtmp, o, r.getOrigName());
				writeAdvertisedRef(m);
				if (o instanceof RevTag)
					writeAdvertisedTag(m, idtmp, o, r.getName());
			}
		}
		pckOut.end();
	}

	private RevObject safeParseAny(final ObjectId id) {
		try {
			return walk.parseAny(id);
		} catch (IOException e) {
			return null;
		}
	}

	private void advertise(final StringBuilder m, final char[] idtmp,
			final RevObject o, final String name) {
		o.add(ADVERTISED);
		m.setLength(0);
		o.getId().copyTo(idtmp, m);
		m.append(' ');
		m.append(name);
	}

	private void writeAdvertisedRef(final StringBuilder m) throws IOException {
		m.append('\n');
		pckOut.writeString(m.toString());
	}

	private void writeAdvertisedTag(final StringBuilder m, final char[] idtmp,
			final RevObject tag, final String name) throws IOException {
		RevObject o = tag;
		while (o instanceof RevTag) {
			// Fully unwrap here so later on we have these already parsed.
			try {
				walk.parseHeaders(((RevTag) o).getObject());
			} catch (IOException err) {
				return;
			}
			o = ((RevTag) o).getObject();
			o.add(ADVERTISED);
		}
		advertise(m, idtmp, ((RevTag) tag).getObject(), name + "^{}");
		writeAdvertisedRef(m);
	}

	private void recvWants() throws IOException {
		boolean isFirst = true;
		for (;; isFirst = false) {
			String line;
			try {
				line = pckIn.readString();
			} catch (EOFException eof) {
				if (isFirst)
					break;
				throw eof;
			}

			if (line == PacketLineIn.END)
				break;
			if (!line.startsWith("want ") || line.length() < 45)
				throw new PackProtocolException("expected want; got " + line);

			if (isFirst) {
				final int sp = line.indexOf(' ', 45);
				if (sp >= 0) {
					for (String c : line.substring(sp + 1).split(" "))
						options.add(c);
					line = line.substring(0, sp);
				}
			}

			final ObjectId id = ObjectId.fromString(line.substring(5));
			final RevObject o;
			try {
				o = walk.parseAny(id);
			} catch (IOException e) {
				throw new PackProtocolException(id.name() + " not valid", e);
			}
			if (!o.has(ADVERTISED))
				throw new PackProtocolException(id.name() + " not valid");
			want(o);
		}
	}

	private void want(RevObject o) {
		if (!o.has(WANT)) {
			o.add(WANT);
			wantAll.add(o);

			if (o instanceof RevCommit)
				wantCommits.add((RevCommit) o);

			else if (o instanceof RevTag) {
				do {
					o = ((RevTag) o).getObject();
				} while (o instanceof RevTag);
				if (o instanceof RevCommit)
					want(o);
			}
		}
	}

	private void negotiate() throws IOException {
		ObjectId last = ObjectId.zeroId();
		for (;;) {
			String line;
			try {
				line = pckIn.readString();
			} catch (EOFException eof) {
				throw eof;
			}

			if (line == PacketLineIn.END) {
				if (commonBase.isEmpty() || multiAck)
					pckOut.writeString("NAK\n");
				pckOut.flush();
			} else if (line.startsWith("have ") && line.length() == 45) {
				final ObjectId id = ObjectId.fromString(line.substring(5));
				if (matchHave(id)) {
					// Both sides have the same object; let the client know.
					//
					if (multiAck) {
						last = id;
						pckOut.writeString("ACK " + id.name() + " continue\n");
					} else if (commonBase.size() == 1)
						pckOut.writeString("ACK " + id.name() + "\n");
				} else {
					// They have this object; we don't.
					//
					if (multiAck && okToGiveUp())
						pckOut.writeString("ACK " + id.name() + " continue\n");
				}

			} else if (line.equals("done")) {
				if (commonBase.isEmpty())
					pckOut.writeString("NAK\n");

				else if (multiAck)
					pckOut.writeString("ACK " + last.name() + "\n");
				break;

			} else {
				throw new PackProtocolException("expected have; got " + line);
			}
		}
	}

	private boolean matchHave(final ObjectId id) {
		final RevObject o;
		try {
			o = walk.parseAny(id);
		} catch (IOException err) {
			return false;
		}

		if (!o.has(PEER_HAS)) {
			o.add(PEER_HAS);
			if (o instanceof RevCommit)
				((RevCommit) o).carry(PEER_HAS);
			addCommonBase(o);
		}
		return true;
	}

	private void addCommonBase(final RevObject o) {
		if (!o.has(COMMON)) {
			o.add(COMMON);
			commonBase.add(o);
			okToGiveUp = null;
		}
	}

	private boolean okToGiveUp() throws PackProtocolException {
		if (okToGiveUp == null)
			okToGiveUp = Boolean.valueOf(okToGiveUpImp());
		return okToGiveUp.booleanValue();
	}

	private boolean okToGiveUpImp() throws PackProtocolException {
		if (commonBase.isEmpty())
			return false;

		try {
			for (final Iterator<RevCommit> i = wantCommits.iterator(); i
					.hasNext();) {
				final RevCommit want = i.next();
				if (wantSatisfied(want))
					i.remove();
			}
		} catch (IOException e) {
			throw new PackProtocolException("internal revision error", e);
		}
		return wantCommits.isEmpty();
	}

	private boolean wantSatisfied(final RevCommit want) throws IOException {
		walk.resetRetain(SAVE);
		walk.markStart(want);
		for (;;) {
			final RevCommit c = walk.next();
			if (c == null)
				break;
			if (c.has(PEER_HAS)) {
				addCommonBase(c);
				return true;
			}
		}
		return false;
	}

	private void sendPack() throws IOException {
		final boolean thin = options.contains(OPTION_THIN_PACK);
		final boolean progress = !options.contains(OPTION_NO_PROGRESS);
		final boolean sideband = options.contains(OPTION_SIDE_BAND)
				|| options.contains(OPTION_SIDE_BAND_64K);

		ProgressMonitor pm = NullProgressMonitor.INSTANCE;
		OutputStream packOut = rawOut;

		if (sideband) {
			int bufsz = SideBandOutputStream.SMALL_BUF;
			if (options.contains(OPTION_SIDE_BAND_64K))
				bufsz = SideBandOutputStream.MAX_BUF;
			bufsz -= SideBandOutputStream.HDR_SIZE;

			packOut = new BufferedOutputStream(new SideBandOutputStream(
					SideBandOutputStream.CH_DATA, pckOut), bufsz);

			if (progress)
				pm = new SideBandProgressMonitor(pckOut);
		}

		final PackWriter pw;
		pw = new PackWriter(db, pm, NullProgressMonitor.INSTANCE);
		pw.setDeltaBaseAsOffset(options.contains(OPTION_OFS_DELTA));
		pw.setThin(thin);
		pw.preparePack(wantAll, commonBase);
		if (options.contains(OPTION_INCLUDE_TAG)) {
			for (final Ref r : refs.values()) {
				final RevObject o;
				try {
					o = walk.parseAny(r.getObjectId());
				} catch (IOException e) {
					continue;
				}
				if (o.has(WANT) || !(o instanceof RevTag))
					continue;
				final RevTag t = (RevTag) o;
				if (!pw.willInclude(t) && pw.willInclude(t.getObject()))
					pw.addObject(t);
			}
		}
		pw.writePack(packOut);

		if (sideband) {
			packOut.flush();
			pckOut.end();
		} else {
			rawOut.flush();
		}
	}
}
