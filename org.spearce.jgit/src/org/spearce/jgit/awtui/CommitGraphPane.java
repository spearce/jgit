package org.spearce.jgit.awtui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.spearce.jgit.awtui.SwingCommitList.SwingLane;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.revplot.AbstractPlotRenderer;
import org.spearce.jgit.revplot.PlotCommit;
import org.spearce.jgit.revplot.PlotCommitList;

/**
 * Draws a commit graph in a JTable.
 * <p>
 * This class is currently a very primitive commit visualization tool. It shows
 * a table of 3 columns:
 * <ol>
 * <li>Commit graph and short message</li>
 * <li>Author name and email address</li>
 * <li>Author date and time</li>
 * </ul>
 */
public class CommitGraphPane extends JTable {
	private static final long serialVersionUID = 1L;

	private final SwingCommitList allCommits;

	/** Create a new empty panel. */
	public CommitGraphPane() {
		allCommits = new SwingCommitList();
		configureHeader();
		setShowHorizontalLines(false);
		setRowMargin(0);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * Get the commit list this pane renders from.
	 * 
	 * @return the list the caller must populate.
	 */
	public PlotCommitList getCommitList() {
		return allCommits;
	}

	@Override
	public void setModel(final TableModel dataModel) {
		if (dataModel != null && !(dataModel instanceof CommitTableModel))
			throw new ClassCastException("Must be special table model.");
		super.setModel(dataModel);
	}

	@Override
	protected TableModel createDefaultDataModel() {
		return new CommitTableModel();
	}

	private void configureHeader() {
		final JTableHeader th = getTableHeader();
		final TableColumnModel cols = th.getColumnModel();

		final TableColumn graph = cols.getColumn(0);
		final TableColumn author = cols.getColumn(1);
		final TableColumn date = cols.getColumn(2);

		graph.setHeaderValue("");
		author.setHeaderValue("Author");
		date.setHeaderValue("Date");

		graph.setCellRenderer(new GraphCellRender());
		author.setCellRenderer(new NameCellRender());
		date.setCellRenderer(new DateCellRender());
	}

	class CommitTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		PlotCommit<SwingLane> lastCommit;

		PersonIdent lastAuthor;

		public int getColumnCount() {
			return 3;
		}

		public int getRowCount() {
			return allCommits != null ? allCommits.size() : 0;
		}

		public Object getValueAt(final int rowIndex, final int columnIndex) {
			final PlotCommit<SwingLane> c = allCommits.get(rowIndex);
			switch (columnIndex) {
			case 0:
				return c;
			case 1:
				return authorFor(c);
			case 2:
				return authorFor(c);
			default:
				return null;
			}
		}

		PersonIdent authorFor(final PlotCommit<SwingLane> c) {
			if (c != lastCommit) {
				lastCommit = c;
				lastAuthor = c.getAuthorIdent();
			}
			return lastAuthor;
		}
	}

	class NameCellRender extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(final JTable table,
				final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			final PersonIdent pi = (PersonIdent) value;

			final String valueStr;
			if (pi != null)
				valueStr = pi.getName() + " <" + pi.getEmailAddress() + ">";
			else
				valueStr = "";
			return super.getTableCellRendererComponent(table, valueStr,
					isSelected, hasFocus, row, column);
		}
	}

	class DateCellRender extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		private final DateFormat fmt = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		public Component getTableCellRendererComponent(final JTable table,
				final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			final PersonIdent pi = (PersonIdent) value;

			final String valueStr;
			if (pi != null)
				valueStr = fmt.format(pi.getWhen());
			else
				valueStr = "";
			return super.getTableCellRendererComponent(table, valueStr,
					isSelected, hasFocus, row, column);
		}
	}

	class GraphCellRender extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		private final AWTPlotRenderer renderer = new AWTPlotRenderer(this);

		PlotCommit<SwingLane> commit;

		public Component getTableCellRendererComponent(final JTable table,
				final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column) {
			super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);
			commit = (PlotCommit<SwingLane>) value;
			return this;
		}

		@Override
		protected void paintComponent(final Graphics inputGraphics) {
			if (inputGraphics == null)
				return;
			renderer.paint(inputGraphics, commit);
		}
	}

	static final Stroke[] strokeCache;

	static {
		strokeCache = new Stroke[4];
		for (int i = 1; i < strokeCache.length; i++)
			strokeCache[i] = new BasicStroke(i);
	}

	static Stroke stroke(final int width) {
		if (width < strokeCache.length)
			return strokeCache[width];
		return new BasicStroke(width);
	}

	final class AWTPlotRenderer extends AbstractPlotRenderer<SwingLane, Color> {

		final GraphCellRender cell;

		Graphics2D g;

		AWTPlotRenderer(final GraphCellRender c) {
			cell = c;
		}

		void paint(final Graphics in, final PlotCommit<SwingLane> commit) {
			g = (Graphics2D) in.create();
			try {
				final int h = cell.getHeight();
				g.setColor(cell.getBackground());
				g.fillRect(0, 0, cell.getWidth(), h);
				if (commit != null)
					paintCommit(commit, h);
			} finally {
				g.dispose();
				g = null;
			}
		}

		@Override
		protected void drawLine(final Color color, int x1, int y1, int x2,
				int y2, int width) {
			if (y1 == y2) {
				x1 -= width / 2;
				x2 -= width / 2;
			} else if (x1 == x2) {
				y1 -= width / 2;
				y2 -= width / 2;
			}

			g.setColor(color);
			g.setStroke(stroke(width));
			g.drawLine(x1, y1, x2, y2);
		}

		@Override
		protected void drawCommitDot(final int x, final int y, final int w,
				final int h) {
			g.setColor(Color.blue);
			g.setStroke(strokeCache[1]);
			g.fillOval(x, y, w, h);
			g.setColor(Color.black);
			g.drawOval(x, y, w, h);
		}

		@Override
		protected void drawBoundaryDot(final int x, final int y, final int w,
				final int h) {
			g.setColor(cell.getBackground());
			g.setStroke(strokeCache[1]);
			g.fillOval(x, y, w, h);
			g.setColor(Color.black);
			g.drawOval(x, y, w, h);
		}

		@Override
		protected void drawText(final String msg, final int x, final int y) {
			final int texty = g.getFontMetrics().getHeight()
					- g.getFontMetrics().getDescent();
			g.setColor(cell.getForeground());
			g.drawString(msg, x, texty - (cell.getHeight() - y * 2));
		}

		@Override
		protected Color laneColor(final SwingLane myLane) {
			return myLane != null ? myLane.color : Color.black;
		}

		void paintTriangleDown(final int cx, final int y, final int h) {
			final int tipX = cx;
			final int tipY = y + h;
			final int baseX1 = cx - 10 / 2;
			final int baseX2 = tipX + 10 / 2;
			final int baseY = y;
			final Polygon triangle = new Polygon();
			triangle.addPoint(tipX, tipY);
			triangle.addPoint(baseX1, baseY);
			triangle.addPoint(baseX2, baseY);
			g.fillPolygon(triangle);
			g.drawPolygon(triangle);
		}
	}

}
