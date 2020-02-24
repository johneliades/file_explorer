import javax.swing.*;
import java.awt.Image;
import java.util.*;

public class Utility {
	static SortedSet<ScaledIcon> set = new TreeSet<>();

	static String getExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

		if (i > p) {
			extension = fileName.substring(i+1);
		}

		return extension;
	}

	static ImageIcon getImageFast(String path, int x, int y) {
		Iterator it = set.iterator();
		while (it.hasNext()) {
			ScaledIcon element = (ScaledIcon) it.next();

			if(element.getPath().equals(path) && element.getX()==x 
													&& element.getY()==y)
				return element.getIcon();
		}

		ImageIcon icon = new ImageIcon(path);

		Image img = icon.getImage().getScaledInstance(x, y, 
					Image.SCALE_DEFAULT);

		icon = new ImageIcon(img);
		ScaledIcon scaled = new ScaledIcon(path, icon, x, y);
		set.add(scaled);

		return icon;
	}

}

class ScaledIcon implements Comparable {
	private String path;
	private ImageIcon icon;
	private int x, y;

	ScaledIcon(String path, ImageIcon icon, int x, int y) {
		this.path = path;
		this.icon = icon;
		this.x = x;
		this.y = y;
	}

	@Override
	public int compareTo(Object obj) {
		ScaledIcon emp = (ScaledIcon) obj;

		return path.compareTo(emp.getPath());
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