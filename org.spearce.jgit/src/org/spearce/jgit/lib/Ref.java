package org.spearce.jgit.lib;

import java.io.File;

public class Ref
{
    private final File file;

    private ObjectId objectId;

    public Ref(final File f, final ObjectId id)
    {
        file = f;
        objectId = id;
    }

    public File getFile()
    {
        return file;
    }

    public ObjectId getObjectId()
    {
        return objectId;
    }

    public String toString()
    {
        return "Ref[" + file + "=" + getObjectId() + "]";
    }
}
