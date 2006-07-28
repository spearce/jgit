/*
 *    Copyright 2006 Shawn Pearce <spearce@spearce.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spearce.jgit.lib;

import java.io.IOException;

public class FileTreeEntry extends TreeEntry
{
    private FileMode mode;

    public FileTreeEntry(
        final Tree parent,
        final ObjectId id,
        final byte[] nameUTF8,
        final boolean execute)
    {
        super(parent, id, nameUTF8);
        setExecutable(execute);
    }

    public FileMode getMode()
    {
        return mode;
    }

    public boolean isExecutable()
    {
        return getMode().equals(FileMode.EXECUTABLE_FILE);
    }

    public void setExecutable(final boolean execute)
    {
        mode = execute ? FileMode.EXECUTABLE_FILE : FileMode.REGULAR_FILE;
    }

    public ObjectReader openReader() throws IOException
    {
        return getRepository().openBlob(getId());
    }

    public void accept(final TreeVisitor tv, final int flags)
        throws IOException
    {
        if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
        {
            return;
        }

        tv.visitFile(this);
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(' ');
        r.append(isExecutable() ? 'X' : 'F');
        r.append(' ');
        r.append(getFullName());
        return r.toString();
    }
}
