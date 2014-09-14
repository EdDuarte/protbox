package edduarte.protbox.ui.window;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.core.CertificateData;
import edduarte.protbox.Main;
import edduarte.protbox.ui.listeners.OnMouseClick;
import edduarte.protbox.utils.Callback;
import edduarte.protbox.utils.Ref;
import edduarte.protbox.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.x509.X509CertImpl;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class eIDTokenLoadingWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(eIDTokenLoadingWindow.class);

    private static eIDTokenLoadingWindow instance;
    private JLabel info, cancel;
    private ImageIcon loadingIcon = new ImageIcon(new File(Constants.INSTALL_DIR, "vfl3Wt7C").getAbsolutePath());


    private eIDTokenLoadingWindow(final String providerName, final Callback<Ref.Double<User, CertificateData>> callback) {
        super("Validating your Citizen Card - Protbox");
        this.setIconImage(Constants.getAsset("box.png"));
        this.setLayout(null);

        info = new JLabel();
        setInfoWithLooking();
        info.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
        info.setBounds(20, 20, 200, 30);
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
                    String alias = Main.pkcs11Providers.get(providerName);
                    logger.info("Card was found!!!");
                    t.cancel();

                    setInfoWithLoading();
                    Ref.Double<User, CertificateData> entry = getCertificateData(alias, ks);

                    setInfoWithUser(entry.first);
                    Thread.sleep(2000);

                    callback.onResult(Ref.of2(entry.first, entry.second));
                    t.cancel();
                    dispose();

                } catch (Exception ex) {
                    if (Constants.verbose) {
                        logger.info("Card was NOT found... Trying again in 3 seconds.");
                    }
                }
            }
        }, 0, 3000);
    }

    public static eIDTokenLoadingWindow showPrompt(String providerName,
                                              Callback<Ref.Double<User, CertificateData>> callback) {
        if (instance == null) {
            instance = new eIDTokenLoadingWindow(providerName, callback);
        } else {
            instance.show();
            instance.toFront();
        }
        return instance;
    }

    private static Ref.Double<User, CertificateData> getCertificateData(String alias, KeyStore ks) {
        Certificate cert = null;
        try {
            ks.load(null, null);

            cert = ks.getCertificate(alias);
            cert.verify(cert.getPublicKey());
            Certificate[] chain = ks.getCertificateChain(alias);
            for (Certificate c : chain) {
                c.verify(cert.getPublicKey());
            }

            if (ks.isKeyEntry(alias)) {
                PrivateKey privateKey = (PrivateKey) ks.getKey(alias, null);
                X509Certificate c = new X509CertImpl(cert.getEncoded());

                // GENERATE A NEW KEYPAIR TO USE IN FOLDER SHARING REQUESTS AND RESPONSES
                KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
                byte[] encodedPublicKey = pair.getPublic().getEncoded();


                //SIGNING
//                Signature sig1 = Signature.getInstance("SHA1withRSA");
                Signature sig1 = Signature.getInstance(c.getSigAlgName());
                sig1.initSign(privateKey);
                sig1.update(encodedPublicKey);
                byte[] signatureBytes = sig1.sign();

                // Gets user first and last name and cc number
                String name = c.getSubjectDN().getName();
                String ccNumber = c.getSerialNumber().toString();
                logger.info(name + " " + ccNumber);

                // RETURN CERTIFICATE, GENERATED PAIR AND SIGNATURE BYTES
                User user = new User(
                        chain, // Certificate chain
                        c, // X509 certificate
                        ccNumber, // user cc number
                        name, // user name
                        InetAddress.getLocalHost().getHostName(), // machine name
                        Utils.getSerialNumber()); // machine serial number

                CertificateData data = new CertificateData(encodedPublicKey, signatureBytes, pair.getPrivate());

                return Ref.of2(user, data);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "You must insert you digital authentication certificate code in order to use this application!\n" +
                            "Please run the application again and insert you digital authentication certificate code!",
                    "Invalid Digital Signature Code!", JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }
        return null;
    }

    private void setInfoWithLooking() {
        info.setText("Please insert your citizen card...");
        info.setIcon(new ImageIcon(Constants.getAsset("id-card.png")));
    }

    private void setInfoWithLoading() {
        info.setIcon(loadingIcon);
        info.setText("Loading Card info ...");
    }

    private void setInfoWithUser(User user) {
        info.setText(user.toString());
        info.setSize(430, 135);
        info.setIconTextGap(JLabel.RIGHT);
        info.setFont(new Font(Constants.FONT, Font.PLAIN, 14));

        JLabel machineName = new JLabel();
        machineName.setText("Machine Name: " + user.getMachineName());
        machineName.setFont(new Font(Constants.FONT, Font.PLAIN, 12));
        machineName.setBounds(125, 100, 370, 50);
        add(machineName);


        cancel.setVisible(false);
        this.setSize(470, 170);
        Utils.setComponentLocationOnCenter(this);
        this.revalidate();
        this.repaint();
    }
}
