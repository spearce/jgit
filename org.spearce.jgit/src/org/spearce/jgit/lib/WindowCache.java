/*
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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

package org.spearce.jgit.lib;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;

/**
 * The WindowCache manages reusable <code>Windows</code> and inflaters used by
 * the other windowed file access classes.
 */
public class WindowCache {
	private static final int bits(int newSize) {
		if (newSize < 4096)
			throw new IllegalArgumentException("Invalid window size");
		if (Integer.bitCount(newSize) != 1)
			throw new IllegalArgumentException("Window size must be power of 2");
		return Integer.numberOfTrailingZeros(newSize);
	}

	private static int maxFileCount;

	private static int maxByteCount;

	private static int windowSize;

	private static int windowSizeShift;

	static boolean mmap;

	static final ReferenceQueue<?> clearedWindowQueue;

	private static ByteWindow[] cache;

	private static ByteWindow lruHead;

	private static ByteWindow lruTail;

	private static int openFileCount;

	private static int openByteCount;

	static {
		final WindowCacheConfig c = new WindowCacheConfig();
		maxFileCount = c.getPackedGitOpenFiles();
		maxByteCount = c.getPackedGitLimit();
		windowSizeShift = bits(c.getPackedGitWindowSize());
		windowSize = 1 << windowSizeShift;
		mmap = c.isPackedGitMMAP();
		cache = new ByteWindow[cacheTableSize()];
		clearedWindowQueue = new ReferenceQueue<Object>();
	}

	private static int cacheTableSize() {
		return 5 * (maxByteCount / windowSize) / 2;
	}

	/**
	 * Modify the configuration of the window cache.
	 * <p>
	 * The new configuration is applied immediately. If the new limits are
	 * smaller than what what is currently cached, older entries will be purged
	 * as soon as possible to allow the cache to meet the new limit.
	 * 
	 * @param packedGitLimit
	 *            maximum number of bytes to hold within this instance.
	 * @param packedGitWindowSize
	 *            number of bytes per window within the cache.
	 * @param packedGitMMAP
	 *            true to enable use of mmap when creating windows.
	 * @param deltaBaseCacheLimit
	 *            number of bytes to hold in the delta base cache.
	 * @deprecated Use {@link WindowCacheConfig} instead.
	 */
	public static void reconfigure(final int packedGitLimit,
			final int packedGitWindowSize, final boolean packedGitMMAP,
			final int deltaBaseCacheLimit) {
		final WindowCacheConfig c = new WindowCacheConfig();
		c.setPackedGitLimit(packedGitLimit);
		c.setPackedGitWindowSize(packedGitWindowSize);
		c.setPackedGitMMAP(packedGitMMAP);
		c.setDeltaBaseCacheLimit(deltaBaseCacheLimit);
		reconfigure(c);
	}

	/**
	 * Modify the configuration of the window cache.
	 * <p>
	 * The new configuration is applied immediately. If the new limits are
	 * smaller than what what is currently cached, older entries will be purged
	 * as soon as possible to allow the cache to meet the new limit.
	 *
	 * @param cfg
	 *            the new window cache configuration.
	 */
	public static void reconfigure(final WindowCacheConfig cfg) {
		reconfigureImpl(cfg);
		UnpackedObjectCache.reconfigure(cfg);
	}

	private static synchronized void reconfigureImpl(final WindowCacheConfig cfg) {
		boolean prune = false;
		boolean evictAll = false;

		if (maxFileCount < cfg.getPackedGitOpenFiles())
			maxFileCount = cfg.getPackedGitOpenFiles();
		else if (maxFileCount > cfg.getPackedGitOpenFiles()) {
			maxFileCount = cfg.getPackedGitOpenFiles();
			prune = true;
		}

		if (maxByteCount < cfg.getPackedGitLimit()) {
			maxByteCount = cfg.getPackedGitLimit();
		} else if (maxByteCount > cfg.getPackedGitLimit()) {
			maxByteCount = cfg.getPackedGitLimit();
			prune = true;
		}

		if (bits(cfg.getPackedGitWindowSize()) != windowSizeShift) {
			windowSizeShift = bits(cfg.getPackedGitWindowSize());
			windowSize = 1 << windowSizeShift;
			evictAll = true;
		}

		if (mmap != cfg.isPackedGitMMAP()) {
			mmap = cfg.isPackedGitMMAP();
			evictAll = true;
		}

		if (evictAll) {
			// We have to throw away every window we have. None
			// of them are suitable for the new configuration.
			//
			for (ByteWindow<?> e : cache) {
				for (; e != null; e = e.chainNext)
					clear(e);
			}
			runClearedWindowQueue();
			cache = new ByteWindow[cacheTableSize()];

		} else {
			if (prune) {
				// We should decrease our memory usage.
				//
				releaseMemory();
				runClearedWindowQueue();
			}

			if (cache.length != cacheTableSize()) {
				// The cache table should be resized.
				// Rehash every entry.
				//
				final ByteWindow[] priorTable = cache;

				cache = new ByteWindow[cacheTableSize()];
				for (ByteWindow<?> e : priorTable) {
					for (ByteWindow<?> n; e != null; e = n) {
						n = e.chainNext;
						final int idx = hash(e.provider, e.id);
						e.chainNext = cache[idx];
						cache[idx] = e;
					}
				}
			}
		}
	}

	/**
	 * Get a specific window.
	 * 
	 * @param curs
	 *            an active cursor object to maintain the window reference while
	 *            the caller needs it.
	 * @param wp
	 *            the provider of the window. If the window is not currently in
	 *            the cache then the provider will be asked to load it.
	 * @param position
	 *            offset (in bytes) within the file that the caller needs access
	 *            to.
	 * @throws IOException
	 *             the window was not found in the cache and the given provider
	 *             was unable to load the window on demand.
	 */
	public static final void get(final WindowCursor curs, final PackFile wp,
			final long position) throws IOException {
		getImpl(curs, wp, position);
		curs.window.ensureLoaded(curs.handle);
	}

	static synchronized final void pin(final PackFile wp) throws IOException {
		if (++wp.openCount == 1) {
			openFile(wp);
		}
	}

	static synchronized final void unpin(final PackFile wp) {
		if (--wp.openCount == 0) {
			openFileCount--;
			wp.cacheClose();
		}
	}

	private static synchronized final void getImpl(final WindowCursor curs,
			final PackFile wp, final long position) throws IOException {
		final int id = (int) (position >> windowSizeShift);
		final int idx = hash(wp, id);
		for (ByteWindow<?> e = cache[idx]; e != null; e = e.chainNext) {
			if (e.provider == wp && e.id == id) {
				if ((curs.handle = e.get()) != null) {
					curs.window = e;
					makeMostRecent(e);
					return;
				}

				clear(e);
				break;
			}
		}

		if (wp.openCount == 0) {
			openFile(wp);

			// The cacheOpen may have mapped the window we are trying to
			// map ourselves. Retrying the search ensures that does not
			// happen to us.
			//
			for (ByteWindow<?> e = cache[idx]; e != null; e = e.chainNext) {
				if (e.provider == wp && e.id == id) {
					if ((curs.handle = e.get()) != null) {
						curs.window = e;
						makeMostRecent(e);
						return;
					}

					clear(e);
					break;
				}
			}
		}

		final int wsz = windowSize(wp, id);
		wp.openCount++;
		openByteCount += wsz;
		releaseMemory();
		runClearedWindowQueue();

		wp.allocWindow(curs, id, (position >>> windowSizeShift) << windowSizeShift, wsz);
		final ByteWindow<?> e = curs.window;
		e.chainNext = cache[idx];
		cache[idx] = e;
		insertLRU(e);
	}

	private static void openFile(final PackFile wp) throws IOException {
		try {
			openFileCount++;
			releaseMemory();
			runClearedWindowQueue();
			wp.openCount = 1;
			wp.cacheOpen();
		} catch (IOException ioe) {
			openFileCount--;
			wp.openCount = 0;
			throw ioe;
		} catch (RuntimeException ioe) {
			openFileCount--;
			wp.openCount = 0;
			throw ioe;
		} catch (Error ioe) {
			openFileCount--;
			wp.openCount = 0;
			throw ioe;
		} finally {
			wp.openCount--;
		}
	}

	static synchronized void markLoaded(final ByteWindow w) {
		if (--w.provider.openCount == 0) {
			openFileCount--;
			w.provider.cacheClose();
		}
	}

	private static void makeMostRecent(ByteWindow<?> e) {
		if (lruHead != e) {
			unlinkLRU(e);
			insertLRU(e);
		}
	}

	private static void releaseMemory() {
		ByteWindow<?> e = lruTail;
		while (isOverLimit() && e != null) {
			final ByteWindow<?> p = e.lruPrev;
			clear(e);
			e = p;
		}
	}

	private static boolean isOverLimit() {
		return openByteCount > maxByteCount || openFileCount > maxFileCount;
	}

	/**
	 * Remove all windows associated with a specific provider.
	 * <p>
	 * Providers should invoke this method as part of their cleanup/close
	 * routines, ensuring that the window cache releases all windows that cannot
	 * ever be requested again.
	 * </p>
	 * 
	 * @param wp
	 *            the window provider whose windows should be removed from the
	 *            cache.
	 */
	public static synchronized final void purge(final PackFile wp) {
		for (ByteWindow e : cache) {
			for (; e != null; e = e.chainNext) {
				if (e.provider == wp)
					clear(e);
			}
		}
		runClearedWindowQueue();
	}

	private static void runClearedWindowQueue() {
		ByteWindow<?> e;
		while ((e = (ByteWindow) clearedWindowQueue.poll()) != null) {
			unlinkSize(e);
			unlinkLRU(e);
			unlinkCache(e);
			e.chainNext = null;
			e.lruNext = null;
			e.lruPrev = null;
		}
	}

	private static void clear(final ByteWindow<?> e) {
		unlinkSize(e);
		e.clear();
		e.enqueue();
	}

	private static void unlinkSize(final ByteWindow<?> e) {
		if (e.sizeActive) {
			if (--e.provider.openCount == 0) {
				openFileCount--;
				e.provider.cacheClose();
			}
			openByteCount -= e.size;
			e.sizeActive = false;
		}
	}

	private static void unlinkCache(final ByteWindow dead) {
		final int idx = hash(dead.provider, dead.id);
		ByteWindow<?> e = cache[idx], p = null, n;
		for (; e != null; p = e, e = n) {
			n = e.chainNext;
			if (e == dead) {
				if (p == null)
					cache[idx] = n;
				else
					p.chainNext = n;
				break;
			}
		}
	}

	private static void unlinkLRU(final ByteWindow e) {
		final ByteWindow<?> prev = e.lruPrev;
		final ByteWindow<?> next = e.lruNext;

		if (prev != null)
			prev.lruNext = next;
		else
			lruHead = next;

		if (next != null)
			next.lruPrev = prev;
		else
			lruTail = prev;
	}

	private static void insertLRU(final ByteWindow<?> e) {
		final ByteWindow h = lruHead;
		e.lruPrev = null;
		e.lruNext = h;
		if (h != null)
			h.lruPrev = e;
		else
			lruTail = e;
		lruHead = e;
	}

	private static int hash(final PackFile wp, final int id) {
		// wp.hash was already "stirred up" a bit by * 31 when
		// it was created. Its reasonable to just add here.
		//
		return ((wp.hash + id) >>> 1) % cache.length;
	}

	private static int windowSize(final PackFile file, final int id) {
		final long len = file.length;
		final long pos = id << windowSizeShift;
		return len < pos + windowSize ? (int) (len - pos) : windowSize;
	}

	private WindowCache() {
		throw new UnsupportedOperationException();
	}
}
