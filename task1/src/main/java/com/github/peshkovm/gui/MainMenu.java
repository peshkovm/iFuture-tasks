package com.github.peshkovm.gui;

import com.github.peshkovm.core.FileTreeFiller;
import com.github.peshkovm.core.ReadBigFileTableModel;
import com.github.peshkovm.core.WorkingWithFilesUtils;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

public class MainMenu {
    private JPanel menuPanel;
    private JPanel fileTreePanel;
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
    private JTabbedPane fileContentTabbedPane;
    private JProgressBar fileTreeProgressBar;

    private Map<Path, DefaultMutableTreeNode> fileTreeContentMap;
    private Set<TreePath> fileTreeExpandedPaths;
    private final java.util.List<FileContentTableScrollPane> fileContentTableScrollPaneList = new ArrayList<>();
    private final JFileChooser fileLocationChooser = new JFileChooser();

    static JFrame frame;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("MainMenu");
        frame.setContentPane(new MainMenu().formPanel);
    }

    public MainMenu() {
        createUIComponents();
    }

    private void createUIComponents() {
        initializeFrame();

        initializeFolderLocationButton();

        initializeFileTree();

        initializeFileContentTabbedPane();

        initializeSearchButton();

        initializeFileContentPanel();

        initializeFileTreePanel();
    }

    private void initializeFrame() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 900);
        frame.setMinimumSize(new Dimension(950, 855));
        //frame.setResizable(false);
        //frame.pack();
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Closed");
                fileContentTableScrollPaneList.stream()
                        .map(FileContentTableScrollPane::getFileContentTable)
                        .map(FileContentTable::getModel)
                        .forEach(model -> {
                            model.close();
                            System.out.println("model is closed");
                        });
                e.getWindow().dispose();
            }
        });
    }

    private void initializeSearchButton() {
        searchButton.addActionListener(e -> {

            if (folderLocationTextField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "File location must not be empty");
                return;
            } else if (findingTextTextField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Text to find must not be empty");
                return;
            } else if (fileExtensionTextField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "File extension must not be empty");
                return;
            }/* else if (!fileExtensionTextField.getText().startsWith(".")) {
                JOptionPane.showMessageDialog(frame, "File extension must start with .");
                return;
            }*/

            //fileContentTableModel.close();
            final File selectedFolder = new File(folderLocationTextField.getText());

            if (selectedFolder.exists() && selectedFolder.isDirectory()) {
                final String textToFind = findingTextTextField.getText();
                final String fileExtension = fileExtensionTextField.getText();

                java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/any_type.png");
                if (imgURL != null) {
                    Icon imageIcon = new ImageIcon(imgURL);


                    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) fileTree.getCellRenderer();
                    renderer.setLeafIcon(imageIcon);
                }

                fileTreeContentMap = new HashMap<>();
                fileTreeExpandedPaths = new HashSet<>();

                DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
                ((DefaultMutableTreeNode) model.getRoot()).removeAllChildren();

                FileTreeFiller callback = this::addNodeToFileTreeRefactored;
                Thread thread = new Thread(() -> {
                    fileTreeProgressBar.setIndeterminate(true);
                    WorkingWithFilesUtils.findFilesContainingTextAndLazyFillTree(selectedFolder, textToFind, fileExtension, callback);
                    fileTreeProgressBar.setIndeterminate(false);
                });
                thread.start();

/*                //background task
                new SwingWorker<Void, DefaultMutableTreeNode>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        searchFiles.traverseTree(Paths.get(pathText.getText()), extension.getText().isEmpty() ? "*" : extension.getText(), textToSearch.getText(), path -> publish(updateTree(path, map)));
                        return null;
                    }

                    //edt
                    @Override
                    protected void process(List<DefaultMutableTreeNode> nodes) {
                        for (DefaultMutableTreeNode file : nodes) {
                            ((DefaultTreeModel) tree.getModel()).reload(file);
                        }
                        fileTreeExpandedPaths.forEach(tree::expandPath);
                    }

                    @Override
                    protected void done() {
                        super.done();
                    }
                }.execute();*/

                System.out.println("After fill file tree");
            }
        });
    }

    private void initializeFolderLocationButton() {
        folderLocationButton.addActionListener(e -> {

            fileLocationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int option = fileLocationChooser.showDialog(null, "Choose folder");

            if (option == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileLocationChooser.getSelectedFile();
                folderLocationTextField.setText(selectedFolder.toString());
            }
        });
    }

    private void initializeFileTree() {
        //create the root node
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

        DefaultTreeModel model = (DefaultTreeModel) fileTree.getModel();
        model.setRoot(root);
        fileTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        fileTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                fileTreeExpandedPaths.add(event.getPath());
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

        fileTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                //int selRow = fileTree.getRowForLocation(e.getX(), e.getY());

                TreePath selPath = fileTree.getPathForLocation(e.getX(), e.getY());

                int tabCount = fileContentTabbedPane.getTabCount();

                for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                    if (((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(tabIndex)).getFilePath().equals(selPath)) {
                        fileContentTabbedPane.setSelectedIndex(tabIndex);
                        return;
                    }
                }

                //if(selRow != -1) {
                if (selPath != null) {
                    if (e.getClickCount() == 1) {
                        fileTree.getSelectionModel().clearSelection();
                    } else if (e.getClickCount() == 2) {
                        if (fileTree.getModel().isLeaf(selPath.getLastPathComponent())) {

                            String filePath = selPath.toString().replaceAll("\\]|\\[", "").replaceFirst("Root, ", "").replaceAll(", ", Matcher.quoteReplacement(File.separator));
                            System.out.println("filePath = " + filePath);

                            ReadBigFileTableModel fileContentTableModel = new ReadBigFileTableModel(filePath);
                            FileContentTable fileContentTable = new FileContentTable(fileContentTableModel);

                            FileContentTableScrollPane fileContentTableScrollPane = new FileContentTableScrollPane(fileContentTable);
                            fileContentTableScrollPane.getFileContentTable().getModel().fireTableDataChanged();

                            ButtonTabComponent buttonTabComponent = new ButtonTabComponent(selPath);
                            buttonTabComponent.setBackground(Color.WHITE);

                            fileContentTabbedPane.add(fileContentTableScrollPane);
                            fileContentTabbedPane.setTabComponentAt(fileContentTabbedPane.getTabCount() - 1, buttonTabComponent);
                            fileContentTabbedPane.setSelectedIndex(fileContentTabbedPane.getTabCount() - 1);

                            fileContentTableScrollPaneList.add(fileContentTableScrollPane);

                            for (int i = 0; i < fileContentTableScrollPaneList.size(); i++) {
                                System.out.println("index = " + i + " pane = " + ((ButtonTabComponent) fileContentTabbedPane.getTabComponentAt(i)).getFilePath());
                            }

                            //fileContentTable.changeSelection(100, 1, false, false);
                        }
                    }
                }
                //}
            }
        });

        //fileTree.setRootVisible(false);
    }

    private void initializeFileContentTabbedPane() {
        fileContentTabbedPane.addChangeListener(e -> {
            int tabCount = fileContentTabbedPane.getTabCount();

            System.out.println("tab changed");

            if (tabCount > 0) {
                System.out.println(fileContentTabbedPane.getSelectedIndex());
                int selectedIndex = fileContentTabbedPane.getSelectedIndex();
                if (fileContentTabbedPane.getTabComponentAt(selectedIndex) != null) {
                    fileContentTabbedPane.getTabComponentAt(selectedIndex).setBackground(Color.WHITE);

                    for (int tabIndex = 0; tabIndex < tabCount; tabIndex++) {
                        if (tabIndex != selectedIndex)
                            fileContentTabbedPane.getTabComponentAt(tabIndex).setBackground(null);
                    }
                }
            }
        });
    }

    private void initializeFileTreePanel() {
        fileTreePanel.setPreferredSize(new Dimension(300, -1));
        fileTreePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
    }

    private void initializeFileContentPanel() {
        fileContentPanel.setPreferredSize(new Dimension(600, -1));
        fileContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
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

            parentNode.add(node);

            model.reload(parentNode);
            fileTreeExpandedPaths.forEach(fileTree::expandPath);
        }

        return fileTreeContentMap.get(foundFile);
    }

    private class ButtonTabComponent extends JPanel {
        private final TreePath filePath;

        public ButtonTabComponent(TreePath filePath) {
            this.filePath = filePath;
            String tabText = filePath.getLastPathComponent().toString();
            JLabel label = new JLabel(tabText);
            add(label);
            //add more space between the label and the button
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
            //tab button
            JButton button = new TabButton();
            add(button);
            //add more space to the top of the component
            //setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        }

        private class TabButton extends JButton implements ActionListener {
            TabButton() {
                setContentAreaFilled(false);
                setFocusable(false);
                setBorder(BorderFactory.createEtchedBorder());
                setBorderPainted(false);
                setRolloverEnabled(true);
                int size = 17;
                setPreferredSize(new Dimension(size, size));

                java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/close.png");
                if (imgURL != null) {
                    Icon closeIcon = new ImageIcon(imgURL);
                    setIcon(closeIcon);
                }

                addActionListener(this);

                //paint tab border
                //TODO не работает
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        setBorderPainted(true);
                    }

                    public void mouseExited(MouseEvent e) {
                        setBorderPainted(false);
                    }
                });
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                int tabIndex = fileContentTabbedPane.indexOfTabComponent(ButtonTabComponent.this);
                System.out.println("close button index = " + tabIndex);
                if (tabIndex != -1) {
                    fileContentTableScrollPaneList.get(tabIndex).getFileContentTable().getModel().close();
                    fileContentTabbedPane.remove(tabIndex);
                    fileContentTableScrollPaneList.remove(tabIndex);
                }
            }

        }

        public TreePath getFilePath() {
            return filePath;
        }
    }

    private static class FileContentTableScrollPane extends JScrollPane {
        public FileContentTableScrollPane(FileContentTable fileContentTable) {
            super(fileContentTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            getVerticalScrollBar().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (fileContentTable.getRowCount() > 5000) {
                        System.out.println("Released");
                        fileContentTable.getModel().isMousePressed = false;
                        fileContentTable.getModel().fireTableDataChanged();
                    }
                }
            });
            getVerticalScrollBar().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (fileContentTable.getRowCount() > 5000) {
                        System.out.println("Pressed");
                        fileContentTable.getModel().isMousePressed = true;
                    }
                }
            });

            getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (fileContentTable.getRowCount() > 5000) {
                        System.out.println("Released");
                        fileContentTable.getModel().isMousePressed = false;
                        fileContentTable.getModel().fireTableDataChanged();
                    }
                }
            });
            getHorizontalScrollBar().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (fileContentTable.getRowCount() > 5000) {
                        System.out.println("Pressed");
                        fileContentTable.getModel().isMousePressed = true;
                    }
                }
            });

            //getViewport().setBackground(Color.WHITE);
        }

        FileContentTable getFileContentTable() {
            return (FileContentTable) getViewport().getView();
        }
    }

    private static class FileContentTable extends JTable {
        FileContentTable(ReadBigFileTableModel fileContentTableModel) {
            super(fileContentTableModel);

            setShowGrid(false);
            setIntercellSpacing(new Dimension(5, 0));
            setTableHeader(null);
            //getColumnModel().getColumn(0).setPreferredWidth(50);
            //getColumnModel().getColumn(0).setMaxWidth(50);
            setAutoscrolls(true);
            getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component rendererComp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    rendererComp.setBackground(new Color(228, 228, 228));
                    rendererComp.setForeground(new Color(128, 128, 128));
                    setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
                    setHorizontalAlignment(JLabel.CENTER);
                    return rendererComp;
                }
            });
            setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            adjustColumnsSize();
        }

        private void adjustColumnsSize() {
            for (int colIndex = 0; colIndex < 2; colIndex++) {
                TableColumn tableColumn = getColumnModel().getColumn(colIndex);
                int preferredWidth = tableColumn.getMinWidth();
                int row;

                if (colIndex == 0)
                    row = getModel().getRowCount() - 1;
                else
                    row = getModel().getNumOfLongestRow();

                System.out.println("longest row = " + row);

                TableCellRenderer cellRenderer = getCellRenderer(row, colIndex);
                Component c = prepareRenderer(cellRenderer, row, colIndex);
                int width = c.getPreferredSize().width + getIntercellSpacing().width;
                preferredWidth = Math.max(preferredWidth, width);

                tableColumn.setPreferredWidth(preferredWidth);
            }
        }

        @Override
        public ReadBigFileTableModel getModel() {
            return (ReadBigFileTableModel) super.getModel();
        }
    }
}
