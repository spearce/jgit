package org.spearce.egit.ui.internal.sharing;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.spearce.egit.ui.UIText;

class ExistingOrNewPage extends WizardPage
{
    final SharingWizard myWizard;

    ExistingOrNewPage(final SharingWizard w)
    {
        super(ExistingOrNewPage.class.getName());
        setTitle(UIText.ExistingOrNewPage_title);
        setDescription(UIText.ExistingOrNewPage_description);
        myWizard = w;
    }

    public void createControl(final Composite parent)
    {
        final Group g;
        final Button useExisting;
        final Button createNew;

        g = new Group(parent, SWT.NONE);
        g.setText(UIText.ExistingOrNewPage_groupHeader);
        g.setLayout(new RowLayout(SWT.VERTICAL));

        useExisting = new Button(g, SWT.RADIO);
        useExisting.setText(UIText.ExistingOrNewPage_useExisting);
        useExisting.addSelectionListener(new SelectionListener()
        {
            public void widgetDefaultSelected(final SelectionEvent e)
            {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e)
            {
                myWizard.setUseExisting();
            }
        });
        useExisting.setSelection(true);

        createNew = new Button(g, SWT.RADIO);
        createNew.setEnabled(myWizard.canCreateNew());
        createNew.setText(UIText.ExistingOrNewPage_createNew);
        createNew.addSelectionListener(new SelectionListener()
        {
            public void widgetDefaultSelected(final SelectionEvent e)
            {
                widgetSelected(e);
            }

            public void widgetSelected(final SelectionEvent e)
            {
                myWizard.setCreateNew();
            }
        });

        setControl(g);
    }
}
