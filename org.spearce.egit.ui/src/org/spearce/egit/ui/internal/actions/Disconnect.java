package org.spearce.egit.ui.internal.actions;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.spearce.egit.core.op.DisconnectProviderOperation;

public class Disconnect extends AbstractOperationAction {
    protected IWorkspaceRunnable createOperation() {
        final List sel = getSelection();
        return sel.isEmpty() ? null : new DisconnectProviderOperation(sel);
    }
}
