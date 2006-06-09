package org.spearce.egit.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.spearce.egit.core.project.GitProjectData;

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
        return FINISH_FOR_ME;
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
            return FINISH_FOR_ME;
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
        return FINISH_FOR_ME;
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
            return FINISH_FOR_ME;
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