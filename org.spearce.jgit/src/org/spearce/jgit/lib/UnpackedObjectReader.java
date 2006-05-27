package org.spearce.jgit.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public class UnpackedObjectReader extends ObjectReader {
    private static final int MAX_TYPE_LEN = 16;

    private final String objectType;

    private final long objectSize;

    private InflaterInputStream inflater;

    public UnpackedObjectReader(final ObjectId id, final InputStream src)
            throws IOException {
        final StringBuffer tempType = new StringBuffer(MAX_TYPE_LEN);
        int tempSize = 0;

        setId(id);
        inflater = new InflaterInputStream(src);

        for (;;) {
            int c = inflater.read();
            if (' ' == c) {
                break;
            }
            if (c < 'a' || c > 'z' || tempType.length() >= 16) {
                throw new CorruptObjectException(id, "bad type in header");
            }
            tempType.append((char) c);
        }
        objectType = tempType.toString();

        for (;;) {
            int c = inflater.read();
            if (0 == c) {
                break;
            }
            if (c < '0' || c > '9') {
                throw new CorruptObjectException(id, "bad length in header");
            }
            tempSize *= 10;
            tempSize += c - '0';
        }
        objectSize = tempSize;
    }

    public String getType() {
        return objectType;
    }

    public long getSize() {
        return objectSize;
    }

    public InputStream getInputStream() {
        if (inflater == null) {
            throw new IllegalStateException("Already closed.");
        }
        return inflater;
    }

    public void close() throws IOException {
        if (inflater != null) {
            inflater.close();
            inflater = null;
        }
    }
}
