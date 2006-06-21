package org.spearce.egit.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.spearce.egit.core.project.GitProjectData;

public class Activator extends Plugin
{
    private static Activator plugin;

    public static Activator getDefault()
    {
        return plugin;
    }

    public static String getPluginId()
    {
        return getDefault().getBundle().getSymbolicName();
    }

    public static CoreException error(final String message, final Throwable thr)
    {
        return new CoreException(new Status(
            IStatus.ERROR,
            getPluginId(),
            0,
            message,
            thr));
    }

    public static void logError(final String message, final Throwable thr)
    {
        getDefault().getLog().log(
            new Status(IStatus.ERROR, getPluginId(), 0, message, thr));
    }

    private static boolean isOptionSet(final String optionId)
    {
        final String option = getPluginId() + optionId;
        final String value = Platform.getDebugOption(option);
        return value != null && value.equals("true");
    }

    public static void trace(final String what)
    {
        if (getDefault().traceVerbose)
        {
            System.out.println("[" + getPluginId() + "] " + what);
        }
    }

    private boolean traceVerbose;

    public Activator()
    {
        plugin = this;
    }

    public void start(final BundleContext context) throws Exception
    {
        super.start(context);
        traceVerbose = isOptionSet("/trace/verbose");
        GitProjectData.attachToWorkspace(true);
    }

    public void stop(final BundleContext context) throws Exception
    {
        GitProjectData.detachFromWorkspace();
        super.stop(context);
        plugin = null;
    }
}
