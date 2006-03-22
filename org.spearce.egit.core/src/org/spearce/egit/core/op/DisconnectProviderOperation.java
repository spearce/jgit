package org.spearce.egit.core.op;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.GitCorePlugin;

public class DisconnectProviderOperation implements IWorkspaceRunnable {
    private final Collection projectList;

    public DisconnectProviderOperation(final Collection projs) {
        projectList = projs;
    }

    public void run(IProgressMonitor m) throws CoreException {
        if (m == null) {
            m = new NullProgressMonitor();
        }

        m.beginTask(CoreText.DisconnectProviderOperation_disconnecting,
                projectList.size() * 200);
        try {
            final Iterator i = projectList.iterator();
            while (i.hasNext()) {
                final Object obj = i.next();
                if (obj instanceof IProject) {
                    final IProject project = (IProject) obj;

                    unmarkTeamPrivate(project);
                    RepositoryProvider.unmap(project);
                    m.worked(100);

                    project.refreshLocal(IResource.DEPTH_INFINITE,
                            new SubProgressMonitor(m, 100));
                } else {
                    m.worked(200);
                }
            }
        } finally {
            m.done();
        }
    }

    private void unmarkTeamPrivate(final IContainer parent)
            throws CoreException {
        final IResource[] children = parent
                .members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
        if (children != null) {
            for (int k = 0; k < children.length; k++) {
                if (children[k] instanceof IContainer) {
                    unmarkTeamPrivate((IContainer) children[k]);
                }

                if (children[k].isTeamPrivateMember()) {
                    GitCorePlugin.traceVerbose("GitProjectData: -teamPrivate: "
                            + children[k]);
                    children[k].setTeamPrivateMember(false);
                }
            }
        }
    }
}
