package org.spearce.jgit.lib;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Constants {
    private static final String HASH_FUNCTION = "SHA-1";

    public static final int OBJECT_ID_LENGTH = 20;

    public static final String TYPE_COMMIT = "commit";

    public static final String TYPE_BLOB = "blob";

    public static final String TYPE_TREE = "tree";

    public static final String TYPE_TAG = "tag";

    public static MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance(HASH_FUNCTION);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Hash function " + HASH_FUNCTION
                    + " not available.", nsae);
        }
    }

    private Constants() {
    }
}
