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

package edduarte.protbox.ui.windows;

import edduarte.protbox.Main;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderValidation;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.exception.ProtboxException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.listeners.OnKeyReleased;
import edduarte.protbox.utils.listeners.OnMouseClick;
import ij.io.DirectoryChooser;
import org.jdesktop.xswingx.PromptSupport;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @version 2.0
 */
public class ConfigurationWindow extends JDialog {
    private static final Map<PReg, ConfigurationWindow> instances = new HashMap<>();
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(ConfigurationWindow.class);
    private PReg reg;
    private JTextField path;
    private JLabel ok;

    private ConfigurationWindow(final PReg reg, final PairPanel instanceCell) {
        super();
        this.reg = reg;
        setLayout(null);

        JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
        close.setLayout(null);
        close.setBounds(542, 7, 18, 18);
        close.setFont(Constants.FONT);
        close.setForeground(Color.gray);
        close.addMouseListener((OnMouseClick) e -> dispose());
        add(close);

        JLabel label2 = new JLabel("Prot folder: ");
        label2.setFont(Constants.FONT);
        label2.setBounds(20, 50, 100, 30);
        path = new JTextField(reg.getPair().getProtFolderPath() + "\\");
        PromptSupport.setPrompt("<none selected>", path);
        path.setMargin(new Insets(0, 10, 0, 10));
        path.setFont(Constants.FONT);
        path.setBorder(new CompoundBorder(new LineBorder(new Color(210, 210, 210), 1, false), new EmptyBorder(0, 3, 0, 0)));
        path.setBounds(130, 50, 341, 30);
        path.addKeyListener((OnKeyReleased) e -> check());
        JButton b2 = new JButton("Choose ...");
        b2.setBorder(new LineBorder(Color.lightGray));
        b2.setBounds(470, 50, 70, 30);
        add(label2);
        add(path);
        add(b2);
        b2.addActionListener(e -> {
            final DirectoryChooser chooser = new DirectoryChooser("Choose Shared folder...");
            String directory = chooser.getDirectory();
            if (directory != null) {
                path.setText(directory);
            }
            check();
        });

        JLabel stop = new JLabel("<html><font color='red'>Stop monitoring this folder</font></html>");
        stop.setIcon(new ImageIcon(Constants.getAsset("delete.png")));
        stop.setFont(Constants.FONT.deriveFont(11f));
        ;
        stop.setBounds(20, 100, 200, 30);
        stop.addMouseListener((OnMouseClick) e -> {
            if (JOptionPane.showConfirmDialog(
                    ConfigurationWindow.this, "Are you sure you wish to cancel access to this shared folder?\n" +
                            "If you do so, every deleted file in the restore history will be permanently lost!\n" +
                            "In addition, if you wish to monitor this folder again, you will need to go through\n" +
                            "the authentication process again, requiring your eID token.\n\n",
                    "Confirm stop monitoring",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                reg.stop();
                File file = new File(Constants.REGISTRIES_DIR, reg.id);
                Constants.delete(file);

                // asks user if he wishes to keep the original files
                if (JOptionPane.showConfirmDialog(
                        ConfigurationWindow.this, "Do you wish to keep the decrypted files in the prot folder?\n"
                                + "Choosing \"No\" will delete the prot folder and all of it's files.",
                        "Delete prot folder",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                    Constants.delete(reg.getPair().getProtFolderFile()); // delete prot folder
                }

                TrayApplet.getInstance().removePairPanel(instanceCell);
                if (TrayApplet.getInstance().getPairPanels().length == 0) {

                    // there are no instances left!
                    Main.hideTrayApplet();
                    NewRegistryWindow.start(true);
                }
                dispose();
            }
        });
        add(stop);

        ok = new JLabel(new ImageIcon(Constants.getAsset("ok.png")));
        ok.setBounds(350, 140, 122, 39);
        ok.setEnabled(false);
        ok.addMouseListener((OnMouseClick) e -> {
            if (ok.isEnabled()) {
                try {
                    String protPath = reg.getPair().getProtFolderPath();
                    if (!protPath.equalsIgnoreCase(path.getText())) {
                        Path newPath = Paths.get(path.getText());
                        Path oldPath = Paths.get(protPath);
                        if (newPath.startsWith(oldPath) || oldPath.startsWith(newPath)) {
                            JOptionPane.showMessageDialog(
                                    ConfigurationWindow.this, "The new configured path contains or is contained in the older path!\n" +
                                            "You can not choose a path dependent of the one before, since it can create file inconsistencies!\n" +
                                            "Please choose another path!",
                                    "Invalid configured path!",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        Path sharedPath = reg.getPair().getSharedFolderFile().toPath();
                        int resultCode = FolderValidation.validate(this, newPath, sharedPath, false);
                        if (resultCode == FolderValidation.RESULT_CODE_INVALID) {
                            return;
                        }

                        reg.changeProtPath(path.getText());
                    }
                    dispose();
                } catch (ProtboxException ex) {
                    logger.error(ex.toString());
                }
            }
        });
        add(ok);

        JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(450, 140, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> dispose());
        add(cancel);

        setSize(570, 190);
        setUndecorated(true);
//        getContentPane().setBackground(new Color(239, 240, 241));
        getContentPane().setBackground(Color.white);
        setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setVisible(true);
    }

    public static void closeAllInstances() {
        for (PReg d : instances.keySet()) {
            instances.get(d).dispose();
        }
        instances.clear();
    }

    public static ConfigurationWindow getInstance(final PReg directory, final PairPanel instanceCell) {
        ConfigurationWindow newInstance = instances.get(directory);
        if (newInstance == null) {
            newInstance = new ConfigurationWindow(directory, instanceCell);
            instances.put(directory, newInstance);
        } else {
            newInstance.toFront();
        }
        return newInstance;
    }

    private void check() {
        String protPath = reg.getPair().getProtFolderPath();
        if ((!path.getText().equalsIgnoreCase(protPath) &&
                !path.getText().equalsIgnoreCase(protPath + "\\"))
//                || (!combo.getSelectedItem().toString().equals("---") &&
//                        !combo.getSelectedItem().toString().equals(registry.getAlgorithm()))
                ) {
            File pathFile = new File(path.getText());
            if (pathFile.exists() && pathFile.isDirectory() && pathFile.canRead() && pathFile.canWrite()) {
                ok.setEnabled(true);
                return;
            }
        }

        ok.setEnabled(false);
    }

    @Override
    public void dispose() {
        instances.put(reg, null);
        super.dispose();
    }

}
