/*
 *  Copyright (C) 2008  Roger C. Soares
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.ui;

import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.IFileRevision;
import org.spearce.egit.core.internal.mapping.GitCommitFileRevision;

/**
 * This class executes the search function for the find toolbar. Only one thread
 * is executed at a time.
 * <p>
 * This class maintains a <code>globalThreadIx</code> internal variable that
 * is incremented for each new thread started and the current running thread
 * constantly checks this variable. If the current thread has the same value as
 * <code>globalThreadIx</code> it continues executing, if it has a lower value
 * it means that a more recent search needs to be done and the current isn't
 * necessary any more, so the current thread returns.
 * </p>
 * <p>
 * To avoid consuming all the memory in the system, this class limits the
 * maximum results it stores.
 * </p>
 *
 * @see FindToolbar
 * @see FindResults
 */
public class FindToolbarThread extends Thread {

	private static final int MAX_RESULTS = 20000;

	String pattern;

	List<IFileRevision> fileRevisions;

	FindToolbar toolbar;

	boolean ignoreCase;

	boolean findInCommitId;

	boolean findInComments;

	boolean findInAuthor;

	boolean findInCommitter;

	private static Display display = Display.getDefault();

	private static int globalThreadIx = 0;

	private int currentThreadIx;

	/**
	 * Creates a new object and increments the internal
	 * <code>globalThreadIx</code> variable causing any earlier running thread
	 * to return.
	 */
	public FindToolbarThread() {
		super("history_find_thread" + ++globalThreadIx);
		currentThreadIx = globalThreadIx;
	}

	public void run() {
		execFind(currentThreadIx, fileRevisions, pattern, toolbar, ignoreCase,
				findInCommitId, findInComments, findInAuthor, findInCommitter);
	}

	private synchronized static void execFind(int threadIx,
			List<IFileRevision> fileRevisions, final String pattern,
			final FindToolbar toolbar, boolean ignoreCase,
			boolean findInCommitId, boolean findInComments,
			boolean findInAuthor, boolean findInCommitter) {
		// If it isn't the last event, just ignore it.
		if (threadIx < globalThreadIx) {
			return;
		}

		FindResults findResults = toolbar.findResults;
		findResults.clear();

		boolean maxResultsOverflow = false;
		if (pattern.length() > 0 && fileRevisions != null) {
			String findPattern = pattern;
			if (ignoreCase) {
				findPattern = pattern.toLowerCase();
			}

			long lastUIUpdate = System.currentTimeMillis();

			int totalRevisions = fileRevisions.size();
			int totalMatches = 0;
			boolean notFound = true;
			for (int i = 0; i < totalRevisions; i++) {
				// If a new find event was generated, ends the current thread.
				if (display.isDisposed() || threadIx < globalThreadIx) {
					return;
				}

				// Updates the toolbar with in process info.
				if (System.currentTimeMillis() - lastUIUpdate > 500) {
					final int percentage = (int) (((i + 1F) / totalRevisions) * 100);
					display.asyncExec(new Runnable() {
						public void run() {
							if (toolbar.isDisposed()) {
								return;
							}
							toolbar.progressUpdate(percentage);
						}
					});
					lastUIUpdate = System.currentTimeMillis();
				}

				// Finds for the pattern in the revision history.
				notFound = true;
				IFileRevision fileRevision = fileRevisions.get(i);
				if (fileRevision instanceof GitCommitFileRevision) {
					GitCommitFileRevision revision = (GitCommitFileRevision) fileRevision;

					if (findInCommitId) {
						String contentId = revision.getContentIdentifier();
						if (contentId != null) {
							if (ignoreCase) {
								contentId = contentId.toLowerCase();
							}
							if (contentId.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i);
								notFound = false;
							}
						}
					}

					if (findInComments && notFound) {
						String comment = revision.getComment();
						if (comment != null) {
							if (ignoreCase) {
								comment = comment.toLowerCase();
							}
							if (comment.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i);
								notFound = false;
							}
						}
					}

					if (findInAuthor && notFound) {
						String author = revision.getCommit().getAuthor()
								.getName();
						if (author != null) {
							if (ignoreCase) {
								author = author.toLowerCase();
							}
							if (author.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i);
								notFound = false;
							}
						}
						if (notFound) {
							String email = revision.getCommit().getAuthor()
									.getEmailAddress();
							if (email != null) {
								if (ignoreCase) {
									email = email.toLowerCase();
								}
								if (email.indexOf(findPattern) != -1) {
									totalMatches++;
									findResults.add(i);
									notFound = false;
								}
							}
						}
					}

					if (findInCommitter && notFound) {
						String committer = revision.getCommit().getCommitter()
								.getName();
						if (committer != null) {
							if (ignoreCase) {
								committer = committer.toLowerCase();
							}
							if (committer.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i);
								notFound = false;
							}
						}
						if (notFound) {
							String email = revision.getCommit().getCommitter()
									.getEmailAddress();
							if (email != null) {
								if (ignoreCase) {
									email = email.toLowerCase();
								}
								if (email.indexOf(findPattern) != -1) {
									totalMatches++;
									findResults.add(i);
									notFound = false;
								}
							}
						}
					}
				}

				if (totalMatches == MAX_RESULTS) {
					maxResultsOverflow = true;
					break;
				}
			}
		}

		// Updates the toolbar with the result find info.
		final boolean overflow = maxResultsOverflow;
		display.syncExec(new Runnable() {
			public void run() {
				if (toolbar.isDisposed()) {
					return;
				}
				toolbar.findCompletionUpdate(pattern, overflow);
			}
		});
	}

	static void updateGlobalThreadIx() {
		++globalThreadIx;
	}
}
