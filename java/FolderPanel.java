import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class FolderPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.getHiddenFilesOption();
	static Set<String> iconSet = FileExplorer.addExtensions();
	private String windowsTopName = Tree.getWindowsTopName();

	private static JPanel lastPanelSelected; 
	private static String lastPanelName="";

	public FolderPanel() {
		//Create the folder viewing pane.
		super(new WrapLayout(FlowLayout.LEFT, 10, 10));
		this.setBackground(new Color(53, 53, 53));

		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {}
			@Override
			public void mouseExited(MouseEvent event) {}
			@Override
			public void mousePressed(MouseEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
				DefaultMutableTreeNode lastPanelNode = MainWindow.getLastPanelNode();
				JTree tree = MainWindow.getTree();
				
				requestFocusInWindow();

				String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();

				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					Tree.findExistingParent(f);
					return;
				}

				if(event.getButton() == MouseEvent.BUTTON1) {
					if(lastPanelSelected!=null) {
						lastPanelSelected.setBackground(new Color(53, 53, 53));
						lastPanelSelected.setBorder(BorderFactory.createLineBorder(new Color(53, 53, 53)));
						lastPanelSelected=null;
						MainWindow.setLastPanelNode(null);
						lastPanelName=null;
					}
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = getBackgroundPopupMenu();

					if(tree.getLastSelectedPathComponent()!=null)
						menu.show(event.getComponent(), event.getX(), event.getY());

					if(lastPanelSelected!=null) {
						lastPanelSelected.setBackground(new Color(53, 53, 53));
						lastPanelSelected.setBorder(BorderFactory.createLineBorder(new Color(53, 53, 53)));
						lastPanelSelected=null;
						MainWindow.setLastPanelNode(null);
						lastPanelName=null;
					}
				}
			}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});
	}

	public JPopupMenu getBackgroundPopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		JMenu sectionsMenu = new JMenu(" New ");
		ImageIcon img=null;
		Image folderImg;

		//New submenu(txt, folder)

		menuItem = new JMenuItem(" Text Document ");
		img = new ImageIcon(ICONPATH + "extensions/txt.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
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
				File f;
 
 				f = new File(filePath + "/");
 				if(!f.canWrite()) {
					JOptionPane.showMessageDialog(null, "Not enough permissions!");
					return;
 				}

				img = new ImageIcon(ICONPATH + "extensions/txt.png");
				folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
				name=(String) JOptionPane.showInputDialog(null, "Enter File Name", "New Text Document",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, "File");
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


		menuItem = new JMenuItem(" Folder ");
		img = new ImageIcon(ICONPATH + "other/folder.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
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
					JOptionPane.showMessageDialog(null, "Not enough permissions!");
					return;
 				}

				img = new ImageIcon(ICONPATH + "other/folder.png");
				folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);
				name=(String) JOptionPane.showInputDialog(null, "Enter Folder Name", "New Folder",
										JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg), 
										null, "Folder");
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
	
				TreePath path = new TreePath(node.getPath());
				tree.setSelectionPath(path);
				tree.scrollPathToVisible(path);
				tree.expandPath(path);
				
				showCurrentDirectory(node);
			}
		});

		menuItem.setBackground(Color.white);
		sectionsMenu.add(menuItem);
	

		img = new ImageIcon(ICONPATH + "other/plus.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		sectionsMenu.setIcon(new ImageIcon(folderImg));	

		popupMenu.addSeparator();
		popupMenu.add(sectionsMenu);
		popupMenu.addSeparator();

		popupMenu.setBorder(new CompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.red), 
				BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black)));
		popupMenu.setBackground(Color.white);	

		//Refresh option

		menuItem = new JMenuItem(" Refresh ");
		img = new ImageIcon(ICONPATH + "other/refresh.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				MainWindow.refresh(lastTreeNodeOpened);
			}
		});

		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);


		menuItem = new JMenuItem(" OS Explorer ");
		img = new ImageIcon(ICONPATH + "other/osexplorer.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				String name="";
				File curFile;

				curFile = ((File) lastTreeNodeOpened.getUserObject());

				try {		
					if(curFile.getName().equals(windowsTopName)) {
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

		return popupMenu;
	}

	static public JPopupMenu getFilePopupMenu() {
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem menuItem;
		ImageIcon img=null;
		Image folderImg;

		menuItem = new JMenuItem(" Rename ");
		img = new ImageIcon(ICONPATH + "other/rename.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				MainWindow.renameSon(lastTreeNodeOpened);
			}
		});
			
		menuItem.setBackground(Color.white);
		popupMenu.add(menuItem);


		menuItem = new JMenuItem(" Delete ");
		img = new ImageIcon(ICONPATH + "other/delete.png");
		folderImg = img.getImage().getScaledInstance(17, 17, Image.SCALE_DEFAULT);
		menuItem.setIcon(new ImageIcon(folderImg));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

				MainWindow.deleteSon(lastTreeNodeOpened);
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
					folder.add(getPanel("folder.png", currentFile, currentNode));
				else if(iconSet.contains(Utility.getExtension(currentFile.getName())))
					folder.add(getPanel(Utility.getExtension(currentFile.getName()) + ".png", currentFile, currentNode));
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
				if(showHiddenFiles ?  true : !element.isHidden() || !element.getName().startsWith(".")) {
					if (element.isDirectory())
						folder.add(getPanel("folder.png", element, null));
					else if(iconSet.contains(Utility.getExtension(element.getName())))
						folder.add(getPanel(Utility.getExtension(element.getName()) + ".png", element, null));
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
		Image folderImg;
		Set<String> set = new HashSet<>(); 
		String name = file.getName();
		String extension = Utility.getExtension(file.getName());

		if(name.trim().length() == 0) {
			name = "Local Disk (" + file.getPath().replace("\\", "") + ")";
			img = new ImageIcon(ICONPATH + "other/harddisk.png");
		}

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(extension)) {
			img = new ImageIcon(file.getPath());
		}

		if(img==null) {
			if(iconName=="folder.png") {
				if(file.list()!=null && file.list().length==0)
					img = new ImageIcon(ICONPATH + "other/" + "folderempty.png");
				else {
					img = new ImageIcon(ICONPATH + "other/" + "folder.png");
				}
			}
			else if(iconName=="question.png") {
				img = new ImageIcon(ICONPATH + "other/" + "question.png");
			}
			else
				img = new ImageIcon(ICONPATH + "extensions/" + iconName);
		}

		//Image folderImg = img.getImage().getScaledInstance(150, 60, Image.SCALE_DEFAULT);
		folderImg = img.getImage().getScaledInstance(60, 60, Image.SCALE_DEFAULT);

		/* You get small resolution system icons. Waiting for official better way
		Icon icon;

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
		label.setForeground (Color.white);
		panel.add(label, BorderLayout.SOUTH);

		label.setName(name);
		panel.setName(name);
		panel.setBorder(BorderFactory.createLineBorder(new Color(53, 53, 53)));
		panel.setBackground(new Color(53, 53, 53));

		panel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {		
				if(lastPanelSelected!=panel)
					panel.setBackground(new Color(0, 170, 170));
			}
			@Override
			public void mouseExited(MouseEvent event) {
				if(lastPanelSelected!=panel)
					panel.setBackground(new Color(53, 53, 53));
			}
			@Override
			public void mousePressed(MouseEvent event) {
				DefaultMutableTreeNode lastTreeNodeOpened = 
										Tree.getLastTreeNodeOpened();
				JTree tree = MainWindow.getTree();
				DefaultMutableTreeNode lastPanelNode =
										MainWindow.getLastPanelNode();

				panel.requestFocusInWindow();

				DefaultMutableTreeNode current = null, parent = lastTreeNodeOpened;
				String name="";
				File curFile=null;
		
				if(!file.exists()) {
					Tree.findExistingParent(file);
					return;
				}

				if(lastPanelSelected!=null) {
					lastPanelSelected.setBackground(new Color(53, 53, 53));
					lastPanelSelected.setBorder(BorderFactory.createLineBorder(new Color(53, 53, 53)));
				}
				panel.setBackground(new Color(0, 100, 100));
				panel.setBorder(BorderFactory.createLineBorder(Color.white));
				lastPanelSelected=panel;

				// Get node and name of last selected panel
				Component curComponents[] = lastPanelSelected.getComponents();
				for(Component comp : curComponents)
					if(comp.getName()!=null && comp.getName()!="")
						name = comp.getName();

				lastPanelName = name;

				int i, numChild=tree.getModel().getChildCount(parent);
				for(i=0; i<numChild; i++) { 
					current=(DefaultMutableTreeNode) tree.getModel().getChild(parent, i);
					curFile=(File) (current).getUserObject();
					if(curFile.getName().compareTo(name)==0)
						break;
				}

				if(current==null || i==numChild || curFile.exists()==false) {
				//	JOptionPane.showMessageDialog(null, "Chose file");
				//	return;
				}
				MainWindow.setLastPanelNode(current);
				// /Get node and name of last selected panel

				if(event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
					if(file.isDirectory()) {
						TreePath path = new TreePath(node.getPath());
						tree.setSelectionPath(path);
						tree.scrollPathToVisible(path);
						tree.expandPath(path);
					
						Tree.setLastTreeNodeOpened(node);
						MainWindow.setLastPanelNode(null);
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

	public static JPanel getLastPanelSelected() {
		return lastPanelSelected;
	}

	public static String getLastPanelName() {
		return lastPanelName;
	}
}