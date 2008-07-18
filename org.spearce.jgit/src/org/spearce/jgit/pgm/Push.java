/*
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
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

package org.spearce.jgit.pgm;

import java.util.Collection;
import java.util.LinkedList;

import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.TextProgressMonitor;
import org.spearce.jgit.transport.PushResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteRefUpdate;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.RemoteRefUpdate.Status;

class Push extends TextBuiltin {

	private boolean verbose = false;

	private boolean first = true;

	@Override
	void execute(String[] args) throws Exception {
		final LinkedList<RefSpec> refSpecs = new LinkedList<RefSpec>();
		Boolean thin = null;
		String exec = null;
		boolean forceAll = false;

		int argi = 0;
		for (; argi < args.length; argi++) {
			final String a = args[argi];
			if ("--thin".equals(a))
				thin = true;
			else if ("--no-thin".equals(a))
				thin = false;
			else if ("-f".equals(a) || "--force".equals(a))
				forceAll = true;
			else if (a.startsWith("--exec="))
				exec = a.substring("--exec=".length());
			else if (a.startsWith("--receive-pack="))
				exec = a.substring("--receive-pack=".length());
			else if ("--tags".equals(a))
				refSpecs.add(Transport.REFSPEC_TAGS);
			else if ("--all".equals(a))
				refSpecs.add(Transport.REFSPEC_PUSH_ALL);
			else if ("-v".equals(a))
				verbose = true;
			else if ("--".equals(a)) {
				argi++;
				break;
			} else if (a.startsWith("-"))
				die("usage: push [--all] [--tags] [--force] [--thin]\n"
						+ "[--receive-pack=<git-receive-pack>] [<repository> [<refspec>]...]");
			else
				break;
		}

		final String repository;
		if (argi == args.length)
			repository = "origin";
		else
			repository = args[argi++];
		final Transport transport = Transport.open(db, repository);
		if (thin != null)
			transport.setPushThin(thin);
		if (exec != null)
			transport.setOptionReceivePack(exec);

		for (; argi < args.length; argi++) {
			RefSpec spec = new RefSpec(args[argi]);
			if (forceAll)
				spec = spec.setForceUpdate(true);
			refSpecs.add(spec);
		}
		final Collection<RemoteRefUpdate> toPush = transport
				.findRemoteRefUpdatesFor(refSpecs);

		final PushResult result = transport.push(new TextProgressMonitor(),
				toPush);
		transport.close();

		printPushResult(result);
	}

	private void printPushResult(final PushResult result) {
		boolean everythingUpToDate = true;
		// at first, print up-to-date ones...
		for (final RemoteRefUpdate rru : result.getRemoteUpdates()) {
			if (rru.getStatus() == Status.UP_TO_DATE) {
				if (verbose)
					printRefUpdateResult(result, rru);
			} else
				everythingUpToDate = false;
		}

		for (final RemoteRefUpdate rru : result.getRemoteUpdates()) {
			// ...then successful updates...
			if (rru.getStatus() == Status.OK)
				printRefUpdateResult(result, rru);
		}

		for (final RemoteRefUpdate rru : result.getRemoteUpdates()) {
			// ...finally, others (problematic)
			if (rru.getStatus() != Status.OK
					&& rru.getStatus() != Status.UP_TO_DATE)
				printRefUpdateResult(result, rru);
		}

		if (everythingUpToDate)
			out.println("Everything up-to-date");
	}

	private void printRefUpdateResult(final PushResult result,
			final RemoteRefUpdate rru) {
		if (first) {
			first = false;
			out.format("To %s\n", result.getURI());
		}

		final String remoteName = rru.getRemoteName();
		final String srcRef = rru.isDelete() ? null : rru.getSrcRef();

		switch (rru.getStatus()) {
		case OK:
			if (rru.isDelete())
				printUpdateLine('-', "[deleted]", null, remoteName, null);
			else {
				final Ref oldRef = result.getAdvertisedRef(remoteName);
				if (oldRef == null) {
					final String summary;
					if (remoteName.startsWith(REFS_TAGS))
						summary = "[new tag]";
					else
						summary = "[new branch]";
					printUpdateLine('*', summary, srcRef, remoteName, null);
				} else {
					boolean fastForward = rru.isFastForward();
					final char flag = fastForward ? ' ' : '+';
					final String summary = abbreviateObject(oldRef
							.getObjectId())
							+ (fastForward ? ".." : "...")
							+ abbreviateObject(rru.getNewObjectId());
					final String message = fastForward ? null : "forced update";
					printUpdateLine(flag, summary, srcRef, remoteName, message);
				}
			}
			break;

		case NON_EXISTING:
			printUpdateLine('X', "[no match]", null, remoteName, null);
			break;

		case REJECTED_NODELETE:
			printUpdateLine('!', "[rejected]", null, remoteName,
					"remote side does not support deleting refs");
			break;

		case REJECTED_NONFASTFORWARD:
			printUpdateLine('!', "[rejected]", srcRef, remoteName,
					"non-fast forward");
			break;

		case REJECTED_REMOTE_CHANGED:
			final String message = "remote ref object changed - is not expected one "
					+ abbreviateObject(rru.getExpectedOldObjectId());
			printUpdateLine('!', "[rejected]", srcRef, remoteName, message);
			break;

		case REJECTED_OTHER_REASON:
			printUpdateLine('!', "[remote rejected]", srcRef, remoteName, rru
					.getMessage());
			break;

		case UP_TO_DATE:
			if (verbose)
				printUpdateLine('=', "[up to date]", srcRef, remoteName, null);
			break;

		case NOT_ATTEMPTED:
		case AWAITING_REPORT:
			printUpdateLine('?', "[unexpected push-process behavior]", srcRef,
					remoteName, rru.getMessage());
			break;
		}
	}

	private void printUpdateLine(final char flag, final String summary,
			final String srcRef, final String destRef, final String message) {
		out.format(" %c %-17s", flag, summary);

		if (srcRef != null)
			out.format(" %s ->", abbreviateRef(srcRef, true));
		out.format(" %s", abbreviateRef(destRef, true));

		if (message != null)
			out.format(" (%s)", message);

		out.println();
	}
}
