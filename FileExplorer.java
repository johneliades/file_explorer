import java.io.File;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class FileExplorer extends JPanel
						implements TreeSelectionListener {
	private static JPanel folder;
	private static JTree tree;
	private static final String ICONPATH="./icons/"; // path-until-src/src/hw4/icons/
	private static JTextField searchField, navigationField;
	
	//Optionally set the look and feel.
	private static final boolean useSystemLookAndFeel = false;
	private static final boolean showHiddenFiles = false;
	static Set<String> iconSet=addExtensions();
	static JPanel lastPanelSelected; 
	static String searchQuery = "";

	public FileExplorer() {
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

		tree.setEditable(true);
		tree.setCellRenderer(getRenderer());
		tree.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//Listen for when the selection changes.
		tree.addTreeSelectionListener(this);
		tree.addTreeWillExpandListener(treeWillExpandListener);

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
		tree.scrollPathToVisible(path);
		tree.expandPath(path);
		showCurrentDirectory(top);
		
		folderView.setMinimumSize(new Dimension(400, 50));
		treeView.setMinimumSize(new Dimension(250, 50));

		//Add the scroll panes to a split pane.
		JSplitPaneWithZeroSizeDivider splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(folderView);

		//Add the split pane to this panel.
		add(splitPane);
	}

	static void showCurrentDirectory(DefaultMutableTreeNode node) {
		int numChild=tree.getModel().getChildCount(node);
		SortedSet<File> set2;
		Iterator it;
		DefaultMutableTreeNode currentNode;
		File currentFile;
		
		String FileName;
		
		FileName = ((File) node.getUserObject()).getName();
		
		if(FileName.isEmpty())
			FileName = ((File) node.getUserObject()).getPath();
		
		navigationField.setText(" " + ((File) node.getUserObject()).getPath());
		searchField.setText(" Search" + " \"" + FileName + "\"");

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
				else if(iconSet.contains(getExtension(currentFile.getName())))
					folder.add(getIcon(getExtension(currentFile.getName()) + ".png", currentFile, currentNode));
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
					else if(iconSet.contains(getExtension(element.getName())))
						folder.add(getIcon(getExtension(element.getName()) + ".png", element, null));
					else
						folder.add(getIcon("question.png", element, null));
				}
			}
		}

		folder.repaint();
		folder.revalidate();
	}

	static String getExtension(String fileName) {
		String extension = "";

		int i = fileName.lastIndexOf('.');
		int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

		if (i > p) {
			extension = fileName.substring(i+1);
		}

		return extension;
	}

	static Set<String> addExtensions() {
		Set<String> set = new HashSet<>(); 
				
		set.add("audio"); 
		set.add("bmp"); 
		set.add("css"); 
		set.add("doc"); 
		set.add("docx"); 
		set.add("giff"); 
		set.add("gz"); 
		set.add("htm"); 
		set.add("html"); 
		set.add("iso"); 
		set.add("jpeg"); 
		set.add("jpg"); 
		set.add("json"); 
		set.add("mp3"); 
		set.add("ods"); 
		set.add("odt"); 
		set.add("ogg"); 
		set.add("pdf"); 
		set.add("png"); 
		set.add("ppt"); 
		set.add("tar"); 
		set.add("tgz"); 
		set.add("txt"); 
		set.add("video"); 
		set.add("wav"); 
		set.add("xlsx"); 
		set.add("xlx"); 
		set.add("xml"); 
		set.add("zip"); 
		set.add("class"); 
		set.add("java"); 
		set.add("log");

		return set;
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
			String extension = getExtension(file.getName());
			
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

		TreeWillExpandListener treeWillExpandListener = new TreeWillExpandListener() {
			@Override
			public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
				TreePath path = treeExpansionEvent.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			}
	  
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
		};

	 /** Required by TreeSelectionListener interface. */
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
			else {

			}
		}

		static private void createNodes(DefaultMutableTreeNode top, int setting) {
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

		public static JMenuBar CreateMenuBar() {
			JMenuBar bar=new JMenuBar();
			JButton button;
			ImageIcon img;
			Image pict;

			/*
		   	img = new ImageIcon(ICONPATH + "other/foldernew.png");
			pict = img.getImage().getScaledInstance(35, 35, Image.SCALE_DEFAULT);
			img = new ImageIcon(pict);

			button = new JButton(img);
			button.setBorder(BorderFactory.createEmptyBorder());
			button.setContentAreaFilled(false);

			bar.add(button); */

			return bar;
		}
		
		public static JPanel createTopPanel() {
			JPanel topPanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			JButton button;

			c.weightx = 0.05;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;

			button = new JButton("");
			button.setOpaque(false);
			button.setContentAreaFilled(false);
			button.setBorderPainted(false);
			button.setPreferredSize(new Dimension(button.getPreferredSize().width, 25));
			topPanel.add(button, c);

			c.weightx = 0.8;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.gridy = 0;

			navigationField = new JTextField("");
			navigationField.setPreferredSize(new Dimension(navigationField.getPreferredSize().width, 25));

			topPanel.add(navigationField, c);

			searchField = new JTextField("");
			searchField.setPreferredSize(new Dimension(searchField.getPreferredSize().width, 25));
			searchField.addKeyListener(new KeyListener() {
				@Override
				public void keyTyped(KeyEvent e) {
					searchQuery += e.getKeyChar();					
				}
				@Override
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() == KeyEvent.VK_ENTER) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
						File top = (File) node.getUserObject();

						folder.removeAll();
						folder.repaint();
						folder.revalidate();

						searchField.setText("");
						JPanel gridPanel = new JPanel(new GridLayout(0, 1, 8, 8));
						gridPanel.setBackground(Color.white);
						folder.add(gridPanel);
						search(node, searchQuery, gridPanel);
						searchQuery="";
						folder.repaint();
						folder.revalidate();
					}
				}
				@Override
				public void keyReleased(KeyEvent e) {}
			});
	   
	   		searchField.addMouseListener(new MouseListener() {
				@Override
				public void mouseClicked(MouseEvent event) {

					if(event.getButton() == MouseEvent.BUTTON1) {
						searchField.setText("");
						searchQuery="";
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

			searchField.addFocusListener(new FocusListener() {
				String lastText="";

				@Override
				public void focusGained(FocusEvent e) {
					lastText = searchField.getText();
					searchField.setText("");
					searchQuery = "";
				}
				public void focusLost(FocusEvent e) {
					searchField.setText(lastText);
				}
			});

			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.15;
			c.gridx = 2;
			c.gridy = 0;

			topPanel.add(searchField, c);

			return topPanel;
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
						TreePath path = new TreePath(parent.getPath());
						tree.setSelectionPath(path);
						tree.scrollPathToVisible(path);
						tree.expandPath(path);
					}
					else {
						JOptionPane.showMessageDialog(null, "Rename Failed!");
						return;
					}
		
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

					showCurrentDirectory(node);
				}
			});

			popupMenu.add(menuItem);

			return popupMenu;
		}

		static void search(DefaultMutableTreeNode top, String searchQuery, JPanel gridPanel) {
			int numChild=tree.getModel().getChildCount(top);
			DefaultMutableTreeNode current;
			File topFile = (File) top.getUserObject();

			if(numChild==0)
				return; 

			boolean isSymbolicLink = Files.isSymbolicLink(topFile.toPath());
			if(isSymbolicLink)
				return;

			createNodes(top, 0);

			for(int i=0; i<numChild; i++) {	  
				current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
				File element = (File) current.getUserObject();

				if(element.getName().contains(searchQuery)) {
					gridPanel.add(getSmallIcon("folder.png", element, current));
				}
				
				File children[] = element.listFiles();
				if(children==null)
					continue;
				for(File child : children) {
					if(child.isFile() && child.getName().contains(searchQuery)) {
						if(iconSet.contains(getExtension(child.getName())))
							gridPanel.add(getSmallIcon(getExtension(child.getName()) + ".png", child, current));
						else
							gridPanel.add(getSmallIcon("question.png", child, current));
					} 
				}

				search(current, searchQuery, gridPanel);
			}		  
		}

		public static JLabel getSmallIcon(String name, File file, DefaultMutableTreeNode node) {
			JLabel label = new JLabel();
			ImageIcon img=null;
			Image pict;
			Set<String> set = new HashSet<>(); 

			// Bad check for images
			set.add("jpeg");
			set.add("jpg");
			set.add("png");
			set.add("gif");
			if(set.contains(getExtension(file.getName()))) {
				img = new ImageIcon(file.getPath());
			}

			if(img==null)
				img = new ImageIcon(ICONPATH + "extensions/" + name);

			pict = img.getImage().getScaledInstance(40, 40, Image.SCALE_DEFAULT);
			img = new ImageIcon(pict);
	 		
			label.setIcon(img);
			label.setText(file.getPath());
			label.addMouseListener(new MouseListener(){
				@Override
				public void mouseClicked(MouseEvent arg0) {
					String fullPath = label.getText();
					File file = new File(fullPath);

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
				@Override
				public void mouseReleased(MouseEvent arg0) {}
				@Override
				public void mousePressed(MouseEvent arg0) {}
				@Override
				public void mouseExited(MouseEvent arg0) {}
				@Override
				public void mouseEntered(MouseEvent arg0) {}
			});
			
			return label;
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
				TreePath path = new TreePath(node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);
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
					if(getExtension(name).equals("txt"))
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

					img = new ImageIcon(ICONPATH + "folder.png");
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
			frame.setJMenuBar(CreateMenuBar());

			//Add content to the window.
			frame.add(createTopPanel(), BorderLayout.NORTH);
			frame.add(new FileExplorer(), BorderLayout.CENTER);
		
			//Display the window.
			frame.setVisible(true);
		}

		public static void main(String[] args) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					createAndShowGUI();
				}
			});
		}
}

class MyFile extends File {
	
	MyFile(String filename) {
		super(filename);
	}

	@Override
	public String toString() {
		if(this.getPath().compareTo("/")!=0)
			return this.getName();
		else
			return this.getPath();
	}
}