import java.io.*;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.lang.management.ManagementFactory;

public class FilePanel {
	private static final String ICONPATH = FileExplorer.getIconPath();
	private static final Color folderBackgroundColor = FileExplorer.folderBackgroundColor;
	private static final Color panelHoverColor = FileExplorer.panelHoverColor;
	private static final Color topBackgroundColor = FileExplorer.topBackgroundColor;
	public static final Color panelSelectionColor = FileExplorer.panelSelectionColor;

	public static java.util.List<FilePanel> filePanelList = 
		new java.util.ArrayList<FilePanel>();

	public static java.util.List<FilePanel> selectedList = 
		new java.util.ArrayList<FilePanel>();

	private File file;
	private DefaultMutableTreeNode node;
	private JPanel panel;

	public FilePanel(File file, DefaultMutableTreeNode node) {
		this.file = file;
		this.node = node;

		JLabel label;
		String name = this.file.getName(), path="";
		ImageIcon img = Utility.chooseIcon(file, 60);

		if(name.trim().length() == 0) {	
			FileSystemView fsv = FileSystemView.getFileSystemView();

			String description = fsv.getSystemTypeDescription(file);
			name = fsv.getSystemDisplayName(this.file);

			path = ICONPATH + "other/harddiskfolder.png";

			img = ImageHandler.getImageFast(path, 60, 60, true);

			if(description.equals("CD Drive")) {
				path = ICONPATH + "other/cdfolder.png";
				name = description + " (" + this.file.getPath().replace("\\", "") + ")";
			}
			else if(description.equals("DVD Drive")) {
				path = ICONPATH + "other/dvd.png";
				name = description + " (" + this.file.getPath().replace("\\", "") + ")";
			}
			else if(description.equals("USB Drive")) {
				path = ICONPATH + "other/usbfolder.png";			
			}
		}

		this.panel = new JPanel(new BorderLayout());
		
		this.panel.setPreferredSize(new Dimension(150, 120));

		label = new JLabel(img, JLabel.CENTER);
		label.setPreferredSize(new Dimension(60, 60));
		this.panel.add(label,  BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());

		if(!path.equals("")) {
			// right empty space
			label = new JLabel("", JLabel.CENTER);
			label.setPreferredSize(new Dimension(10, 10));
			bottomPanel.add(label,  BorderLayout.EAST);

			// left empty space
			label = new JLabel("", JLabel.CENTER);
			label.setPreferredSize(new Dimension(10, 10));
			bottomPanel.add(label,  BorderLayout.WEST);

			long free = this.file.getFreeSpace();
			long total = this.file.getTotalSpace();
			int used = (int) (((total-free)*100)/total);

			JProgressBar bar = new JProgressBar(0, 100);

			bar.setPreferredSize(new Dimension(80, 17));
			bar.setValue(used);	
			bar.setStringPainted(true);
			bar.setString(Utility.convertBytes(free, false) + " free");

			bar.setBackground(topBackgroundColor);
			if(used>90) {
				bar.setForeground(Color.RED);
				bar.setUI(new BasicProgressBarUI() {
					protected Color getSelectionForeground() { return Color.WHITE; }
				});
			}
			else {
				bar.setForeground(Color.CYAN);
				bar.setUI(new BasicProgressBarUI() {
					protected Color getSelectionForeground() { return Color.BLACK; }
				});
			}

			bar.setBorderPainted(false);

			bottomPanel.add(bar,  BorderLayout.CENTER);
		}

		this.panel.getActionMap().put("select all", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					clearPanelSelection();
					for(FilePanel current : filePanelList) {
						current.selectPanel(false);
					}
				}
			});

		this.panel.getActionMap().put("cut", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					FileExplorer.clipboard.clear();

					for(FilePanel currentPanel : selectedList) 
						FileExplorer.clipboard.add(currentPanel.getNode());

					FileExplorer.operation = "cut";	
				}
			});

		this.panel.getActionMap().put("copy", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					FileExplorer.clipboard.clear();

					for(FilePanel currentPanel : selectedList) 
						FileExplorer.clipboard.add(currentPanel.getNode());

					FileExplorer.operation = "copy";
				}
			});

		this.panel.getActionMap().put("rename", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					if(selectedList.size()==1)
						FileExplorer.rename(node);		
				}
			});

		this.panel.getActionMap().put("refresh", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.refresh(FileExplorer.getLastTreeNodeOpened());
				FileExplorer.focusLast();
			}
		});

		this.panel.getActionMap().put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.delete(node);		
			}
		});

		this.panel.getActionMap().put("select left", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(getLastSelectedPanel());

				for (FilePanel fPanel : filePanelList) {
					if (fPanel.getPanel() == (JPanel) WrapLayout.getComponent(position - 1)) {
						fPanel.selectPanel(true);
						break;
					}
				}
			}
		});

		this.panel.getActionMap().put("select right", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(getLastSelectedPanel());
				for (FilePanel fPanel : filePanelList) {
					if (fPanel.getPanel() == (JPanel) WrapLayout.getComponent(position + 1)) {
						fPanel.selectPanel(true);
						break;
					}
				}
			}
		});

		this.panel.getActionMap().put("select down", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(getLastSelectedPanel());

				for (FilePanel fPanel : filePanelList) {
					if (fPanel.getPanel() == (JPanel) WrapLayout.
						getComponent(position + WrapLayout.getRowLength())) {
						
						fPanel.selectPanel(true);
						break;
					}
				}
			}
		});

		this.panel.getActionMap().put("select up", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				int position = WrapLayout.getIndex(getLastSelectedPanel());

				for (FilePanel fPanel : filePanelList) {
					if (fPanel.getPanel() == (JPanel) WrapLayout.
						getComponent(position - WrapLayout.getRowLength())) {
						
						fPanel.selectPanel(true);
						break;
					}
				}
			}
		});

		this.panel.getActionMap().put("history back", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyBack(); 
				FileExplorer.focusLast();
			}
		});

		this.panel.getActionMap().put("history forward", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				FileExplorer.historyForward(); 
				FileExplorer.focusLast();
			}
		});

		this.panel.getActionMap().put("enter", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(selectedList.size()==1) {
					FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
					FileExplorer.clearFuture();
					if(node!=null)
						FileExplorer.enterOrOpen(node);
					else
						FileExplorer.enterOrOpen(new DefaultMutableTreeNode(file));
					FileExplorer.focusLast();
				}
			}
		});

		InputMap inputMap = this.panel.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "select all");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "cut");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "select left");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "select right");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "select down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "select up");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), "history back");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), "history forward");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");

		label = new JLabel(name, JLabel.CENTER);
		label.setPreferredSize(new Dimension(150, 30));
		label.setForeground (Color.white);
		bottomPanel.add(label, BorderLayout.SOUTH);
		bottomPanel.setBackground(folderBackgroundColor);

		this.panel.add(bottomPanel, BorderLayout.SOUTH);

		this.panel.setName(name);
		this.panel.setBackground(folderBackgroundColor);

		this.panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent event) {
				java.util.List<JPanel> selectedListPanels = 
					new java.util.ArrayList<JPanel>();

				for (FilePanel fPanel : selectedList) {
					selectedListPanels.add(fPanel.getPanel());
				}

				if(!selectedListPanels.contains(panel)) {
					for (int i = 0; i < panel.getComponentCount(); i++) {
						panel.getComponent(i).setBackground(panelHoverColor);
					}
					panel.setBackground(panelHoverColor);
				}
			}
			@Override
			public void mouseExited(MouseEvent event) {
				java.util.List<JPanel> selectedListPanels = 
					new java.util.ArrayList<JPanel>();

				for (FilePanel fPanel : selectedList) {
					selectedListPanels.add(fPanel.getPanel());
				}

				if(!selectedListPanels.contains(panel)) {
					for (int i = 0; i < panel.getComponentCount(); i++) {
						panel.getComponent(i).setBackground(folderBackgroundColor);
					}
					panel.setBackground(folderBackgroundColor);
				}
			}
			@Override
			public void mousePressed(MouseEvent event) {
				java.util.List<JPanel> selectedListPanels = 
					new java.util.ArrayList<JPanel>();

				for (FilePanel fPanel : selectedList) {
					selectedListPanels.add(fPanel.getPanel());
				}

				if(!file.exists()) {
					FileExplorer.findExistingParent(file);
					return;
				}

				FileExplorer.setFocusExplorer();

				/* No control pressed */
				if(!event.isControlDown()) {
					/* Left Mouse Button */

					if(SwingUtilities.isLeftMouseButton(event) || 
						(SwingUtilities.isRightMouseButton(event) 
							&& !selectedListPanels.contains(panel))) {
						
						selectPanel(true);
					}
				}
				else if(event.isControlDown() && SwingUtilities.isLeftMouseButton(event) 
					&& event.getClickCount() == 1) {

					selectPanel(false);
				}

				if(event.getClickCount()%2==0 && event.getButton() == MouseEvent.BUTTON1) {
					FileExplorer.historyPush(FileExplorer.getLastTreeNodeOpened());
					FileExplorer.clearFuture();
					FileExplorer.enterOrOpen(node);

					FileExplorer.focusLast();
				}
				else if(event.getButton() == MouseEvent.BUTTON3) {
					JPopupMenu menu = FileExplorer.getFilePopupMenu(node);
					menu.show(event.getComponent(), event.getX(), event.getY());
				}
			}
		});

		this.panel.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {

			}
			public void focusLost(FocusEvent e) {

			}
		});
	}

	public JPanel getPanel() {
		return this.panel;
	}

	public DefaultMutableTreeNode getNode() {
		return this.node;
	}

	public void selectPanel(Boolean clear) {
		if(clear) {
			clearPanelSelection();
			for (int i = 0; i < this.panel.getComponentCount(); i++) {
				this.panel.getComponent(i).setBackground(panelSelectionColor);
			}
			this.panel.setBackground(panelSelectionColor);
			selectedList.add(this);
		}
		else {
			if(selectedList.contains(this)) {
				for (int i = 0; i < this.panel.getComponentCount(); i++) {
					this.panel.getComponent(i).setBackground(folderBackgroundColor);
				}
				this.panel.setBackground(folderBackgroundColor);

				selectedList.remove(this);
			}
			else
				selectedList.add(this);

			for(FilePanel element : selectedList) {
				for (int i = 0; i < element.getPanel().getComponentCount(); i++) {
					element.getPanel().getComponent(i).setBackground(panelSelectionColor);
				}
				element.getPanel().setBackground(panelSelectionColor);
			}
		}

		this.panel.requestFocusInWindow();
	}

	static public void clearPanelSelection() {
		for(FilePanel element : selectedList) {
			for (int i = 0; i < element.getPanel().getComponentCount(); i++) {
				element.getPanel().getComponent(i).setBackground(folderBackgroundColor);
			}
			element.getPanel().setBackground(folderBackgroundColor);
		}
		selectedList.clear();
	}

	public static JPanel getLastSelectedPanel() {
		if(selectedList.size()>0)
			return selectedList.get(selectedList.size()-1).getPanel();
		else
			return null;
	}

	public static FilePanel findFilePanel(JPanel panel) {
		for (FilePanel fPanel : filePanelList) {
			if (fPanel.getPanel() == panel) {
				return fPanel;
			}
		}
		return null;
	}
}