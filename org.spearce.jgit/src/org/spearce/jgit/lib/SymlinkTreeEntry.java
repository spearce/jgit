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

public class SymlinkTreeEntry extends TreeEntry
{
    private static final long serialVersionUID = 1L;

    public SymlinkTreeEntry(
        final Tree parent,
        final ObjectId id,
        final byte[] nameUTF8)
    {
        super(parent, id, nameUTF8);
    }

    public FileMode getMode()
    {
        return FileMode.SYMLINK;
    }

    public void accept(final TreeVisitor tv, final int flags)
        throws IOException
    {
        if ((MODIFIED_ONLY & flags) == MODIFIED_ONLY && !isModified())
        {
            return;
        }

        tv.visitSymlink(this);
    }

    public String toString()
    {
        final StringBuffer r = new StringBuffer();
        r.append(ObjectId.toString(getId()));
        r.append(" S ");
        r.append(getFullName());
        return r.toString();
    }
}
