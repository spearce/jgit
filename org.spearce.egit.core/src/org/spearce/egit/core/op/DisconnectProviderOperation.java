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
import org.spearce.egit.core.Activator;

public class DisconnectProviderOperation implements IWorkspaceRunnable
{
    private final Collection projectList;

    public DisconnectProviderOperation(final Collection projs)
    {
        projectList = projs;
    }

    public void run(IProgressMonitor m) throws CoreException
    {
        if (m == null)
        {
            m = new NullProgressMonitor();
        }

        m.beginTask(
            CoreText.DisconnectProviderOperation_disconnecting,
            projectList.size() * 200);
        try
        {
            final Iterator i = projectList.iterator();
            while (i.hasNext())
            {
                final Object obj = i.next();
                if (obj instanceof IProject)
                {
                    final IProject p = (IProject) obj;

                    Activator.trace("disconnect " + p.getName());
                    unmarkTeamPrivate(p);
                    RepositoryProvider.unmap(p);
                    m.worked(100);

                    p.refreshLocal(
                        IResource.DEPTH_INFINITE,
                        new SubProgressMonitor(m, 100));
                }
                else
                {
                    m.worked(200);
                }
            }
        }
        finally
        {
            m.done();
        }
    }

    private void unmarkTeamPrivate(final IContainer p) throws CoreException
    {
        final IResource[] c;
        c = p.members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
        if (c != null)
        {
            for (int k = 0; k < c.length; k++)
            {
                if (c[k] instanceof IContainer)
                {
                    unmarkTeamPrivate((IContainer) c[k]);
                }
                if (c[k].isTeamPrivateMember())
                {
                    Activator.trace("notTeamPrivate " + c[k]);
                    c[k].setTeamPrivateMember(false);
                }
            }
        }
    }
}
