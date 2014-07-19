package pt.ua.sio.protbox.ui;

import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.Constants;
import pt.ua.sio.protbox.core.User;
import pt.ua.sio.protbox.core.directory.Registry;
import pt.ua.sio.protbox.util.AWTUtils;
import pt.ua.sio.protbox.util.DoubleRef;
import pt.ua.sio.protbox.util.TripleRef;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class UserValidation extends JFrame {
    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(UserValidation.class);

    private JLabel info, allow, deny;

    public static UserValidation getInstance(final Registry directory, final String algorithm, final SecretKey key, final String sharedFolderName, final File askFile) {
        return new UserValidation(directory, algorithm, key, sharedFolderName, askFile);
    }

    private UserValidation(final Registry directory, final String algorithm, final SecretKey directoryKey, final String sharedFolderName, final File askFile) {
        super("A new user asked your permission to access the folder "+sharedFolderName+" - Protbox");
        this.setIconImage(Constants.ASSETS.get("box.png"));
        this.setLayout(null);

        final File parent = askFile.getParentFile();
        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(askFile))){
            final TripleRef<User, byte[], byte[]> receivedFileData = (TripleRef<User, byte[], byte[]>)in.readObject();
            Constants.delete(askFile);
            final User newUser = receivedFileData.getFirst();
            final byte[] encodedPublicKey = receivedFileData.getSecond();
            final byte[] signature = receivedFileData.getThird();

            // CHECK IF THIS USER ALREADY EXISTS IN THE RECEIVED MACHINE SERIAL ID
            File usersListFile = new File(directory.SHARED_PATH, "»users");
            try(ObjectInputStream in2 = new ObjectInputStream(new FileInputStream(usersListFile))){
                List<User> userList = (List<User>)in2.readObject();
                if(userList.contains(newUser)){
                    generateInvalidFile(parent, newUser);
                    return;
                }

            }catch(IOException|ReflectiveOperationException ex) {
                logger.error(ex.toString());
                return;
            }

            // VERIFY IF CERTIFICATE IS VALID
            try{
                newUser.getCertificate().checkValidity(Constants.getToday());
            }catch (CertificateException ex){
                generateInvalidFile(parent, newUser);
                return;
            }

            try{
                // VERIFY SIGNATURE FROM SENT DATA
                Signature sig = Signature.getInstance("SHA1withRSA");
                sig.initVerify(newUser.getCertificate().getPublicKey());
                sig.update(encodedPublicKey);
                boolean verifyResult = sig.verify(signature);
                System.out.println(verifyResult);
                if(!verifyResult){
                    generateInvalidFile(parent, newUser);
                    return;
                }
            } catch (GeneralSecurityException ex) {
                generateInvalidFile(parent, newUser);
                return;
            }


            info = new JLabel();
            info.setIcon(newUser.getPhoto());
            info.setText(newUser.toString());
            info.setBounds(20, 20, 430, 135);
            info.setIconTextGap(JLabel.RIGHT);
            info.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            add(info);



            JLabel machineName = new JLabel();
            machineName.setText("Machine Name: "+newUser.getMachineName());
            machineName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            machineName.setBounds(125, 100, 370, 50);
            add(machineName);


            allow = new JLabel(new ImageIcon(Constants.ASSETS.get("allow.png")));
            allow.setLayout(null);
            allow.setBounds(110, 175, 122, 39);
            allow.setBackground(Color.black);
            allow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    generateKeyFile(parent, newUser, algorithm, directoryKey, encodedPublicKey);
                }
            });
            add(allow);

            deny = new JLabel(new ImageIcon(Constants.ASSETS.get("deny.png")));
            deny.setLayout(null);
            deny.setBounds(260, 175, 122, 39);
            deny.setBackground(Color.black);
            deny.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    generateInvalidFile(parent, newUser);
                }
            });
            add(deny);



            askFile.deleteOnExit();


            this.setSize(470, 220);
            this.setUndecorated(true);
            this.getContentPane().setBackground(Color.white);
            this.setBackground(Color.white);
            this.setResizable(false);
            getRootPane().setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)));
            AWTUtils.setComponentLocationOnCenter(this);
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            this.setVisible(true);

            new java.util.Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    dispose();
                }
            }, 90000);

        }catch (IOException|ReflectiveOperationException ex) {
            logger.error(ex.toString());
            ex.printStackTrace();
        }
    }

    private void generateKeyFile(File parent, User newUser, String algorithm, SecretKey directoryKey, byte[] encodedPublicKey){
        try {

            // GENERATE SIGNED PUBLIC KEY FROM ENCODED BYTES
            PublicKey newUserPKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(encodedPublicKey));


            // ENCRYPT SYMMETRIC KEY FROM DIRECTORY WITH PUBLIC KEY
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, newUserPKey);
            byte[] encodedKey = c.doFinal(directoryKey.getEncoded());


            // SAVE DIRECTORY'S ALGORITHM AND ENCRYPTED KEY IN FILE
            File keyFile = new File(parent, "»key" + newUser.getId());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keyFile))) {
                out.writeObject(new DoubleRef<>(algorithm, encodedKey));
            }

            dispose();

        } catch (GeneralSecurityException|IOException ex) {
            generateInvalidFile(parent, newUser);
        }
    }

    private void generateInvalidFile(File parent, User newUser){
        try {
            File invalidFile = new File(parent, "»invalid"+newUser.getId());
            invalidFile.createNewFile();
            dispose();
        } catch (IOException ex) {
            logger.error(ex.toString());
            ex.printStackTrace();
        }
    }
}
