package pt.ua.sio.protbox.ui;

import ij.io.DirectoryChooser;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.User;
import pt.ua.sio.protbox.core.directory.Registry;
import pt.ua.sio.protbox.core.watcher.SpecificWatcher;
import pt.ua.sio.protbox.exception.ProtException;
import pt.ua.sio.protbox.util.AWTUtils;
import pt.ua.sio.protbox.util.DoubleRef;
import pt.ua.sio.protbox.util.TripleRef;
import pt.ua.sio.protbox.util.Uno;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class NewDirectory extends JFrame {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(NewDirectory.class);

    private final boolean firstTime;
    private boolean usedCloseButton;

    private int SPACING = 0;
    private Container previousCard;
    private JTextField path1;
    private JTextField path2;
    private JComboBox<String> combo1, combo2;
    private JButton go;

    private static NewDirectory instance;

    public static NewDirectory getInstance(final boolean firstTime) {
        if(instance ==null){
            instance = new NewDirectory(firstTime);
        } else {
            instance.toFront();
        }
        return instance;
    }

    private NewDirectory(final boolean firstTime){
        super("Monitor a new Directory - Protbox");
        this.setIconImage(Constants.ASSETS.get("box.png"));
        this.firstTime = firstTime;
        if(firstTime){
            SPACING = 120;
        }

        setContentPane(new PathConfigurationCard());

        setSize(570, SPACING + 290);
        this.setUndecorated(true);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setVisible(true);


        setResizable(false);
        setVisible(true);
    }

    public class PathConfigurationCard extends JPanel {
        public PathConfigurationCard(){
            super();
            setLayout(null);
            setBackground(Color.white);


            JLabel logoPane = new JLabel(new ImageIcon(Constants.ASSETS.get("protbox-splash.png")));
            logoPane.setBounds(0, 10, 500, 70);
            add(logoPane);


            JLabel close = new JLabel(new ImageIcon(Constants.ASSETS.get("close.png")));
            close.setLayout(null);
            close.setBounds(542, 7, 18, 18);
            close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            close.setForeground(Color.gray);
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    usedCloseButton = true;
                    dispose();
                }
            });
            this.add(close);


            if(firstTime){
                JXLabel firstTimeInfo = new JXLabel("Welcome to Protbox!\n\n" +
                        "You will need to configure at least one shared folder in order to use this application!\nPlease set the shared folder to be " +
                        "encrypted (like a Dropbox folder you are sharing with another user) and the output " +
                        "folder, where every file from the shared folder above will be decoded and available " +
                        "for normal usage.");
                firstTimeInfo.setLineWrap(true);
                firstTimeInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                firstTimeInfo.setBounds(20, 100, 520, SPACING);
                add(firstTimeInfo);
            } else {
                JXLabel info = new JXLabel("Monitor another shared folder for Protbox protection...");
                info.setLineWrap(true);
                info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                info.setBounds(20, 90, 520, 20);
                add(info);
            }


            JLabel label1 = new JLabel("Shared folder: ");
            label1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label1.setBounds(20, SPACING + 130, 100, 30);
            path1 = new JTextField();
            PromptSupport.setPrompt("<none selected>", path1);
            path1.setMargin(new Insets(0, 10, 0, 10));
            path1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            path1.setBorder(new CompoundBorder(new LineBorder(new Color(210,210,210), 1, false), new EmptyBorder(0, 3, 0, 0)));
            path1.setBounds(130, SPACING + 130, 341, 30);
            path1.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    check1();
                }
            });
            JButton b1 = new JButton("Choose ...");
            b1.addAncestorListener(new FocusOnDefaultListener()); // makes this component the focused object by default
            b1.setBorder(new LineBorder(Color.lightGray));
            b1.setBounds(470, SPACING + 130, 70, 30);
            add(label1);
            add(path1);
            add(b1);
            b1.addActionListener(new PathChooserListener("Shared", path1));



            JLabel label2 = new JLabel("Output folder: ");
            label2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label2.setBounds(20, SPACING + 170, 100, 30);
            path2 = new JTextField();
            PromptSupport.setPrompt("<none selected>", path2);
            path2.setMargin(new Insets(0, 10, 0, 10));
            path2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            path2.setBorder(new CompoundBorder(new LineBorder(new Color(210, 210, 210), 1, false), new EmptyBorder(0, 3, 0, 0)));
            path2.setBounds(130, SPACING + 170, 341, 30);
            path2.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    check1();
                }
            });
            JButton b2 = new JButton("Choose ...");
            b2.setBorder(new LineBorder(Color.lightGray));
            b2.setBounds(470, SPACING + 170, 70, 30);
            add(label2);
            add(path2);
            add(b2);
            b2.addActionListener(new PathChooserListener("Output", path2));


            go = new JButton("Next >");
            go.setBorder(new LineBorder(Color.lightGray));
            go.setBounds(450, SPACING+230, 90, 30);
            go.setEnabled(false);
            add(go);
            go.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    go1();
//                    dispose();
                }
            });
        }

    }



    // IF CONFIGURED DROPBOX FOLDER IS NOT CONFIGURED, LOADS THIS ONE
    public class NewUserListCard extends JPanel {

        public NewUserListCard(final File usersListFile){
            super();
            setLayout(null);
            setBackground(Color.white);


            JLabel logoPane = new JLabel(new ImageIcon(Constants.ASSETS.get("protbox-splash.png")));
            logoPane.setBounds(0, 10, 500, 70);
            add(logoPane);


            JLabel close = new JLabel(new ImageIcon(Constants.ASSETS.get("close.png")));
            close.setLayout(null);
            close.setBounds(542, 7, 18, 18);
            close.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            close.setForeground(Color.gray);
            close.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    usedCloseButton = true;
                    dispose();
                }
            });
            this.add(close);


            if(firstTime){
                JXLabel firstTimeInfo = new JXLabel("It looks like the configured shared folder is still not " +
                        "configured with Protbox. In order to first start using this folder, you will need to set " +
                        "the encryption algorithm and padding to be used with this folder's contents!");
                firstTimeInfo.setLineWrap(true);
                firstTimeInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                firstTimeInfo.setBounds(20, 100, 520, SPACING);
                add(firstTimeInfo);
            } else {
                JXLabel info = new JXLabel("Monitor another shared folder for Protbox protection...");
                info.setLineWrap(true);
                info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                info.setBounds(20, 90, 520, 20);
                add(info);
            }

            int newSpace = SPACING;
            if(!firstTime){
                newSpace = newSpace + 100;
            }

            JLabel label3 = new JLabel("Algorithm: ");
            label3.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label3.setBounds(20, newSpace+93, 80, 30);
            combo1 = new JComboBox<>();
            combo1.setBounds(90, newSpace+100, 120, 20);
            combo1.addItem("AES");
            combo1.addItem("DESede");
            add(label3);
            add(combo1);

            JLabel label4 = new JLabel("Padding: ");
            label4.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label4.setBounds(250, SPACING+93, 80, 30);
            combo2 = new JComboBox<>();
            combo2.setBounds(310, SPACING + 100, 120, 20);
            combo2.addItem("PKCS5Padding");
            combo2.addItem("NoPadding");
//            add(label4); // NOT IMPLEMENTED
//            add(combo2); // NOT IMPLEMENTED


            JButton previous = new JButton("< Previous");
            previous.setBorder(new LineBorder(Color.lightGray));
            previous.setBounds(20, SPACING+230, 90, 30);
            previous.setEnabled(true);
            add(previous);
            previous.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    go0();
                }
            });


            go = new JButton("Finish");
            go.setBorder(new LineBorder(Color.lightGray));
            go.setBounds(450, SPACING+230, 90, 30);
            go.setEnabled(true);
            add(go);
            go.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    go2(usersListFile);
                    usedCloseButton = false;
                    dispose();
                }
            });
        }
    }

    private void check1() {
        if (!path1.getText().equals("") &&
                !path2.getText().equals("")) {
            File path1File = new File(path1.getText());
            File path2File = new File(path2.getText());
            if(path1File.exists() && path1File.isDirectory() && path1File.canRead() && path1File.canWrite() &&
                    path2File.exists() && path2File.isDirectory() && path2File.canRead() && path2File.canWrite()){
                go.setEnabled(true);
                return;
            }
        }

        go.setEnabled(false);
    }

    private void go0(){
        Container aux = getContentPane();
        setContentPane(previousCard);
        previousCard = aux;
        revalidate();
        repaint();
    }

    private void go1(){
        Path protPath = Paths.get(path2.getText());
        Path dropPath = Paths.get(path1.getText());

        if(FolderValidation.validate(NewDirectory.this, protPath, dropPath, true)){


//            if (previousCard==null) {
            previousCard = getContentPane();

            File usersListFile = new File(dropPath.toString(), "»users");
            List<User> usersList = getUsersList(usersListFile);
            if (usersList == null)
                setContentPane(new NewUserListCard(usersListFile));
            else {
                dispose();
                PERMISSION_ASKING(dropPath, usersList);
            }
//            } else {
//                Container aux = getContentPane();
//                setContentPane(previousCard);
//                previousCard = aux;
//            }
            revalidate();
            repaint();
        }
    }

    private List<User> getUsersList(File usersListFile) {
        if(!usersListFile.exists())
            return null;
        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersListFile))){
            return (List<User>)in.readObject();
        }catch (IOException|ReflectiveOperationException ex) {
            return null;
        }
    }

    private void go2(File usersListFile){
        try{
            String algorithm = combo1.getSelectedItem().toString();

            // directory is completely new -> generate the single simetric key to be used from now on by all users
            KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
            addInstance(keygen.generateKey(), algorithm, true);

            List<User> userList = new ArrayList<>();
            userList.add(Main.initializedData.user);
            try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(usersListFile))){
                out.writeObject(userList);
                out.flush();
            }
        }catch (GeneralSecurityException|IOException ex){
            logger.error(ex.toString());
        }
    }

    private void PERMISSION_ASKING(final Path dropPath, List<User> list) {
        final UserList userList = UserList.getInstance(dropPath.getFileName().toString(), list, true);

        new Thread(){
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    if (userList.result != null) {
                        sendingAction(userList.result, dropPath);
                        return;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        logger.error(ex.toString());
                    }
                }
            }
        }.start();
    }
    private void sendingAction(Uno<User> resultingUser, final Path dropPath) {

        if (resultingUser.get() == null) { // was canceled
            go0();
            previousCard = null;
            setVisible(true);
            toFront();
        } else {
            File askFile = new File(dropPath.toFile(), "»ask" + resultingUser.get().getId());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(askFile))) {
                User thisUser = Main.initializedData.user;
                byte[] encodedPublicKey = Main.initializedData.encodedPublicKey;
                byte[] signatureBytes = Main.initializedData.signatureBytes;

                // SAVE THE SIGNED PUBLIC KEY, THE SIGNATURE AND THE USER DATA IN THE "»ASK" FILE
                out.writeObject(new TripleRef<>(thisUser, encodedPublicKey, signatureBytes));
                out.flush();
                waitForResponse(dropPath, askFile, Main.initializedData.privateKey);

            }catch (Exception ex){
                logger.error(ex.toString());
                ex.printStackTrace();
                System.exit(2);
            }
        }
    }

    private void waitForResponse(final Path dropPath, final File askFile, final PrivateKey privateKey) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final UserWaiting waitingWindow = UserWaiting.getInstance();
                try {
                    final Timer timer = new Timer();
                    final Thread thread = new Thread(new SpecificWatcher(dropPath, new SpecificWatcher.Process() {
                        @Override
                        public void run(File detectedFile) throws IOException, InterruptedException {
                            Thread.sleep(2000);
                            if (detectedFile.getName().contains("»invalid") &&
                                    detectedFile.getName().substring(8).equalsIgnoreCase(Main.initializedData.user.getId())) {
                                waitingWindow.dispose();
                                timer.cancel();
                                timer.purge();
                                Constants.delete(askFile);
                                JOptionPane.showMessageDialog(
                                        NewDirectory.this, "Your user data and authentication certificate were refused \n" +
                                        "by the user you asked permission to!\n\n",
                                        "Access Denied!",
                                        JOptionPane.ERROR_MESSAGE);
                                go0();
                                previousCard = null;
                                setVisible(true);
                                toFront();
                                Constants.delete(detectedFile);

                            } else if (detectedFile.getName().contains("»key") &&
                                    detectedFile.getName().substring(4).equalsIgnoreCase(Main.initializedData.user.getId())) {
                                System.out.println("detected key");
                                waitingWindow.dispose();
                                timer.cancel();
                                timer.purge();
                                Constants.delete(askFile);
                                Thread.sleep(2000);

                                // LOAD RECEIVED FILE WITH ALGORITHM AND ENCRYPTED KEY
                                try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(detectedFile))){
                                    DoubleRef<String,  byte[]> keyFile = (DoubleRef)in.readObject();
                                    in.close();
                                    Constants.delete(detectedFile);

                                    // DECRYPT KEY USING THE PREVIOUSLY GENERATED PRIVATE KEY
                                    String directoryAlgorithm = keyFile.pollFirst();

                                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                    c.init(Cipher.DECRYPT_MODE, privateKey);
                                    byte[] decryptedKey = c.doFinal(keyFile.pollSecond());
                                    SecretKey secretKey =new SecretKeySpec(
                                            decryptedKey,
                                            0,
                                            decryptedKey.length,
                                            directoryAlgorithm);

                                    addInstance(secretKey, directoryAlgorithm, false);

                                    // UPDATE USERS LIST TO ADD THIS USER IN IT!
                                    File usersListFile = new File(dropPath.toFile(), "»users");
                                    List<User> userList = new ArrayList<>();
                                    try(ObjectInputStream in2 = new ObjectInputStream(new FileInputStream(usersListFile))){
                                        userList = (List<User>)in2.readObject();
                                    }catch (IOException|ReflectiveOperationException ex) {
                                        logger.error(ex.toString());
                                    }
                                    userList.add(Main.initializedData.user);
                                    try(FileOutputStream fileOut = new FileOutputStream(usersListFile);
                                        ObjectOutputStream out = new ObjectOutputStream(fileOut)){
                                        fileOut.write(new String().getBytes());
                                        out.writeObject(userList);
                                        out.flush();
                                    }

                                }catch (GeneralSecurityException|IOException|ReflectiveOperationException ex) {
                                    waitingWindow.dispose();
                                    timer.cancel();
                                    timer.purge();
                                    Constants.delete(askFile);
                                    JOptionPane.showMessageDialog(
                                            NewDirectory.this, "The received file from the authenticator is unreadable!\n" +
                                            "Either the received key file was corrupt or you could not decrypt it.\n\n",
                                            "Access Denied!",
                                            JOptionPane.ERROR_MESSAGE);
                                    go0();
                                    previousCard = null;
                                    setVisible(true);
                                    toFront();
                                    Constants.delete(detectedFile);
                                }
                            }
                        }
                    }));
                    thread.start();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            thread.interrupt();
                            waitingWindow.dispose();
                            Constants.delete(askFile);
                            JOptionPane.showMessageDialog(
                                    NewDirectory.this, "The user you asked permission to took too long to respond.\n" +
                                    "Please try again later.\n\n",
                                    "Access Denied!",
                                    JOptionPane.ERROR_MESSAGE);
                            go0();
                            previousCard = null;
                            setVisible(true);
                            toFront();
                        }
                    }, 2 * 1000 * 60);

                } catch (IOException ex) {
                    logger.error(ex.toString());
                    ex.printStackTrace();
                }
            }
        });
    }



    private void addInstance(SecretKey key, String algorithm, boolean isANewDirectory){
        try{
            Registry directory = new Registry(Main.initializedData.user, path1.getText(), path2.getText(), algorithm, key, isANewDirectory);
            directory.initialize();
            JLabel l = new InstanceCell(directory);
            l.setMinimumSize(new Dimension(0, 50));
            l.setPreferredSize(new Dimension(0, 50));
            JPanel instanceList = TrayApplet.getInstance().instanceList;
            instanceList.add(l);
            instanceList.revalidate();
            instanceList.repaint();

            if(firstTime){
                Main.showTrayApplet();
            }

        }catch (ProtException|
                GeneralSecurityException|
                AWTException ex){
            JOptionPane.showMessageDialog(this, ex.toString());
        }
    }

    private class PathChooserListener implements ActionListener {

        private final String fieldName;
        private final JTextField field;

        private PathChooserListener(final String fieldName, final JTextField field) {
            this.fieldName = fieldName;
            this.field = field;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final DirectoryChooser chooser = new DirectoryChooser("Choose "+fieldName+" folder...");
            String directory = chooser.getDirectory();
            if(directory != null) {
                field.setText(directory);
            }
            check1();
        }
    }

    @Override
    public void dispose(){
        if (firstTime && usedCloseButton) {
            if (JOptionPane.showConfirmDialog(
                    NewDirectory.this,
                    "Are you sure you wish to cancel? You need to set at least one folder to use this application!\n" +
                            "Canceling this process will close the application.", "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                System.exit(1);
            }else{
                usedCloseButton = false;
            }
        } else {
            instance = null;
            super.dispose();
        }
    }
}
