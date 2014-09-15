package edduarte.protbox.ui.panels;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.Folder;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.ui.window.ConfigurationWindow;
import edduarte.protbox.ui.window.RestoreFileWindow;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
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

        JLabel icon = new JLabel(new ImageIcon(Constants.getAsset("instance.png")));
        icon.setBounds(10, 12, 28, 28);
        add(icon);

        JLabel label = new JLabel(Paths.get(reg.SHARED_PATH).getFileName().toString());
        label.setBounds(50, 12, 100, 28);
        label.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
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
        openDrop.addActionListener(new OpenFolderListener(Folder.SHARED));
        menu.add(openDrop);

        JMenuItem openProt = new JMenuItem("Open prot folder...");
        openProt.addActionListener(new OpenFolderListener(Folder.PROT));
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
        addMouseListener(new OpenFolderListener(Folder.PROT));

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

        private Folder folderToOpen;

        private OpenFolderListener(Folder folderToOpen) {
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
