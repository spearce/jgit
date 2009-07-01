/*
 * Copyright (C) 2009, JetBrains s.r.o.
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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.spearce.jgit.treewalk.TreeWalk;

/**
 * The configuration file based on the blobs stored in the repository
 */
public class BlobBasedConfig extends Config {
	private Callable<byte[]> blobProvider;

	/**
	 * The constructor for blob based config
	 *
	 * @param base
	 *            the base configuration file
	 * @param blob
	 *            the provider for blobs
	 */
	public BlobBasedConfig(Config base, Callable<byte[]> blob) {
		super(base);
		blobProvider = blob;
	}

	/**
	 * The constructor from a byte array
	 *
	 * @param base
	 *            the base configuration file
	 * @param blob
	 *            the byte array
	 */
	public BlobBasedConfig(Config base, final byte[] blob) {
		this(base, new Callable<byte[]>() {
			public byte[] call() throws Exception {
				return blob;
			}
		});
	}

	/**
	 * The constructor from object identifier
	 *
	 * @param base
	 *            the base configuration file
	 * @param r
	 *            the repository
	 * @param objectId
	 *            the object identifier
	 */
	public BlobBasedConfig(Config base, final Repository r,
			final ObjectId objectId) {
		this(base, new Callable<byte[]>() {
			public byte[] call() throws Exception {
				ObjectLoader loader = r.openBlob(objectId);
				if (loader == null) {
					throw new IOException("Blob not found: " + objectId);
				}
				return loader.getBytes();
			}
		});
	}

	/**
	 * The constructor from commit and path
	 *
	 * @param base
	 *            the base configuration file
	 * @param commit
	 *            the commit that contains the object
	 * @param path
	 *            the path within the tree of the commit
	 */
	public BlobBasedConfig(Config base, final Commit commit, final String path) {
		this(base, new Callable<byte[]>() {
			public byte[] call() throws Exception {
				final ObjectId treeId = commit.getTreeId();
				final Repository r = commit.getRepository();
				final TreeWalk tree = TreeWalk.forPath(r, path, treeId);
				if (tree == null) {
					throw new FileNotFoundException("Entry not found by path: " + path);
				}
				ObjectId blobId = tree.getObjectId(0);
				ObjectLoader loader = tree.getRepository().openBlob(blobId);
				if (loader == null) {
					throw new IOException("Blob not found: " + blobId + " for path: " + path);
				}
				return loader.getBytes();
			}
		});
	}

	@Override
	protected InputStream openInputStream() throws IOException {
		try {
			return new ByteArrayInputStream(blobProvider.call());
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			final IOException e2 = new IOException("Unable to read config");
			e2.initCause(e);
			throw e2;
		}
	}
}
