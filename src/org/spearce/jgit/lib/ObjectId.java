package org.spearce.jgit.lib;

public class ObjectId {
    private static final ObjectId ZEROID;

    private static final String ZEROID_STR;

    static {
        byte[] x = new byte[20];
        ZEROID = new ObjectId(x);
        ZEROID_STR = ZEROID.toString();
    }

    public static final boolean isId(final String id) {
        if (id.length() != 40) {
            return false;
        }
        for (int k = 0; k < 40; k++) {
            final char c = id.charAt(k);
            if ('0' <= c && c <= '9') {
                continue;
            } else if ('a' <= c && c <= 'f') {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public static String toString(final ObjectId i) {
        return i != null ? i.toString() : ZEROID_STR;
    }

    private final byte[] id;

    public ObjectId(final String i) {
        id = new byte[20];
        for (int j = 0, k = 0; k < 20; k++) {
            final char c1 = i.charAt(j++);
            final char c2 = i.charAt(j++);
            int b;

            if ('0' <= c1 && c1 <= '9') {
                b = c1 - '0';
            } else {
                b = c1 - 'a' + 10;
            }
            b <<= 4;
            if ('0' <= c2 && c2 <= '9') {
                b |= c2 - '0';
            } else {
                b |= c2 - 'a' + 10;
            }
            id[k] = (byte) b;
        }
    }

    public ObjectId(final byte[] i) {
        id = i;
    }

    public int hashCode() {
        int r = 0;
        for (int k = 0; k < 20; k++) {
            r *= 31;
            r += id[k];
        }
        return r;
    }

    public boolean equals(final Object o) {
        if (o instanceof ObjectId) {
            final byte[] o_id = ((ObjectId) o).id;
            for (int k = 0; k < 20; k++) {
                if (id[k] != o_id[k]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String toString() {
        final StringBuffer r = new StringBuffer(40);
        for (int k = 0; k < 20; k++) {
            final int b = id[k];
            final int b1 = (b >> 4) & 0xf;
            final int b2 = b & 0xf;
            r.append(b1 < 10 ? (char) ('0' + b1) : (char) ('a' + b1 - 10));
            r.append(b2 < 10 ? (char) ('0' + b2) : (char) ('a' + b2 - 10));
        }
        return r.toString();
    }
}
