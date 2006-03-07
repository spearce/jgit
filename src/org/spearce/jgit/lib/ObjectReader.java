package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.InflaterInputStream;

public class ObjectReader {
    private static final int TYPESZ = 16;

    private final ObjectId objectId;

    private final String objectType;

    private final int objectSize;

    private InflaterInputStream inflater;

    public ObjectReader(final ObjectId id, final InputStream src)
            throws IOException {
        objectId = id;
        inflater = new InflaterInputStream(src);

        final StringBuffer tempType = new StringBuffer(TYPESZ);
        int tempSize = 0;
        int c;

        for (;;) {
            c = inflater.read();
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
            c = inflater.read();
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

    public ObjectId getId() {
        return objectId;
    }

    public String getType() {
        return objectType;
    }

    public int getSize() {
        return objectSize;
    }

    public InputStream getInputStream() {
        if (inflater == null) {
            throw new IllegalStateException("Already closed.");
        }
        return inflater;
    }

    public BufferedReader getBufferedReader()
            throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(getInputStream(),
                "UTF-8"));
    }

    public void close() throws IOException {
        if (inflater != null) {
            inflater.close();
            inflater = null;
        }
    }
}
