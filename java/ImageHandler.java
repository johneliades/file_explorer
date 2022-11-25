import javax.swing.*;
import java.awt.Image;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ImageHandler {
	private static Set<ScaledIcon> explorer_icons = 
		ConcurrentHashMap.newKeySet();
	private static Set<ScaledIcon> images = 
		ConcurrentHashMap.newKeySet();
	private static ScaledIcon last=null;

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
				&& element.getY()==y) {
				
				return element.getIcon();
			}
		}

		ImageIcon icon;
		if(explorer) {
			java.net.URL imgURL = FileExplorer.class.getResource(path);
			if(imgURL==null)
				return null;
			icon = new ImageIcon(imgURL);
		}
		else
			icon = new ImageIcon(path);

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

	static void clearPathIcons(String path) {
		Iterator it;
			
		if(path.contains("/"))
			path = path + "/";
		else if(path.contains("\\"))
			path = path + "\\";
			
		it = images.iterator();
		
		while (it.hasNext()) {
			ScaledIcon element = (ScaledIcon) it.next();

			if(element.getPath().contains(path) && path.length()!=1) {
				String remnant = element.getPath().replace(path, "");

				if(!remnant.contains("/") && !remnant.contains("\\"))
					images.remove(element);
			}
		}
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