package org.spearce.egit.ui.internal.sharing;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;
import org.spearce.egit.core.op.RegisterProviderJob;
import org.spearce.egit.ui.UIText;

public class SharingWizard extends Wizard implements IConfigurationWizard {
    private IProject project;

    public SharingWizard() {
        setWindowTitle(UIText.SharingWizard_windowTitle);
    }

    public void init(final IWorkbench workbench, final IProject p) {
        project = p;
    }

    public void addPages() {
        addPage(new ExistingRepositoryPage());
    }

    public boolean performFinish() {
        new RegisterProviderJob(project).schedule();
        return true;
    }
}
