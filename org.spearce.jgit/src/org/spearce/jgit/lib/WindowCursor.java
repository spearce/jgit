package org.spearce.jgit.lib;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Active handle to a ByteWindow. */
public final class WindowCursor {
	ByteWindow window;

	/**
	 * Copy bytes from the window to a caller supplied buffer.
	 * 
	 * @param pos
	 *            offset within the window to start copying from.
	 * @param dstbuf
	 *            destination buffer to copy into.
	 * @param dstoff
	 *            offset within <code>dstbuf</code> to start copying into.
	 * @param cnt
	 *            number of bytes to copy. This value may exceed the number of
	 *            bytes remaining in the window starting at offset
	 *            <code>pos</code>.
	 * @return number of bytes actually copied; this may be less than
	 *         <code>cnt</code> if <code>cnt</code> exceeded the number of
	 *         bytes available.
	 */
	public int copy(int pos, byte[] dstbuf, int dstoff, int cnt) {
		return window.copy(pos, dstbuf, dstoff, cnt);
	}

	/**
	 * Pump bytes into the supplied inflater as input.
	 * 
	 * @param pos
	 *            offset within the window to start supplying input from.
	 * @param dstbuf
	 *            destination buffer the inflater should output decompressed
	 *            data to.
	 * @param dstoff
	 *            current offset within <code>dstbuf</code> to inflate into.
	 * @param inf
	 *            the inflater to feed input to. The caller is responsible for
	 *            initializing the inflater as multiple windows may need to
	 *            supply data to the same inflater to completely decompress
	 *            something.
	 * @return updated <code>dstoff</code> based on the number of bytes
	 *         successfully copied into <code>dstbuf</code> by
	 *         <code>inf</code>. If the inflater is not yet finished then
	 *         another window's data must still be supplied as input to finish
	 *         decompression.
	 * @throws DataFormatException
	 *             the inflater encountered an invalid chunk of data. Data
	 *             stream corruption is likely.
	 */
	public int inflate(int pos, byte[] dstbuf, int dstoff, Inflater inf)
			throws DataFormatException {
		return window.inflate(pos, dstbuf, dstoff, inf);
	}

	/** Release the current window cursor. */
	public void release() {
		window = null;
	}
}
