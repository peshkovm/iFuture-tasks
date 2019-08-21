package com.github.peshkovm.gui;

import com.github.peshkovm.core.WorkingWithFilesUtils;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class MainMenu {
    private JPanel menuPanel;
    private JPanel treePanel;
    private JPanel fileContentPanel;
    private JPanel formPanel;
    private JTree fileTree;
    private JTextField folderLocationTextField;
    private JTextField findingTextTextField;
    private JButton folderLocationButton;
    private JButton findingTextButton;
    private JLabel folderLocationLabel;
    private JLabel findingTextLabel;
    private JSeparator menuPanelSeparator;
    private JTextPane fileContentTextPane;
    private JSeparator fileTreeFileContentTextPaneSeparator;

    public MainMenu() {
        folderLocationButton.addActionListener(e -> {
            JFileChooser fileLocationChooser = new JFileChooser();

            fileLocationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = fileLocationChooser.showDialog(null, "Choose folder");

            if (ret == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileLocationChooser.getSelectedFile();

                List<File> foundFiles = WorkingWithFilesUtils.findFilesContainingText(selectedFolder, "public");

                System.out.println(selectedFolder.getName());

                foundFiles.forEach(System.out::println);
            }
        });

        findingTextButton.addActionListener(e -> {
            JFileChooser findingTextLocation = new JFileChooser();
            findingTextLocation.showDialog(null, "Choose text");
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainMenu");
        frame.setContentPane(new MainMenu().formPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);
        //frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
