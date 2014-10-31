/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.core;

import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
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
                            "You cannot set intersecting paths!\n" +
                            "Please choose another path!",
                    "Invalid path!",
                    JOptionPane.ERROR_MESSAGE);
            return RESULT_CODE_INVALID;
        } else if (sharedPath.startsWith(protPath)) {
            JOptionPane.showMessageDialog(
                    window, "The configured Prot path " + protPath.toString() + " contains the Shared folder path!\n" +
                            "You cannot set intersecting paths!\n" +
                            "Please choose another path!",
                    "Invalid path!",
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
                                    "another folder pair!\nPlease choose another path!",
                            "Invalid configured path!",
                            JOptionPane.ERROR_MESSAGE);
                    return RESULT_CODE_INVALID;
                }
            } else if (existingProt.equals(protPath) || protPath.startsWith(existingProt) || existingProt.startsWith(protPath)) {
                JOptionPane.showMessageDialog(
                        window, "The path " + existingProt.toString() + " is already in use by " +
                                "another folder pair!\nPlease choose another path!",
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
                                "file, which will then be encrypted into the Shared folder.\n\n" +
                                "Please add at least one file to the configured Prot folder!\n",
                        "Invalid configured path!",
                        JOptionPane.ERROR_MESSAGE);
                return RESULT_CODE_INVALID;
            }

            if (sharedSubFiles.length != 0) {
                if (JOptionPane.showConfirmDialog(
                        window, "The configured Shared folder is not empty, so it's assumed to be part of another " +
                                "existing folder pair (either yours or from another user)!\n" +
                                "If you accept this folder as the Shared folder, a request will be sent to " +
                                "users with access to this folder's contents.\n" +
                                "Do you wish to proceed?\n\n",
                        "Confirm Shared folder",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE) != JOptionPane.YES_OPTION) {
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
