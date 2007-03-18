/*
 *  Copyright (C) 2006  Robin Rosenberg
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License, version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 */
package org.spearce.egit.core;

import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.team.core.RepositoryProvider;
import org.spearce.egit.core.project.RepositoryMapping;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Tree;

public class GitTreeStorage implements IContainer {

	private Tree entry;

	public GitTreeStorage(ObjectId treeId, IResource resource, String relativeResourceName) {
		if (treeId == null)
			return;

		GitProvider provider = (GitProvider) RepositoryProvider
				.getProvider(resource.getProject());
		RepositoryMapping repositoryMapping = provider.getData()
				.getRepositoryMapping(resource.getProject());
		Tree tree;
		try {
			tree = repositoryMapping.getRepository().mapTree(treeId);
			String prefix = repositoryMapping.getSubset();
			if (prefix != null)
				prefix = prefix + "/";
			else
				prefix = "";
			String name = prefix + resource.getProjectRelativePath().toString();
			entry = (Tree)tree.findMember(name);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean exists(IPath path) {
		try {
			return entry.findMember(path.toString()) != null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public IFile[] findDeletedMembersWithHistory(int depth,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource findMember(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource findMember(IPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource findMember(String name, boolean includePhantoms) {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource findMember(IPath path, boolean includePhantoms) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDefaultCharset() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDefaultCharset(boolean checkImplicit) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IFile getFile(IPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	public IFolder getFolder(IPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource[] members() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource[] members(boolean includePhantoms) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IResource[] members(int memberFlags) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDefaultCharset(String charset) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setDefaultCharset(String charset, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void accept(IResourceVisitor visitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void accept(IResourceProxyVisitor visitor, int memberFlags)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void accept(IResourceVisitor visitor, int depth,
			boolean includePhantoms) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void accept(IResourceVisitor visitor, int depth, int memberFlags)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void clearHistory(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void copy(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void copy(IPath destination, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void copy(IProjectDescription description, boolean force,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void copy(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public IMarker createMarker(String type) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IResourceProxy createProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	public void delete(boolean force, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void delete(int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void deleteMarkers(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	public IMarker findMarker(long id) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth)
			throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getFullPath() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getLocalTimeStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	public IPath getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	public URI getLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	public IMarker getMarker(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getModificationStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public IContainer getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPersistentProperty(QualifiedName key) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public IProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getProjectRelativePath() {
		// TODO Auto-generated method stub
		return null;
	}

	public IPath getRawLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	public URI getRawLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	public ResourceAttributes getResourceAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getSessionProperty(QualifiedName key) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	public IWorkspace getWorkspace() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isDerived() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isLinked() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isLinked(int options) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isLocal(int depth) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isPhantom() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSynchronized(int depth) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isTeamPrivateMember() {
		// TODO Auto-generated method stub
		return false;
	}

	public void move(IPath destination, boolean force, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void move(IPath destination, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void move(IProjectDescription description, int updateFlags,
			IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void move(IProjectDescription description, boolean force,
			boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void refreshLocal(int depth, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void revertModificationStamp(long value) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setDerived(boolean isDerived) throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setLocal(boolean flag, int depth, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public long setLocalTimeStamp(long value) throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setPersistentProperty(QualifiedName key, String value)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setReadOnly(boolean readOnly) {
		// TODO Auto-generated method stub

	}

	public void setResourceAttributes(ResourceAttributes attributes)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setSessionProperty(QualifiedName key, Object value)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void setTeamPrivateMember(boolean isTeamPrivate)
			throws CoreException {
		// TODO Auto-generated method stub

	}

	public void touch(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

	}

	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean contains(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}
}
