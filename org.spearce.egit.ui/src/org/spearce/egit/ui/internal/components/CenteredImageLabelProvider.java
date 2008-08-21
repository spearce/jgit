/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

/**
 * Label provider displaying image centered.
 * <p>
 * This implementation is actually workaround for lacking SWT/JFace features.
 * Code is based on official snippet found on Internet.
 */
// FIXME: doesn't work on Mac OS X 10.5 / Eclipse 3.3
public abstract class CenteredImageLabelProvider extends OwnerDrawLabelProvider {
	/**
	 * @param element
	 *            element to provide label for.
	 * @return image for provided element.
	 */
	protected abstract Image getImage(final Object element);

	@Override
	protected void measure(Event event, Object element) {
		// empty
	}

	@Override
	protected void paint(final Event event, final Object element) {
		final Image image = getImage(element);
		final Rectangle bounds = ((TableItem) event.item)
				.getBounds(event.index);
		final Rectangle imgBounds = image.getBounds();
		bounds.width /= 2;
		bounds.width -= imgBounds.width / 2;
		bounds.height /= 2;
		bounds.height -= imgBounds.height / 2;

		final int x = bounds.width > 0 ? bounds.x + bounds.width : bounds.x;
		final int y = bounds.height > 0 ? bounds.y + bounds.height : bounds.y;

		event.gc.drawImage(image, x, y);
	}
}
