import java.io.File;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class TopPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static String windowsTopName = Tree.getWindowsTopName();
	private static final Color topColor = new Color(25, 25, 25);
	private static final int navHeight = 26;

	private static JButton buttonBack, buttonForward;
	private static JTextFieldIcon searchField, navigationField;
	private static JPanel buttonField;
	private static String searchQuery = "";

	public TopPanel() {
		super(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		this.setBackground(Color.WHITE);
		this.setBorder(
			BorderFactory.createMatteBorder(6, 3, 6, 3, topColor));

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;


		buttonBack = new JButton(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedback.png", navHeight, navHeight, true));
		buttonBack.setBorder(BorderFactory.createEmptyBorder());
		buttonBack.setPreferredSize(new Dimension(navHeight, navHeight));
		buttonBack.setFocusPainted(false);

		buttonBack.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e){  
				historyBack();
				MainWindow.focusLast();
			}
		});
		this.add(buttonBack, c);

		c.weightx = 0.005;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;

		buttonForward = new JButton(Utility.getImageFast(
			FileExplorer.getIconPath() + 
				"other/grayedforward.png", navHeight, navHeight, true));
		buttonForward.setBorder(BorderFactory.createEmptyBorder());
		buttonForward.setPreferredSize(new Dimension(navHeight, navHeight));
		buttonForward.setFocusPainted(false);

		buttonForward.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				historyForward();
				MainWindow.focusLast();
			}
		});
		this.add(buttonForward, c);

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;

		navigationField = new JTextFieldIcon(new JTextField(), 
			Utility.getImageFast(FileExplorer.getIconPath() + "other/pc.png", 
				15, 15, true));

		navigationField.setCaretColor(Color.WHITE);
		navigationField.setBackground(topColor);
		navigationField.setForeground(new Color(0, 255, 255));
		navigationField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, navHeight));
		
		Font font = new Font("SansSerif", Font.BOLD, 13);
		navigationField.setFont(font);

		navigationField.setVisible(false);

		navigationField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
                navigationField.select(0, navigationField.getText().length());
			}
			public void focusLost(FocusEvent e) {
				DefaultMutableTreeNode node = Tree.getLastTreeNodeOpened();
				String path = ((File) node.getUserObject()).getPath();

				navigationField.setText(path);
				toggleNavigation();
			}
		});

		navigationField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					String path = navigationField.getText();
					File file = new File(path);

					if(file.exists()) {
						java.util.Stack<String> pathComponents = 
							new java.util.Stack<String>();

						while(true) {
							if(file.getParentFile() == null)
								break;
							pathComponents.add(file.getName());
							file = file.getParentFile();
						} 
						pathComponents.add(file.getPath().replace("\\", ""));
					
						MainWindow.loadPath(MainWindow.getTop(), pathComponents);
						MainWindow.focusLast();
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		this.add(navigationField, c);

		buttonField = new JPanel();
		buttonField.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttonField.setBackground(topColor);
		buttonField.setForeground(new Color(0, 255, 255));
		buttonField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, navHeight));

		buttonField.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {
				toggleNavigation();
				navigationField.requestFocusInWindow();
			}
			@Override
			public void mouseEntered(MouseEvent event) {}
			@Override
			public void mouseExited(MouseEvent event) {}
			@Override
			public void mousePressed(MouseEvent event) {}
			@Override
			public void mouseReleased(MouseEvent event) {}
		});

		this.add(buttonField, c);

		searchField = new JTextFieldIcon(new JTextField(), 
			Utility.getImageFast(FileExplorer.getIconPath() + 
				"other/magnifyingglass.png", 15, 15, true));

		searchField.setCaretColor(Color.WHITE);
		searchField.setBackground(topColor);
		searchField.setForeground(new Color(0, 255, 255));
		searchField.setPreferredSize(new Dimension(searchField.
				getPreferredSize().width, navHeight));

		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					JPanel folder = MainWindow.getFolder();
					searchQuery = searchField.getText();
					
					MainWindow.focusLast();

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
						Tree.getLastTreeNodeOpened();
					File top = (File) node.getUserObject();

					folder.removeAll();
					folder.repaint();
					folder.revalidate();

					searchField.setText("");

					 // create object of table and table model
					DefaultTableModel model = new DefaultTableModel(0, 1) {};
					JTable table = new JTable(model);

					// add header of the table
					String header[] = new String[] { "Name" };

					// add header in table model     
					model.setColumnIdentifiers(header);
					//set model into the table object
					TableColumnModel tcm = table.getColumnModel();
					tcm.getColumn(0).setCellRenderer(new IconTextCellRenderer());

					table.setBackground(new Color(49, 49, 49));
					table.setForeground(Color.WHITE);
					table.setRowHeight(50);
					table.setShowGrid(false);
					table.setDefaultEditor(Object.class, null);
					table.setSelectionModel(new DefaultListSelectionModel() {
					    @Override
					    public void clearSelection() {
					    }

					    @Override
					    public void removeSelectionInterval(
					    	int index0, int index1) {
					    }
					});
					table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

					table.addMouseListener(new MouseAdapter() {
						public void mouseClicked(MouseEvent e) {
							JTree tree = MainWindow.getTree();
							JPanel folder = MainWindow.getFolder();
										
							if (e.getClickCount() == 2 && 
										e.getButton() == MouseEvent.BUTTON1) {
								JTable target = (JTable) e.getSource();
								int row = target.getSelectedRow();
								int column = target.getSelectedColumn();

								DefaultMutableTreeNode node = 
									(DefaultMutableTreeNode)
										 target.getValueAt(row, column);

								File file = (File) node.getUserObject();
								if(file.isDirectory()) {
									MainWindow.selectDirectory(node);
								}
								else {
									try {
										Desktop.getDesktop().open(file);
									}
									catch(IOException exc) {

									}
								}
							}
							else if(e.getButton() == MouseEvent.BUTTON3) {
								System.out.println("right click");
							}						
						}
					});

					folder.add(table);
					folder.setLayout(new GridLayout());
	
					JTree tree = MainWindow.getTree();
					Executor executor = FolderPanel.getExecutor();
					executor.execute(new Runnable() {
						public void run() { 
							search(tree, node, searchQuery, model);
						}
					});
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

				searchField.setText("Search" + " \"" + fileName + "\"");
				searchField.setCaretPosition(0);
			}
		});

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.15;
		c.gridx = 3;
		c.gridy = 0;

		this.add(searchField, c);
	}

	static void search(JTree tree, DefaultMutableTreeNode top, String searchQuery, DefaultTableModel model) {
		Tree.createNodes(top);

		int numChild=tree.getModel().getChildCount(top);
		DefaultMutableTreeNode current;
		File topFile = (File) top.getUserObject();
		JPanel folder = MainWindow.getFolder();

		if(numChild==0)
			return; 

		boolean isSymbolicLink = Files.isSymbolicLink(topFile.toPath());
		if(isSymbolicLink)
			return;

		File children[] = ((File) top.getUserObject()).listFiles();
		if(children!=null) {
			for(File child : children) {
				if(child.isFile() && child.getName().contains(searchQuery)) {
					model.addRow(new Object[] { new DefaultMutableTreeNode(child) });
				
					folder.repaint();
					folder.revalidate();
				}
			}
		}

		for(int i=0; i<numChild; i++) {	  
			current = (DefaultMutableTreeNode) tree.getModel().getChild(top, i);
			File element = (File) current.getUserObject();

			System.out.println(element.getName());

			if(element.getName().contains(searchQuery)) {
				model.addRow(new Object[] { current });
				folder.repaint();
				folder.revalidate();
			}

			search(tree, current, searchQuery, model);
		}		  
	}

	public static void historyBack() {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode previous,
						lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		FolderPanel.clearPanelSelection();

		previous = MainWindow.historyPop();
		if(previous==null) {
			return;
		}

		MainWindow.futureHistoryPush(previous);
		if(previous==lastTreeNodeOpened) {
			previous = MainWindow.historyPop();
			if(previous==null) {
				return;
			}

			MainWindow.futureHistoryPush(previous);
		}

		File file = (File) previous.getUserObject();

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			MainWindow.selectDirectory(previous);

			return;
		}

		MainWindow.enterOrOpen(previous);
	}

	public static void historyForward() {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode next,
						lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		next=MainWindow.futureHistoryPop();
		if(next==null)
			return;

		MainWindow.historyPush(next);
		if(next==lastTreeNodeOpened) {
			next=MainWindow.futureHistoryPop();
			if(next==null)
				return;
			MainWindow.historyPush(next);	
		}

		File file = (File) next.getUserObject();

		if(file.getName().equals(windowsTopName) && !file.exists()) {
			MainWindow.selectDirectory(next);

			return;
		}
		MainWindow.enterOrOpen(next);
	}

	public static void setNavigationText(String text) {
		navigationField.setText(text);
	}

	public static void setSearchText(String text) {
		searchField.setText(text);
	}

	public static void clearNavButtons() {
		buttonField.removeAll();
		buttonField.revalidate();
		buttonField.repaint();

		JLabel myLabel = new JLabel(Utility.getImageFast(
			FileExplorer.getIconPath() + "other/pc.png", 
				navHeight, navHeight, true));
		myLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
		buttonField.add(myLabel);
	}

	public static void addNavButton(DefaultMutableTreeNode node) {
		File file = (File) node.getUserObject();
		String name;

		name = file.getName();
		if(name.trim().length() == 0) {
			if(file.getPath().equals("/"))
				name = file.getPath();
			else
				name = "Local Disk (" + file.getPath().replace("\\", "") + ")";
		}

		JButton button = new JButton(name);
		button.setFocusPainted(false);
		button.setBackground(topColor);
		button.setForeground(new Color(0, 255, 255));
		button.setPreferredSize(new Dimension(button.
					getPreferredSize().width, navHeight));
		button.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {}
			@Override
			public void mouseEntered(MouseEvent event) {
				button.setForeground(Color.WHITE);
			}
			@Override
			public void mouseExited(MouseEvent event) {
				button.setForeground(new Color(0, 255, 255));
			}
			@Override
			public void mousePressed(MouseEvent event) {
				button.setForeground(Color.BLACK);
			}
			@Override
			public void mouseReleased(MouseEvent event) {
				MainWindow.selectDirectory(node);
				MainWindow.getFolder().requestFocusInWindow();
				MainWindow.focusLast();
			}
		});

		buttonField.add(button);
		buttonField.revalidate();
		buttonField.repaint();
	}

	public static void toggleNavigation() {
		boolean navShown = navigationField.isVisible();
		boolean buttonShown = buttonField.isVisible();

		if(navShown && !buttonShown) {
			navigationField.setVisible(false);
			buttonField.setVisible(true);
		}
		else if(!navShown && buttonShown){
			navigationField.setVisible(true);
			buttonField.setVisible(false);		
		}
	}

	public static JButton getButtonBack() {
		return buttonBack;
	}

	public static JButton getButtonForward() {
		return buttonForward;
	}
}

class IconTextCellRenderer extends DefaultTableCellRenderer {
	private static final String ICONPATH = FileExplorer.getIconPath();
	static Set<String> iconSet = FileExplorer.addExtensions();

	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		File file = (File) node.getUserObject();

		setText(file.getPath());
		if(file.isDirectory())
			setIcon(getSmallIcon("folder.png", file));
		else if(file.isFile() && iconSet.contains(Utility.getExtension(file.getName())))
			setIcon(getSmallIcon(Utility.
				getExtension(file.getName()) + ".png", file));
		else
			setIcon(getSmallIcon("question.png", file));

		setBorder(new EmptyBorder(0, 10, 0, 0));
		setIconTextGap(10);

		return this;
	}

	public static ImageIcon getSmallIcon(String name, File file) {
		JLabel label = new JLabel();
		ImageIcon img=null;
		Set<String> set = new HashSet<>(); 
		String path = null;

		// Bad check for images
		set.add("jpeg");
		set.add("jpg");
		set.add("png");
		set.add("gif");
		if(set.contains(Utility.getExtension(file.getName()))) {
			path = file.getPath();
		}

		if(path==null) {
			if(name=="folder.png") {
				if(file.list()!=null && file.list().length==0)
					path = ICONPATH + "other/" + "folderempty.png";
				else {
					path = ICONPATH + "other/" + "folder.png";
				}
			}
			else if(name=="question.png") {
				path = ICONPATH + "other/" + "question.png";
			}
			else
				path = ICONPATH + "extensions/" + name;
		}

		img = Utility.getImageFast(path, 35, 35, true);

		return img;
	}
}