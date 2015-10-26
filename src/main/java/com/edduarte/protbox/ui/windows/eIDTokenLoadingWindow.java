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

package com.edduarte.protbox.ui.windows;

import com.edduarte.protbox.Protbox;
import com.edduarte.protbox.core.CertificateData;
import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.core.PbxUser;
import com.edduarte.protbox.utils.TokenParser;
import com.edduarte.protbox.utils.Utils;
import com.edduarte.protbox.utils.listeners.OnMouseClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.x509.X509CertImpl;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;

/**
 * @author Ed Duarte (<a href="mailto:ed@edduarte.com">ed@edduarte.com</a>)
 * @version 2.0
 */
public class eIDTokenLoadingWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(eIDTokenLoadingWindow.class);

    private static eIDTokenLoadingWindow instance;

    private JLabel info, cancel;

    private ImageIcon loadingIcon = new ImageIcon(new File(Constants.INSTALL_DIR, "vfl3Wt7C").getAbsolutePath());


    private eIDTokenLoadingWindow(final String providerName, final BiConsumer<PbxUser, CertificateData> consumer) {
        super("Validating your Citizen Card - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);

        info = new JLabel();
        setInfoWithLooking();
        info.setFont(Constants.FONT);
        info.setBounds(20, 20, 250, 30);
        add(info);


        cancel = new JLabel(new ImageIcon(Constants.getAsset("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(250, 15, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener((OnMouseClick) e -> {
            if (JOptionPane.showConfirmDialog(
                    eIDTokenLoadingWindow.this, "You need to authenticate yourself in order to use this application!\n" +
                            "Are you sure you want to cancel and quit the application?\n\n",
                    "Confirm quit application",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                dispose();
                System.exit(2);
            }
        });
        add(cancel);

        this.setSize(370, 73);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);

        // tests every 3 seconds if card was added
        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setInfoWithLooking();
                try {
                    Provider p = Security.getProvider(providerName);
                    KeyStore ks = KeyStore.getInstance("PKCS11", p);
                    String alias = Protbox.pkcs11Providers.get(providerName);
                    if (Constants.verbose) {
                        logger.info("eID Token was found!");
                    }
                    t.cancel();

                    setInfoWithLoading();

                    try {
                        ks.load(null, null);

                        Certificate cert = ks.getCertificate(alias);
                        Certificate[] chain = ks.getCertificateChain(alias);

                        if (ks.isKeyEntry(alias)) {
                            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, null);
                            X509Certificate c = new X509CertImpl(cert.getEncoded());

                            // generates a new key-pair to use in folder sharing requests and responses
                            KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
                            byte[] encodedPublicKey = pair.getPublic().getEncoded();


                            // obtains signature to use in key requests
                            Signature sig1 = Signature.getInstance(c.getSigAlgName());
                            sig1.initSign(privateKey);
                            sig1.update(encodedPublicKey);
                            byte[] signatureBytes = sig1.sign();

                            // gets user name and cc number
                            TokenParser parser = TokenParser.parse(c);
                            if (Constants.verbose) {
                                logger.info("Token parser results: {} {}", parser.getUserName(), parser.getSerialNumber());
                            }

                            // returns certificate, generated pair and signature bytes
                            PbxUser user = new PbxUser(
                                    chain,                     // certificate chain
                                    c,                         // X509 certificate
                                    parser.getSerialNumber(),  // user cc number
                                    parser.getUserName()       // user name
                            );

                            CertificateData data = new CertificateData(encodedPublicKey, signatureBytes, pair.getPrivate());

                            setInfoWithUser(user);
                            Thread.sleep(2000);

                            consumer.accept(user, data);
                            t.cancel();
                            dispose();
                        }
                    } catch (CertificateException | KeyStoreException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "You must insert you digital authentication certificate code in order to use this application!\n" +
                                        "Please run the application again and insert you digital authentication certificate code!",
                                "Invalid Digital Signature Code!", JOptionPane.ERROR_MESSAGE);
                        System.exit(2);
                    } catch (GeneralSecurityException | IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(2);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (Constants.verbose) {
                        logger.info("Card was NOT found... Trying again in 3 seconds.");
                    }
                }
            }
        }, 0, 3000);
    }


    public static eIDTokenLoadingWindow showPrompt(final String providerName,
                                                   final BiConsumer<PbxUser, CertificateData> consumer) {
        if (instance == null) {
            instance = new eIDTokenLoadingWindow(providerName, consumer);
        } else {
            instance.setVisible(true);
            instance.toFront();
        }
        return instance;
    }


    private void setInfoWithLooking() {
        info.setText("Please insert your eID token...");
        info.setIcon(new ImageIcon(Constants.getAsset("id-card.png")));
    }


    private void setInfoWithLoading() {
        info.setIcon(loadingIcon);
        info.setText("Reading token data ...");
    }


    private void setInfoWithUser(PbxUser user) {
        info.setText(user.toString());
        info.setSize(430, 135);
        info.setIconTextGap(JLabel.RIGHT);
        info.setFont(Constants.FONT.deriveFont(14f));

        cancel.setVisible(false);
        this.setSize(470, 170);
        Utils.setComponentLocationOnCenter(this);
        this.revalidate();
        this.repaint();
    }
}
