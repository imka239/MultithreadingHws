package ru.ifmo.rain.gnatyuk.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class providing directories management utilities for {@link Implementor}
 *
 * @author gnatyuk
 * @version 1.0
 */

public class ImplementorUtils {

    /**
     * {@link Path} place where file will be stored
     */

    private Path path;

    /**
     * Constructor from {@link Path} object.
     * {@link #path} created in given location
     *
     * @param root {@link Path} to location where to create files
     * @throws ImplerException if an error occurs while creating directories in {@code root}
     */

    public ImplementorUtils(Path root) throws ImplerException {
        if (root == null) {
            throw new ImplerException("Invalid directory provided");
        }
        try {
            path = Files.createTempDirectory(root.toAbsolutePath(), "tempdir");
        } catch (IOException e) {
            throw new ImplerException(String.format("Unable to create temporary directory: %s", e.getMessage()));
        }
    }

    /**
     * create all directories {@link #path}
     *
     * @param path {@link Path} to where create directories
     * @throws ImplerException if an error occurs while creating directories {@link #path}
     */

    public static void createDirectoriesTo(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException(String.format("Unable to create directories: %s", e.getMessage()));
            }
        }
    }

    /**
     * getter for {@code path}
     * @return {@link #path} for directories
     */

    public Path getTempDirectory() {
        return path;
    }

}