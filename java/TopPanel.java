import java.io.File;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class TopPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	static Set<String> iconSet = FileExplorer.addExtensions();
	static JPanel folder;
	
	private static JButton buttonBack, buttonForward;
	private static JTextField searchField, navigationField;
	private static String searchQuery = "";

	public TopPanel() {
		super(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		this.setBackground(Color.white);

		ImageIcon img;
		Image pict;

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;

		img = new ImageIcon(FileExplorer.getIconPath() + "other/grayedback.png");
		pict = img.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT);
		img = new ImageIcon(pict);
		
		buttonBack = new JButton(img);
		buttonBack.setBorder(BorderFactory.createEmptyBorder());
		buttonBack.setContentAreaFilled(false);
		buttonBack.setPreferredSize(new Dimension(23, 23));
		
		buttonBack.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e){  
				MainWindow.historyBack();
			}
		});
		this.add(buttonBack, c);

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;

		img = new ImageIcon(FileExplorer.getIconPath() + "other/grayedforward.png");
		pict = img.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT);
		img = new ImageIcon(pict);

		buttonForward = new JButton(img);
		buttonForward.setBorder(BorderFactory.createEmptyBorder());
		buttonForward.setContentAreaFilled(false);
		buttonForward.setPreferredSize(new Dimension(23, 23));
	
		buttonForward.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				MainWindow.getFolder().requestFocusInWindow();
			}
		});
		this.add(buttonForward, c);

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;

		navigationField = new JTextField("");
		navigationField.setPreferredSize(new Dimension(navigationField.getPreferredSize().width, 25));

		this.add(navigationField, c);

		searchField = new JTextField("");
		searchField.setPreferredSize(new Dimension(searchField.getPreferredSize().width, 25));
		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					JTree tree = MainWindow.getTree();
					folder = MainWindow.getFolder();
					searchQuery = searchField.getText();
					
					tree.requestFocusInWindow();

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
					File top = (File) node.getUserObject();

					folder.removeAll();
					folder.repaint();
					folder.revalidate();

					searchField.setText("");
					JPanel gridPanel = new JPanel(new GridLayout(0, 1, 8, 8));
					gridPanel.setBackground(new Color(53, 53, 53));
					gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); //top,left,bottom,right
					folder.add(gridPanel);
	
					Thread thread = new Thread() {
						public void run() {
							search(tree, node, searchQuery, gridPanel);
						}
					};
					thread.start();
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		searchField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				searchField.setText("");
			}
			public void focusLost(FocusEvent e) {
				DefaultMutableTreeNode node = Tree.getLastTreeNodeOpened();
				String fileName = ((File) node.getUserObject()).getName();

				if(fileName.equals("") || fileName==null) {
					fileName = ((File) node.getUserObject()).getPath();
				}

				searchField.setText(" Search" + " \"" + fileName + "\"");
			}
		});

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.15;
		c.gridx = 3;
		c.gridy = 0;

		this.add(searchField, c);
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
		if(set.contains(Utility.getExtension(file.getName()))) {
			img = new ImageIcon(file.getPath());
		}

		if(img==null) {
			if(name=="folder.png") {
				if(file.list()!=null && file.list().length==0)
					img = new ImageIcon(ICONPATH + "other/" + "folderempty.png");
				else {
					img = new ImageIcon(ICONPATH + "other/" + "folder.png");
				}
			}
			else if(name=="question.png") {
				img = new ImageIcon(ICONPATH + "other/" + "question.png");
			}
			else
				img = new ImageIcon(ICONPATH + "extensions/" + name);
		}

		pict = img.getImage().getScaledInstance(45, 45, Image.SCALE_DEFAULT);
		img = new ImageIcon(pict);

		label.setIcon(img);
		label.setText(file.getPath());
		label.setBorder(new EmptyBorder(5, 0, 5, 0));

		final Font currentFont = label.getFont();
		final Font bigFont = new Font(currentFont.getName(), 
					currentFont.getStyle(), currentFont.getSize() + 1);
		label.setFont(bigFont);
		label.setForeground(Color.WHITE);
		label.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent arg0) {}
			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
			@Override
			public void mousePressed(MouseEvent event) {
				JTree tree = MainWindow.getTree();
				JPanel folder = MainWindow.getFolder();

				String fullPath = label.getText();
				File file = new File(fullPath);
							
				label.setForeground(Color.RED);		

				if(event.getButton() == MouseEvent.BUTTON1) {
					if(file.isDirectory()) {
						TreePath path = new TreePath(node.getPath());
						tree.setSelectionPath(path);
						tree.scrollPathToVisible(path);
						tree.expandPath(path);

						FolderPanel.showCurrentDirectory(node); 
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
					System.out.println("right click");
				}
			}
			@Override
			public void mouseExited(MouseEvent arg0) {
				label.setForeground(Color.WHITE);		
			}
			@Override
			public void mouseEntered(MouseEvent arg0) {
				label.setForeground(new Color(0, 255, 255));		
			}
		});
		
		return label;
	}

	static void search(JTree tree, DefaultMutableTreeNode top, String searchQuery, JPanel gridPanel) {
		int numChild=tree.getModel().getChildCount(top);
		DefaultMutableTreeNode current;
		File topFile = (File) top.getUserObject();

		if(numChild==0)
			return; 

		boolean isSymbolicLink = Files.isSymbolicLink(topFile.toPath());
		if(isSymbolicLink)
			return;

		Tree.createNodes(top, 0);

		for(int i=0; i<numChild; i++) {	  
			current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
			File element = (File) current.getUserObject();

			if(element.getName().contains(searchQuery)) {
				gridPanel.add(getSmallIcon("folder.png", element, current));
				folder.repaint();
				folder.revalidate();
			}
			
			File children[] = element.listFiles();
			if(children==null)
				continue;
			for(File child : children) {
				if(child.isFile() && child.getName().contains(searchQuery)) {
					if(iconSet.contains(Utility.getExtension(child.getName())))
						gridPanel.add(getSmallIcon(Utility.getExtension(child.getName()) + ".png", child, current));
					else
						gridPanel.add(getSmallIcon("question.png", child, current));
				
					folder.repaint();
					folder.revalidate();
				} 
			}

			search(tree, current, searchQuery, gridPanel);
		}		  
	}

	public static JTextField getNavigationField() {
		return navigationField;
	}

	public static JTextField getSearchField() {
		return searchField;
	}

	public static JButton getButtonBack() {
		return buttonBack;
	}

	public static JButton getButtonForward() {
		return buttonForward;
	}
}
