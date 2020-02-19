import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class JTextFieldHint extends JTextField implements FocusListener {
	private JTextField jtf;
	private Icon icon;
	private Insets dummyInsets;

	public JTextFieldHint(JTextField jtf, ImageIcon icon){
		this.jtf = jtf;
		setIcon(icon);

		Border border = UIManager.getBorder("TextField.border");
		JTextField dummy = new JTextField();
		this.dummyInsets = border.getBorderInsets(dummy);

		addFocusListener(this);
	}

	public void setIcon(Icon newIcon){
		this.icon = newIcon;
	}

	@Override
	protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int textX = 2;

			if(this.icon!=null){
				int iconWidth = icon.getIconWidth();
				int iconHeight = icon.getIconHeight();
				int x = dummyInsets.left + 5;
				textX = x+iconWidth+2;
				int y = (this.getHeight() - iconHeight)/2;
				icon.paintIcon(this, g, x, y);
			}

			setMargin(new Insets(2, textX, 2, 2));

			if(this.getText().equals("")) {
				int width = this.getWidth();
				int height = this.getHeight();
				Font prev = g.getFont();
				Font italic = prev.deriveFont(Font.ITALIC);
				Color prevColor = g.getColor();
				g.setFont(italic);
				g.setColor(UIManager.getColor("textInactiveText"));
				int h = g.getFontMetrics().getHeight();
				int textBottom = (height - h) / 2 + h - 4;
				int x = this.getInsets().left;
				Graphics2D g2d = (Graphics2D) g;
				g.setFont(prev);
				g.setColor(prevColor);
			}
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		this.repaint();
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		this.repaint();
	}

}