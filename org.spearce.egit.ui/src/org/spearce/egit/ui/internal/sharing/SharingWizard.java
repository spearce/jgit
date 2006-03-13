package org.spearce.egit.ui.internal.sharing;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.core.CoreText;
import org.spearce.egit.core.op.ConnectProviderOperation;
import org.spearce.egit.ui.GitUIPlugin;
import org.spearce.egit.ui.UIText;

public class SharingWizard extends Wizard implements IConfigurationWizard {
    private IProject project;

    private File gitdir;

    private boolean create;

    public SharingWizard() {
        setWindowTitle(UIText.SharingWizard_windowTitle);
    }

    public void init(final IWorkbench workbench, final IProject p) {
        project = p;
        gitdir = new File(new File(project.getLocation().toOSString()), ".git");
    }

    public void addPages() {
        if (gitdir.isDirectory()) {
            create = false;
            addPage(new ExistingRepositoryPage());
        } else {
            create = true;
            addPage(new CreateRepositoryPage());
        }
    }

    public boolean performFinish() {
        final ConnectProviderOperation op;
        op = new ConnectProviderOperation(project, gitdir, create);
        try {
            getContainer().run(false, false, new IRunnableWithProgress() {
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException {
                    try {
                        ResourcesPlugin.getWorkspace().run(op, monitor);
                    } catch (CoreException ce) {
                        throw new InvocationTargetException(ce);
                    }
                }
            });
            return true;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = e.getCause();
            }
            final IStatus status;
            if (e instanceof CoreException) {
                status = ((CoreException) e).getStatus();
                e = status.getException();
            } else {
                status = new Status(IStatus.ERROR, GitUIPlugin.getPluginId(),
                        1, CoreText.ConnectProviderOperation_failed, e);
            }
            GitUIPlugin.log(CoreText.ConnectProviderOperation_failed, e);
            ErrorDialog.openError(getContainer().getShell(), getWindowTitle(),
                    CoreText.ConnectProviderOperation_failed, status, status
                            .getSeverity());
            return false;
        }
    }
}
