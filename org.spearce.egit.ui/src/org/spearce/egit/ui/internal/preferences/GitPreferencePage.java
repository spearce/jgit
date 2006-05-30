package org.spearce.egit.ui.internal.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class GitPreferencePage extends PreferencePage implements
        IWorkbenchPreferencePage {
    protected Control createContents(final Composite parent) {
        final Label b = new Label(parent, SWT.NONE);
        b.setText("Hi.  I'm an empty preference page.");
        return b;
    }

    public void init(final IWorkbench workbench) {
    }
}
