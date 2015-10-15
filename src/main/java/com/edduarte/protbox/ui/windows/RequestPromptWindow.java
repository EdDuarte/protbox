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

import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.core.PbxPair;
import com.edduarte.protbox.core.PbxUser;
import com.edduarte.protbox.core.keyexchange.Response;
import com.edduarte.protbox.core.registry.PReg;
import com.edduarte.protbox.core.watcher.RequestWatcher;
import com.edduarte.protbox.utils.Utils;
import com.edduarte.protbox.utils.listeners.OnMouseClick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.TimerTask;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class RequestPromptWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(RequestPromptWindow.class);


    private RequestPromptWindow(final PbxPair pair,
                                final String sharedFolderName,
                                final RequestWatcher.Result request) {
        super("A new user asked your permission to access the folder " + sharedFolderName + " - Protbox");
        setIconImage(Constants.getAsset("box.png"));
        setLayout(null);

        PbxUser requestingUser = request.getRequestingUser();

        // verifies if the user's certificate and its chain is valid
        try {
            Date date = Constants.getToday();
            requestingUser.getUserCertificate().checkValidity(date);
            Certificate[] chain = requestingUser.getCertificateChain();
            for (Certificate c : chain) {
                ((X509Certificate) c).checkValidity(date);
            }
        } catch (CertificateException ex) {
            logger.info("User certificate is not valid! Ignoring request." + ex);
            return;
        }

        try {
            // verifies if public key of certificate is valid with provided signature
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(requestingUser.getUserCertificate().getPublicKey());
            sig.update(request.getEncodedUserPublicKey());
            boolean verifyResult = sig.verify(request.getSignatureByteArray());
            if (!verifyResult) {
                logger.info("Signature is not valid! Ignoring request.");
                return;
            }
        } catch (GeneralSecurityException ex) {
            logger.info("There was a problem reading and validating the signature! Ignoring request." + ex);
            return;
        }


        JLabel info = new JLabel();
        info.setText(requestingUser.toString());
        info.setBounds(20, 20, 430, 135);
        info.setIconTextGap(JLabel.RIGHT);
        info.setFont(Constants.FONT.deriveFont(14f));
        add(info);


        JLabel allow = new JLabel(new ImageIcon(Constants.getAsset("allow.png")));
        allow.setLayout(null);
        allow.setBounds(110, 175, 122, 39);
        allow.setBackground(Color.black);
        allow.addMouseListener((OnMouseClick) e -> generateResponseFile(request, pair));
        add(allow);

        JLabel deny = new JLabel(new ImageIcon(Constants.getAsset("deny.png")));
        deny.setLayout(null);
        deny.setBounds(260, 175, 122, 39);
        deny.setBackground(Color.black);
        deny.addMouseListener((OnMouseClick) e -> dispose());
        add(deny);

        setSize(470, 220);
        setUndecorated(true);
        getContentPane().setBackground(Color.white);
        setBackground(Color.white);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        Utils.setComponentLocationOnCenter(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setVisible(true);

        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dispose();
            }
        }, 90000);

    }


    public static RequestPromptWindow getInstance(final PReg registry, final RequestWatcher.Result request) {
        String sharedFolderName = registry.getPair().getSharedFolderFile().getName();
        return new RequestPromptWindow(registry.getPair(), sharedFolderName, request);
    }


    private void generateResponseFile(RequestWatcher.Result request,
                                      PbxPair pair) {
        try {

            // generates signed public key from encoded bytes
            PublicKey userPublicKey = KeyFactory
                    .getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(request.getEncodedUserPublicKey()));


            // encrypts symmetric key from directory with public key
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, userPublicKey);
            byte[] encryptedPairKey = c.doFinal(pair.getPairKey().getEncoded());

//            c.init(Cipher.ENCRYPT_MODE, userPublicKey);
//            byte[] encryptedIntegrityKey = c.doFinal(pair.getIntegrityKey().getEncoded());

            // saves pair algorithm and encrypted pair key in response file
            Response response = new Response(pair.getPairAlgorithm(), encryptedPairKey);
            request.createResponseFile(response);

            dispose();

        } catch (GeneralSecurityException | IOException ex) {
            logger.info("There was a problem writing the pair key to the response file! Ignoring request." + ex);
        }
    }
}
