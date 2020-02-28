import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

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
		this.setEditable(true);
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
			public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {}
	  
			@Override
			public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
				TreePath path = treeExpansionEvent.getPath();
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
										path.getLastPathComponent();

				File current;
				current = (File) node.getUserObject();
				if (current.isDirectory()) {
					createNodes(node, 0);
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
				if(node==null)
					return;

				if(lastTreeNodeOpened!=node) {
					MainWindow.historyPush(lastTreeNodeOpened);
					MainWindow.clearFuture();
					String filePath = ((File) 
						lastTreeNodeOpened.getUserObject()).getPath();

					File f = new File(filePath + "/");
					if(!f.getName().equals(windowsTopName) && !f.exists()) {
						findExistingParent(f);
						return;
					}

					File current = (File) node.getUserObject();

					if (current.exists()) {
						createNodes(node, 0);
					}
					lastTreeNodeOpened = node;
					FolderPanel.showCurrentDirectory(node);
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
							createNodes(node, 0);
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

	static public void createNodes(DefaultMutableTreeNode top, int setting) {
		SortedSet<File> set = new TreeSet<>();
		Iterator it;

		File curDir = (File) top.getUserObject();
		File children[] = curDir.listFiles(); 

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
			DefaultMutableTreeNode currentNode = null;

			int i, numChild= MainWindow.getTree().getModel().getChildCount(top);
			for(i=0; i<numChild; i++) { 
				File currentFile;

				currentNode = (DefaultMutableTreeNode) MainWindow.getTree().
						getModel().getChild(top, i);

				currentFile=(File) currentNode.getUserObject();
				if(currentFile.getName().compareTo(element.getName())==0) {
					break;
				}
			}

			if(i==numChild) {
				currentNode = new DefaultMutableTreeNode(new MyFile(
					element.getPath()));
				top.add(currentNode);
			}

			if(setting==0)
				createNodes(currentNode, 1);
		}
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
