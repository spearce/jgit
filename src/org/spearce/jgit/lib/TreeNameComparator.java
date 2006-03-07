package org.spearce.jgit.lib;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;

public class TreeNameComparator implements Comparator {

    public int compare(final Object arg0, final Object arg1) {
        final byte[] a = toUTF8(arg0);
        final byte[] b = toUTF8(arg1);

        for (int k = 0; k < a.length && k < b.length; k++) {
            final int ak = a[k] & 0xff;
            final int bk = b[k] & 0xff;
            if (ak < bk) {
                return -1;
            } else if (ak > bk) {
                return 1;
            }
        }

        if (a.length < b.length) {
            return -1;
        } else if (a.length == b.length) {
            return 0;
        } else {
            return 1;
        }
    }

    private static byte[] toUTF8(final Object a) {
        try {
            if (a instanceof TreeEntry) {
                return ((TreeEntry) a).getNameUTF8();
            }
            if (a instanceof String) {
                return ((String) a).getBytes("UTF-8");
            }
            throw new IllegalArgumentException("Not a TreeEntry or String: "
                    + a);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("JVM does not support UTF-8."
                    + "  It should have.", uee);
        }
    }
}
