package edduarte.protbox;

import com.google.common.collect.Lists;
import edduarte.protbox.core.CertificateData;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.PbxUser;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.panels.PairPanel;
import edduarte.protbox.ui.windows.InsertPasswordWindow;
import edduarte.protbox.ui.windows.NewRegistryWindow;
import edduarte.protbox.ui.windows.ProviderListWindow;
import edduarte.protbox.ui.windows.eIDTokenLoadingWindow;
import edduarte.protbox.utils.tuples.Pair;
import edduarte.protbox.utils.tuples.Single;
import ij.io.DirectoryChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
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
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Security;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class Main {
    public static final Map<String, String> pkcs11Providers = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        try {
            // Lifts JCA restrictions on AES key length
            Class security_class = Class.forName("javax.crypto.JceSecurity");
            Field restricted_field = security_class.getDeclaredField("isRestricted");
            restricted_field.setAccessible(true);
            restricted_field.set(null, false);

        } catch (ReflectiveOperationException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static PbxUser user;
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
        FileFilter fileFilter = new AndFileFilter(
                new WildcardFileFilter(Lists.newArrayList("*.config")), HiddenFileFilter.VISIBLE);

        File[] providersConfigFiles = new File(Constants.PROVIDERS_DIR).listFiles(fileFilter);

        if (providersConfigFiles != null) {
            for (File f : providersConfigFiles) {
                try {
                    List<String> lines = FileUtils.readLines(f);
                    String aliasLine = lines
                            .stream()
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
                    if (ex.getMessage().equals("Initialization failed")) {
                        ex.printStackTrace();

                        String s = "The following error occurred:\n"
                                + ex.getCause().getMessage()+"\n\nIn addition, make sure you have "
                                + "an available smart card reader connected before opening the application.";
                        JTextArea textArea = new JTextArea(s);
                        textArea.setColumns(60);
                        textArea.setLineWrap(true);
                        textArea.setWrapStyleWord(true);
                        textArea.setSize(textArea.getPreferredSize().width, 1);

                        JOptionPane.showMessageDialog(
                                null, textArea, "Error loading PKCS11 provider", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    } else {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null,
                                "Error while setting up PKCS11 provider from configuration file " + f.getName() +
                                        ".\n" + ex.getMessage(),
                                "Error loading PKCS11 provider", JOptionPane.ERROR_MESSAGE);
                    }
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
            eIDTokenLoadingWindow.showPrompt(providerName, (returnedUser, returnedCertificateData) -> {
                user = returnedUser;
                certificateData = returnedCertificateData;

                // gets a password to use on the saved registry files (for loading and saving)
                final Single<Consumer<SecretKey>> consumerHolder = Single.of(null);
                consumerHolder.value = password -> {
                    registriesPasswordKey = password;
                    try {
                        // if there are serialized files, load them if they can be decoded by this user's private key
                        final List<Pair<File, byte[]>> serializedDirectoryFiles = new ArrayList<>();
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
                                        serializedDirectoryFiles.add(Pair.of(f, realData));
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
                            trayApplet.repaint();
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
                        insertPassword(consumerHolder.value);
                    }
                };
                insertPassword(consumerHolder.value);
            });
        });
    }


    private static void insertPassword(Consumer<SecretKey> passwordKeyConsumer) {
        InsertPasswordWindow.showPrompt(pw -> {
            // decode the serialized directories using the password
            // if password results in error, this registry does not belong to this user
            pw = pw + pw + pw + pw;

            SecretKey sKey = new SecretKeySpec(pw.getBytes(), "AES");
            passwordKeyConsumer.accept(sKey);
        });
    }


    private static void loadRegistry(final List<Pair<File, byte[]>> dataList) throws
    IOException,
            ReflectiveOperationException,
            GeneralSecurityException,
            ProtException {

        for (Pair<File, byte[]> pair : dataList) {
            File serialized = pair.first;
            if (Constants.verbose) {
                logger.info("Reading {}...", serialized);
            }

            try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(pair.pollSecond()))) {

                PReg reg = (PReg) stream.readObject();
                if (reg.getUser().equals(user)) {

                    if (!reg.getPair().getSharedFolderFile().exists()) {

                        // shared folder from registry was deleted
                        JOptionPane.showMessageDialog(
                                null, "The shared folder at " + reg.getPair().getSharedFolderPath() + "\n" +
                                        "was either deleted, moved or renamed while Protbox wasn't running!\n" +
                                        "Unfortunately this means that this registry will be deleted and you\n" +
                                        "will need to ask for permission again, requiring your Citizen Card.\n" +
                                        "But don't worry! Your decoded files will be kept on the output folder.",
                                "Shared Folder was deleted!",
                                JOptionPane.ERROR_MESSAGE);
                        reg.stop();
                        Constants.delete(serialized);

                    } else if (!reg.getPair().getProtFolderFile().exists()) {
                        changeProtPath(reg, serialized);

                    } else {
                        // start the registry
                        reg.initialize();
                        PairPanel l = new PairPanel(reg);
                        trayApplet.addPairPanel(l);
                        if (Constants.verbose) {
                            logger.info("Added registry " + reg.id + " to instance list...");
                        }
                    }
                }
            }
        }

        if (trayApplet.getPairPanels().length == 0) {
            // there are no instances left!
            hideTrayApplet();
        }
    }


    private static void exit() {
        // if there are directories, save them into serialized files
        if (user != null && trayApplet != null && trayApplet.getPairPanels().length != 0) {
            if (Constants.verbose) {
                logger.info("Serializing and saving directories...");
            }
            saveAllRegistries();
        }
    }


    private static void saveAllRegistries() {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            for (PairPanel c : trayApplet.getPairPanels()) {
                PReg toSerialize = c.getRegistry();

                // stops the registry, which stops the running threads and processes
                toSerialize.stop();
                File file = new File(Constants.REGISTRIES_DIR, toSerialize.id);

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
        } catch (GeneralSecurityException | IOException ex) {
            if (Constants.verbose) {
                logger.info("Error while saving registries.", ex);
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


    public static PbxUser getUser() {
        return user;
    }


    public static CertificateData getCertificateData() {
        return certificateData;
    }
}
