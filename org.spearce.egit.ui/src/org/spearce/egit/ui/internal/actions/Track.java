package org.spearce.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jface.action.IAction;
import org.spearce.egit.core.op.TrackOperation;
import org.spearce.egit.ui.internal.decorators.GitResourceDecorator;

public class Track extends AbstractOperationAction
{
    protected IWorkspaceRunnable createOperation(
        final IAction act,
        final List sel)
    {
        return sel.isEmpty() ? null : new TrackOperation(sel);
    }

    protected void postOperation()
    {
        GitResourceDecorator.refresh();
    }
}
