package edduarte.protbox.ui;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.core.directory.Registry;
import edduarte.protbox.core.directory.Source;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class InstanceCell extends JLabel {

    private final JLabel revertButton, userButton, configButton;
    private final Registry protReg;

    public InstanceCell(final Registry protReg){
        this.protReg = protReg;
        setLayout(null);
        setBorder(new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        setSize(312, 50);
        setOpaque(false);


        JLabel icon = new JLabel(new ImageIcon(Constants.getAsset("instance.png")));
        icon.setBounds(10, 12, 28, 28);
        add(icon);


        JLabel label = new JLabel(Paths.get(protReg.SHARED_PATH).getFileName().toString());
        label.setBounds(50, 12, 100, 28);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(label);




        revertButton = new JLabel(new ImageIcon(Constants.getAsset("trash.png")));
        revertButton.setToolTipText("Revert deleted files from this directory.");
        revertButton.setBounds(210, 17, 16, 16);
        revertButton.setVisible(false);
        revertButton.addMouseListener(new CellMouseListener());
        revertButton.addMouseListener(new OpenRestoreListener());
        add(revertButton);
        
        

        userButton = new JLabel(new ImageIcon(Constants.getAsset("user.png")));
        userButton.setToolTipText("Check this directory's users.");
        userButton.setBounds(240, 17, 16, 16);
        userButton.setVisible(false);
        userButton.addMouseListener(new CellMouseListener());
        userButton.addMouseListener(new OpenUserListener());
        add(userButton);



        configButton = new JLabel(new ImageIcon(Constants.getAsset("config.png")));
        configButton.setToolTipText("Configure this directory's path or algorithms.");
        configButton.setBounds(270, 17, 16, 16);
        configButton.setVisible(false);
        configButton.addMouseListener(new CellMouseListener());
        configButton.addMouseListener(new OpenConfigListener());
        add(configButton);




        final JPopupMenu menu = new JPopupMenu();

        JMenuItem openDrop = new JMenuItem("Open shared folder...");
        openDrop.addActionListener(new OpenFolderListener(Source.SHARED));
        menu.add(openDrop);

        JMenuItem openProt = new JMenuItem("Open output folder...");
        openProt.addActionListener(new OpenFolderListener(Source.PROT));
        menu.add(openProt);

//        menu.addSeparator();
//
//        JMenuItem openRestore = new JMenuItem("Restore deleted files");
//        openRestore.addMouseListener(new OpenRestoreListener());
//        menu.add(openRestore);
//
//        JMenuItem openUsers = new JMenuItem("UserList list");
//        openRestore.addMouseListener(new OpenConfigListener());
//        menu.add(openUsers);
//
//        JMenuItem openSettings = new JMenuItem("Settings");
//        openRestore.addMouseListener(new OpenConfigListener());
//        menu.add(openSettings);

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
                if (e.isPopupTrigger()){
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });




        addMouseListener(new CellMouseListener());
        addMouseListener(new OpenFolderListener(Source.PROT));
    }

    private class CellMouseListener extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseHovering();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseStopHovering();
        }
    }

    private void mouseHovering(){
        setBackground(new Color(239, 240, 241));
        setOpaque(true);
        revertButton.setVisible(true);
        userButton.setVisible(true);
        configButton.setVisible(true);
    }

    private void mouseStopHovering(){
        setBackground(Color.white);
        setOpaque(false);
        revertButton.setVisible(false);
        userButton.setVisible(false);
        configButton.setVisible(false);
    }

    public Registry getProtReg(){
        return protReg;
    }


    private class OpenFolderListener extends MouseAdapter implements ActionListener {

        private Source folderToOpen;

        private OpenFolderListener(Source folderToOpen){
            this.folderToOpen = folderToOpen;
        }

        @Override
        public void actionPerformed(ActionEvent e){
            go();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                // DOUBLE CLICK ACTION
                go();
            }
        }

        private void go(){
            try{
                protReg.openExplorerFolder(folderToOpen);
                mouseStopHovering();
            } catch (IOException ex){
                System.err.println(ex);
            }
        }
    }

    private class OpenRestoreListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            mouseStopHovering();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Restore.getInstance(protReg);
                }
            });
        }
    }

    private class OpenUserListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            mouseStopHovering();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    File usersFile = new File(protReg.SHARED_PATH, "»users");

                    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersFile))){
                        java.util.List<User> userList = (java.util.List<User>) in.readObject();
                        UserList.getInstance(Paths.get(protReg.SHARED_PATH).getFileName().toString(), userList, false);
                    }catch (IOException|ReflectiveOperationException ex) {
                        System.err.println(ex);
                    }

                }
            });
        }
    }

    private class OpenConfigListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            mouseStopHovering();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() { Config.getInstance(protReg, InstanceCell.this); }
            });
        }
    }
}
