/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.spearce.egit.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * A collection of factory methods for creating common SWT controls
 */
public class SWTUtils {

	/** */
	public static final int MARGINS_DEFAULT = -1;

	/** */
	public static final int MARGINS_NONE = 0;

	/** */
	public static final int MARGINS_DIALOG = 1;

	/**
	 * Creates a preference link which will open in the specified container
	 *
	 * @param container
	 * @param parent
	 * @param pageId
	 * @param text
	 *
	 * @return the created link
	 */
	public static PreferenceLinkArea createPreferenceLink(
			IWorkbenchPreferenceContainer container, Composite parent,
			String pageId, String text) {
		final PreferenceLinkArea area = new PreferenceLinkArea(parent,
				SWT.NONE, pageId, text, container, null);
		return area;
	}

	/**
	 * Creates a grid data with the specified metrics
	 *
	 * @param width
	 * @param height
	 * @param hFill
	 * @param vFill
	 *
	 * @return the created grid data
	 */
	public static GridData createGridData(int width, int height, boolean hFill,
			boolean vFill) {
		return createGridData(width, height, hFill ? SWT.FILL : SWT.BEGINNING,
				vFill ? SWT.FILL : SWT.CENTER, hFill, vFill);
	}

	/**
	 * Creates a grid data with the specified metrics
	 *
	 * @param width
	 * @param height
	 * @param hAlign
	 * @param vAlign
	 * @param hGrab
	 * @param vGrab
	 *
	 * @return the created grid data
	 */
	public static GridData createGridData(int width, int height, int hAlign,
			int vAlign, boolean hGrab, boolean vGrab) {
		final GridData gd = new GridData(hAlign, vAlign, hGrab, vGrab);
		gd.widthHint = width;
		gd.heightHint = height;
		return gd;
	}

	/**
	 * Creates a horizontal grid data with the default metrics
	 *
	 * @return the created grid data
	 */
	public static GridData createHFillGridData() {
		return createHFillGridData(1);
	}

	/**
	 * Creates a horizontal grid data with the specified span
	 *
	 * @param span
	 *
	 * @return the created grid data
	 */
	public static GridData createHFillGridData(int span) {
		final GridData gd = createGridData(0, SWT.DEFAULT, SWT.FILL,
				SWT.CENTER, true, false);
		gd.horizontalSpan = span;
		return gd;
	}

	/**
	 * Creates a horizontal fill composite with the specified margins
	 *
	 * @param parent
	 * @param margins
	 *
	 * @return the created composite
	 */
	public static Composite createHFillComposite(Composite parent, int margins) {
		return createHFillComposite(parent, margins, 1);
	}

	/**
	 * Creates a horizontal fill composite with the specified margins and
	 * columns
	 *
	 * @param parent
	 * @param margins
	 * @param columns
	 *
	 * @return the created composite
	 */
	public static Composite createHFillComposite(Composite parent, int margins,
			int columns) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(createHFillGridData());
		composite.setLayout(createGridLayout(columns,
				new PixelConverter(parent), margins));
		return composite;
	}

	/**
	 * Creates a horizontal/vertical fill composite with the specified margins
	 *
	 * @param parent
	 * @param margins
	 *
	 * @return the created composite
	 */
	public static Composite createHVFillComposite(Composite parent, int margins) {
		return createHVFillComposite(parent, margins, 1);
	}

	/**
	 * Creates a horizontal/vertical fill composite with the specified margins
	 * and columns
	 *
	 * @param parent
	 * @param margins
	 * @param columns
	 *
	 * @return the created composite
	 */
	public static Composite createHVFillComposite(Composite parent,
			int margins, int columns) {
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setFont(parent.getFont());
		composite.setLayoutData(createHVFillGridData());
		composite.setLayout(createGridLayout(columns,
				new PixelConverter(parent), margins));
		return composite;
	}

	/**
	 * Creates a horizontal fill group with the specified text and margins
	 *
	 * @param parent
	 * @param text
	 * @param margins
	 * @return the created group
	 */
	public static Group createHFillGroup(Composite parent, String text,
			int margins) {
		return createHFillGroup(parent, text, margins, 1);
	}

	/**
	 * Creates a horizontal fill group with the specified text, margins and rows
	 *
	 * @param parent
	 * @param text
	 * @param margins
	 * @param rows
	 *
	 * @return the created group
	 */
	public static Group createHFillGroup(Composite parent, String text,
			int margins, int rows) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(parent.getFont());
		group.setLayoutData(createHFillGridData());
		if (text != null)
			group.setText(text);
		group.setLayout(createGridLayout(rows, new PixelConverter(parent),
				margins));
		return group;
	}

	/**
	 * Creates a horizontal/vertical fill group with the specified text and
	 * margins
	 *
	 * @param parent
	 * @param text
	 * @param margins
	 *
	 * @return the created group
	 */
	public static Group createHVFillGroup(Composite parent, String text,
			int margins) {
		return createHVFillGroup(parent, text, margins, 1);
	}

	/**
	 * Creates a horizontal/vertical fill group with the specified text, margins
	 * and rows
	 *
	 * @param parent
	 * @param text
	 * @param margins
	 * @param rows
	 *
	 * @return the created group
	 */
	public static Group createHVFillGroup(Composite parent, String text,
			int margins, int rows) {
		final Group group = new Group(parent, SWT.NONE);
		group.setFont(parent.getFont());
		group.setLayoutData(createHVFillGridData());
		if (text != null)
			group.setText(text);
		group.setLayout(createGridLayout(rows, new PixelConverter(parent),
				margins));
		return group;
	}

	/**
	 * Creates a horizontal/vertical fill grid data with the default metrics
	 *
	 * @return the created grid data
	 */
	public static GridData createHVFillGridData() {
		return createHVFillGridData(1);
	}

	/**
	 * Creates a horizontal/vertical fill grid data with the specified span
	 *
	 * @param span
	 *
	 * @return the created grid data
	 */
	public static GridData createHVFillGridData(int span) {
		final GridData gd = createGridData(0, 0, true, true);
		gd.horizontalSpan = span;
		return gd;
	}

	/**
	 * Creates a grid layout with the specified number of columns and the
	 * standard spacings.
	 *
	 * @param numColumns
	 *            the number of columns
	 * @param converter
	 *            the pixel converter
	 * @param margins
	 *            one of <code>MARGINS_DEFAULT</code>, <code>MARGINS_NONE</code>
	 *            or <code>MARGINS_DIALOG</code>.
	 *
	 * @return the created grid layout
	 */
	public static GridLayout createGridLayout(int numColumns,
			PixelConverter converter, int margins) {
		Assert.isTrue(margins == MARGINS_DEFAULT || margins == MARGINS_NONE
				|| margins == MARGINS_DIALOG);

		final GridLayout layout = new GridLayout(numColumns, false);
		layout.horizontalSpacing = converter
				.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = converter
				.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		switch (margins) {
		case MARGINS_NONE:
			layout.marginLeft = layout.marginRight = 0;
			layout.marginTop = layout.marginBottom = 0;
			break;
		case MARGINS_DIALOG:
			layout.marginLeft = layout.marginRight = converter
					.convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
			layout.marginTop = layout.marginBottom = converter
					.convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
			break;
		case MARGINS_DEFAULT:
			layout.marginLeft = layout.marginRight = layout.marginWidth;
			layout.marginTop = layout.marginBottom = layout.marginHeight;
		}
		layout.marginWidth = layout.marginHeight = 0;
		return layout;
	}

	/**
	 * Creates a label with the specified message
	 *
	 * @param parent
	 * @param message
	 *
	 * @return the created label
	 */
	public static Label createLabel(Composite parent, String message) {
		return createLabel(parent, message, 1);
	}

	/**
	 * Creates a label with the specified message and span
	 *
	 * @param parent
	 * @param message
	 * @param span
	 *
	 * @return the created label
	 */
	public static Label createLabel(Composite parent, String message, int span) {
		final Label label = new Label(parent, SWT.WRAP);
		if (message != null)
			label.setText(message);
		label.setLayoutData(createHFillGridData(span));
		return label;
	}

	/**
	 * Creates a check box with the specified message
	 *
	 * @param parent
	 * @param message
	 *
	 * @return the created check box
	 */
	public static Button createCheckBox(Composite parent, String message) {
		return createCheckBox(parent, message, 1);
	}

	/**
	 * Creates a check box with the specified message and span
	 *
	 * @param parent
	 * @param message
	 * @param span
	 *
	 * @return the created check box
	 */
	public static Button createCheckBox(Composite parent, String message,
			int span) {
		final Button button = new Button(parent, SWT.CHECK);
		button.setText(message);
		button.setLayoutData(createHFillGridData(span));
		return button;
	}

	/**
	 * Creates a radio button with the specified message
	 *
	 * @param parent
	 * @param message
	 *
	 * @return the created radio button
	 */
	public static Button createRadioButton(Composite parent, String message) {
		return createRadioButton(parent, message, 1);
	}

	/**
	 * Creates a radio button with the specified message and span
	 *
	 * @param parent
	 * @param message
	 * @param span
	 *
	 * @return the created radio button
	 */
	public static Button createRadioButton(Composite parent, String message,
			int span) {
		final Button button = new Button(parent, SWT.RADIO);
		button.setText(message);
		button.setLayoutData(createHFillGridData(span));
		return button;
	}

	/**
	 * Creates a text control
	 *
	 * @param parent
	 *
	 * @return the created text control
	 */
	public static Text createText(Composite parent) {
		return createText(parent, 1);
	}

	/**
	 * Creates a text control with the specified span
	 *
	 * @param parent
	 * @param span
	 *
	 * @return the created text control
	 */
	public static Text createText(Composite parent, int span) {
		final Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(createHFillGridData(span));
		return text;
	}

	/**
	 * Creates a place holder with the specified height and span
	 *
	 * @param parent
	 * @param heightInChars
	 * @param span
	 *
	 * @return the created place holder
	 */
	public static Control createPlaceholder(Composite parent,
			int heightInChars, int span) {
		Assert.isTrue(heightInChars > 0);
		final Control placeHolder = new Composite(parent, SWT.NONE);
		final GridData gd = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
		gd.heightHint = new PixelConverter(parent)
				.convertHeightInCharsToPixels(heightInChars);
		gd.horizontalSpan = span;
		placeHolder.setLayoutData(gd);
		return placeHolder;
	}

	/**
	 * Creates a place holder with the specified height
	 *
	 * @param parent
	 * @param heightInChars
	 * @return the created place holder
	 */
	public static Control createPlaceholder(Composite parent, int heightInChars) {
		return createPlaceholder(parent, heightInChars, 1);
	}

	/**
	 * Creates a pixel converter
	 *
	 * @param control
	 *
	 * @return the created pixel converter
	 */
	public static PixelConverter createDialogPixelConverter(Control control) {
		Dialog.applyDialogFont(control);
		return new PixelConverter(control);
	}

	/**
	 * Calculates the size of the specified controls, using the specified
	 * converter
	 *
	 * @param converter
	 * @param controls
	 *
	 * @return the size of the control(s)
	 */
	public static int calculateControlSize(PixelConverter converter,
			Control[] controls) {
		return calculateControlSize(converter, controls, 0, controls.length - 1);
	}

	/**
	 * Calculates the size of the specified subset of controls, using the
	 * specified converter
	 *
	 * @param converter
	 * @param controls
	 * @param start
	 * @param end
	 *
	 * @return the created control
	 */
	public static int calculateControlSize(PixelConverter converter,
			Control[] controls, int start, int end) {
		int minimum = converter
				.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		for (int i = start; i <= end; i++) {
			final int length = controls[i]
					.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			if (minimum < length)
				minimum = length;
		}
		return minimum;
	}

	/**
	 * Equalizes the specified controls using the specified converter
	 *
	 * @param converter
	 * @param controls
	 */
	public static void equalizeControls(PixelConverter converter,
			Control[] controls) {
		equalizeControls(converter, controls, 0, controls.length - 1);
	}

	/**
	 * Equalizes the specified subset of controls using the specified converter
	 *
	 * @param converter
	 * @param controls
	 * @param start
	 * @param end
	 */
	public static void equalizeControls(PixelConverter converter,
			Control[] controls, int start, int end) {
		final int size = calculateControlSize(converter, controls, start, end);
		for (int i = start; i <= end; i++) {
			final Control button = controls[i];
			if (button.getLayoutData() instanceof GridData) {
				((GridData) button.getLayoutData()).widthHint = size;
			}
		}
	}

	/**
	 * Gets the width of the longest string in <code>strings</code>, using the
	 * specified pixel converter
	 *
	 * @param converter
	 * @param strings
	 *
	 * @return the width of the longest string
	 */
	public static int getWidthInCharsForLongest(PixelConverter converter,
			String[] strings) {
		int minimum = 0;
		for (int i = 0; i < strings.length; i++) {
			final int length = converter.convertWidthInCharsToPixels(strings[i]
					.length());
			if (minimum < length)
				minimum = length;
		}
		return minimum;
	}

	private static class PixelConverter {

		private final FontMetrics fFontMetrics;

		public PixelConverter(Control control) {
			GC gc = new GC(control);
			try {
				gc.setFont(control.getFont());
				fFontMetrics = gc.getFontMetrics();
			} finally {
				gc.dispose();
			}
		}

		public int convertHeightInCharsToPixels(int chars) {
			return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
		}

		public int convertHorizontalDLUsToPixels(int dlus) {
			return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
		}

		public int convertVerticalDLUsToPixels(int dlus) {
			return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
		}

		public int convertWidthInCharsToPixels(int chars) {
			return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
		}
	}
}
