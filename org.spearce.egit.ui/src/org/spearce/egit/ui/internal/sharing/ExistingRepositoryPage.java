package org.spearce.egit.ui.internal.sharing;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.spearce.egit.ui.UIText;

class ExistingRepositoryPage extends WizardPage {
    ExistingRepositoryPage() {
        super(ExistingRepositoryPage.class.getName());
        setTitle(UIText.ExistingRepositoryPage_title);
        setDescription(UIText.ExistingRepositoryPage_description);
    }

    public void createControl(final Composite parent) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(UIText.ExistingRepositoryPage_mainText);
        setControl(label);
    }
}
