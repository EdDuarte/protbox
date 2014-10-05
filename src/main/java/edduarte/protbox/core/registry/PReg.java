package edduarte.protbox.core.registry;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderOption;
import edduarte.protbox.core.PbxPair;
import edduarte.protbox.core.PbxUser;
import edduarte.protbox.core.synchronization.SyncModule;
import edduarte.protbox.core.watcher.RegistryWatcher;
import edduarte.protbox.core.watcher.RequestFilesWatcher;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.windows.UserValidationWindow;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.dataholders.Pair;
import edduarte.protbox.utils.dataholders.Single;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
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
 * Protbox Registry (or PReg for short) is a parallel control data structure that assures the
 * coherence of the data in Prot folders and Shared folders. It contains structural information
 * about both folders (sub-files and sub-folders, encrypted and decrypted names, last modified
 * dates and lengths of contents).
 *
 * Coherency checking and synchronization tasks run on a periodic basis and use that structural
 * information and the effective contents of each {@link PbxEntry} to take the appropriate data
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
     * The name of the registry, which is used by the serialized registry file.
     * Every time the application is closed, this registry is serialized into a file with this name, and every
     * time the application is opened, this registry is resumed using the stored file with this name.
     */
    public final String id;


    /**
     * A collection of files that were changed directly by this application, meaning that they do not need to be
     * caught as events by any of the PRegWatchers.
     */
    public final List<String> SKIP_WATCHER_ENTRIES;


    /**
     * The user with local access to this registry. When this registry is serialized, the serialized file is
     * encrypted with this user's public key, guaranteeing that every data stored in this registry, including
     * entries and the secret key, will be protected. With this, to load the serialized file, it is always required
     * to validate the users Citizen Card.
     */
    private final PbxUser user;


    /**
     * The pair that this registry maintains and validates throughout the application's execution.
     */
    private final PbxPair pair;


    /**
     * The root entry that is linked to every other entry through a node-based implementation.
     */
    private PbxFolder root;


    /**
     * The maximum amount of data per file stored for backup and restoring purposes.
     */
    private int maxDeletedSize;


    private transient Cipher CIPHER;


    private transient boolean initialized;


    private transient boolean currentlyIndexing;


    private transient Timer timerIndex = new Timer();


    private transient Thread sharedFolderWatcher;


    private transient Thread protFolderWatcher;


    private transient Thread requestFileWatcher;


    /**
     * Constructs a new registry structure that links the Shared and the Prot folders using entries.
     */
    public PReg(PbxUser user, PbxPair pair, boolean isANewPReg) throws ProtException {
        this.id = Utils.generateUniqueId();
        this.user = user;
        this.pair = pair;
        this.SKIP_WATCHER_ENTRIES = new ArrayList<>();
        this.initialized = false;
        this.root = new PbxFolder(this, null, "", "");
        if (isANewPReg) {
            try {
                Constants.moveContentsFromDirToDir(pair.getSharedFolderFile(), pair.getProtFolderFile());
            } catch (IOException ex) {
                throw new ProtException(ex);
            }
        }
    }

    private static void emptyData(PbxEntry entry) {
        if (entry instanceof PbxFile) {
            PbxFile PbxFile = (PbxFile) entry;
            PbxFile.clearData();
        }
    }

    public void initialize() throws GeneralSecurityException, IOException {
        if (initialized) {
            return;
        }

        // starts the cipher according to the chosen algorithm
        CIPHER = Cipher.getInstance(pair.getPairAlgorithm() + "/ECB/PKCS5Padding");

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
        final Path sharedPath = Paths.get(pair.getSharedFolderPath());
        final Path protPath = Paths.get(pair.getProtFolderPath());
        sharedFolderWatcher = new Thread(new RegistryWatcher(this, FolderOption.SHARED, sharedPath));
        protFolderWatcher = new Thread(new RegistryWatcher(this, FolderOption.PROT, protPath));
        sharedFolderWatcher.start();
        protFolderWatcher.start();

        // starts a file watcher for creation of access request files
        requestFileWatcher = new Thread(new RequestFilesWatcher(sharedPath, result -> {
            if (result.getName().contains(Constants.SPECIAL_FILE_ASK_PREFIX) &&
                    result.getName().substring(4).equalsIgnoreCase(user.getId())) {

                // wait 1 second to avoid incomplete readings or file locks
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                UserValidationWindow.getInstance(this, result);
            }
        }));
        requestFileWatcher.start();


        // starts the synchronization threads, who are responsible of syncing elements between the folders
        SyncModule.start();

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

        SyncModule.removeSyncPairsForReg(this);

        initialized = false;
    }

    private PbxFolder goToFolder(String path, FolderOption at) {

        String relative = getRelativePath(path, at);
        if (relative.equals("")) {
            return root;
        }

        String[] pathParts = relative.split("/");
        PbxFolder atFolder = root;
        for (String next : pathParts) {
            atFolder = atFolder.goToFolder(next);
        }

        return atFolder;
    }

    private String getRelativePath(String absolutePath, FolderOption at) {
        String toRemoveFromPath = "";
        if (at.equals(FolderOption.SHARED))
            toRemoveFromPath = pair.getSharedFolderPath() + File.separator;
        else if (at.equals(FolderOption.PROT))
            toRemoveFromPath = pair.getProtFolderPath() + File.separator;

        toRemoveFromPath = toRemoveFromPath.replace("\\", File.separator);
        return new File(toRemoveFromPath).toURI().relativize(new File(absolutePath).toURI()).getPath();
    }


    // -- INTEGRITY CHECKING METHODS --

    public void executeIntegrityCheck() throws ProtException {
        if (currentlyIndexing) {
            return;
        }
        currentlyIndexing = true;
        integrityCheck(root, pair.getSharedFolderFile(), pair.getProtFolderFile());
        currentlyIndexing = false;
    }

    private void integrityCheck(PbxFolder folder, File sharedFolder, File protFolder) throws ProtException {
        if (folder == null)
            return;

        // generate structures to perform a careful ONE-BY-ONE file and folder checking
        Map<String, File> sharedMap = new HashMap<>();
        Map<String, File> protMap = new HashMap<>();
        List<File> filesAtShared = new ArrayList<>();
        List<File> filesAtProt = new ArrayList<>();

        if (sharedFolder != null) {
            File[] list = sharedFolder.listFiles();
            if (list != null) {
                Arrays.asList(list)
                        .stream()
                        .filter(f -> f.getName().charAt(0) != Constants.SPECIAL_FILE_FIRST_CHAR)
                        .forEach(f -> {
                            filesAtShared.add(f);
                            sharedMap.put(f.getName(), f);
                        });
            }
        }

        if (protFolder != null) {
            File[] list = protFolder.listFiles();
            if (list != null) {
                Arrays.asList(list)
                        .stream()
                        .forEach(f -> {
                            filesAtProt.add(f);
                            protMap.put(f.getName(), f);
                        });
            }
        }


        // checking sub-files and sub-folders with reg pairs (= already existed in the registry before checking)
        for (PbxFile f : folder.getSubFiles()) {
            File sharedFile = sharedMap.get(f.encodedName());
            File protFile = protMap.get(f.realName());
            evaluate(f, sharedFile, protFile);
            filesAtShared.remove(sharedFile);
            filesAtProt.remove(protFile);
        }
        for (PbxFolder f : folder.getSubFolders()) {
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
                PbxEntry newEntry = evaluate(null, sharedFile, protFile);
                if (sharedFile.isDirectory()) {
                    // iterate this folder with the pair that was just created
                    integrityCheck((PbxFolder) newEntry, sharedFile, protFile);
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
                PbxEntry newEntry = evaluate(null, sharedFile, protFile);
                if (protFile.isDirectory()) {
                    // iterate this folder with the pair that was just created
                    integrityCheck((PbxFolder) newEntry, sharedFile, protFile);
                }

            } catch (GeneralSecurityException ex) {
                logger.error("Error while checking prot file without pair " + protFile.getName(), ex);
            }
        }
    }

    private PbxEntry evaluate(PbxEntry entry, File sharedFile, File protFile) throws ProtException {
        try {
            if (entry == null) {
                // is a file or folder that is not represented by a Pair on this PReg

                if (sharedFile == null && protFile != null) {
                    // new file at prot folder -> add to registry and sync to shared
                    return add(protFile, FolderOption.PROT);

                } else if (sharedFile != null && protFile == null) {
                    // new file at shared folder -> add to registry and sync to prot
                    return add(sharedFile, FolderOption.SHARED);

                } else if (sharedFile != null && protFile != null) {
                    // new files at both folders -> add to registry as conflicted copy and sync to shared
                    addConflicted(protFile, FolderOption.PROT);
                    return add(sharedFile, FolderOption.SHARED);
                }

            } else if (entry instanceof PbxFile) {
                // is a file that is already represented by a Pair on this PReg

                PbxFile entry1 = (PbxFile) entry;
                if ((sharedFile == null || !sharedFile.exists()) &&
                        (protFile == null || !protFile.exists()) && !entry.hidden) {
                    // pair was deleted from both folders -> keep entry and do nothing
//                    permanentDelete(entry);

                } else if ((sharedFile == null || !sharedFile.exists()) && protFile != null) {
                    // file was deleted from shared folder
                    Date protLS = new Date(protFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getSnapshotLastModifiedDate();

                    if (protLS.compareTo(entryLS) < 0) {
                        // prot file is more recent than pair -> sync prot file to shared folder
                        SyncModule.toShared(this, entry);

                    } else {
                        // prot file is older than pair -> delete it while retaining data
                        delete(protFile.toPath(), FolderOption.PROT);

                    }

                } else if (sharedFile != null && protFile == null) {
                    // file was deleted from prot folder
                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getSnapshotLastModifiedDate();

                    if (sharedLS.compareTo(entryLS) < 0) {
                        // shared file is more recent than pair -> sync shared file to prot folder
                        SyncModule.toProt(this, entry);

                    } else {
                        // shared file is older than pair -> delete it while retaining data
                        delete(sharedFile.toPath(), FolderOption.SHARED);

                    }

                } else if (sharedFile != null && protFile != null) {
                    // file exists at both folders

                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date protLS = new Date(protFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getSnapshotLastModifiedDate();
                    int compareSharedProt = sharedLS.compareTo(protLS);
                    int compareProtPair = protLS.compareTo(entryLS);
                    int compareSharedPair = sharedLS.compareTo(entryLS);

                    if (compareSharedProt == 0 && compareProtPair == 0) {
                        // all last modified dates are equal -> do nothing

                    } else if (compareSharedProt != 0) {

                        if (compareProtPair == 0 && compareSharedPair != 0) {
                            // shared file was updated -> sync shared to prot
                            SyncModule.toProt(this, entry);

                        } else if (compareProtPair != 0 && compareSharedPair == 0) {
                            // prot file was updated -> sync prot to shared
                            SyncModule.toShared(this, entry);

                        } else if (compareProtPair != 0 && compareSharedPair != 0) {
                            // both files were updated -> conflict
                            addConflicted(protFile, FolderOption.PROT);
                            SyncModule.toProt(this, entry);
                        }
                    }
                }
            } else if (entry instanceof PbxFolder) {
                // is a folder that is already represented by a Pair on this PReg
                if ((sharedFile == null || !sharedFile.exists()) && (protFile == null || !protFile.exists()) && !entry.hidden) {
                    // pair was deleted from both folders, data cannot be obtained -> delete it from registry
                    permanentDelete(entry);

                } else if ((sharedFile == null || !sharedFile.exists()) && protFile != null) {
                    // pair was deleted at shared folder
                    hidePair(entry);

                } else if (sharedFile != null) {
                    // pair was deleted at prot folder or was not deleted at all -> assure that it is updated to shared folder
                    SyncModule.toProt(this, entry);

                }
            }

        } catch (NullPointerException ex) {
            // This catch is here only to avoid crashing due to non-existent / null files
            ex.printStackTrace();
        }
        return entry;
    }


    // -- ADD METHODS --

    public PbxEntry add(File file, FolderOption fileFrom) throws ProtException {
        if (Constants.verbose) {
            logger.info("Adding " + file.getAbsolutePath());
        }
        return addAux(file, false, fileFrom);
    }


    public PbxEntry addConflicted(File file, FolderOption fileFrom) throws ProtException {
        if (Constants.verbose) {
            logger.info("Adding conflicted copy " + file.getAbsolutePath());
        }
        return addAux(file, true, fileFrom);
    }


    private PbxEntry addAux(File file, boolean conflicted, FolderOption fileFrom) throws ProtException {
        PbxEntry newEntry = null;
        if (fileFrom.equals(FolderOption.PROT)) {
            newEntry = addOnlyToPRegFromProt(file, conflicted);
        } else if (fileFrom.equals(FolderOption.SHARED)) {
            newEntry = addOnlyToPRegFromShared(file, conflicted);
        }

        if (newEntry != null) {
            if (fileFrom.equals(FolderOption.PROT)) {
                SyncModule.toShared(this, newEntry);
            } else if (fileFrom.equals(FolderOption.SHARED)) {
                SyncModule.toProt(this, newEntry);
            }
        }
        return newEntry;
    }


    private PbxEntry addOnlyToPRegFromProt(File file, boolean conflicted) throws ProtException {
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
            PbxFolder parent = null;
            if (!parentPath.equalsIgnoreCase(pair.getProtFolderPath()))
                parent = goToFolder(parentPath, FolderOption.PROT);

            if (parent == null)
                parent = root;

            return addFinal(file, parent, realName, encodedName, FolderOption.PROT);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    private PbxEntry addOnlyToPRegFromShared(File file, boolean conflicted) throws ProtException {
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
            PbxFolder parent = null;
            if (!parentPath.equalsIgnoreCase(pair.getSharedFolderPath()))
                parent = goToFolder(parentPath, FolderOption.SHARED);

            if (parent == null)
                parent = root;

            return addFinal(file, parent, realName, encodedName, FolderOption.SHARED);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    private PbxEntry addFinal(final File file, final PbxFolder parent,
                              final String realName, final String encodedName, final FolderOption fromFolder)
            throws ProtException, IOException, GeneralSecurityException {

        if (file.isDirectory()) {
            PbxFolder pair;

            try {
                // checks if a deleted PbxFolder that represents the files already exists
                pair = parent.getSubFolders()
                        .stream()
                        .filter(PbxEntry::isHidden)
                        .filter(p -> p.realName().equals(realName) || p.encodedName().equals(encodedName))
                        .findFirst()
                        .get();

                // shows the existing PbxFolder
                showPair(pair);

            } catch (NoSuchElementException ex) {

                // there are no deleted PbxFolder, so create a new one
                pair = new PbxFolder(this, parent, encodedName, realName);

                // links the just created PbxFolder with this registry's entry hierarchy
                parent.addFolder(pair);

            }

            return pair;

        } else {
            PbxFile pair;

            try {
                // checks if a deleted PbxFile that represents the files already exists
                pair = parent.getSubFiles()
                        .stream()
                        .filter(PbxEntry::isHidden)
                        .filter(p -> p.realName().equals(realName) || p.encodedName().equals(encodedName))
                        .findFirst()
                        .get();

                // shows the existing PbxFile with updated data
                pair.createSnapshotFromFile(file, fromFolder);
                showPair(pair);

            } catch (NoSuchElementException ex) {

                // there are no deleted PbxFile, so create a new one
                pair = new PbxFile(this, parent, encodedName, realName);

                // links the just created PbxFile with this registry's entry hierarchy
                parent.addFile(pair);
            }

            return pair;
        }
    }


    // -- NAME CONVERSION METHODS --

    private String convertEncodedNameToRealName(String encodedName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(pair.getPairAlgorithm());
        c.init(Cipher.DECRYPT_MODE, pair.getPairKey());
        try {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("-", "/"))), "UTF8");

        } catch (UnsupportedEncodingException ex) {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("-", "/"))));

        }
    }


    private String convertRealNameToEncodedName(String realName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(pair.getPairAlgorithm());
        c.init(Cipher.ENCRYPT_MODE, pair.getPairKey());
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

    public void permanentDelete(PbxEntry entry) {
        entry.parentFolder().remove(entry);
        emptyData(entry);
    }


    public void delete(Path filePath, FolderOption fromFolder) throws ProtException {
        if (Constants.verbose) logger.info("Deleting file " + filePath);

        PbxFolder parent = goToFolder(filePath.getParent().toString(), fromFolder);
        PbxEntry toDelete;
        toDelete = parent.goToFile(filePath.getFileName().toString());
        if (toDelete == null) {
            toDelete = parent.goToFolder(filePath.getFileName().toString());
        }
        hidePair(toDelete);
    }


    /**
     * Hides the file or folder, keeping it in the registry but deleting it from both PROT and SHARED folders.
     */
    private void hidePair(PbxEntry entry) {
        if (entry.isHidden()) {
            return;
        }

        if (entry instanceof PbxFolder) {
            PbxFolder folder = (PbxFolder) entry;
            folder.getSubFolders().forEach(this::hidePair);
            folder.getSubFiles().forEach(this::hidePair);
        }

        try {
            File fileAtProt = new File(pair.getProtFolderPath() + File.separator + entry.relativeRealPath());
            if (fileAtProt.exists()) {
                if (entry instanceof PbxFile) {
                    PbxFile file = (PbxFile) entry;
                    file.createSnapshotFromFile(fileAtProt, FolderOption.PROT);
                }
                SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtProt.getParentFile().getAbsolutePath());
                Constants.delete(fileAtProt);
            }

            File fileAtShared = new File(pair.getSharedFolderPath() + File.separator + entry.relativeEncodedPath());
            if (fileAtShared.exists()) {
                if (entry instanceof PbxFile) {
                    PbxFile file = (PbxFile) entry;
                    file.createSnapshotFromFile(fileAtShared, FolderOption.SHARED);
                }
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getParentFile().getAbsolutePath());
                Constants.delete(fileAtShared);
            }

        } catch (ProtException ex) {
            if (Constants.verbose) {
                logger.error("Error while hiding PReg pair " + entry.toString(), ex);
            }
        }

        entry.hidden = true;
    }


    /**
     * Shows the file or folder, reversing the delete process on both PROT and SHARED folders.
     */
    public void showPair(PbxEntry entry) { // TODO HAVE AN INDEX
        if (!entry.isHidden()) {
            return;
        }

        if (entry.parentFolder().isHidden()) {
            showPair(entry.parentFolder());
        }

        entry.hidden = false;
        File fileAtProt = new File(pair.getProtFolderPath() + File.separator + entry.relativeRealPath());
        SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());

        try {

            if (entry instanceof PbxFile) {
                PbxFile pbxFile = (PbxFile) entry;
                pbxFile.writeSnapshotToFile(0, fileAtProt, FolderOption.PROT);

                TrayApplet.getInstance().baloon("File Restored",
                        "The file \"" + entry.realName() + "\" was restored to its original location.",
                        TrayIcon.MessageType.INFO);
                emptyData(entry);

            } else if (entry instanceof PbxFolder) {
                fileAtProt.mkdir();
                TrayApplet.getInstance().baloon("Folder Restored",
                        "The folder \"" + entry.realName() + "\" was restored to its original location.",
                        TrayIcon.MessageType.INFO);
            }

            SyncModule.toShared(this, entry);

        } catch (ProtException ex) {
            if (Constants.verbose) {
                logger.error("Error while writing file data for " + entry.toString(), ex);
            }
            entry.hidden = true;
            SKIP_WATCHER_ENTRIES.remove(fileAtProt.getAbsolutePath());
        }
    }


    public DefaultMutableTreeNode buildDeletedTree() {
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(root, true);
        build(treeRoot, root);
        return treeRoot;
    }


    private boolean build(final DefaultMutableTreeNode treeRoot, final PbxFolder root) {
        final Single<Boolean> deletedFileWasAdded = new Single<>(false);

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
        CIPHER.init(Cipher.ENCRYPT_MODE, pair.getPairKey());
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
        CIPHER.init(Cipher.DECRYPT_MODE, pair.getPairKey());
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
            Pair<List<PbxEntry>> removedEntries = SyncModule.removeSyncPairsForReg(this);


            // 2) set new path
            File oldProtFile = pair.getProtFolderFile();
            String newProtPath = new File(newPath).getAbsolutePath();
            File newProtFile = new File(newProtPath + root.realName());


            // 3) move all files and folders from old folder to new folder
            Constants.moveContentsFromDirToDir(oldProtFile, newProtFile);
            executeIntegrityCheck();


            // 4) start new Watcher on the new Folder
            protFolderWatcher = new Thread(new RegistryWatcher(this, FolderOption.PROT, newProtFile.toPath()));
            protFolderWatcher.start();


            // 5) add removed entries to the syncing threads again
            for (PbxEntry e : removedEntries.first)
                SyncModule.toProt(this, e);

            for (PbxEntry e : removedEntries.second)
                SyncModule.toShared(this, e);

            // 6) update the pair
            pair.setProtFolderPath(newProtPath);

            TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
        } catch (IOException ex) {
            throw new ProtException(ex);
        }
    }


    public void openExplorerFolder(FolderOption folderToOpen) throws IOException {
        String path = "";
        if (folderToOpen.equals(FolderOption.SHARED))
            path = pair.getSharedFolderPath();
        else if (folderToOpen.equals(FolderOption.PROT))
            path = pair.getProtFolderPath();


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


    public final PbxUser getUser() {
        return user;
    }


    public final PbxPair getPair() {
        return pair;
    }


    public int getMaxDeletedSize() {
        return maxDeletedSize;
    }


    public void setMaxDeletedSize(int maxDeletedSize) {
        this.maxDeletedSize = maxDeletedSize;
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
        return pair.equals(d.pair);
    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String indent = "   ";
        sb.append(pair.getSharedFolderFile().getName());
        sb.append("\n");
        return print(sb, root, indent, indent).toString();
    }

    private StringBuffer print(StringBuffer sb, PbxFolder root, String originalIndent, String indent) {

        for (PbxFile f : root.getSubFiles()) {
            sb.append(indent);
            sb.append(f.realName());
            sb.append(" (file");

            if (f.isHidden()){
                sb.append(", deleted)\n");

            } else {
                sb.append(")\n");

            }
        }

        for (PbxFolder f : root.getSubFolders()) {
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
