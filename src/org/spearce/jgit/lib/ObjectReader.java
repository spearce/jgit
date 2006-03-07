package org.spearce.jgit.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.InflaterInputStream;

public class ObjectReader {
    private final ObjectId objectId;

    private final String objectType;

    private final int objectSize;

    private InflaterInputStream inflater;

    public ObjectReader(final ObjectId id, final InputStream src)
            throws IOException {
        objectId = id;
        inflater = new InflaterInputStream(src);

        final StringBuffer tempType = new StringBuffer(16);
        int tempSize = 0;
        int c;

        for (;;) {
            c = inflater.read();
            if (' ' == c) {
                break;
            }
            if (c < 'a' || c > 'z') {
                throw new CorruptObjectException("Corrupt header in "
                        + objectId);
            }
            if (tempType.length() >= 16) {
                throw new CorruptObjectException("Type header exceed limit in "
                        + objectId);
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
                throw new CorruptObjectException("Corrupt header in "
                        + objectId);
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

    public BufferedReader getBufferedReader()
            throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(inflater, "UTF-8"));
    }

    public InputStream getInputStream() {
        return inflater;
    }

    public void close() throws IOException {
        inflater.close();
        inflater = null;
    }
}
