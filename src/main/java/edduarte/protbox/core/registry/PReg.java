package edduarte.protbox.core.registry;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.Folder;
import edduarte.protbox.core.User;
import edduarte.protbox.core.synchronization.Sync;
import edduarte.protbox.core.watcher.RequestFileWatcher;
import edduarte.protbox.core.watcher.PRegWatcher;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.window.UserValidationWindow;
import edduarte.protbox.utils.Callback;
import edduarte.protbox.utils.Ref;
import edduarte.protbox.utils.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

/**
 * The PReg is a parallel control data structure that assures the coherence of the data in Prot
 * folders and Shared folders. It contains structural information about both folders (sub-files
 * and sub-folders, encrypted and decrypted names, last modified dates and lengths of contents).
 *
 * Coherency checking and synchronization tasks run on a periodic basis and use that structural
 * information and the effective contents of each {@link Pair} to take the appropriate data
 * transfer decisions.
 *
 * Note that a PReg is a local, private data structure that helps a local {\protbox} instance to
 * take the appropriate, local decisions regarding file synchronizations, encryption/decryption
 * and recovery actions. In particular, a PReg is never synchronized with another one.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PReg implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient static final Logger logger = LoggerFactory.getLogger(PReg.class);


    /**
     * The configured SHARED shared folder where the encrypted files will be.
     */
    public final String SHARED_PATH;


    /**
     * The configured PROT folder where every file from the SHARED folder will be stored in decrypted form.
     */
    public String PROT_PATH;


    /**
     * The user with local access to this registry. When this registry is serialized, the serialized file is
     * encrypted with this user's public key, guaranteeing that every data stored in this registry, including
     * entries and the secret key, will be protected. With this, to load the serialized file, it is always required
     * to validate the users Citizen Card.
     */
    public final User user;


    /**
     * The name of the registry, which is used by the serialized registry file.
     * <p>
     * Every time the application is closed, this registry is serialized into a file with this name, and every
     * time the application is opened, this registry is resumed using the stored file with this name.
     */
    public final String ID = Utils.generateUniqueId();


    /**
     * A collection of files that were changed directly by this application, meaning that they do not need to be
     * caught as events by any of the PRegWatchers.
     */
    public final List<String> SKIP_WATCHER_ENTRIES;


    /**
     * Simple access to the algorithm used by this registry. This is used to instantiate the Cipher
     * per execution, since the Cipher object cannot be serialized.
     */
    private final String algorithm;


    /**
     * The secret key used by this PReg. This key is private and completely restricted from use.
     */
    private final SecretKey CIPHER_KEY;


    /**
     * The secret key used by this PReg. This key is private and completely restricted from use.
     */
    private final SecretKey INTEGRITY_KEY;


    /**
     * The initial pair that links to every other pair through a node-based implementation.
     */
    private PairFolder root;


    /**
     * A simple Cipher used with the secret key to encrypt or decrypt files in this PReg.
     */
    private transient Cipher CIPHER;

    private transient boolean initialized;

    private transient boolean currentlyIndexing;

    private transient Timer timerIndex = new Timer();

    private transient Thread sharedFolderWatcher;

    private transient Thread protFolderWatcher;

    private transient Thread requestFileWatcher;


    /**
     * Constructs a new PReg structure that links the Shared and the Prot folders using entries.
     */
    public PReg(User user,
                String sharedPath,
                String protPath,
                String algorithm,
                SecretKey cipherKey,
                SecretKey integrityKey,
                boolean isANewPReg) throws ProtException {

        File sharedPathFile = new File(sharedPath);
        File protPathFile = new File(protPath);
        this.user = user;
        SHARED_PATH = sharedPathFile.getAbsolutePath();
        PROT_PATH = protPathFile.getAbsolutePath();
        this.algorithm = algorithm;
        this.CIPHER_KEY = cipherKey;
        this.INTEGRITY_KEY = integrityKey;
        this.SKIP_WATCHER_ENTRIES = new ArrayList<>();
        this.initialized = false;
        this.root = new PairFolder(null, "", "");
        if (isANewPReg) {
            try {
                Constants.moveContentsFromDirToDir(sharedPathFile, protPathFile);
            } catch (IOException ex) {
                throw new ProtException(ex);
            }
        }
    }

    private static void emptyData(Pair pair) {
        if (pair instanceof PairFile) {
            PairFile pairFile = (PairFile) pair;
            pairFile.emptyData();
        }
    }

    public void initialize() throws GeneralSecurityException, IOException {
        if (initialized) {
            return;
        }

        // starts the cipher according to the chosen algorithm
        CIPHER = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");

        // checks the registry periodically (every 2 seconds) and detects any changes made
        timerIndex.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    executeIntegrityCheck();
                } catch (ProtException ex) {
                    run();
                }
            }
        }, 0, 5000);

        // starts the registry watchers for both prot and shared folders
        final Path sharedPath = Paths.get(SHARED_PATH);
        final Path protPath = Paths.get(PROT_PATH);
        sharedFolderWatcher = new Thread(new PRegWatcher(this, Folder.SHARED, sharedPath));
        protFolderWatcher = new Thread(new PRegWatcher(this, Folder.PROT, protPath));
        sharedFolderWatcher.start();
        protFolderWatcher.start();

        // starts a file watcher for creation of access request files
        requestFileWatcher = new Thread(new RequestFileWatcher(sharedPath, result -> {
            if (result.getName().contains(Constants.SPECIAL_FILE_ASK_PREFIX) &&
                    result.getName().substring(4).equalsIgnoreCase(user.getId())) {

                // wait 1 second to avoid incomplete readings or file locks
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                UserValidationWindow.getInstance(this, algorithm, CIPHER_KEY, sharedPath.getFileName().toString(), result);
            }
        }));
        requestFileWatcher.start();


        // starts the synchronization threads, who are responsible of syncing elements between the folders
        Sync.start();

        initialized = true;
    }

    public void stop() {
        timerIndex.cancel();
        timerIndex.purge();

        if (sharedFolderWatcher != null)
            sharedFolderWatcher.interrupt();

        if (protFolderWatcher != null)
            protFolderWatcher.interrupt();

        if (requestFileWatcher != null)
            requestFileWatcher.interrupt();

        Sync.removeSyncPairsForReg(this);

        initialized = false;
    }

    private PairFolder goToFolder(String path, Folder at) {

        String relative = getRelativePath(path, at);
        if (relative.equals("")) {
            return root;
        }

        String[] pathParts = relative.split("/");
        PairFolder atFolder = root;
        for (String next : pathParts) {
            atFolder = atFolder.goToFolder(next);
        }

        return atFolder;
    }

    private String getRelativePath(String absolutePath, Folder at) {
        String toRemoveFromPath = "";
        if (at.equals(Folder.SHARED))
            toRemoveFromPath = SHARED_PATH + File.separator;
        else if (at.equals(Folder.PROT))
            toRemoveFromPath = PROT_PATH + File.separator;

        toRemoveFromPath = toRemoveFromPath.replace("\\", File.separator);
        return new File(toRemoveFromPath).toURI().relativize(new File(absolutePath).toURI()).getPath();
    }


    // -- INTEGRITY CHECKING METHODS --

    public void executeIntegrityCheck() throws ProtException {
        if (currentlyIndexing) {
            return;
        }
        currentlyIndexing = true;
        integrityCheck(root, new File(SHARED_PATH), new File(PROT_PATH));
        currentlyIndexing = false;
    }

    private void integrityCheck(PairFolder pairFolder, File sharedFolder, File protFolder) throws ProtException {
        if (pairFolder == null)
            return;

        // generate structures to perform a careful ONE-BY-ONE file and folder checking
        Map<String, File> sharedMap = new HashMap<>();
        Map<String, File> protMap = new HashMap<>();
        List<File> filesAtShared = new ArrayList<>();
        List<File> filesAtProt = new ArrayList<>();

        if (sharedFolder != null && sharedFolder.listFiles() != null) {
            Arrays.asList(sharedFolder.listFiles())
                    .stream()
                    .filter(f -> f.getName().charAt(0) != Constants.SPECIAL_FILE_FIRST_CHAR)
                    .forEach(f -> {
                        filesAtShared.add(f);
                        sharedMap.put(f.getName(), f);
                    });
        }
        if (protFolder != null) {
            Collections.addAll(filesAtProt, protFolder.listFiles());
            filesAtProt.stream()
                    .forEach(f -> protMap.put(f.getName(), f));
        }


        // checking sub-files and sub-folders with reg pairs (= already existed in the registry before checking)
        for (PairFile f : pairFolder.getSubFiles()) {
            File sharedFile = sharedMap.get(f.encodedName());
            File protFile = protMap.get(f.realName());
            evaluate(f, sharedFile, protFile);
            filesAtShared.remove(sharedFile);
            filesAtProt.remove(protFile);
        }
        for (PairFolder f : pairFolder.getSubFolders()) {
            File sharedFile = sharedMap.get(f.encodedName());
            File protFile = protMap.get(f.realName());
            integrityCheck(f, sharedFile, protFile);
            evaluate(f, sharedFile, protFile);
            filesAtShared.remove(sharedFile);
            filesAtProt.remove(protFile);
        }


        // checking sub-files and sub-folders without reg pairs (= did not exist in the registry before checking)
        for (File sharedFile : filesAtShared) {
            // iterating shared files that do not exist in this PReg
            try {
                String realName = convertEncodedNameToRealName(sharedFile.getName());
                File protFile = protMap.get(realName);
                Pair newPair = evaluate(null, sharedFile, protFile);
                if (sharedFile.isDirectory()) {
                    // iterate this folder with the pair that was just created
                    integrityCheck((PairFolder) newPair, sharedFile, protFile);
                }

            } catch (GeneralSecurityException ex) {
                logger.error("Error while checking shared file without pair " + sharedFile.getName(), ex);
            }
        }
        for (File protFile : filesAtProt) {
            // iterating prot files that do not exist in this PReg
            try {
                String encodedName = convertRealNameToEncodedName(protFile.getName());
                File sharedFile = sharedMap.get(encodedName);
                Pair newPair = evaluate(null, sharedFile, protFile);
                if (protFile.isDirectory()) {
                    // iterate this folder with the pair that was just created
                    integrityCheck((PairFolder) newPair, sharedFile, protFile);
                }

            } catch (GeneralSecurityException ex) {
                logger.error("Error while checking prot file without pair " + protFile.getName(), ex);
            }
        }
    }

    private Pair evaluate(Pair pair, File sharedFile, File protFile) throws ProtException {
        try {
            if (pair == null) {
                // is a file or folder that is not represented by a Pair on this PReg

                if (sharedFile == null && protFile != null) {
                    // new file at prot folder -> add to registry and sync to shared
                    return add(protFile, Folder.PROT);

                } else if (sharedFile != null && protFile == null) {
                    // new file at shared folder -> add to registry and sync to prot
                    return add(sharedFile, Folder.SHARED);

                } else if (sharedFile != null && protFile != null) {
                    // new files at both folders -> add to registry as conflicted copy and sync to shared
                    addConflicted(protFile, Folder.PROT);
                    return add(sharedFile, Folder.SHARED);
                }

            } else if (pair instanceof PairFile) {
                // is a file that is already represented by a Pair on this PReg

                PairFile pair0 = (PairFile) pair;
                if ((sharedFile == null || !sharedFile.exists()) && (protFile == null || !protFile.exists()) && !pair.hidden) {
                    // pair was hidden from both folders, data cannot be obtained -> delete it from registry
                    permanentDelete(pair);

                } else if ((sharedFile == null || !sharedFile.exists()) && protFile != null) {
                    // file was deleted from shared folder
                    Date protLS = new Date(protFile.lastModified());
                    Date pairLS = pair0.getLastModified();

                    if (protLS.compareTo(pairLS) < 0) {
                        // prot file is more recent than pair -> sync prot file to shared folder
                        Sync.toShared(this, pair);

                    } else {
                        // prot file is older than pair -> delete it while retaining data
                        delete(protFile.toPath(), Folder.PROT);

                    }

                } else if (sharedFile != null && protFile == null) {
                    // file was deleted from prot folder
                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date pairLS = pair0.getLastModified();

                    if (sharedLS.compareTo(pairLS) < 0) {
                        // shared file is more recent than pair -> sync shared file to prot folder
                        Sync.toProt(this, pair);

                    } else {
                        // shared file is older than pair -> delete it while retaining data
                        delete(sharedFile.toPath(), Folder.SHARED);

                    }

                } else if (sharedFile != null && protFile != null) {
                    // file exists at both folders

                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date protLS = new Date(protFile.lastModified());
                    Date pairLS = pair0.getLastModified();
                    int compareSharedProt = sharedLS.compareTo(protLS);
                    int compareProtPair = protLS.compareTo(pairLS);
                    int compareSharedPair = sharedLS.compareTo(pairLS);

                    if (compareSharedProt == 0 && compareProtPair == 0) {
                        // all last modified dates are equal -> do nothing

                    } else if (compareSharedProt != 0) {

                        if (compareProtPair == 0 && compareSharedPair != 0) {
                            // shared file was updated -> sync shared to prot
                            Sync.toProt(this, pair);

                        } else if (compareProtPair != 0 && compareSharedPair == 0) {
                            // prot file was updated -> sync prot to shared
                            Sync.toShared(this, pair);

                        } else if (compareProtPair != 0 && compareSharedPair != 0) {
                            // both files were updated -> conflict
                            addConflicted(protFile, Folder.PROT);
                            Sync.toProt(this, pair);
                        }
                    }
                }
            } else if (pair instanceof PairFolder) {
                // is a folder that is already represented by a Pair on this PReg
                if ((sharedFile == null || !sharedFile.exists()) && (protFile == null || !protFile.exists()) && !pair.hidden) {
                    // pair was deleted from both folders, data cannot be obtained -> delete it from registry
                    permanentDelete(pair);

                } else if ((sharedFile == null || !sharedFile.exists()) && protFile != null) {
                    // pair was deleted at shared folder
                    hidePair(pair);

                } else if (sharedFile != null) {
                    // pair was deleted at prot folder or was not hidden at all -> assure that it is updated to shared folder
                    Sync.toProt(this, pair);

                }
            }

        } catch (NullPointerException ex) {
            // This catch is here only to avoid crashing due to non-existent / null files
            ex.printStackTrace();
        }
        return pair;
    }


    // -- ADD METHODS --

    public Pair add(File file, Folder fileFrom) throws ProtException {
        if (Constants.verbose) {
            logger.info("Adding " + file.getAbsolutePath());
        }
        return addAux(file, false, fileFrom);
    }


    public Pair addConflicted(File file, Folder fileFrom) throws ProtException {
        if (Constants.verbose) {
            logger.info("Adding conflicted copy " + file.getAbsolutePath());
        }
        return addAux(file, true, fileFrom);
    }


    private Pair addAux(File file, boolean conflicted, Folder fileFrom) throws ProtException {
        Pair newPair = null;
        if (fileFrom.equals(Folder.PROT)) {
            newPair = addOnlyToPRegFromProt(file, conflicted);
        } else if (fileFrom.equals(Folder.SHARED)) {
            newPair = addOnlyToPRegFromShared(file, conflicted);
        }

        if (newPair != null) {
            if (fileFrom.equals(Folder.PROT)) {
                Sync.toShared(this, newPair);
            } else if (fileFrom.equals(Folder.SHARED)) {
                Sync.toProt(this, newPair);
            }
        }
        return newPair;
    }


    private Pair addOnlyToPRegFromProt(File file, boolean conflicted) throws ProtException {
        try {
            String realName = file.getName();
            if (conflicted && !file.isDirectory()) {
                realName = realNameToConflicted(realName);
                File newConflictFile = new File(file.getParentFile(), realName);
                SKIP_WATCHER_ENTRIES.add(newConflictFile.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(file.getAbsolutePath());

                // get the data from the added file
                byte[] protFileData = FileUtils.readFileToByteArray(file);

                // move data to a new conflicted file and delete old file
                FileUtils.writeByteArrayToFile(newConflictFile, protFileData);
                Constants.delete(file);
            }
            String encodedName = convertRealNameToEncodedName(realName);


            String parentPath = file.getParentFile().getAbsolutePath();
            PairFolder parent = null;
            if (!parentPath.equalsIgnoreCase(PROT_PATH))
                parent = goToFolder(parentPath, Folder.PROT);

            if (parent == null)
                parent = root;

            return addFinal(file, parent, realName, encodedName, Folder.PROT);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    private Pair addOnlyToPRegFromShared(File file, boolean conflicted) throws ProtException {
        try {
            String encodedName = file.getName();
            String realName = convertEncodedNameToRealName(encodedName);
            if (conflicted && !file.isDirectory()) {
                realName = realNameToConflicted(realName);
                encodedName = convertRealNameToEncodedName(realName);
                File newConflictFile = new File(file.getParentFile(), encodedName);
                SKIP_WATCHER_ENTRIES.add(newConflictFile.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(file.getAbsolutePath());

                // get the data from the added file
                byte[] sharedFileData = FileUtils.readFileToByteArray(file);

                // move data to a new shared folder file and delete old file
                FileUtils.writeByteArrayToFile(newConflictFile, sharedFileData);
                Constants.delete(file);
            }

            String parentPath = file.getParentFile().getAbsolutePath();
            PairFolder parent = null;
            if (!parentPath.equalsIgnoreCase(SHARED_PATH))
                parent = goToFolder(parentPath, Folder.SHARED);

            if (parent == null)
                parent = root;

            return addFinal(file, parent, realName, encodedName, Folder.SHARED);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    private Pair addFinal(File file, PairFolder parent, String realName, String encodedName, Folder fromFolder) throws ProtException, IOException, GeneralSecurityException {
        Pair pairToReturn;

        if (file.isDirectory()) {
            // checks if a hidden PairFolder that represents the files already exists
            PairFolder pair = parent.getSubFolders()
                    .stream()
                    .filter(Pair::isHidden)
                    .filter(p -> p.realName().equals(realName) || p.encodedName().equals(encodedName))
                    .findFirst()
                    .get();

            if (pair != null) {
                // needs to show the existing PairFolder
                showPair(pair);

            } else {
                // needs to create a new PairFolder
                pair = new PairFolder(parent, encodedName, realName);
                parent.addFolder(pair);

            }

            pairToReturn = pair;

        } else {
            // checks if a hidden PairFile that represents the files already exists
            PairFile pair = parent.getSubFiles()
                    .stream()
                    .filter(Pair::isHidden)
                    .filter(p -> p.realName().equals(realName) || p.encodedName().equals(encodedName))
                    .findFirst()
                    .get();

            if (pair != null) {
                // needs to show the existing PairFile with updated data
                byte[] newData = FileUtils.readFileToByteArray(file);
                if (fromFolder.equals(Folder.SHARED)) {
                    pair.setData(decrypt(newData));

                } else if (fromFolder.equals(Folder.PROT)) {
                    pair.setData(newData);

                }
                showPair(pair);

            } else {
                // needs to create a new PairFolder
                pair = new PairFile(parent, encodedName, realName, new Date(file.lastModified()), file.length());
                parent.addFile(pair);

            }

            pairToReturn = pair;
        }

        return pairToReturn;
    }


    // -- NAME CONVERSION METHODS --

    private String convertEncodedNameToRealName(String encodedName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.DECRYPT_MODE, CIPHER_KEY);
        try {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("-", "/"))), "UTF8");

        } catch (UnsupportedEncodingException ex) {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("-", "/"))));

        }
    }


    private String convertRealNameToEncodedName(String realName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.ENCRYPT_MODE, CIPHER_KEY);
        try {
            return new String(Base64.encodeBase64(c.doFinal(realName.getBytes("UTF8"))), "UTF8").replaceAll("/", "-");

        } catch (UnsupportedEncodingException ex) {
            return new String(Base64.encodeBase64(c.doFinal(realName.getBytes()))).replaceAll("/", "-");

        }
    }

    private String realNameToConflicted(String realName) throws GeneralSecurityException {
        String conflictText = " (conflicted copy from " + user.getName() + ")";

        int dotIndex = realName.lastIndexOf(".");
        String name, ext;
        if (dotIndex == -1) {
            name = realName;
            ext = "";
        } else {
            name = realName.substring(0, dotIndex);
            ext = realName.substring(dotIndex);
        }
        return name + conflictText + ext;
    }


    // -- DELETE METHODS --

    public void permanentDelete(Pair pair) {
        pair.parentFolder().remove(pair);
        emptyData(pair);
    }


    public void delete(Path filePath, Folder fromFolder) throws ProtException {
        if (Constants.verbose) logger.info("Deleting file " + filePath);

        PairFolder parent = goToFolder(filePath.getParent().toString(), fromFolder);
        Pair toDelete;
        toDelete = parent.goToFile(filePath.getFileName().toString());
        if (toDelete == null) {
            toDelete = parent.goToFolder(filePath.getFileName().toString());
        }
        hidePair(toDelete);
    }


    /**
     * Hides the file or folder, keeping it in the registry but deleting it from both PROT and SHARED folders.
     */
    private void hidePair(Pair pair) {
        if (pair.isHidden()) {
            return;
        }

        if (pair instanceof PairFolder) {
            PairFolder pairFolder = (PairFolder) pair;
            pairFolder.getSubFolders().forEach(this::hidePair);
            pairFolder.getSubFiles().forEach(this::hidePair);
        }

        try {
            File fileAtProt = new File(PROT_PATH + File.separator + pair.relativeRealPath());
            if (fileAtProt.exists()) {
                if (pair instanceof PairFile) {
                    ((PairFile) pair).setData(FileUtils.readFileToByteArray(fileAtProt));
                }
                SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtProt.getParentFile().getAbsolutePath());
                Constants.delete(fileAtProt);
            }

            File fileAtShared = new File(SHARED_PATH + File.separator + pair.relativeEncodedPath());
            if (fileAtShared.exists()) {
                if (pair instanceof PairFile) {
                    ((PairFile) pair).setData(decrypt(FileUtils.readFileToByteArray(fileAtShared)));
                }
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getParentFile().getAbsolutePath());
                Constants.delete(fileAtShared);
            }

        } catch (IOException | GeneralSecurityException ex) {
            if (Constants.verbose) {
                logger.error("Error while hiding PReg pair " + pair.toString(), ex);
            }
        }

        pair.hidden = true;
    }


    /**
     * Shows the file or folder, reversing the delete project on both PROT and SHARED folders.
     */
    public void showPair(Pair pair) {
        if (!pair.isHidden()) {
            return;
        }

        if (pair.parentFolder().isHidden()) {
            showPair(pair.parentFolder());
        }

        pair.hidden = false;
        File fileAtProt = new File(PROT_PATH + File.separator + pair.relativeRealPath());
        SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());

        try {

            if (pair instanceof PairFile) {
                PairFile pbxFile = (PairFile) pair;
                pbxFile.setLastModified(new Date(fileAtProt.lastModified()));
                FileUtils.writeByteArrayToFile(fileAtProt, pbxFile.getData());
                TrayApplet.getInstance().baloon("File Restored",
                        "The hidden file \"" + pair.realName() + "\" was restored to its original location.",
                        TrayIcon.MessageType.INFO);
                emptyData(pair);

            } else if (pair instanceof PairFolder) {
                fileAtProt.mkdir();
                TrayApplet.getInstance().baloon("Folder Restored",
                        "The hidden folder \"" + pair.realName() + "\" was restored to its original location.",
                        TrayIcon.MessageType.INFO);
            }

            Sync.toShared(this, pair);

        } catch (IOException ex) {
            if (Constants.verbose) {
                logger.error("Error while showing PReg pair " + pair.toString(), ex);
            }
            pair.hidden = true;
            SKIP_WATCHER_ENTRIES.remove(fileAtProt.getAbsolutePath());
        }
    }


    public DefaultMutableTreeNode buildDeletedTree() {
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(root, true);
        build(treeRoot, root);
        return treeRoot;
    }


    private boolean build(final DefaultMutableTreeNode treeRoot, final PairFolder root) {
        final Ref.Single<Boolean> deletedFileWasAdded = Ref.of1(false);

        root.getSubFolders().forEach(f -> {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(true);
            deletedFileWasAdded.value = build(newNode, f);
            if (f.hidden || deletedFileWasAdded.value)
                treeRoot.add(newNode);
        });

        root.getSubFiles().forEach(f -> {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(false);
            if (f.hidden) {
                deletedFileWasAdded.value = true;
                treeRoot.add(newNode);
            }
        });

        return deletedFileWasAdded.value;
    }


    // -- CIPHER METHODS --

    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        CIPHER.init(Cipher.ENCRYPT_MODE, CIPHER_KEY);
        return CIPHER.doFinal(data);

        // DEPRECATED: PUT REAL SIZE AT THE END OF FILE WITHIN 4 BYTES FOR PADDING SPECIFICATION
//        try(ByteArrayInputStream in = new ByteArrayInputStream(data)){
//            int multiplier = 1;
//            if(algorithm.equalsIgnoreCase("AES"))
//                multiplier = 16;
//            else if(algorithm.equalsIgnoreCase("DESede"))
//                multiplier = 8;
//            byte[] expandedData = new byte[data.length*multiplier];
//            System.out.println(expandedData.length);
//            int realSize = in.read(expandedData);
////            System.out.println("real size:"+realSize);
//
//            // encrypt data with registry's key, which may result in non-padded results
//            CIPHER.init(Cipher.ENCRYPT_MODE, CIPHER_KEY);
//            byte[] encryptedBytes = CIPHER.doFinal(expandedData);
//
//            // convert the real size into 4 bytes
//            byte[] realSizeBytes = ByteBuffer.allocate(4).putInt(realSize).array();
//
//
//            // place the real size bytes after the encrypted data array
//            int resultSize = encryptedBytes.length+4;
//            List<Byte> collection = new ArrayList<>(resultSize);
//            for(byte b : encryptedBytes)
//                collection.add(b);
//            for(byte b : realSizeBytes)
//                collection.add(b);
//
//            byte[] result = new byte[resultSize];
//            for(int i = 0; i<collection.size(); i++)
//                result[i] = collection.get(i);
//
//            return result;
//
//        }catch (IOException ex){
//            // this is never going to happen
//            return null;
//        }
    }


    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        CIPHER.init(Cipher.DECRYPT_MODE, CIPHER_KEY);
        return CIPHER.doFinal(data);

        // DEPRECATED: GET REAL SIZE AT THE END OF FILE WITHIN 4 BYTES FOR PADDING SPECIFICATION
//        // get real size from the last 4 bytes of the file
//        int len = data.length;
//        int realSize =
//                ByteBuffer.wrap(new byte[]{data[len - 4], data[len - 3], data[len - 2], data[len - 1]}).getInt();
////        System.out.println(realSize);
//
//        // get the encrypted data from every byte except for the last 4 and decrypt them with registry's key
//        data = Arrays.copyOf(data, len-4);
////        System.out.println(data.length);
//
//        CIPHER.init(Cipher.DECRYPT_MODE, CIPHER_KEY);
//        byte[] decrypted = CIPHER.doFinal(data);
//        return Arrays.copyOf(decrypted, realSize);
    }


    public void changeProtPath(String newPath) throws ProtException {
        try {
            TrayApplet.getInstance().status(TrayApplet.TrayStatus.LOADING, "Moving files to new prot folder...");

            // 1) remove entries from the syncing threads
            protFolderWatcher.interrupt();
            Ref.Duo<List<Pair>> removedEntries = Sync.removeSyncPairsForReg(this);


            // 2) set new path
            File oldProtFile = new File(PROT_PATH);
            PROT_PATH = new File(newPath).getAbsolutePath();
            File newProtFile = new File(PROT_PATH + root.realName());


            // 3) move all files and folders from old folder to new folder
            Constants.moveContentsFromDirToDir(oldProtFile, newProtFile);
            executeIntegrityCheck();


            // 4) start new Watcher on the new Folder
            protFolderWatcher = new Thread(new PRegWatcher(this, Folder.PROT, newProtFile.toPath()));
            protFolderWatcher.start();


            // 5) add removed entries to the syncing threads again
            for (Pair e : removedEntries.first)
                Sync.toProt(this, e);

            for (Pair e : removedEntries.second)
                Sync.toShared(this, e);


            TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
        } catch (IOException ex) {
            throw new ProtException(ex);
        }
    }


    public void openExplorerFolder(Folder folderToOpen) throws IOException {
        String path = "";
        if (folderToOpen.equals(Folder.SHARED))
            path = SHARED_PATH;
        else if (folderToOpen.equals(Folder.PROT))
            path = PROT_PATH;


        if (SystemUtils.IS_OS_WINDOWS) {
            Runtime.getRuntime().exec("explorer " + path);

        } else if (SystemUtils.IS_OS_MAC_OSX) {
            Runtime.getRuntime().exec("open " + path);

        } else {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(path));
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;

        } else if (this != o) {
            return false;

        } else if (!(o instanceof PReg)) {
            return false;

        }

        PReg d = (PReg) o;
        return SHARED_PATH.equalsIgnoreCase(d.SHARED_PATH) && PROT_PATH.equalsIgnoreCase(d.PROT_PATH);
    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String indent = "   ";
        sb.append(new File(SHARED_PATH).getName());
        sb.append("\n");
        return print(sb, root, indent, indent).toString();
    }

    private StringBuffer print(StringBuffer sb, PairFolder root, String originalIndent, String indent) {

        for (PairFile f : root.getSubFiles()) {
            sb.append(indent);
            sb.append(f.realName());
            sb.append(" (file");

            if (f.isHidden()){
                sb.append(", hidden)\n");

            } else {
                sb.append(")\n");

            }
        }

        for (PairFolder f : root.getSubFolders()) {
            if (f.parentFolder() != null) {
                sb.append(indent);
                sb.append(f.realName());
                sb.append(" (folder, parent:");
                sb.append(f.parentFolder().realName());
                sb.append(")\n");

            } else {
                sb.append(indent);
                sb.append(f.realName());
                sb.append(" (folder, parent:ROOT)\n");

            }
            print(sb, f, originalIndent, indent + originalIndent);
        }

        return sb;
    }
}
