import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class MyScrollBarUI extends BasicScrollBarUI {
	private final Dimension d = new Dimension();

	@Override protected JButton createDecreaseButton(int orientation) {
		return new JButton() {
			@Override public Dimension getPreferredSize() {
				return d;
			}
		};
	}

	@Override protected JButton createIncreaseButton(int orientation) {
		return new JButton() {
			@Override public Dimension getPreferredSize() {
				return d;
			}
		};
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
		Graphics2D g2d = (Graphics2D)g.create();

		g2d.setPaint(Color.GRAY);
//		g2d.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

		g2d.fill(r);
		g2d.draw(r);
		g2d.dispose();
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
		Graphics2D g2 = (Graphics2D)g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
		Color color = null;
		JScrollBar sb = (JScrollBar)c;
		if(!sb.isEnabled() || r.width>r.height) {
			return;
		}
		else {
			color = new Color(0, 255, 255);
		}
		g2.setPaint(color);
		g2.fillRoundRect(r.x, r.y, r.width - 1, r.height, 10, 10);

		g2.dispose();
	}

	@Override
	protected void setThumbBounds(int x, int y, int width, int height) {
		super.setThumbBounds(x, y, width, height);
		scrollbar.repaint();
	}
};