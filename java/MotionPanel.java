import javax.swing.*;
import java.awt.Point;
import java.awt.event.*;

public class MotionPanel extends JPanel{
	private Point initialClick;
	private JDialog parent;

	public MotionPanel(final JDialog parent) {
		this.parent = parent;

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				initialClick = e.getPoint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {

				// get location of Window
				int thisX = parent.getLocation().x;
				int thisY = parent.getLocation().y;

				// Determine how much the mouse moved since the initial click
				int xMoved = e.getX() - initialClick.x;
				int yMoved = e.getY() - initialClick.y;

				// Move window to this position
				int X = thisX + xMoved;
				int Y = thisY + yMoved;
				parent.setLocation(X, Y);
			}
		});
	}
}