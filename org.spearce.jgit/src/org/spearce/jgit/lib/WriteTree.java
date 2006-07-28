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

import java.io.File;
import java.io.IOException;

import org.spearce.jgit.errors.SymlinksNotSupportedException;

public class WriteTree extends TreeVisitorWithCurrentDirectory
{
    private final ObjectWriter ow;

    public WriteTree(final File sourceDirectory, final Repository db)
    {
        super(sourceDirectory);
        ow = new ObjectWriter(db);
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        f.setId(ow.writeBlob(new File(getCurrentDirectory(), f.getName())));
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
        if (s.isModified())
        {
            throw new SymlinksNotSupportedException("Symlink \""
                + s.getFullName()
                + "\" cannot be written as the link target"
                + " cannot be read from within Java.");
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        super.endVisitTree(t);
        t.setId(ow.writeTree(t));
    }
}
