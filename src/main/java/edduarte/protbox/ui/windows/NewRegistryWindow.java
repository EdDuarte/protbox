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

import edduarte.protbox.Protbox;
import edduarte.protbox.core.CertificateData;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderValidation;
import edduarte.protbox.core.PbxPair;
import edduarte.protbox.core.PbxUser;
import edduarte.protbox.core.keyexchange.Request;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.watcher.ResponseWatcher;
import edduarte.protbox.exception.ProtboxException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.listeners.OnMouseClick;
import ij.io.DirectoryChooser;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class NewRegistryWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(NewRegistryWindow.class);

    private static NewRegistryWindow instance;

    private final boolean firstTime;

    private boolean usedCloseButton;

    private int SPACING = 0;

    private Container previousCard;

    private JTextField path1;

    private JTextField path2;

    private JComboBox<String> combo1, combo2;

    private JButton go;


    private NewRegistryWindow(final boolean firstTime) {
        super("Create new directory pair - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.firstTime = firstTime;
        if (firstTime) {
            SPACING = 100;
        }

        setContentPane(new PathConfigurationCard());

        setSize(700, SPACING + 310);
        setUndecorated(true);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setVisible(true);
    }


    public static NewRegistryWindow start(final boolean firstTime) {
        if (instance == null) {
            instance = new NewRegistryWindow(firstTime);
        } else {
            instance.setVisible(true);
            instance.toFront();
        }
        return instance;
    }


    private void check1() {
        if (!path1.getText().equals("") && !path2.getText().equals("")) {
            File path1File = new File(path1.getText());
            File path2File = new File(path2.getText());
            if (path1File.exists() && path1File.isDirectory() && path1File.canRead() && path1File.canWrite() &&
                    path2File.exists() && path2File.isDirectory() && path2File.canRead() && path2File.canWrite()) {
                go.setEnabled(true);
                return;
            }
        }
        go.setEnabled(false);
    }


    private void go0() {
        Container aux = getContentPane();
        setContentPane(previousCard);
        previousCard = aux;
        revalidate();
        repaint();
    }


    private void go1() {
        Path protPath = Paths.get(path2.getText());
        Path sharedPath = Paths.get(path1.getText());

        int resultCode = FolderValidation.validate(NewRegistryWindow.this, protPath, sharedPath, true);
        previousCard = getContentPane();

        if (resultCode == FolderValidation.RESULT_CODE_NEW_REGISTRY) {
            setContentPane(new ConfigureNewRegistryCard());

        } else if (resultCode == FolderValidation.RESULT_CODE_EXISTING_REGISTRY) {
            dispose();
            requestKey(sharedPath);
        }
        revalidate();
        repaint();
    }


    private void go2() {
        try {
            String encryptionAlgorithm = combo1.getSelectedItem().toString();
            String cipherMode = combo2.getSelectedItem().toString();

            String algorithm = encryptionAlgorithm + "/" + cipherMode + "/" + "PKCS5Padding";

            // registry is completely new -> generate the single symmetric key to be used from now on by all users
            KeyGenerator pairKeyGen = KeyGenerator.getInstance(encryptionAlgorithm);
            pairKeyGen.init(256);
            SecretKey pairKey = pairKeyGen.generateKey();
//            SecretKey integrityKey = new SecretKeySpec(pairKey.getEncoded(), "HmacSHA512");
            addInstance(pairKey/*, integrityKey*/, algorithm, true);

        } catch (GeneralSecurityException ex) {
            logger.info(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, ex.toString());
        }
    }


    private void requestKey(final Path sharedFolderPath) {

        String requestHash = Utils.generateRandomHash();
        String requestFileName = Constants.SPECIAL_FILE_FIRST_CHAR + requestHash;

        File requestFile = new File(sharedFolderPath.toFile(), requestFileName);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(requestFile))) {

            PbxUser thisUser = Protbox.getUser();
            byte[] encodedPublicKey = Protbox.getCertificateData().getEncodedPublicKey();
            byte[] signatureBytes = Protbox.getCertificateData().getSignatureBytes();

            // saves the signed public key, the signature and the user data in the request file
            out.writeObject(new Request(thisUser, encodedPublicKey, signatureBytes));
            out.flush();


            final Timer timer = new Timer();
            final WaitingForResponseWindow waitingWindow = WaitingForResponseWindow.getInstance();

            AtomicReference<ResponseWatcher> watcherRef = new AtomicReference<>();
            ResponseWatcher w = new ResponseWatcher(sharedFolderPath, requestHash, response -> {
                try {
                    CertificateData certificateData = Protbox.getCertificateData();

                    // decrypts key using the previously generated private key
                    Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");

                    c.init(Cipher.DECRYPT_MODE, certificateData.getExchangePrivateKey());
                    byte[] decryptedPairKey = c.doFinal(response.encryptedPairKey);

//                    c.init(Cipher.DECRYPT_MODE, certificateData.getExchangePrivateKey());
//                    byte[] decryptedIntegrityKey = c.doFinal(response.encryptedIntegrityKey);

                    SecretKey pairKey = new SecretKeySpec(decryptedPairKey, "AES");
//                    SecretKey integrityKey = new SecretKeySpec(decryptedIntegrityKey, "HmacSHA512");

                    addInstance(pairKey,/* integrityKey,*/ response.directoryAlgorithm, false);

                    waitingWindow.dispose();
                    ResponseWatcher watcher = watcherRef.get();
                    if (watcher != null) {
                        watcher.interrupt();
                        watcherRef.set(null);
                    }
                    timer.cancel();
                    timer.purge();

                } catch (GeneralSecurityException ex) {
                    logger.info("The response file from the replier is unreadable! Ignoring file.", ex);
                }
            });
            watcherRef.set(w);
            w.start();

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    waitingWindow.dispose();
                    ResponseWatcher watcher = watcherRef.get();
                    if (watcher != null) {
                        watcher.interrupt();
                        watcherRef.set(null);
                    }
                    JOptionPane.showMessageDialog(
                            NewRegistryWindow.this, "Your request timed-out! Check if your certificate and\n" +
                                    "exchange keys are valid and try again.\n",
                            "Access Denied!",
                            JOptionPane.ERROR_MESSAGE);
                    go0();
                    previousCard = null;
                    setVisible(true);
                    toFront();
                }
            }, 2 * 1000 * 60);

        } catch (Exception ex) {
            logger.error(ex.toString());
            ex.printStackTrace();
            System.exit(2);
        }
    }


    private void addInstance(SecretKey encryptionKey/*, SecretKey integrityKey*/, String algorithm, boolean isANewDirectory) {
        try {
            PbxPair pair =
                    new PbxPair(path1.getText(), path2.getText(), algorithm, encryptionKey/*, integrityKey*/);

            PReg registry = new PReg(Protbox.getUser(), pair, isANewDirectory);
            registry.initialize();

            PairPanel pairPanel = new PairPanel(registry);
            pairPanel.setMinimumSize(new Dimension(0, 50));
            pairPanel.setPreferredSize(new Dimension(0, 50));
            TrayApplet.getInstance().addPairPanel(pairPanel);

            if (firstTime) {
                Protbox.showTrayApplet();
            }

        } catch (ProtboxException | IOException | GeneralSecurityException | AWTException ex) {
            logger.info(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, ex.toString());
        }
    }


    @Override
    public void dispose() {
        if (firstTime && usedCloseButton) {
            if (JOptionPane.showConfirmDialog(
                    NewRegistryWindow.this,
                    "Are you sure you wish to cancel? You need to set at least one folder to use this application!\n" +
                            "Canceling this process will close the application.", "Confirm Cancel",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                System.exit(1);
            } else {
                usedCloseButton = false;
            }
        } else {
            instance = null;
            super.dispose();
        }
    }


    /**
     * The card that is used to set the shared folder's and the prot folder's paths.
     */
    public class PathConfigurationCard extends JPanel {
        public PathConfigurationCard() {
            super();
            setLayout(null);
            setBackground(Color.white);

            JLabel logoPane = new JLabel(new ImageIcon(Constants.getAsset("splash.png")));
            logoPane.setBounds(5, 5, 700, 158);
            add(logoPane);


            JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
            close.setLayout(null);
            close.setBounds(672, 7, 18, 18);
            close.setFont(Constants.FONT);
            close.setForeground(Color.gray);
            close.addMouseListener((OnMouseClick) e -> {
                usedCloseButton = true;
                dispose();
            });
            this.add(close);


            if (firstTime) {
                JXLabel firstTimeInfo = new JXLabel("Welcome to Protbox!\n\n" +
                        "You will need to configure at least one pair of folders in order to use this application!\nPlease set the shared folder to be " +
                        "encrypted (like a cloud folder you are sharing with another user) and the prot " +
                        "folder, where every file from the shared folder above will be decoded and available " +
                        "for normal usage.");
                firstTimeInfo.setLineWrap(true);
                firstTimeInfo.setFont(Constants.FONT.deriveFont(13f));
                firstTimeInfo.setBounds(20, 160, 680, 90);
                add(firstTimeInfo);
            }


            JLabel label1 = new JLabel("Shared folder: ");
            label1.setFont(Constants.FONT);
            label1.setBounds(20, SPACING + 170, 100, 30);
            path1 = new JTextField();
            PromptSupport.setPrompt("<none selected>", path1);
            path1.setMargin(new Insets(0, 10, 0, 10));
            path1.setFont(Constants.FONT);
            path1.setBorder(new CompoundBorder(
                    new LineBorder(new Color(210, 210, 210), 1, false),
                    new EmptyBorder(0, 3, 0, 0)
            ));
            path1.setBounds(130, SPACING + 170, 470, 30);
            path1.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    check1();
                }
            });
            JButton b1 = new JButton("Choose ...");
            b1.addAncestorListener(new AncestorListener() {

                @Override
                public void ancestorAdded(AncestorEvent e) {
                    JComponent component = e.getComponent();
                    component.requestFocusInWindow();
                }


                @Override
                public void ancestorMoved(AncestorEvent e) {
                    // nothing here, not needed
                }


                @Override
                public void ancestorRemoved(AncestorEvent e) {
                    // nothing here, not needed
                }
            });
            b1.setBorder(new LineBorder(Color.lightGray));
            b1.setBounds(610, SPACING + 170, 70, 30);
            add(label1);
            add(path1);
            add(b1);
            b1.addActionListener(new PathChooserListener("Shared folder", path1));


            JLabel label2 = new JLabel("Prot folder: ");
            label2.setFont(Constants.FONT);
            label2.setBounds(20, SPACING + 210, 100, 30);
            path2 = new JTextField();
            PromptSupport.setPrompt("<none selected>", path2);
            path2.setMargin(new Insets(0, 10, 0, 10));
            path2.setFont(Constants.FONT);
            path2.setBorder(new CompoundBorder(new LineBorder(new Color(210, 210, 210), 1, false), new EmptyBorder(0, 3, 0, 0)));
            path2.setBounds(130, SPACING + 210, 470, 30);
            path2.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    check1();
                }
            });
            JButton b2 = new JButton("Choose ...");
            b2.setBorder(new LineBorder(Color.lightGray));
            b2.setBounds(610, SPACING + 210, 70, 30);
            add(label2);
            add(path2);
            add(b2);
            b2.addActionListener(new PathChooserListener("Prot folder", path2));


            go = new JButton("Next >");
            go.setBorder(new LineBorder(Color.lightGray));
            go.setBounds(590, SPACING + 260, 90, 30);
            go.setEnabled(false);
            add(go);
            go.addActionListener(e -> go1());
        }
    }


    /**
     * The card that is used when the set shared folder was not yet configured.
     */
    private class ConfigureNewRegistryCard extends JPanel {

        public ConfigureNewRegistryCard() {
            super();
            setLayout(null);
            setBackground(Color.white);

            JLabel logoPane = new JLabel(new ImageIcon(Constants.getAsset("splash.png")));
            logoPane.setBounds(5, 5, 700, 158);
            add(logoPane);


            JLabel close = new JLabel(new ImageIcon(Constants.getAsset("close.png")));
            close.setLayout(null);
            close.setBounds(672, 7, 18, 18);
            close.setFont(Constants.FONT);
            close.setForeground(Color.gray);
            close.addMouseListener((OnMouseClick) e -> {
                usedCloseButton = true;
                dispose();
            });
            this.add(close);


            JXLabel info = new JXLabel("Set the encryption algorithm and mode that will be used to protect your files:");
            info.setLineWrap(true);
            info.setFont(Constants.FONT.deriveFont(13f));
            info.setBounds(20, 160, 680, 30);
            add(info);

            int newSpace = SPACING;
            if (!firstTime) {
                newSpace = newSpace + 100;
            }

            JLabel label3 = new JLabel("Algorithm: ");
            label3.setFont(Constants.FONT);
            label3.setBounds(20, newSpace + 113, 80, 30);
            combo1 = new JComboBox<>();
            combo1.setBounds(90, newSpace + 120, 120, 20);
            combo1.addItem("AES");
            add(label3);
            add(combo1);

            JLabel label4 = new JLabel("Cipher Mode: ");
            label4.setFont(Constants.FONT);
            label4.setBounds(250, newSpace + 113, 80, 30);
            combo2 = new JComboBox<>();
            combo2.setBounds(340, newSpace + 120, 120, 20);
            combo2.addItem("ECB");
            combo2.addItem("CBC");
            add(label4);
            add(combo2);


            JButton previous = new JButton("< Previous");
            previous.setBorder(new LineBorder(Color.lightGray));
            previous.setBounds(20, SPACING + 260, 90, 30);
            previous.setEnabled(true);
            add(previous);
            previous.addActionListener(e -> go0());


            go = new JButton("Finish");
            go.setBorder(new LineBorder(Color.lightGray));
            go.setBounds(590, SPACING + 260, 90, 30);
            go.setEnabled(true);
            add(go);
            go.addActionListener(e -> {
                go2();
                usedCloseButton = false;
                dispose();
            });
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
            final DirectoryChooser chooser = new DirectoryChooser("Choose " + fieldName + "...");
            String directory = chooser.getDirectory();
            if (directory != null) {
                field.setText(directory);
            }
            check1();
        }
    }
}
