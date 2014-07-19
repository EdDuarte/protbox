package pt.ua.sio.protbox.ui;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.slf4j.LoggerFactory;
import pt.gov.cartaodecidadao.*;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.User;
import pt.ua.sio.protbox.util.*;
import pt.ua.sio.protbox.util.DuoRef;
import pt.ua.sio.protbox.util.DoubleRef;
import pt.ua.sio.protbox.util.TripleRef;
import sun.security.x509.X509CertImpl;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Timer;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class CardLookup extends JFrame {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(CardLookup.class);

    private JLabel info, cancel;
    private ImageIcon loadingIcon = new ImageIcon(new File(Constants.INSTALL_DIR, "vfl3Wt7C").getAbsolutePath());
    private static CardLookup instance;

    public static CardLookup getInstance() {
        if(instance==null){
            instance = new CardLookup();
        } else {
            instance.toFront();
        }
        return instance;
    }

    private CardLookup() {
        super("Validating your Citizen Card - Protbox");
        this.setIconImage(Constants.ASSETS.get("box.png"));
        this.setLayout(null);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (JOptionPane.showConfirmDialog(
                        CardLookup.this, "You need to authenticate yourself in order to use this application!\n" +
                        "Are you sure you want to cancel and quit the application?\n\n",
                        "Confirm quit application",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(2);
                }
            }
        };


        info = new JLabel();
        setInfoWithLooking();
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setBounds(20, 20, 200, 30);
        add(info);


        cancel = new JLabel(new ImageIcon(Constants.ASSETS.get("cancel.png")));
        cancel.setLayout(null);
        cancel.setBounds(250, 15, 122, 39);
        cancel.setBackground(Color.black);
        cancel.addMouseListener(ma);
        add(cancel);

        this.setSize(370, 73);
        this.setUndecorated(true);
        this.getContentPane().setBackground(Color.white);
        this.setBackground(Color.white);
        this.setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
        AWTUtils.setComponentLocationOnCenter(this);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setVisible(true);

        start();
    }

    private void setInfoWithLooking(){
        info.setText("Please insert your citizen card...");
        info.setIcon(new ImageIcon(Constants.ASSETS.get("id-card.png")));
    }

    private void setInfoWithLoading(){
        info.setIcon(loadingIcon);
        info.setText("Loading Card info ...");
    }

    private void setInfoWithUser(User user){
        info.setIcon(user.getPhoto());
        info.setText(user.toString());
        info.setSize(430, 135);
        info.setIconTextGap(JLabel.RIGHT);
        info.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel machineName = new JLabel();
        machineName.setText("Machine Name: "+user.getMachineName());
        machineName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        machineName.setBounds(125, 100, 370, 50);
        add(machineName);


        cancel.setVisible(false);
        this.setSize(470, 170);
        AWTUtils.setComponentLocationOnCenter(this);
        this.revalidate();
        this.repaint();
    }

    private void start() {
        // tests every 3 seconds if card was added
        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try{
                    setInfoWithLooking();
                    Provider p = Security.getProvider("SunPKCS11-CartaoCidadao");
                    KeyStore ks = KeyStore.getInstance("PKCS11", p);
                    logger.info("Card was found!!!");
                    t.cancel();

                    setInfoWithLoading();
                    TripleRef<X509Certificate, DoubleRef<byte[], PrivateKey>, byte[]> entry = getCertificateAndKeyPair(ks);
                    DoubleRef<DuoRef<String>, ImageIcon> userData = readCardInfo();
                    User loadedUser = new User(
                            userData.getFirst().getFirst(), // CC number
                            userData.getFirst().getSecond(), // name
                            InetAddress.getLocalHost().getHostName(), // machine name
                            MachineUtils.getSerialNumber(), // machine serial number
                            userData.getSecond(), // photo
                            entry.getFirst()); // x509 certificate


                    setInfoWithUser(loadedUser);
                    Thread.sleep(2000);


                    Main.initializedData = new Main.InitializedData(
                            loadedUser,                     // this loaded User object (with X509 certificate in it)
                            entry.getSecond().pollSecond(), // the generated private key for communication
                            entry.pollSecond().pollFirst(), // the generated public key in encoded form
                            entry.pollThird());             // the signature bytes from the public key
                    CardLookup.this.dispose();
                }catch (Exception ex) {
                    logger.info("Card was NOT found...");
                }
            }
        }, 0, 3000);
    }

    private static TripleRef<X509Certificate, DoubleRef<byte[], PrivateKey>, byte[]> getCertificateAndKeyPair(KeyStore ks){
        try{
            String alias = "CITIZEN SIGNATURE CERTIFICATE";
            ks.load(null, null);

            Certificate cert = ks.getCertificate(alias);
            if(ks.isKeyEntry(alias)){
                PrivateKey keyStorePrivateKey = (PrivateKey)ks.getKey(alias, null);
                X509Certificate c = new X509CertImpl(cert.getEncoded());


                // GENERATE A NEW KEYPAIR TO USE IN COMMUNICATIONS!
                KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
                byte[] encodedPublicKey = pair.getPublic().getEncoded();


                //SIGINING
                Signature sig1 = Signature.getInstance("SHA1withRSA");
                sig1.initSign(keyStorePrivateKey);
                sig1.update(encodedPublicKey);
                byte[] signatureBytes = sig1.sign();


                // RETURN CERTIFICATE, GENERATED PAIR AND SIGNATURE BYTES
                return new TripleRef<>(c, new DoubleRef<>(encodedPublicKey, pair.getPrivate()), signatureBytes);
            }
        }catch (Exception ex){
            JOptionPane.showMessageDialog(null, "You must insert you digital signature code in order to use this application!\n" +
                    "Please run the application again and insert you digital signature code!",
                    "Invalid Digital Signature Code!", JOptionPane.ERROR_MESSAGE);
            System.exit(2);
        }
        return null;
    }


    private static DoubleRef<DuoRef<String>, ImageIcon> readCardInfo() throws PTEID_Exception, IOException {
        String id = null, name = null;
        ImageIcon photo = null;
        PTEID_ReaderSet.initSDK();

        PTEID_ReaderSet readerSet = PTEID_ReaderSet.instance();
        for(int i = 0; i<readerSet.readerCount(); i++){

            PTEID_ReaderContext context = readerSet.getReaderByNum(i);

            if (context.isCardPresent()){
                PTEID_EIDCard card = context.getEIDCard();
                PTEID_EId eid = card.getID();

                // get the id
                id = eid.getCivilianIdNumber();

                // get the full name
                name = eid.getGivenName() + " " + eid.getSurname();

                // get and resize the photo
                PTEID_ByteArray pteid_photo = eid.getPhotoObj().getphoto();
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(pteid_photo.GetBytes()));
                ImagePlus imp_big = new ImagePlus(id+"photo", bi);
                ImageProcessor ip_big = imp_big.getProcessor();
                ip_big.setInterpolate(true);
                ImageProcessor ip_small = ip_big.resize(101, 134);
                ImagePlus small = new ImagePlus("small", ip_small);
                photo = new ImageIcon(small.getBufferedImage());
            }
        }

        PTEID_ReaderSet.releaseSDK();
        return new DoubleRef<>(new DuoRef<>(id, name), photo);
    }
}
