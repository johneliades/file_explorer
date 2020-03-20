import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.IconUIResource;

import java.text.SimpleDateFormat;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import java.util.*;

public class MainWindow extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final boolean showHiddenFiles = FileExplorer.getHiddenFilesOption();
	private static String windowsTopName = Tree.getWindowsTopName();
	static private java.util.Stack<DefaultMutableTreeNode> history = 
			new java.util.Stack<DefaultMutableTreeNode>();

	static public java.util.Stack<DefaultMutableTreeNode> futureHistory = 
			new java.util.Stack<DefaultMutableTreeNode>();

	private static DefaultMutableTreeNode top;
	private static JPanel folder;
	private static JTree tree;

	public MainWindow(File fileToOpen) {
		super(new GridLayout(1, 0));

		//Create the nodes.
		File roots[]=File.listRoots();
		
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
		treeView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		treeView.setBorder(new EmptyBorder(0, 0, 0, 0));

		BasicTreeUI basicTreeUI = (BasicTreeUI) tree.getUI();
		basicTreeUI.setLeftChildIndent(0);
		basicTreeUI.setRightChildIndent(12);
		basicTreeUI.setCollapsedIcon(Utility.getImageFast(ICONPATH + 
								"other/collapsed.png", 9, 9, true));
		basicTreeUI.setExpandedIcon(Utility.getImageFast(ICONPATH + 
			"other/expanded.png", 9, 9, true));

		folder = new FolderPanel();
		JScrollPane folderView = new JScrollPane(folder);
		folderView.getVerticalScrollBar().setUnitIncrement(16);
		folderView.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		folderView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
		folderView.getVerticalScrollBar().setBackground(new Color(53, 53, 53));
		folderView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		folderView.setBorder(new EmptyBorder(0, 0, 0, 0));

		if(roots.length==1)
			Tree.createNodes(top);
		else {
			int numChild=tree.getModel().getChildCount(top);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) 
						tree.getModel().getChild(top, i);
				Tree.createNodes(current);
			}
		}

		if(fileToOpen==null) {
			selectDirectory(top);
		}
		else {
			java.util.Stack<String> pathComponents = 
				new java.util.Stack<String>();

			while(true) {
				if(fileToOpen.getParentFile() == null)
					break;
				pathComponents.add(fileToOpen.getName());
				fileToOpen = fileToOpen.getParentFile();
			} 
			
			pathComponents.add(fileToOpen.getPath().replace("\\", ""));
			//PathComponents now contains the path 
			//components starting from root

			loadPath(top, pathComponents);
		}

		folderView.setMinimumSize(new Dimension(400, 50));
		treeView.setMinimumSize(new Dimension(250, 50));
		treeView.getVerticalScrollBar().setValue(0);

		//Add the scroll panes to a split pane.
		JSplitPaneWithZeroSizeDivider splitPane = new 
					JSplitPaneWithZeroSizeDivider(
						JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(treeView);
		splitPane.setRightComponent(folderView);
		splitPane.setBorder(
			BorderFactory.createMatteBorder(0, 3, 3, 3, Color.BLACK));

		// Mouse back and forward
		if (Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() && 
				MouseInfo.getNumberOfButtons() > 3) {
			
			Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
				if (event instanceof MouseEvent) {
					MouseEvent mouseEvent = (MouseEvent) event;
					if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED &&
						mouseEvent.getButton() > 3) {
						
						if (mouseEvent.getButton() == 4) {
							TopPanel.historyBack();
							MainWindow.focusLast();
						} else if (mouseEvent.getButton() == 5) {
							TopPanel.historyForward();
							MainWindow.focusLast();
						}
					}
				}
			}, AWTEvent.MOUSE_EVENT_MASK);
		}

		//Add the split pane to this panel.
		add(splitPane);
	}

	static void loadPath(DefaultMutableTreeNode top, 
								java.util.Stack<String> pathComponents) {	
		
		String current = pathComponents.pop();
		if(!((File) top.getUserObject()).getPath().equals(windowsTopName)) {
			if(!pathComponents.empty()) {
				current = pathComponents.pop();
				//If on Unix and path was not the root double pop
			}
			else {
				selectDirectory(top);

				return;
			}
		}

		DefaultMutableTreeNode currentTop = top;

		end:
		while(true) {
			int i;
			int numChild=tree.getModel().getChildCount(currentTop);
			for(i=0; i<numChild; i++) { 
				DefaultMutableTreeNode temp=(DefaultMutableTreeNode) 
						tree.getModel().getChild(currentTop, i);

				String nodeName = temp.getUserObject().
							toString().replace("\\", "");
				if(nodeName.equals(current)) {
					if(pathComponents.empty()) {
						//Found path to open
						selectDirectory(temp);
						break end;
					}
					current = pathComponents.pop();
					currentTop = temp;
					Tree.createNodes(currentTop);
					break;
				}
			}
			if(i==numChild)
				break;
		}
	}

	static void refresh(DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();

		Utility.clearPathIcons(((File) node.getUserObject()).getPath());
		node.removeAllChildren();
		DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
		defMod1.reload();
	
		TreePath path = new TreePath(node.getPath());
		if(path.toString().equals("[" + windowsTopName + "]")) {
			//Create root nodes.
			File roots[]=File.listRoots();

			for (File root : roots) {
				node.add(new DefaultMutableTreeNode(root));
			}

			int numChild=defMod1.getChildCount(node);
			for(int i=0; i<numChild; i++) { 
				DefaultMutableTreeNode current=(DefaultMutableTreeNode) 
												defMod1.getChild(node, i);
				Tree.createNodes(current);
			}
		}
		selectDirectory(node);

		JPanel folder = getFolder();
		JPanel current = FolderPanel.getCurrentPanelSelected();

		if(current!=null)
			for(Component comp : folder.getComponents()) {
				if(current.getName().equals(comp.getName())) {
					FolderPanel.selectPanel((JPanel) comp);
					break;
				}
			}
	}

	static void rename(DefaultMutableTreeNode panelNode, JPanel panel) {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();
		String nameNew,	nameOld="";

		ImageIcon img=null;
		Image folderImg;
		int i;

		nameOld = panel.getName();

		File f = new File(filePath + "/" + nameOld);

		if(f.exists() && f.canWrite()) {
			img = Utility.getImageFast(ICONPATH + "other/rename.png", 50, 50, true);

			nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name",
									"Rename", JOptionPane.INFORMATION_MESSAGE,
									img, null, nameOld);

			if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals(""))
				return;

			File file2 = new File(filePath + "/" + nameNew);

			if(file2.exists()) {
				JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
				return;
			}

			if(f.isDirectory()) {
				panelNode.removeFromParent();
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

		selectDirectory(lastTreeNodeOpened);
	}

	static void delete(DefaultMutableTreeNode panelNode, JPanel panel) {
		DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();

		ImageIcon img=null;
		int i;

		String name = panel.getName();
		if(name==null) {
			return;
		}
		File f = new File(filePath + "/" + name);

		img = Utility.getImageFast(ICONPATH + "other/delete.png", 50, 50, true);

		if(f.exists() && f.isFile() && f.canWrite()){
			int input = JOptionPane.showConfirmDialog(null, "Deleting file \"" + 
				name + "\" ?", "Any deletion is permanent", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, 
				img);
		
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
			int input = JOptionPane.showConfirmDialog(null, "Deleting folder \"" 
				+ name + "\" ?", "Any deletion is permanent", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, 
				img);
		
			if(input==JOptionPane.CANCEL_OPTION || input==-1) {
				return;
			}

			panelNode.removeAllChildren();
			panelNode.removeFromParent();
			removeDirectory(f);
			f.delete(); 

			JTree tree = MainWindow.getTree();
			DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
			defMod1.reload();
		}
		else {
			if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				if(panel!=null) {
					panel.setBackground(new Color(0x3fa9ff));
					panel.setBorder(BorderFactory.createLineBorder(Color.black));
				}
				return;
			}

			JOptionPane.showMessageDialog(null, "File didn't exist!");
			return;
		}

		selectDirectory(lastTreeNodeOpened);
	}

	public static String hashSHA(File file, String type) {
		byte[] buffer= new byte[8192];
		int count;
		
		try {
			MessageDigest digest = MessageDigest.getInstance(type);
			BufferedInputStream bis = new BufferedInputStream(new 
				FileInputStream(file));
			
			while ((count = bis.read(buffer)) > 0) {
				digest.update(buffer, 0, count);
			}
			bis.close();

			byte[] hash = digest.digest();
	
			// Conver hash to hex string
			char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

			char[] hexChars = new char[hash.length * 2];
			for (int j = 0; j < hash.length; j++) {
				int v = hash[j] & 0xFF;
				hexChars[j * 2] = HEX_ARRAY[v >>> 4];
				hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
			}
			return new String(hexChars);
		}
		catch(Exception exc) {

		}

		return null;
	}

	public static void properties(File file) {
		ImageIcon img = Utility.getImageFast(ICONPATH + 
					"other/info.png", 50, 50, true);

		long fileSizeInBytes = file.length();
		if(file.isDirectory())
			fileSizeInBytes=0;
		long fileSizeInKB=0, fileSizeInMB=0, fileSizeInGB=0;
		if(fileSizeInBytes!=0) {
			fileSizeInKB = fileSizeInBytes / 1024;
			fileSizeInMB = fileSizeInKB / 1024;
			fileSizeInGB = fileSizeInMB / 1024;
		}

		String size="No calculation (Folder)";
		if(fileSizeInBytes!=0) {
			size = fileSizeInBytes + " B";
		}

		if(fileSizeInKB!=0) {
			double tempSize = (double)file.length()/1024;
			size = String.format("%.2f", tempSize) + " KB "
				+ " ( " + fileSizeInBytes + " B )";
		}
		
		if(fileSizeInMB!=0) {
			double tempSize = (double)file.length()/1024/1024;
			size = String.format("%.2f", tempSize) + " MB "
				+ " ( " + fileSizeInBytes + " B )";
		}

		if(fileSizeInGB!=0) {
			double tempSize = (double)file.length()/1024/1024/1024;
			size = String.format("%.2f", tempSize) + " GB "
				+ " ( " + fileSizeInBytes + " B )";
		}

		SimpleDateFormat sdf = new SimpleDateFormat(
										"dd/MM/yyyy HH:mm:ss");
		
		String fileName = file.getName();
		if(fileName==null || fileName.compareTo("")==0)
			fileName = file.getPath();
		
		String sha256=hashSHA(file, "SHA-256");
		if(sha256==null)
			sha256="No calculation (Folder)";

		String sha1=hashSHA(file, "SHA-1");
		if(sha1==null)
			sha1="No calculation (Folder)";
		
		String avail_space="No space (Folder)";
		if(file.getName().trim().length() == 0)
			avail_space = file.getFreeSpace() + "";

		String text=
			"Name: " + fileName 
			+ "\nSize: " + size
			+ "\nFree Space: " + avail_space
			+ "\nModified: " + sdf.format(file.lastModified())
			+ "\n\nRead: " + file.canRead()
			+ "\nWrite: " + file.canWrite()
			+ "\nExecute: " + file.canExecute()
			+ "\n\nSHA1: " + sha1
			+ "\nSHA256: " + sha256;

		JTextArea properties = new JTextArea(text.toString());
		properties.setEditable(false);
		properties.setBackground(new Color(32, 32, 32));
		properties.setForeground(Color.WHITE);
		
		Font currentFont = properties.getFont();
		Font bigFont = new Font(currentFont.getName(), 
				currentFont.getStyle(), currentFont.getSize() + 5);
		properties.setFont(bigFont);

		JOptionPane.showMessageDialog(null, 
			properties, "Properties", 
			JOptionPane.INFORMATION_MESSAGE, img);	
	}

	static public void selectDirectory(DefaultMutableTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);
		Tree.setLastTreeNodeOpened(node);
		FolderPanel.showCurrentDirectory(node);	
	}

	static public void enterOrOpen(File file, DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();
	
		if(!file.exists()) {
			Tree.findExistingParent(file);
			return;
		}

		if(file.isDirectory()) {
			selectDirectory(node);
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

	public static DefaultMutableTreeNode getTop() {
		return top;
	}

	public static JPanel getFolder() {
		return folder;
	}

	public static JTree getTree() {
		return tree;
	}

	private static boolean isLastExplorer=true;

	public static void setFocusExplorer() {
		isLastExplorer=true;
	}

	public static void setFocusTree() {
		isLastExplorer=false;
	}

	public static void focusLast() {
		if(isLastExplorer)
			folder.requestFocusInWindow();
		else
			tree.requestFocusInWindow();
	}

	public static void historyPush(DefaultMutableTreeNode node) {
		if(node==null)
			return;

		if(history.empty() || (!history.empty() && history.peek()!=node)) {
			history.push(node);
			TopPanel.getButtonBack().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/backarrow.png", 23, 23, true));
		}
	}

	public static DefaultMutableTreeNode historyPop() {
		if(!history.empty()) {
			if(history.size()==1) {
				TopPanel.getButtonBack().setIcon(Utility.getImageFast(
					FileExplorer.getIconPath() + 
						"other/grayedback.png", 23, 23, true));
			}

			return history.pop();
		}

		return null;
	}

	public static void clearFuture() {
		futureHistory.clear();
		TopPanel.getButtonForward().setIcon(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedforward.png", 23, 23, true));
	}

	public static void futureHistoryPush(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		if(futureHistory.size()==0) {
			futureHistory.push(lastTreeNodeOpened);		
		}

		if(futureHistory.peek()!=node) {
			futureHistory.push(node);
			TopPanel.getButtonForward().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/forwardarrow.png", 23, 23, true));
		}
	}

	public static DefaultMutableTreeNode futureHistoryPop() {
		DefaultMutableTreeNode node;

		if(futureHistory.empty()) {
			return null;
		}

		node = futureHistory.pop();
		if(futureHistory.empty()) {
			TopPanel.getButtonForward().setIcon(Utility.getImageFast(
				FileExplorer.getIconPath() + 
					"other/grayedforward.png", 23, 23, true));		
		}
		return node;
	}
}
