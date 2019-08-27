package com.github.peshkovm.core;

import com.eaio.stringsearch.BoyerMooreHorspool;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class WorkingWithFilesUtils {

    private WorkingWithFilesUtils() {

    }

    public static List<Path> findFilesContainingText(final File directory, final String textToFind, final String fileExtension) throws RuntimeException {
        final Path directoryPath = directory.toPath();

        final List<Path> foundFiles = new ArrayList<>();
        final BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();

        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    try {
                        //System.out.println("\n visited file = " + file.getFileName() + "\n");

                        final String extension = FilenameUtils.getExtension(file.toString());

                        if (file.toFile().isFile() && extension.equals(fileExtension)) {

                            //System.out.println("\n visited file = " + file.getFileName() + "\n");

                            byte[] fileContent = Files.readAllBytes(file);

                            if (boyerMooreHorspool.searchBytes(fileContent, textToFind.getBytes()) != -1) {
                                foundFiles.add(file);
                                //return FileVisitResult.TERMINATE;
                            }
                        }

                    } catch (Exception e) {

                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }

        return foundFiles;
    }

    public static void findFilesContainingTextAndLazyFillTree(final File directory, final String textToFind, final String fileExtension, final Consumer<Path> consumer) throws RuntimeException {
        final Path directoryPath = directory.toPath();

        final BoyerMooreHorspool boyerMooreHorspool = new BoyerMooreHorspool();

        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path filePath, final BasicFileAttributes attrs) throws IOException {

                    try {
                        //System.out.println("\n visited file = " + file.getFileName() + "\n");

                        final String extension = FilenameUtils.getExtension(filePath.toString());

                        if (filePath.toFile().isFile() && extension.equals(fileExtension)) {

                            //System.out.println("\n visited file = " + file.getFileName() + "\n");

                            /*if (fileContent.length > 10 && SystemTray.isSupported()) {
                                SystemTray systemTray = SystemTray.getSystemTray();
                                java.net.URL imgURL = getClass().getClassLoader().getResource("gui-icons/warning.png");

                                if (imgURL != null) {
                                    Image icon = Toolkit.getDefaultToolkit().getImage(imgURL);
                                    TrayIcon trayIcon = new TrayIcon(icon, "Предупреждение от iFuture task 1");
                                    trayIcon.setImageAutoSize(true);
                                    systemTray.add(trayIcon);
                                    System.out.println("Before tray");
                                    trayIcon.displayMessage("Файл слишком большой", filePath.toString(), TrayIcon.MessageType.INFO);
                                    System.out.println("After tray");
                                }

                                //return FileVisitResult.CONTINUE;
                            } else
                                System.out.println("SystemTray is not supported");*/

                            try (FileChannel fileChannel = FileChannel.open(filePath)) {
                                if (fileChannel.size() > Integer.MAX_VALUE) {
                                    System.out.println("\nФайл слишком большой: " + filePath);

                                    return FileVisitResult.CONTINUE;
                                }

                                byte[] fileContent = new byte[(int) fileChannel.size()];
                                MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, fileChannel.size());
                                mappedByteBuffer.get(fileContent);

                                int position = boyerMooreHorspool.searchBytes(fileContent, textToFind.getBytes());
                                if (position != -1) {
                                    consumer.accept(filePath);
                                    //return FileVisitResult.TERMINATE;
                                }
                            }
                        }

                    } catch (Exception e) {

                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }
}