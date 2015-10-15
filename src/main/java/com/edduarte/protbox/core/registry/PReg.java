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

package com.edduarte.protbox.core.registry;

import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.core.FolderOption;
import com.edduarte.protbox.core.PbxPair;
import com.edduarte.protbox.core.PbxUser;
import com.edduarte.protbox.core.synchronization.SyncModule;
import com.edduarte.protbox.core.watcher.DefaultWatcher;
import com.edduarte.protbox.core.watcher.RequestWatcher;
import com.edduarte.protbox.exception.ProtboxException;
import com.edduarte.protbox.ui.TrayApplet;
import com.edduarte.protbox.ui.windows.RequestPromptWindow;
import com.edduarte.protbox.utils.Pair;
import com.edduarte.protbox.utils.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
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
import java.util.stream.Collectors;

/**
 * Protbox Registry (or PReg for short) is a parallel control data structure that assures the
 * coherence of the data in Prot folders and Shared folders. It contains structural information
 * about both folders (sub-files and sub-folders, encrypted and decrypted names, last modified
 * dates and lengths of contents).
 * <p>
 * Coherency checking and synchronization tasks run on a periodic basis and use that structural
 * information and the effective contents of each {@link PbxEntry} to take the appropriate data
 * transfer decisions.
 * <p>
 * Note that a PReg is a local, private data structure that helps a local {\protbox} instance to
 * take the appropriate, local decisions regarding file synchronizations, encryption/decryption
 * and recovery actions. In particular, a PReg is never synchronized with another one.
 *
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
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


    private transient Cipher CIPHER;


    private transient boolean initialized;


    private transient boolean currentlyIndexing;


    private transient Timer timerIndex;


    private transient DefaultWatcher sharedFolderWatcher;


    private transient DefaultWatcher protFolderWatcher;


    private transient RequestWatcher requestFileWatcher;


    /**
     * Constructs a new registry structure that links the Shared and the Prot folders using entries.
     */
    public PReg(PbxUser user, PbxPair pair, boolean isANewPReg) throws ProtboxException {
        this.id = "dir" + Utils.generateRandomHash();
        this.user = user;
        this.pair = pair;
        this.SKIP_WATCHER_ENTRIES = new ArrayList<>();
        this.initialized = false;
        this.root = new PbxFolder(this, null, "", "");
        if (isANewPReg) {
            try {
                Constants.moveContentsFromDirToDir(pair.getSharedFolderFile(), pair.getProtFolderFile());
            } catch (IOException ex) {
                throw new ProtboxException(ex);
            }
        }
    }


    public void initialize() throws GeneralSecurityException, IOException {
        if (initialized) {
            return;
        }

        // starts the cipher according to the chosen algorithm
        CIPHER = Cipher.getInstance(pair.getPairAlgorithm());

        // starts the synchronization threads, who are responsible of syncing elements between the folders
        SyncModule.start();

        // checks the registry periodically (every 2 seconds) and detects any changes made
        timerIndex = new Timer();
        timerIndex.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    executeIntegrityCheck();
                } catch (ProtboxException ex) {
                    run();
                }
            }
        }, 0, 5000);

        // starts the registry watchers for both prot and shared folders
        final Path sharedPath = Paths.get(pair.getSharedFolderPath());
        final Path protPath = Paths.get(pair.getProtFolderPath());
        sharedFolderWatcher = new DefaultWatcher(this, FolderOption.SHARED, sharedPath);
        protFolderWatcher = new DefaultWatcher(this, FolderOption.PROT, protPath);
        sharedFolderWatcher.start();
        protFolderWatcher.start();

        // starts a file watcher for creation of access request files
        requestFileWatcher = new RequestWatcher(sharedPath, detectedRequest -> {
            RequestPromptWindow.getInstance(this, detectedRequest);
        });
        requestFileWatcher.start();

        initialized = true;
    }


    public void stop() {
        if (initialized) {
            timerIndex.cancel();
            timerIndex.purge();

            if (sharedFolderWatcher != null)
                sharedFolderWatcher.interrupt();

            if (protFolderWatcher != null)
                protFolderWatcher.interrupt();

            if (requestFileWatcher != null)
                requestFileWatcher.interrupt();

            SyncModule.removeSyncEntriesOfRegistry(this);

            initialized = false;

        }
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


    public void executeIntegrityCheck() throws ProtboxException {
        if (currentlyIndexing) {
            return;
        }
        currentlyIndexing = true;
        integrityCheck(root, pair.getSharedFolderFile(), pair.getProtFolderFile());
        currentlyIndexing = false;
    }


    private void integrityCheck(PbxFolder folder, File sharedFolder, File protFolder) throws ProtboxException {
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


        // checking sub-files and sub-folders with entries (= already existed in the registry before checking)
        List<PbxFile> subFiles = folder.getSubFiles().collect(Collectors.toList());
        List<PbxFolder> subFolders = folder.getSubFolders().collect(Collectors.toList());

        for (PbxFile f : subFiles) {
            File sharedFile = sharedMap.get(f.encodedName());
            File protFile = protMap.get(f.realName());
            evaluate(f, sharedFile, protFile);
            filesAtShared.remove(sharedFile);
            filesAtProt.remove(protFile);
        }
        for (PbxFolder f : subFolders) {
            File sharedFile = sharedMap.get(f.encodedName());
            File protFile = protMap.get(f.realName());
            integrityCheck(f, sharedFile, protFile);
            evaluate(f, sharedFile, protFile);
            filesAtShared.remove(sharedFile);
            filesAtProt.remove(protFile);
        }


        // checking sub-files and sub-folders without entries (= did not exist in the registry before checking)
        for (File sharedFile : filesAtShared) {
            // iterating shared files that do not exist in this PReg
            try {
                String realName = convertEncodedNameToRealName(sharedFile.getName());
                File protFile = protMap.get(realName);
                PbxEntry newEntry = evaluate(null, sharedFile, protFile);
                if (sharedFile.isDirectory()) {
                    // iterate this folder with the entry that was just created
                    integrityCheck((PbxFolder) newEntry, sharedFile, protFile);
                }

            } catch (ProtboxException ex) {
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

            } catch (ProtboxException ex) {
                logger.error("Error while checking prot file without pair " + protFile.getName(), ex);
            }
        }
    }


    private PbxEntry evaluate(PbxEntry entry, File sharedFile, File protFile) throws ProtboxException {
        try {
            if (entry == null) {
                // is a file or folder that is not represented by an entry on this PReg
                if (Constants.verbose) {
                    logger.info("is a file or folder that is not represented by an entry on this PReg");
                }


                if (sharedFile == null && protFile != null) {
                    // new file at prot folder -> add to registry and sync to shared
                    if (Constants.verbose) {
                        logger.info("new file at prot folder -> add to registry and sync to shared");
                    }
                    return add(protFile, FolderOption.PROT);

                } else if (sharedFile != null && protFile == null) {
                    // new file at shared folder -> add to registry and sync to prot
                    if (Constants.verbose) {
                        logger.info("new file at shared folder -> add to registry and sync to prot");
                    }
                    return add(sharedFile, FolderOption.SHARED);

                } else if (sharedFile != null && protFile != null) {
                    // new files at both folders -> add to registry as conflicted copy and sync to shared
                    if (Constants.verbose) {
                        logger.info("new files at both folders -> add to registry as conflicted copy and sync to shared");
                    }
                    addConflicted(protFile, FolderOption.PROT);
                    return add(sharedFile, FolderOption.SHARED);
                }

            } else if (entry instanceof PbxFile) {
                // is a file that is already represented by an entry on this PReg

                PbxFile entry1 = (PbxFile) entry;
                if ((sharedFile == null || !sharedFile.exists()) &&
                        (protFile == null || !protFile.exists())) {
                    // entry was deleted from both folders -> keep entry and do nothing
//                    logger.info("entry was deleted from both folders -> keep entry and do nothing");

                } else if ((sharedFile == null || !sharedFile.exists()) && protFile != null) {
                    // file was deleted from shared folder
                    Date protLS = new Date(protFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getLastModifiedDate();

                    if (protLS.compareTo(entryLS) < 0) {
                        // prot file is more recent than entry -> sync prot file to shared folder
                        if (Constants.verbose) {
                            logger.info("prot file is more recent than entry -> sync prot file to shared folder");
                        }
                        SyncModule.toShared(this, entry);

                    } else {
                        // prot file is older than entry -> delete it while retaining data
                        if (Constants.verbose) {
                            logger.info("prot file is older than entry -> delete it while retaining data");
                        }
                        delete(protFile, FolderOption.PROT);

                    }

                } else if (sharedFile != null && protFile == null) {
                    // file was deleted from prot folder
                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getLastModifiedDate();

                    if (sharedLS.compareTo(entryLS) < 0) {
                        // shared file is more recent than entry -> sync shared file to prot folder
                        if (Constants.verbose) {
                            logger.info("shared file is more recent than entry -> sync shared file to prot folder");
                        }
                        SyncModule.toProt(this, entry);

                    } else {
                        // shared file is older than entry -> delete it while retaining data
                        if (Constants.verbose) {
                            logger.info("shared file is older than entry -> delete it while retaining data");
                        }
                        delete(sharedFile, FolderOption.SHARED);

                    }

                } else if (sharedFile != null && protFile != null) {
                    // file exists at both folders

                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date protLS = new Date(protFile.lastModified());
                    Date entryLS = entry1.getLatestSnapshot().getLastModifiedDate();
                    int compareSharedProt = sharedLS.compareTo(protLS);
                    int compareProtEntry = protLS.compareTo(entryLS);
                    int compareSharedEntry = sharedLS.compareTo(entryLS);

                    if (compareSharedProt == 0 && compareProtEntry == 0) {
                        // all last modified dates are equal -> do nothing
//                        logger.info("all last modified dates are equal -> do nothing");

                    } else if (compareSharedProt != 0) {

                        if (compareProtEntry == 0 && compareSharedEntry != 0) {
                            // shared file was updated -> sync shared to prot
                            if (Constants.verbose) {
                                logger.info("shared file was updated -> sync shared to prot");
                            }
                            SyncModule.toProt(this, entry);

                        } else if (compareProtEntry != 0 && compareSharedEntry == 0) {
                            // prot file was updated -> sync prot to shared
                            if (Constants.verbose) {
                                logger.info("prot file was updated -> sync prot to shared");
                            }
                            SyncModule.toShared(this, entry);

                        } else if (compareProtEntry != 0 && compareSharedEntry != 0) {
                            // both files were updated -> conflict
                            if (Constants.verbose) {
                                logger.info("both files were updated -> conflict");
                            }
                            addConflicted(protFile, FolderOption.PROT);
                            SyncModule.toProt(this, entry);
                        }
                    }
                }
            } else if (entry instanceof PbxFolder) {
                // is a folder that is already represented by an entry on this PReg
                if ((sharedFile == null || !sharedFile.exists()) && (protFile == null || !protFile.exists())) {
                    // entry was deleted from both folders -> keep entry and do nothing
                    if (Constants.verbose) {
                        logger.info("entry was deleted from both folders -> keep entry and do nothing");
                    }

                } else if ((protFile == null || !protFile.exists()) && (sharedFile != null && sharedFile.exists())) {
                    // entry was deleted at prot folder -> delete it from shared folder but keep entry
                    if (Constants.verbose) {
                        logger.info("entry was deleted at prot folder -> delete it from shared folder but keep entry");
                    }
                    deleteFilesFromEntry(entry);

                } else if ((sharedFile == null || !sharedFile.exists()) && (protFile != null && protFile.exists())) {
                    // entry was deleted at shared folder -> assure that it is updated, by syncing from prot to shared
                    if (Constants.verbose) {
                        logger.info("entry was deleted at shared folder -> assure that it is updated, by syncing from prot to shared");
                    }
                    SyncModule.toShared(this, entry);

                }
            }

        } catch (NullPointerException ex) {
            // This catch is here only to avoid crashing due to non-existent / null files
            ex.printStackTrace();
        }
        return entry;
    }


    // -- ADD METHODS --


    public PbxEntry add(File file, FolderOption fileFrom) throws ProtboxException {
        if (Constants.verbose) {
            logger.info("Adding " + file.getAbsolutePath());
        }
        return addAux(file, false, fileFrom);
    }


    public PbxEntry addConflicted(File file, FolderOption fileFrom) throws ProtboxException {
        if (Constants.verbose) {
            logger.info("Adding conflicted copy " + file.getAbsolutePath());
        }
        return addAux(file, true, fileFrom);
    }


    private PbxEntry addAux(File file, boolean conflicted, FolderOption fileFrom) throws ProtboxException {
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


    private PbxEntry addOnlyToPRegFromProt(File file, boolean conflicted) throws ProtboxException {
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
            throw new ProtboxException(ex);
        }
    }


    private PbxEntry addOnlyToPRegFromShared(File file, boolean conflicted) throws ProtboxException {
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
            throw new ProtboxException(ex);
        }
    }


    private PbxEntry addFinal(final File file, final PbxFolder parent,
                              final String realName, final String encodedName, final FolderOption fromFolder)
            throws ProtboxException, IOException, GeneralSecurityException {

        if (file.isDirectory()) {
            PbxFolder pair;

            try {
                // checks if a deleted PbxFolder that represents the files already exists
                pair = parent.getSubFolders()
                        .filter(PbxEntry::areNativeFilesDeleted)
                        .filter(p -> fromFolder == FolderOption.PROT ? p.realName().equals(realName) : p.encodedName().equals(encodedName))
                        .findFirst()
                        .get();

                // shows the existing PbxFolder
                restoreFolderFromEntry(pair);

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
                        .filter(PbxEntry::areNativeFilesDeleted)
                        .filter(p -> fromFolder == FolderOption.PROT ? p.realName().equals(realName) : p.encodedName().equals(encodedName))
                        .findFirst()
                        .get();

                // shows the existing PbxFile with updated data
                restoreFileFromEntry(pair, 0);

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


    private String convertEncodedNameToRealName(String encodedName) throws ProtboxException {

        // perform replacements from a normal name to a RFC 3548 compliant name
        String rfc3548Name = encodedName;
        rfc3548Name = StringUtils.replaceChars(rfc3548Name, '-', '/');
        rfc3548Name = StringUtils.replaceChars(rfc3548Name, '_', '+');

        byte[] decryptedNameData = decrypt(Base64.decodeBase64(rfc3548Name), false);

        try {
            return new String(decryptedNameData, "UTF-8");

        } catch (UnsupportedEncodingException ex) {
            return new String(decryptedNameData);
        }

    }


    private String convertRealNameToEncodedName(String realName) throws ProtboxException {
        String encodedName;

        try {
            byte[] encryptedNameData = encrypt(realName.getBytes("UTF-8"), false);
            encodedName = new String(Base64.encodeBase64(encryptedNameData), "UTF-8");

        } catch (UnsupportedEncodingException ex) {
            byte[] encryptedNameData = encrypt(realName.getBytes(), false);
            encodedName = new String(Base64.encodeBase64(encryptedNameData));
        }

        // perform replacements from a RFC 3548 compliant name to a normal name
        encodedName = StringUtils.replaceChars(encodedName, '/', '-');
        encodedName = StringUtils.replaceChars(encodedName, '+', '_');
        return encodedName;

    }


    private String realNameToConflicted(String realName) {
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
        deleteFilesFromEntry(entry);
        if (entry instanceof PbxFile) {
            PbxFile file = (PbxFile) entry;
            file.clearSnapshots();
        }
        entry.parentFolder().remove(entry);
    }


    public void delete(File file, FolderOption fromFolder) {
        if (Constants.verbose) {
            logger.info("Deleting file " + file.getAbsolutePath());
        }

        PbxFolder parent = goToFolder(file.getParentFile().getAbsolutePath(), fromFolder);
        PbxEntry toDelete;
        toDelete = parent.goToFile(file.getName());
        if (toDelete == null) {
            toDelete = parent.goToFolder(file.getName());
        }
        deleteFilesFromEntry(toDelete);
    }


    /**
     * Hides the file or folder, keeping it in the registry but deleting it from both PROT and SHARED folders.
     */
    private void deleteFilesFromEntry(PbxEntry entry) {

        if (entry instanceof PbxFolder) {
            PbxFolder folder = (PbxFolder) entry;
            folder.getSubFolders().forEach(this::deleteFilesFromEntry);
            folder.getSubFiles().forEach(this::deleteFilesFromEntry);
        }

        File fileAtProt = new File(pair.getProtFolderPath() + File.separator + entry.relativeRealPath());
        if (fileAtProt.exists()) {
            SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());
            SKIP_WATCHER_ENTRIES.add(fileAtProt.getParentFile().getAbsolutePath());
            Constants.delete(fileAtProt);
        }

        File fileAtShared = new File(pair.getSharedFolderPath() + File.separator + entry.relativeEncodedPath());
        if (fileAtShared.exists()) {
            SKIP_WATCHER_ENTRIES.add(fileAtShared.getAbsolutePath());
            SKIP_WATCHER_ENTRIES.add(fileAtShared.getParentFile().getAbsolutePath());
            Constants.delete(fileAtShared);
        }

        entry.areNativeFilesDeleted = true;
    }


    /**
     * Shows the folder represented by the specified entry, reversing the delete
     * process on both PROT and SHARED folders.
     */
    public void restoreFolderFromEntry(final PbxFolder pbxFolder) {
        if (!pbxFolder.areNativeFilesDeleted()) {
            return;
        }

        if (pbxFolder.parentFolder().areNativeFilesDeleted()) {
            restoreFolderFromEntry(pbxFolder.parentFolder());
        }

        pbxFolder.areNativeFilesDeleted = false;
        File fileAtProt = new File(pair.getProtFolderPath() + File.separator + pbxFolder.relativeRealPath());
        SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());

        fileAtProt.mkdir();
        TrayApplet.getInstance().showBalloon("Folder Restored",
                "The folder \"" + pbxFolder.realName() + "\" was restored to its original location.",
                TrayIcon.MessageType.INFO);

        SyncModule.toShared(this, pbxFolder);
    }


    /**
     * Shows the folder represented by the specified entry, reversing the delete
     * process on both PROT and SHARED folders.
     */
    public void restoreFileFromEntry(final PbxFile pbxFile, final int snapshotIndex) {

        if (pbxFile.parentFolder().areNativeFilesDeleted()) {
            restoreFolderFromEntry(pbxFile.parentFolder());
        }

        pbxFile.areNativeFilesDeleted = false;
        File fileAtProt = new File(pair.getProtFolderPath() + File.separator + pbxFile.relativeRealPath());
        SKIP_WATCHER_ENTRIES.add(fileAtProt.getAbsolutePath());

        try {
            pbxFile.writeSnapshotToFile(snapshotIndex, fileAtProt, FolderOption.PROT);
            TrayApplet.getInstance().showBalloon("File Restored",
                    "The file \"" + pbxFile.realName() + "\" was restored to its original location.",
                    TrayIcon.MessageType.INFO);

            SyncModule.toShared(this, pbxFile);

        } catch (ProtboxException ex) {
            if (Constants.verbose) {
                logger.error("Error while writing file data for " + pbxFile.toString(), ex);
            }
            pbxFile.areNativeFilesDeleted = true;
            SKIP_WATCHER_ENTRIES.remove(fileAtProt.getAbsolutePath());
        }
    }


    public DefaultMutableTreeNode buildEntryTree() {
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(root, true);
        build(treeRoot, root);
        return treeRoot;
    }


    private boolean build(final DefaultMutableTreeNode treeRoot, final PbxFolder root) {
        root.getSubFolders().forEach(f -> {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(true);
            build(newNode, f);
            treeRoot.add(newNode);
        });

        root.getSubFiles().forEach(f -> {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(false);
            treeRoot.add(newNode);
        });

        return false;
    }


    // -- CIPHER METHODS --


    public byte[] encrypt(byte[] decryptedData, boolean appendChecksum) throws ProtboxException {
        try {
            CIPHER.init(Cipher.ENCRYPT_MODE, pair.getPairKey());

            byte[] integrityControlValue = null;
            int checksumLength = 0;
            if (appendChecksum) {
                Mac mac = Mac.getInstance("HmacSHA512");
                mac.init(pair.getIntegrityKey());

                integrityControlValue = mac.doFinal(decryptedData);
                checksumLength = 64;
            }

            byte[] encryptedData = CIPHER.doFinal(decryptedData);

            boolean isCBC = pair.getPairAlgorithm().contains("CBC");
            byte[] iv = CIPHER.getIV();
            int ivLength = isCBC ? 16 : 0;

            byte[] result = new byte[checksumLength + ivLength + encryptedData.length];
            if (appendChecksum) {
                System.arraycopy(integrityControlValue, 0, result, 0, checksumLength);
            }
            if (isCBC) {
                System.arraycopy(iv, 0, result, checksumLength, ivLength);
            }
            System.arraycopy(encryptedData, 0, result, checksumLength + ivLength, encryptedData.length);
            return result;

        } catch (GeneralSecurityException ex) {
            throw new ProtboxException(ex);
        }
    }


    public byte[] decrypt(byte[] encryptedData, boolean hasChecksum) throws ProtboxException {
        try {
            byte[] dataToDecrypt;

            int checksumLength = hasChecksum ? 64 : 0;

            if (pair.getPairAlgorithm().contains("CBC")) {
                byte[] iv = new byte[16];
                System.arraycopy(encryptedData, checksumLength, iv, 0, 16);

                int dataToDecryptLength = encryptedData.length - checksumLength - 16;
                dataToDecrypt = new byte[dataToDecryptLength];
                System.arraycopy(encryptedData, checksumLength + 16, dataToDecrypt, 0, dataToDecryptLength);

                CIPHER.init(Cipher.DECRYPT_MODE, pair.getPairKey(), new IvParameterSpec(iv));

            } else {
                int dataToDecryptLength = encryptedData.length - checksumLength;
                dataToDecrypt = new byte[dataToDecryptLength];
                System.arraycopy(encryptedData, checksumLength, dataToDecrypt, 0, dataToDecryptLength);

                CIPHER.init(Cipher.DECRYPT_MODE, pair.getPairKey());
            }

            byte[] result = CIPHER.doFinal(dataToDecrypt);
            boolean isValid = true;

            if (hasChecksum) {
                byte[] fileCheckSum = new byte[checksumLength];
                System.arraycopy(encryptedData, 0, fileCheckSum, 0, checksumLength);

                Mac mac = Mac.getInstance("HmacSHA512");
                mac.init(pair.getIntegrityKey());
                byte[] integrityControlValue = mac.doFinal(result);

                isValid = Arrays.equals(fileCheckSum, integrityControlValue);
            }

            if (isValid) {
                return result;

            } else {
                throw new ProtboxException("Protected file contains invalid checksum.");
            }

        } catch (GeneralSecurityException ex) {
            throw new ProtboxException(ex);
        }
    }


    public void changeProtPath(String newPath) throws ProtboxException {
        try {
            TrayApplet.getInstance().setStatus(TrayApplet.TrayStatus.LOADING, "Moving files to new prot folder...");

            // 1) remove entries from the syncing threads
            protFolderWatcher.interrupt();
            Pair<List<PbxEntry>> removedEntries = SyncModule.removeSyncEntriesOfRegistry(this);


            // 2) set new path
            File oldProtFile = pair.getProtFolderFile();
            String newProtPath = new File(newPath).getAbsolutePath();
            File newProtFile = new File(newProtPath + root.realName());


            // 3) move all files and folders from old folder to new folder
            Constants.moveContentsFromDirToDir(oldProtFile, newProtFile);
            executeIntegrityCheck();


            // 4) start new Watcher on the new Folder
            protFolderWatcher = new DefaultWatcher(this, FolderOption.PROT, newProtFile.toPath());
            protFolderWatcher.start();


            // 5) add removed entries to the syncing threads again
            for (PbxEntry e : removedEntries.first)
                SyncModule.toProt(this, e);

            for (PbxEntry e : removedEntries.second)
                SyncModule.toShared(this, e);

            // 6) update the pair
            pair.setProtFolderPath(newProtPath);

            TrayApplet.getInstance().setStatus(TrayApplet.TrayStatus.OKAY, "");
        } catch (IOException ex) {
            throw new ProtboxException(ex);
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

        root.getSubFiles().forEach(f -> {
            sb.append(indent);
            sb.append(f.realName());
            sb.append(" (file");

            if (f.areNativeFilesDeleted()) {
                sb.append(", deleted)\n");

            } else {
                sb.append(")\n");
            }
        });

        root.getSubFolders().forEach(f -> {
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
        });

        return sb;
    }
}
