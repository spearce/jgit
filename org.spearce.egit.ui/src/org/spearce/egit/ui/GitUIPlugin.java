package org.spearce.egit.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class GitUIPlugin extends AbstractUIPlugin {
    private static GitUIPlugin plugin;

    public static GitUIPlugin getDefault() {
        return plugin;
    }

    public static String getPluginId() {
        return getDefault().getBundle().getSymbolicName();
    }

    public static void log(final String message) {
        System.out.println("log:" + message);
        getDefault().getLog().log(
                new Status(IStatus.INFO, getPluginId(), IStatus.OK, message,
                        null));
    }

    public static void log(final String message, final int severity) {
        System.out.println("log:" + message);
        getDefault().getLog().log(
                new Status(severity, getPluginId(), IStatus.OK, message, null));
    }

    public static void log(final String message, final Throwable thr) {
        System.out.println("log:" + message);
        thr.printStackTrace();
        getDefault().getLog().log(
                new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message,
                        thr));
    }

    public GitUIPlugin() {
        plugin = this;
    }

    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
    }
}
