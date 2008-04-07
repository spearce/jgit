/*
 *  Copyright (C) 2007  Robin Rosenberg <robin.rosenberg@dewire.com>
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

/**
 * Important state of the repository that affects what can and cannot bed
 * done. This is things like unhandles conflicted merges and unfinished rebase.
 */
public enum RepositoryState {
	/**
	 * A safe state for working normally
	 * */
	SAFE {
		public boolean canCheckout() { return true; }
		public boolean canResetHead() { return true; }
		public boolean canCommit() { return true; }
		public String getDescription() { return "Normal"; }
	},

	/** An unfinished merge. Must resole or reset before continuing normally
	 */
	MERGING {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return false; }
		public String getDescription() { return "Conflicts"; }
	},

	/**
	 * An unfinished rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return "Rebase/Apply mailbox"; }
	},

	/**
	 * An unfinished rebase with merge. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_MERGE {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return "Rebase w/merge"; }
	},

	/**
	 * An unfinished interactive rebase. Must resolve, skip or abort before normal work can take place
	 */
	REBASING_INTERACTIVE {
		public boolean canCheckout() { return false; }
		public boolean canResetHead() { return false; }
		public boolean canCommit() { return true; }
		public String getDescription() { return "Rebase interactive"; }
	},

	/**
	 * Bisecting being done. Normal work may continue but is discouraged
	 */
	BISECTING {
		/* Changing head is a normal operation when bisecting */
		public boolean canCheckout() { return true; }

		/* Do not reset, checkout instead */
		public boolean canResetHead() { return false; }

		/* Actually it may make sense, but for now we err on the side of caution */
		public boolean canCommit() { return false; }

		public String getDescription() { return "Bisecting"; }
	};

	/**
	 * @return true if changing HEAD is sane.
	 */
	public abstract boolean canCheckout();

	/**
	 * @return true if we can commit
	 */
	public abstract boolean canCommit();

	/**
	 * @return true if reset to another HEAD is considered SAFE
	 */
	public abstract boolean canResetHead();

	/**
	 * @return a human readable description of the state.
	 */
	public abstract String getDescription();
}
