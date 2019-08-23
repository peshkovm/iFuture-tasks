package com.github.peshkovm.gui;

import com.github.peshkovm.core.WorkingWithFilesUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        initializeFileTree();

        folderLocationButton.addActionListener(e -> {
            JFileChooser fileLocationChooser = new JFileChooser();

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

                List<Path> foundFiles = findFilesContainingText(selectedFolder, textToFind, fileExtension);
                fillFileTreeRefined(foundFiles);
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

    private List<Path> findFilesContainingText(final File selectedFolder, final String textToFind,
                                               final String fileExtension) {
        List<Path> foundFiles = null;

        if (selectedFolder.exists()) {
            if (textToFind == null)
                System.out.println("textToFind = null");
            if (("").equals(textToFind))
                System.out.println("textToFind = \"\"");
            else
                System.out.println("textToFind = " + textToFind);


            System.out.println("fileExtension = " + fileExtension);

            foundFiles = WorkingWithFilesUtils.findFilesContainingText(selectedFolder, textToFind, fileExtension);

            System.out.println("\n\n" + selectedFolder.getName());

            //System.out.println("\n\nFound files: ");
            //foundFiles.forEach(file -> System.out.println(file.getAbsolutePath()));
        } else {
            System.out.println("Folder: " + selectedFolder.getAbsolutePath() + " doesn't exists");
            //throw new RuntimeException("Folder: " + selectedFolder.getName() + " doesn't exists");
        }

        return foundFiles;
    }

    private void fillFileTree(final List<Path> foundFiles) {
        if (foundFiles.size() > 0) {
            final Map<String, DefaultMutableTreeNode> fileTreeContentMap = new HashMap<>();
            final String FILE_TREE_ROOT_NAME = "root";

            //add root of file tree
            final DefaultMutableTreeNode fileTreeRootNode = new DefaultMutableTreeNode(FILE_TREE_ROOT_NAME);
            fileTreeContentMap.putIfAbsent(FILE_TREE_ROOT_NAME, fileTreeRootNode);

            //add disk nodes
            //System.out.println("\n\nRoots: ");
            foundFiles.stream()
                    .map(file -> file.getRoot().toString())
                    .forEach(diskName -> {
                        DefaultMutableTreeNode newDiskNode = new DefaultMutableTreeNode(diskName);
                        //DefaultMutableTreeNode fileTreeRootNode = fileTreeContentMap.get(FILE_TREE_ROOT_NAME);

                        if (fileTreeContentMap.putIfAbsent(diskName, newDiskNode) == null)
                            fileTreeRootNode.add(newDiskNode);

                        //System.out.println(diskName);
                    });

            //add leaf nodes
            System.out.println("\n\nLeaf nodes: ");
            foundFiles.stream()
                    .forEach(foundFilePath -> {
                        //treeNodeNameAbsolute = found file root name, for example C:\
                        final StringBuilder nodeNameAbsolute = new StringBuilder(foundFilePath.getRoot().toString());
                        //System.out.println("absolute: " + nodeNameAbsolute);

                        foundFilePath.forEach(nodePathRelative -> {
                            final String nodeNameRelative = nodePathRelative.toString();

                            DefaultMutableTreeNode newLeafNode = new DefaultMutableTreeNode(nodeNameRelative);

                            DefaultMutableTreeNode rootNode = fileTreeContentMap.get(nodeNameAbsolute.toString());

                            nodeNameAbsolute.append(nodeNameRelative);

                            if (fileTreeContentMap.putIfAbsent(nodeNameAbsolute.toString(), newLeafNode) == null)
                                rootNode.add(newLeafNode);

                            //System.out.println("relative: " + nodeNameRelative);
                            //System.out.println("absolute: " + nodeNameAbsolute);
                        });
                    });

            //reload/update fileTree to show new nodes
            //fileTree.setRootVisible(false);
            DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
            model.setRoot(fileTreeRootNode);
            model.reload();
            //fileTree.updateUI();
        }
    }

    private void fillFileTreeRefined(final List<Path> foundFiles) {
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
}
