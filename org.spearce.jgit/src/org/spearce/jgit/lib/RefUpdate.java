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
package org.spearce.jgit.lib;

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevObject;
import org.spearce.jgit.revwalk.RevWalk;

/**
 * Updates any locally stored ref.
 */
public class RefUpdate {
	/** Status of an update request. */
	public static enum Result {
		/** The ref update has not been attempted by the caller. */
		NOT_ATTEMPTED,

		/**
		 * The ref could not be locked for update.
		 * <p>
		 * This is generally a transient failure and is usually caused by
		 * another process trying to access the ref at the same time as this
		 * process was trying to update it. It is possible a future operation
		 * will be successful.
		 */
		LOCK_FAILURE,

		/**
		 * Same value already stored.
		 * <p>
		 * Both the old value and the new value are identical. No change was
		 * necessary.
		 */
		NO_CHANGE,

		/**
		 * The ref was created locally.
		 * <p>
		 * The ref did not exist when the update started, but it was created
		 * successfully with the new value.
		 */
		NEW,

		/**
		 * The ref had to be forcefully updated.
		 * <p>
		 * The ref already existed but its old value was not fully merged into
		 * the new value. The configuration permitted a forced update to take
		 * place, so ref now contains the new value. History associated with the
		 * objects not merged may no longer be reachable.
		 */
		FORCED,

		/**
		 * The ref was updated in a fast-forward way.
		 * <p>
		 * The tracking ref already existed and its old value was fully merged
		 * into the new value. No history was made unreachable.
		 */
		FAST_FORWARD,

		/**
		 * Not a fast-forward and not stored.
		 * <p>
		 * The tracking ref already existed but its old value was not fully
		 * merged into the new value. The configuration did not allow a forced
		 * update to take place, so ref still contains the old value. No
		 * previous history was lost.
		 */
		REJECTED
	}

	/** Repository the ref is stored in. */
	private final Repository db;

	/** Name of the ref. */
	private final String name;

	/** Location of the loose file holding the value of this ref. */
	private final File looseFile;

	/** New value the caller wants this ref to have. */
	private ObjectId newValue;

	/** Does this specification ask for forced updated (rewind/reset)? */
	private boolean force;

	/** Message the caller wants included in the reflog. */
	private String refLogMessage;

	/** Should the Result value be appended to {@link #refLogMessage}. */
	private boolean refLogIncludeResult;

	/** Old value of the ref, obtained after we lock it. */
	private ObjectId oldValue;

	/** Result of the update operation. */
	private Result result = Result.NOT_ATTEMPTED;

	RefUpdate(final Repository r, final Ref ref, final File f) {
		db = r;
		name = ref.getName();
		oldValue = ref.getObjectId();
		looseFile = f;
	}

	/**
	 * Get the name of the ref this update will operate on.
	 * 
	 * @return name of this ref.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the new value the ref will be (or was) updated to.
	 * 
	 * @return new value. Null if the caller has not configured it.
	 */
	public ObjectId getNewObjectId() {
		return newValue;
	}

	/**
	 * Set the new value the ref will update to.
	 * 
	 * @param id
	 *            the new value.
	 */
	public void setNewObjectId(final AnyObjectId id) {
		newValue = id.toObjectId();
	}

	/**
	 * Check if this update wants to forcefully change the ref.
	 * 
	 * @return true if this update should ignore merge tests.
	 */
	public boolean isForceUpdate() {
		return force;
	}

	/**
	 * Set if this update wants to forcefully change the ref.
	 * 
	 * @param b
	 *            true if this update should ignore merge tests.
	 */
	public void setForceUpdate(final boolean b) {
		force = b;
	}

	/**
	 * Get the message to include in the reflog.
	 * 
	 * @return message the caller wants to include in the reflog.
	 */
	public String getRefLogMessage() {
		return refLogMessage;
	}

	/**
	 * Set the message to include in the reflog.
	 * 
	 * @param msg
	 *            the message to describe this change.
	 * @param appendStatus
	 *            true if the status of the ref change (fast-forward or
	 *            forced-update) should be appended to the user supplied
	 *            message.
	 */
	public void setRefLogMessage(final String msg, final boolean appendStatus) {
		refLogMessage = msg;
		refLogIncludeResult = appendStatus;
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
		return oldValue;
	}

	/**
	 * Get the status of this update.
	 * <p>
	 * The same value that was previously returned from an update method.
	 * 
	 * @return the status of the update.
	 */
	public Result getResult() {
		return result;
	}

	private void requireCanDoUpdate() {
		if (newValue == null)
			throw new IllegalStateException("A NewObjectId is required.");
	}

	/**
	 * Force the ref to take the new value.
	 * <p>
	 * No merge tests are performed, so the value of {@link #isForceUpdate()}
	 * will not be honored.
	 * 
	 * @return the result status of the update.
	 * @throws IOException
	 *             an unexpected IO error occurred while writing changes.
	 */
	public Result forceUpdate() throws IOException {
		requireCanDoUpdate();
		return result = forceUpdateImpl();
	}

	private Result forceUpdateImpl() throws IOException {
		final LockFile lock;

		lock = new LockFile(looseFile);
		if (!lock.lock())
			return Result.LOCK_FAILURE;
		try {
			oldValue = lock.readCurrentObjectId();
			if (oldValue == null)
				return store(lock, Result.NEW);
			if (oldValue.equals(newValue))
				return Result.NO_CHANGE;
			return store(lock, Result.FORCED);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Gracefully update the ref to the new value.
	 * <p>
	 * Merge test will be performed according to {@link #isForceUpdate()}.
	 * <p>
	 * This is the same as:
	 * 
	 * <pre>
	 * return update(new RevWalk(repository));
	 * </pre>
	 * 
	 * @return the result status of the update.
	 * @throws IOException
	 *             an unexpected IO error occurred while writing changes.
	 */
	public Result update() throws IOException {
		return update(new RevWalk(db));
	}

	/**
	 * Gracefully update the ref to the new value.
	 * <p>
	 * Merge test will be performed according to {@link #isForceUpdate()}.
	 * 
	 * @param walk
	 *            a RevWalk instance this update command can borrow to perform
	 *            the merge test. The walk will be reset to perform the test.
	 * @return the result status of the update.
	 * @throws IOException
	 *             an unexpected IO error occurred while writing changes.
	 */
	public Result update(final RevWalk walk) throws IOException {
		requireCanDoUpdate();
		return result = updateImpl(walk);
	}

	private Result updateImpl(final RevWalk walk) throws IOException {
		final LockFile lock;
		RevObject newObj;
		RevObject oldObj;

		lock = new LockFile(looseFile);
		if (!lock.lock())
			return Result.LOCK_FAILURE;
		try {
			oldValue = lock.readCurrentObjectId();
			if (oldValue == null)
				return store(lock, Result.NEW);

			newObj = walk.parseAny(newValue);
			oldObj = walk.parseAny(oldValue);
			if (newObj == oldObj)
				return Result.NO_CHANGE;

			if (newObj instanceof RevCommit && oldObj instanceof RevCommit) {
				if (walk.isMergedInto((RevCommit) oldObj, (RevCommit) newObj))
					return store(lock, Result.FAST_FORWARD);
				if (isForceUpdate())
					return store(lock, Result.FORCED);
				return Result.REJECTED;
			}

			if (isForceUpdate())
				return store(lock, Result.FORCED);
			return Result.REJECTED;
		} finally {
			lock.unlock();
		}
	}

	private Result store(final LockFile lock, final Result status)
			throws IOException {
		lock.write(newValue);
		String msg = getRefLogMessage();
		if (msg != null && refLogIncludeResult) {
			if (status == Result.FORCED)
				msg += ": forced-update";
			else if (status == Result.FAST_FORWARD)
				msg += ": fast forward";
			else if (status == Result.NEW)
				msg += ": created";
		}
		RefLogWriter.writeReflog(db, oldValue, newValue, msg, getName());
		if (!lock.commit())
			return Result.LOCK_FAILURE;
		return status;
	}
}
