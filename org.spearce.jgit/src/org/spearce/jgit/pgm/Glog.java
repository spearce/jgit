package org.spearce.jgit.pgm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.spearce.jgit.awtui.CommitGraphPane;
import org.spearce.jgit.revplot.PlotWalk;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;

class Glog extends RevWalkTextBuiltin {
	final JFrame frame;

	final CommitGraphPane graphPane;

	Glog() {
		frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				frame.dispose();
			}
		});

		graphPane = new CommitGraphPane();

		final JScrollPane graphScroll = new JScrollPane(graphPane);

		final JPanel buttons = new JPanel(new FlowLayout());
		final JButton repaint = new JButton();
		repaint.setText("Repaint");
		repaint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				graphPane.repaint();
			}
		});
		buttons.add(repaint);

		final JPanel world = new JPanel(new BorderLayout());
		world.add(buttons, BorderLayout.SOUTH);
		world.add(graphScroll, BorderLayout.CENTER);

		frame.getContentPane().add(world);
	}

	@Override
	protected int walkLoop() throws Exception {
		graphPane.getCommitList().source(walk);
		graphPane.getCommitList().fillTo(Integer.MAX_VALUE);

		frame.setTitle("[" + repoName() + "]");
		frame.pack();
		frame.setVisible(true);
		return graphPane.getCommitList().size();
	}

	@Override
	protected void show(final RevCommit c) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	protected RevWalk createWalk() {
		return new PlotWalk(db);
	}

	private String repoName() {
		final File f = db.getDirectory();
		String n = f.getName();
		if (".git".equals(n))
			n = f.getParentFile().getName();
		return n;
	}
}
