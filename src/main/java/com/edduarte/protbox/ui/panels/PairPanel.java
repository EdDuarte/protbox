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

package com.edduarte.protbox.ui.panels;

import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.core.FolderOption;
import com.edduarte.protbox.core.registry.PReg;
import com.edduarte.protbox.ui.windows.ConfigurationWindow;
import com.edduarte.protbox.ui.windows.RestoreFileWindow;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class PairPanel extends JLabel {

    private final JLabel revertButton, configButton;

    private final PReg reg;


    public PairPanel(final PReg reg) {
        this.reg = reg;
        setLayout(null);
        setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        setSize(312, 50);
        setOpaque(false);

        JLabel icon = new JLabel(new ImageIcon(Constants.getAsset("folder.png")));
        icon.setBounds(10, 12, 28, 28);
        add(icon);

        JLabel label = new JLabel(reg.getPair().getSharedFolderFile().getName());
        label.setBounds(50, 12, 100, 28);
        label.setFont(Constants.FONT);
        add(label);

        revertButton = new JLabel(new ImageIcon(Constants.getAsset("trash.png")));
        revertButton.setToolTipText("Revert deleted files from this registry.");
        revertButton.setBounds(240, 17, 16, 16);
        revertButton.setVisible(false);
        revertButton.addMouseListener(new CellMouseClickListener());
        revertButton.addMouseListener(new OpenRestoreListener());
        add(revertButton);

        configButton = new JLabel(new ImageIcon(Constants.getAsset("config.png")));
        configButton.setToolTipText("Configure this registry's path or algorithms.");
        configButton.setBounds(270, 17, 16, 16);
        configButton.setVisible(false);
        configButton.addMouseListener(new CellMouseClickListener());
        configButton.addMouseListener(new OpenConfigListener());
        add(configButton);


        final JPopupMenu menu = new JPopupMenu();

        JMenuItem openDrop = new JMenuItem("Open shared folder...");
        openDrop.addActionListener(new OpenFolderListener(FolderOption.SHARED));
        menu.add(openDrop);

        JMenuItem openProt = new JMenuItem("Open prot folder...");
        openProt.addActionListener(new OpenFolderListener(FolderOption.PROT));
        menu.add(openProt);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                go(e);
            }


            @Override
            public void mouseReleased(MouseEvent e) {
                go(e);
            }


            private void go(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        addMouseListener(new CellMouseClickListener());
        addMouseListener(new OpenFolderListener(FolderOption.PROT));

        setMinimumSize(new Dimension(0, 50));
        setPreferredSize(new Dimension(0, 50));
    }


    private void mouseHovering() {
        setBackground(new Color(239, 240, 241));
        setOpaque(true);
        revertButton.setVisible(true);
        configButton.setVisible(true);
    }


    private void mouseStopHovering() {
        setBackground(Color.white);
        setOpaque(false);
        revertButton.setVisible(false);
        configButton.setVisible(false);
    }


    public PReg getRegistry() {
        return reg;
    }


    private class CellMouseClickListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseHovering();
        }


        @Override
        public void mouseExited(MouseEvent e) {
            mouseStopHovering();
        }
    }


    private class OpenFolderListener extends MouseAdapter implements ActionListener {

        private FolderOption folderToOpen;


        private OpenFolderListener(FolderOption folderToOpen) {
            this.folderToOpen = folderToOpen;
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            go();
        }


        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                // DOUBLE CLICK ACTION
                go();
            }
        }


        private void go() {
            try {
                reg.openExplorerFolder(folderToOpen);
                mouseStopHovering();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    private class OpenRestoreListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            mouseStopHovering();
            SwingUtilities.invokeLater(() -> RestoreFileWindow.getInstance(reg));
        }
    }


    private class OpenConfigListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            mouseStopHovering();
            SwingUtilities.invokeLater(() -> ConfigurationWindow.getInstance(reg, PairPanel.this));
        }
    }
}
