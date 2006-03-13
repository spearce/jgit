package org.spearce.egit.ui.internal.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.spearce.egit.ui.GitUIPlugin;

public class Disconnect implements IObjectActionDelegate {
    private ISelection selection;

    public void selectionChanged(final IAction act, final ISelection sel) {
        selection = sel;
    }

    public void setActivePart(final IAction act, final IWorkbenchPart part) {
    }

    public void run(final IAction act) {
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            final IStructuredSelection ssel = (IStructuredSelection) selection;
            final Iterator it = ssel.iterator();
            while (it.hasNext()) {
                final IProject project = (IProject) it.next();
                try {
                    RepositoryProvider.unmap(project);
                } catch (TeamException err) {
                    GitUIPlugin.log("Unmap project " + project.getName(), err);
                }
            }
        }
    }
}
