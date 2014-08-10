package edduarte.protbox;

import edduarte.protbox.ui.*;
import ij.io.DirectoryChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.core.directory.Registry;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.util.DoubleRef;
import sun.security.pkcs11.SunPKCS11;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.security.*;
import java.util.*;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class Main {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Main.class);

    private static TrayApplet trayApplet;
    private static SystemTray tray;
    public static InitializedData initializedData;
    private static SecretKey serializedDirectoriesPasswordKey;


    public static class InitializedData {
        public final User user;
        public final PrivateKey privateKey;
        public final byte[] encodedPublicKey, signatureBytes;

        public InitializedData(final User user, PrivateKey privateKey, byte[] encodedPublicKey, byte[] signatureBytes) {
            this.user = user;                         // the user read from the inserted card
            this.privateKey = privateKey;             // the generated private key
            this.encodedPublicKey = encodedPublicKey; // the generated public key (signed)
            this.signatureBytes = signatureBytes;     // the signature
        }
    }

    public static void unzipDLLFile(String resourceName) throws IOException {
        File temp = new File(Constants.INSTALL_DIR, resourceName);
        try (InputStream in = Main.class.getResourceAsStream(File.separator + resourceName);
             FileOutputStream out = new FileOutputStream(temp)) {

            IOUtils.copy(in, out);
            out.close();
        }
    }

    private static final List<String> pkcs11Providers = new ArrayList<>();

    static {
        pkcs11Providers.add("pteidpkcs11.dll");

        try {
            for (String s : pkcs11Providers) {
                unzipDLLFile(s);
            }

            unzipDLLFile("pteidlibJava_Wrapper.dll");

            System.setProperty("java.library.path", Constants.INSTALL_DIR);
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);

            System.loadLibrary("pteidlibJava_Wrapper");

        } catch (IOException | UnsatisfiedLinkError | NoSuchFieldException | IllegalAccessException ex) {
            logger.error("Native code library failed to load.");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String... args) {

        // activate Debug mode
        if(args.length != 0) {
            List<String> argsList = Arrays.asList(args);
            if(argsList.contains("-v")){
                Constants.verboseMode = true;
            }
        }

        // use System's look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch (Exception ex) {
            // If the System's look and feel is not obtainable, continue execution with JRE look and feel
        }

        // check this is a single instance
//        try{
//            new ServerSocket(1882);
//        }catch (IOException ex){
//            JOptionPane.showMessageDialog(null, "Another instance of Protbox is already running.\n" +
//                    "Please close the other instance or contact your administrator.",
//                    "Protbox already running", JOptionPane.ERROR_MESSAGE);
//            System.exit(1);
//        }

        // check if System Tray is supported by this operative system
        if(!SystemTray.isSupported()){
            JOptionPane.showMessageDialog(null, "Your operative system does not support system tray functionality.\n" +
                    "Please try running Protbox on another operative system.",
                    "System tray not supported", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // add PKCS11 providers
        File configFile = new File(Constants.INSTALL_DIR, "config");
        for (String providerName : pkcs11Providers) {
            File libraryFile = new File(Constants.INSTALL_DIR, providerName);
            String s = "name="+providerName+"\n" + "library="+libraryFile.getAbsolutePath();

            try{
                FileUtils.write(configFile, s, "UTF-8", false);
                Provider p = new SunPKCS11(new FileInputStream(configFile));
                Security.addProvider(p);
            }catch (IOException|ProviderException ex) {
                JOptionPane.showMessageDialog(null, "Error while setting up PKCS11 provider with name "+providerName+
                                ".\nPlease install the application again!",
                        "Error loading PKCS11 provider", JOptionPane.ERROR_MESSAGE);
            }
        }


        // makes sure if the desired PKCS11 provider exists
//        Provider p = Security.getProvider("SunPKCS11-CartaoCidadao");
//        if(p==null){
//            JOptionPane.showMessageDialog(null, "Could not load PKCS11 provider! Please make sure PKCS11 provider is configured.",
//                    "PKCS11 not found", JOptionPane.ERROR_MESSAGE);
//            System.exit(1);
//        }


        // adds a shutdown hook to save instantiated directories into files when the application is being closed
        Runtime.getRuntime().addShutdownHook(new Thread(Main::exit));


        // get system tray and run tray applet
        tray = SystemTray.getSystemTray();
        SwingUtilities.invokeLater(() -> {
            if (Constants.verboseMode)
                if (Constants.verboseMode) logger.info("Starting application");
            //Start a new TrayApplet object
            trayApplet = TrayApplet.getInstance();
        });

        start();
    }

    private static void start() {

        // loads card
        CardLookup.getInstance();

        // wait for data from card
        new Thread(){
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if(initializedData !=null){
                            // gets a password to use on the saved directory files (for loading and saving)
                            insertPassword();
                            return;
                        }
                        Thread.sleep(100);
                    }catch (InterruptedException ex) {
                        logger.error(ex.toString());
                        ex.printStackTrace();
                    }
                }
            }
        }.start();

        // wait for a password to be inserted
        new Thread(){
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if(serializedDirectoriesPasswordKey !=null){

                            // if there are serialized files, load them if they can be decoded by this user's private key
                            final List<DoubleRef<File, byte[]>> serializedDirectoryFiles = new ArrayList<>();
                            File installDir = new File(Constants.INSTALL_DIR);
                            if(Constants.verboseMode) logger.info("Reading serialized directory files...");

                            File[] list = installDir.listFiles();
                            if(list!=null) {
                                for(File f : list){
                                    if(f.getName().contains("dir") && f.isFile()){
                                        byte[] data = FileUtils.readFileToByteArray(f);
                                        try{
                                            Cipher cipher = Cipher.getInstance("DESede");
                                            cipher.init(Cipher.DECRYPT_MODE, serializedDirectoriesPasswordKey);
                                            byte[] realData = cipher.doFinal(data);
                                            serializedDirectoryFiles.add(new DoubleRef<>(f, realData));
                                        }catch (GeneralSecurityException ex) {
                                            logger.info("Inserted Password does not correspond to " + f.getName());
                                        }
                                    }
                                }
                            }

                            // if there were no serialized directories, show NewDirectory window to configure the first folder
                            if(serializedDirectoryFiles.isEmpty() || list == null){
                                if(Constants.verboseMode) logger.info("No directory files were found: running app as first time!");
                                NewDirectory.getInstance(true);
                            }
                            else{ // there were serialized directories
                                load(serializedDirectoryFiles);
                                trayApplet.instanceList.revalidate();
                                trayApplet.instanceList.repaint();
                                showTrayApplet();
                            }
                            break;
                        }

                        Thread.sleep(100);
                    }catch (ProtException|AWTException|IOException|InterruptedException ex) {
                        logger.error(ex.toString());
                        ex.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private static void insertPassword() {
        final PasswordAsker asker = PasswordAsker.getInstance();
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String pw = asker.result;
                if (pw != null) {
                    try {
                        // decode the serialized directories using the password
                        // if password results in error, this directory does not belong to this user

                        // TODO
//                        pw = pw + pw + pw + pw;
//                        serializedDirectoriesPasswordKey = new SecretKeySpec(pw.getBytes(), "AES");

                        SecretKeyFactory factory = SecretKeyFactory.getInstance("DESede");
                        pw = pw + pw + pw + pw;
                        serializedDirectoriesPasswordKey = factory.generateSecret(new DESedeKeySpec(pw.getBytes()));


                    }catch (GeneralSecurityException ex) {
                        JOptionPane.showMessageDialog(
                                null, "The inserted password was invalid! Please try another one!",
                                "Invalid password!",
                                JOptionPane.ERROR_MESSAGE);
                        serializedDirectoriesPasswordKey = null;
                        insertPassword();
                    }
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    logger.error(ex.toString());
                }
            }
        }).start();
    }

    private static void load(List<DoubleRef<File, byte[]>> dataList) throws ProtException {
        try{
            for(DoubleRef<File, byte[]> pair : dataList) {
                File serialized = pair.getFirst();
                if(Constants.verboseMode) logger.info("Reading {}...", serialized);
                ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(pair.pollSecond()));
                Registry directory = (Registry) stream.readObject();

                if(!directory.accessUser.equals(initializedData.user)){
                    // this directory does not belong to this user, so skip it!
                    continue;
                } else if(!new File(directory.SHARED_PATH).exists()){
                    JOptionPane.showMessageDialog(
                            null, "The shared folder at "+directory.SHARED_PATH +"\n" +
                            "was either deleted, moved or renamed while Protbox wasn't running!\n" +
                            "Unfortunately this means that this directory will be deleted and you\n" +
                            "will need to ask for permission again, requiring your Citizen Card.\n" +
                            "But don't worry! Your decoded files will be kept on the output folder.",
                            "Shared Folder was deleted!",
                            JOptionPane.ERROR_MESSAGE);
                    directory.stop();
                    Constants.delete(serialized);
                } else if(!new File(directory.OUTPUT_PATH).exists()){
                    changeOutputPath(directory, serialized);
                } else {
                    // update the »users file to show this user as available
                    File usersListFile = new File(directory.SHARED_PATH, "»users");
                    List<User> userList = new ArrayList<>();
                    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersListFile))){
                        userList = (List<User>)in.readObject();
                    }catch (IOException|ReflectiveOperationException ex) {
                        logger.error(ex.toString());
                    }
                    for(User u : userList){
                        if(u.equals(initializedData.user)){
                            u.makeAvailable();
                            break;
                        }
                    }
                    try(FileOutputStream fileOut = new FileOutputStream(usersListFile);
                        ObjectOutputStream out = new ObjectOutputStream(fileOut)){
                        fileOut.write(new String().getBytes());
                        out.writeObject(userList);
                        out.flush();
                    }

                    // start the directory
                    directory.initialize();
                    JLabel l = new InstanceCell(directory);
                    l.setMinimumSize(new Dimension(0, 50));
                    l.setPreferredSize(new Dimension(0, 50));
                    trayApplet.instanceList.add(l);
                    if(Constants.verboseMode) logger.info("Added directory "+directory.NAME +" to instance list...");
                }
            }

            if(trayApplet.instanceList.getComponentCount()==0){
                // there are no instances left!
                hideTrayApplet();
            }

        } catch(IOException |
                ReflectiveOperationException |
                GeneralSecurityException ex){
            throw new ProtException(ex);
        }
    }

    private static void exit() {

        // if there are directories, save them into serialized files
        if(initializedData !=null && trayApplet!=null && trayApplet.instanceList!=null && trayApplet.instanceList.getComponents().length!=0){
            if(Constants.verboseMode) logger.info("Serializing and saving directories...");

            try{
                Cipher cipher = Cipher.getInstance("DESede");
                save(cipher);
            }catch (GeneralSecurityException|IOException ex){
                ex.printStackTrace();
            }
        }
    }

    private static void save(final Cipher cipher) throws IOException {
        for(Component c : trayApplet.instanceList.getComponents()){
            if(c.getClass().getSimpleName().toLowerCase().equalsIgnoreCase("InstanceCell")){
                Registry toSerialize = ((InstanceCell)c).getProtReg();


                // update the »users file to show this user as unavailable
                File usersListFile = new File(toSerialize.SHARED_PATH, "»users");
                System.out.println(usersListFile);
                List<User> userList = new ArrayList<>();
                try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(usersListFile))){
                    userList = (List<User>)in.readObject();
                }catch (IOException|ReflectiveOperationException ex) {
                    logger.error(ex.toString());
                }
                for(User u : userList){
                    if(u.getId().equalsIgnoreCase(initializedData.user.getId())){
                        u.makeUnavailable();
                        break;
                    }
                }
                try(FileOutputStream fileOut = new FileOutputStream(usersListFile);
                    ObjectOutputStream out = new ObjectOutputStream(fileOut)){
                    fileOut.write(new String().getBytes());
                    out.writeObject(userList);
                    out.flush();
                }


                // stops the directory, which stops the running threads and processes
                toSerialize.stop();
                logger.info(Constants.INSTALL_DIR);
                File file = new File(Constants.INSTALL_DIR, toSerialize.NAME);


                // encrypt directories using the inserted password at the beginning of the application
                try(ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ObjectOutputStream stream = new ObjectOutputStream(out)){
                    stream.writeObject(toSerialize);
                    stream.flush();

                    byte[] data = out.toByteArray();
                    cipher.init(Cipher.ENCRYPT_MODE, serializedDirectoriesPasswordKey);
                    data = cipher.doFinal(data);

                    FileUtils.writeByteArrayToFile(file, data);
                } catch (GeneralSecurityException ex) {
                    logger.error("Password invalid! Directory not saved!");
                    ex.printStackTrace();
                }
            }
        }

    }

    private static void changeOutputPath(Registry directory, File serializedDirectory) throws ProtException {
        if (JOptionPane.showConfirmDialog(
                null, "The output folder from one of your directories\n" +
                "was deleted while Protbox wasn't running!\n" +
                "Do you wish to set a new folder to place the decoded files from the shared folder?\n" +
                "CHOOSING \"NO\" WILL DELETE THE DIRECTORY!",
                "Output Folder was deleted!",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            final DirectoryChooser chooser = new DirectoryChooser("Choose Output folder...");
            String newOutputPath = chooser.getDirectory();
            if(directory != null) {
                directory.changeOutputPath(newOutputPath);
            }else{
                changeOutputPath(directory, serializedDirectory);
            }
        } else {
            directory.stop();
            serializedDirectory.deleteOnExit();
        }
    }

    public static void showTrayApplet() throws AWTException {
        tray.add(trayApplet.trayIcon);
    }

    public static void hideTrayApplet() {
        tray.remove(trayApplet.trayIcon);
    }


}
