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

import com.google.common.collect.Lists;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.PbxEntry;
import edduarte.protbox.core.registry.PbxFile;
import edduarte.protbox.core.registry.PbxFolder;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.listeners.OnKeyReleased;
import edduarte.protbox.utils.listeners.OnMouseClick;
import org.apache.commons.lang3.SystemUtils;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.jdesktop.xswingx.PromptSupport;

/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @version 2.0
 */
public class RestoreFileWindow extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(RestoreFileWindow.class);

    private RestoreFileWindow(final PReg registry) {
        super();
        setLayout(null);

        final JTextField searchField = new JTextField();
        searchField.setLayout(null);
        searchField.setBounds(2, 2, 301, 26);
        searchField.setBorder(new LineBorder(Color.lightGray));
        searchField.setFont(Constants.FONT);
        add(searchField);


        final JLabel noBackupFilesLabel = new JLabel("<html>No backups files were found!<br><br>" +
                "<font color=\"gray\">If you think there is a problem with the<br>" +
                "backup system, please create an issue here:<br>" +
                "<a href=\"#\">https://github.com/edduarte/protbox/issues</a></font></html>");
        noBackupFilesLabel.setLayout(null);
        noBackupFilesLabel.setBounds(20, 50, 300, 300);
        noBackupFilesLabel.setFont(Constants.FONT.deriveFont(14f));
        noBackupFilesLabel.addMouseListener((OnMouseClick) e -> {
            String urlPath = "https://github.com/edduarte/protbox/issues";

            try {

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(urlPath));

                } else {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + urlPath);

                    } else {
                        java.util.List<String> browsers =
                                Lists.newArrayList("firefox", "opera", "safari", "mozilla", "chrome");

                        for (String browser : browsers) {
                            if (Runtime.getRuntime().exec(new String[]{"which", browser}).waitFor() == 0) {

                                Runtime.getRuntime().exec(new String[]{browser, urlPath});
                                break;
                            }
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        DefaultMutableTreeNode rootTreeNode = registry.buildEntryTree();
        final JTree tree = new JTree(rootTreeNode);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        if (rootTreeNode.getChildCount() == 0) {
            searchField.setEnabled(false);
            add(noBackupFilesLabel);
        }
        expandTree(tree);
        tree.setLayout(null);
        tree.setRootVisible(false);
        tree.setEditable(false);
        tree.setCellRenderer(new SearchableTreeCellRenderer(searchField));
        searchField.addKeyListener((OnKeyReleased) e -> {

            // update and expand tree
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.nodeStructureChanged((TreeNode) model.getRoot());
            expandTree(tree);
        });
        final JScrollPane scroll = new JScrollPane(tree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new DropShadowBorder());
        scroll.setBorder(new LineBorder(Color.lightGray));
        scroll.setBounds(2, 30, 334, 360);
        add(scroll);


        JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
        close.setLayout(null);
        close.setBounds(312, 7, 18, 18);
        close.setFont(Constants.FONT);
        close.setForeground(Color.gray);
        close.addMouseListener((OnMouseClick) e -> dispose());
        add(close);


        final JLabel permanentDeleteButton = new JLabel(new ImageIcon(Constants.getAsset("permanent.png")));
        permanentDeleteButton.setLayout(null);
        permanentDeleteButton.setBounds(91, 390, 40, 39);
        permanentDeleteButton.setBackground(Color.black);
        permanentDeleteButton.setEnabled(false);
        permanentDeleteButton.addMouseListener((OnMouseClick) e -> {
            if (permanentDeleteButton.isEnabled()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                PbxEntry entry = (PbxEntry) node.getUserObject();

                if (JOptionPane.showConfirmDialog(
                        null,
                        "Are you sure you wish to permanently delete '" + entry.realName() + "'?\nThis file and its " +
                                "backup copies will be deleted immediately. You cannot undo this action.",
                        "Confirm Cancel",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

                    registry.permanentDelete(entry);
                    dispose();
                    RestoreFileWindow.getInstance(registry);

                } else {
                    setVisible(true);
                }
            }
        });
        add(permanentDeleteButton);


        final JLabel configBackupsButton = new JLabel(new ImageIcon(Constants.getAsset("config.png")));
        configBackupsButton.setLayout(null);
        configBackupsButton.setBounds(134, 390, 40, 39);
        configBackupsButton.setBackground(Color.black);
        configBackupsButton.setEnabled(false);
        configBackupsButton.addMouseListener((OnMouseClick) e -> {
            if (configBackupsButton.isEnabled()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                PbxEntry entry = (PbxEntry) node.getUserObject();
                if (entry instanceof PbxFile) {
                    PbxFile pbxFile = (PbxFile) entry;

                    JFrame frame = new JFrame("Choose backup policy");
                    Object option = JOptionPane.showInputDialog(frame,
                            "Choose below the backup policy for this file:",
                            "Choose backup",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            PbxFile.BackupPolicy.values(),
                            pbxFile.getBackupPolicy());

                    if (option == null) {
                        setVisible(true);
                        return;
                    }
                    PbxFile.BackupPolicy pickedPolicy = PbxFile.BackupPolicy.valueOf(option.toString());
                    pbxFile.setBackupPolicy(pickedPolicy);
                }
                setVisible(true);
            }
        });
        add(configBackupsButton);


        final JLabel restoreBackupButton = new JLabel(new ImageIcon(Constants.getAsset("restore.png")));
        restoreBackupButton.setLayout(null);
        restoreBackupButton.setBounds(3, 390, 85, 39);
        restoreBackupButton.setBackground(Color.black);
        restoreBackupButton.setEnabled(false);
        restoreBackupButton.addMouseListener((OnMouseClick) e -> {
            if (restoreBackupButton.isEnabled()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                PbxEntry entry = (PbxEntry) node.getUserObject();
                if (entry instanceof PbxFolder) {
                    registry.restoreFolderFromEntry((PbxFolder) entry);

                } else if (entry instanceof PbxFile) {
                    PbxFile pbxFile = (PbxFile) entry;
                    java.util.List<String> snapshots = pbxFile.snapshotsToString();
                    if (snapshots.isEmpty()) {
                        setVisible(true);
                        return;
                    }

                    JFrame frame = new JFrame("Choose backup");
                    Object option = JOptionPane.showInputDialog(frame,
                            "Choose below what backup snapshot would you like restore:",
                            "Choose backup",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            snapshots.toArray(),
                            snapshots.get(0));

                    if (option == null) {
                        setVisible(true);
                        return;
                    }
                    int pickedIndex = snapshots.indexOf(option.toString());
                    registry.restoreFileFromEntry((PbxFile) entry, pickedIndex);
                }
                dispose();
            }
        });
        add(restoreBackupButton);


        tree.addMouseListener((OnMouseClick) e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null) {
                PbxEntry entry = (PbxEntry) node.getUserObject();
                if ((entry instanceof PbxFolder && entry.areNativeFilesDeleted())) {
                    permanentDeleteButton.setEnabled(true);
                    restoreBackupButton.setEnabled(true);
                    configBackupsButton.setEnabled(false);

                } else if (entry instanceof PbxFile) {
                    permanentDeleteButton.setEnabled(true);
                    restoreBackupButton.setEnabled(true);
                    configBackupsButton.setEnabled(true);

                } else {
                    permanentDeleteButton.setEnabled(false);
                    restoreBackupButton.setEnabled(false);
                    configBackupsButton.setEnabled(false);
                }
            }
        });


        final JLabel cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(229, 390, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> dispose());
        add(cancel);

        addWindowFocusListener(new WindowFocusListener() {
            private boolean gained = false;

            @Override
            public void windowGainedFocus(WindowEvent e) {
                gained = true;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                if (gained) {
                    dispose();
                }
            }
        });

        setSize(340, 432);
        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setVisible(true);
    }


    public static RestoreFileWindow getInstance(final PReg registry) {
        RestoreFileWindow newInstance;
//                = instances.get(registry);
//        if (newInstance == null) {
        newInstance = new RestoreFileWindow(registry);
//            instances.put(registry, newInstance);
//        } else {
        newInstance.setVisible(true);
        newInstance.toFront();
//        }
        return newInstance;
    }


    private void expandTree(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++)
            tree.expandRow(i);
    }


    private class SearchableTreeCellRenderer extends DefaultTreeCellRenderer {

        private final JTextField searchField;

        public SearchableTreeCellRenderer(JTextField searchField) {
            this.searchField = searchField;
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            // process search functionality
            String search = searchField.getText().toLowerCase();
            String query = value.toString();
            StringBuffer html = new StringBuffer("<html>");
            Matcher m = Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE).matcher(query);
            while (m.find()) {
                m.appendReplacement(html, "<b>" + m.group() + "</b>");
                m.appendTail(html).append("</html>");
            }
            super.getTreeCellRendererComponent(tree, html.toString(), sel, expanded, leaf, row, hasFocus);
            tree.setRowHeight(32);


            PbxEntry entry = (PbxEntry) ((DefaultMutableTreeNode) value).getUserObject();
            setFont(Constants.FONT.deriveFont(13f));

            if (entry instanceof PbxFile) {
                if (entry.areNativeFilesDeleted()) {
                    setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of deleted file
                    setForeground(Color.gray);
                } else {
                    setIcon(new ImageIcon(Constants.getAsset("file.png"))); // image of normal file
                }
            } else if (entry instanceof PbxFolder) {
                if (entry.areNativeFilesDeleted()) {
                    setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of deleted file
                    setForeground(Color.gray);
                } else {
                    setIcon(new ImageIcon(Constants.getAsset("folder.png"))); // image of normal folder
                }
            }

            return this;
        }
    }

}
