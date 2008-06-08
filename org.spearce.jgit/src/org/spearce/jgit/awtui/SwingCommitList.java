/*
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Git Development Community nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spearce.jgit.awtui;

import java.awt.Color;
import java.util.LinkedList;

import org.spearce.jgit.revplot.PlotCommitList;
import org.spearce.jgit.revplot.PlotLane;

class SwingCommitList extends PlotCommitList<SwingCommitList.SwingLane> {
	final LinkedList<Color> colors;

	SwingCommitList() {
		colors = new LinkedList<Color>();
		repackColors();
	}

	private void repackColors() {
		colors.add(Color.green);
		colors.add(Color.blue);
		colors.add(Color.red);
		colors.add(Color.magenta);
		colors.add(Color.darkGray);
		colors.add(Color.yellow.darker());
		colors.add(Color.orange);
	}

	@Override
	protected SwingLane createLane() {
		final SwingLane lane = new SwingLane();
		if (colors.isEmpty())
			repackColors();
		lane.color = colors.removeFirst();
		return lane;
	}

	@Override
	protected void recycleLane(final SwingLane lane) {
		colors.add(lane.color);
	}

	static class SwingLane extends PlotLane {
		Color color;
	}
}
