/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.history;

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

	private static final Object EXEC_LOCK = new Object();

	private static final int MAX_RESULTS = 20000;

	String pattern;

	SWTCommit[] fileRevisions;

	FindToolbar toolbar;

	boolean ignoreCase;

	boolean findInCommitId;

	boolean findInComments;

	boolean findInAuthor;

	boolean findInCommitter;

	private volatile static int globalThreadIx = 0;

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
		synchronized (EXEC_LOCK) {
			execFind();
		}
	}

	private void execFind() {
		// If it isn't the last event, just ignore it.
		if (currentThreadIx < globalThreadIx) {
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

			int totalRevisions = fileRevisions.length;
			int totalMatches = 0;
			boolean notFound = true;
			for (int i = 0; i < totalRevisions; i++) {
				// If a new find event was generated, ends the current thread.
				if (toolbar.getDisplay().isDisposed()
						|| currentThreadIx < globalThreadIx) {
					return;
				}

				// Updates the toolbar with in process info.
				if (System.currentTimeMillis() - lastUIUpdate > 500) {
					final int percentage = (int) (((i + 1F) / totalRevisions) * 100);
					toolbar.getDisplay().asyncExec(new Runnable() {
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
				SWTCommit revision = fileRevisions[i];

				if (findInCommitId) {
					String contentId = revision.getId().name();
					if (contentId != null) {
						if (ignoreCase) {
							contentId = contentId.toLowerCase();
						}
						if (contentId.indexOf(findPattern) != -1) {
							totalMatches++;
							findResults.add(i, revision);
							notFound = false;
						}
					}
				}

				if (findInComments && notFound) {
					String comment = revision.getFullMessage();
					if (comment != null) {
						if (ignoreCase) {
							comment = comment.toLowerCase();
						}
						if (comment.indexOf(findPattern) != -1) {
							totalMatches++;
							findResults.add(i, revision);
							notFound = false;
						}
					}
				}

				if (findInAuthor && notFound) {
					String author = revision.getAuthorIdent().getName();
					if (author != null) {
						if (ignoreCase) {
							author = author.toLowerCase();
						}
						if (author.indexOf(findPattern) != -1) {
							totalMatches++;
							findResults.add(i, revision);
							notFound = false;
						}
					}
					if (notFound) {
						String email = revision.getAuthorIdent()
								.getEmailAddress();
						if (email != null) {
							if (ignoreCase) {
								email = email.toLowerCase();
							}
							if (email.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i, revision);
								notFound = false;
							}
						}
					}
				}

				if (findInCommitter && notFound) {
					String committer = revision.getCommitterIdent().getName();
					if (committer != null) {
						if (ignoreCase) {
							committer = committer.toLowerCase();
						}
						if (committer.indexOf(findPattern) != -1) {
							totalMatches++;
							findResults.add(i, revision);
							notFound = false;
						}
					}
					if (notFound) {
						String email = revision.getCommitterIdent()
								.getEmailAddress();
						if (email != null) {
							if (ignoreCase) {
								email = email.toLowerCase();
							}
							if (email.indexOf(findPattern) != -1) {
								totalMatches++;
								findResults.add(i, revision);
								notFound = false;
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
		toolbar.getDisplay().syncExec(new Runnable() {
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
