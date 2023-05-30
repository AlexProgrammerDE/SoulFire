/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.gui.libs;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class NativeJFileChooser extends JFileChooser {
    private final FileChooser fileChooser = new FileChooser();
    private List<File> currentFiles;
    private File currentFile;
    private DirectoryChooser directoryChooser;

    public NativeJFileChooser(Path currentDirectory) {
        super(currentDirectory.toFile());
        fileChooser.setInitialDirectory(currentDirectory.toFile());
        new JFXPanel(); // Initializes the JavaFX Platform
    }

    @Override
    public int showOpenDialog(final Component parent) throws HeadlessException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {

            if (parent != null) {
                parent.setEnabled(false);
            }

            if (isDirectorySelectionEnabled()) {
                currentFile = directoryChooser.showDialog(null);
            } else {
                if (isMultiSelectionEnabled()) {
                    currentFiles = fileChooser.showOpenMultipleDialog(null);
                } else {
                    currentFile = fileChooser.showOpenDialog(null);
                }
            }
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (parent != null) {
                parent.setEnabled(true);
            }
        }

        if (isMultiSelectionEnabled()) {
            if (currentFiles != null) {
                return JFileChooser.APPROVE_OPTION;
            } else {
                return JFileChooser.CANCEL_OPTION;
            }
        } else {
            if (currentFile != null) {
                return JFileChooser.APPROVE_OPTION;
            } else {
                return JFileChooser.CANCEL_OPTION;
            }
        }

    }

    @Override
    public int showSaveDialog(final Component parent) throws HeadlessException {
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            if (parent != null) {
                parent.setEnabled(false);
            }

            if (isDirectorySelectionEnabled()) {
                currentFile = directoryChooser.showDialog(null);
            } else {
                currentFile = fileChooser.showSaveDialog(null);
            }
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (parent != null) {
                parent.setEnabled(true);
            }
        }

        if (currentFile != null) {
            return JFileChooser.APPROVE_OPTION;
        } else {
            return JFileChooser.CANCEL_OPTION;
        }
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) {
        return showOpenDialog(parent);
    }

    @Override
    public File[] getSelectedFiles() {
        if (currentFiles == null) {
            return null;
        }
        return currentFiles.toArray(new File[0]);
    }

    @Override
    public void setSelectedFiles(File[] selectedFiles) {
        if (selectedFiles == null || selectedFiles.length == 0) {
            currentFiles = null;
        } else {
            setSelectedFile(selectedFiles[0]);
            currentFiles = new ArrayList<>(Arrays.asList(selectedFiles));
        }
    }

    @Override
    public File getSelectedFile() {
        return currentFile;
    }

    @Override
    public void setSelectedFile(File file) {
        currentFile = file;
        if (file != null) {
            if (file.isDirectory()) {
                fileChooser.setInitialDirectory(file.getAbsoluteFile());

                if (directoryChooser != null) {
                    directoryChooser.setInitialDirectory(file.getAbsoluteFile());
                }
            } else if (file.isFile()) {
                fileChooser.setInitialDirectory(file.getParentFile());
                fileChooser.setInitialFileName(file.getName());

                if (directoryChooser != null) {
                    directoryChooser.setInitialDirectory(file.getParentFile());
                }
            }

        }
    }

    @Override
    public void setFileSelectionMode(int mode) {
        super.setFileSelectionMode(mode);
        if (mode == DIRECTORIES_ONLY) {
            if (directoryChooser == null) {
                directoryChooser = new DirectoryChooser();
            }
            // Set file again, so directory chooser will be affected by it
            setSelectedFile(currentFile);
            setDialogTitle(getDialogTitle());
        }
    }

    @Override
    public String getDialogTitle() {
        return fileChooser.getTitle();
    }

    @Override
    public void setDialogTitle(String dialogTitle) {
        fileChooser.setTitle(dialogTitle);
        if (directoryChooser != null) {
            directoryChooser.setTitle(dialogTitle);
        }
    }

    @Override
    public void changeToParentDirectory() {
        File parentDir = fileChooser.getInitialDirectory().getParentFile();
        if (parentDir.isDirectory()) {
            fileChooser.setInitialDirectory(parentDir);
            if (directoryChooser != null) {
                directoryChooser.setInitialDirectory(parentDir);
            }
        }
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        super.addChoosableFileFilter(filter);
        if (filter.getClass().equals(FileNameExtensionFilter.class)) {
            FileNameExtensionFilter f = (FileNameExtensionFilter) filter;

            List<String> ext = new ArrayList<>();
            for (String extension : f.getExtensions()) {
                ext.add(extension.replaceAll("^\\*?\\.?(.*)$", "*.$1"));
            }
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter(f.getDescription(), ext));
        }
    }

    @Override
    public void setAcceptAllFileFilterUsed(boolean bool) {
        boolean differs = isAcceptAllFileFilterUsed() ^ bool;
        super.setAcceptAllFileFilterUsed(bool);
        if (!differs) {
            return;
        }
        if (bool) {
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("All files", "*.*"));
        } else {
            fileChooser.getExtensionFilters().removeIf(filter -> filter.getExtensions().size() == 1
                    && filter.getExtensions().contains("*.*"));
        }
    }
}
