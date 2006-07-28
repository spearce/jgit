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
package org.spearce.egit.core.project;

import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.spearce.jgit.lib.FileTreeEntry;
import org.spearce.jgit.lib.SymlinkTreeEntry;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeVisitor;

public class UpdateTreeFromWorkspace implements TreeVisitor
{
    private IContainer currentContainer;

    public UpdateTreeFromWorkspace(final IContainer root)
    {
        currentContainer = root;
    }

    public void startVisitTree(final Tree t) throws IOException
    {
        if (!t.isRoot())
        {
            final IResource r = currentContainer.findMember(t.getName());
            IContainer c;

            if (r == null)
            {
                c = null;
            }
            else if (r instanceof IContainer)
            {
                c = (IContainer) r;
            }
            else
            {
                c = (IContainer) r.getAdapter(IContainer.class);
            }

            if (c == null || !c.exists())
            {
                t.delete();
            }
            else
            {
                currentContainer = c;
            }
        }
    }

    public void endVisitTree(final Tree t) throws IOException
    {
        if (t.getParent() != null)
        {
            currentContainer = currentContainer.getParent();
        }
    }

    public void visitFile(final FileTreeEntry f) throws IOException
    {
        final IResource r = currentContainer.findMember(f.getName());
        if (r == null || !r.exists())
        {
            f.delete();
        }
        else if (!(r instanceof IFile) && r.getAdapter(IFile.class) == null)
        {
            f.delete();
        }
        else
        {
            f.setModified();
        }
    }

    public void visitSymlink(final SymlinkTreeEntry s) throws IOException
    {
    }
}
