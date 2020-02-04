import javax.swing.*;
import java.awt.*;
import java.util.*;

public class FileExplorer {
	//Optionally set the look and feel.
	private static final boolean useSystemLookAndFeel = false;
	private static final String ICONPATH="./icons/"; // path-until-src/src/hw4/icons/
	private static final boolean showHiddenFiles = false;

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Create the GUI and show it.	For thread safety,
	 * this method should be invoked from the
	 * event dispatch thread.
	 */
	private static void createAndShowGUI() {
		if (useSystemLookAndFeel) {
			try {
				UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
				System.err.println("Couldn't use system look and feel.");
			}
		}

		//Create and set up the window.
		JFrame frame = new JFrame("File Explorer");
		frame.setIconImage(new ImageIcon(ICONPATH + "extensions/folder.png").getImage());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Gets screen's Dimensions
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int windowHeight=(int) screenSize.getHeight()*3/4;
		int windowWidth=(int) screenSize.getWidth()*3/4;

		//Set Window's dimensions
		frame.setSize(windowWidth, windowHeight);

		//Set Window's location
		frame.setLocation((screenSize.width-windowWidth)/2, (screenSize.height-windowHeight)/2);

		//Set window layout manager
		frame.setLayout(new BorderLayout());

		//Set Menu Bar
		frame.setJMenuBar(new MenuBar());

		//Add content to the window.
		frame.add(new TopPanel(), BorderLayout.NORTH);
		frame.add(new MainWindow(), BorderLayout.CENTER);
	
		//Display the window.
		frame.setVisible(true);
	}

	public static String getIconPath() {
		return ICONPATH;
	}

	public static boolean getHiddenFilesOption() {
		return showHiddenFiles;
	}
	
	public static Set<String> addExtensions() {
		Set<String> set = new HashSet<>(); 
	
		set.add("3gp"); 
		set.add("audio"); 
		set.add("avi"); 
		set.add("bmp"); 
		set.add("class"); 
		set.add("css"); 
		set.add("doc"); 
		set.add("docx"); 
		set.add("exe"); 
		set.add("gz"); 
		set.add("htm"); 
		set.add("html"); 
		set.add("iso"); 
		set.add("java"); 
		set.add("json"); 
		set.add("log");
		set.add("mkv"); 
		set.add("mp3"); 
		set.add("mp4"); 
		set.add("ods"); 
		set.add("odt"); 
		set.add("ogg"); 
		set.add("pdf"); 
		set.add("ppt"); 
		set.add("tar"); 
		set.add("tgz"); 
		set.add("txt"); 
		set.add("video"); 
		set.add("wav"); 
		set.add("wmv"); 
		set.add("xlsx"); 
		set.add("xlx"); 
		set.add("xml"); 
		set.add("zip"); 

		return set;
	}

}