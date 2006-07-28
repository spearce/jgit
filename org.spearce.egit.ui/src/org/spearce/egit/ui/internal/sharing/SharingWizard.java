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
package org.spearce.egit.ui.internal.sharing;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.ui.Activator;
import org.spearce.egit.ui.UIText;
import org.spearce.egit.ui.internal.decorators.GitResourceDecorator;

public class SharingWizard extends Wizard implements IConfigurationWizard
{
    private IProject project;

    private boolean create;

    private File newGitDir;

    public SharingWizard()
    {
        setWindowTitle(UIText.SharingWizard_windowTitle);
        setNeedsProgressMonitor(true);
    }

    public void init(final IWorkbench workbench, final IProject p)
    {
        project = p;
        newGitDir = new File(project.getLocation().toFile(), ".git");
    }

    public void addPages()
    {
        addPage(new ExistingOrNewPage(this));
    }

    boolean canCreateNew()
    {
        return !newGitDir.exists();
    }

    void setCreateNew()
    {
        if (canCreateNew())
        {
            create = true;
        }
    }

    void setUseExisting()
    {
        create = false;
    }

    public boolean performFinish()
    {
        final ConnectProviderOperation op = new ConnectProviderOperation(
            project,
            create ? newGitDir : null);
        try
        {
            getContainer().run(true, false, new IRunnableWithProgress()
            {
                public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException
                {
                    try
                    {
                        op.run(monitor);
                    }
                    catch (CoreException ce)
                    {
                        throw new InvocationTargetException(ce);
                    }
                }
            });

            GitResourceDecorator.refresh();
            return true;
        }
        catch (Throwable e)
        {
            if (e instanceof InvocationTargetException)
            {
                e = e.getCause();
            }
            final IStatus status;
            if (e instanceof CoreException)
            {
                status = ((CoreException) e).getStatus();
                e = status.getException();
            }
            else
            {
                status = new Status(
                    IStatus.ERROR,
                    Activator.getPluginId(),
                    1,
                    UIText.SharingWizard_failed,
                    e);
            }
            Activator.logError(UIText.SharingWizard_failed, e);
            ErrorDialog.openError(
                getContainer().getShell(),
                getWindowTitle(),
                UIText.SharingWizard_failed,
                status,
                status.getSeverity());
            return false;
        }
    }
}
