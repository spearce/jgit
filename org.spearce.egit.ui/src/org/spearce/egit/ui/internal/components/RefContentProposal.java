/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import java.io.IOException;
import java.sql.Blob;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.spearce.egit.ui.Activator;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tag;
import org.spearce.jgit.lib.Tree;

/**
 * Content proposal class for refs names, specifically Ref objects - name with
 * optionally associated object id. This class can be used for Eclipse field
 * assist as content proposal.
 * <p>
 * Content of this proposal is simply a ref name, but description and labels
 * tries to be smarter - showing easier to read label for user (stripping
 * prefixes) and information about pointed object if it exists locally.
 */
public class RefContentProposal implements IContentProposal {
	private static final String PREFIXES[] = new String[] { Constants.R_HEADS,
			Constants.R_REMOTES, Constants.R_TAGS };

	private static final String PREFIXES_DESCRIPTIONS[] = new String[] {
			" [branch]", " [tracking branch]", " [tag]" };

	private static void appendObjectSummary(final StringBuilder sb,
			final String type, final PersonIdent author, final String message) {
		sb.append(type + " by ");
		sb.append(author.getName());
		sb.append("\n");
		sb.append(author.getWhen());
		sb.append("\n\n");
		final int newLine = message.indexOf('\n');
		final int last = (newLine != -1 ? newLine : message.length());
		sb.append(message.substring(0, last));
	}

	private final Repository db;

	private final String refName;

	private final ObjectId objectId;

	/**
	 * Create content proposal for specified ref.
	 *
	 * @param repo
	 *            repository for accessing information about objects. Could be a
	 *            local repository even for remote objects.
	 * @param ref
	 *            ref being a content proposal. May have null or locally
	 *            non-existent object id.
	 */
	public RefContentProposal(final Repository repo, final Ref ref) {
		this(repo, ref.getName(), ref.getObjectId());
	}

	/**
	 * Create content proposal for specified ref name and object id.
	 *
	 * @param repo
	 *            repository for accessing information about objects. Could be a
	 *            local repository even for remote objects.
	 * @param refName
	 *            ref name being a content proposal.
	 * @param objectId
	 *            object being pointed by this ref name. May be null or locally
	 *            non-existent object.
	 */
	public RefContentProposal(final Repository repo, final String refName,
			final ObjectId objectId) {
		this.db = repo;
		this.refName = refName;
		this.objectId = objectId;
	}

	public String getContent() {
		return refName;
	}

	public int getCursorPosition() {
		return refName.length();
	}

	public String getDescription() {
		if (objectId == null)
			return null;
		final Object obj;
		try {
			obj = db.mapObject(objectId, refName);
		} catch (IOException e) {
			Activator.logError("Unable to read object " + objectId
					+ " for content proposal assistance", e);
			return null;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append(refName);
		sb.append('\n');
		sb.append(objectId.abbreviate(db).name());
		sb.append(" - ");
		if (obj instanceof Commit) {
			final Commit c = ((Commit) obj);
			appendObjectSummary(sb, "commit", c.getAuthor(), c.getMessage());
		} else if (obj instanceof Tag) {
			final Tag t = ((Tag) obj);
			appendObjectSummary(sb, "tag", t.getAuthor(), t.getMessage());
		} else if (obj instanceof Tree) {
			sb.append("tree");
		} else if (obj instanceof Blob) {
			sb.append("blob");
		} else
			sb.append("locally unknown object");
		return sb.toString();
	}

	public String getLabel() {
		for (int i = 0; i < PREFIXES.length; i++)
			if (refName.startsWith(PREFIXES[i]))
				return refName.substring(PREFIXES[i].length())
						+ PREFIXES_DESCRIPTIONS[i];
		return refName;

	}
}
