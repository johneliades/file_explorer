import java.io.File;

import java.lang.management.ManagementFactory;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class FolderPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.
													getHiddenFilesOption();
	static Set<String> iconSet = FileExplorer.addExtensions();
	private static String windowsTopName = Tree.getWindowsTopName();

	private static JPanel currentPanelSelected; 
	private static String currentPanelName="";

	public FolderPanel() {
		//Create the folder viewing pane.
		super(new WrapLayout(FlowLayout.LEFT, 10, 10));
		this.setBackground(new Color(49, 49, 49));

		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {}
			@Override
			public void mouseExited(MouseEvent event) {}
			@Override
			public void mousePressed(MouseEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.
													getLastTreeNodeOpened();
				DefaultMutableTreeNode lastPanelNode = MainWindow.
													getLastPanelNode();
				JTree tree = MainWindow.getTree();
				
				requestFocusInWindow();

				String filePath = ((File) lastTreeNodeOpened.getUserObject()).
															getPath();

				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					Tree.findExistingParent(f);
					return;
				}


				if(event.getButton() == MouseEvent.BUTTON1) {
					clearLastPanelSelection();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getBackgroundPopupMenu();

					if(tree.getLastSelectedPathComponent()!=null)
						menu.show(event.getComponent(), event.getX(), 
													event.getY());

					clearLastPanelSelection();
				}
			}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});

		this.addKeyListener(new KeyListener() {
			boolean alt_pressed = false;

			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.
													getLastTreeNodeOpened();
				JPanel panel;

				switch(e.getKeyCode()) {
					case KeyEvent.VK_ALT:
						alt_pressed = true;
						break;
					case KeyEvent.VK_F5:
						MainWindow.refresh(lastTreeNodeOpened);
						break;

					case KeyEvent.VK_LEFT: 
						if(alt_pressed) {TopPanel.historyBack(); break;}
					case KeyEvent.VK_RIGHT: 
						if(alt_pressed) {TopPanel.historyForward(); break;}
					case KeyEvent.VK_UP:
					case KeyEvent.VK_DOWN:
						panel = (JPanel) WrapLayout.getComponent(0);
						selectPanel(panel);

						break;

					case KeyEvent.VK_BACK_SPACE:
						TopPanel.historyBack();

						break;
					default:
				}

			}
			@Override
			public void keyReleased(KeyEvent e) {
				switch(e.getKeyCode()) {
					case KeyEvent.VK_ALT:
						alt_pressed = false;
						break;
				}
			}
		});

		this.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
			}
			public void focusLost(FocusEvent e) {}
		});	
	}

	public JPopupMenu getBackgroundPopupMenu() {
		File selected = (File) Tree.getLastTreeNodeOpened().getUserObject();

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		JMenu sectionsMenu = new JMenu("  New");

		//New submenu(txt, folder)

		menuItem = new JMenuItem("  Text Document");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "extensions/txt.png", 17, 17));

		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.
											getLastTreeNodeOpened();
				JTree tree = MainWindow.getTree();

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
											50, 50);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter File Name", "New Text Document", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "File");
				if(name==null || name.equals(""))
					return;

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
					JOptionPane.showMessageDialog(null, "File with that name already exists!");
					return;
				}

				showCurrentDirectory(node);
			}
		});

		menuItem.setBackground(Color.white);

		sectionsMenu.add(menuItem);


		menuItem = new JMenuItem("  Folder");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/folder.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
				JTree tree = MainWindow.getTree();

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
					ICONPATH + "other/folder.png", 50, 50);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter Folder Name", "New Folder", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "Folder");
				
				if(name==null || name.equals(""))
					return;

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
					JOptionPane.showMessageDialog(null, "Directory with that name already exists!");
					return;
				}

				/* add node to tree and reload tree here */
				DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
				defMod1.reload();
	
				MainWindow.selectDirectory(node);
			}
		});

		menuItem.setBackground(Color.white);
		sectionsMenu.add(menuItem);
	

		sectionsMenu.setIcon(Utility.getImageFast(
					ICONPATH + "other/plus.png", 17, 17));	

		if(selected.exists() && selected.canWrite()) {
			popupMenu.addSeparator();
			popupMenu.add(sectionsMenu);
			popupMenu.addSeparator();
		}

		popupMenu.setBorder(new CompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red), 
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black)));
		popupMenu.setBackground(Color.white);	

		//Refresh option

		menuItem = new JMenuItem("  Refresh");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/refresh.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				MainWindow.refresh(lastTreeNodeOpened);
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);
		popupMenu.addSeparator();

		menuItem = new JMenuItem("  OS Explorer");
		menuItem.setIcon(Utility.getImageFast(ICONPATH + 
								"other/osexplorer.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

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
				getImageFast(ICONPATH + "other/folder.png", 17, 17));
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

				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

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

	static public JPopupMenu getFilePopupMenu(File selected, 
											DefaultMutableTreeNode node) {

		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		ImageIcon img=null;

		menuItem = new JMenuItem("  Open");
		img = Utility.getImageFast(ICONPATH + "other/open.png", 17, 17);
		menuItem.setIcon(img);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				MainWindow.historyPush(Tree.getLastTreeNodeOpened());
				MainWindow.clearFuture();
				MainWindow.enterOrOpen(selected, node);
				MainWindow.getFolder().requestFocusInWindow();
			}
		});
		menuItem.setBackground(Color.white);
		if(selected.exists() && selected.canRead())
			popupMenu.add(menuItem);
	
		menuItem = new JMenuItem("  New Window");
		menuItem.setIcon(Utility.getImageFast(ICONPATH + 
			"other/folder.png", 17, 17));
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
		if(selected.exists() && selected.canRead())
			popupMenu.add(menuItem);

		popupMenu.addSeparator();

		menuItem = new JMenuItem("  Rename");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/rename.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.
												getLastTreeNodeOpened();

				MainWindow.renameSon(lastTreeNodeOpened);
			}
		});
			
		menuItem.setBackground(Color.white);
		if(selected.exists() && selected.canRead())
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Delete");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/delete.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.
												getLastTreeNodeOpened();

				MainWindow.deleteSon(lastTreeNodeOpened);
			}
		});

		menuItem.setBackground(Color.white);
		if(selected.exists() && selected.canWrite())
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Properties");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "other/properties.png", 17, 17));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.out.println("Properties");
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);

		popupMenu.setBorder(new CompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red), 
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black)));		
		popupMenu.setBackground(Color.white);
		
		return popupMenu;
	}

	public static void showCurrentDirectory(DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();
		JPanel folder = MainWindow.getFolder();
		
		folder.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));

		int numChild=tree.getModel().getChildCount(node);
		SortedSet<File> set2;
		Iterator it;
		DefaultMutableTreeNode currentNode;
		File currentFile;
		
		String FileName;
	
		FileName = ((File) node.getUserObject()).getName();

		if(FileName.isEmpty())
			FileName = ((File) node.getUserObject()).getPath();
		
		TopPanel.setNavigationText(((File) node.getUserObject()).getPath());
		TopPanel.setSearchText("Search" + " \"" + FileName + "\"");
		TopPanel.clearNavButtons();
		
		TreePath path = new TreePath(node.getPath());
		for(int i=0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode current =  (DefaultMutableTreeNode)
				path.getPathComponent(i);

			TopPanel.addNavButton(current);
		}

		folder.removeAll();

		final class NodeInfo implements Comparable {
			public DefaultMutableTreeNode node;
			public File file;
		
			NodeInfo(DefaultMutableTreeNode node, File file) {
				this.node=node;
				this.file=file;
			}

			@Override
			public int compareTo(Object obj) {
				NodeInfo emp = (NodeInfo) obj;
			   
				return file.compareTo(emp.file);
			}
		}

		SortedSet<NodeInfo> set1 = new TreeSet<>();

		for(int i=0; i<numChild; i++) { 
			currentNode = (DefaultMutableTreeNode) tree.getModel().getChild(node, i);
			currentFile =(File) currentNode.getUserObject();
			
			NodeInfo current = new NodeInfo(currentNode, currentFile);

			set1.add(current);
		}

		it=set1.iterator();

		while (it.hasNext()) {
			NodeInfo current = (NodeInfo) it.next();
			currentNode = current.node;
			currentFile = current.file;

			if(showHiddenFiles ?  true : !currentFile.isHidden() || 
								!currentFile.getName().startsWith(".")) {
				if(currentFile.isDirectory())
					folder.add(getPanel("folder.png", currentFile, currentNode));
				else if(iconSet.contains(Utility.getExtension(currentFile.getName())))
					folder.add(getPanel(Utility.getExtension(
						currentFile.getName()) + ".png", currentFile, currentNode));
				else
					folder.add(getPanel("question.png", currentFile, currentNode));
			}
		}
		
		folder.repaint();
		folder.revalidate();

		set2 = new TreeSet<>();
		currentFile=(File) node.getUserObject();
		File children[] = currentFile.listFiles();

		if(children==null)
			return;

		set2.addAll(Arrays.asList(children));

		it = set2.iterator();
		while (it.hasNext()) {
			File element = (File) it.next();
  
			if(element.isFile()) {
				if(showHiddenFiles ? true : !element.isHidden() || 
									!element.getName().startsWith(".")) {
					if (element.isDirectory())
						folder.add(getPanel("folder.png", element, null));
					else if(iconSet.contains(Utility.getExtension(
											element.getName()))) {
						folder.add(getPanel(Utility.getExtension(
							element.getName()) + ".png", element, null));
					}
					else
						folder.add(getPanel("question.png", element, null));
				}
			}
		}

		folder.repaint();
		folder.revalidate();
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

	static JPanel getPanel(String iconName, File file, DefaultMutableTreeNode node) {
		JLabel label;
		ImageIcon img=null;
		Set<String> set = new HashSet<>(); 
		String name = file.getName(), path;
		String extension = Utility.getExtension(file.getName());

		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(file);
			name = description + " (" + file.getPath().replace("\\", "") + ")";
			
			path = ICONPATH + "other/harddiskfolder.png";

			if(description.equals("CD Drive")) {
				path = ICONPATH + "other/cd.png";
			}
			else if(description.equals("DVD Drive")) {
				path = ICONPATH + "other/dvd.png";
			}
			else if(description.equals("USB Drive")) {
				path = ICONPATH + "other/usb.png";			
			}

			img = Utility.getImageFast(path, 60, 60);
		}

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(extension)) {
			img = Utility.getImageFast(file.getPath(), 60, 60);
		}

		if(img==null) {
			if(iconName=="folder.png") {
				if(file.list()!=null && file.list().length==0)
					img = Utility.getImageFast(FileExplorer.getIconPath() 
						+ "other/folderempty.png", 60, 60);
				else {
					img = Utility.getImageFast(FileExplorer.getIconPath() 
						+ "other/folder.png", 60, 60);
				}
			}
			else if(iconName=="question.png") {
				img = Utility.getImageFast(FileExplorer.getIconPath() 
					+ "other/question.png", 60, 60);
			}
			else {
				img = Utility.getImageFast(ICONPATH + "extensions/" + iconName, 
					60, 60);
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
		
		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(150, 10));
		panel.add(label,  BorderLayout.NORTH);

		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(10, 40));
		panel.add(label,  BorderLayout.EAST);

		label = new JLabel("", JLabel.CENTER);
		label.setPreferredSize(new Dimension(10, 40));
		panel.add(label,  BorderLayout.WEST);

		label = new JLabel(img, JLabel.CENTER);
		label.setPreferredSize(new Dimension(60, 60));
		//Border b = new BevelBorder(BevelBorder.RAISED, Color.LIGHT_GRAY, Color.DARK_GRAY);
		//label.setBorder(b);
		panel.add(label,  BorderLayout.CENTER);

		label = new JLabel(name, JLabel.CENTER);
		label.setPreferredSize(new Dimension(150, 30));
		label.setForeground (Color.white);
		panel.add(label, BorderLayout.SOUTH);

		panel.setName(name);
		panel.setBorder(BorderFactory.createLineBorder(new Color(49, 49, 49)));
		panel.setBackground(new Color(49, 49, 49));

		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {		
				if(currentPanelSelected!=panel)
					panel.setBackground(new Color(0, 170, 170));
			}
			@Override
			public void mouseExited(MouseEvent event) {
				if(currentPanelSelected!=panel)
					panel.setBackground(new Color(49, 49, 49));
			}
			@Override
			public void mousePressed(MouseEvent event) {
				if(!file.exists()) {
					Tree.findExistingParent(file);
					return;
				}

				selectPanel(panel);

				if(event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
					MainWindow.historyPush(Tree.getLastTreeNodeOpened());
					MainWindow.clearFuture();
					MainWindow.enterOrOpen(file, node);
					MainWindow.getFolder().requestFocusInWindow();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getFilePopupMenu(file, node);

					menu.show(event.getComponent(), event.getX(), event.getY());
				}
			}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});

		panel.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				DefaultMutableTreeNode previous, lastTreeNodeOpened = 
									Tree.getLastTreeNodeOpened();
				JPanel current=null;
				int position = WrapLayout.getIndex(currentPanelSelected);

				switch(e.getKeyCode()) {
					case KeyEvent.VK_F2:
						MainWindow.renameSon(lastTreeNodeOpened);		
						break;

					case KeyEvent.VK_F5:
						MainWindow.refresh(lastTreeNodeOpened);
						break;

					case KeyEvent.VK_DELETE:
						MainWindow.deleteSon(lastTreeNodeOpened);		
						break;
				
					case KeyEvent.VK_ENTER:
						MainWindow.historyPush(Tree.getLastTreeNodeOpened());
						MainWindow.clearFuture();
						MainWindow.enterOrOpen(file, node);
						MainWindow.getFolder().requestFocusInWindow();

						break;
				
					case KeyEvent.VK_LEFT:
						current = (JPanel) WrapLayout.getComponent(position - 1);
						break;
				
					case KeyEvent.VK_DOWN:
						current = (JPanel) WrapLayout.
									getComponent(position + WrapLayout.getRowLength());
						break;
				
					case KeyEvent.VK_UP:
						current = (JPanel) WrapLayout.
									getComponent(position - WrapLayout.getRowLength());
						break;
				
					case KeyEvent.VK_RIGHT:
						current = (JPanel) WrapLayout.getComponent(position + 1);
						break;

					case KeyEvent.VK_BACK_SPACE:
						TopPanel.historyBack();
						MainWindow.getFolder().requestFocusInWindow();

						break;

					default:
				}

				if(current!=null) {
					selectPanel(current);
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
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

	static public void selectPanel(JPanel panel) {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode lastTreeNodeOpened = 
				Tree.getLastTreeNodeOpened();
		DefaultMutableTreeNode current = null, parent = lastTreeNodeOpened;
		File curFile=null;

		if(panel==null)
			return;

		clearLastPanelSelection();
		panel.setBackground(new Color(0, 100, 100));
		panel.setBorder(BorderFactory.createLineBorder(Color.white));
		panel.requestFocusInWindow();
		currentPanelSelected = panel;

		// Get node and name of last selected panel
		String name=currentPanelSelected.getName();

		currentPanelName = name;

		int i, numChild=tree.getModel().getChildCount(parent);
		for(i=0; i<numChild; i++) { 
			current=(DefaultMutableTreeNode) tree.getModel().getChild(parent, i);
			curFile=(File) (current).getUserObject();
			if(curFile.getName().compareTo(name)==0) {
				MainWindow.setLastPanelNode(current);	
				break;
			}
			else
				MainWindow.setLastPanelNode(null);
		}

		// /Get node and name of last selected panel
	}

	static public void clearLastPanelSelection() {
		if(currentPanelSelected!=null) {
			currentPanelSelected.setBackground(new Color(49, 49, 49));
			currentPanelSelected.setBorder(BorderFactory.createLineBorder(
				new Color(49, 49, 49)));
			currentPanelSelected=null;
			MainWindow.setLastPanelNode(null);
			currentPanelName=null;
		}
	}

	public static JPanel getCurrentPanelSelected() {
		return currentPanelSelected;
	}

	public static String getCurrentPanelName() {
		return currentPanelName;
	}
}
