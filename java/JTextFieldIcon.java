import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class JTextFieldIcon extends JTextField {
	private JTextField jtf;
	private Icon icon;
	private Insets dummyInsets;
	private Color borderColor = Color.WHITE;

	public JTextFieldIcon(JTextField jtf, ImageIcon icon){
		this.jtf = jtf;
		setIcon(icon);

		Border border = UIManager.getBorder("TextField.border");
		JTextField dummy = new JTextField();
		this.dummyInsets = border.getBorderInsets(dummy);
	}

	public void setIcon(Icon newIcon){
		this.icon = newIcon;
		repaint();
	}

	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
		repaint();
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

	protected void paintBorder(Graphics g) {
		g.setColor(borderColor);
		g.drawRect(0, 0, getWidth()-1, getHeight()-1);
	}
}