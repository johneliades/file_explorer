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

public class TopPanel extends JPanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static String windowsTopName = Tree.getWindowsTopName();
	
	private static JButton buttonBack, buttonForward;
	private static JTextFieldIcon searchField, navigationField;
	private static JPanel buttonField;
	private static String searchQuery = "";
		
	public static final ImageIcon grayedForward = new ImageIcon(
		(new ImageIcon(FileExplorer.getIconPath() + "other/grayedforward.png"))
			.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT));
	
	public static final ImageIcon backArrow = new ImageIcon(
		(new ImageIcon(FileExplorer.getIconPath() + "other/backarrow.png"))
			.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT));
	
	public static final ImageIcon grayedBack = new ImageIcon(
		(new ImageIcon(FileExplorer.getIconPath() + "other/grayedback.png"))
			.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT));

	public static final ImageIcon forwardArrow = new ImageIcon(
		(new ImageIcon(FileExplorer.getIconPath() + "other/forwardarrow.png"))
			.getImage().getScaledInstance(23, 23, Image.SCALE_DEFAULT));

	private static final ImageIcon folderIconPC = new ImageIcon(
		new ImageIcon(ICONPATH + "other/pc.png").getImage().
						getScaledInstance(20, 20, Image.SCALE_DEFAULT));

	public TopPanel() {
		super(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		this.setBackground(Color.WHITE);

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
		buttonBack.setPreferredSize(new Dimension(23, 23));
		buttonBack.setFocusPainted(false);

		buttonBack.addActionListener(new ActionListener(){  
			public void actionPerformed(ActionEvent e){  
				historyBack();
				MainWindow.getFolder().requestFocusInWindow();
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
		buttonForward.setPreferredSize(new Dimension(23, 23));
		buttonForward.setFocusPainted(false);

		buttonForward.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				historyForward();
				MainWindow.getFolder().requestFocusInWindow();
			}
		});
		this.add(buttonForward, c);

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 0;

		navigationField = new JTextFieldIcon(new JTextField(), 
			new ImageIcon((new ImageIcon(
				FileExplorer.getIconPath() + "other/pc.png"))
						.getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));		
		
		navigationField.setCaretColor(Color.WHITE);
		navigationField.setBackground(new Color(30, 30, 30));
		navigationField.setForeground(new Color(0, 255, 255));
		navigationField.setSelectionColor(Color.WHITE);
		navigationField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, 25));
		
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
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});

		this.add(navigationField, c);

		buttonField = new JPanel();
		buttonField.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttonField.setBackground(new Color(30, 30, 30));
		buttonField.setForeground(new Color(0, 255, 255));
		buttonField.setPreferredSize(new Dimension(navigationField.
					getPreferredSize().width, 25));

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
			new ImageIcon((new ImageIcon(
				FileExplorer.getIconPath() + "other/magnifyingglass.png"))
						.getImage().getScaledInstance(15, 15, Image.SCALE_DEFAULT)));		

		searchField.setCaretColor(Color.WHITE);
		searchField.setBackground(new Color(30, 30, 30));
		searchField.setForeground(new Color(0, 255, 255));
		searchField.setPreferredSize(new Dimension(searchField.
				getPreferredSize().width, 25));

		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					JTree tree = MainWindow.getTree();
					JPanel folder = MainWindow.getFolder();
					searchQuery = searchField.getText();
					
					tree.requestFocusInWindow();

					DefaultMutableTreeNode node = (DefaultMutableTreeNode) 
						tree.getLastSelectedPathComponent();
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
					    public void removeSelectionInterval(int index0, int index1) {
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

								DefaultMutableTreeNode node = (DefaultMutableTreeNode)
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
	
					Thread thread = new Thread() {
						public void run() {
							search(tree, node, searchQuery, model);
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

				searchField.setText("Search" + " \"" + fileName + "\"");
			}
		});

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.15;
		c.gridx = 3;
		c.gridy = 0;

		this.add(searchField, c);
	}

	static void search(JTree tree, DefaultMutableTreeNode top, String searchQuery, DefaultTableModel model) {
		int numChild=tree.getModel().getChildCount(top);
		DefaultMutableTreeNode current;
		File topFile = (File) top.getUserObject();
		JPanel folder = MainWindow.getFolder();

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
				model.addRow(new Object[] { current });
				folder.repaint();
				folder.revalidate();
			}
			
			File children[] = element.listFiles();
			if(children==null)
				continue;
			for(File child : children) {
				if(child.isFile() && child.getName().contains(searchQuery)) {
					model.addRow(new Object[] { new DefaultMutableTreeNode(child) });
				
					folder.repaint();
					folder.revalidate();
				}
			}

			search(tree, current, searchQuery, model);
		}		  
	}

	public static void historyBack() {
		JTree tree = MainWindow.getTree();
		DefaultMutableTreeNode previous,
						lastTreeNodeOpened = Tree.getLastTreeNodeOpened();

		FolderPanel.clearLastPanelSelection();

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
		MainWindow.enterOrOpen(file, previous);
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
		MainWindow.enterOrOpen(file, next);
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

		JLabel myLabel = new JLabel(folderIconPC);
		myLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
		buttonField.add(myLabel);
	}

	public static void addNavButton(DefaultMutableTreeNode node) {
		File file = (File) node.getUserObject();

		JButton button = new JButton(node.toString());
		button.setFocusPainted(false);
		button.setBackground(new Color(30, 30, 30));
		button.setForeground(new Color(0, 255, 255));
		button.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {
				MainWindow.selectDirectory(node);
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

		buttonField.add(button);
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

		pict = img.getImage().getScaledInstance(35, 35, Image.SCALE_DEFAULT);
		img = new ImageIcon(pict);

		return img;
	}
}