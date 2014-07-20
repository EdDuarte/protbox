package edduarte.protbox.ui;

import edduarte.protbox.core.directory.Registry;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class FolderValidation {

    public static boolean validate(Component window, Path protPath, Path dropPath, boolean doSharedFolderChecks){
        if(protPath.equals(dropPath) || protPath.startsWith(dropPath)){
            JOptionPane.showMessageDialog(
                    window, "The configured output path "+protPath.toString()+" is equal to or contained in the shared folder path!\n" +
                    "You can not configure dependent paths, since it can create file inconsistencies!\n" +
                    "Please choose another path!",
                    "Invalid configured path!",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        else if(dropPath.startsWith(protPath)){
            JOptionPane.showMessageDialog(
                    window, "The configured output path "+protPath.toString()+" contains the shared folder path!\n" +
                    "You can not configure dependent paths, since it can create file inconsistencies!\n" +
                    "Please choose another path!",
                    "Invalid configured path!",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }


        for(Component c : TrayApplet.getInstance().instanceList.getComponents()){
            if(c.getClass().getSimpleName().toLowerCase().equalsIgnoreCase("InstanceCell")){
                Registry toCheck = ((InstanceCell)c).getDirectory();
                Path existingDrop = Paths.get(toCheck.SHARED_PATH);
                Path existingProt = Paths.get(toCheck.OUTPUT_PATH);

                if(doSharedFolderChecks){
                    if(existingDrop.equals(dropPath) || dropPath.startsWith(existingDrop) || existingDrop.startsWith(dropPath)){
                        JOptionPane.showMessageDialog(
                                window, "The path "+existingDrop.toString()+" is already in use by another directory!\n" +
                                "Please choose another path!",
                                "Invalid configured path!",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                else if(existingProt.equals(protPath) || protPath.startsWith(existingProt) || existingProt.startsWith(protPath)){
                    JOptionPane.showMessageDialog(
                            window, "The path "+existingProt+" is already in use by another directory!\n" +
                            "Please choose another path!",
                            "Invalid configured path!",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

//        if(doSharedFolderChecks){
            File dropFile = dropPath.toFile();
            if(!new File(dropFile, "»users").exists() && !hasNoDirectoriesInside(dropFile)){
                JOptionPane.showMessageDialog(
                        window, "The configured shared folder is part of another existing directory (either yours or from another user)!\n" +
                        "To obtain access to the contents of this folder, you should set the shared folder path at the root!\n",
                        "Invalid configured path!",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
//        }


        File protFile = protPath.toFile();
        if(protFile.listFiles().length!=0){
            if (JOptionPane.showConfirmDialog(
                    window, "The configured output path already contains files!\n" +
                    "If you accept this folder as the output folder, the already existing files\n" +
                    "will be synchronized into the shared folder!\n" +
                    "Do you wish to proceed?\n\n",
                    "Confirm path change",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasNoDirectoriesInside(File root){
        for(File f : root.listFiles()){
            if(f.getName().equalsIgnoreCase("»==")){
                return false;
            } else if(f.isDirectory()){
                boolean result = hasNoDirectoriesInside(f);
                if(!result){
                    return false;
                }
            }
        }
        return true;
    }
}
