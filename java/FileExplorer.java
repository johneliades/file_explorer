import java.io.File;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.util.*;

public class FileExplorer {
	//Optionally set the look and feel.
	private static final boolean useSystemLookAndFeel = false;
	private static final String ICONPATH="./icons/"; // path-until-src/src/hw4/icons/
	private static final boolean showHiddenFiles = false;
	private static File fileToOpen=null;

	public static void main(String[] args) {

		if(args.length>1)
			return;

		if(args.length==1) {
			fileToOpen = new File(args[0]);
			if(!fileToOpen.exists())
				return;
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createAndShowGUI(fileToOpen);
			}
		});
	}

	/**
	 * Create the GUI and show it.	For thread safety,
	 * this method should be invoked from the
	 * event dispatch thread.
	 */
	private static void createAndShowGUI(File file) {
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
		frame.setBackground(new Color(53, 53, 53));
		frame.getRootPane().setBorder(new EmptyBorder(0, 0, 0, 0));

		frame.setIconImage(new ImageIcon(ICONPATH + "other/folder.png").getImage());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Gets screen's Dimensions
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int windowHeight=(int) screenSize.getHeight()*3/4;
		int windowWidth=(int) screenSize.getWidth()*3/4;

		//Set Window's dimensions
		frame.setSize(windowWidth, windowHeight);

		//Set Window's location
		frame.setLocation((screenSize.width-windowWidth)/2, 
					(screenSize.height-windowHeight)/2);

		//Set window layout manager
		frame.setLayout(new BorderLayout());
		
		//Set Menu Bar
	//	frame.setJMenuBar(new MenuBar());

		//Add content to the window.
		frame.add(new TopPanel(), BorderLayout.NORTH);
		frame.add(new MainWindow(file), BorderLayout.CENTER);
	
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
	
		File path = new File(ICONPATH + "extensions");
		File icons[] = path.listFiles();
		for(File temp : icons) {
			String name = temp.getName().replace(".png", "");
			set.add(name);
		}

		return set;
	}

}
