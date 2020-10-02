import javax.swing.*;
import java.awt.Point;
import java.awt.event.*;

public class MotionPanel extends JPanel{
	private Point initialClick;
	private JDialog parentJDialog;
	private JFrame parentJFrame;

	public MotionPanel(final JDialog parent) {
		this.parentJDialog = parent;

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				initialClick = e.getPoint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {

				// get location of Window
				int thisX = parentJDialog.getLocation().x;
				int thisY = parentJDialog.getLocation().y;

				// Determine how much the mouse moved since the initial click
				int xMoved = e.getX() - initialClick.x;
				int yMoved = e.getY() - initialClick.y;

				// Move window to this position
				int X = thisX + xMoved;
				int Y = thisY + yMoved;
				parentJDialog.setLocation(X, Y);
			}
		});
	}

	public MotionPanel(final JFrame parent) {
		this.parentJFrame = parent;

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				initialClick = e.getPoint();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {

				// get location of Window
				int thisX = parentJFrame.getLocation().x;
				int thisY = parentJFrame.getLocation().y;

				// Determine how much the mouse moved since the initial click
				int xMoved = e.getX() - initialClick.x;
				int yMoved = e.getY() - initialClick.y;

				// Move window to this position
				int X = thisX + xMoved;
				int Y = thisY + yMoved;
				parentJFrame.setLocation(X, Y);
			}
		});
	}
}