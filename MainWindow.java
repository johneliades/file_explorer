import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class MainWindow extends JPanel implements TreeSelectionListener {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.getHiddenFilesOption();
	static final String windowsTopName="This PC";

	private static JPanel folder;
	private static JTree tree;

	private static DefaultMutableTreeNode lastTreeNodeOpened, lastPanelNode=null;

	public MainWindow() {
		super(new GridLayout(1, 0));

		//Create the nodes.
		File roots[]=File.listRoots();
		
		DefaultMutableTreeNode top;
		if(roots.length==1)
			top = new DefaultMutableTreeNode(roots[0]);
		else {
			top = new DefaultMutableTreeNode(new MyFile(windowsTopName));
			for (File root : roots) {
				top.add(new DefaultMutableTreeNode(root));
			}
		}
		lastTreeNodeOpened=top;

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
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				if(node==null)
					return;
				
				lastTreeNodeOpened=node;

				String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();

				File f = new File(filePath + "/");
				if(!f.getName().equals(windowsTopName) && !f.exists()) {
					findExistingParent(f);
					return;
				}

				FolderPanel.showCurrentDirectory(node);
			}
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

					lastTreeNodeOpened=node;
					FolderPanel.showCurrentDirectory(node);
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

		folder = new FolderPanel();

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
		
		FolderPanel.showCurrentDirectory(top);

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

	public static DefaultMutableTreeNode getLastTreeNodeOpened() {
		return lastTreeNodeOpened;
	}

	public static void setLastTreeNodeOpened(DefaultMutableTreeNode node) {
		lastTreeNodeOpened = node;
	}

	public static DefaultMutableTreeNode getLastPanelNode() {
		return lastPanelNode;
	}

	public static void setLastPanelNode(DefaultMutableTreeNode node) {
		lastPanelNode = node;
	}

	public static String getWindowsTopName() {
		return windowsTopName;
	}

	static void refresh(DefaultMutableTreeNode node) {
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

		lastTreeNodeOpened = node;

		FolderPanel.showCurrentDirectory(node);
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
		String filePath = ((File) node.getUserObject()).getPath();
		DefaultMutableTreeNode current=null;
		String name="";
		String lastPanelName = FolderPanel.getLastPanelName();
		JPanel lastPanelSelected = FolderPanel.getLastPanelSelected();

		ImageIcon img=null;
		Image folderImg;
		int i;

		name = lastPanelName;
		if(name==null) {
			return;
		}
		File f = new File(filePath + "/" + name);

		img = new ImageIcon(ICONPATH + "other/delete.png");
		folderImg = img.getImage().getScaledInstance(50, 50, Image.SCALE_DEFAULT);

		if(f.exists() && f.isFile() && f.canWrite()){
			int input = JOptionPane.showConfirmDialog(null, "Deleting file \"" + name + "\" ?",
						"Any deletion is permanent", JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg));
		
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
			int input = JOptionPane.showConfirmDialog(null, "Deleting folder \"" + name + "\" ?",
						"Any deletion is permanent", JOptionPane.OK_CANCEL_OPTION, 
						JOptionPane.INFORMATION_MESSAGE, new ImageIcon(folderImg));
		
			if(input==JOptionPane.CANCEL_OPTION || input==-1) {
				return;
			}

			current = lastPanelNode;
			current.removeAllChildren();
			current.removeFromParent();
			removeDirectory(f);
			f.delete(); 

			DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
			defMod1.reload();
		}
		else {
			if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				if(lastPanelSelected!=null) {
					lastPanelSelected.setBackground(new Color(0x3fa9ff));
					lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.black));
				}
				return;
			}

			JOptionPane.showMessageDialog(null, "File didn't exist!");
			return;
		}

		TreePath path = new TreePath(node.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);

		FolderPanel.showCurrentDirectory(node);
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

	static void findExistingParent(File f) {
		DefaultMutableTreeNode node = lastTreeNodeOpened;

		if(!f.exists()) {
			JOptionPane.showMessageDialog(null, "This directory no longer exists");
			// Restore to previous working directory

			while(!f.exists()) {
				node = (DefaultMutableTreeNode) node.getParent();
				if(node==null)
					return;
				f=(File) node.getUserObject();
			}

			refresh(node);
			return;
		}
	}
}
