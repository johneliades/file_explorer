import javax.swing.*;
import java.awt.Image;
import java.util.*;

public class Utility {
	private static HashSet<ScaledIcon> explorer_icons = new HashSet<>();
	private static HashSet<ScaledIcon> images = new HashSet<>();
	private static ScaledIcon last=null;

	static String getExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

		if (i > p) {
			extension = fileName.substring(i+1);
		}

		return extension;
	}

	static ImageIcon getImageFast(String path, int x, int y, boolean explorer) {
		if(last!=null && last.getPath().equals(path) 
				&& last.getX()==x & last.getY()==y)
			return last.getIcon();

		Iterator it;

		if(explorer)
			it = explorer_icons.iterator();
		else {
			it = images.iterator();
		}
		
		while (it.hasNext()) {
			ScaledIcon element = (ScaledIcon) it.next();

			if(element.getPath().equals(path) && element.getX()==x 
													&& element.getY()==y)
				return element.getIcon();
		}

		ImageIcon icon = new ImageIcon(path);

		Image img = icon.getImage().getScaledInstance(x, y, 
					Image.SCALE_SMOOTH);

		icon = new ImageIcon(img);
		ScaledIcon scaled = new ScaledIcon(path, icon, x, y);
		if(explorer)
			explorer_icons.add(scaled);
		else
			images.add(scaled);
		
		last = scaled;

		return icon;
	}
}

class ScaledIcon {
	private String path;
	private ImageIcon icon;
	private int x, y;

	ScaledIcon(String path, ImageIcon icon, int x, int y) {
		this.path = path;
		this.icon = icon;
		this.x = x;
		this.y = y;
	}

	String getPath() {
		return path;
	}

	ImageIcon getIcon() {
		return icon;
	}

	int getX() {
		return x;
	}

	int getY() {
		return y;
	}
}