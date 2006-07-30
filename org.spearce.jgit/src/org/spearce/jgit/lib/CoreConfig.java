package org.spearce.jgit.lib;

import java.util.zip.Deflater;

public class CoreConfig
{
    private final int compression;

    private final boolean legacyHeaders;

    protected CoreConfig(final RepositoryConfig rc)
    {
        compression = rc.getInt(
            "core",
            "compression",
            Deflater.DEFAULT_COMPRESSION);
        legacyHeaders = rc.getBoolean("core", "legacyHeaders", false);
    }

    public int getCompression()
    {
        return compression;
    }

    public boolean useLegacyHeaders()
    {
        return legacyHeaders;
    }
}
