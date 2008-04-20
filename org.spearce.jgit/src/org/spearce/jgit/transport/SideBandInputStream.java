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

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spearce.jgit.errors.PackProtocolException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ProgressMonitor;
import org.spearce.jgit.util.NB;

/**
 * Unmultiplexes the data portion of a side-band channel.
 * <p>
 * Reading from this input stream obtains data from channel 1, which is
 * typically the bulk data stream.
 * <p>
 * Channel 2 is transparently unpacked and "scraped" to update a progress
 * monitor. The scraping is performed behind the scenes as part of any of the
 * read methods offered by this stream.
 * <p>
 * Channel 3 results in an exception being thrown, as the remote side has issued
 * an unrecoverable error.
 * 
 * @see PacketLineIn#sideband(ProgressMonitor)
 */
class SideBandInputStream extends InputStream {
	private static final int CH_DATA = 1;

	private static final int CH_PROGRESS = 2;

	private static final int CH_ERROR = 3;

	private static Pattern P_UNBOUNDED = Pattern.compile(
			".*?([\\w ]+): (\\d+)(, done)?.*", Pattern.DOTALL);

	private static Pattern P_BOUNDED = Pattern.compile(
			".*?([\\w ]+):.*\\((\\d+)/(\\d+)\\).*", Pattern.DOTALL);

	private final PacketLineIn pckIn;

	private final InputStream in;

	private final ProgressMonitor monitor;

	private String currentTask;

	private int lastCnt;

	private boolean eof;

	private int channel;

	private int available;

	SideBandInputStream(final PacketLineIn aPckIn, final InputStream aIn,
			final ProgressMonitor aProgress) {
		pckIn = aPckIn;
		in = aIn;
		monitor = aProgress;
		currentTask = "";
	}

	@Override
	public int read() throws IOException {
		needDataPacket();
		if (eof)
			return -1;
		available--;
		return in.read();
	}

	@Override
	public int read(final byte[] b, int off, int len) throws IOException {
		int r = 0;
		while (len > 0) {
			needDataPacket();
			if (eof)
				break;
			final int n = in.read(b, off, Math.min(len, available));
			if (n < 0)
				break;
			r += n;
			off += n;
			len -= n;
			available -= n;
		}
		return eof && r == 0 ? -1 : r;
	}

	private void needDataPacket() throws IOException {
		if (eof || (channel == CH_DATA && available > 0))
			return;
		for (;;) {
			available = pckIn.readLength();
			if (available == 0) {
				eof = true;
				return;
			}

			channel = in.read();
			available -= 5; // length header plus channel indicator
			if (available == 0)
				continue;

			switch (channel) {
			case CH_DATA:
				return;
			case CH_PROGRESS:
				progress(readString(available));

				continue;
			case CH_ERROR:
				eof = true;
				throw new TransportException("remote: " + readString(available));
			default:
				throw new PackProtocolException("Invalid channel " + channel);
			}
		}
	}

	private void progress(final String msg) {
		Matcher matcher;

		matcher = P_BOUNDED.matcher(msg);
		if (matcher.matches()) {
			final String taskname = matcher.group(1);
			if (!currentTask.equals(taskname)) {
				currentTask = taskname;
				lastCnt = 0;
				final int tot = Integer.parseInt(matcher.group(3));
				monitor.beginTask(currentTask, tot);
			}
			final int cnt = Integer.parseInt(matcher.group(2));
			monitor.update(cnt - lastCnt);
			lastCnt = cnt;
			return;
		}

		matcher = P_UNBOUNDED.matcher(msg);
		if (matcher.matches()) {
			final String taskname = matcher.group(1);
			if (!currentTask.equals(taskname)) {
				currentTask = taskname;
				lastCnt = 0;
				monitor.beginTask(currentTask, ProgressMonitor.UNKNOWN);
			}
			final int cnt = Integer.parseInt(matcher.group(2));
			monitor.update(cnt - lastCnt);
			lastCnt = cnt;
		}
	}

	private String readString(final int len) throws IOException {
		final byte[] raw = new byte[len];
		NB.readFully(in, raw, 0, len);
		return new String(raw, 0, len, Constants.CHARACTER_ENCODING);
	}
}
