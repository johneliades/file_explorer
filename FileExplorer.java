import java.io.File;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;

public class FileExplorer extends JPanel
                        implements TreeSelectionListener {
    private static JPanel folder;
    private static JTree tree;
    private static final String ICONPATH="./icons/"; // path-until-src/src/hw4/icons/
    private static JTextField search_field, navigation_field;
    
    //Optionally set the look and feel.
    private static final boolean useSystemLookAndFeel = false;
    private static final boolean showHiddenFiles = false;
    static Set<String> iconSet=addExtensions();
    static JPanel lastPanelSelected; 
    static String searchQuery = "";

    public FileExplorer() {
        super(new GridLayout(1, 0));

        //Create the nodes.
        File roots[]=File.listRoots();
        
        DefaultMutableTreeNode top;
        if(roots.length==1)
            top = new DefaultMutableTreeNode(roots[0]);
        else {
            top = new DefaultMutableTreeNode(new MyFile("This PC"));
            for (File root : roots) {
                top.add(new DefaultMutableTreeNode(root));
            }
        }

        //Create a tree that allows one selection at a time.
        tree = new JTree(top);
        tree.setBorder(new EmptyBorder(5, 10, 5, 0)); //top,left,bottom,right

        tree.setEditable(true);
        tree.setCellRenderer(getRenderer());
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        //Listen for when the selection changes.
        tree.addTreeSelectionListener(this);
        tree.addTreeWillExpandListener(treeWillExpandListener);

        tree.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                if(node==null)
                    return;

                showCurrentDirectory(node);
            }
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
        });

        //Create the scroll pane and add the tree to it. 
        JScrollPane treeView = new JScrollPane(tree);

        //Create the folder viewing pane.
        folder = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
		folder.setBackground(Color.white);

        folder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if(event.getButton() == MouseEvent.BUTTON1) {
                    if(lastPanelSelected!=null) {
                        lastPanelSelected.setBackground(Color.white);
                        lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
                        lastPanelSelected=null;
                    }
                }
                else if(event.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu menu = getBackgroundPopupMenu();

                    if(tree.getLastSelectedPathComponent()!=null)
                        menu.show(event.getComponent(), event.getX(), event.getY());

                    if(lastPanelSelected!=null) {
                        lastPanelSelected.setBackground(Color.white);
                        lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
                        lastPanelSelected=null;
                    }
                }
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

        JScrollPane folderView = new JScrollPane(folder);
        folderView.getVerticalScrollBar().setUnitIncrement(16);

        if(roots.length==1)
            createNodes(top, 0);
        else {
            int numChild=tree.getModel().getChildCount(top);
            for(int i=0; i<numChild; i++) { 
                DefaultMutableTreeNode current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
                createNodes(current, 0);
            }
        }
        TreePath path = new TreePath(top.getPath());
        tree.expandPath(path);
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        showCurrentDirectory(top);
        
        folderView.setMinimumSize(new Dimension(400, 50));
        treeView.setMinimumSize(new Dimension(200, 50));

        //Add the scroll panes to a split pane.
        JSplitPaneWithZeroSizeDivider splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPaneWithZeroSizeDivider.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(folderView);

        //Add the split pane to this panel.
        add(splitPane);
    }

    static void showCurrentDirectory(DefaultMutableTreeNode node) {
        int numChild=tree.getModel().getChildCount(node);
        SortedSet<File> set2;
        Iterator it;
        DefaultMutableTreeNode currentNode;
        File currentFile;
        
        String FileName;
        
        FileName = ((File) node.getUserObject()).getName();
        
        if(FileName.isEmpty())
            FileName = ((File) node.getUserObject()).getPath();
        
        navigation_field.setText(" " + ((File) node.getUserObject()).getPath());
        search_field.setText(" Search" + " \"" + FileName + "\"");

        folder.removeAll();

        final class NodeInfo implements Comparable {
            public DefaultMutableTreeNode node;
            public File file;
        
            NodeInfo(DefaultMutableTreeNode node, File file) {
                this.node=node;
                this.file=file;
            }

            @Override
            public int compareTo(Object obj) {
                NodeInfo emp = (NodeInfo) obj;
               
                return file.compareTo(emp.file);
            }
        }

        SortedSet<NodeInfo> set1 = new TreeSet<>();

        for(int i=0; i<numChild; i++) { 
            currentNode = (DefaultMutableTreeNode) tree.getModel().getChild(node, i);
            currentFile =(File) currentNode.getUserObject();
            
            NodeInfo current = new NodeInfo(currentNode, currentFile);

            set1.add(current);
        }

        it=set1.iterator();

        while (it.hasNext()) {
            NodeInfo current = (NodeInfo) it.next();
            currentNode = current.node;
            currentFile = current.file;

            if(showHiddenFiles ?  true : !currentFile.isHidden() || !currentFile.getName().startsWith(".")) {
                if (currentFile.isDirectory())
                    folder.add(getIcon("folder.png", currentFile, currentNode));
                else if(iconSet.contains(getExtension(currentFile.getName())))
                    folder.add(getIcon(getExtension(currentFile.getName()) + ".png", currentFile, currentNode));
                else
                    folder.add(getIcon("question.png", currentFile, currentNode));
            }
        }
        
        folder.repaint();
        folder.revalidate();

        set2 = new TreeSet<>();
        currentFile=(File) node.getUserObject();
        File children[] = currentFile.listFiles();

        if(children==null)
            return;

        set2.addAll(Arrays.asList(children));

        it = set2.iterator();
        while (it.hasNext()) {
            File element = (File) it.next();
  
            if(element.isFile()) {
                if(showHiddenFiles ?  true : !element.isHidden() || !element.getName().startsWith(".")) {
                    if (element.isDirectory())
                        folder.add(getIcon("folder.png", element, null));
                    else if(iconSet.contains(getExtension(element.getName())))
                        folder.add(getIcon(getExtension(element.getName()) + ".png", element, null));
                    else
                        folder.add(getIcon("question.png", element, null));
                }
            }
        }

		folder.repaint();
		folder.revalidate();
    }

    static String getExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p) {
            extension = fileName.substring(i+1);
        }

        return extension;
    }

    static Set<String> addExtensions() {
        Set<String> set = new HashSet<>(); 
                
        set.add("audio"); 
        set.add("bmp"); 
        set.add("css"); 
        set.add("doc"); 
        set.add("docx"); 
        set.add("giff"); 
        set.add("gz"); 
        set.add("htm"); 
        set.add("html"); 
        set.add("image"); 
        set.add("iso"); 
        set.add("jpeg"); 
        set.add("jpg"); 
        set.add("json"); 
        set.add("mp3"); 
        set.add("ods"); 
        set.add("odt"); 
        set.add("ogg"); 
        set.add("pdf"); 
        set.add("png"); 
        set.add("ppt"); 
        set.add("tar"); 
        set.add("tgz"); 
        set.add("txt"); 
        set.add("video"); 
        set.add("wav"); 
        set.add("xlsx"); 
        set.add("xlx"); 
        set.add("xml"); 
        set.add("zip"); 

        return set;
    }

    static JPanel getIcon(String iconName, File file, DefaultMutableTreeNode node) {
        JLabel label;
        ImageIcon img;
        Set<String> set = new HashSet<>(); 
        String name = file.getName();
        
        if(name.trim().length() == 0)
        	name = "Local Disk(" + file.getPath().replace("\\", "") + ")";

        img = new ImageIcon(ICONPATH + iconName);

        // Bad check for images
        set.add("jpeg");
        set.add("jpg");
        set.add("png");
        set.add("bmp");
        if(set.contains(getExtension(file.getName()))) {
        	img = new ImageIcon(file.getPath());
        }

        Image folderImg = img.getImage().getScaledInstance(60, 60, Image.SCALE_DEFAULT);
        img = new ImageIcon(folderImg);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        label = new JLabel("", JLabel.CENTER);
        panel.add(label,  BorderLayout.NORTH);
        label.setPreferredSize(new Dimension(150, 10));

        label = new JLabel(img, JLabel.CENTER);
        panel.add(label,  BorderLayout.CENTER);

        label = new JLabel(name, JLabel.CENTER);
        label.setPreferredSize(new Dimension(130, 30));
        panel.add(label, BorderLayout.SOUTH);
        label.setName(name);
        panel.setName(name);
        panel.setBorder(BorderFactory.createLineBorder(Color.white));
        panel.setBackground(Color.white);

        panel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent event) {

                if(lastPanelSelected!=null) {
                    lastPanelSelected.setBackground(Color.white);
                    lastPanelSelected.setBorder(BorderFactory.createLineBorder(Color.white));
                }
                panel.setBackground(new Color(135, 206, 255, 200));
                panel.setBorder(BorderFactory.createLineBorder(Color.black));
                lastPanelSelected=panel;

                if(event.getClickCount() == 2 && event.getButton() == MouseEvent.BUTTON1) {
                    if(file.isDirectory()) {
                        TreePath path = new TreePath(node.getPath());
                        tree.expandPath(path);
                        tree.setSelectionPath(path);
                        tree.scrollPathToVisible(path);
                        showCurrentDirectory(node);
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
                    JPopupMenu menu = getFilePopupMenu();

                    menu.show(event.getComponent(), event.getX(), event.getY());
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
            	panel.setBackground(new Color(135, 206, 255, 120));
            }
            @Override
            public void mouseExited(MouseEvent event) {
            	if(lastPanelSelected!=panel)
            		panel.setBackground(Color.white);
            }
            @Override
            public void mousePressed(MouseEvent event) {}
            @Override
            public void mouseReleased(MouseEvent event) {}
        });

        return panel;
    }

    TreeWillExpandListener treeWillExpandListener = new TreeWillExpandListener() {
        @Override
        public void treeWillCollapse(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
            TreePath path = treeExpansionEvent.getPath();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        }
  
        @Override
        public void treeWillExpand(TreeExpansionEvent treeExpansionEvent) throws ExpandVetoException {
            TreePath path = treeExpansionEvent.getPath();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            File current;

            String data = node.getUserObject().toString();

            current = (File) node.getUserObject();
            if (current.isDirectory()) {
                createNodes(node, 0);
            }
        }
    };

 /** Required by TreeSelectionListener interface. */
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
        File current;
        
        if (node == null) 
            return;

        current = (File) node.getUserObject();
        if (current.isDirectory()) {
            createNodes(node, 0);
        }
        else {

        }
    }

    static private void createNodes(DefaultMutableTreeNode top, int setting) {
        DefaultMutableTreeNode file = null;
        int i, numChild;
        SortedSet<File> set = new TreeSet<>();
        Iterator it;

        File curDir = (File) top.getUserObject();
        File current, children[] = curDir.listFiles(); 

        numChild=tree.getModel().getChildCount(top);

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
                current=(File) ((DefaultMutableTreeNode) tree.getModel().getChild(top, i)).getUserObject();
                if(current.getName().compareTo(element.getName())==0) {
                    file=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
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

    private DefaultTreeCellRenderer getRenderer() {
        DefaultTreeCellRenderer tRenderer = new DefaultTreeCellRenderer();

        ImageIcon folderIcon = new ImageIcon(ICONPATH + "folder.png");
        Image folderImg = folderIcon.getImage().getScaledInstance(28, 28, Image.SCALE_DEFAULT);
        folderIcon = new ImageIcon(folderImg);

        tRenderer.setLeafIcon(folderIcon);
        tRenderer.setClosedIcon(folderIcon);
        tRenderer.setOpenIcon(folderIcon);
        tRenderer.setTextSelectionColor(Color.RED);

        return tRenderer;
    }

    public static JMenuBar CreateMenuBar() {
        JMenuBar bar=new JMenuBar();
        JLabel label = new JLabel();
        ImageIcon img;
        Image pict;
   
		label.setBorder(new EmptyBorder(5,10,0,0)); //top,left,bottom,right

        img = new ImageIcon(ICONPATH + "folder.png");
        pict = img.getImage().getScaledInstance(35, 35, Image.SCALE_DEFAULT);
        img = new ImageIcon(pict);
 
        label.setIcon(img);
        bar.add(label);

        return bar;
    }
    
    public static JPanel create_top_panel() {
        JPanel top_panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
		JButton button;

		c.weightx = 0.05;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;

        button = new JButton("filler");
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 25));
        top_panel.add(button, c);

		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;

        navigation_field = new JTextField("");
        navigation_field.setPreferredSize(new Dimension(navigation_field.getPreferredSize().width, 25));

        top_panel.add(navigation_field, c);

        search_field = new JTextField("");
        search_field.setPreferredSize(new Dimension(search_field.getPreferredSize().width, 25));
        search_field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                searchQuery += e.getKeyChar();                    
            }
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    File top = (File) node.getUserObject();

                    folder.removeAll();
                    folder.repaint();
                    folder.revalidate();

                    search_field.setText("");
                    JPanel gridPanel = new JPanel(new GridLayout(0, 1, 8, 8));
                    gridPanel.setBackground(Color.white);
                    folder.add(gridPanel);
                    search(node, searchQuery, gridPanel);
                    searchQuery="";
                    folder.repaint();
                    folder.revalidate();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
        });
   
   		search_field.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent event) {

                if(event.getButton() == MouseEvent.BUTTON1) {
                    search_field.setText("");
                    searchQuery="";
                }
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

        search_field.addFocusListener(new FocusListener() {
		    String last_text="";

			@Override
		    public void focusGained(FocusEvent e) {
                last_text = search_field.getText();
                search_field.setText("");
                searchQuery = "";
            }
		    public void focusLost(FocusEvent e) {
        		search_field.setText(last_text);
		    }
		});

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.15;
		c.gridx = 2;
		c.gridy = 0;

        top_panel.add(search_field, c);

        return top_panel;
    }

    static public JPopupMenu getFilePopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItem;

        menuItem = new JMenuItem("Delete");
        popupMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                delete(node);

                showCurrentDirectory(node);
            }
        });

        menuItem = new JMenuItem("Rename");
        popupMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Component curComponents[] = lastPanelSelected.getComponents();
                String FilePath = ((File) ((DefaultMutableTreeNode) tree.getLastSelectedPathComponent()).getUserObject()).getPath();
                DefaultMutableTreeNode current=null, parent= (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                String nameOld = curComponents[2].getName(), nameNew;
                int i;
        
                File f = new File(FilePath + "/" + nameOld);

                if(f.exists() && f.isFile()) {
                    nameNew=JOptionPane.showInputDialog(null, "Enter new name");
                    File file2 = new File(FilePath + "/" + nameNew);

                    if (file2.exists()) {
                        JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
                    }

                    boolean success = f.renameTo(file2);

                    if (!success) {
                        JOptionPane.showMessageDialog(null, "Rename Failed!");
                    }
                }
                else if(f.exists() && f.isDirectory()){
                    nameNew=JOptionPane.showInputDialog(null, "Enter new name");
                    File file2 = new File(FilePath + "/" + nameNew);

                    if (file2.exists()) {
                        JOptionPane.showMessageDialog(null, "Rename Failed! File exists");
                    }

                    int numChild=tree.getModel().getChildCount(parent);

                    for(i=0; i<numChild; i++) { 
                        current=(DefaultMutableTreeNode) tree.getModel().getChild(parent, i);
                        File curFile=(File) (current).getUserObject();
                        if(curFile.getName().compareTo(nameOld)==0)
                            break;
                    }

                    boolean success = f.renameTo(file2);

                    if (!success) {
                        JOptionPane.showMessageDialog(null, "Rename Failed!");
                    }

                    current.removeFromParent();
                    DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();    
                    defMod1.reload();
                    TreePath path = new TreePath(parent.getPath());
                    tree.expandPath(path);
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);     
                }
                else {
                    JOptionPane.showMessageDialog(null, "Rename Failed!");
                    return;
                }
    
                showCurrentDirectory(parent);
            }
        });
        
        return popupMenu;
    }

    static void search(DefaultMutableTreeNode top, String searchQuery, JPanel gridPanel) {
        int numChild=tree.getModel().getChildCount(top);
        DefaultMutableTreeNode current;
        File top_file = (File) top.getUserObject();

        if(numChild==0)
            return; 

        boolean isSymbolicLink = Files.isSymbolicLink(top_file.toPath());
		if(isSymbolicLink)
			return;

        createNodes(top, 0);

        for(int i=0; i<numChild; i++) {      
            current=(DefaultMutableTreeNode) tree.getModel().getChild(top, i);
            File element = (File) current.getUserObject();

            if(element.getName().contains(searchQuery)) {
                gridPanel.add(getSmallIcon("folder.png", element, current));
            }
            
            File children[] = element.listFiles();
            if(children==null)
                continue;
            for(File child : children) {
                if(child.isFile() && child.getName().contains(searchQuery)) {
                    if(iconSet.contains(getExtension(child.getName())))
                        gridPanel.add(getSmallIcon(getExtension(child.getName()) + ".png", child, current));
                    else
                        gridPanel.add(getSmallIcon("question.png", child, current));
                } 
            }

            search(current, searchQuery, gridPanel);
        }          
    }

    public static JLabel getSmallIcon(String name, File file, DefaultMutableTreeNode node) {
        JLabel label = new JLabel();
        ImageIcon img;
        Image pict;
        Set<String> set = new HashSet<>(); 

        img = new ImageIcon(ICONPATH + name);
     
 		// Bad check for images
        set.add("jpeg");
        set.add("jpg");
        set.add("png");
        set.add("bmp");
        if(set.contains(getExtension(file.getName()))) {
        	img = new ImageIcon(file.getPath());
        }

        pict = img.getImage().getScaledInstance(35, 35, Image.SCALE_DEFAULT);
        img = new ImageIcon(pict);
 		
        label.setIcon(img);
        label.setText(file.getPath());
        label.addMouseListener(new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent arg0) {
                String fullPath = label.getText();
                File file = new File(fullPath);

                if(file.isDirectory()) {
                    TreePath path = new TreePath(node.getPath());
                    tree.expandPath(path);
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    showCurrentDirectory(node); 
                }
                else {
                    try {
                        Desktop.getDesktop().open(file);
                    }
                    catch(IOException e) {

                    }
                }
            }
            @Override
            public void mouseReleased(MouseEvent arg0) {}
            @Override
            public void mousePressed(MouseEvent arg0) {}
            @Override
            public void mouseExited(MouseEvent arg0) {}
            @Override
            public void mouseEntered(MouseEvent arg0) {}
        });
        
        return label;
    }

    static void delete(DefaultMutableTreeNode node) {
        Component curComponents[] = lastPanelSelected.getComponents();
        String FilePath = ((File) node.getUserObject()).getPath();
        DefaultMutableTreeNode current=null;
        String name = curComponents[2].getName();
        int i;

        File f = new File(FilePath + "/" + name);
        if(f.exists() && f.isFile()){
            try {
                f.delete();
            }
            catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
        else if(f.exists() && f.isDirectory()) {
            int numChild=tree.getModel().getChildCount(node);
            for(i=0; i<numChild; i++) {
                current=(DefaultMutableTreeNode) tree.getModel().getChild(node, i);
                if(((File) current.getUserObject()).getName().compareTo(name)==0)
                    break;
            }
            if(current==null || i==numChild || ((File) current.getUserObject()).exists()==false)
                return;

            current.removeAllChildren();
            current.removeFromParent();
            removeDirectory(f);
            f.delete(); 

            DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();    
            defMod1.reload();
            TreePath path = new TreePath(node.getPath());
            tree.expandPath(path);
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
        else {
            JOptionPane.showMessageDialog(null, "File didn't exist!");
            return;
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

    public JPopupMenu getBackgroundPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItem;

        menuItem = new JMenuItem("New Text File");
        popupMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                String FilePath = ((File) node.getUserObject()).getPath();
                String name;

                name=JOptionPane.showInputDialog(null, "Enter file name");

                File f = new File(FilePath + "/" + name);
                if(!f.exists()){
                    try {
                        f.createNewFile();
                    }
                    catch(IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
                else {
                    JOptionPane.showMessageDialog(null, "File with that name already exists!");
                    return;
                }

                showCurrentDirectory(node);
            }
        });

        menuItem = new JMenuItem("New Directory");
        popupMenu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                String FilePath = ((File) node.getUserObject()).getPath();
                String name;
                
                name=JOptionPane.showInputDialog(null, "Enter directory name");

                File f = new File(FilePath + "/" + name);
                if(!f.exists()){
                    try {
                        f.mkdir();
                    }
                    catch(Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                else {
                    JOptionPane.showMessageDialog(null, "Directory with that name already exists!");
                    return;
                }

                /* add node to tree and reload tree here */
                DefaultTreeModel defMod1 = (DefaultTreeModel) tree.getModel();    
                defMod1.reload();
                TreePath path = new TreePath(node.getPath());
                tree.expandPath(path);
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
    
                showCurrentDirectory(node);
            }
        });
    
        return popupMenu;
    }
    
    /**
     * Create the GUI and show it.    For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }

        //Create and set up the window.
        JFrame frame = new JFrame("File Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Gets screen's Dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int windowHeight=(int) screenSize.getHeight()*7/8;
        int windowWidth=(int) screenSize.getWidth()*7/8;

        //Set Window's dimensions
        frame.setSize(windowWidth, windowHeight);

        //Set Window's location
        frame.setLocation((screenSize.width-windowWidth)/2, (screenSize.height-windowHeight)/2);

        //Set window layout manager
        frame.setLayout(new BorderLayout());

        //Set Menu Bar
        frame.setJMenuBar(CreateMenuBar());

        //Add content to the window.
        frame.add(create_top_panel(), BorderLayout.NORTH);
        frame.add(new FileExplorer(), BorderLayout.CENTER);
    
        //Display the window.
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
}

class MyFile extends File {
    
    MyFile(String filename) {
        super(filename);
    }

    @Override
    public String toString() {
        if(this.getPath().compareTo("/")!=0)
            return this.getName();
        else
            return this.getPath();
    }
}