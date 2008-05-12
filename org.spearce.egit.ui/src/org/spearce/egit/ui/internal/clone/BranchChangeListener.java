package org.spearce.egit.ui.internal.clone;

interface BranchChangeListener {
	/** Notify the receiver that the branches have changed. */
	void branchesChanged();
}
