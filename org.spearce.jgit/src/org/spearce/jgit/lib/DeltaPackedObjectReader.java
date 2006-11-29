package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.InputStream;

/** Reader for a deltafied object stored in a pack file. */
abstract class DeltaPackedObjectReader extends PackedObjectReader {
    DeltaPackedObjectReader(final PackFile pr, final long offset) {
	super(pr, offset);
	objectSize = -1;
    }

    public String getType() throws IOException {
	if (objectType == null) {
	    final ObjectReader b = baseReader();
	    try {
		objectType = b.getType();
	    } finally {
		b.close();
	    }
	}
	return objectType;
    }

    public long getSize() throws IOException {
	if (objectSize == -1) {
	    final PatchDeltaStream p;
	    p = new PatchDeltaStream(packStream(), null);
	    objectSize = p.getResultLength();
	    p.close();
	}
	return objectSize;
    }

    public InputStream getInputStream() throws IOException {
	final ObjectReader b = baseReader();
	final PatchDeltaStream p = new PatchDeltaStream(packStream(), b);
	if (objectSize == -1) {
	    objectSize = p.getResultLength();
	}
	if (objectType == null) {
	    objectType = b.getType();
	}
	return p;
    }

    protected abstract ObjectReader baseReader() throws IOException;
}
