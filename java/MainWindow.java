import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class MainWindow extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.getHiddenFilesOption();
	private static String windowsTopName = Tree.getWindowsTopName();
	static private java.util.Stack<DefaultMutableTreeNode> history = 
			new java.util.Stack<DefaultMutableTreeNode>();

	static public java.util.Stack<DefaultMutableTreeNode> futureHistory = 
			new java.util.Stack<DefaultMutableTreeNode>();

	private static DefaultMutableTreeNode lastPanelNode=null;

	private static JPanel folder;
	private static JTree tree;

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

		Tree.setLastTreeNodeOpened(top);

		//Create a tree that allows one selection at a time.
		tree = new Tree(top);
		//Create the scroll pane and add the tree to it. 
		JScrollPane treeView = new JScrollPane(tree);
		treeView.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		treeView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
		
		treeView.getVerticalScrollBar().setBackground(new Color(53, 53, 53));
		treeView.getHorizontalScrollBar().setBackground(new Color(53, 53, 53));

		folder = new FolderPanel();
		JScrollPane folderView = new JScrollPane(folder);
		folderView.getVerticalScrollBar().setUnitIncrement(16);
		folderView.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		folderView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));

		folderView.getVerticalScrollBar().setBackground(new Color(53, 53, 53));
		folderView.getHorizontalScrollBar().setBackground(new Color(53, 53, 53));

		if(roots.length==1)
			Tree.createNodes(top, 0);
		else {
			int numChild=tree.getModel().getChildCount(top);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
				Tree.createNodes(current, 0);
			}
		}		
		TreePath path = new TreePath(top.getPath());
		tree.setSelectionPath(path);
		tree.expandPath(path);
		
		FolderPanel.showCurrentDirectory(top);

		folderView.setMinimumSize(new Dimension(400, 50));
		treeView.setMinimumSize(new Dimension(250, 50));
		treeView.getVerticalScrollBar().setValue(0);

		//Add the scroll panes to a split pane.
		JSplitPaneWithZeroSizeDivider splitPane = new 
					JSplitPaneWithZeroSizeDivider(
						JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(folderView);

		//Add the split pane to this panel.
		add(splitPane);
	}

	static void refresh(DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();

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
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) 
												defMod1.getChild(node, i);
				Tree.createNodes(current, 0);
			}
		}
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);

		Tree.setLastTreeNodeOpened(node);

		FolderPanel.showCurrentDirectory(node);
		folder.requestFocusInWindow();
	}

	static void renameSon(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode lastPanelNode = MainWindow.getLastPanelNode();
		JTree tree = MainWindow.getTree();

		DefaultMutableTreeNode current = null, parent = node;
		String filePath = ((File) parent.getUserObject()).getPath();
		String nameNew,	nameOld="";

		ImageIcon img=null;
		Image folderImg;
		int i;

		nameOld = FolderPanel.getCurrentPanelName();

		File f = new File(filePath + "/" + nameOld);

		if(f.exists() && f.canWrite()) {
			img = new ImageIcon(ICONPATH + "other/rename.png");
			folderImg = img.getImage().getScaledInstance(50, 
											50, Image.SCALE_DEFAULT);

			nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name",
									"Rename", JOptionPane.INFORMATION_MESSAGE,
									new ImageIcon(folderImg), null, nameOld);

			if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals(""))
				return;

			File file2 = new File(filePath + "/" + nameNew);

			if(file2.exists()) {
				JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
				return;
			}

			if(f.isDirectory()) {
				current = lastPanelNode;

				current.removeFromParent();
			}

			boolean success = f.renameTo(file2);

			if (!success) {
				JOptionPane.showMessageDialog(null, "Rename Failed!");
				return;
			}
		}
		else {
			if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				return;
			}

			JOptionPane.showMessageDialog(null, "Rename Failed!");
			return;
		}

		DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
		defMod1.reload();

		TreePath path = new TreePath(parent.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);

		FolderPanel.showCurrentDirectory(parent);
	}

	static void deleteSon(DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();

		String filePath = ((File) node.getUserObject()).getPath();
		DefaultMutableTreeNode current=null;
		String name="";
		String lastPanelName = FolderPanel.getCurrentPanelName();
		JPanel lastPanelSelected = FolderPanel.getCurrentPanelSelected();

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

			current = MainWindow.getLastPanelNode();
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

	static public void enterOrOpen(File file, DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();

		if(file.isDirectory()) {
			TreePath path = new TreePath(node.getPath());
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
			tree.expandPath(path);
			
			Tree.setLastTreeNodeOpened(node);
			lastPanelNode = null;
			FolderPanel.showCurrentDirectory(node);
		}
		else if(file.isFile()) {
			try {
				Desktop.getDesktop().open(file);
			}
			catch(IOException e) {

			}
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

	public static JPanel getFolder() {
		return folder;
	}

	public static JTree getTree() {
		return tree;
	}

	public static DefaultMutableTreeNode getLastPanelNode() {
		return lastPanelNode;
	}

	public static void setLastPanelNode(DefaultMutableTreeNode node) {
		lastPanelNode = node;
	}

	public static void historyPush(DefaultMutableTreeNode node) {
		if(node==null)
			return;

		if(history.empty() || (!history.empty() && history.peek()!=node)) {
			history.push(node);
			TopPanel.getButtonBack().setIcon(TopPanel.backArrow);
		}
	}

	public static DefaultMutableTreeNode historyPop() {
		ImageIcon img;
		Image pict;

		if(!history.empty()) {
			if(history.size()==1) {
				TopPanel.getButtonBack().setIcon(TopPanel.grayedBack);
			}

			return history.pop();
		}

		return null;
	}

	public static void clearFuture() {
		futureHistory.clear();
		TopPanel.getButtonForward().setIcon(TopPanel.grayedForward);
	}

	public static void historyBack() {
		DefaultMutableTreeNode previous,
						lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		previous = historyPop();
		if(previous==null) {
			getFolder().requestFocusInWindow();
			return;
		}

		futureHistory.push(previous);
		System.out.print(Arrays.toString(history.toArray()) + " | ");
		System.out.println(Arrays.toString(futureHistory.toArray()));

		TopPanel.getButtonForward().setIcon(TopPanel.forwardArrow);

		File file = (File) previous.getUserObject();

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			TreePath path = new TreePath(previous.getPath());

			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
			tree.expandPath(path);

			Tree.setLastTreeNodeOpened(previous);
			FolderPanel.showCurrentDirectory(previous);
			getFolder().requestFocusInWindow();
			return;
		}
		enterOrOpen(file, previous);
		getFolder().requestFocusInWindow();
	}

	public static void historyForward() {
		DefaultMutableTreeNode next,
						lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		if(futureHistory.empty()) {
			getFolder().requestFocusInWindow();		
			return;
		}

		next = futureHistory.pop();
		if(futureHistory.empty()) {
			TopPanel.getButtonForward().setIcon(TopPanel.grayedForward);		
		}
		historyPush(next);
		File file = (File) next.getUserObject();
		
		System.out.print(Arrays.toString(history.toArray()) + " | ");
		System.out.println(Arrays.toString(futureHistory.toArray()));

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			TreePath path = new TreePath(next.getPath());

			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
			tree.expandPath(path);

			Tree.setLastTreeNodeOpened(next);
			FolderPanel.showCurrentDirectory(next);
			getFolder().requestFocusInWindow();
			return;
		}
		enterOrOpen(file, next);
		getFolder().requestFocusInWindow();		
	}
}
