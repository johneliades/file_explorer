import java.io.*;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.util.*;

public class FileExplorer {
	private static final String ICONPATH="/icons/";
	private static final boolean showHiddenFiles = false;
	private static File fileToOpen=null;

	public static final Color folderBackgroundColor = new Color(49, 49, 49);
	public static final Color treeBackgroundColor = new Color(32, 32, 32);
	public static final Color topBackgroundColor = new Color(25, 25, 25);
	public static final Color panelHoverColor = new Color(0, 170, 170);
	public static final Color panelSelectionColor = new Color(0, 100, 100);
	public static final Color textSelectionColor = new Color(0, 255, 255);
	public static final Color propertiesColor = new Color(0, 0, 0);

	private static JFrame frame;

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
		//Create and set up the window.
		frame = new JFrame("File Explorer");
		frame.setBackground(folderBackgroundColor);
		frame.getRootPane().setBorder(new EmptyBorder(0, 0, 0, 0));

		java.net.URL imgURL = FileExplorer.class.getResource(ICONPATH + "other/folder.png");
		frame.setIconImage(new ImageIcon(imgURL).getImage());
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
	
	public static JFrame getFrame() {
		return frame;
	}

	private static ArrayList<String> getResourceFiles(String path) throws IOException {
		ArrayList<String> filenames = new ArrayList<>();

		try (
			InputStream in = getResourceAsStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;

			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		}

		return filenames;
	}

	private static InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);

		return in == null ? FileExplorer.class.getResourceAsStream(resource) : in;
	}

	private static ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public static Set<String> addExtensions() {
		Set<String> set = new HashSet<>(); 
	
		try {
			ArrayList<String> icons = getResourceFiles(ICONPATH + "extensions");

			for(String temp : icons) {
				String name = temp.replace(".png", "");
				set.add(name);
			}

			return set;
		}
		catch(Exception e) {}

		return null;

	}

}
