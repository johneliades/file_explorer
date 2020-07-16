import java.io.File;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.filechooser.FileSystemView;

import java.text.SimpleDateFormat;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;

import java.util.*;
import java.util.concurrent.*;

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
	
		BasicTreeUI basicTreeUI = (BasicTreeUI) tree.getUI();
		basicTreeUI.setLeftChildIndent(0);
		basicTreeUI.setRightChildIndent(12);
		basicTreeUI.setCollapsedIcon(Utility.getImageFast(ICONPATH + 
								"other/collapsed.png", 9, 9, true));
		basicTreeUI.setExpandedIcon(Utility.getImageFast(ICONPATH + 
			"other/expanded.png", 9, 9, true));

		LookAndFeel previousLF = UIManager.getLookAndFeel();
		try {
			UIManager.setLookAndFeel(
				UIManager.getSystemLookAndFeelClassName());

			//Create the scroll pane and add the tree to it. 
			JScrollPane treeView = new JScrollPane(tree);
			treeView.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
			treeView.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
			treeView.getVerticalScrollBar().setBackground(new Color(53, 53, 53));
			treeView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			treeView.setBorder(new EmptyBorder(0, 0, 0, 0));

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
			
			UIManager.setLookAndFeel(previousLF);
		} 
		catch (ClassNotFoundException | 
			IllegalAccessException | 
			InstantiationException | 
			UnsupportedLookAndFeelException e) {
			
			System.err.println("Couldn't use system look and feel.");
		}	
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
		JPanel current = FolderPanel.getLastPanelSelected();

		if(current!=null)
			for(Component comp : folder.getComponents()) {
				if(current.getName().equals(comp.getName())) {
					FolderPanel.selectPanel((JPanel) comp, true);
					break;
				}
			}
	}

	static void rename(DefaultMutableTreeNode panelNode) {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
		
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();
		String nameNew,	nameOld="", path;

		File nodeFile = (File) panelNode.getUserObject();

		ImageIcon img=null;
		Image folderImg;
		int i;

		nameOld = nodeFile.getName();

		if(nameOld.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(nodeFile);
			nameOld = description + " (" + nodeFile.getPath().replace("\\", "") + ")";
		}

		File f = new File(filePath + "/" + nameOld);

		if(f.exists() && f.canWrite()) {
			img = Utility.getImageFast(ICONPATH + "other/rename.png", 50, 50, true);

			nameNew=(String) JOptionPane.showInputDialog(null, "Enter New Name",
									"Rename", JOptionPane.INFORMATION_MESSAGE,
									img, null, nameOld);

			
			if(nameNew==null || nameNew.equals(nameOld) || nameNew.equals("")) {
				return;
			}

			String invalidStripped = 
				nameNew.replaceAll("[\\\\/:*?\"<>|]", "_");
			if(!nameNew.equals(invalidStripped)) {
				JOptionPane.showMessageDialog(null, 
					"Replace invalid characters with \"_\"");
				nameNew = invalidStripped;
			}

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
			if(!f.exists()) {
				JOptionPane.showMessageDialog(null, "File doesn't exist!");
				return;
			}
			else if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				return;
			}

			JOptionPane.showMessageDialog(null, "Rename Failed!");
			return;
		}

		DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();	
		defMod1.reload();

		selectDirectory(lastTreeNodeOpened);
		MainWindow.focusLast();
	}

	static void delete(DefaultMutableTreeNode panelNode) {
		DefaultMutableTreeNode lastTreeNodeOpened = Tree.getLastTreeNodeOpened();
		String filePath = ((File) lastTreeNodeOpened.getUserObject()).getPath();

		File nodeFile;
		 
		nodeFile = ((File) panelNode.getUserObject());
	
		ImageIcon img=null;
		int i;

		String name = nodeFile.getName();
		if(name.trim().length() == 0) {
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(nodeFile);
			name = description + " (" + nodeFile.getPath().replace("\\", "") + ")";
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
			if(!f.exists()) {
				JOptionPane.showMessageDialog(null, "File doesn't exist!");
				return;
			}
			else if(!f.canWrite()) {
				JOptionPane.showMessageDialog(null, "Not enough permissions!");
				return;
			}
			
			JOptionPane.showMessageDialog(null, "Deletion failed!");
			return;
		}

		selectDirectory(lastTreeNodeOpened);
		MainWindow.focusLast();
	}

	public static boolean pasteFile(File source, File destination, String op) throws IOException {
		File newFile = new File(destination.getPath() + File.separator + source.getName());
		boolean success = true;

		if(op == "cut") {
			source.renameTo(newFile);
		}
		else if(op == "copy") {
			if(source.isDirectory()) {
				if(!newFile.exists())
					newFile.mkdir();
			
				String files[] = source.list();
				for (String file : files) {
					File srcFile = new File(source, file);
					success = pasteFile(srcFile, newFile, op);
					
					File destFile = new File(newFile, file);
					if(!hashFile(srcFile, "MD5").equals(hashFile(destFile, "MD5"))) {
						System.out.println("copy failed " + srcFile.toPath() + " " + destFile.toPath());
						return false;
					}
				}
			}
			else {
				Files.copy(source.toPath(), newFile.toPath());
				if(!hashFile(newFile, "MD5").equals(hashFile(source, "MD5"))) {
					System.out.println("copy failed " + source.toPath() + " " + newFile.toPath());
					return false;
				}
			}	
		}

		return success;
	}

	public static String hashStream(BufferedInputStream bis, String type) {
		byte[] buffer= new byte[4096];
		int count;

		try {
			MessageDigest digest = MessageDigest.getInstance(type);
			
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

	public static String hashFile(File file, String type) {

		if(file.isDirectory()) {
			File[] files = file.listFiles();
			if(files == null)
				return "";
			
			String total = "";

			Arrays.sort(files);

			for(File current : files)
				total += hashFile(current, type);

			InputStream stream = new ByteArrayInputStream(
				total.getBytes(StandardCharsets.UTF_8));

			BufferedInputStream bis = new BufferedInputStream(stream);
			return hashStream(bis, type);
		}
		else {
			try {
				BufferedInputStream bis = new BufferedInputStream(new 
					FileInputStream(file));

				return hashStream(bis, type);
			}
			catch(Exception e) {

			}
		}
		return "";
	}

	private static String convertBytes(long bytes) {
		long fileSizeInKB=0, fileSizeInMB=0, fileSizeInGB=0;
		if(bytes!=0) {
			fileSizeInKB = bytes / 1024;
			fileSizeInMB = fileSizeInKB / 1024;
			fileSizeInGB = fileSizeInMB / 1024;
		}

		String size="";
		if(bytes!=0) {
			size = bytes + " B";
		}

		if(fileSizeInKB!=0) {
			double tempSize = (double) bytes/1024;
			size = String.format("%.2f", tempSize) + " KB "
				+ " ( " + bytes + " B )";
		}
		
		if(fileSizeInMB!=0) {
			double tempSize = (double) bytes/1024/1024;
			size = String.format("%.2f", tempSize) + " MB "
				+ " ( " + bytes + " B )";
		}

		if(fileSizeInGB!=0) {
			double tempSize = (double) bytes/1024/1024/1024;
			size = String.format("%.2f", tempSize) + " GB "
				+ " ( " + bytes + " B )";
		}

		return size;
	}

	public static long folderSize(File directory) {
		long length = 0;
		try {
			for (File file : directory.listFiles()) {
				if (file.isFile())
					length += file.length();
				else
					length += folderSize(file);
			}
		}
		catch(Exception e) {
			return 0;
		}

		return length;
	}

	public static void properties(File file) {
		JFrame frame = FileExplorer.getFrame();

		JDialog dialog = new JDialog(frame, "Properties"); 
		dialog.setUndecorated(true);

		JPanel panel = new MotionPanel(dialog); 
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(new EmptyBorder(20, 20, 20, 20));

		ImageIcon img = Utility.getImageFast(ICONPATH + 
					"other/info.png", 50, 50, true);
		JLabel label = new JLabel(img); 

		Font currentFont = label.getFont();
		Font bigFont = new Font(currentFont.getName(), 
				currentFont.getStyle(), currentFont.getSize() + 2);

		label.setForeground(Color.WHITE);
		panel.add(label);
		
		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		dialog.add(panel); 
		dialog.setResizable(false);
		dialog.setSize(200, 200); 
		dialog.setAlwaysOnTop(true);
		dialog.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent e) {
			}

			public void windowLostFocus(WindowEvent e) {
				dialog.setVisible(false);
			}
		});

		String fileName = file.getName();
		if(fileName==null || fileName.compareTo("")==0)
			fileName = file.getPath();
		
		JTextField field = new JTextField("Name: " + fileName);
		field.setEditable(false);
		field.setBorder(null);
		field.setForeground(Color.WHITE);
		field.setBackground(UIManager.getColor(Color.BLACK));
		field.setFont(bigFont);
		panel.add(field);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		SimpleDateFormat sdf = new SimpleDateFormat(
										"dd/MM/yyyy HH:mm:ss");
		
		label = new JLabel("Modified: " + sdf.format(file.lastModified())); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		panel.add(label);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		
		JCheckBox execute = new JCheckBox("Execute", file.canExecute());
		execute.setBackground(new Color(22, 22, 22));
		execute.setForeground(Color.WHITE);
		execute.setFocusPainted(false);
		if(file.setExecutable(!file.canExecute())){
		   file.setExecutable(!file.canExecute());
		}
		else{
			execute.setEnabled(false);
		}
		execute.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setExecutable(true);
				else
					file.setExecutable(false);
			}
		});
		
		JCheckBox read = new JCheckBox("Read", file.canRead());
		read.setBackground(new Color(22, 22, 22));
		read.setForeground(Color.WHITE);
		read.setFocusPainted(false);
		if(file.setReadable(!file.canRead())){
			file.setReadable(!file.canRead());
		}
		else{
			read.setEnabled(false);
		}
		read.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setReadable(true);
				else
					file.setReadable(false);
			}
		});

		JCheckBox write = new JCheckBox("Write", file.canWrite());
		write.setBackground(new Color(22, 22, 22));
		write.setForeground(Color.WHITE);
		write.setFocusPainted(false);
		if(file.setWritable(!file.canWrite())){
			file.setWritable(!file.canWrite());
		}
		else{
			write.setEnabled(false);
		}
		write.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==1)
					file.setWritable(true);
				else
					file.setWritable(false);
			}
		});

		panel.add(execute);
		panel.add(read);
		panel.add(write);

		panel.add(Box.createRigidArea(new Dimension(0, 20)));

		LookAndFeel previousLF = UIManager.getLookAndFeel();
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : 
				javax.swing.UIManager.getInstalledLookAndFeels()) {
				
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}

			JButton button = new JButton("MD5");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String sha256=hashFile(file, "MD5");
					if(sha256==null)
						sha256="";

					JTextField field = new JTextField(sha256);
					field.setEditable(false);
					field.setBorder(null);
					field.setForeground(Color.WHITE);
					field.setBackground(UIManager.getColor("Panel.background"));
					field.setFont(bigFont);
					if(sha256.length()!=0) {
						JButton buttonThatWasClicked = (JButton) e.getSource();
						panel.add(field, panel.getComponentZOrder(buttonThatWasClicked));
						panel.remove(buttonThatWasClicked);

						dialog.pack();
					}
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);

			panel.add(Box.createRigidArea(new Dimension(0, 3)));

			button = new JButton("SHA1");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String sha1=hashFile(file, "SHA-1");
					if(sha1==null)
						sha1="";
					JTextField field = new JTextField(sha1);
					field.setEditable(false);
					field.setBorder(null);
					field.setForeground(Color.WHITE);
					field.setBackground(UIManager.getColor("Panel.background"));
					field.setFont(bigFont);
					if(sha1.length()!=0) {
						JButton buttonThatWasClicked = (JButton) e.getSource();
						panel.add(field, panel.getComponentZOrder(buttonThatWasClicked));
						panel.remove(buttonThatWasClicked);
		
						dialog.pack();
					}
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);	

			panel.add(Box.createRigidArea(new Dimension(0, 3)));

			button = new JButton("SHA256");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String sha256=hashFile(file, "SHA-256");
					if(sha256==null)
						sha256="";

					JTextField field = new JTextField(sha256);
					field.setEditable(false);
					field.setBorder(null);
					field.setForeground(Color.WHITE);
					field.setBackground(UIManager.getColor("Panel.background"));
					field.setFont(bigFont);
					if(sha256.length()!=0) {
						JButton buttonThatWasClicked = (JButton) e.getSource();
						panel.add(field, panel.getComponentZOrder(buttonThatWasClicked));
						panel.remove(buttonThatWasClicked);

						dialog.pack();
					}
				}
			});
			button.setBackground(Color.BLACK);
			button.setForeground(Color.CYAN);
			panel.add(button);

			UIManager.setLookAndFeel(previousLF);
		}
		catch(Exception e) {}

		long bytes = 0;
		String size = "", avail_space = "";
		boolean calculated = false;

		if(!file.isDirectory()) {
			bytes = file.length();
			size = convertBytes(bytes);
			calculated = true;
			panel.add(Box.createRigidArea(new Dimension(0, 20)));
		}
		else if(file.getName().trim().length() == 0) {
			avail_space = convertBytes(file.getFreeSpace());
			size = convertBytes(file.getTotalSpace());
			calculated = true;
			panel.add(Box.createRigidArea(new Dimension(0, 20)));
		}

		label = new JLabel("\nSize: " + size); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		if(size.length()!=0)
			panel.add(label);

		label = new JLabel("\nFree Space: " + avail_space); 
		label.setForeground(Color.WHITE);
		label.setFont(bigFont);
		if(avail_space.length()!=0)
			panel.add(label);

		if(!calculated) {
			Executor calcSize = Executors.newSingleThreadExecutor();
			calcSize.execute(new Runnable() {
				public void run() { 
					panel.add(Box.createRigidArea(new Dimension(0, 20)));
					JLabel label = new JLabel("\nSize: Calculating..."); 
					label.setForeground(Color.WHITE);
					label.setFont(bigFont);
					panel.add(label);
					dialog.pack();

					long bytes = folderSize(file);

					String size="0";
					if(bytes!=0)
						size = convertBytes(bytes);

					panel.remove(label);
					dialog.pack();

					label = new JLabel("\nSize: " + size); 
					label.setForeground(Color.WHITE);
					label.setFont(bigFont);
					if(size.length()!=0) {
						panel.add(label);
						dialog.pack();
					}
				}
			});
		}
		
		dialog.pack();

		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	static public void selectDirectory(DefaultMutableTreeNode node) {
		TreePath path = new TreePath(node.getPath());
		tree.setSelectionPath(path);
		tree.scrollPathToVisible(path);
		tree.expandPath(path);
		Tree.setLastTreeNodeOpened(node);
		FolderPanel.showCurrentDirectory(node);	
	}

	static public void enterOrOpen(DefaultMutableTreeNode node) {
		JTree tree = MainWindow.getTree();
	
		File file = (File) node.getUserObject();

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
