import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class Tree extends JTree implements TreeSelectionListener {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final String windowsTopName="This PC";
	private static DefaultMutableTreeNode lastTreeNodeOpened=null;
	private static final boolean showHiddenFiles = 
									FileExplorer.getHiddenFilesOption();

	public Tree(DefaultMutableTreeNode top) {
		super(top);

		this.setBorder(new EmptyBorder(0, 15, 15, 0)); //top,left,bottom,right
		this.putClientProperty("JTree.lineStyle", "None");
		this.setBackground(new Color(32, 32, 32));
		final Font currentFont = this.getFont();
		final Font bigFont = new Font(currentFont.getName(), 
					currentFont.getStyle(), currentFont.getSize() + 1);
		this.setFont(bigFont);
		this.setEditable(false);
		this.setCellEditor(new DefaultTreeCellEditor(this, 
				(DefaultTreeCellRenderer) this.getCellRenderer()) {
			@Override
			public boolean isCellEditable(EventObject event) {
				if(event instanceof MouseEvent){
					return false;
				}
				return super.isCellEditable(event);
			}
		});
		this.setCellRenderer(new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent ( JTree tree, 
										Object value, boolean sel,
										boolean expanded, boolean leaf,
										int row, boolean hasFocus ) {

				JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, 
									value, sel, expanded, leaf, row, hasFocus );

				setBackground(new Color(32, 32, 32));
				setTextNonSelectionColor(Color.WHITE);
				setTextSelectionColor(new Color(0, 255, 255));
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
					setText(file.getPath().replace("\\", ""));
					
					String description = fsv.getSystemTypeDescription(file);
					name = file.getPath().replace("\\", "");

					if(description.equals("CD Drive")) {
						path = ICONPATH + "other/cd.png";
					}
					else if(description.equals("DVD Drive")) {
						path = ICONPATH + "other/dvd.png";
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

		this.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//Listen for when the selection changes.
		this.addTreeSelectionListener(this);
		this.addTreeWillExpandListener(new TreeWillExpandListener() {
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

		this.addMouseListener(new MouseListener() {
			DefaultMutableTreeNode last;

			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
									getLastSelectedPathComponent();

				MainWindow.setFocusTree();

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
					JTree tree = MainWindow.getTree();
					int row = tree.getClosestRowForLocation(e.getX(), e.getY());
					tree.setSelectionRow(row);

					node = (DefaultMutableTreeNode) 
						getLastSelectedPathComponent();

					JPopupMenu menu = getFolderPopupMenu(node);
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
				else {
					if(lastTreeNodeOpened!=node) {
						MainWindow.historyPush(lastTreeNodeOpened);
						MainWindow.clearFuture();
						lastTreeNodeOpened = node;
						FolderPanel.showCurrentDirectory(node);
					}
				}
			}
			@Override
			public void mouseReleased(MouseEvent e) {}
		});

		this.addKeyListener(new KeyListener() {
			boolean alt_pressed = false;

			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				switch(e.getKeyCode()) {
					case KeyEvent.VK_ALT:
						alt_pressed = true;
						break;

					case KeyEvent.VK_ENTER:

						DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
											getLastSelectedPathComponent();
					
						if(lastTreeNodeOpened!=node) {
							MainWindow.historyPush(lastTreeNodeOpened);
							MainWindow.clearFuture();
						}
						lastTreeNodeOpened = node;
						File current;

						current = (File) node.getUserObject();
						if (current.isDirectory()) {
							createNodes(node);
						}
						FolderPanel.showCurrentDirectory(node);
						break;
	
					case KeyEvent.VK_BACK_SPACE:
						TopPanel.historyBack();

						break;

					case KeyEvent.VK_LEFT:
						if(alt_pressed)
							TopPanel.historyBack();

						break;

					case KeyEvent.VK_RIGHT:
						if(alt_pressed)
							TopPanel.historyForward();
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

		popupMenu.addSeparator();

		menuItem = new JMenuItem("  Rename");
		menuItem.setIcon(Utility.getImageFast(
			ICONPATH + "other/rename.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				MainWindow.rename(node);
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
				MainWindow.delete(node);
			}
		});

		menuItem.setBackground(Color.white);
		if(fileNode.exists() && fileNode.canWrite())
			popupMenu.add(menuItem);

		menuItem = new JMenuItem("  Properties");
		menuItem.setIcon(
			Utility.getImageFast(ICONPATH + "other/properties.png", 17, 17, true));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				MainWindow.properties((File) node.getUserObject());
				MainWindow.focusLast();
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
		JTree tree = MainWindow.getTree();
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
				currentNode = new DefaultMutableTreeNode(new MyFile(
					element.getPath()));
				
				model.insertNodeInto(currentNode, top, top.getChildCount());
				sortNode(top);
			}
			else {
				int numChild= MainWindow.getTree().getModel().getChildCount(top);
				for(int i=0; i<numChild; i++) { 
					File currentFile;

					currentNode = (DefaultMutableTreeNode) MainWindow.getTree().
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
					firstChild = new DefaultMutableTreeNode(new MyFile(
						current.getPath()));

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
		int i, numChild= MainWindow.getTree().getModel().getChildCount(top);
		for(i=0; i<numChild; i++) { 
			File currentFile;

			currentNode = (DefaultMutableTreeNode) MainWindow.getTree().
					getModel().getChild(top, i);

			currentFile=(File) currentNode.getUserObject();
			if(currentFile.getName().compareTo(element.getName())==0) {
				return true;
			}
		}

		return false;
	}

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

 	/* Targets selected node when clicked in tree */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
									getLastSelectedPathComponent();
		File current;
	
		if (node == null) 
			return;

		current = (File) node.getUserObject();
		if (current.isDirectory()) {
			createNodes(node);
		}
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

			MainWindow.refresh(node);
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
}
