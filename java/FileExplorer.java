import java.io.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.lang.management.ManagementFactory;

public class FileExplorer {
	private static final String ICONPATH="/icons/";
	private static final boolean showHiddenFiles = false;
	private static File fileToOpen=null;

	public static final Color folderBackgroundColor = new Color(49, 49, 49);
	public static final Color treeBackgroundColor = new Color(32, 32, 32);
	public static final Color topBackgroundColor = new Color(25, 25, 25);
	public static final Color exitPanelBackgroundColor = new Color(10, 10, 10);
	public static final Color panelHoverColor = new Color(0, 170, 170);
	public static final Color panelSelectionColor = new Color(0, 100, 100);
	public static final Color textSelectionColor = new Color(0, 255, 255);
	public static final Color propertiesColor = new Color(0, 0, 0);

	private static final String windowsTopName="This PC";

	private static JFrame frame;

	/*
	
	============================================================
	                  Top Window variables
	============================================================

	*/

	private static final int exitPanelHeight = 33;
	private static final int navHeight = 25;

	private static JButton buttonBack, buttonForward;
	private static JTextFieldIcon searchField, navigationField;
	private static JPanel buttonField;
	private static String searchQuery = "";


	/*
	
	============================================================
	                  Main Window variables
	============================================================

	*/

	static private java.util.Stack<DefaultMutableTreeNode> history = 
		new java.util.Stack<DefaultMutableTreeNode>();

	static public java.util.Stack<DefaultMutableTreeNode> futureHistory = 
		new java.util.Stack<DefaultMutableTreeNode>();

	private static DefaultMutableTreeNode top;
	private static JPanel folder;
	private static JTree tree;

	/*
	
	============================================================
	                  Folder Panel variables
	============================================================

	*/

	static Set<String> iconSet = FileExplorer.addExtensions();

	private static JPanel lastPanelSelected; 
	private static HashMap<JPanel, DefaultMutableTreeNode> mapPanelNode = 
		new HashMap<JPanel, DefaultMutableTreeNode>();

	private static java.util.List<JPanel> selectedList = 
		new java.util.ArrayList<JPanel>();
	
	private static java.util.List<DefaultMutableTreeNode> clipboard = 
		new java.util.ArrayList<DefaultMutableTreeNode>();
	private static String operation = "";

	private static Executor executor = Executors.newSingleThreadExecutor();

	private static int x, y, x2, y2;

	/*
	
	============================================================
	                  Tree Panel variables
	============================================================

	*/

	private static DefaultMutableTreeNode lastTreeNodeOpened=null;

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

		frame.setUndecorated(true);

		ComponentResizer cr = new ComponentResizer();
		cr.registerComponent(frame);
		cr.setSnapSize(new Dimension(10, 10));

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

		JPanel topWindow = getTopWindow();
		topWindow.setBorder(new EmptyBorder(5, 0, 0, 0));
		topWindow.setBackground(exitPanelBackgroundColor);

		//Add content to the window.
		frame.add(topWindow, BorderLayout.NORTH);

		JPanel FileExplorer = getMainWindow(file);
		FileExplorer.setBorder(new EmptyBorder(0, 5, 5, 5));
		FileExplorer.setBackground(topBackgroundColor);

		frame.add(FileExplorer, BorderLayout.CENTER);
	
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


	/*
	
	============================================================
	                      Main Window
	============================================================

	*/

	static JPanel getMainWindow(File fileToOpen) {
		JPanel newPanel = new JPanel(new GridLayout(1, 0));

		UIManager UI=new UIManager();
		UI.put("OptionPane.background", FileExplorer.propertiesColor);
		UI.put("Panel.background", FileExplorer.propertiesColor);
		UI.put("OptionPane.messageForeground", Color.WHITE);

		//Create the nodes.
		File roots[]=File.listRoots();
		
		if(roots.length==1)
			top = new DefaultMutableTreeNode(roots[0]);
		else {
			top = new DefaultMutableTreeNode(new File(windowsTopName) {
				@Override
				public String toString() {
					if(this.getPath().compareTo("/")!=0)
						return this.getName();
					else
						return this.getPath();
				}
			});
			for (File root : roots) {
				top.add(new DefaultMutableTreeNode(root));
			}
		}

		FileExplorer.setLastTreeNodeOpened(top);

		//Create a tree that allows one selection at a time.
		tree = getTreePanel(top);
	
		BasicTreeUI basicTreeUI = (BasicTreeUI) tree.getUI();
		basicTreeUI.setLeftChildIndent(0);
		basicTreeUI.setRightChildIndent(12);
		basicTreeUI.setCollapsedIcon(Utility.getImageFast(ICONPATH + 
								"other/collapsed.png", 9, 9, true));
		basicTreeUI.setExpandedIcon(Utility.getImageFast(ICONPATH + 
			"other/expanded.png", 9, 9, true));

		//Create the scroll pane and add the tree to it. 
		JScrollPane treeView = new JScrollPane(tree);

		MyScrollBarUI scrollbar = new MyScrollBarUI();
		treeView.getVerticalScrollBar().setUI(scrollbar);
			treeView.getVerticalScrollBar().addMouseListener(new MouseAdapter() {
			boolean pressed = false, entered = false;

			@Override
			public void mousePressed(final java.awt.event.MouseEvent evt) {
				pressed = true;
				treeView.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
				treeView.getVerticalScrollBar().revalidate();
				treeView.getVerticalScrollBar().repaint();
			}

			@Override
			public void mouseReleased(final java.awt.event.MouseEvent evt) {
				pressed = false;
				if(!entered) {
					treeView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
					treeView.getVerticalScrollBar().revalidate();
					treeView.getVerticalScrollBar().repaint();
				}
			}

			@Override
			public void mouseEntered(final java.awt.event.MouseEvent evt) {
				entered = true;
				treeView.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
				treeView.getVerticalScrollBar().revalidate();
				treeView.getVerticalScrollBar().repaint();
			}

			@Override
			public void mouseExited(final java.awt.event.MouseEvent evt) {
				entered = false;
				if(!pressed) {
					treeView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
					treeView.getVerticalScrollBar().revalidate();
					treeView.getVerticalScrollBar().repaint();
				}
			}
		});

		treeView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		treeView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
		treeView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		treeView.setBorder(new EmptyBorder(0, 0, 0, 0));

		folder = getFolderPanel();
		JScrollPane folderView = new JScrollPane(folder);
		
		scrollbar = new MyScrollBarUI();
		folderView.getVerticalScrollBar().setUI(scrollbar);
		folderView.getVerticalScrollBar().addMouseListener(new MouseAdapter() {
			boolean pressed = false;
			boolean entered = false;

			@Override
			public void mousePressed(final java.awt.event.MouseEvent evt) {
				pressed = true;
				folderView.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
				folderView.getVerticalScrollBar().revalidate();
				folderView.getVerticalScrollBar().repaint();
			}

			@Override
			public void mouseReleased(final java.awt.event.MouseEvent evt) {
				pressed = false;
				if(!entered) {
					folderView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
					folderView.getVerticalScrollBar().revalidate();
					folderView.getVerticalScrollBar().repaint();
				}
			}

			@Override
			public void mouseEntered(final java.awt.event.MouseEvent evt) {
				entered = true;
				folderView.getVerticalScrollBar().setPreferredSize(new Dimension(11, 0));
				folderView.getVerticalScrollBar().revalidate();
				folderView.getVerticalScrollBar().repaint();
			}

			@Override
			public void mouseExited(final java.awt.event.MouseEvent evt) {
				entered = false;
				if(!pressed) {
					folderView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
					folderView.getVerticalScrollBar().revalidate();
					folderView.getVerticalScrollBar().repaint();
				}
			}
		});

		folderView.getVerticalScrollBar().setUnitIncrement(16);
		folderView.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		folderView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
		folderView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		folderView.setBorder(new EmptyBorder(0, 0, 0, 0));

		if(roots.length==1)
			FileExplorer.createNodes(top);
		else {
			int numChild=tree.getModel().getChildCount(top);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) 
						tree.getModel().getChild(top, i);
				FileExplorer.createNodes(current);
			}
		}

		if(fileToOpen==null) {
			selectDirectory(top);
		}
		else {
			java.util.Stack<String> pathComponents = 
				new java.util.Stack<String>();

			while(true) {
				if(fileToOpen.getParentFile() == null)
					break;
				pathComponents.add(fileToOpen.getName());
				fileToOpen = fileToOpen.getParentFile();
			} 
			
			pathComponents.add(fileToOpen.getPath().replace("\\", ""));
			//PathComponents now contains the path 
			//components starting from root

			loadPath(top, pathComponents);
		}

		folderView.setMinimumSize(new Dimension(400, 50));
		treeView.setMinimumSize(new Dimension(250, 50));
		treeView.getVerticalScrollBar().setValue(0);

		//Add the scroll panes to a split pane.
		JSplitPaneWithZeroSizeDivider splitPane = new 
					JSplitPaneWithZeroSizeDivider(
						JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(folderView);
		splitPane.setBorder(new EmptyBorder(0, 0, 0, 0));

		// Mouse back and forward
		if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() && 
				MouseInfo.getNumberOfButtons() > 3) {
			
			Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
				if (event instanceof MouseEvent) {
					MouseEvent mouseEvent = (MouseEvent) event;
					if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED &&
						mouseEvent.getButton() > 3) {
						
						if (mouseEvent.getButton() == 4) {
							FileExplorer.historyBack();
							FileExplorer.focusLast();
						} else if (mouseEvent.getButton() == 5) {
							FileExplorer.historyForward();
							FileExplorer.focusLast();
						}
					}
				}
			}, AWTEvent.MOUSE_EVENT_MASK);
		}

		//Add the split pane to this panel.
		newPanel.add(splitPane);

		return newPanel;
	}

	static void loadPath(DefaultMutableTreeNode top, 
		java.util.Stack<String> pathComponents) {	
		
		String current = pathComponents.pop();
		if(!((File) top.getUserObject()).getPath().equals(windowsTopName)) {
			if(!pathComponents.empty()) {
				current = pathComponents.pop();
				//If on Unix and path was not the root double pop
			}
			else {
				selectDirectory(top);

				return;
			}
		}

		DefaultMutableTreeNode currentTop = top;

		end:
		while(true) {
			int i;
			int numChild=tree.getModel().getChildCount(currentTop);
			for(i=0; i<numChild; i++) { 
				DefaultMutableTreeNode temp=(DefaultMutableTreeNode) 
						tree.getModel().getChild(currentTop, i);

				String nodeName = temp.getUserObject().
							toString().replace("\\", "");
				if(nodeName.equals(current)) {
					if(pathComponents.empty()) {
						//Found path to open
						selectDirectory(temp);
						break end;
					}
					current = pathComponents.pop();
					currentTop = temp;
					FileExplorer.createNodes(currentTop);
					break;
				}
			}
			if(i==numChild)
				break;
		}
	}

	public static Boolean isInvalidFileName(String fileName) {
		ArrayList<String> list = new ArrayList<String>();
		list.add("CON");
		list.add("PRN");
		list.add("AUX");
		list.add("NUL");
		list.add("COM1");
		list.add("COM2");
		list.add("COM3");
		list.add("COM4");
		list.add("COM5");
		list.add("COM6");
		list.add("COM7");
		list.add("COM8");
		list.add("COM9");
		list.add("LPT1");
		list.add("LPT2");
		list.add("LPT3");
		list.add("LPT4");
		list.add("LPT5");
		list.add("LPT6");
		list.add("LPT7");
		list.add("LPT8");
		list.add("LPT9");

		Iterator itr=list.iterator();
		while(itr.hasNext()) {
			String next = (String) itr.next();
			boolean isFound = fileName.indexOf(next) != -1 ? true : false;
			if(isFound) {
				return true;
			}
			
			isFound = fileName.indexOf(next.toLowerCase()) != -1 ? true : false;
			if(isFound) {
				return true;
			}
		}

		return false;
	}

	static void refresh(DefaultMutableTreeNode node) {
		JTree tree = FileExplorer.getTree();

		Utility.clearPathIcons(((File) node.getUserObject()).getPath());
		node.removeAllChildren();
		DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
		defMod1.reload();
	
		TreePath path = new TreePath(node.getPath());
		if(path.toString().equals("[" + windowsTopName + "]")) {
			//Create root nodes.
			File roots[]=File.listRoots();

			for (File root : roots) {
				node.add(new DefaultMutableTreeNode(root));
			}

			int numChild=defMod1.getChildCount(node);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) 
												defMod1.getChild(node, i);
				FileExplorer.createNodes(current);
			}
		}
		selectDirectory(node);

		JPanel folder = getFolder();
		JPanel current = FileExplorer.getLastPanelSelected();

		if(current!=null)
			for(Component comp : folder.getComponents()) {
				if(current.getName().equals(comp.getName())) {
					FileExplorer.selectPanel((JPanel) comp, true);
					break;
				}
			}
	}

	static void rename(DefaultMutableTreeNode panelNode) {
		JTree tree = FileExplorer.getTree();
		DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();
		
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();
		String nameNew,	nameOld="", path;

		File nodeFile = (File) panelNode.getUserObject();

		ImageIcon img=null;
		Image folderImg;
		int i;

		nameOld = nodeFile.getName();

		if(nameOld.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(nodeFile);
			nameOld = description + " (" + nodeFile.getPath().replace("\\", "") + ")";
		}

		File f = new File(filePath + "/" + nameOld);

		if(f.exists() && f.canWrite()) {
			img = Utility.getImageFast(ICONPATH + "other/rename.png", 50, 50, true);

			nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name",
									"Rename", JOptionPane.INFORMATION_MESSAGE,
									img, null, nameOld);

			
			if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals("")) {
				return;
			}

			if(isInvalidFileName(nameNew)) {
				JOptionPane.showMessageDialog(null, "Rename failed! Invalid name");
				return;
			}

			String invalidStripped = 
				nameNew.replaceAll("[\\\\/:*?\"<>|]", "_");
			if(!nameNew.equals(invalidStripped)) {
				JOptionPane.showMessageDialog(null, 
					"Replaced invalid characters with \"_\"");
				nameNew = invalidStripped;
			}

			File file2 = new File(filePath + "/" + nameNew);

			if(file2.exists()) {
				JOptionPane.showMessageDialog(null, "Rename failed! File exists");
				return;
			}

			if(f.isDirectory()) {
				panelNode.removeFromParent();
			}

			boolean success = f.renameTo(file2);

			if (!success) {
				JOptionPane.showMessageDialog(null, "Rename failed!");
				return;
			}
		}
		else {
			if(!f.exists()) {
				JOptionPane.showMessageDialog(null, "File doesn't exist!");
				return;
			}
			else if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				return;
			}

			JOptionPane.showMessageDialog(null, "Rename failed!");
			return;
		}

		DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
		defMod1.reload();

		selectDirectory(lastTreeNodeOpened);
		FileExplorer.focusLast();
	}

	static void delete(DefaultMutableTreeNode panelNode) {
		DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();

		File nodeFile;
		 
		nodeFile = ((File) panelNode.getUserObject());
	
		ImageIcon img=null;
		int i;

		String name = nodeFile.getName();
		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(nodeFile);
			name = description + " (" + nodeFile.getPath().replace("\\", "") + ")";
		}

		File f = new File(filePath + "/" + name);

		img = Utility.getImageFast(ICONPATH + "other/delete.png", 50, 50, true);

		if(f.exists() && f.isFile() && f.canWrite()){
			int input = JOptionPane.showConfirmDialog(null, "Deleting file \"" + 
				name + "\" ?", "Any deletion is permanent", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, 
				img);
		
			if(input==JOptionPane.CANCEL_OPTION || input==-1) {
				return;
			}

			try {
				f.delete();
			}
			catch(Exception e) {
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
		else if(f.exists() && f.isDirectory() && f.canWrite()) {
			int input = JOptionPane.showConfirmDialog(null, "Deleting folder \"" 
				+ name + "\" ?", "Any deletion is permanent", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, 
				img);
		
			if(input==JOptionPane.CANCEL_OPTION || input==-1) {
				return;
			}

			panelNode.removeAllChildren();
			panelNode.removeFromParent();
			removeDirectory(f);
			f.delete(); 

			JTree tree = FileExplorer.getTree();
			DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
			defMod1.reload();
		}
		else {
			if(!f.exists()) {
				JOptionPane.showMessageDialog(null, "File doesn't exist!");
				return;
			}
			else if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				return;
			}
			
			JOptionPane.showMessageDialog(null, "Deletion failed!");
			return;
		}

		selectDirectory(lastTreeNodeOpened);
		FileExplorer.focusLast();
	}

	public static boolean pasteFile(File source, File destination, String op) throws IOException {
		File newFile = new File(destination.getPath() + File.separator + source.getName());
		boolean success = true;

		if(op == "cut") {
			source.renameTo(newFile);
		}
		else if(op == "copy") {
			if(source.isDirectory()) {
				if(!newFile.exists())
					newFile.mkdir();
			
				String files[] = source.list();
				for (String file : files) {
					File srcFile = new File(source, file);
					success = pasteFile(srcFile, newFile, op);
					
					File destFile = new File(newFile, file);
					if(!hashFile(srcFile, "MD5").equals(hashFile(destFile, "MD5"))) {
						System.out.println("copy failed " + srcFile.toPath() + " " + destFile.toPath());
						return false;
					}
				}
			}
			else {
				Files.copy(source.toPath(), newFile.toPath());
				if(!hashFile(newFile, "MD5").equals(hashFile(source, "MD5"))) {
					System.out.println("copy failed " + source.toPath() + " " + newFile.toPath());
					return false;
				}
			}	
		}

		return success;
	}

	public static String hashStream(BufferedInputStream bis, String type) {
		byte[] buffer= new byte[4096];
		int count;

		try {
			MessageDigest digest = MessageDigest.getInstance(type);
			
			while ((count = bis.read(buffer)) > 0) {
				digest.update(buffer, 0, count);
			}
			bis.close();

			byte[] hash = digest.digest();
	
			// Conver hash to hex string
			char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

			char[] hexChars = new char[hash.length * 2];
			for (int j = 0; j < hash.length; j++) {
				int v = hash[j] & 0xFF;
				hexChars[j * 2] = HEX_ARRAY[v >>> 4];
				hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
			}
			return new String(hexChars);
		}
		catch(Exception exc) {

		}

		return null;
	}

	public static String hashFile(File file, String type) {

		if(file.isDirectory()) {
			File[] files = file.listFiles();
			if(files == null)
				return "";
			
			String total = "";

			Arrays.sort(files);

			for(File current : files)
				total += hashFile(current, type);

			InputStream stream = new ByteArrayInputStream(
				total.getBytes(StandardCharsets.UTF_8));

			BufferedInputStream bis = new BufferedInputStream(stream);
			return hashStream(bis, type);
		}
		else {
			try {
				BufferedInputStream bis = new BufferedInputStream(new 
					FileInputStream(file));

				return hashStream(bis, type);
			}
			catch(Exception e) {

			}
		}
		return "";
	}

	public static String convertBytes(long bytes, boolean show_bytes) {
		long fileSizeInKB=0, fileSizeInMB=0, fileSizeInGB=0;
		if(bytes!=0) {
			fileSizeInKB = bytes / 1024;
			fileSizeInMB = fileSizeInKB / 1024;
			fileSizeInGB = fileSizeInMB / 1024;
		}

		String size="";
		if(bytes!=0) {
			size = bytes + " B";
		}

		if(fileSizeInKB!=0) {
			double tempSize = (double) bytes/1024;
			size = String.format("%.2f", tempSize) + " KB";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}
		
		if(fileSizeInMB!=0) {
			double tempSize = (double) bytes/1024/1024;
			size = String.format("%.2f", tempSize) + " MB ";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}

		if(fileSizeInGB!=0) {
			double tempSize = (double) bytes/1024/1024/1024;
			size = String.format("%.2f", tempSize) + " GB ";
			if(show_bytes)
				size += "  ( " + bytes + " B )";
		}

		return size;
	}

	public static long[] folderSize(File directory) {
		long[] total = new long[3];
		total[0] = 0; //length
		total[1] = 0; //files
		total[2] = 0; //folders

		try {
			for (File file : directory.listFiles()) {
				if(!Files.isSymbolicLink(file.toPath())) {
					if(file.isFile()) {
						total[0] += file.length();
						total[1] += 1;
					}
					else {
						long[] temp = folderSize(file);
	
						total[0] += temp[0];
						total[1] += temp[1];
						total[2] += temp[2] + 1;
					}
				}
			}
		}
		catch(Exception e) {
			return total;
		}

		return total;
	}

	public static void properties(File file) {
		JFrame frame = FileExplorer.getFrame();

		JDialog dialog = new JDialog(frame, "Properties"); 
		dialog.setUndecorated(true);

		JPanel panel = new MotionPanel(dialog); 
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(20, 20, 20, 20));

		ImageIcon img = Utility.getImageFast(ICONPATH + 
					"other/info.png", 50, 50, true);
		JLabel label = new JLabel(img); 

		Font currentFont = label.getFont();
		Font bigFont = new Font(currentFont.getName(), 
				currentFont.getStyle(), currentFont.getSize() + 2);

		label.setForeground(Color.WHITE);
		panel.add(label);
		
		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		dialog.add(panel); 
		dialog.setResizable(false);
		dialog.setSize(200, 200); 
		dialog.setAlwaysOnTop(true);
		dialog.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
			}

			public void windowLostFocus(WindowEvent e) {
				dialog.setVisible(false);
			}
		});

		String fileName = file.getName();
		if(fileName==null || fileName.compareTo("")==0)
			fileName = file.getPath();
		
		JTextField field = new JTextField("Name: " + fileName);
		field.setEditable(false);
		field.setBorder(null);
		field.setForeground(Color.WHITE);
		field.setBackground(UIManager.getColor(Color.BLACK));
		field.setFont(bigFont);
		panel.add(field);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		SimpleDateFormat sdf = new SimpleDateFormat(
										"dd/MM/yyyy HH:mm:ss");
		
		label = new JLabel("Modified: " + sdf.format(file.lastModified())); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		panel.add(label);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		
		JCheckBox execute = new JCheckBox("Execute", file.canExecute());
		execute.setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked.png", 15, 15, true));
		execute.setSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked.png", 15, 15, true));
		execute.setDisabledIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked_disabled.png", 15, 15, true));
		execute.setDisabledSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked_disabled.png", 15, 15, true));
		execute.setPressedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/half_checked.png", 15, 15, true));
	
		execute.setBackground(FileExplorer.propertiesColor);
		execute.setForeground(Color.WHITE);
		execute.setFocusPainted(false);
		if(file.setExecutable(!file.canExecute())){
		   file.setExecutable(!file.canExecute());
		}
		else{
			execute.setEnabled(false);
		}
		execute.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setExecutable(true);
				else
					file.setExecutable(false);
			}
		});
		
		JCheckBox read = new JCheckBox("Read", file.canRead());
		read.setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked.png", 15, 15, true));
		read.setSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked.png", 15, 15, true));
		read.setDisabledIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked_disabled.png", 15, 15, true));
		read.setDisabledSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked_disabled.png", 15, 15, true));
		read.setPressedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/half_checked.png", 15, 15, true));

		read.setBackground(FileExplorer.propertiesColor);
		read.setForeground(Color.WHITE);
		read.setFocusPainted(false);
		if(file.setReadable(!file.canRead())){
			file.setReadable(!file.canRead());
		}
		else{
			read.setEnabled(false);
		}
		read.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setReadable(true);
				else
					file.setReadable(false);
			}
		});

		JCheckBox write = new JCheckBox("Write", file.canWrite());
		write.setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked.png", 15, 15, true));
		write.setSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked.png", 15, 15, true));
		write.setDisabledIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/unchecked_disabled.png", 15, 15, true));
		write.setDisabledSelectedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/checked_disabled.png", 15, 15, true));
		write.setPressedIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/half_checked.png", 15, 15, true));

		write.setBackground(FileExplorer.propertiesColor);
		write.setForeground(Color.WHITE);
		write.setFocusPainted(false);
		if(file.setWritable(!file.canWrite())){
			file.setWritable(!file.canWrite());
		}
		else{
			write.setEnabled(false);
		}
		write.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setWritable(true);
				else
					file.setWritable(false);
			}
		});

		panel.add(execute);
		panel.add(read);
		panel.add(write);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		LookAndFeel previousLF = UIManager.getLookAndFeel();
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : 
				javax.swing.UIManager.getInstalledLookAndFeels()) {
				
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}

			JButton button = new JButton("MD5");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JProgressBar progress = new JProgressBar();
					progress.setIndeterminate(true);
					progress.setBackground(FileExplorer.propertiesColor);
					progress.setForeground(Color.CYAN);
					progress.setBorderPainted(false);

					JButton buttonThatWasClicked = (JButton) e.getSource();
					panel.add(progress, panel.getComponentZOrder(buttonThatWasClicked));
					panel.remove(buttonThatWasClicked);
				
					dialog.pack();

					Executor calcMD5 = Executors.newSingleThreadExecutor();
					calcMD5.execute(new Runnable() {
						public void run() { 
							String hash=hashFile(file, "MD5");
							if(hash==null)
								hash="";
							JTextField field = new JTextField(hash);
							field.setEditable(false);
							field.setBorder(null);
							field.setForeground(Color.WHITE);
							field.setBackground(UIManager.getColor("Panel.background"));
							field.setFont(bigFont);
							if(hash.length()!=0) {
								panel.add(field, panel.getComponentZOrder(progress));
								panel.remove(progress);
				
								dialog.pack();
							}
						}
					});
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);

			panel.add(Box.createRigidArea(new Dimension(0, 3)));

			button = new JButton("SHA1");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JProgressBar progress = new JProgressBar();
					progress.setIndeterminate(true);
					progress.setBackground(FileExplorer.propertiesColor);
					progress.setForeground(Color.CYAN);
					progress.setBorderPainted(false);

					JButton buttonThatWasClicked = (JButton) e.getSource();
					panel.add(progress, panel.getComponentZOrder(buttonThatWasClicked));
					panel.remove(buttonThatWasClicked);
				
					dialog.pack();

					Executor calcSHA = Executors.newSingleThreadExecutor();
					calcSHA.execute(new Runnable() {
						public void run() { 
							String hash=hashFile(file, "SHA-1");
							if(hash==null)
								hash="";
							JTextField field = new JTextField(hash);
							field.setEditable(false);
							field.setBorder(null);
							field.setForeground(Color.WHITE);
							field.setBackground(UIManager.getColor("Panel.background"));
							field.setFont(bigFont);
							if(hash.length()!=0) {
								panel.add(field, panel.getComponentZOrder(progress));
								panel.remove(progress);
				
								dialog.pack();
							}
						}
					});
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);	

			panel.add(Box.createRigidArea(new Dimension(0, 3)));

			button = new JButton("SHA256");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JProgressBar progress = new JProgressBar();
					progress.setIndeterminate(true);
					progress.setBackground(FileExplorer.propertiesColor);
					progress.setForeground(Color.CYAN);
					progress.setBorderPainted(false);

					JButton buttonThatWasClicked = (JButton) e.getSource();
					panel.add(progress, panel.getComponentZOrder(buttonThatWasClicked));
					panel.remove(buttonThatWasClicked);
				
					dialog.pack();

					Executor calcSHA = Executors.newSingleThreadExecutor();
					calcSHA.execute(new Runnable() {
						public void run() { 
							String hash=hashFile(file, "SHA-256");
							if(hash==null)
								hash="";
							JTextField field = new JTextField(hash);
							field.setEditable(false);
							field.setBorder(null);
							field.setForeground(Color.WHITE);
							field.setBackground(UIManager.getColor("Panel.background"));
							field.setFont(bigFont);
							if(hash.length()!=0) {
								panel.add(field, panel.getComponentZOrder(progress));
								panel.remove(progress);
				
								dialog.pack();
							}
						}
					});
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);

			UIManager.setLookAndFeel(previousLF);
		}
		catch(Exception e) {}

		long bytes = 0;
		String size = "", avail_space = "";
		boolean calculated = false;

		if(!file.isDirectory()) {
			bytes = file.length();
			size = convertBytes(bytes, true);
			calculated = true;
			panel.add(Box.createRigidArea(new Dimension(0, 20)));
		}
		else if(file.getName().trim().length() == 0) {
			avail_space = convertBytes(file.getFreeSpace(), true);
			size = convertBytes(file.getTotalSpace(), true);
			calculated = true;
			panel.add(Box.createRigidArea(new Dimension(0, 20)));
		}

		label = new JLabel("\nSize: " + size); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		if(size.length()!=0)
			panel.add(label);

		label = new JLabel("\nFree Space: " + avail_space); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		if(avail_space.length()!=0) {
			panel.add(label);

			panel.add(Box.createRigidArea(new Dimension(0, 20)));

			long free = file.getFreeSpace();
			long total = file.getTotalSpace();
			int used = (int) (((total-free)*100)/total);
			JProgressBar bar = new JProgressBar(0, 100);
			bar.setValue(used);			

			bar.setBackground(FileExplorer.folderBackgroundColor);
			if(used>90)
				bar.setForeground(Color.RED);
			else
				bar.setForeground(Color.CYAN);

			bar.setBorderPainted(false);

			panel.add(bar);
		}

		dialog.pack();

		if(!calculated) {
			Executor calcSize = Executors.newSingleThreadExecutor();
			calcSize.execute(new Runnable() {
				public void run() { 
					panel.add(Box.createRigidArea(new Dimension(0, 20)));
					JLabel label = new JLabel("\nSize: Calculating..."); 
					label.setForeground(Color.WHITE);
					label.setFont(bigFont);
					panel.add(label);
					dialog.pack();

					long[] total;
					total = folderSize(file);

					long bytes = total[0];
					long files = total[1];
					long folders = total[2];

					String size="0";
					if(bytes!=0)
						size = convertBytes(bytes, true);

					panel.remove(label);
					dialog.pack();

					label = new JLabel("\nSize: " + size); 
					label.setForeground(Color.WHITE);
					label.setFont(bigFont);
					if(size.length()!=0) {
						panel.add(label);
						dialog.pack();
					}
					
					panel.add(Box.createRigidArea(new Dimension(0, 20)));
		
					label = new JLabel("\nContains: " + files + " Files, " + folders + " Folders"); 
					label.setForeground(Color.WHITE);
					label.setFont(bigFont);
					panel.add(label);
					dialog.pack();
				}
			});
		}

		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	static public void selectDirectory(DefaultMutableTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);
		FileExplorer.setLastTreeNodeOpened(node);
		FileExplorer.showCurrentDirectory(node);	
	}

	static public void enterOrOpen(DefaultMutableTreeNode node) {
		JTree tree = FileExplorer.getTree();
	
		File file = (File) node.getUserObject();

		if(!file.exists()) {
			FileExplorer.findExistingParent(file);
			return;
		}

		if(file.isDirectory()) {
			selectDirectory(node);
		}
		else if(file.isFile()) {
			try {
				Desktop.getDesktop().open(file);
			}
			catch(IOException e) {

			}
		}
	}

	static void removeDirectory(File current) {
		File children[] = current.listFiles();
		
		if(children==null)
			return;
		
		for(File element : children) {
			if(element.isDirectory()) {
				removeDirectory(element);
			}
			element.delete();
		}
	}

	public static DefaultMutableTreeNode getTop() {
		return top;
	}

	public static JPanel getFolder() {
		return folder;
	}

	public static JTree getTree() {
		return tree;
	}

	private static boolean isLastExplorer=true;

	public static void setFocusExplorer() {
		isLastExplorer=true;
	}

	public static void setFocusTree() {
		isLastExplorer=false;
	}

	public static void focusLast() {
		if(isLastExplorer)
			folder.requestFocusInWindow();
		else
			tree.requestFocusInWindow();
	}

	public static void historyPush(DefaultMutableTreeNode node) {
		if(node==null)
			return;

		if(history.empty() || (!history.empty() && history.peek()!=node)) {
			history.push(node);
			FileExplorer.getButtonBack().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/backarrow.png", 23, 23, true));
		}
	}

	public static DefaultMutableTreeNode historyPop() {
		if(!history.empty()) {
			if(history.size()==1) {
				FileExplorer.getButtonBack().setIcon(Utility.getImageFast(
					FileExplorer.getIconPath() + 
						"other/grayedback.png", 23, 23, true));
			}

			return history.pop();
		}

		return null;
	}

	public static void clearFuture() {
		futureHistory.clear();
		FileExplorer.getButtonForward().setIcon(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedforward.png", 23, 23, true));
	}

	public static void futureHistoryPush(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

		if(futureHistory.size()==0) {
			futureHistory.push(lastTreeNodeOpened);		
		}

		if(futureHistory.peek()!=node) {
			futureHistory.push(node);
			FileExplorer.getButtonForward().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/forwardarrow.png", 23, 23, true));
		}
	}

	public static DefaultMutableTreeNode futureHistoryPop() {
		DefaultMutableTreeNode node;

		if(futureHistory.empty()) {
			return null;
		}

		node = futureHistory.pop();
		if(futureHistory.empty()) {
			FileExplorer.getButtonForward().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/grayedforward.png", 23, 23, true));		
		}
		return node;
	}

	/*
	
	============================================================
	                      Folder Panel
	============================================================

	*/

	public static JPanel getFolderPanel() {
		JPanel newPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10)) {
            @Override
            public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				int px = Math.min(x, x2);
				int py = Math.min(y, y2);
				int pw = Math.abs(x-x2);
				int ph = Math.abs(y-y2);
			
				g.setColor(Color.CYAN);
				g.fillRect(px, py, pw, ph);

				g.setColor(Color.WHITE);
				g.drawRect(px, py, pw, ph);	
            }
		};

		x = y = x2 = y2 = -1;

		newPanel.setBackground(FileExplorer.folderBackgroundColor);

		newPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.
													getLastTreeNodeOpened();
				JTree tree = FileExplorer.getTree();
				
				if(event.getButton() == MouseEvent.BUTTON1) {
					x = event.getX();
					y = event.getY();
				}

				newPanel.requestFocusInWindow();
				FileExplorer.setFocusExplorer();

				String filePath = ((File) lastTreeNodeOpened.getUserObject()).
															getPath();

				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					FileExplorer.findExistingParent(f);
					return;
				}

				if(event.getButton() == MouseEvent.BUTTON1) {
					clearPanelSelection();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {	
					JPopupMenu menu = getBackgroundPopupMenu();
					if(tree.getLastSelectedPathComponent()!=null)
						menu.show(event.getComponent(), event.getX(), 
													event.getY());

					clearPanelSelection();
				}
			}
			@Override
			public void mouseReleased(MouseEvent event) {
				if(event.getButton() == MouseEvent.BUTTON1) {
					x2 = event.getX();
					y2 = event.getY();
					newPanel.repaint();

					clearPanelSelection();

					for (int i = 0; i < FileExplorer.getFolder().getComponentCount(); i++) {
						Component current = FileExplorer.getFolder().getComponent(i);
						int curX = current.getX(); 
						int curY = current.getY();
						int curWidth = current.getWidth();
						int curHeight = current.getHeight();
					
						Rectangle rect1 = new Rectangle(curX, curY, curWidth, curHeight);

						int px = Math.min(x, x2);
						int py = Math.min(y, y2);
						int pw = Math.abs(x-x2);
						int ph = Math.abs(y-y2);

						Rectangle rect2 = new Rectangle(px, py, pw, ph);

						if(overlaps(rect1, rect2))
							selectPanel((JPanel) current, false);
					}
					x = y = x2 = y2 = -1; 
					newPanel.repaint();
				}
			}

			public boolean overlaps(Rectangle z, Rectangle r) {
				return z.x < r.x + r.width && z.x + z.width > r.x && 
					z.y < r.y + r.height && z.y + z.height > r.y;
			}
		});

		newPanel.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent event) {
				if(SwingUtilities.isLeftMouseButton(event)) {
					x2 = event.getX();
					y2 = event.getY();
					newPanel.repaint();
		
					clearPanelSelection();

					for (int i = 0; i < FileExplorer.getFolder().getComponentCount(); i++) {
						Component current = FileExplorer.getFolder().getComponent(i);
						int curX = current.getX(); 
						int curY = current.getY();
						int curWidth = current.getWidth();
						int curHeight = current.getHeight();
					
						Rectangle rect1 = new Rectangle(curX, curY, curWidth, curHeight);

						int px = Math.min(x, x2);
						int py = Math.min(y, y2);
						int pw = Math.abs(x-x2);
						int ph = Math.abs(y-y2);

						Rectangle rect2 = new Rectangle(px, py, pw, ph);

						if(overlaps(rect1, rect2)) {
							selectPanel((JPanel) current, false);
						}
					}
				}
			}

			public boolean overlaps(Rectangle z, Rectangle r) {
				return z.x < r.x + r.width && z.x + z.width > r.x && 
					z.y < r.y + r.height && z.y + z.height > r.y;
			}
		});

		newPanel.getActionMap().put("select all", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					for(JPanel current : mapPanelNode.keySet())
						selectPanel(current, false);
				}
			});

		newPanel.getActionMap().put("paste", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for(DefaultMutableTreeNode current : clipboard) {
					try {
						File selected = (File) FileExplorer.getLastTreeNodeOpened().getUserObject();

						FileExplorer.pasteFile((File) current.getUserObject(), selected, operation);
					}
					catch(Exception except) {

					}
				}

				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				clipboard.clear();
				operation = "";
			}
		});

		newPanel.getActionMap().put("history back", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyBack(); 
				FileExplorer.focusLast();
			}
		});

		newPanel.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyForward(); 
				FileExplorer.focusLast();
			}
		});

		newPanel.getActionMap().put("select first", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JPanel panel = (JPanel) WrapLayout.getComponent(0);
				selectPanel(panel, true);
			}
		});
		
		newPanel.getActionMap().put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				FileExplorer.focusLast();
			}
		});

		InputMap inputMap = newPanel.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select all");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "history forward");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "select first");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "select first");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "select first");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "select first");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");

		newPanel.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
			}
			public void focusLost(FocusEvent e) {}
		});	

		return newPanel;
	}

	public static JPopupMenu getBackgroundPopupMenu() {
		File selected = (File) FileExplorer.getLastTreeNodeOpened().getUserObject();

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		JMenu sectionsMenu = new JMenu("  New");

		//New submenu(txt, folder)

		menuItem = new JMenuItem("  Text Document");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "extensions/txt.png", 17, 17, true));

		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.
											getLastTreeNodeOpened();
				JTree tree = FileExplorer.getTree();

				DefaultMutableTreeNode node = lastTreeNodeOpened;
				String filePath = ((File) node.getUserObject()).getPath();
				String name;
				ImageIcon img=null;
				File f;
 
				f = new File(filePath + "/");
				if(!f.canWrite()) {
					JOptionPane.showMessageDialog(null, 
						"Not enough permissions!");
					return;
				}

				img = Utility.getImageFast(ICONPATH + "extensions/txt.png", 
											50, 50, true);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter File Name", "New Text Document", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "File");

				if(name==null)
					return;

				if(name.equals("")) {
					JOptionPane.showMessageDialog(null, "Can't have empty name");
					
					return;
				}

				if(FileExplorer.isInvalidFileName(name)) {
					JOptionPane.showMessageDialog(null, "Rename failed! Invalid name");
					return;
				}

				String invalidStripped = 
					name.replaceAll("[\\\\/:*?\"<>|]", "_");
				if(!name.equals(invalidStripped)) {
					JOptionPane.showMessageDialog(null, 
						"Replace invalid characters with \"_\"");
					name = invalidStripped;
				}

				if(Utility.getExtension(name).equals("txt"))
					f = new File(filePath + "/" + name);
				else
					f = new File(filePath + "/" + name + ".txt");

				if(!f.exists()) {
					try {
						f.createNewFile();
					}
					catch(IOException e) {
						JOptionPane.showMessageDialog(null, e.getMessage());
					}
				}
				else {
					JOptionPane.showMessageDialog(null, 
						"File with that name already exists!");
					return;
				}

				showCurrentDirectory(node);
			}
		});

		menuItem.setBackground(Color.white);

		sectionsMenu.add(menuItem);


		menuItem = new JMenuItem("  Folder");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/folder.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();
				JTree tree = FileExplorer.getTree();

				DefaultMutableTreeNode node = lastTreeNodeOpened;
				String filePath = ((File) node.getUserObject()).getPath();
				String name;
				ImageIcon img=null;
				Image folderImg;
				
				File f = new File(filePath + "/");
				if(!f.canWrite()) {
					JOptionPane.showMessageDialog(
						null, "Not enough permissions!");
					return;
				}

				img = Utility.getImageFast(
					ICONPATH + "other/folder.png", 50, 50, true);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter Folder Name", "New Folder", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "Folder");
			
				if(name==null)
					return;

				if(name.equals("")) {
					JOptionPane.showMessageDialog(null, "Can't have empty name");
					
					return;
				}

				if(FileExplorer.isInvalidFileName(name)) {
					JOptionPane.showMessageDialog(null, "Rename failed! Invalid name");	
					return;
				}

				String invalidStripped = 
					name.replaceAll("[\\\\/:*?\"<>|]", "_");
				if(!name.equals(invalidStripped)) {
					JOptionPane.showMessageDialog(null, 
						"Replace invalid characters with \"_\"");
					name = invalidStripped;
				}

				f = new File(filePath + "/" + name);
				if(!f.exists()){
					try {
						f.mkdir();
					}
					catch(Exception e) {
						JOptionPane.showMessageDialog(null, e.getMessage());
					}
				}
				else {
					JOptionPane.showMessageDialog(null, 
						"Directory with that name already exists!");
					return;
				}

				/* add node to tree and reload tree here */
				DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
				defMod1.reload();
	
				FileExplorer.selectDirectory(node);
			}
		});

		menuItem.setBackground(Color.white);
		sectionsMenu.add(menuItem);
	

		sectionsMenu.setIcon(Utility.getImageFast(
					ICONPATH + "other/plus.png", 17, 17, true));	

		if(selected.exists() && selected.canWrite()) {
			popupMenu.add(sectionsMenu);
		}

		popupMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		popupMenu.setBackground(Color.white);	

		//Refresh option

		menuItem = new JMenuItem("  Refresh");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/refresh.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

				FileExplorer.refresh(lastTreeNodeOpened);
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Paste");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/paste.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				for(DefaultMutableTreeNode current : clipboard) {
					try {
						FileExplorer.pasteFile((File) current.getUserObject(), selected, operation);
					}
					catch(Exception e) {

					}
				}

				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				clipboard.clear();
				operation = "";
			}
		});
		menuItem.setBackground(Color.white);
		if(selected.exists() && selected.canWrite() && 
				!selected.getName().equals("") && clipboard.size()!=0) {
			popupMenu.add(menuItem);
		}

		menuItem = new JMenuItem("  OS Explorer");
		menuItem.setIcon(Utility.getImageFast(ICONPATH + 
								"other/osexplorer.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

				String name="";
				File curFile;

				curFile = ((File) lastTreeNodeOpened.getUserObject());

				try {		
					if(curFile.getName().equals(windowsTopName) && !curFile.exists()) {
						Runtime.getRuntime().exec("cmd /c start explorer");
						return;
					}
					Desktop.getDesktop().open(curFile);
				}
				catch(IOException e) {

				}
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);

		menuItem = new JMenuItem("  New Window");
		menuItem.setIcon(Utility.
				getImageFast(ICONPATH + "other/folder.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				File currentJar=null;

				final String javaBin = System.getProperty("java.home") + 
					File.separator + "bin" + File.separator + "java";

				try {
					currentJar = new File(FileExplorer.class.
						getProtectionDomain().getCodeSource().getLocation().toURI());
				}
				catch(Exception e) {
				}

				DefaultMutableTreeNode lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

				/* is it a jar file? */
				if(currentJar.getName().endsWith(".jar")) {
					final ArrayList<String> command = new ArrayList<String>();

					/* Build command: java -jar application.jar */
					command.add(javaBin);
					command.add("-jar");
					command.add(currentJar.getPath());
					
					if(!((File) lastTreeNodeOpened.getUserObject()).
							getPath().equals(windowsTopName)) {
						command.add(((File) lastTreeNodeOpened.getUserObject()).getPath());
					}

					final ProcessBuilder builder = new ProcessBuilder(command);
					try {
						builder.start();
					}
					catch(Exception e) {

					}
				}
				else {
					StringBuilder cmd = new StringBuilder();

					cmd.append(javaBin);
					cmd.append(" -cp ").append(
						ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
					cmd.append(FileExplorer.class.getName()).append(" ");
					
					if(!((File) lastTreeNodeOpened.getUserObject()).
								getPath().equals(windowsTopName)) {
						cmd.append(((File) lastTreeNodeOpened.getUserObject()).getPath());
					}
					try {
						Runtime.getRuntime().exec(cmd.toString());
					}
					catch(Exception e) {
					}

					return;
				}
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);

		return popupMenu;
	}

	static public JPopupMenu getFilePopupMenu(
		DefaultMutableTreeNode panelNode) {

		File panelFile = (File) panelNode.getUserObject();

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		ImageIcon img=null;

		Boolean multiple = false;
		if(selectedList.size()>1)
			multiple = true;

		menuItem = new JMenuItem("  Open");
		img = Utility.getImageFast(ICONPATH + "other/open.png", 17, 17, true);
		menuItem.setIcon(img);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
				FileExplorer.clearFuture();
				if(panelNode!=null)
					FileExplorer.enterOrOpen(panelNode);
				else
					FileExplorer.enterOrOpen(new DefaultMutableTreeNode(panelFile));
		
				FileExplorer.focusLast();
			}
		});
		menuItem.setBackground(Color.white);
		if(panelFile.exists() && panelFile.canRead() && !multiple)
			popupMenu.add(menuItem);
	
		menuItem = new JMenuItem("  New Window");
		menuItem.setIcon(Utility.getImageFast(ICONPATH + 
			"other/folder.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				File currentJar=null;

				final String javaBin = System.getProperty("java.home") + 
					File.separator + "bin" + File.separator + "java";

				try {
					currentJar = new File(FileExplorer.class.
						getProtectionDomain().getCodeSource().getLocation().toURI());
				}
				catch(Exception e) {
				}

				/* is it a jar file? */
				if(currentJar.getName().endsWith(".jar")) {
					final ArrayList<String> command = new ArrayList<String>();

					/* Build command: java -jar application.jar */
					command.add(javaBin);
					command.add("-jar");
					command.add(currentJar.getPath());

					command.add(((File) panelNode.getUserObject()).getPath());
				
					final ProcessBuilder builder = new ProcessBuilder(command);
					try {
						builder.start();
					}
					catch(Exception e) {

					}
				}
				else {
					StringBuilder cmd = new StringBuilder();

					cmd.append(javaBin);
					cmd.append(" -cp ").append(
						ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
					cmd.append(FileExplorer.class.getName()).append(" ");

					cmd.append(((File) panelNode.getUserObject()).getPath());
					try {
						Runtime.getRuntime().exec(cmd.toString());
					}
					catch(Exception e) {
					}

					return;
				}
			}
		});

		menuItem.setBackground(Color.white);
		if(panelFile.isDirectory() && panelFile.exists() && panelFile.canRead() 
			&& !multiple)
			
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Cut");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/cut.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				clipboard.clear();

				for(JPanel currentPanel : selectedList) 
					clipboard.add(mapPanelNode.get(currentPanel));

				operation = "cut";
			}
		});
		menuItem.setBackground(Color.white);
		if(panelFile.exists() && panelFile.canWrite() && 
				!panelFile.getName().equals(""))
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Copy");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/copy.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				clipboard.clear();

				for(JPanel currentPanel : selectedList)
					clipboard.add(mapPanelNode.get(currentPanel));

				operation = "copy";
			}
		});
		menuItem.setBackground(Color.white);
		if(panelFile.exists() && !panelFile.getName().equals(""))
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Paste");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/paste.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				for(DefaultMutableTreeNode current : clipboard) {
					try {
						FileExplorer.pasteFile((File) current.getUserObject(), panelFile, operation);
					}
					catch(Exception e) {

					}
				}
				
				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				clipboard.clear();
				operation = "";
			}
		});
		menuItem.setBackground(Color.white);
		if(panelFile.exists() && panelFile.canWrite() && clipboard.size()!=0 &&
				!panelFile.getName().equals("") && panelFile.isDirectory() && !multiple)
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Rename");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/rename.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.rename(panelNode);
			}
		});
			
		menuItem.setBackground(Color.white);
		if(panelFile.exists() && panelFile.canWrite() && 
				!panelFile.getName().equals("") && !multiple)
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Delete");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/delete.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.delete(panelNode);
			}
		});

		menuItem.setBackground(Color.white);
		if(panelFile.exists() && panelFile.canWrite() && 
				!panelFile.getName().equals("") && !multiple)
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Properties");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "other/properties.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.properties(panelFile);
			}
		});

		menuItem.setBackground(Color.white);
		if(!multiple)
			popupMenu.add(menuItem);

		popupMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK));		
		popupMenu.setBackground(Color.white);
		
		return popupMenu;
	}

	public static void showCurrentDirectory(DefaultMutableTreeNode node) {
		executor.execute(new Runnable() {
			public void run() { 
				JTree tree = FileExplorer.getTree();
				JPanel folder = FileExplorer.getFolder();
				
				folder.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));

				int numChild=tree.getModel().getChildCount(node);
				DefaultMutableTreeNode currentNode;
				File currentFile;
				
				String FileName;
			
				FileName = ((File) node.getUserObject()).getName();

				if(FileName.isEmpty())
					FileName = ((File) node.getUserObject()).getPath();
				
				FileExplorer.setNavigationText(((File) node.getUserObject()).getPath());
				FileExplorer.setSearchText("Search" + " \"" + FileName + "\"");
				FileExplorer.clearNavButtons();
				
				TreePath path = new TreePath(node.getPath());
				for(int i=0; i < path.getPathCount(); i++) {
					DefaultMutableTreeNode current =  (DefaultMutableTreeNode)
						path.getPathComponent(i);

					FileExplorer.addNavButton(current);
				}

				mapPanelNode.clear();
				folder.removeAll();
				folder.repaint();
				folder.revalidate();
				for(int i=0; i<numChild; i++) { 
					currentNode = (DefaultMutableTreeNode) tree.getModel().
						getChild(node, i);
					currentFile =(File) currentNode.getUserObject();

					if(showHiddenFiles ?  true : !currentFile.isHidden() || 
						!currentFile.getName().startsWith(".")) {
					
						JPanel newPanel = getPanel(currentFile, currentNode);

						mapPanelNode.put(newPanel, currentNode);
						folder.add(newPanel);
					}
					
					folder.repaint();
					folder.revalidate();
				}

				currentFile=(File) node.getUserObject();
				File children[] = currentFile.listFiles();

				if(children==null)
					return;

				Arrays.sort(children);

				for(File element : children) {
		  
					if(!element.isFile())
						continue;

					if(showHiddenFiles ? true : !element.isHidden() || 
										!element.getName().startsWith(".")) {
						
						JPanel newPanel;
						newPanel = getPanel(element, new DefaultMutableTreeNode(element));
		
						mapPanelNode.put(newPanel, 
							new DefaultMutableTreeNode(element));
						folder.add(newPanel);
					}
					folder.repaint();
					folder.revalidate();
				}
			}
		});
	}

	/*
	static Image iconToImage(Icon icon) {
	   if (icon instanceof ImageIcon) {
		  return ((ImageIcon)icon).getImage();
	   } 
	   else {
		  int w = icon.getIconWidth();
		  int h = icon.getIconHeight();
		  GraphicsEnvironment ge = 
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		  GraphicsDevice gd = ge.getDefaultScreenDevice();
		  GraphicsConfiguration gc = gd.getDefaultConfiguration();
		  BufferedImage image = gc.createCompatibleImage(w, h);
		  Graphics2D g = image.createGraphics();
		  icon.paintIcon(null, g, 0, 0);
		  g.dispose();
		  return image;
	   }
	 }
	*/

	static JPanel getPanel(File currentFile, DefaultMutableTreeNode panelNode) {
		JLabel label;
		ImageIcon img=null;
		Set<String> set = new HashSet<>();
		File panelFile = (File) panelNode.getUserObject();
		String name = panelFile.getName(), path="";
		String extension = Utility.getExtension(panelFile.getName());

		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(panelFile);
			name = fsv.getSystemDisplayName(panelFile);
			
			path = ICONPATH + "other/harddiskfolder.png";

			if(description.equals("CD Drive")) {
				path = ICONPATH + "other/cdfolder.png";
				name = description + " (" + panelFile.getPath().replace("\\", "") + ")";
			}
			else if(description.equals("DVD Drive")) {
				path = ICONPATH + "other/dvd.png";
				name = description + " (" + panelFile.getPath().replace("\\", "") + ")";
			}
			else if(description.equals("USB Drive")) {
				path = ICONPATH + "other/usbfolder.png";			
			}

			img = Utility.getImageFast(path, 60, 60, true);
		}

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(extension)) {
			img = Utility.getImageFast(panelFile.getPath(), 60, 60, false);
		}

		if(img==null) {
			if(currentFile.isDirectory()) {
				if(panelFile.list()!=null && panelFile.list().length==0)
					img = Utility.getImageFast(FileExplorer.getIconPath() 
						+ "other/folderempty.png", 60, 60, true);
				else {
					img = Utility.getImageFast(ICONPATH 
						+ "other/folder.png", 60, 60, true);
				}
			}
			else {
				img = Utility.getImageFast(ICONPATH + "extensions/" + 
					Utility.getExtension(currentFile.getName()) + ".png", 60, 60, true);

				if(img==null) {
					img = Utility.getImageFast(ICONPATH 
						+ "other/question.png", 60, 60, true);
				}
			}
		}

		//Image folderImg = img.getImage().getScaledInstance(150, 60, 
		//		Image.SCALE_DEFAULT);

		/* You get small resolution system icons. Waiting for official better 
		way	Icon icon;

		if(!iconSet.contains(extension) && iconName!="folder.png") {
			icon = FileSystemView.getFileSystemView().getSystemIcon(file);
			folderImg = iconToImage(icon).getScaledInstance(60, 60, 
				Image.SCALE_DEFAULT);
		}
		*/

		JPanel panel = new JPanel(new BorderLayout());
		
		// top empty space
		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(150, 10));
		panel.add(label,  BorderLayout.NORTH);

		// right empty space
		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(10, 40));
		panel.add(label,  BorderLayout.EAST);

		// left empty space
		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(10, 40));
		panel.add(label,  BorderLayout.WEST);

		label = new JLabel(img, JLabel.CENTER);
		label.setPreferredSize(new Dimension(60, 60));
		//Border b = new BevelBorder(BevelBorder.RAISED, Color.LIGHT_GRAY, Color.DARK_GRAY);
		//label.setBorder(b);
		panel.add(label,  BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		if(!path.equals("")) {
			// top empty space
			label = new JLabel("", JLabel.CENTER);
			label.setPreferredSize(new Dimension(150, 10));
			bottomPanel.add(label,  BorderLayout.NORTH);

			// right empty space
			label = new JLabel("", JLabel.CENTER);
			label.setPreferredSize(new Dimension(10, 10));
			bottomPanel.add(label,  BorderLayout.EAST);

			// left empty space
			label = new JLabel("", JLabel.CENTER);
			label.setPreferredSize(new Dimension(10, 10));
			bottomPanel.add(label,  BorderLayout.WEST);

			long free = currentFile.getFreeSpace();
			long total = currentFile.getTotalSpace();
			int used = (int) (((total-free)*100)/total);

			JProgressBar bar = new JProgressBar(0, 100);
			bar.setValue(used);	
			bar.setStringPainted(true);
			bar.setString(FileExplorer.convertBytes(free, false) + " free");

			bar.setBackground(FileExplorer.topBackgroundColor);
			if(used>90) {
				bar.setForeground(Color.RED);
				bar.setUI(new BasicProgressBarUI() {
					protected Color getSelectionForeground() { return Color.WHITE; }
				});
			}
			else {
				bar.setForeground(Color.CYAN);
				bar.setUI(new BasicProgressBarUI() {
					protected Color getSelectionForeground() { return Color.BLACK; }
				});
			}

			bar.setBorderPainted(false);

			bottomPanel.add(bar,  BorderLayout.CENTER);
		}

		panel.getActionMap().put("select all", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					clearPanelSelection();
					for(JPanel current : mapPanelNode.keySet())
						selectPanel(current, false);
				}
			});

		panel.getActionMap().put("cut", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					clipboard.clear();

					for(JPanel currentPanel : selectedList) 
						clipboard.add(mapPanelNode.get(currentPanel));

					operation = "cut";	
				}
			});

		panel.getActionMap().put("copy", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					clipboard.clear();

					for(JPanel currentPanel : selectedList) 
						clipboard.add(mapPanelNode.get(currentPanel));

					operation = "copy";
				}
			});

		panel.getActionMap().put("rename", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					if(selectedList.size()==1)
						FileExplorer.rename(panelNode);		
				}
			});

		panel.getActionMap().put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				FileExplorer.focusLast();
			}
		});

		panel.getActionMap().put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.delete(panelNode);		
			}
		});

		panel.getActionMap().put("select left", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(lastPanelSelected);
				selectPanel((JPanel) WrapLayout.getComponent(position - 1), true);
			}
		});

		panel.getActionMap().put("select right", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(lastPanelSelected);
				selectPanel((JPanel) WrapLayout.getComponent(position + 1), true);
			}
		});

		panel.getActionMap().put("select down", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(lastPanelSelected);
				selectPanel((JPanel) WrapLayout.
					getComponent(position + WrapLayout.getRowLength()), true);
			}
		});

		panel.getActionMap().put("select up", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(lastPanelSelected);
				selectPanel((JPanel) WrapLayout.
					getComponent(position - WrapLayout.getRowLength()), true);
			}
		});

		panel.getActionMap().put("history back", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyBack(); 
				FileExplorer.focusLast();
			}
		});

		panel.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyForward(); 
				FileExplorer.focusLast();
			}
		});

		panel.getActionMap().put("enter", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(selectedList.size()==1) {
					FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
					FileExplorer.clearFuture();
					if(panelNode!=null)
						FileExplorer.enterOrOpen(panelNode);
					else
						FileExplorer.enterOrOpen(new DefaultMutableTreeNode(panelFile));
					FileExplorer.focusLast();
				}
			}
		});

		InputMap inputMap = panel.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select all");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "cut");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "select left");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "select right");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "select down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "select up");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "history forward");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");

		label = new JLabel(name, JLabel.CENTER);
		label.setPreferredSize(new Dimension(150, 30));
		label.setForeground (Color.white);
		label.setBorder(new EmptyBorder(0, 10, 0, 10));
		bottomPanel.add(label, BorderLayout.SOUTH);
		bottomPanel.setBackground(FileExplorer.folderBackgroundColor);

		panel.add(bottomPanel, BorderLayout.SOUTH);

		panel.setName(name);
		panel.setBorder(BorderFactory.createLineBorder(FileExplorer.folderBackgroundColor));
		panel.setBackground(FileExplorer.folderBackgroundColor);

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent event) {		
				if(!selectedList.contains(panel)) {
					for (int i = 0; i < panel.getComponentCount(); i++) {
						panel.getComponent(i).setBackground(FileExplorer.panelHoverColor);
					}
					panel.setBackground(FileExplorer.panelHoverColor);
				}
			}
			@Override
			public void mouseExited(MouseEvent event) {
				if(!selectedList.contains(panel)) {
					for (int i = 0; i < panel.getComponentCount(); i++) {
						panel.getComponent(i).setBackground(FileExplorer.folderBackgroundColor);
					}
					panel.setBackground(FileExplorer.folderBackgroundColor);
				}
			}
			@Override
			public void mousePressed(MouseEvent event) {
				if(!panelFile.exists()) {
					FileExplorer.findExistingParent(panelFile);
					return;
				}

				FileExplorer.setFocusExplorer();

				/* No control pressed */
				if(!event.isControlDown()) {
					/* Left Mouse Button */

					if(SwingUtilities.isLeftMouseButton(event) || 
						(SwingUtilities.isRightMouseButton(event) 
							&& !selectedList.contains(panel))) {
						
						selectPanel(panel, true);
					}
				}
				else if(event.isControlDown() && SwingUtilities.isLeftMouseButton(event) 
					&& event.getClickCount() == 1) {

					selectPanel(panel, false);
				}

				if(event.getClickCount()%2==0 && event.getButton() == MouseEvent.BUTTON1) {
					FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
					FileExplorer.clearFuture();
					FileExplorer.enterOrOpen(panelNode);

					FileExplorer.focusLast();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getFilePopupMenu(panelNode);
					menu.show(event.getComponent(), event.getX(), event.getY());
				}
			}
		});

		panel.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {

			}
			public void focusLost(FocusEvent e) {

			}
		});

		return panel;
	}

	static public void selectPanel(JPanel panel, Boolean clear) {
		
		if(panel==null)
			return;

		if(clear) {
			clearPanelSelection();
			for (int i = 0; i < panel.getComponentCount(); i++) {
				panel.getComponent(i).setBackground(FileExplorer.panelSelectionColor);
			}
			panel.setBackground(FileExplorer.panelSelectionColor);
			panel.setBorder(BorderFactory.createLineBorder(Color.white));
			selectedList.add(panel);
		}
		else {
			if(selectedList.contains(panel)) {
				for (int i = 0; i < panel.getComponentCount(); i++) {
					panel.getComponent(i).setBackground(FileExplorer.folderBackgroundColor);
				}
				panel.setBackground(FileExplorer.folderBackgroundColor);
				panel.setBorder(BorderFactory.createLineBorder(
					FileExplorer.folderBackgroundColor));	
				selectedList.remove(panel);
			}
			else
				selectedList.add(panel);

			for(JPanel element : selectedList) {
				for (int i = 0; i < element.getComponentCount(); i++) {
					element.getComponent(i).setBackground(FileExplorer.panelSelectionColor);
				}
				element.setBackground(FileExplorer.panelSelectionColor);
				element.setBorder(BorderFactory.createLineBorder(Color.white));
			}
		}

		panel.requestFocusInWindow();
		lastPanelSelected = panel;
	}

	static public void clearPanelSelection() {
		for(JPanel element : selectedList) {
			for (int i = 0; i < element.getComponentCount(); i++) {
				element.getComponent(i).setBackground(FileExplorer.folderBackgroundColor);
			}
			element.setBackground(FileExplorer.folderBackgroundColor);
			element.setBorder(BorderFactory.createLineBorder(
				FileExplorer.folderBackgroundColor));
		}
		selectedList.clear();

		if(lastPanelSelected!=null) {
			for (int i = 0; i < lastPanelSelected.getComponentCount(); i++) {
				lastPanelSelected.getComponent(i).setBackground(FileExplorer.folderBackgroundColor);
			}
			lastPanelSelected.setBackground(FileExplorer.folderBackgroundColor);
			lastPanelSelected.setBorder(BorderFactory.createLineBorder(
				FileExplorer.folderBackgroundColor));
		}
		lastPanelSelected = null;
	}

	public static JPanel getLastPanelSelected() {
		return lastPanelSelected;
	}

	public static Executor getExecutor() {
		return executor;
	}

	/*
	
	============================================================
	                      Tree Window
	============================================================

	*/

	private static JTree getTreePanel(DefaultMutableTreeNode top) {
		JTree newPanel = new JTree(top) {
			//Added bounds.x = 0 to stop horizontal scrolling
			@Override
			public void scrollPathToVisible(TreePath treePath) {
				if (treePath != null) {
					this.makeVisible(treePath);

					Rectangle bounds = this.getPathBounds(treePath);

					if (bounds != null) {
						bounds.x = 0;
						this.scrollRectToVisible(bounds);
					}
				}
			}
		};

		newPanel.setBorder(new EmptyBorder(0, 15, 15, 0)); //top,left,bottom,right
		newPanel.putClientProperty("JTree.lineStyle", "None");
		newPanel.setBackground(FileExplorer.treeBackgroundColor);
		final Font currentFont = newPanel.getFont();
		final Font bigFont = new Font(currentFont.getName(), 
					currentFont.getStyle(), currentFont.getSize() + 1);
		newPanel.setFont(bigFont);
		newPanel.setEditable(false);
		newPanel.setCellEditor(new DefaultTreeCellEditor(newPanel, 
				(DefaultTreeCellRenderer) newPanel.getCellRenderer()) {
			@Override
			public boolean isCellEditable(EventObject event) {
				if(event instanceof MouseEvent){
					return false;
				}
				return super.isCellEditable(event);
			}
		});

		newPanel.setCellRenderer(new DefaultTreeCellRenderer() {
			private HashMap<File, FsvCache> descriptions = new HashMap<File, FsvCache>();

			class FsvCache {
				private String description;
				private String name;

				FsvCache(String description, String name) {
					this.description = description;
					this.name = name;
				}

				String getDescription() {
					return description;
				}

				String getName() {
					return name;
				}
			}

			public Component getTreeCellRendererComponent ( JTree tree, 
				Object value, boolean sel, boolean expanded, boolean leaf,
				int row, boolean hasFocus ) {

				JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, 
									value, sel, expanded, leaf, row, hasFocus );

				setBackground(FileExplorer.treeBackgroundColor);
				setTextNonSelectionColor(Color.WHITE);
				setTextSelectionColor(FileExplorer.textSelectionColor);
				setOpaque(true);

				DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) value;
				File file = (File) nodo.getUserObject();
				String name = file.getName();

				TreeModel tmodel = tree.getModel();
				Object root = tmodel.getRoot();
				label.setBorder(new EmptyBorder(0, 0, 0, 0)); 
				//top,left,bottom,right

				if(nodo==root) {
					setIcon(Utility.getImageFast(ICONPATH + 
								"other/pc.png", 25, 25, true));
					label.setBorder(new EmptyBorder(15, 0, 0, 0)); 
					//top,left,bottom,right
				}
				else if(name.trim().length() == 0 && nodo.getParent()==root) {
					FileSystemView fsv = FileSystemView.getFileSystemView();

					String path = ICONPATH + "other/harddisk.png";

					/* Dear john-from-the-future, this is john-from-the-past. You
					almost certainly think I messed up here and that the code could be
					cleaned up. Well don't! It’s like this for a reason! */

					FsvCache info = descriptions.get(file);
					String description;

					if(info!=null) {
						description = info.getDescription();
						name  = info.getName();
					}
					else {
						description = fsv.getSystemTypeDescription(file);
						name = fsv.getSystemDisplayName(file);
						descriptions.put(file, new FsvCache(description, name));
					}

					if(description.equals("CD Drive")) {
						path = ICONPATH + "other/cd.png";
						name = description + " (" + file.getPath().replace("\\", "") + ")";
					}
					else if(description.equals("DVD Drive")) {
						path = ICONPATH + "other/dvd.png";
						name = description + " (" + file.getPath().replace("\\", "") + ")";
					}
					else if(description.equals("USB Drive")) {
						path = ICONPATH + "other/usb.png";			
					}
					
					setText(name);
					setIcon(Utility.getImageFast(path, 25, 25, true));
				}
				else if(file.list()!=null && file.list().length==0) {
					setIcon(Utility.getImageFast(
						ICONPATH + "other/folderempty.png", 25, 25, true));
				}
				else if(expanded) {
					setIcon(Utility.getImageFast(
						ICONPATH + "other/folderopen.png", 25, 25, true));
				}
				else {
					setIcon(Utility.getImageFast(
						ICONPATH + "other/folder.png", 25, 25, true));
				}

				return label;
			}
		});

		newPanel.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//Listen for when the selection changes.
		newPanel.addTreeSelectionListener(new TreeSelectionListener() {
			 	/* Targets selected node when clicked in tree */
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)
												newPanel.getLastSelectedPathComponent();
					File current;
				
					if (node == null) 
						return;

					current = (File) node.getUserObject();
					if (current.isDirectory()) {
						createNodes(node);
					}
				}
			}
		);
		newPanel.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) 
				throws ExpandVetoException {}
	  
			@Override
			public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) 
				throws ExpandVetoException {
			
				TreePath path = treeExpansionEvent.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
										path.getLastPathComponent();

				File current;
				current = (File) node.getUserObject();
				if (current.isDirectory()) {
					createNodes(node);
				}
			}
		});

		newPanel.addMouseListener(new MouseAdapter() {
			DefaultMutableTreeNode last;

			@Override
			public void mousePressed(MouseEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
					newPanel.getLastSelectedPathComponent();

				FileExplorer.setFocusTree();

				if(node==null)
					return;

				String filePath = ((File) node.getUserObject()).getPath();
				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					findExistingParent(f);
					return;
				}

				File current = (File) node.getUserObject();
				if(current.exists()) {
					createNodes(node);
				}
				
				if(e.getButton() == MouseEvent.BUTTON3) {
					JTree tree = FileExplorer.getTree();
					int row = tree.getClosestRowForLocation(e.getX(), e.getY());
					tree.setSelectionRow(row);

					node = (DefaultMutableTreeNode) 
						newPanel.getLastSelectedPathComponent();

					JPopupMenu menu = getFolderPopupMenu(node);
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
				else {
					if(lastTreeNodeOpened!=node) {
						FileExplorer.historyPush(lastTreeNodeOpened);
						FileExplorer.clearFuture();
						lastTreeNodeOpened = node;
						FileExplorer.showCurrentDirectory(node);
					}
				}
			}
		});

		newPanel.getActionMap().put("history back", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyBack();
				FileExplorer.focusLast();
			}
		});

		newPanel.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyForward(); 
				FileExplorer.focusLast();
			}
		});

		newPanel.getActionMap().put("enter", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
									newPanel.getLastSelectedPathComponent();
			
				if(lastTreeNodeOpened!=node) {
					FileExplorer.historyPush(lastTreeNodeOpened);
					FileExplorer.clearFuture();
				}
				lastTreeNodeOpened = node;
				File current;

				current = (File) node.getUserObject();
				if (current.isDirectory()) {
					createNodes(node);
				}
				FileExplorer.showCurrentDirectory(node);
			}
		});

		InputMap inputMap = newPanel.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "history forward");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");

		return newPanel;
	}

	static private JPopupMenu getFolderPopupMenu(DefaultMutableTreeNode node) {
		File fileNode = (File) node.getUserObject();

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		ImageIcon img=null;

		menuItem = new JMenuItem("  New Window");
		menuItem.setIcon(Utility.getImageFast(ICONPATH + 
			"other/folder.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				File currentJar=null;

				final String javaBin = System.getProperty("java.home") + 
					File.separator + "bin" + File.separator + "java";

				try {
					currentJar = new File(FileExplorer.class.
						getProtectionDomain().getCodeSource().getLocation().toURI());
				}
				catch(Exception e) {
				}

				/* is it a jar file? */
				if(currentJar.getName().endsWith(".jar")) {
					final ArrayList<String> command = new ArrayList<String>();

					/* Build command: java -jar application.jar */
					command.add(javaBin);
					command.add("-jar");
					command.add(currentJar.getPath());

					command.add(((File) node.getUserObject()).getPath());
				
					final ProcessBuilder builder = new ProcessBuilder(command);
					try {
						builder.start();
					}
					catch(Exception e) {

					}
				}
				else {
					StringBuilder cmd = new StringBuilder();

					cmd.append(javaBin);
					cmd.append(" -cp ").append(
						ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");
					cmd.append(FileExplorer.class.getName()).append(" ");

					cmd.append(((File) node.getUserObject()).getPath());
					try {
						Runtime.getRuntime().exec(cmd.toString());
					}
					catch(Exception e) {
					}

					return;
				}
			}
		});

		menuItem.setBackground(Color.white);
		if(fileNode.isDirectory() && fileNode.exists() && fileNode.canRead())
			popupMenu.add(menuItem);

		/*
		menuItem = new JMenuItem("  Rename");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/rename.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.rename(node);
			}
		});
			
		menuItem.setBackground(Color.white);
		if(fileNode.exists() && fileNode.canWrite())
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Delete");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/delete.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.delete(node);
			}
		});

		menuItem.setBackground(Color.white);
		if(fileNode.exists() && fileNode.canWrite())
			popupMenu.add(menuItem);
		*/

		menuItem = new JMenuItem("  Properties");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "other/properties.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FileExplorer.properties((File) node.getUserObject());
				FileExplorer.focusLast();
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);

		popupMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK));		
		popupMenu.setBackground(Color.white);
		
		return popupMenu;
	}

	private static void sortNode(DefaultMutableTreeNode parent) {
		DefaultMutableTreeNode min_node;
		DefaultMutableTreeNode j_node;

		int n = parent.getChildCount();
		for (int i = 0; i < n - 1; i++) {
			int min = i;
			for (int j = i + 1; j < n; j++) {
				min_node = (DefaultMutableTreeNode) parent.getChildAt(min);
				j_node = (DefaultMutableTreeNode) parent.getChildAt(j);
				if (((File) min_node.getUserObject()).getName().toLowerCase().
						compareTo(((File) j_node.getUserObject()).
							getName().toLowerCase())>0) {
					min = j;
				}
			}
			if (i != min) {
				MutableTreeNode a = (MutableTreeNode) parent.getChildAt(i);
				MutableTreeNode b = (MutableTreeNode) parent.getChildAt(min);
				parent.insert(b, i);
				parent.insert(a, min);
			}
		}
	}

	static public void createNodes(DefaultMutableTreeNode top) {
		JTree tree = FileExplorer.getTree();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

		File curDir = (File) top.getUserObject();
		File children[] = curDir.listFiles(); 

		if(children==null)
			return;

		Arrays.sort(children);

		for(File element : children) {
			if(!showHiddenFiles && (element.isHidden() || 
									element.getName().startsWith(".")))
				continue;

			if(!element.isDirectory())
				continue;
			
			DefaultMutableTreeNode currentNode=null;
			if(!isNodeInSubtree(top, element)) {
				currentNode = new DefaultMutableTreeNode(new File(
					element.getPath()) {
					@Override
					public String toString() {
						if(this.getPath().compareTo("/")!=0)
							return this.getName();
						else
							return this.getPath();
					}
				});
				
				model.insertNodeInto(currentNode, top, top.getChildCount());
				sortNode(top);
			}
			else {
				int numChild= FileExplorer.getTree().getModel().getChildCount(top);
				for(int i=0; i<numChild; i++) { 
					File currentFile;

					currentNode = (DefaultMutableTreeNode) FileExplorer.getTree().
							getModel().getChild(top, i);

					currentFile=(File) currentNode.getUserObject();
					if(currentFile.getName().compareTo(element.getName())==0) {
						break;
					}
				}
			}

			File[] new_children = element.listFiles();
			if(new_children==null)
				continue;

			for(File current : new_children) {
				if(!showHiddenFiles && (current.isHidden() || 
									current.getName().startsWith(".")))
					continue;

				if(!current.isDirectory())
					continue;

				DefaultMutableTreeNode firstChild;
				if(!isNodeInSubtree(currentNode, current)) {
					firstChild = new DefaultMutableTreeNode(new File(
						current.getPath()) {
						@Override
						public String toString() {
							if(this.getPath().compareTo("/")!=0)
								return this.getName();
							else
								return this.getPath();
						}
					});

					model.insertNodeInto(firstChild, currentNode, 
						currentNode.getChildCount());
					sortNode(currentNode);
				}
				break;
			}
		}
	}

	private static boolean isNodeInSubtree(DefaultMutableTreeNode top, 
		File element) {
		
		DefaultMutableTreeNode currentNode = null;
		int i, numChild= FileExplorer.getTree().getModel().getChildCount(top);
		for(i=0; i<numChild; i++) { 
			File currentFile;

			currentNode = (DefaultMutableTreeNode) FileExplorer.getTree().
					getModel().getChild(top, i);

			currentFile=(File) currentNode.getUserObject();
			if(currentFile.getName().compareTo(element.getName())==0) {
				return true;
			}
		}

		return false;
	}

	static void findExistingParent(File f) {
		DefaultMutableTreeNode node = lastTreeNodeOpened;

		if(!f.exists()) {
			JOptionPane.showMessageDialog(null, "This directory no longer exists");
			// Restore to previous working directory
			
			f=(File) node.getUserObject();
			while(!f.exists()) {
				node = (DefaultMutableTreeNode) node.getParent();
				if(node==null)
					return;
				f=(File) node.getUserObject();
			}

			FileExplorer.refresh(node);
			return;
		}
	}

	public static String getWindowsTopName() {
		return windowsTopName;
	}

	public static DefaultMutableTreeNode getLastTreeNodeOpened() {
		return lastTreeNodeOpened;
	}

	public static void setLastTreeNodeOpened(DefaultMutableTreeNode node) {
		lastTreeNodeOpened = node;
	}

	/*
	
	============================================================
	                      Top Window
	============================================================

	*/

	public static JPanel getTopWindow() {
		JPanel newPanel = new JPanel();
		newPanel.setLayout(new BorderLayout());

		JPanel exitPanel = new MotionPanel(FileExplorer.getFrame());
		exitPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
		exitPanel.setBackground(FileExplorer.exitPanelBackgroundColor);
		exitPanel.setPreferredSize(new Dimension(exitPanelHeight, exitPanelHeight));

		exitPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		exitPanel.setComponentOrientation(
			ComponentOrientation.RIGHT_TO_LEFT);

		ImageIcon img = new ImageIcon(FileExplorer.class.getResource(
			ICONPATH + "other/close.png"));
		Image pict = img.getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT);
		img = new ImageIcon(pict);

		JButton button = new JButton(img);
		button.setBorder(new EmptyBorder(0, 0, 0, 0));
		button.setContentAreaFilled(false);

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				System.exit(0);
			}
		});

		exitPanel.add(button);

		newPanel.add(exitPanel, BorderLayout.NORTH);

		JPanel topPanel = new JPanel(new GridBagLayout());
		topPanel.setBackground(FileExplorer.topBackgroundColor);

		GridBagConstraints c = new GridBagConstraints();
		topPanel.setBackground(FileExplorer.topBackgroundColor);
		topPanel.setBorder(
			BorderFactory.createMatteBorder(6, 6, 6, 3, FileExplorer.topBackgroundColor));

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;

		buttonBack = new JButton(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedback.png", navHeight, navHeight, true));
		buttonBack.setBorder(BorderFactory.createEmptyBorder());
		buttonBack.setPreferredSize(new Dimension(navHeight, navHeight));
		buttonBack.setFocusPainted(false);

		buttonBack.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e){  
				historyBack();
				FileExplorer.focusLast();
			}
		});
		topPanel.add(buttonBack, c);

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;

		buttonForward = new JButton(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedforward.png", navHeight, navHeight, true));
		buttonForward.setBorder(BorderFactory.createEmptyBorder());
		buttonForward.setPreferredSize(new Dimension(navHeight, navHeight));
		buttonForward.setFocusPainted(false);

		buttonForward.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				historyForward();
				FileExplorer.focusLast();
			}
		});
		topPanel.add(buttonForward, c);

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;

		navigationField = new JTextFieldIcon(new JTextField(), 
			Utility.getImageFast(FileExplorer.getIconPath() + "other/pc.png", 
				15, 15, true));

		navigationField.setCaretColor(Color.RED);
		navigationField.setBackground(FileExplorer.treeBackgroundColor);
		navigationField.setForeground(FileExplorer.textSelectionColor);
		navigationField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, navHeight));
		
		Font font = new Font("SansSerif", Font.BOLD, 13);
		navigationField.setFont(font);

		navigationField.setVisible(false);

		navigationField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				navigationField.select(0, navigationField.getText().length());
			}
			public void focusLost(FocusEvent e) {
				DefaultMutableTreeNode node = FileExplorer.getLastTreeNodeOpened();
				String path = ((File) node.getUserObject()).getPath();

				navigationField.setText(path);
				toggleNavigation();
			}
		});

		navigationField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					String path = navigationField.getText();
					File file = new File(path);

					if(file.exists()) {
						java.util.Stack<String> pathComponents = 
							new java.util.Stack<String>();

						while(true) {
							if(file.getParentFile() == null)
								break;
							pathComponents.add(file.getName());
							file = file.getParentFile();
						} 
						pathComponents.add(file.getPath().replace("\\", ""));
		
						FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
						FileExplorer.clearFuture();
						FileExplorer.loadPath(FileExplorer.getTop(), pathComponents);

						FileExplorer.focusLast();
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		topPanel.add(navigationField, c);

		buttonField = new JPanel();
		buttonField.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttonField.setBackground(FileExplorer.topBackgroundColor);
		buttonField.setForeground(FileExplorer.textSelectionColor);
		buttonField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, navHeight));

		buttonField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				toggleNavigation();
				navigationField.requestFocusInWindow();
			}
		});

		topPanel.add(buttonField, c);

		searchField = new JTextFieldIcon(new JTextField(), 
			Utility.getImageFast(FileExplorer.getIconPath() + 
				"other/magnifyingglass.png", 15, 15, true));

		searchField.setCaretColor(Color.RED);
		searchField.setBackground(FileExplorer.topBackgroundColor);
		searchField.setForeground(FileExplorer.textSelectionColor);
		searchField.setPreferredSize(new Dimension(searchField.
				getPreferredSize().width, navHeight));

		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					JPanel folder = FileExplorer.getFolder();
					searchQuery = searchField.getText();
					
					FileExplorer.focusLast();

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
						FileExplorer.getLastTreeNodeOpened();
					File top = (File) node.getUserObject();

					folder.removeAll();
					folder.repaint();
					folder.revalidate();

					searchField.setText("");

					 // create object of table and table model
					DefaultTableModel model = new DefaultTableModel(0, 1) {};
					JTable table = new JTable(model);

					// add header of the table
					String header[] = new String[] { "Name" };

					// add header in table model     
					model.setColumnIdentifiers(header);
					//set model into the table object
					TableColumnModel tcm = table.getColumnModel();
					tcm.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
						public Component getTableCellRendererComponent(JTable table,
							Object value, boolean isSelected, boolean hasFocus,
								int row, int column) {
							super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

							DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
							File file = (File) node.getUserObject();

							setText(file.getPath());
							if(file.isDirectory())
								setIcon(getSmallIcon("folder.png", file));
							else if(file.isFile() && iconSet.contains(Utility.getExtension(file.getName())))
								setIcon(getSmallIcon(Utility.
									getExtension(file.getName()) + ".png", file));
							else
								setIcon(getSmallIcon("question.png", file));

							setBorder(new EmptyBorder(0, 10, 0, 0));
							setIconTextGap(10);

							return this;
						}

						public ImageIcon getSmallIcon(String name, File file) {
							JLabel label = new JLabel();
							ImageIcon img=null;
							Set<String> set = new HashSet<>(); 
							String path = null;

							// Bad check for images
							set.add("jpeg");
							set.add("jpg");
							set.add("png");
							set.add("gif");
							if(set.contains(Utility.getExtension(file.getName()))) {
								path = file.getPath();
							}

							if(path==null) {
								if(name=="folder.png") {
									if(file.list()!=null && file.list().length==0)
										path = ICONPATH + "other/" + "folderempty.png";
									else {
										path = ICONPATH + "other/" + "folder.png";
									}
								}
								else if(name=="question.png") {
									path = ICONPATH + "other/" + "question.png";
								}
								else
									path = ICONPATH + "extensions/" + name;
							}

							img = Utility.getImageFast(path, 35, 35, true);

							return img;
						}
					});

					table.setBackground(FileExplorer.folderBackgroundColor);
					table.setForeground(Color.WHITE);
					table.setRowHeight(50);
					table.setShowGrid(false);
					table.setDefaultEditor(Object.class, null);
					table.setSelectionModel(new DefaultListSelectionModel() {
						@Override
						public void clearSelection() {
						}

						@Override
						public void removeSelectionInterval(
							int index0, int index1) {
						}
					});
					table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					table.setSelectionBackground(Color.CYAN);

					table.addMouseListener(new MouseAdapter() {
						public void mousePressed(MouseEvent e) {
							JTree tree = FileExplorer.getTree();
							JPanel folder = FileExplorer.getFolder();

							JTable target = (JTable) e.getSource();
							int row = target.rowAtPoint( e.getPoint() );
							int column = target.columnAtPoint( e.getPoint() );

							if (row >= 0 && row < table.getRowCount()) {
								table.setRowSelectionInterval(row, row);
							} 
							else {
								table.clearSelection();
							}

							DefaultMutableTreeNode node = 
								(DefaultMutableTreeNode)
									 target.getValueAt(row, column);

							if (e.getClickCount()%2==0 && 
										e.getButton() == MouseEvent.BUTTON1) {

								File file = (File) node.getUserObject();
								if(file.isDirectory()) {
									FileExplorer.selectDirectory(node);
								}
								else {
									try {
										Desktop.getDesktop().open(file);
									}
									catch(IOException exc) {

									}
								}
							}
							else if(e.getButton() == MouseEvent.BUTTON3) {
								JPopupMenu menu = FileExplorer.getFilePopupMenu(node);
								menu.show(e.getComponent(), e.getX(), e.getY());
							}						
						}
					});

					folder.add(table);
					folder.setLayout(new GridLayout());
	
					JTree tree = FileExplorer.getTree();
					Executor executor = FileExplorer.getExecutor();
					executor.execute(new Runnable() {
						public void run() { 
							search(tree, node, searchQuery, model);
						}
					});
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		searchField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				searchField.setText("");
				searchField.setBackground(FileExplorer.treeBackgroundColor);
			}
			public void focusLost(FocusEvent e) {
				DefaultMutableTreeNode node = FileExplorer.getLastTreeNodeOpened();
				String fileName = ((File) node.getUserObject()).getName();
		
				searchField.setBackground(FileExplorer.topBackgroundColor);

				if(fileName.equals("") || fileName==null) {
					fileName = ((File) node.getUserObject()).getPath();
				}

				searchField.setText("Search" + " \"" + fileName + "\"");
				searchField.setCaretPosition(0);
			}
		});

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.15;
		c.gridx = 3;
		c.gridy = 0;

		topPanel.add(searchField, c);
		
		newPanel.add(topPanel, BorderLayout.CENTER);

		return newPanel;
	}

	static void search(JTree tree, DefaultMutableTreeNode top, String searchQuery, DefaultTableModel model) {
		FileExplorer.createNodes(top);

		int numChild=tree.getModel().getChildCount(top);
		DefaultMutableTreeNode current;
		File topFile = (File) top.getUserObject();
		JPanel folder = FileExplorer.getFolder();

		if(numChild==0)
			return; 

		boolean isSymbolicLink = Files.isSymbolicLink(topFile.toPath());
		if(isSymbolicLink)
			return;

		File children[] = ((File) top.getUserObject()).listFiles();
		if(children!=null) {
			for(File child : children) {
				if(child.isFile() && child.getName().contains(searchQuery)) {
					model.addRow(new Object[] { new DefaultMutableTreeNode(child) });
				
					folder.repaint();
					folder.revalidate();
				}
			}
		}

		for(int i=0; i<numChild; i++) {	  
			current = (DefaultMutableTreeNode) tree.getModel().getChild(top, i);
			File element = (File) current.getUserObject();

			if(element.getName().contains(searchQuery)) {
				model.addRow(new Object[] { current });
				folder.repaint();
				folder.revalidate();
			}

			search(tree, current, searchQuery, model);
		}		  
	}

	public static void historyBack() {
		JTree tree = FileExplorer.getTree();
		DefaultMutableTreeNode previous,
						lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

		FileExplorer.clearPanelSelection();

		previous = FileExplorer.historyPop();
		if(previous==null) {
			return;
		}

		FileExplorer.futureHistoryPush(previous);
		if(previous==lastTreeNodeOpened) {
			previous = FileExplorer.historyPop();
			if(previous==null) {
				return;
			}

			FileExplorer.futureHistoryPush(previous);
		}

		File file = (File) previous.getUserObject();

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			FileExplorer.selectDirectory(previous);

			return;
		}

		FileExplorer.enterOrOpen(previous);
	}

	public static void historyForward() {
		JTree tree = FileExplorer.getTree();
		DefaultMutableTreeNode next,
						lastTreeNodeOpened = FileExplorer.getLastTreeNodeOpened();

		next=FileExplorer.futureHistoryPop();
		if(next==null)
			return;

		FileExplorer.historyPush(next);
		if(next==lastTreeNodeOpened) {
			next=FileExplorer.futureHistoryPop();
			if(next==null)
				return;
			FileExplorer.historyPush(next);	
		}

		File file = (File) next.getUserObject();

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			FileExplorer.selectDirectory(next);

			return;
		}
		FileExplorer.enterOrOpen(next);
	}

	public static void setNavigationText(String text) {
		navigationField.setText(text);
	}

	public static void setSearchText(String text) {
		searchField.setText(text);
	}

	public static void clearNavButtons() {
		buttonField.removeAll();
		buttonField.add(Box.createRigidArea(new Dimension(6, 0)));
		buttonField.revalidate();
		buttonField.repaint();
	}

	public static void addNavButton(DefaultMutableTreeNode node) {
		File file = (File) node.getUserObject();

		LookAndFeel previousLF = UIManager.getLookAndFeel();
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : 
				javax.swing.UIManager.getInstalledLookAndFeels()) {
				
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}

			JButton button;

			String name = file.getName();
			if(name.trim().length() == 0 || name.equals(windowsTopName)) {
				if(file.getPath().equals("/") || name.equals(windowsTopName)) {
					button = new JButton(Utility.getImageFast(
						FileExplorer.getIconPath() + "other/pc.png", 
						navHeight-4, navHeight-7, true));	
				}
				else {
					name = "Local Disk (" + file.getPath().replace("\\", "") + ")";
					button = new JButton(name);		
				}
			}
			else {
				button = new JButton(name);	
			}

			UIManager.setLookAndFeel(previousLF);

			button.setFocusPainted(false);
			button.setBackground(FileExplorer.topBackgroundColor);
			button.setForeground(FileExplorer.textSelectionColor);
			button.setPreferredSize(new Dimension(button.
				getPreferredSize().width, navHeight));
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent event) {
					button.setForeground(Color.WHITE);
				}
				@Override
				public void mouseExited(MouseEvent event) {
					button.setForeground(FileExplorer.textSelectionColor);
				}
				@Override
				public void mouseReleased(MouseEvent event) {
					FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
					FileExplorer.clearFuture();
					FileExplorer.selectDirectory(node);
					FileExplorer.getFolder().requestFocusInWindow();
					FileExplorer.focusLast();
				}
			});

			buttonField.add(button);
			buttonField.revalidate();
			buttonField.repaint();
		} catch (IllegalAccessException | 
			UnsupportedLookAndFeelException | 
			InstantiationException | ClassNotFoundException e) {}
	}

	public static void toggleNavigation() {
		boolean navShown = navigationField.isVisible();
		boolean buttonShown = buttonField.isVisible();

		if(navShown && !buttonShown) {
			navigationField.setVisible(false);
			buttonField.setVisible(true);
		}
		else if(!navShown && buttonShown){
			navigationField.setVisible(true);
			buttonField.setVisible(false);		
		}
	}

	public static JButton getButtonBack() {
		return buttonBack;
	}

	public static JButton getButtonForward() {
		return buttonForward;
	}
}