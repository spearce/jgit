package org.spearce.egit.ui.internal.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.spearce.egit.core.project.GitProjectData;
import org.spearce.egit.ui.UIIcons;
import org.spearce.jgit.lib.Repository;

public class GitResourceDecorator extends LabelProvider
    implements
        ILightweightLabelDecorator
{
    private static IResource toIResource(final Object e)
    {
        if (e instanceof IResource)
        {
            return (IResource) e;
        }
        else if (e instanceof IAdaptable)
        {
            final Object c = ((IAdaptable) e).getAdapter(IResource.class);
            if (c instanceof IResource)
            {
                return (IResource) c;
            }
        }
        return null;
    }

    public void decorate(final Object element, final IDecoration decoration)
    {
        final IResource rsrc = toIResource(element);
        if (rsrc == null)
        {
            return;
        }

        final GitProjectData d = GitProjectData.getDataFor(rsrc.getProject());
        if (d == null)
        {
            return;
        }

        final Repository ownRepo = d.getOwnRepository(rsrc);
        if (ownRepo != null)
        {
            decoration.addSuffix(" [GIT]");
            decoration.addOverlay(UIIcons.OVR_SHARED);
            return;
        }

        if (true)
        {
            decoration.addOverlay(UIIcons.OVR_PENDING_ADD);
        }

        if (true)
        {
            decoration.addPrefix(">");
        }
    }
}
