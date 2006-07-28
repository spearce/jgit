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
package org.spearce.egit.core;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.ForceModified;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;

class GitMoveDeleteHook implements IMoveDeleteHook
{
    private static final boolean NOT_ALLOWED = true;

    private static final boolean FINISH_FOR_ME = false;

    private final GitProjectData data;

    GitMoveDeleteHook(final GitProjectData d)
    {
        Assert.isNotNull(d);
        data = d;
    }

    public boolean deleteFile(
        final IResourceTree tree,
        final IFile file,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        return delete(tree, file);
    }

    public boolean deleteFolder(
        final IResourceTree tree,
        final IFolder folder,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        // Deleting a GIT repository which is in use is a pretty bad idea. To
        // delete disconnect the team provider first.
        //
        if (data.isProtected(folder))
        {
            return cannotModifyRepository(tree);
        }
        else
        {
            return delete(tree, folder);
        }
    }

    public boolean deleteProject(
        final IResourceTree tree,
        final IProject project,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        return FINISH_FOR_ME;
    }

    public boolean moveFile(
        final IResourceTree tree,
        final IFile source,
        final IFile destination,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        return move(tree, source, destination);
    }

    public boolean moveFolder(
        final IResourceTree tree,
        final IFolder source,
        final IFolder destination,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        // Moving a GIT repository which is in use is rather complicated. So
        // we just disallow it. Instead disconnect the team provider before
        // attempting to move the repository.
        //
        if (data.isProtected(source))
        {
            return cannotModifyRepository(tree);
        }
        else
        {
            return move(tree, source, destination);
        }
    }

    public boolean moveProject(
        final IResourceTree tree,
        final IProject source,
        final IProjectDescription description,
        final int updateFlags,
        final IProgressMonitor monitor)
    {
        // Never allow moving a project as it can complicate updating our
        // project data with the new repository mappings. To move a project
        // disconnect the GIT team provider, move the project, then reconnect
        // the GIT team provider.
        //
        return NOT_ALLOWED;
    }

    private boolean delete(final IResourceTree tree, IResource r)
    {
        String s = null;
        RepositoryMapping m = null;

        while (r != null)
        {
            m = data.getRepositoryMapping(r);
            if (m != null)
            {
                break;
            }

            if (s != null)
            {
                s = r.getName() + "/" + s;
            }
            else
            {
                s = r.getName();
            }

            r = r.getParent();
        }

        if (s != null && m != null && m.getCacheTree() != null)
        {
            try
            {
                final TreeEntry e = m.getCacheTree().findMember(s);
                if (e != null)
                {
                    e.delete();
                    m.recomputeMerge();
                }
            }
            catch (IOException ioe)
            {
                tree.failed(new Status(
                    IStatus.ERROR,
                    Activator.getPluginId(),
                    0,
                    CoreText.MoveDeleteHook_operationError,
                    ioe));
                return NOT_ALLOWED;
            }
        }

        return FINISH_FOR_ME;
    }

    private boolean move(final IResourceTree tree, IResource src, IResource dst)
    {
        final String dstName = dst.getName();
        String srcPath = null;
        RepositoryMapping srcMap = null;
        String dstPath = null;
        RepositoryMapping dstMap = null;
        final TreeEntry srcEnt;
        Tree dstTree;

        while (src != null)
        {
            srcMap = data.getRepositoryMapping(src);
            if (srcMap != null)
            {
                break;
            }

            if (srcPath != null)
            {
                srcPath = src.getName() + "/" + srcPath;
            }
            else
            {
                srcPath = src.getName();
            }

            src = src.getParent();
        }

        dst = dst.getParent();
        while (dst != null)
        {
            dstMap = data.getRepositoryMapping(dst);
            if (dstMap != null)
            {
                break;
            }

            if (dstPath != null)
            {
                dstPath = dst.getName() + "/" + dstPath;
            }
            else
            {
                dstPath = dst.getName();
            }

            dst = dst.getParent();
        }

        if (srcPath == null || srcMap == null || srcMap.getCacheTree() == null)
        {
            return FINISH_FOR_ME;
        }

        try
        {
            srcEnt = srcMap.getCacheTree().findMember(srcPath);
            if (srcEnt == null)
            {
                return FINISH_FOR_ME;
            }

            // If it moved outside of our mapped area then simply delete it.
            //
            if (dstMap == null || dstMap.getCacheTree() == null)
            {
                srcEnt.delete();
                return FINISH_FOR_ME;
            }

            // Locate the target tree.
            //
            if (dstPath == null)
            {
                dstTree = dstMap.getCacheTree();
            }
            else
            {
                final TreeEntry e = dstMap.getCacheTree().findMember(dstPath);
                if (e == null)
                {
                    dstTree = dstMap.getCacheTree().addTree(dstPath);
                }
                else if (e instanceof Tree)
                {
                    dstTree = (Tree) e;
                }
                else
                {
                    // What the heck? Assume Eclipse meant for us to replace
                    // the item into here instead.
                    //
                    e.delete();
                    dstTree = dstMap.getCacheTree().addTree(dstPath);
                }
            }

            // What? Something already exists at the destination? Assume
            // Eclipse meant for us to replace the item.
            //
            final TreeEntry existing = dstTree.findMember(dstName);
            if (existing != null)
            {
                existing.delete();
            }

            // Delete the entry from the source tree before we do anything.
            //
            final Tree srcTree = srcEnt.getParent();
            srcEnt.delete();

            // If the repository differs then we shouldn't assume the
            // objects exist in the new repository so force everything to
            // appear modified so we'll create any objects if necessary.
            //
            if (srcMap.getRepository() != dstMap.getRepository())
            {
                try
                {
                    srcEnt.accept(new ForceModified());
                }
                catch (IOException ioe)
                {
                    srcTree.addEntry(srcEnt);
                    throw ioe;
                }
            }

            srcEnt.rename(dstName);
            dstTree.addEntry(srcEnt);

            if (srcMap == dstMap)
            {
                srcMap.recomputeMerge();
            }
            else
            {
                srcMap.recomputeMerge();
                dstMap.recomputeMerge();
            }

            return FINISH_FOR_ME;
        }
        catch (IOException ioe)
        {
            tree.failed(new Status(
                IStatus.ERROR,
                Activator.getPluginId(),
                0,
                CoreText.MoveDeleteHook_operationError,
                ioe));
            return NOT_ALLOWED;
        }
    }

    private boolean cannotModifyRepository(final IResourceTree tree)
    {
        tree.failed(new Status(
            IStatus.ERROR,
            Activator.getPluginId(),
            0,
            CoreText.MoveDeleteHook_cannotModifyFolder,
            null));
        return NOT_ALLOWED;
    }
}