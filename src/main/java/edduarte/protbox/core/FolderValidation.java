package edduarte.protbox.core;

import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class FolderValidation {

    public static final int RESULT_CODE_INVALID = 5325348;

    public static final int RESULT_CODE_EXISTING_REGISTRY = 1326326;

    public static final int RESULT_CODE_NEW_REGISTRY = 2326325;

    public static int validate(Component window, Path protPath, Path sharedPath, boolean isChangingProtPath) {
        if (protPath.equals(sharedPath) || protPath.startsWith(sharedPath)) {
            JOptionPane.showMessageDialog(
                    window, "The configured Prot path " + protPath.toString() + " is equal to or contained in the Shared folder path!\n" +
                            "You can not configure dependent paths, since it can create file inconsistencies!\n" +
                            "Please choose another path!",
                    "Invalid configured path!",
                    JOptionPane.ERROR_MESSAGE);
            return RESULT_CODE_INVALID;
        } else if (sharedPath.startsWith(protPath)) {
            JOptionPane.showMessageDialog(
                    window, "The configured Prot path " + protPath.toString() + " contains the Shared folder path!\n" +
                            "You can not configure dependent paths, since it can create file inconsistencies!\n" +
                            "Please choose another path!",
                    "Invalid configured path!",
                    JOptionPane.ERROR_MESSAGE);
            return RESULT_CODE_INVALID;
        }


        for (PairPanel c : TrayApplet.getInstance().getPairPanels()) {
            PReg toCheck = c.getRegistry();
            Path existingShared = toCheck.getPair().getSharedFolderFile().toPath();
            Path existingProt = toCheck.getPair().getProtFolderFile().toPath();

            if (isChangingProtPath) {
                if (existingShared.equals(sharedPath) || sharedPath.startsWith(existingShared) || existingShared.startsWith(sharedPath)) {
                    JOptionPane.showMessageDialog(
                            window, "The path " + existingShared.toString() + " is already in use by " +
                                    "another registry!\nPlease choose another path!",
                            "Invalid configured path!",
                            JOptionPane.ERROR_MESSAGE);
                    return RESULT_CODE_INVALID;
                }
            } else if (existingProt.equals(protPath) || protPath.startsWith(existingProt) || existingProt.startsWith(protPath)) {
                JOptionPane.showMessageDialog(
                        window, "The path " + existingProt.toString() + " is already in use by " +
                                "another registry!\nPlease choose another path!",
                        "Invalid configured path!",
                        JOptionPane.ERROR_MESSAGE);
                return RESULT_CODE_INVALID;
            }
        }


        File[] sharedSubFiles = sharedPath.toFile().listFiles();
        File[] protSubFiles = protPath.toFile().listFiles();
        boolean existing = true;
        if (isChangingProtPath && protSubFiles != null && sharedSubFiles != null) {

            if (protSubFiles.length == 0 && sharedSubFiles.length == 0) {
                JOptionPane.showMessageDialog(
                        window, "The configured Prot and Shared folder are empty! Protbox assumes that empty Shared " +
                                "folders are folders that were\nnot previously configured / encrypted. In order to " +
                                "configure a new Protbox Registry, the Prot folder must have at\nleast one decrypted " +
                                "file, which is then encrypted into the Shared folder.\n\n" +
                                "Please add at least one file to the configured Prot folder!\n",
                        "Invalid configured path!",
                        JOptionPane.ERROR_MESSAGE);
                return RESULT_CODE_INVALID;
            }

            if (sharedSubFiles.length != 0) {
                if (JOptionPane.showConfirmDialog(
                        window, "The configured Shared folder is not empty, so it's assumed to be part of another " +
                                "existing PReg (either yours or from another user)!\n" +
                                "If you accept this folder as the Shared folder, a request will be sent to other " +
                                "users with access to this folder's contents.\n" +
                                "Do you wish to proceed?\n\n",
                        "Confirm Shared folder",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                    return RESULT_CODE_INVALID;
                }
            } else {
                existing = false;
            }
        }

        if (!isChangingProtPath && protSubFiles != null && protSubFiles.length != 0) {
            if (JOptionPane.showConfirmDialog(
                    window, "The configured Prot path already contains files!\n" +
                            "If you accept this folder as the Prot folder, the already existing files\n" +
                            "will be synchronized into the Shared folder!\n" +
                            "Do you wish to proceed?\n\n",
                    "Confirm path change",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return RESULT_CODE_INVALID;
            }
        }

        return existing ? RESULT_CODE_EXISTING_REGISTRY : RESULT_CODE_NEW_REGISTRY;
    }
}
