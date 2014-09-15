package edduarte.protbox;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.core.CertificateData;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;
import edduarte.protbox.ui.window.eIDTokenLoadingWindow;
import edduarte.protbox.ui.window.InsertPasswordWindow;
import edduarte.protbox.ui.window.NewRegistryWindow;
import edduarte.protbox.ui.window.ProviderListWindow;
import edduarte.protbox.utils.Callback;
import edduarte.protbox.utils.Ref;
import ij.io.DirectoryChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs11.SunPKCS11;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.security.*;
import java.util.*;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static final Map<String, String> pkcs11Providers = new HashMap<>();

    private static User user;
    private static CertificateData certificateData;
    private static TrayApplet trayApplet;
    private static SystemTray tray;
    private static SecretKey registriesPasswordKey;


    public static void main(String... args) {

        // activate debug / verbose mode
        if (args.length != 0) {
            List<String> argsList = Arrays.asList(args);
            if (argsList.contains("-v")) {
                Constants.verbose = true;
            }
        }

        // use System's look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // If the System's look and feel is not obtainable, continue execution with JRE look and feel
        }

        // check this is a single instance
        try {
            new ServerSocket(1882);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Another instance of Protbox is already running.\n" +
                            "Please close the other instance or contact your administrator.",
                    "Protbox already running", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // check if System Tray is supported by this operative system
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Your operative system does not support system tray functionality.\n" +
                            "Please try running Protbox on another operative system.",
                    "System tray not supported", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // add PKCS11 providers
        File[] providersConfigFiles = new File(Constants.PROVIDERS_DIR).listFiles();

        if (providersConfigFiles != null) {
            for (File f : providersConfigFiles) {
                try {
                    List<String> lines = FileUtils.readLines(f);
                    String aliasLine = lines.stream()
                            .filter(line -> line.contains("alias"))
                            .findFirst()
                            .get();
                    lines.remove(aliasLine);
                    String alias = aliasLine.split("=")[1].trim();

                    StringBuilder sb = new StringBuilder();
                    for (String s : lines) {
                        sb.append(s);
                        sb.append("\n");
                    }

                    Provider p = new SunPKCS11(new ReaderInputStream(new StringReader(sb.toString())));
                    Security.addProvider(p);

                    pkcs11Providers.put(p.getName(), alias);

                } catch (IOException | ProviderException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "Error while setting up PKCS11 provider from configuration file " + f.getName() +
                                    ".\n"+ex.getMessage(),
                            "Error loading PKCS11 provider", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        // adds a shutdown hook to save instantiated directories into files when the application is being closed
        Runtime.getRuntime().addShutdownHook(new Thread(Main::exit));


        // get system tray and run tray applet
        tray = SystemTray.getSystemTray();
        SwingUtilities.invokeLater(() -> {

            if (Constants.verbose) {
                logger.info("Starting application");
            }

            //Start a new TrayApplet object
            trayApplet = TrayApplet.getInstance();
        });


        // prompts the user to choose which provider to use
        ProviderListWindow.showWindow(Main.pkcs11Providers.keySet(), providerName -> {

            // loads eID token
            eIDTokenLoadingWindow.showPrompt(providerName, result -> {
                user = result.pollFirst();
                certificateData = result.pollSecond();

                // gets a password to use on the saved registry files (for loading and saving)
                final Ref.Single<Callback<SecretKey>> callbackRef = Ref.of1(null);
                callbackRef.value = password -> {
                    registriesPasswordKey = password;
                    try {
                        // if there are serialized files, load them if they can be decoded by this user's private key
                        final List<Ref.Double<File, byte[]>> serializedDirectoryFiles = new ArrayList<>();
                        if (Constants.verbose) {
                            logger.info("Reading serialized registry files...");
                        }

                        File[] registryFileList = new File(Constants.REGISTRIES_DIR).listFiles();
                        if (registryFileList != null) {
                            for (File f : registryFileList) {
                                if (f.isFile()) {
                                    byte[] data = FileUtils.readFileToByteArray(f);
                                    try {
                                        Cipher cipher = Cipher.getInstance("AES");
                                        cipher.init(Cipher.DECRYPT_MODE, registriesPasswordKey);
                                        byte[] realData = cipher.doFinal(data);
                                        serializedDirectoryFiles.add(Ref.of2(f, realData));
                                    } catch (GeneralSecurityException ex) {
                                        if (Constants.verbose) {
                                            logger.info("Inserted Password does not correspond to " + f.getName());
                                        }
                                    }
                                }
                            }
                        }

                        // if there were no serialized directories, show NewDirectory window to configure the first folder
                        if (serializedDirectoryFiles.isEmpty() || registryFileList == null) {
                            if (Constants.verbose) {
                                logger.info("No registry files were found: running app as first time!");
                            }
                            NewRegistryWindow.start(true);

                        } else { // there were serialized directories
                            loadRegistry(serializedDirectoryFiles);
                            trayApplet.instanceList.revalidate();
                            trayApplet.instanceList.repaint();
                            showTrayApplet();
                        }

                    } catch (AWTException |
                            IOException |
                            GeneralSecurityException |
                            ReflectiveOperationException |
                            ProtException ex) {

                        JOptionPane.showMessageDialog(
                                null, "The inserted password was invalid! Please try another one!",
                                "Invalid password!",
                                JOptionPane.ERROR_MESSAGE);
                        insertPassword(callbackRef.value);
                    }
                };
                insertPassword(callbackRef.value);

            });
        });
    }


    private static void insertPassword(Callback<SecretKey> passwordKeyCallback) {
        InsertPasswordWindow.showPrompt(pw -> {
            // decode the serialized directories using the password
            // if password results in error, this registry does not belong to this user
            pw = pw + pw + pw + pw;

            SecretKey sKey = new SecretKeySpec(pw.getBytes(), "AES");
            passwordKeyCallback.onResult(sKey);
        });
    }


    private static void loadRegistry(final List<Ref.Double<File, byte[]>> dataList) throws
            IOException,
            ReflectiveOperationException,
            GeneralSecurityException,
            ProtException {

        for (Ref.Double<File, byte[]> pair : dataList) {
            File serialized = pair.first;
            if (Constants.verbose) {
                logger.info("Reading {}...", serialized);
            }

            try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(pair.pollSecond()))) {

                PReg reg = (PReg) stream.readObject();
                if (reg.user.equals(user)) {

                    if (!new File(reg.SHARED_PATH).exists()) {

                        // shared folder from registry was deleted
                        JOptionPane.showMessageDialog(
                                null, "The shared folder at " + reg.SHARED_PATH + "\n" +
                                        "was either deleted, moved or renamed while Protbox wasn't running!\n" +
                                        "Unfortunately this means that this registry will be deleted and you\n" +
                                        "will need to ask for permission again, requiring your Citizen Card.\n" +
                                        "But don't worry! Your decoded files will be kept on the output folder.",
                                "Shared Folder was deleted!",
                                JOptionPane.ERROR_MESSAGE);
                        reg.stop();
                        Constants.delete(serialized);

                    } else if (!new File(reg.PROT_PATH).exists()) {
                        changeProtPath(reg, serialized);

                    } else {
                        // start the registry
                        reg.initialize();
                        PairPanel l = new PairPanel(reg);
                        trayApplet.instanceList.add(l);
                        if (Constants.verbose) {
                            logger.info("Added registry " + reg.ID + " to instance list...");
                        }
                    }
                }
            }
        }

        if (trayApplet.instanceList.getComponentCount() == 0) {
            // there are no instances left!
            hideTrayApplet();
        }
    }


    private static void exit() {
        // if there are directories, save them into serialized files
        if (user != null && trayApplet != null && trayApplet.instanceList.getComponents().length != 0) {
            if (Constants.verbose) {
                logger.info("Serializing and saving directories...");
            }
            saveAllRegistries();
        }
    }


    private static void saveAllRegistries() {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            for (Component c : trayApplet.instanceList.getComponents()) {
                if (c.getClass().getSimpleName().toLowerCase().equalsIgnoreCase("InstanceCell")) {
                    PReg toSerialize = ((PairPanel) c).getRegistry();

                    // stops the registry, which stops the running threads and processes
                    toSerialize.stop();
                    File file = new File(Constants.REGISTRIES_DIR, toSerialize.ID);

                    // encrypt directories using the inserted password at the beginning of the application
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                         ObjectOutputStream stream = new ObjectOutputStream(out)) {
                        stream.writeObject(toSerialize);
                        stream.flush();

                        byte[] data = out.toByteArray();
                        cipher.init(Cipher.ENCRYPT_MODE, registriesPasswordKey);
                        data = cipher.doFinal(data);

                        FileUtils.writeByteArrayToFile(file, data);

                    } catch (GeneralSecurityException ex) {
                        logger.error("Invalid password! Registry {} not saved!", toSerialize.toString());
                        ex.printStackTrace();
                    }
                }
            }
        } catch (GeneralSecurityException | IOException ex) {
            if (Constants.verbose) {
                logger.info("Error whole saving registries.", ex);
            }
        }
    }


    private static void changeProtPath(PReg reg, File serializedDirectory) throws ProtException {

        if (JOptionPane.showConfirmDialog(
                null, "The prot folder from one of your registries\n" +
                        "was deleted while Protbox wasn't running!\n" +
                        "Do you wish to set a new folder to place the decoded files from the shared folder?\n" +
                        "CHOOSING \"NO\" WILL DELETE THE REGISTRY AND YOU WILL LOSE ACCESS TO THE FILES " +
                        "IN THE SHARED FOLDER!",
                "Prot Folder was deleted!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            final DirectoryChooser chooser = new DirectoryChooser("Choose Output folder...");
            String newOutputPath = chooser.getDirectory();
            if (newOutputPath != null) {
                reg.changeProtPath(newOutputPath);
            } else {
                changeProtPath(reg, serializedDirectory);
            }

        } else {
            reg.stop();
            serializedDirectory.deleteOnExit();
        }
    }


    public static void showTrayApplet() throws AWTException {
        tray.add(trayApplet.trayIcon);
    }


    public static void hideTrayApplet() {
        tray.remove(trayApplet.trayIcon);
    }


    public static User getUser() {
        return user;
    }


    public static CertificateData getCertificateData() {
        return certificateData;
    }
}
