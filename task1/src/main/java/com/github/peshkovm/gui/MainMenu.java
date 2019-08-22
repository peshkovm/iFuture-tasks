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
    private JLabel folderLocationLabel;
    private JLabel findingTextLabel;
    private JSeparator menuPanelSeparator;
    private JTextPane fileContentTextPane;
    private JSeparator fileTreeFileContentTextPaneSeparator;
    private JTextField fileExtensionTextField;
    private JButton searchButton;

    //my fields
    private File selectedFolder;

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

        folderLocationButton.addActionListener(e -> {
            JFileChooser fileLocationChooser = new JFileChooser();

            fileLocationChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int option = fileLocationChooser.showDialog(null, "Choose folder");

            if (option == JFileChooser.APPROVE_OPTION) {
                selectedFolder = fileLocationChooser.getSelectedFile();
                folderLocationTextField.setText(selectedFolder.toString());
            }
        });

        searchButton.addActionListener(e -> {
            if (selectedFolder != null) {
                String textToFind = findingTextTextField.getText();

                if (textToFind == null)
                    System.out.println("textToFind = null");
                if (("").equals(textToFind))
                    System.out.println("textToFind = \"\"");
                else
                    System.out.println("textToFind = " + textToFind);

                String fileExtension = fileExtensionTextField.getText().substring(1);

                System.out.println("fileExtension = " + fileExtension);

                List<File> foundFiles = WorkingWithFilesUtils.findFilesContainingText(selectedFolder, textToFind, fileExtension);

                System.out.println("\n\n" + selectedFolder.getName());

                System.out.println("\n\nFounded files: ");
                foundFiles.forEach(System.out::println);
            }
        });
    }
}
