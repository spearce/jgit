/*******************************************************************************
 * Copyright (C) 2006  Guilhem Bonnefille
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.spearce.egit.ui.internal.actions;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.spearce.egit.ui.Activator;

/**
 * An abstract class that acts as a super class for FileSystemProvider actions.
 * It provides some general methods applicable to multiple actions.
 */
public abstract class GitAction extends TeamAction {

	/**
	 * @see TeamAction#isEnabled()
	 */
	protected boolean isEnabled() {
		return getSelectedMappings().length > 0;
	}

	/**
	 * Return the selected resource mappings that are associated with the
	 * file system provider.
	 * @return the selected resource mappings that are associated with the
	 * file system provider.
	 */
	protected ResourceMapping[] getSelectedMappings() {
		return getSelectedResourceMappings(Activator.getPluginProviderId());
	}
}
