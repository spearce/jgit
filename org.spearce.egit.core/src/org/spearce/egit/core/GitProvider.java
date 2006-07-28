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
