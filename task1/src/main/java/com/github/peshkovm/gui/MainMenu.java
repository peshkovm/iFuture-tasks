package com.github.peshkovm.gui;

import com.github.peshkovm.core.FileTreeFiller;
import com.github.peshkovm.core.WorkingWithFilesUtils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

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
    private JTextPane fileContentTextPane;
    private JSeparator fileTreeFileContentTextPaneSeparator;
    private JTextField fileExtensionTextField;
    private JButton searchButton;
    private JScrollPane fileTreeScrollPane;


    Map<Path, DefaultMutableTreeNode> fileTreeContentMap;
    HashMap<TreePath, TreePath> fileTreeExpandedPaths;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("MainMenu");
        frame.setContentPane(new MainMenu().formPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
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

/*                try {
                    thread.join();
                    DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
                    model.reload();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }*/

                //foundFiles.forEach(this::addNode);


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
        //fileTree.setRootVisible(false);
    }

    private void fillFileTree(final List<Path> foundFiles) {
        if (foundFiles.size() > 0) {
            final Map<Path, DefaultMutableTreeNode> fileTreeContentMap = new HashMap<>();

            //add root of file tree
            DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
            final DefaultMutableTreeNode fileTreeRootNode = (DefaultMutableTreeNode) model.getRoot();
            fileTreeRootNode.removeAllChildren();

            foundFiles.forEach(foundFile -> {
                //root
                Path rootNodePath = foundFile.getRoot();
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootNodePath);
                if (fileTreeContentMap.putIfAbsent(rootNodePath, rootNode) == null)
                    fileTreeRootNode.add(rootNode);

                //children
                for (int nameIndex = 1; nameIndex <= foundFile.getNameCount(); nameIndex++) {
                    Path nodePath = rootNodePath.resolve(foundFile.subpath(0, nameIndex));

                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nodePath.getFileName());

                    if (fileTreeContentMap.putIfAbsent(nodePath, newNode) == null) {
                        DefaultMutableTreeNode parentNode = fileTreeContentMap.getOrDefault(nodePath.getParent(), fileTreeRootNode);
                        parentNode.add(newNode);
                    }
                }

            });

            java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/any_type.png");
            //if (imgURL != null) {
            Icon imageIcon = new ImageIcon(imgURL);

            DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) fileTree.getCellRenderer();
            renderer.setLeafIcon(imageIcon);

            model.reload();
        }
    }

    private void addNodeToFileTree(final Path foundFile) {
        //add root of file tree
        DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
        final DefaultMutableTreeNode fileTreeRootNode = (DefaultMutableTreeNode) model.getRoot();
        //fileTreeRootNode.removeAllChildren();

        //root
        Path rootNodePath = foundFile.getRoot();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootNodePath);
        if (fileTreeContentMap.putIfAbsent(rootNodePath, rootNode) == null) {
            fileTreeRootNode.add(rootNode);
            //model.reload(rootNode);
            model.reload(fileTreeRootNode);
        }

        //children
        for (int nameIndex = 1; nameIndex <= foundFile.getNameCount(); nameIndex++) {
            Path nodePath = rootNodePath.resolve(foundFile.subpath(0, nameIndex));

            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nodePath.getFileName());

            if (fileTreeContentMap.putIfAbsent(nodePath, newNode) == null) {
                DefaultMutableTreeNode parentNode = fileTreeContentMap.getOrDefault(nodePath.getParent(), fileTreeRootNode);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                parentNode.add(newNode);

                model.reload(parentNode);
                fileTreeExpandedPaths.keySet().forEach(fileTree::expandPath);
            }
        }
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

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            parentNode.add(node);

            model.reload(parentNode);
            fileTreeExpandedPaths.keySet().forEach(fileTree::expandPath);
        }

        return fileTreeContentMap.get(foundFile);
    }
}
