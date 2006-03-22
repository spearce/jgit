package org.spearce.egit.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.spearce.jgit.lib.Repository;

public class GitProjectData implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Map projectDataCache = new HashMap();

    private static final Map repositoryCache = new HashMap();

    private static final IResourceChangeListener uncacher = new IResourceChangeListener() {
        public void resourceChanged(final IResourceChangeEvent event) {
            final IResource r = event.getResource();
            if (r instanceof IProject) {
                uncacheDataFor((IProject) r);
            }
        }
    };

    public synchronized static GitProjectData getInstance(final IProject p)
            throws IOException {
        GitProjectData d = (GitProjectData) projectDataCache.get(p);
        if (d == null) {
            d = loadDataFor(p);
            p.getWorkspace().addResourceChangeListener(
                    uncacher,
                    IResourceChangeEvent.PRE_CLOSE
                            | IResourceChangeEvent.PRE_DELETE);
            projectDataCache.put(p, d);
            GitCorePlugin.traceVerbose("GitProjectData: +cached: " + p);
        }
        return d;
    }

    private synchronized static void uncacheDataFor(final IProject p) {
        if (projectDataCache.remove(p) != null) {
            GitCorePlugin.traceVerbose("GitProjectData: -cached: " + p);
        }
    }

    public synchronized static void deleteDataFor(final IProject p) {
        final File dat = fileFor(p);
        uncacheDataFor(p);
        dat.delete();
        GitCorePlugin.traceVerbose("GitProjectData: deleted " + dat);
    }

    public static GitProjectData loadDataFor(final IProject p)
            throws IOException {
        final File dat = fileFor(p);
        GitCorePlugin.traceVerbose("GitProjectData: loading " + dat);

        final ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(dat));
        try {
            final GitProjectData d = (GitProjectData) ois.readObject();
            d.setProject(p);
            return d;
        } catch (ClassNotFoundException cnfe) {
            final IOException e = new IOException("Can't load " + dat + ".");
            e.initCause(cnfe);
            throw e;
        } finally {
            ois.close();
        }
    }

    public synchronized static Repository getRepository(final File gitDir) {
        // Clean out stale entries in repositoryCache.
        //
        final Iterator i = repositoryCache.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry e = (Map.Entry) i.next();
            if (((Reference) e.getValue()).get() == null) {
                i.remove();
            }
        }

        final Reference r = (Reference) repositoryCache.get(gitDir);
        Repository d = r != null ? (Repository) r.get() : null;

        if (d == null) {
            d = new Repository(gitDir);
            repositoryCache.put(gitDir, new WeakReference(d));
        }
        return d;
    }

    private static File fileFor(final IProject p) {
        return new File(p.getWorkingLocation(GitCorePlugin.getPluginId())
                .toFile(), "GitProjectData.ser");
    }

    private transient IProject project;

    private transient Map liveRepoMappings;

    private transient Set protectedResources;

    private transient Map readRepoMappings;

    public GitProjectData() {
        liveRepoMappings = new HashMap();
        protectedResources = new HashSet();
    }

    public void setProject(final IProject p) throws IOException {
        project = p;

        if (readRepoMappings != null) {
            final Iterator i = readRepoMappings.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry e = (Map.Entry) i.next();
                final IPath cp = Path.fromPortableString((String) e.getKey());
                final IResource m = getProject().findMember(cp);
                final File gitDir;

                gitDir = new File((String) e.getValue()).getCanonicalFile();
                if (m instanceof IContainer) {
                    registerRepository((IContainer) m, gitDir);
                } else {
                    GitCorePlugin.log(
                            CoreText.GitProjectData_mappedResourceGone,
                            new FileNotFoundException(cp.toString()));
                }
            }
            readRepoMappings = null;
        }
    }

    public IProject getProject() {
        return project;
    }

    public void setRepositoryMappings(final Map m) throws IOException {
        liveRepoMappings.clear();
        protectedResources.clear();
        readRepoMappings = null;

        final Iterator i = m.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry e = (Map.Entry) i.next();
            final IContainer c = (IContainer) e.getKey();
            final File gitDir = (File) e.getValue();
            registerRepository(c, gitDir);
        }
    }

    public void markTeamPrivateResources() throws CoreException {
        final Iterator i = liveRepoMappings.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry e = (Map.Entry) i.next();
            final IContainer c = (IContainer) e.getKey();
            final IResource dotGit = c.findMember(".git");
            if (dotGit != null) {
                final Repository r = (Repository) e.getValue();
                final File dotGitDir;

                try {
                    dotGitDir = dotGit.getLocation().toFile()
                            .getCanonicalFile();
                } catch (IOException err) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            GitCorePlugin.getPluginId(), 1, err.getMessage(),
                            err));
                }

                if (dotGitDir.equals(r.getDirectory())) {
                    GitCorePlugin.traceVerbose("GitProjectData: +teamPrivate: "
                            + dotGit);
                    dotGit.setTeamPrivateMember(true);
                }
            }
        }
    }

    public boolean isProtected(final IFolder f) {
        return protectedResources.contains(f);
    }

    public void save() throws IOException {
        final File dat = fileFor(getProject());
        final File tmp = File.createTempFile("gpd_", ".ser", dat
                .getParentFile());
        GitCorePlugin.traceVerbose("GitProjectData: saving " + dat);

        final ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(tmp));
        boolean ok = false;
        try {
            oos.writeObject(this);
            ok = true;
        } finally {
            oos.close();
            if (!ok) {
                tmp.delete();
            }
        }

        dat.delete();
        if (!tmp.renameTo(dat)) {
            tmp.delete();
            throw new IOException("Failed to rename temporary file "
                    + tmp.getName() + " to " + dat);
        }
    }

    private void registerRepository(final IContainer c, final File gitDir)
            throws IOException {
        final Repository r;
        final IResource dotGit;

        r = getRepository(gitDir.getCanonicalFile());
        liveRepoMappings.put(c, r);
        GitCorePlugin.traceVerbose("GitProjectData: mapped: " + c + " -> "
                + r.getDirectory());

        // If the repository for this container is stored directly within the
        // container itself we must protect the repository and the container,
        // even if it is a linked resource. We don't want our mapped containers
        // disappearing on us.
        //
        dotGit = c.findMember(".git");
        if (dotGit != null) {
            if (r.getDirectory().equals(
                    dotGit.getLocation().toFile().getCanonicalFile())) {
                protect(dotGit);
            }
        }
    }

    private void protect(IResource c) {
        while (c != null && !c.equals(getProject())) {
            GitCorePlugin.traceVerbose("GitProjectData: protect: " + c);
            protectedResources.add(c);
            c = c.getParent();
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();

        liveRepoMappings = new HashMap();
        protectedResources = new HashSet();

        readRepoMappings = new HashMap();
        int mappingsLeft = in.readInt();
        while (mappingsLeft-- > 0) {
            final String containerPath = in.readUTF();
            final String repoPath = in.readUTF();
            readRepoMappings.put(containerPath, repoPath);
        }
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeInt(liveRepoMappings.size());
        final Iterator i = liveRepoMappings.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry e = (Map.Entry) i.next();
            final IContainer c = (IContainer) e.getKey();
            final Repository r = (Repository) e.getValue();
            out.writeUTF(c.getProjectRelativePath().toPortableString());
            out.writeUTF(r.getDirectory().getCanonicalPath());
        }
    }
}
