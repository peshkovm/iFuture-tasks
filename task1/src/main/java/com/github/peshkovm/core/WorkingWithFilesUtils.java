package com.github.peshkovm.core;

import com.eaio.stringsearch.BoyerMooreHorspool;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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

    public static void findFilesContainingTextAndLazyFillTree(final File directory, final String textToFind, final String fileExtension, final FileTreeFiller callback) throws RuntimeException {
        final Path directoryPath = directory.toPath();

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
                                callback.addNode(file);
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
    }
}