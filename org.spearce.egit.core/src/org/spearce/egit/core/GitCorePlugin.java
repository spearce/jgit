package org.spearce.egit.core;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

public class GitCorePlugin extends Plugin {
    private static GitCorePlugin plugin;

    public static GitCorePlugin getDefault() {
        return plugin;
    }

    public static String getPluginId() {
        return getDefault().getBundle().getSymbolicName();
    }

    public static void log(final String message) {
        getDefault().getLog().log(
                new Status(IStatus.INFO, getPluginId(), IStatus.OK, message,
                        null));
    }

    public static void log(final String message, final int severity) {
        getDefault().getLog().log(
                new Status(severity, getPluginId(), IStatus.OK, message, null));
    }

    public static void log(final String message, final Throwable thr) {
        getDefault().getLog().log(
                new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message,
                        thr));
    }

    public static boolean isTracing(final String optionId) {
        final String option = getPluginId() + "/trace/" + optionId;
        final String value = Platform.getDebugOption(option);
        return value != null && value.equals("true");
    }

    public static void trace(final String optionId, final String what) {
        if (isTracing(optionId)) {
            System.out.println(what);
        }
    }

    public static void traceVerbose(final String what) {
        trace("verbose", what);
    }

    public GitCorePlugin() {
        plugin = this;
    }

    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
    }
}
