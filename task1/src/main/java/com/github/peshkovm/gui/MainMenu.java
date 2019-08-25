package com.github.peshkovm.gui;

import com.github.peshkovm.core.FileTreeFiller;
import com.github.peshkovm.core.LogViewTableModel;
import com.github.peshkovm.core.WorkingWithFilesUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;

public class MainMenu {
    private JPanel menuPanel;
    private JPanel treePanel;
    private JPanel fileContentPanel;
    private JPanel formPanel;
    private JTree fileTree;
    private JTextField folderLocationTextField;
    private JTextField findingTextTextField;
    private JButton folderLocationButton;
    private JLabel folderLocationLabel;
    private JLabel findingTextLabel;
    private JSeparator menuPanelSeparator;
    private JSeparator fileTreeFileContentTextPaneSeparator;
    private JTextField fileExtensionTextField;
    private JButton searchButton;
    private JScrollPane fileTreeScrollPane;
    private JTable fileContentTable;
    private JScrollPane fileContentTableScrollPane;


    Map<Path, DefaultMutableTreeNode> fileTreeContentMap;
    HashMap<TreePath, TreePath> fileTreeExpandedPaths;
    LogViewTableModel fileContentTableModel = new LogViewTableModel();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("MainMenu");
        frame.setContentPane(new MainMenu().formPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 900);
        //frame.pack();
        frame.setVisible(true);
    }

    public MainMenu() {
        createUIComponents();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        JFileChooser fileLocationChooser = new JFileChooser();

        initializeFileTree();

        initializeFileContentTable();

        folderLocationButton.addActionListener(e -> {

            fileLocationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int option = fileLocationChooser.showDialog(null, "Choose folder");

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileLocationChooser.getSelectedFile();
                folderLocationTextField.setText(selectedFolder.toString());
            }
        });

        searchButton.addActionListener(e -> {
            final File selectedFolder = new File(folderLocationTextField.getText());

            if (selectedFolder.exists() && selectedFolder.isDirectory()) {
                final String textToFind = findingTextTextField.getText();
                final String fileExtension = fileExtensionTextField.getText().substring(1);

                java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/any_type.png");
                //if (imgURL != null) {
                Icon imageIcon = new ImageIcon(imgURL);

                DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) fileTree.getCellRenderer();
                renderer.setLeafIcon(imageIcon);

                fileTreeContentMap = new HashMap<>();
                fileTreeExpandedPaths = new HashMap<>();

                DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
                ((DefaultMutableTreeNode) model.getRoot()).removeAllChildren();

                FileTreeFiller callback = this::addNodeToFileTreeRefactored;
                Thread thread = new Thread(() -> WorkingWithFilesUtils.findFilesContainingTextAndLazyFillTree(selectedFolder, textToFind, fileExtension, callback));
                thread.start();

                System.out.println("After fill file tree");
            }
        });

        fileTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                fileTreeExpandedPaths.putIfAbsent(event.getPath(), event.getPath());
                //System.out.println("expanded path:" + event.getPath().getLastPathComponent());
                //fileTreeExpandedPaths.keySet().forEach(System.out::println);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                fileTreeExpandedPaths.remove(event.getPath());
                //System.out.println("collapsed path:" + event.getPath().getLastPathComponent());
                //fileTreeExpandedPaths.keySet().forEach(System.out::println);
            }
        });
    }

    private void initializeFileTree() {
        //create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
        model.setRoot(root);
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        //fileTree.setRootVisible(false);
    }

    private void initializeFileContentTable() {
        fileContentTable.setModel(fileContentTableModel);

        MouseListener ml = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                //int selRow = fileTree.getRowForLocation(e.getX(), e.getY());

                TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());
                //if(selRow != -1) {
                if (e.getClickCount() == 1) {
                    fileTree.getSelectionModel().clearSelection();
                } else if (e.getClickCount() == 2) {
                    if (fileTree.getModel().isLeaf(selPath.getLastPathComponent())) {
                        //System.out.println(selPath);
                        //System.out.println("path root: " + selPath.getPathComponent(0));

                        //TODO не убирать пробелы в названии файла
                        String filePath = selPath.toString().replaceAll("]| |\\[|", "").replaceFirst("Root,", "").replaceAll(",", Matcher.quoteReplacement(File.separator));
                        System.out.println("filePath = " + filePath);

                        try {
                            fileContentTableModel.setFileFromReading(filePath);
                            //fileContentTable.updateUI();

                            //TODO сделать вместо updateUI
                            ((AbstractTableModel) fileContentTable.getModel()).fireTableDataChanged();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                //}
            }
        };

        fileTree.addMouseListener(ml);

        fileContentTable.setShowGrid(false);
        fileContentTable.setIntercellSpacing(new Dimension(5, 0));
        fileContentTable.setTableHeader(null);
        fileContentTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        fileContentTable.getColumnModel().getColumn(0).setMaxWidth(50);
        fileContentTable.setAutoscrolls(true);
        fileContentTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component rendererComp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                rendererComp.setBackground(new Color(228, 228, 228));
                rendererComp.setForeground(new Color(128, 128, 128));
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
                setHorizontalAlignment(JLabel.CENTER);
                return rendererComp;
            }
        });
        fileContentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private DefaultMutableTreeNode addNodeToFileTreeRefactored(final Path foundFile) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(foundFile.getNameCount() == 0 ? foundFile : foundFile.getFileName());

        if (fileTreeContentMap.putIfAbsent(foundFile, node) == null) {
            DefaultMutableTreeNode parentNode;
            DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();

            if (foundFile.getParent() != null)
                parentNode = addNodeToFileTreeRefactored(foundFile.getParent());
            else {
                //Root of file tree
                parentNode = (DefaultMutableTreeNode) model.getRoot();
            }

/*            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            parentNode.add(node);

            model.reload(parentNode);
            fileTreeExpandedPaths.keySet().forEach(fileTree::expandPath);
        }

        return fileTreeContentMap.get(foundFile);
    }
}
