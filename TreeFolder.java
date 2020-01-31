import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class TreeFolder extends JPanel implements TreeSelectionListener {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.getHiddenFilesOption();
	static Set<String> iconSet = FileExplorer.addExtensions();

	private static JPanel folder;
	private static JTree tree;

	static JPanel lastPanelSelected; 

	public TreeFolder() {
		super(new GridLayout(1, 0));

		//Create the nodes.
		File roots[]=File.listRoots();
		
		DefaultMutableTreeNode top;
		if(roots.length==1)
			top = new DefaultMutableTreeNode(roots[0]);
		else {
			top = new DefaultMutableTreeNode(new MyFile("This PC"));
			for (File root : roots) {
				top.add(new DefaultMutableTreeNode(root));
			}
		}

		//Create a tree that allows one selection at a time.
		tree = new JTree(top);
		tree.setBorder(new EmptyBorder(5, 10, 5, 0)); //top,left,bottom,right
		tree.putClientProperty("JTree.lineStyle", "None");

		tree.setEditable(true);
		tree.setCellRenderer(getRenderer());
		tree.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//Listen for when the selection changes.
		tree.addTreeSelectionListener(this);
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {}
	  
			@Override
			public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
				TreePath path = treeExpansionEvent.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				File current;

				String data = node.getUserObject().toString();

				current = (File) node.getUserObject();
				if (current.isDirectory()) {
					createNodes(node, 0);
				}
			}
		});

		tree.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				if(node==null)
					return;

				showCurrentDirectory(node);
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		tree.addKeyListener(new KeyListener() {
			boolean pressed = false;

			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && !pressed) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
					pressed = true;
					showCurrentDirectory(node);
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					pressed = false;
				}
			}
		});

		//Create the scroll pane and add the tree to it. 
		JScrollPane treeView = new JScrollPane(tree);
		treeView.getVerticalScrollBar().setPreferredSize(new Dimension(13, 0));
		treeView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 13));

		//Create the folder viewing pane.
		folder = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
		folder.setBackground(Color.white);

		folder.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if(event.getButton() == MouseEvent.BUTTON1) {
					if(lastPanelSelected!=null) {
						lastPanelSelected.setBackground(Color.white);
						lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
						lastPanelSelected=null;
					}
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getBackgroundPopupMenu();

					if(tree.getLastSelectedPathComponent()!=null)
						menu.show(event.getComponent(), event.getX(), event.getY());

					if(lastPanelSelected!=null) {
						lastPanelSelected.setBackground(Color.white);
						lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
						lastPanelSelected=null;
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {}
			@Override
			public void mouseExited(MouseEvent event) {}
			@Override
			public void mousePressed(MouseEvent event) {}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});

		JScrollPane folderView = new JScrollPane(folder);
		folderView.getVerticalScrollBar().setUnitIncrement(16);
		folderView.getVerticalScrollBar().setPreferredSize(new Dimension(13, 0));
		folderView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 13));

		if(roots.length==1)
			createNodes(top, 0);
		else {
			int numChild=tree.getModel().getChildCount(top);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
				createNodes(current, 0);
			}
		}		
		TreePath path = new TreePath(top.getPath());
		tree.setSelectionPath(path);
		tree.expandPath(path);
		
		showCurrentDirectory(top);

		treeView.getVerticalScrollBar().setValue(0);

		folderView.setMinimumSize(new Dimension(400, 50));
		treeView.setMinimumSize(new Dimension(250, 50));

		//Add the scroll panes to a split pane.
		JSplitPaneWithZeroSizeDivider splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(folderView);

		//Add the split pane to this panel.
		add(splitPane);
	}

	public static JPanel getFolder() {
		return folder;
	}

	public static JTree getTree() {
		return tree;
	}

	public static void showCurrentDirectory(DefaultMutableTreeNode node) {
		int numChild=tree.getModel().getChildCount(node);
		SortedSet<File> set2;
		Iterator it;
		DefaultMutableTreeNode currentNode;
		File currentFile;
		
		String FileName;
		
		FileName = ((File) node.getUserObject()).getName();
		
		if(FileName.isEmpty())
			FileName = ((File) node.getUserObject()).getPath();
		
		TopPanel.getNavigationField().setText(" " + ((File) node.getUserObject()).getPath());
		TopPanel.getSearchField().setText(" Search" + " \"" + FileName + "\"");

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

			if(showHiddenFiles ?  true : !currentFile.isHidden() || !currentFile.getName().startsWith(".")) {
				if (currentFile.isDirectory())
					folder.add(getIcon("folder.png", currentFile, currentNode));
				else if(iconSet.contains(Utility.getExtension(currentFile.getName())))
					folder.add(getIcon(Utility.getExtension(currentFile.getName()) + ".png", currentFile, currentNode));
				else
					folder.add(getIcon("question.png", currentFile, currentNode));
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
				if(showHiddenFiles ?  true : !element.isHidden() || !element.getName().startsWith(".")) {
					if (element.isDirectory())
						folder.add(getIcon("folder.png", element, null));
					else if(iconSet.contains(Utility.getExtension(element.getName())))
						folder.add(getIcon(Utility.getExtension(element.getName()) + ".png", element, null));
					else
						folder.add(getIcon("question.png", element, null));
				}
			}
		}

		folder.repaint();
		folder.revalidate();
	}

	static public JPopupMenu getFilePopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		ImageIcon img=null;
		Image folderImg;

		menuItem = new JMenuItem("Rename");
		img = new ImageIcon(ICONPATH + "other/rename.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				Component curComponents[] = lastPanelSelected.getComponents();
				String filePath = ((File) ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject()).getPath();
				DefaultMutableTreeNode current=null, parent= (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				String nameOld = curComponents[4].getName(), nameNew;
				ImageIcon img=null;
				Image folderImg;
				int i;
		
				File f = new File(filePath + "/" + nameOld);

				if(f.exists() && f.isFile()) {
					img = new ImageIcon(ICONPATH + "other/rename.png");
					folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
					nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name", "Rename",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, nameOld);

					if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals(""))
						return;

					File file2 = new File(filePath + "/" + nameNew);

					if(file2.exists()) {
						JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
						return;
					}

					boolean success = f.renameTo(file2);

					if (!success) {
						JOptionPane.showMessageDialog(null, "Rename Failed!");
						return;
					}
				}
				else if(f.exists() && f.isDirectory()){
					img = new ImageIcon(ICONPATH + "other/rename.png");
					folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
					nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name", "Rename",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, nameOld);
					
					if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals(""))
						return;

					File file2 = new File(filePath + "/" + nameNew);

					if(file2.exists()) {
						JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
						return;
					}

					int numChild=tree.getModel().getChildCount(parent);

					for(i=0; i<numChild; i++) { 
						current=(DefaultMutableTreeNode) tree.getModel().getChild(parent, i);
						File curFile=(File) (current).getUserObject();
						if(curFile.getName().compareTo(nameOld)==0)
							break;
					}
					if(current==null || i==numChild || ((File) current.getUserObject()).exists()==false) {
						JOptionPane.showMessageDialog(null, "This should never happen...!");
						return;
					}

					boolean success = f.renameTo(file2);

					if(!success) {
						JOptionPane.showMessageDialog(null, "Rename Failed!");
						return;
					}

					current.removeFromParent();
					DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
					defMod1.reload();
				}
				else {
					JOptionPane.showMessageDialog(null, "Rename Failed!");
					return;
				}

				TreePath path = new TreePath(parent.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);

				showCurrentDirectory(parent);
			}
		});
		
		popupMenu.add(menuItem);

		menuItem = new JMenuItem("Delete");
		img = new ImageIcon(ICONPATH + "other/delete.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

				deleteSon(node);
			
				TreePath path = new TreePath(node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);

				showCurrentDirectory(node);
			}
		});

		popupMenu.add(menuItem);

		return popupMenu;
	}

	public JPopupMenu getBackgroundPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		JMenu sectionsMenu = new JMenu("New");
		ImageIcon img=null;
		Image folderImg;

		//New submenu(txt, folder)

		menuItem = new JMenuItem("Text Document");
		img = new ImageIcon(ICONPATH + "extensions/txt.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				String filePath = ((File) node.getUserObject()).getPath();
				String name;
				ImageIcon img=null;
				Image folderImg;

				img = new ImageIcon(ICONPATH + "extensions/txt.png");
				folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
				name=(String) JOptionPane.showInputDialog(null, "Enter File Name", "New Text Document",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, "File");
				if(name==null || name.equals(""))
					return;

				File f;
				if(Utility.getExtension(name).equals("txt"))
					f = new File(filePath + "/" + name);
				else
					f = new File(filePath + "/" + name + ".txt");

				if(!f.exists()) {
					try {
						f.createNewFile();
					}
					catch(IOException e) {
						System.out.println(e.getMessage());
					}
				}
				else {
					JOptionPane.showMessageDialog(null, "File with that name already exists!");
					return;
				}

				showCurrentDirectory(node);
			}
		});
		sectionsMenu.add(menuItem);

		menuItem = new JMenuItem("Folder");
		img = new ImageIcon(ICONPATH + "extensions/folder.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				String filePath = ((File) node.getUserObject()).getPath();
				String name;
				ImageIcon img=null;
				Image folderImg;	

				img = new ImageIcon(ICONPATH + "extensions/folder.png");
				folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
				name=(String) JOptionPane.showInputDialog(null, "Enter Folder Name", "New Folder",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, "Folder");
				if(name==null || name.equals(""))
					return;

				File f = new File(filePath + "/" + name);
				if(!f.exists()){
					try {
						f.mkdir();
					}
					catch(Exception e) {
						System.out.println(e.getMessage());
					}
				}
				else {
					JOptionPane.showMessageDialog(null, "Directory with that name already exists!");
					return;
				}

				/* add node to tree and reload tree here */
				DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
				defMod1.reload();
	
				TreePath path = new TreePath(node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);
				
				showCurrentDirectory(node);
			}
		});

		sectionsMenu.add(menuItem);
	
		img = new ImageIcon(ICONPATH + "other/plus.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		sectionsMenu.setIcon(new ImageIcon(folderImg));
		popupMenu.add(sectionsMenu);

		//Refresh option

		menuItem = new JMenuItem("Refresh");
		img = new ImageIcon(ICONPATH + "other/refresh.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

				node.removeAllChildren();
				DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
				defMod1.reload();
			
				TreePath path = new TreePath(node.getPath());
				if(path.toString().equals("[This PC]")) {
					//Create root nodes.
					File roots[]=File.listRoots();

					for (File root : roots) {
						node.add(new DefaultMutableTreeNode(root));
					}

					int numChild=defMod1.getChildCount(node);
					for(int i=0; i<numChild; i++) { 
						DefaultMutableTreeNode current=(DefaultMutableTreeNode) defMod1.getChild(node, i);
						createNodes(current, 0);
					}
				}
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);

				showCurrentDirectory(node);
			}
		});

		popupMenu.add(menuItem);

		return popupMenu;
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

	static JPanel getIcon(String iconName, File file, DefaultMutableTreeNode node) {
		JLabel label;
		ImageIcon img=null;
		Image folderImg;
		Set<String> set = new HashSet<>(); 
		String name = file.getName();
		String extension = Utility.getExtension(file.getName());
		
		if(name.trim().length() == 0) {
			name = "Local Disk(" + file.getPath().replace("\\", "") + ")";
			img = new ImageIcon(ICONPATH + "extensions/harddisk.png");
		}

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(extension)) {
			img = new ImageIcon(file.getPath());
		}

		if(img==null)
			img = new ImageIcon(ICONPATH + "extensions/" + iconName);

		//Image folderImg = img.getImage().getScaledInstance(150, 60, Image.SCALE_DEFAULT);
		folderImg = img.getImage().getScaledInstance(60, 60, Image.SCALE_DEFAULT);

		Icon icon;
		/* If false is removed you get small resolution system icons
		Waiting for official better way
		if(!iconSet.contains(extension) && iconName!="folder.png") {
			icon = FileSystemView.getFileSystemView().getSystemIcon(file);
			folderImg = iconToImage(icon).getScaledInstance(60, 60, Image.SCALE_DEFAULT);
		}
		*/

		img = new ImageIcon(folderImg);

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
		panel.add(label, BorderLayout.SOUTH);

		label.setName(name);
		panel.setName(name);
		panel.setBorder(BorderFactory.createLineBorder(Color.white));
		panel.setBackground(Color.white);

		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {

				if(lastPanelSelected!=null) {
					lastPanelSelected.setBackground(Color.white);
					lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
				}
				panel.setBackground(new Color(0x3fa9ff));
				panel.setBorder(BorderFactory.createLineBorder(Color.black));
				lastPanelSelected=panel;

				if(event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
					if(file.isDirectory()) {
						TreePath path = new TreePath(node.getPath());
						tree.setSelectionPath(path);
						tree.scrollPathToVisible(path);
						tree.expandPath(path);

						showCurrentDirectory(node);
					}
					else {
						try {
							Desktop.getDesktop().open(file);
						}
						catch(IOException e) {

						}

					}
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getFilePopupMenu();

					menu.show(event.getComponent(), event.getX(), event.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {
				if(lastPanelSelected!=panel)
					panel.setBackground(new Color(0x8fd2ff));
			}
			@Override
			public void mouseExited(MouseEvent event) {
				if(lastPanelSelected!=panel)
					panel.setBackground(Color.white);
			}
			@Override
			public void mousePressed(MouseEvent event) {}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});

		return panel;
	}

 	/* Targets selected node when clicked in tree */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		File current;
		
		if (node == null) 
			return;

		current = (File) node.getUserObject();
		if (current.isDirectory()) {
			createNodes(node, 0);
		}
	}

	static public void createNodes(DefaultMutableTreeNode top, int setting) {
		DefaultMutableTreeNode file = null;
		int i, numChild;
		SortedSet<File> set = new TreeSet<>();
		Iterator it;

		File curDir = (File) top.getUserObject();
		File current, children[] = curDir.listFiles(); 

		numChild=tree.getModel().getChildCount(top);

		if(children==null)
			return;

		for(File element : children) {
			if(!showHiddenFiles && (element.isHidden() || element.getName().startsWith(".")))
				continue;

			if(element.isDirectory())
				set.add(element);
		}

		it=set.iterator();
		while (it.hasNext()) {
			File element = (File) it.next();

			for(i=0; i<numChild; i++) { 
				current=(File) ((DefaultMutableTreeNode) tree.getModel().getChild(top, i)).getUserObject();
				if(current.getName().compareTo(element.getName())==0) {
					file=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
					break;
				}
			}

			if(i==numChild) {
				file=new DefaultMutableTreeNode(new MyFile(element.getPath()));
				top.add(file);
			}

			if(setting==0)
				createNodes(file, 1);
		}
	}

	private DefaultTreeCellRenderer getRenderer() {
		DefaultTreeCellRenderer tRenderer = new DefaultTreeCellRenderer();

		ImageIcon folderIcon = new ImageIcon(ICONPATH + "extensions/folder.png");
		Image folderImg = folderIcon.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
		folderIcon = new ImageIcon(folderImg);
		
		ImageIcon folderIconOpen = new ImageIcon(ICONPATH + "other/folderopen.png");
		Image folderImgOpen = folderIconOpen.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
		folderIconOpen = new ImageIcon(folderImgOpen);

		ImageIcon folderIconEmpty = new ImageIcon(ICONPATH + "other/folderempty.png");
		Image folderImgEmpty = folderIconEmpty.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
		folderIconEmpty = new ImageIcon(folderImgEmpty);

		tRenderer.setLeafIcon(folderIconEmpty);
		tRenderer.setClosedIcon(folderIcon);
		tRenderer.setOpenIcon(folderIconOpen);
		tRenderer.setTextSelectionColor(Color.RED);

		return tRenderer;
	}

	static void deleteSon(DefaultMutableTreeNode node) {
		Component curComponents[] = lastPanelSelected.getComponents();
		String filePath = ((File) node.getUserObject()).getPath();
		DefaultMutableTreeNode current=null;
		String name = curComponents[4].getName();
		ImageIcon img=null;
		Image folderImg;
		int i;

		if(name==null) {
			return;
		}
		File f = new File(filePath + "/" + name);

		img = new ImageIcon(ICONPATH + "other/delete.png");
		folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);

		int input = JOptionPane.showConfirmDialog(null, "Delete \"" + name + "\" ?",
					"Any deletion is permanent", JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg));
	
		if(input==2 || input==-1)
			return;

		if(f.exists() && f.isFile()){
			try {
				f.delete();
			}
			catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
		else if(f.exists() && f.isDirectory()) {
			int numChild=tree.getModel().getChildCount(node);
			for(i=0; i<numChild; i++) {
				current=(DefaultMutableTreeNode) tree.getModel().getChild(node, i);
				if(((File) current.getUserObject()).getName().compareTo(name)==0)
					break;
			}
			if(current==null || i==numChild || ((File) current.getUserObject()).exists()==false)
				return;

			current.removeAllChildren();
			current.removeFromParent();
			removeDirectory(f);
			f.delete(); 

			DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
			defMod1.reload();
		}
		else {
			JOptionPane.showMessageDialog(null, "File didn't exist!");
			return;
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
}