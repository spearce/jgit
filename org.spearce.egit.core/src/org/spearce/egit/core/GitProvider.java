/*
 *  Copyright (C) 2006  Shawn Pearce <spearce@spearce.org>
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.core;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.project.GitProjectData;

public class GitProvider extends RepositoryProvider
{
    private GitProjectData data;

    private GitMoveDeleteHook hook;

    public String getID()
    {
        return getClass().getName();
    }

    public void configureProject() throws CoreException
    {
        getData().markTeamPrivateResources();
    }

    public void deconfigure() throws CoreException
    {
        GitProjectData.delete(getProject());
    }

    public boolean canHandleLinkedResources()
    {
        return true;
    }

    public synchronized IMoveDeleteHook getMoveDeleteHook()
    {
        if (hook == null)
        {
            hook = new GitMoveDeleteHook(getData());
        }
        return hook;
    }

    private synchronized GitProjectData getData()
    {
        if (data == null)
        {
            data = GitProjectData.get(getProject());
        }
        return data;
    }
}
