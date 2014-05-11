package pt.ua.sio.protbox.gui;

import ij.io.DirectoryChooser;
import org.apache.commons.io.FileUtils;
import org.jdesktop.xswingx.PromptSupport;
import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.User;
import pt.ua.sio.protbox.core.directory.Directory;
import pt.ua.sio.protbox.exception.ProtException;
import pt.ua.sio.protbox.util.AWTUtils;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class Config extends JDialog {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(Config.class);

    //    private final TrayApplet mainApp;
//    private final JPanel instanceList;
    private Directory directory;
//    private final Map<String, BufferedImage> ASSETS;

    private JTextField path;
    //    private JComboBox<String> combo;
    private JLabel ok, cancel;

    private static Map<Directory, Config> instances = new HashMap<>();

    static void closeAllInstances(){
        for(Directory d : instances.keySet()){
            instances.get(d).dispose();
        }
        instances.clear();
    }

    public static Config getInstance(final Directory directory, final InstanceCell instanceCell) {
        Config newInstance = instances.get(directory);
        if(newInstance==null){
            newInstance = new Config(directory, instanceCell);
            instances.put(directory, newInstance);
        } else {
            newInstance.toFront();
        }
        return newInstance;
    }

    private Config(final Directory directory, final InstanceCell instanceCell) {
        super();
        this.setLayout(null);
//        this.mainApp = mainApp;
//        this.instanceList = instanceList;
        this.directory = directory;
//        this.ASSETS = ASSETS;



        JLabel close = new JLabel(new ImageIcon(Constants.ASSETS.get("close.png")));
        close.setLayout(null);
        close.setBounds(542, 7, 18, 18);
        close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        close.setForeground(Color.gray);
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        this.add(close);






        JLabel label2 = new JLabel("Output folder: ");
        label2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label2.setBounds(20, 50, 100, 30);
        path = new JTextField(directory.OUTPUT_PATH +"\\");
        PromptSupport.setPrompt("<none selected>", path);
        path.setMargin(new Insets(0, 10, 0, 10));
        path.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        path.setBorder(new CompoundBorder(new LineBorder(new Color(210, 210, 210), 1, false), new EmptyBorder(0, 3, 0, 0)));
        path.setBounds(130, 50, 341, 30);
        path.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                check();
            }
        });
        JButton b2 = new JButton("Choose ...");
        b2.setBorder(new LineBorder(Color.lightGray));
        b2.setBounds(470, 50, 70, 30);
        add(label2);
        add(path);
        add(b2);
        b2.addActionListener(new PathChooserListener(path));



//        JLabel noAuth = new JLabel("<html><font color=\"gray\">You do not have authorization to change this directory's ciphering algorithm.</font></html>");
//        noAuth.setBounds(260, 94, 300, 30);
//
//
//        JLabel label3 = new JLabel("Algorithm: ");
//        label3.setFont(new Font("Segoe UI", Font.PLAIN, 12));
//        label3.setBounds(20, 94, 100, 30);
//        combo = new JComboBox<>();
//        combo.setBounds(130, 100, 120, 20);
//        combo.addItem("---");
//        combo.addItem("AES");
//        combo.addItem("DES");
//        combo.setSelectedItem(directory.getAlgorithm());
//        combo.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                check();
//            }
//        });
//        if(!directory.USER_HAS_AUTH){
//            combo.setEnabled(false);
//            add(noAuth);
//        }
//        add(label3);
//        add(combo);


        JLabel stop = new JLabel("<html><font color='red'>Stop monitoring this folder</font></html>");
        stop.setIcon(new ImageIcon(Constants.ASSETS.get("delete.png")));
        stop.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        stop.setBounds(20, 100, 200, 30);
        stop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (JOptionPane.showConfirmDialog(
                        Config.this, "Are you sure you wish to stop monitoring this folder?\n" +
                        "If you do so, every deleted file in the restore history will be permanently lost!\n" +
                        "In addition, if you wish to monitor this folder again, you will need to go through\n" +
                        "the authorization process again, requiring your Citizen Card.\n\n",
                        "Confirm stop monitoring",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    directory.stop();
                    File file = new File(Constants.INSTALL_DIR, directory.NAME);
                    try {
                        Constants.delete(file);

                        File usersListFile = new File(directory.SHARED_PATH, "»users");

                        java.util.List<User> userList = new ArrayList<>();
                        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersListFile))){
                            userList = (java.util.List<User>)in.readObject();
                        }catch (IOException|ReflectiveOperationException ex) {
                            logger.error(ex.toString());
                        }

                        for(User u : userList){
                            if(u.getId().equalsIgnoreCase(Main.initializedData.user.getId())){
                                userList.remove(u);
                                break;
                            }
                        }

                        if(!userList.isEmpty()){
                            try(FileOutputStream fileOut = new FileOutputStream(usersListFile);
                                ObjectOutputStream out = new ObjectOutputStream(fileOut)){

                                fileOut.write(new String().getBytes());

                                out.writeObject(userList);
                                out.flush();
                            }

                            // asks user if he wishes to keep the original files
                            if (JOptionPane.showConfirmDialog(
                                    Config.this, "Do you wish to keep the decrypted files?\n" +
                                    "Choosing \"No\" will delete the output folder and all of it's files.",
                                    "Delete output files",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                                Constants.delete(new File(directory.OUTPUT_PATH)); // delete output folder
                            }
                        } else { // the userList is empty -> no-one is using configuring this folder anymore
                            File dropFile = new File(directory.SHARED_PATH);
                            File protFile = new File(directory.OUTPUT_PATH);

                            FileUtils.deleteDirectory(dropFile); // delete shared folder
                            Thread.sleep(100);
                            FileUtils.moveDirectory(protFile, dropFile); // move decoded files to shared folder
                            Thread.sleep(100);
                            Constants.delete(protFile);
                        }

                    }catch (IOException|InterruptedException ex) {
                        logger.error(ex.toString());
                    }

                    TrayApplet.getInstance().instanceList.remove(instanceCell);
                    if(TrayApplet.getInstance().instanceList.getComponentCount()==0){
                        // there are no instances left!
                        Main.hideTrayApplet();
                        NewDirectory.getInstance(true);
                    }
                    dispose();
                }
            }
        });
        add(stop);



        ok = new JLabel(new ImageIcon(Constants.ASSETS.get("ok.png")));
        ok.setBounds(350, 140, 122, 39);
        ok.setEnabled(false);
        ok.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(ok.isEnabled()){
                    try {
                        if (!directory.OUTPUT_PATH.equalsIgnoreCase(path.getText())) {
                            Path newPath = Paths.get(path.getText());
                            Path oldPath = Paths.get(directory.OUTPUT_PATH);
                            if (newPath.startsWith(oldPath) || oldPath.startsWith(newPath)) {
                                JOptionPane.showMessageDialog(
                                        Config.this, "The new configured path contains or is contained in the older path!\n" +
                                        "You can not choose a path dependent of the one before, since it can create file inconsistencies!\n" +
                                        "Please choose another path!",
                                        "Invalid configured path!",
                                        JOptionPane.ERROR_MESSAGE);
                                return;
                            }

                            if (!FolderValidation.validate(Config.this, newPath, Paths.get(directory.SHARED_PATH), false))
                                return;

                            directory.changeOutputPath(path.getText());

                        }
//                    else if (!directory.getAlgorithm().equalsIgnoreCase(combo.getSelectedItem().toString())) {
//                        if (JOptionPane.showConfirmDialog(
//                                Config.this, "You are attempting to change the algorithm of this directory! This will require\n" +
//                                "every file in the shared to be re-ciphered according to this new algorithm, and this change will\n" +
//                                "be notified to other users of the shared folders.\n\n" +
//                                "Because of this, this process can take some time. As a security measure, we ask you and the\n" +
//                                "users of this shared folder to not use this folder's files while this process of change is running!\n\n",
//                                "Confirm algorithm change",
//                                JOptionPane.YES_NO_OPTION,
//                                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
//                            directory.setAlgorithm(combo.getSelectedItem().toString());
//                        } else {
//                            return;
//                        }
//                    }
                        dispose();
                    } catch (ProtException ex) {
                        logger.error(ex.toString());
                    }
                }
            }
        });
        add(ok);

        cancel = new JLabel(new ImageIcon(Constants.ASSETS.get("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(450, 140, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        add(cancel);




//        this.addWindowFocusListener(new WindowFocusListener() {
//            private boolean gained = false;
//
//            @Override
//            public void windowGainedFocus(WindowEvent e) {
//                gained = true;
//            }
//
//            @Override
//            public void windowLostFocus(WindowEvent e) {
//                if (gained) {
//                    dispose();
//                }
////                if (SwingUtilities.isDescendingFrom(e.getOppositeWindow(), ClickAwayDialog.this)) {
////                    return;
////                }
////                ClickAwayDialog.this.setVisible(false);
//            }
//        });

        this.setSize(570, 190);
        this.setUndecorated(true);
//        this.getContentPane().setBackground(new Color(239, 240, 241));
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setVisible(true);
    }

    private void check() {
        if ((!path.getText().equalsIgnoreCase(directory.OUTPUT_PATH) &&
                !path.getText().equalsIgnoreCase(directory.OUTPUT_PATH +"\\"))
//                || (!combo.getSelectedItem().toString().equals("---") &&
//                        !combo.getSelectedItem().toString().equals(directory.getAlgorithm()))
                ) {
            File pathFile = new File(path.getText());
            if(pathFile.exists() && pathFile.isDirectory() && pathFile.canRead() && pathFile.canWrite()){
                ok.setEnabled(true);
                return;
            }
        }

        ok.setEnabled(false);
    }

    private class PathChooserListener implements ActionListener {

        private JTextField field;

        private PathChooserListener(JTextField field) {
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final DirectoryChooser chooser = new DirectoryChooser("Choose Shared folder...");
            String directory = chooser.getDirectory();
            if(directory != null) {
                field.setText(directory);
            }
            check();
        }
    }


    @Override
    public void dispose(){
        instances.put(directory, null);
        super.dispose();
    }


}
