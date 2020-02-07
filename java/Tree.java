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

	private static final ImageIcon folderIcon = new ImageIcon(
		new ImageIcon(ICONPATH + "other/folder.png").getImage().
						getScaledInstance(25, 25, Image.SCALE_DEFAULT));

	private static final ImageIcon folderIconOpen = new ImageIcon(
		new ImageIcon(ICONPATH + "other/folderopen.png").getImage().
						getScaledInstance(25, 25, Image.SCALE_DEFAULT));

	private static final ImageIcon folderIconEmpty = new ImageIcon(
		new ImageIcon(ICONPATH + "other/folderempty.png").getImage().
						getScaledInstance(25, 25, Image.SCALE_DEFAULT));
	
	private static final ImageIcon folderIconDisk = new ImageIcon(
		new ImageIcon(ICONPATH + "other/harddisk.png").getImage().
						getScaledInstance(25, 25, Image.SCALE_DEFAULT));
	
	private static final ImageIcon folderIconPC = new ImageIcon(
		new ImageIcon(ICONPATH + "other/pc.png").getImage().
						getScaledInstance(25, 25, Image.SCALE_DEFAULT));

	public Tree(DefaultMutableTreeNode top) {
		super(top);

		this.setBorder(new EmptyBorder(0, 15, 15, 0)); //top,left,bottom,right
		this.putClientProperty("JTree.lineStyle", "None");
		this.setBackground(new Color(53, 53, 53));
		final Font currentFont = this.getFont();
		final Font bigFont = new Font(currentFont.getName(), 
					currentFont.getStyle(), currentFont.getSize() + 1);
		this.setFont(bigFont);

		this.setEditable(true);
		this.setCellRenderer(new DefaultTreeCellRenderer() {
			public Component getTreeCellRendererComponent ( JTree tree, 
										Object value, boolean sel,
										boolean expanded, boolean leaf,
										int row, boolean hasFocus ) {

				JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, 
									value, sel, expanded, leaf, row, hasFocus );

				setBackground(new Color(53, 53, 53));
				setTextNonSelectionColor(Color.WHITE);
				setTextSelectionColor(new Color(0, 255, 255));
				setOpaque(true);

				DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) value;
				File file = (File) nodo.getUserObject();
				String name = file.getName();

				TreeModel tmodel = tree.getModel();
				Object root = tmodel.getRoot();
				label.setBorder(new EmptyBorder(0, 0, 0, 0)); //top,left,bottom,right

				if(nodo==root) {
					setIcon(folderIconPC);
					label.setBorder(new EmptyBorder(15, 0, 0, 0)); //top,left,bottom,right
				}
				else if(name.trim().length() == 0 && nodo.getParent()==root) {
					setIcon(folderIconDisk);
				}
				else if(file.list()!=null && file.list().length==0) {
					setIcon(folderIconEmpty);
				}
				else if(expanded) {
					setIcon(folderIconOpen);
				}
				else {
					setIcon(folderIcon);
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
