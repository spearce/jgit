/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * See LICENSE for the full license text, also available.
 *******************************************************************************/
package org.spearce.egit.ui.internal.components;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.spearce.egit.ui.UIIcons;

/**
 * Label provider displaying native check boxes images for boolean values.
 * Label-image is centralized.
 * <p>
 * Concrete implementations must provide object to boolean mapping.
 * <p>
 * This implementation is actually workaround for lacking features in
 * TableViewer. It is based on (workaround) snippets&tricks found on Internet.
 */
public abstract class CheckboxLabelProvider extends CenteredImageLabelProvider {
	private static Image createCheckboxImage(final Control control,
			boolean checked, boolean enabled) {

		String checkboxhack = System.getProperty("egit.swt.checkboxhack");
		if (checkboxhack == null)
			if (Platform.getOS().equals(Platform.OS_MACOSX))
				checkboxhack = "hardwired";
			else
				checkboxhack = "screenshot";

		if (checkboxhack == "hardwired") {
			if (enabled) {
				if (checked)
					return UIIcons.CHECKBOX_ENABLED_CHECKED.createImage();
				return UIIcons.CHECKBOX_ENABLED_UNCHECKED.createImage();
			}
			if (checked)
				return UIIcons.CHECKBOX_DISABLED_CHECKED.createImage();
			return UIIcons.CHECKBOX_DISABLED_UNCHECKED.createImage();
		}

		// else if checkboxhack = "screenshot";

		// FIXME: Shawn says that blinking shell caused by below code is very
		// annoying...(at least on Mac) - anyone knows better workaround?
		final Shell s = new Shell(control.getShell(), SWT.NO_TRIM);
		// Hopefully no platform uses exactly this color because we'll make
		// it transparent in the image.
		final Color greenScreen = new Color(control.getDisplay(), 222, 223, 224);

		// otherwise we have a default gray color
		s.setBackground(greenScreen);

		final Button b = new Button(s, SWT.CHECK);
		b.setSelection(checked);
		b.setEnabled(enabled);
		b.setBackground(greenScreen);

		// otherwise an image is located in a corner
		b.setLocation(0, 0);
		final Point bSize = b.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		// otherwise an image is stretched by width
		bSize.x = Math.max(bSize.x, bSize.y);
		bSize.y = Math.max(bSize.x, bSize.y);
		b.setSize(bSize);
		s.setSize(bSize);
		s.open();

		final GC gc = new GC(b);
		final Image image = new Image(control.getShell().getDisplay(), bSize.x,
				bSize.y);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		s.close();

		final ImageData imageData = image.getImageData();
		imageData.transparentPixel = imageData.palette.getPixel(greenScreen
				.getRGB());
		return new Image(control.getDisplay(), imageData);
	}

	private final Image imageCheckedEnabled;

	private final Image imageUncheckedEnabled;

	private final Image imageCheckedDisabled;

	private final Image imageUncheckedDisabled;

	/**
	 * Create label provider for provided viewer.
	 *
	 * @param control
	 *            viewer where label provided is used.
	 */
	public CheckboxLabelProvider(final Control control) {
		imageCheckedEnabled = createCheckboxImage(control, true, true);
		imageUncheckedEnabled = createCheckboxImage(control, false, true);
		imageCheckedDisabled = createCheckboxImage(control, true, false);
		imageUncheckedDisabled = createCheckboxImage(control, false, false);
	}

	@Override
	public void dispose() {
		super.dispose();
		imageCheckedEnabled.dispose();
		imageUncheckedEnabled.dispose();
		imageCheckedDisabled.dispose();
		imageUncheckedDisabled.dispose();
	}

	@Override
	protected Image getImage(final Object element) {
		if (isEnabled(element)) {
			if (isChecked(element))
				return imageCheckedEnabled;
			return imageUncheckedEnabled;
		} else {
			if (isChecked(element))
				return imageCheckedDisabled;
			return imageUncheckedDisabled;
		}
	}

	/**
	 * @param element
	 *            element to provide label for.
	 * @return true if checkbox label should be checked for this element, false
	 *         otherwise.
	 */
	protected abstract boolean isChecked(Object element);

	/**
	 * Default implementation always return true.
	 *
	 * @param element
	 *            element to provide label for.
	 * @return true if checkbox label should be enabled for this element, false
	 *         otherwise.
	 */
	protected boolean isEnabled(final Object element) {
		return true;
	}
}
