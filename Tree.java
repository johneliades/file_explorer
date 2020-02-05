import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;

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

		this.setBorder(new EmptyBorder(5, 10, 5, 0)); //top,left,bottom,right
		this.putClientProperty("JTree.lineStyle", "None");

		this.setEditable(true);
		this.setCellRenderer(getRenderer());
		this.getSelectionModel().setSelectionMode
				(TreeSelectionModel.SINGLE_TREE_SELECTION);

		//Listen for when the selection changes.
		this.addTreeSelectionListener(this);
		this.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {}
	  
			@Override
			public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
				TreePath path = treeExpansionEvent.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
										path.getLastPathComponent();

				File current;

				String data = node.getUserObject().toString();

				current = (File) node.getUserObject();
				if (current.isDirectory()) {
					createNodes(node, 0);
				}
			}
		});

		this.addMouseListener(new MouseListener() {
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
				if(node==null)
					return;
				
				lastTreeNodeOpened = node;

				String filePath = ((File) 
						lastTreeNodeOpened.getUserObject()).getPath();

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

		this.addKeyListener(new KeyListener() {
			boolean pressed = false;

			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER && !pressed) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
										getLastSelectedPathComponent();

					pressed = true;

					lastTreeNodeOpened = node;
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
	}

	private DefaultTreeCellRenderer getRenderer() {
		DefaultTreeCellRenderer tRenderer = new DefaultTreeCellRenderer();

		ImageIcon folderIcon = new ImageIcon(ICONPATH + "extensions/folder.png");
		Image folderImg = folderIcon.getImage().getScaledInstance(25, 25, Image.SCALE_DEFAULT);
		folderIcon = new ImageIcon(folderImg);
		
		ImageIcon folderIconOpen = new ImageIcon(ICONPATH + "other/folderopen.png");
		Image folderImgOpen = folderIconOpen.getImage().getScaledInstance(25, 25, Image.SCALE_DEFAULT);
		folderIconOpen = new ImageIcon(folderImgOpen);

		ImageIcon folderIconEmpty = new ImageIcon(ICONPATH + "other/folderempty.png");
		Image folderImgEmpty = folderIconEmpty.getImage().getScaledInstance(25, 25, Image.SCALE_DEFAULT);
		folderIconEmpty = new ImageIcon(folderImgEmpty);

		tRenderer.setLeafIcon(folderIconEmpty);
		tRenderer.setClosedIcon(folderIcon);
		tRenderer.setOpenIcon(folderIconOpen);
		tRenderer.setTextSelectionColor(Color.RED);

		return tRenderer;
	}

	static public void createNodes(DefaultMutableTreeNode top, int setting) {
		DefaultMutableTreeNode file = null;
		int i, numChild;
		SortedSet<File> set = new TreeSet<>();
		Iterator it;

		File curDir = (File) top.getUserObject();
		File current, children[] = curDir.listFiles(); 

		numChild= MainWindow.getTree().getModel().getChildCount(top);

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
				current=(File) ((DefaultMutableTreeNode) MainWindow.getTree().getModel().getChild(top, i)).getUserObject();
				if(current.getName().compareTo(element.getName())==0) {
					file=(DefaultMutableTreeNode) MainWindow.getTree().getModel().getChild(top, i);
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
			createNodes(node, 0);
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
