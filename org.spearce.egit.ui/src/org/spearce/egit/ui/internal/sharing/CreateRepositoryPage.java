package org.spearce.egit.ui.internal.sharing;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.spearce.egit.ui.UIText;

class CreateRepositoryPage extends WizardPage {
    CreateRepositoryPage() {
        super(CreateRepositoryPage.class.getName());
        setTitle(UIText.CreateRepositoryPage_title);
        setDescription(UIText.CreateRepositoryPage_description);
    }

    public void createControl(final Composite parent) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(UIText.CreateRepositoryPage_mainText);
        setControl(label);
    }
}
