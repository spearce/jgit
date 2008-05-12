/*
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

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RefUpdate.Result;
import org.spearce.jgit.revwalk.RevWalk;

/** Update of a locally stored tracking branch. */
public class TrackingRefUpdate {
	private final RefSpec spec;

	private final RefUpdate update;

	TrackingRefUpdate(final Repository db, final RefSpec s,
			final AnyObjectId nv, final String msg) throws IOException {
		spec = s;
		update = db.updateRef(s.getDestination());
		update.setForceUpdate(spec.isForceUpdate());
		update.setNewObjectId(nv);
		update.setRefLogMessage(msg, true);
	}

	/**
	 * Get the name of the remote ref.
	 * <p>
	 * Usually this is of the form "refs/heads/master".
	 * 
	 * @return the name used within the remote repository.
	 */
	public String getRemoteName() {
		return spec.getSource();
	}

	/**
	 * Get the name of the local tracking ref.
	 * <p>
	 * Usually this is of the form "refs/remotes/origin/master".
	 * 
	 * @return the name used within this local repository.
	 */
	public String getLocalName() {
		return update.getName();
	}

	/**
	 * Get the new value the ref will be (or was) updated to.
	 * 
	 * @return new value. Null if the caller has not configured it.
	 */
	public ObjectId getNewObjectId() {
		return update.getNewObjectId();
	}

	/**
	 * The old value of the ref, prior to the update being attempted.
	 * <p>
	 * This value may differ before and after the update method. Initially it is
	 * populated with the value of the ref before the lock is taken, but the old
	 * value may change if someone else modified the ref between the time we
	 * last read it and when the ref was locked for update.
	 * 
	 * @return the value of the ref prior to the update being attempted; null if
	 *         the updated has not been attempted yet.
	 */
	public ObjectId getOldObjectId() {
		return update.getOldObjectId();
	}

	/**
	 * Get the status of this update.
	 * 
	 * @return the status of the update.
	 */
	public Result getResult() {
		return update.getResult();
	}

	void update(final RevWalk walk) throws IOException {
		update.update(walk);
	}
}
