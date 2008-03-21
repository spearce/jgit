package org.spearce.jgit.lib;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/** Active handle to a ByteWindow. */
public final class WindowCursor {
	ByteWindow window;

	Object handle;

	/**
	 * Copy bytes from the window to a caller supplied buffer.
	 * 
	 * @param provider
	 *            the file the desired window is stored within.
	 * @param id
	 *            unique id number of this window within the file.
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
	 * @throws IOException
	 *             this cursor does not match the provider or id and the proper
	 *             window could not be acquired through the provider's cache.
	 */
	int copy(final WindowedFile provider, final int id, final int pos,
			final byte[] dstbuf, final int dstoff, final int cnt)
			throws IOException {
		pin(provider, id);
		return window.copy(handle, pos, dstbuf, dstoff, cnt);
	}

	/**
	 * Pump bytes into the supplied inflater as input.
	 * 
	 * @param provider
	 *            the file the desired window is stored within.
	 * @param id
	 *            unique id number of this window within the file.
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
	 * @throws IOException
	 *             this cursor does not match the provider or id and the proper
	 *             window could not be acquired through the provider's cache.
	 * @throws DataFormatException
	 *             the inflater encountered an invalid chunk of data. Data
	 *             stream corruption is likely.
	 */
	int inflate(final WindowedFile provider, final int id, final int pos,
			final byte[] dstbuf, final int dstoff, final Inflater inf)
			throws IOException, DataFormatException {
		pin(provider, id);
		return window.inflate(handle, pos, dstbuf, dstoff, inf);
	}

	private void pin(final WindowedFile provider, final int id)
			throws IOException {
		final ByteWindow w = window;
		if (w == null || w.provider != provider || w.id != id)
			provider.cache.get(this, provider, id);
	}

	/** Release the current window cursor. */
	public void release() {
		window = null;
		handle = null;
	}
}
