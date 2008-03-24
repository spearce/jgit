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
		colors.add(Color.red);
		colors.add(Color.blue);
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
