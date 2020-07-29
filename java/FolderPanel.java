import java.io.File;

import java.lang.management.ManagementFactory;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JTextArea;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class FolderPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.
													getHiddenFilesOption();
	static Set<String> iconSet = FileExplorer.addExtensions();
	private static String windowsTopName = Tree.getWindowsTopName();

	private static JPanel lastPanelSelected; 
	private static HashMap<JPanel, DefaultMutableTreeNode> mapPanelNode = 
		new HashMap<JPanel, DefaultMutableTreeNode>();

	private static java.util.List<JPanel> selectedList = 
		new java.util.ArrayList<JPanel>();
	
	private static java.util.List<DefaultMutableTreeNode> clipboard = 
		new java.util.ArrayList<DefaultMutableTreeNode>();
	private static String operation = "";

	private static Executor executor = Executors.newSingleThreadExecutor();

	private int x, y, x2, y2;

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.CYAN);

		int px = Math.min(x, x2);
		int py = Math.min(y, y2);
		int pw = Math.abs(x-x2);
		int ph = Math.abs(y-y2);
		g.drawRect(px, py, pw, ph);		
		g.fillRect(px, py, pw, ph);
	}

	public FolderPanel() {
		//Create the folder viewing pane.
		super(new WrapLayout(FlowLayout.LEFT, 10, 10));

		x = y = x2 = y2 = -1;

		this.setBackground(FileExplorer.folderBackgroundColor);

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
				JTree tree = MainWindow.getTree();
				
				if(event.getButton() == MouseEvent.BUTTON1) {
					x = event.getX();
					y = event.getY();
				}

				requestFocusInWindow();
				MainWindow.setFocusExplorer();

				String filePath = ((File) lastTreeNodeOpened.getUserObject()).
															getPath();

				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					Tree.findExistingParent(f);
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
					repaint();

					clearPanelSelection();

					for (int i = 0; i < MainWindow.getFolder().getComponentCount(); i++) {
						Component current = MainWindow.getFolder().getComponent(i);
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
							selectedList.add((JPanel) current);

							for(JPanel element : selectedList) {
								element.setBackground(FileExplorer.panelSelectionColor);
								element.setBorder(BorderFactory.createLineBorder(Color.white));
							}	
						}
					}
					x = y = x2 = y2 = -1; 
					repaint();
				}
			}

			public boolean overlaps(Rectangle z, Rectangle r) {
				return z.x < r.x + r.width && z.x + z.width > r.x && 
					z.y < r.y + r.height && z.y + z.height > r.y;
			}
		});

		this.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent event) {
				if(SwingUtilities.isLeftMouseButton(event)) {
					x2 = event.getX();
					y2 = event.getY();
					repaint();
		
					clearPanelSelection();

					for (int i = 0; i < MainWindow.getFolder().getComponentCount(); i++) {
						Component current = MainWindow.getFolder().getComponent(i);
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
							selectedList.add((JPanel) current);

							for(JPanel element : selectedList) {
								element.setBackground(FileExplorer.panelSelectionColor);
								element.setBorder(BorderFactory.createLineBorder(Color.white));
							}	
						}
					}
				}
            }

			public boolean overlaps(Rectangle z, Rectangle r) {
				return z.x < r.x + r.width && z.x + z.width > r.x && 
					z.y < r.y + r.height && z.y + z.height > r.y;
			}
		});

		this.getActionMap().put("select all", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					for(JPanel current : mapPanelNode.keySet())
							selectedList.add(current);

					for(JPanel element : selectedList) {
						element.setBackground(FileExplorer.panelSelectionColor);
						element.setBorder(BorderFactory.createLineBorder(Color.white));
					}	
				}
			});

		this.getActionMap().put("paste", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for(DefaultMutableTreeNode current : clipboard) {
					try {
						File selected = (File) Tree.getLastTreeNodeOpened().getUserObject();

						MainWindow.pasteFile((File) current.getUserObject(), selected, operation);
					}
					catch(Exception except) {

					}
				}

				MainWindow.refresh(Tree.getLastTreeNodeOpened());
				clipboard.clear();
				operation = "";
			}
		});

		this.getActionMap().put("history back", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TopPanel.historyBack(); 
				MainWindow.focusLast();
			}
		});

		this.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TopPanel.historyForward(); 
				MainWindow.focusLast();
			}
		});

		this.getActionMap().put("select first", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JPanel panel = (JPanel) WrapLayout.getComponent(0);
				selectPanel(panel, true);
			}
		});
		
		this.getActionMap().put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.refresh(Tree.getLastTreeNodeOpened());
				MainWindow.focusLast();
			}
		});

		InputMap inputMap = this.getInputMap();
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
			Utility.getImageFast(ICONPATH + "extensions/txt.png", 17, 17, true));

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
											50, 50, true);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter File Name", "New Text Document", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "File");

				if(name==null || name.equals("")) {
					JOptionPane.showMessageDialog(null, 
						"Can't have empty name");
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
					ICONPATH + "other/folder.png", 50, 50, true);
				name = (String) JOptionPane.showInputDialog(null, 
					"Enter Folder Name", "New Folder", 
					JOptionPane.INFORMATION_MESSAGE, img, null, "Folder");
			
				if(name==null || name.equals("")) {
					JOptionPane.showMessageDialog(null, 
						"Can't have empty name");			
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
	
				MainWindow.selectDirectory(node);
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
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				MainWindow.refresh(lastTreeNodeOpened);
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
						MainWindow.pasteFile((File) current.getUserObject(), selected, operation);
					}
					catch(Exception e) {

					}
				}

				MainWindow.refresh(Tree.getLastTreeNodeOpened());
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

	static public JPopupMenu getFilePopupMenu(
		DefaultMutableTreeNode panelNode, JPanel panel) {

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
				MainWindow.historyPush(Tree.getLastTreeNodeOpened());
				MainWindow.clearFuture();
				if(panelNode!=null)
					MainWindow.enterOrOpen(panelNode);
				else
					MainWindow.enterOrOpen(new DefaultMutableTreeNode(panelFile));
		
				MainWindow.focusLast();
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
						MainWindow.pasteFile((File) current.getUserObject(), panelFile, operation);
					}
					catch(Exception e) {

					}
				}
				
				MainWindow.refresh(Tree.getLastTreeNodeOpened());
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
				MainWindow.rename(panelNode);
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
				MainWindow.delete(panelNode);
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
				MainWindow.properties(panelFile);
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
				JTree tree = MainWindow.getTree();
				JPanel folder = MainWindow.getFolder();
				
				folder.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));

				int numChild=tree.getModel().getChildCount(node);
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
					
						JPanel newPanel;
						if(currentFile.isDirectory())
							newPanel = getPanel("folder.png", currentNode);
						else if(iconSet.contains(Utility.getExtension(
							currentFile.getName()))) {
						
							newPanel = getPanel(Utility.getExtension(
								currentFile.getName()) + ".png", currentNode);
						}
						else
							newPanel = getPanel("question.png", currentNode);
				
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
						if(element.isDirectory()) {
							newPanel = getPanel("folder.png", 
								new DefaultMutableTreeNode(element));
						}
						else if(iconSet.contains(Utility.getExtension(
							element.getName()))) {
							
							newPanel = getPanel(Utility.getExtension(
								element.getName()) + ".png", 
								new DefaultMutableTreeNode(element));
						}
						else {
							newPanel = getPanel("question.png", 
								new DefaultMutableTreeNode(element));
						}
		
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

	static JPanel getPanel(String iconName, DefaultMutableTreeNode panelNode) {
		JLabel label;
		ImageIcon img=null;
		Set<String> set = new HashSet<>();
		File panelFile = (File) panelNode.getUserObject();
		String name = panelFile.getName(), path;
		String extension = Utility.getExtension(panelFile.getName());

		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(panelFile);
			name = description + " (" + panelFile.getPath().replace("\\", "") + ")";
			
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
			if(iconName=="folder.png") {
				if(panelFile.list()!=null && panelFile.list().length==0)
					img = Utility.getImageFast(FileExplorer.getIconPath() 
						+ "other/folderempty.png", 60, 60, true);
				else {
					img = Utility.getImageFast(FileExplorer.getIconPath() 
						+ "other/folder.png", 60, 60, true);
				}
			}
			else if(iconName=="question.png") {
				img = Utility.getImageFast(FileExplorer.getIconPath() 
					+ "other/question.png", 60, 60, true);
			}
			else {
				img = Utility.getImageFast(ICONPATH + "extensions/" + iconName, 
					60, 60, true);
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

		panel.getActionMap().put("select all", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					for(JPanel current : mapPanelNode.keySet())
							selectedList.add(current);

					for(JPanel element : selectedList) {
						element.setBackground(FileExplorer.panelSelectionColor);
						element.setBorder(BorderFactory.createLineBorder(Color.white));
					}	
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
						MainWindow.rename(panelNode);		
				}
			});

		panel.getActionMap().put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.refresh(Tree.getLastTreeNodeOpened());
				MainWindow.focusLast();
			}
		});

		panel.getActionMap().put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				MainWindow.delete(panelNode);		
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
				TopPanel.historyBack(); 
				MainWindow.focusLast();
			}
		});

		panel.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TopPanel.historyForward(); 
				MainWindow.focusLast();
			}
		});

		panel.getActionMap().put("enter", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(selectedList.size()==1) {
					MainWindow.historyPush(Tree.getLastTreeNodeOpened());
					MainWindow.clearFuture();
					if(panelNode!=null)
						MainWindow.enterOrOpen(panelNode);
					else
						MainWindow.enterOrOpen(new DefaultMutableTreeNode(panelFile));
					MainWindow.focusLast();
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
		panel.add(label, BorderLayout.SOUTH);

		panel.setName(name);
		panel.setBorder(BorderFactory.createLineBorder(FileExplorer.folderBackgroundColor));
		panel.setBackground(FileExplorer.folderBackgroundColor);

		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {		
				if(!selectedList.contains(panel))
					panel.setBackground(FileExplorer.panelHoverColor);
			}
			@Override
			public void mouseExited(MouseEvent event) {
				if(!selectedList.contains(panel))
					panel.setBackground(FileExplorer.folderBackgroundColor);
			}
			@Override
			public void mousePressed(MouseEvent event) {
				if(!panelFile.exists()) {
					Tree.findExistingParent(panelFile);
					return;
				}

				MainWindow.setFocusExplorer();

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
					MainWindow.historyPush(Tree.getLastTreeNodeOpened());
					MainWindow.clearFuture();
					MainWindow.enterOrOpen(panelNode);

					MainWindow.focusLast();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getFilePopupMenu(panelNode, panel);
					menu.show(event.getComponent(), event.getX(), event.getY());
				}
			}
			@Override
			public void mouseReleased(MouseEvent event) {}
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
			panel.setBackground(FileExplorer.panelSelectionColor);
			panel.setBorder(BorderFactory.createLineBorder(Color.white));	
			selectedList.add(panel);
		}
		else {
			if(selectedList.contains(panel)) {
				panel.setBackground(FileExplorer.folderBackgroundColor);
				panel.setBorder(BorderFactory.createLineBorder(
					FileExplorer.folderBackgroundColor));	
				selectedList.remove(panel);
			}
			else
				selectedList.add(panel);

			for(JPanel element : selectedList) {
				element.setBackground(FileExplorer.panelSelectionColor);
				element.setBorder(BorderFactory.createLineBorder(Color.white));
			}
		}

		panel.requestFocusInWindow();
		lastPanelSelected = panel;
	}

	static public void clearPanelSelection() {
		for(JPanel element : selectedList) {
			element.setBackground(FileExplorer.folderBackgroundColor);
			element.setBorder(BorderFactory.createLineBorder(
				FileExplorer.folderBackgroundColor));
		}
		selectedList.clear();

		if(lastPanelSelected!=null) {
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
}
