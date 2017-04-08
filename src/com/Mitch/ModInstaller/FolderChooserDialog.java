package com.Mitch.ModInstaller;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FolderChooserDialog extends JFileChooser {
    private JWindow jWindow = null;
    private String Title = null;
    private Path CurrentFolder = Paths.get(".");
    private Path ChosenFolder = null;

    public FolderChooserDialog(JWindow window) {
        this.jWindow = window;

        setFileSelectionMode(DIRECTORIES_ONLY);
        setAcceptAllFileFilterUsed(false);
    }

    public Path showOpenDialog() {
        jWindow.toBack();
        if (showOpenDialog(this.jWindow) == APPROVE_OPTION) {
            this.ChosenFolder = getSelectedFile().toPath();
        }
        return this.ChosenFolder;
    }

    public String getTitle() {
        return this.Title;
    }

    public void setTitle(String title) {
        setDialogTitle(this.Title = title);
    }

    public Path getCurrentFolder() {
        return this.CurrentFolder;
    }

    public void setCurrentFolder(Path path) {
        setCurrentDirectory((this.CurrentFolder = path).toFile());
    }
 /*
   public Dimension getPreferredSize()
   {
     return new Dimension(400, 600);
   }
 */
}