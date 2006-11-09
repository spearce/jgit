package org.spearce.jgit.lib;

/** Reader for a non-delta (just deflated) object in a pack file. */
class WholePackedObjectReader extends PackedObjectReader {
    WholePackedObjectReader(final PackReader pr, final long offset,
	    final String type, final long size) {
	super(pr, offset);
	objectType = type;
	objectSize = size;
    }
}
