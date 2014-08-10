package edduarte.protbox.core.directory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.User;
import edduarte.protbox.core.synchronization.Sync;
import edduarte.protbox.core.watcher.DirectoryWatcher;
import edduarte.protbox.core.watcher.SpecificWatcher;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.ui.UserValidation;
import edduarte.protbox.util.DuoRef;

import javax.crypto.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class Registry implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient static org.slf4j.Logger logger = LoggerFactory.getLogger(Registry.class);

    /**
     * The configured SHARED shared folder where the encrypted files will be.
     */
    public final String SHARED_PATH;

    /**
     * The configured PROT folder where every file from the SHARED folder will be stored in decrypted form.
     */
    public String OUTPUT_PATH;

    /**
     * The initial entry that links to every other entry through a node-based implementation.
     */
    private PairFolder root;

    /**
     * The user with local access to this directory. When this directory is serialized, the serialized file is
     * encrypted with this user's public key, guaranteeing that every data stored in this directory, including
     * entries and the secret key, will be protected. With this, to load the serialized file, it is always required
     * to validate the users Citizen Card.
     */
    public final User accessUser;

    /**
     * The name of the directory, which is used by the serialized directory file.
     *
     * Every time the application is closed, this directory is serialized into a file with this name, and every
     * time the application is opened, this directory is resumed using the stored file with this name.
     */
    public final String NAME = Constants.generateUniqueDirID();

    /**
     * A collection of files that were changed directly by this application, meaning that they do not need to be
     * caught as events by any of the DirectoryWatchers.
     */
    public final List<String> SKIP_WATCHER_ENTRIES;

    /**
     * Simple access to the algorithm used by this directory. This is used to instantiate the Cipher
     * per execution, since the Cipher object cannot be serialized.
     */
    private final String algorithm;

    /**
     * The secret key used by this Directory. This key is private and completely restricted from use.
     */
    private final SecretKey KEY;

    /**
     * A simple Cipher used with the secret key to encrypt or decrypt files in this Directory.
     */
    private transient Cipher CIPHER;

    private transient boolean initialized;

    private transient boolean currentlyIndexing;

    private transient Timer timerIndex;

    private transient Thread dropboxWatcher;

    private transient Thread protboxWatcher;

    private transient Thread askFileWatcher;



    /**
     * Constructs a new Directory structure that links the Shared and the Output folders using entries.
     */
    public Registry(User accessUser, String sharedPath, String outputPath, String algorithm, SecretKey key, boolean isANewDirectory) throws ProtException {
        File sharedPathFile = new File(sharedPath);
        File outputPathFile = new File(outputPath);
        this.accessUser = accessUser;
        SHARED_PATH = sharedPathFile.getAbsolutePath();
        OUTPUT_PATH = outputPathFile.getAbsolutePath();
        this.algorithm = algorithm;
        this.KEY = key;
        this.SKIP_WATCHER_ENTRIES = new ArrayList<>();
        this.initialized = false;
        this.root = new PairFolder(null, "", "");
        if(isANewDirectory){
            try{
                Constants.moveContentsFromDirToDir(sharedPathFile, outputPathFile);
            }catch (IOException ex) {
                throw new ProtException(ex);
            }
        }
    }



    public void initialize() throws ProtException, GeneralSecurityException {
        if(initialized)
            return;

        try{
            // starts the cipher according to the chosen algorithm
            CIPHER = Cipher.getInstance(algorithm+"/ECB/PKCS5Padding");

            // checks the directory periodically (every 2 seconds) for existing
            // files at folders and detects changes made
            timerIndex = new Timer();
            timerIndex.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        indexingDirectory();
                    } catch (ProtException ex) {
                        run();
                    }
                }
            }, 0, 5000);

            // starts the directory watchers at both folders
            final Path dropPath = Paths.get(SHARED_PATH);
            final Path protPath = Paths.get(OUTPUT_PATH);
            dropboxWatcher = new Thread(new DirectoryWatcher(this, Source.SHARED, dropPath));
            protboxWatcher = new Thread(new DirectoryWatcher(this, Source.PROT, protPath));
            dropboxWatcher.start();
            protboxWatcher.start();

            // starts a specific watcher for addition of files that start with "»ask"
            askFileWatcher = new Thread(new SpecificWatcher(dropPath, new SpecificWatcher.Process(){
                @Override
                public void run(File detectedFile) throws IOException, InterruptedException {
                    if(detectedFile.getName().contains("»ask") &&
                            detectedFile.getName().substring(4).equalsIgnoreCase(accessUser.getId())){
                        Thread.sleep(2000);
                        UserValidation.getInstance(Registry.this, algorithm, KEY, dropPath.getFileName().toString(), detectedFile);
                    }
                }
            }));
            askFileWatcher.start();


            // starts the syncing threads, who are responsible of syncing elements between the folders
            Sync.start();


            initialized = true;
        }catch (IOException ex){
            throw new ProtException(ex);
        }
    }


    public void stop() {
        timerIndex.cancel();
        timerIndex.purge();

        if(dropboxWatcher!=null)
            dropboxWatcher.interrupt();

        if(protboxWatcher!=null)
            protboxWatcher.interrupt();

        if(askFileWatcher!=null)
            askFileWatcher.interrupt();

        Sync.removeEntriesOfDirectory(this);

        initialized = false;
    }


    private PairFolder goToFolder(String path, Source at) {

        String relative = getRelativePath(path, at);
        if(relative.equals("")){
            return root;
        }

        String[] pathParts = relative.split("/");
        PairFolder atFolder = root;
        for(String next : pathParts){
            atFolder = atFolder.goToFolder(next);
        }

        return atFolder;
    }

    private String getRelativePath(String absolutePath, Source at){
        String toRemoveFromPath = "";
        if(at.equals(Source.SHARED))
            toRemoveFromPath = SHARED_PATH + File.separator;
        else if(at.equals(Source.PROT))
            toRemoveFromPath = OUTPUT_PATH + File.separator;

        toRemoveFromPath = toRemoveFromPath.replace("\\", "/");
        return new File(toRemoveFromPath).toURI().relativize(new File(absolutePath).toURI()).getPath();
    }





    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!! INDEXING !!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public void indexingDirectory() throws ProtException {
        if(currentlyIndexing){
            return;
        }
        currentlyIndexing = true;
        indexingRecursive(root, new File(SHARED_PATH), new File(OUTPUT_PATH));
        currentlyIndexing = false;
    }

    private void indexingRecursive(PairFolder checkingEntry, File sharedFolder, File outputFolder) throws ProtException {
        if(checkingEntry==null)
            return;
        // generate structures to perform a careful ONE-BY-ONE file and folder checking
        Map<String, File> dropMap = new HashMap<>();
        Map<String, File> protMap = new HashMap<>();
        List<File> filesAtShared = new ArrayList<>();
        List<File> filesAtOutput = new ArrayList<>();
        if(sharedFolder!=null){
            try{
                for(File f : sharedFolder.listFiles()){
                    if(f.getName().charAt(0)!='»') {
                        filesAtShared.add(f);
                        dropMap.put(f.getName(), f);
                    }
                }
            }catch (Exception ex){}
        }
        if(outputFolder!=null){
            try{
                Collections.addAll(filesAtOutput, outputFolder.listFiles());
                for(File f : filesAtOutput){
                    protMap.put(f.getName(), f);
                }
            }catch (Exception ex){}
        }


        // files and dirs with entries (= already existed in the directory before checking)
        List<PairFile> entryFiles = new ArrayList<>();
        entryFiles.addAll(checkingEntry.files);
        for(PairFile f : entryFiles){
            File sharedFile = dropMap.get(f.encodedName());
            File outputFile = protMap.get(f.realName());
            evaluate(f, sharedFile, outputFile);
            filesAtShared.remove(sharedFile);
            filesAtOutput.remove(outputFile);
        }
        List<PairFolder> entryFolders = new ArrayList<>();
        entryFolders.addAll(checkingEntry.folders);
        for (PairFolder f : entryFolders){
            File sharedFile = dropMap.get(f.encodedName());
            File outputFile = protMap.get(f.realName());
            indexingRecursive(f, sharedFile, outputFile);
            evaluate(f, sharedFile, outputFile);
            filesAtShared.remove(sharedFile);
            filesAtOutput.remove(outputFile);
        }


        // files and dirs without entries (= did not exist in the directory before checking)
        for (File sharedFile : filesAtShared) {
            // iterating shared files that do not exist at directory
            try {
                String realName = convertEncodedNameToRealName(sharedFile.getName());
                File outputFile = protMap.get(realName);
                Pair thisEntry = evaluate(null, sharedFile, outputFile);
                if(sharedFile.isDirectory()){
                    // iterate this folder with the entry that was just created
                    indexingRecursive((PairFolder)thisEntry, sharedFile, outputFile);
                }

            } catch(GeneralSecurityException ex) {
                ex.printStackTrace();
                logger.error(ex.toString());
                continue;
            }
        }
        for (File outputFile : filesAtOutput) {
            // iterating output files that do not exist at directory
            try {
                String encodedName = convertRealNameToEncodedName(outputFile.getName());
                File sharedFile = protMap.get(encodedName);
                Pair thisEntry = evaluate(null, sharedFile, outputFile);
                if(outputFile.isDirectory()){
                    // iterate this folder with the entry that was just created
                    indexingRecursive((PairFolder)thisEntry, sharedFile, outputFile);
                }

            } catch(GeneralSecurityException ex) {
                logger.error(ex.toString());
                ex.printStackTrace();
                continue;
            }
        }
    }

    private Pair evaluate(Pair entry, File sharedFile, File outputFile) throws ProtException {
        try{
            if(entry==null){
                if(sharedFile==null && outputFile!=null){ // new file at output folder -> add to directory and sync to shared
                    return add(outputFile, Source.PROT);
                } else if(sharedFile!=null && outputFile==null){ // new file at shared folder -> add to directory and sync to output
                    return add(sharedFile, Source.SHARED);
                } else if(sharedFile!=null && outputFile!=null){// new files at both folders -> add both to directory as conflict copies and sync to each side
                    addConflicted(outputFile, Source.PROT);
                    return add(sharedFile, Source.SHARED);
                }
            } else if(entry instanceof PairFile) { // is a file that already existed at directory
                PairFile entry0 = (PairFile) entry;
                if ((sharedFile == null || !sharedFile.exists()) && (outputFile == null || !outputFile.exists()) && !entry.hidden) { // entry was deleted from both folders -> delete it from directory
                    permanentDelete(entry);
                } else if ((sharedFile == null || !sharedFile.exists()) && outputFile != null) { // file was deleted from shared folder
                    Date outputLS = new Date(outputFile.lastModified());
                    Date entryLS = entry0.lastModified();

                    if (outputLS.compareTo(entryLS) <= 0) { // outputFile is more recent -> sync output to shared
                        Sync.toShared(this, entry);
                    }
                } else if (sharedFile != null && outputFile == null) { // file was deleted from output folder
                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date entryLS = entry0.lastModified();

                    if (sharedLS.compareTo(entryLS) <= 0) { // sharedFile is more recent -> sync shared to output
                        Sync.toOutput(this, entry);
                    }
                } else if (sharedFile != null && outputFile != null) { // file exists at directory and at both folders
                    Date sharedLS = new Date(sharedFile.lastModified());
                    Date outputLS = new Date(outputFile.lastModified());
                    Date entryLS = entry0.lastModified();
                    int compareSharedOutput = sharedLS.compareTo(outputLS);
                    int compareOutputEntry = outputLS.compareTo(entryLS);
                    int compareSharedEntry = sharedLS.compareTo(entryLS);

                    if (compareSharedOutput == 0 && compareOutputEntry == 0) { // all last modified dates are equal -> do nothing
                        // do nothing

                    } else if (compareSharedOutput != 0) {

                        if (compareOutputEntry == 0 && compareSharedEntry != 0) { // sharedFile was updated -> sync shared to output
                            Sync.toOutput(this, entry);

                        } else if (compareOutputEntry != 0 && compareSharedEntry == 0) { // outputFile was updated -> sync output to shared
                            Sync.toShared(this, entry);

                        } else if (compareOutputEntry != 0 && compareSharedEntry != 0) { // both files were updated -> conflict
                            addConflicted(outputFile, Source.PROT);
                            Sync.toOutput(this, entry);
                        }
                    }
                }
            } else if(entry instanceof PairFolder) {
                if ((sharedFile == null || !sharedFile.exists()) && (outputFile == null || !outputFile.exists()) && !entry.hidden) { // entry was deleted from both folders -> delete it from directory
                    permanentDelete(entry);
                } else if ((sharedFile == null || !sharedFile.exists()) && outputFile != null) { // entry was deleted from shared folder
                    hideEntry(entry);
                } else if (sharedFile!=null) { // entry was deleted from output folder or was not deleted at all -> make sure it still exists!
                    Sync.toOutput(this, entry);
                }
            }
        }catch (NullPointerException ex) {}
        return entry;
    }





    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!! ADD ACTIONS !!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public Pair add(File file, Source fileFrom) throws ProtException {
        logger.info("Adding "+file.getAbsolutePath());
        if(fileFrom.equals(Source.PROT)){
            return addFromOutput(file, false);
        }
        else if(fileFrom.equals(Source.SHARED)){
            return addFromShared(file, false);
        }
        return null;
    }
    public Pair addConflicted(File file, Source fileFrom) throws ProtException {
        logger.info("Adding "+file.getAbsolutePath());
        if(fileFrom.equals(Source.PROT)){
            return addFromOutput(file, true);
        }
        else if(fileFrom.equals(Source.SHARED)){
            return addFromShared(file, true);
        }
        return null;
    }
    private Pair addFromOutput(File file, boolean conflicted) throws ProtException {
        Pair newEntry = addOnlyToDirectoryFromOutput(file, conflicted);
        if(newEntry!=null)
            Sync.toShared(this, newEntry);
        return newEntry;
    }
    private Pair addFromShared(File file, boolean conflicted) throws ProtException {
        Pair newEntry = addOnlyToDirectoryFromShared(file, conflicted);
        if(newEntry!=null)
            Sync.toOutput(this, newEntry);
        return newEntry;
    }
    private Pair addOnlyToDirectoryFromOutput(File file, boolean conflicted) throws ProtException {
        try{
            String realName = file.getName();
            if(conflicted && !file.isDirectory()){
                realName = realNameToConflicted(realName);
                File newConflictFile = new File(file.getParentFile(), realName);
                SKIP_WATCHER_ENTRIES.add(newConflictFile.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(file.getAbsolutePath());

                // get the data from the added file
                byte[] outputFileData = FileUtils.readFileToByteArray(file);

                // move data to a new conflicted file and delete old file
                FileUtils.writeByteArrayToFile(newConflictFile, outputFileData);
                Constants.delete(file);
            }
            String encodedName = convertRealNameToEncodedName(realName);


            String parentPath = file.getParentFile().getAbsolutePath();
            PairFolder parent = null;
            if(!parentPath.equalsIgnoreCase(OUTPUT_PATH))
                parent = goToFolder(parentPath, Source.PROT);

            if(parent==null)
                parent = root;

            return addFinalize(file, parent, realName, encodedName, Source.PROT);
        }catch (IOException|GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }
    private Pair addOnlyToDirectoryFromShared(File file, boolean conflicted) throws ProtException {
        try{
            String encodedName = file.getName();
            String realName = convertEncodedNameToRealName(encodedName);
            if(conflicted && !file.isDirectory()){
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
            if(!parentPath.equalsIgnoreCase(SHARED_PATH))
                parent = goToFolder(parentPath, Source.SHARED);

            if(parent==null)
                parent = root;

            return addFinalize(file, parent, realName, encodedName, Source.SHARED);
        }catch (IOException|GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }
    private Pair addFinalize(File file, PairFolder parent, String realName, String encodedName, Source fromFolder) throws ProtException, IOException, GeneralSecurityException {
        Pair newPbxEntry;

        if(file.isDirectory()) { // create a new PbxFolder
            // checks if entry already existed at parent
            for(PairFolder entry : parent.folders){
                if(entry.realName().equals(realName) || entry.encodedName().equals(encodedName)){ // entry already exists!!!
                    if(entry.isHidden()){ // if it's hidden, restore it
                        this.showEntry(entry);
//                        return null; // no need to return entry for syncing since showEntry will make sure the files appear again
//                    } else {
//                        return entry; // TODO
                    }
                    return null;
                }
            }

            // does not exist, so create it
            newPbxEntry = new PairFolder(parent, encodedName, realName);
            parent.addFolder((PairFolder) newPbxEntry);

            if(fromFolder.equals(Source.SHARED)){
                new File(file, "»==").createNewFile();
            }
        } else { // create a new PbxFile
            // checks if entry already existed at parent
            for(PairFile entry : parent.files){
                if(entry.realName().equals(realName) || entry.encodedName().equals(encodedName)){ // entry already exists!!!
                    entry.lastModified = new Date(file.lastModified());
                    if(entry.isHidden()){ // if it's hidden, update the byte data and restore it
                        byte[] newData = FileUtils.readFileToByteArray(file);
                        if(fromFolder.equals(Source.SHARED)) {
                            entry.data = decrypt(newData);
                        }
                        else if (fromFolder.equals(Source.PROT)) {
                            entry.data = newData;
                        }
                        this.showEntry(entry);
                    }
                    return null;
                }
            }


            // entry is completely new -> add it normally to its parent
            newPbxEntry = new PairFile(parent, encodedName, realName, new Date(file.lastModified()), file.length());
            parent.addFile((PairFile) newPbxEntry);
        }
        return newPbxEntry;
    }

    private String convertEncodedNameToRealName(String encodedName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.DECRYPT_MODE, KEY);
        try {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("~", "/"))), "UTF8");
        }catch (UnsupportedEncodingException ex) {
            return new String(c.doFinal(Base64.decodeBase64(encodedName.replaceAll("~", "/"))));
        }
    }

    private String convertRealNameToEncodedName(String realName) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.ENCRYPT_MODE, KEY);
        try{
            return new String(Base64.encodeBase64(c.doFinal(realName.getBytes("UTF8"))), "UTF8").replaceAll("/", "~");
        }catch (UnsupportedEncodingException ex) {
            return new String(Base64.encodeBase64(c.doFinal(realName.getBytes()))).replaceAll("/", "~");
        }
    }

    private String realNameToConflicted(String realName) throws GeneralSecurityException {
        String conflictText = " (conflicted copy from "+accessUser.getName()+")";

        int dotIndex = realName.lastIndexOf(".");
        String name, ext;
        if(dotIndex==-1){
            name = realName;
            ext = "";
        } else {
            name = realName.substring(0, dotIndex);
            ext = realName.substring(dotIndex);
        }
        return name+conflictText+ext;
    }




    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!! DELETE ACTIONS !!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public void permanentDelete(Pair entry){
        entry.parentFolder().removeEntry(entry);
        emptyData(entry);
    }

    public void delete(Path filePath, Source fromFolder) throws ProtException {
        if(Constants.verboseMode) logger.info("Deleting file "+filePath);

        PairFolder parent = goToFolder(filePath.getParent().toString(), fromFolder);
        Pair toDelete;
        toDelete = parent.goToFile(filePath.getFileName().toString());
        if(toDelete==null){
            toDelete = parent.goToFolder(filePath.getFileName().toString());
        }
        hideEntry(toDelete);
    }

    /**
     * Hides the file or folder, keeping it in the directory but deleting it from both PROT and SHARED folders
     * @throws ProtException if the file was not successfully read
     */
    private void hideEntry(Pair entry) throws ProtException {
        if(entry.isHidden()){
            return;
        }

        if(entry instanceof PairFolder){
            for(PairFolder f : ((PairFolder)entry).folders)
                hideEntry(f);

            for(PairFile f : ((PairFolder)entry).files)
                hideEntry(f);
        }

        try{
            File fileAtOutput = new File(OUTPUT_PATH + File.separator + entry.relativeRealPath());
            if(fileAtOutput.exists()){
                if(entry instanceof PairFile){
                    ((PairFile)entry).data = FileUtils.readFileToByteArray(fileAtOutput);
                }
                SKIP_WATCHER_ENTRIES.add(fileAtOutput.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtOutput.getParentFile().getAbsolutePath());
                Constants.delete(fileAtOutput);
            }

            File fileAtShared = new File(SHARED_PATH + File.separator + entry.relativeEncodedPath());
            if(fileAtShared.exists()){
                if(entry instanceof PairFile){
                    ((PairFile)entry).data = decrypt(FileUtils.readFileToByteArray(fileAtShared));
                }
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getAbsolutePath());
                SKIP_WATCHER_ENTRIES.add(fileAtShared.getParentFile().getAbsolutePath());
                Constants.delete(fileAtShared);
            }

        }catch (IOException | GeneralSecurityException ex){
            throw new ProtException(ex);
        }

        entry.hidden = true;
    }

    /**
     * Shows the file or folder, reversing the delete project on both PROT and SHARED folders
     * @throws IOException if the file was not successfully read
     */
    public void showEntry(Pair entry) throws IOException {
        if(!entry.isHidden()){
            return;
        }

        if(entry.parentFolder().isHidden()){
            showEntry(entry.parentFolder());
        }

        entry.hidden = false;
        File fileAtOutput = new File(OUTPUT_PATH + File.separator + entry.relativeRealPath());
        SKIP_WATCHER_ENTRIES.add(fileAtOutput.getAbsolutePath());

        if(entry instanceof PairFile){
            PairFile pbxFile = (PairFile)entry;
            pbxFile.lastModified = new Date(fileAtOutput.lastModified());
            FileUtils.writeByteArrayToFile(fileAtOutput, pbxFile.data);
            TrayApplet.getInstance().baloon("File Restored",
                    "The deleted file \"" + entry.realName() + "\" was restored to its original location.",
                    TrayIcon.MessageType.INFO);
            emptyData(entry);

        } else if(entry instanceof PairFolder){
            fileAtOutput.mkdir();
            TrayApplet.getInstance().baloon("Folder Restored",
                    "The deleted folder \"" + entry.realName() + "\" was restored to its original location.",
                    TrayIcon.MessageType.INFO);
        }

        Sync.toShared(this, entry);
    }

    public DefaultMutableTreeNode buildDeletedTree() {
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(root, true);
        build(treeRoot, root);
        return treeRoot;
    }
    private boolean build(DefaultMutableTreeNode treeRoot, PairFolder root) {
        boolean deletedFileWasAdded = false;

        for(PairFolder f : root.folders) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(true);
            deletedFileWasAdded = build(newNode, f);
            if(f.hidden || deletedFileWasAdded)
                treeRoot.add(newNode);
        }

        for(PairFile f : root.files) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(f, true);
            newNode.setAllowsChildren(false);
            if(f.hidden){
                deletedFileWasAdded = true;
                treeRoot.add(newNode);
            }
        }

        return deletedFileWasAdded;
    }

    private static void emptyData(Pair entry){
        // suggest Java Garbage Collector to stop storing this entry's data
        if(entry instanceof PairFile){
            PairFile pbxFile = (PairFile)entry;
            if(pbxFile.data!=null){
                Arrays.fill(pbxFile.data, (byte)0);
                pbxFile.data = null;
                System.gc();
            }
        }
    }



    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!! MODIFY ACTIONS !!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

//    public void modify(Path filePath, Source fromFolder) {
//        System.err.println("LOOKING FOR PARENT OF:"+filePath.toString());
//        PbxFolder parent = goToFolder(filePath.getParent().toString(), fromFolder);
//
//        // modified entry is a file
//        PbxEntry toModify = parent.goToFile(filePath.getFileName().toString());
//
//        if(toModify==null){
//            // modified entry is a folder
//            System.err.println("LOOKING FOR FOLDER ITSELF OF:"+filePath.toString());
//            toModify = parent.goToFolder(filePath.getFileName().toString());
//        }
//
//        modify(toModify, fromFolder);
//    }
//    private void modify(PbxEntry entry, Source fromFolder) {
//
//        if (fromFolder.equals(Source.SHARED)) {
//            if(entry instanceof PbxFile){
//                File modifiedFile = new File(SHARED_PATH + File.separator + entry.relativeEncodedPath());
//                PbxFile pbxFile = (PbxFile)entry;
//                pbxFile.fileSize = modifiedFile.length();
//            }
//            Sync.toOutput(this, entry);
//
//        } else if (fromFolder.equals(Source.PROT)) {
//            if(entry instanceof PbxFile) {
//                File modifiedFile = new File(OUTPUT_PATH + File.separator + entry.relativeRealPath());
//                ((PbxFile)entry).fileSize = modifiedFile.length();
//            }
//            Sync.toShared(this, entry);
//        }
//    }









    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!! CIPHER ACTIONS !!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    public byte[] encrypt(byte[] data) throws GeneralSecurityException {
        CIPHER.init(Cipher.ENCRYPT_MODE, KEY);
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
//            // encrypt data with directory's key, which may result in non-padded results
//            CIPHER.init(Cipher.ENCRYPT_MODE, KEY);
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
        CIPHER.init(Cipher.DECRYPT_MODE, KEY);
        return CIPHER.doFinal(data);

        // DEPRECATED: GET REAL SIZE AT THE END OF FILE WITHIN 4 BYTES FOR PADDING SPECIFICATION
//        // get real size from the last 4 bytes of the file
//        int len = data.length;
//        int realSize =
//                ByteBuffer.wrap(new byte[]{data[len - 4], data[len - 3], data[len - 2], data[len - 1]}).getInt();
////        System.out.println(realSize);
//
//        // get the encrypted data from every byte except for the last 4 and decrypt them with directory's key
//        data = Arrays.copyOf(data, len-4);
////        System.out.println(data.length);
//
//        CIPHER.init(Cipher.DECRYPT_MODE, KEY);
//        byte[] decrypted = CIPHER.doFinal(data);
//        return Arrays.copyOf(decrypted, realSize);
    }










    public void changeOutputPath(String newPath) throws ProtException {
        try {
            TrayApplet.getInstance().status(TrayApplet.TrayStatus.LOADING, "Moving files to new output folder...");

            // 1) remove entries from the syncing threads
            protboxWatcher.interrupt();
            DuoRef<List<Pair>> removedEntries = Sync.removeEntriesOfDirectory(this);


            // 2) set new path
            File oldOutputFile = new File(OUTPUT_PATH);
            OUTPUT_PATH = new File(newPath).getAbsolutePath();
            File newOutputFile = new File(OUTPUT_PATH + root.realName());


            // 3) move all files and folders from old folder to new folder
            Constants.moveContentsFromDirToDir(oldOutputFile, newOutputFile);
            indexingDirectory();


            // 4) start new Watcher on the new Folder
            protboxWatcher = new Thread(new DirectoryWatcher(this, Source.PROT, newOutputFile.toPath()));
            protboxWatcher.start();


            // 5) add removed entries to the syncing threads again
            for(Pair e : removedEntries.getFirst())
                Sync.toOutput(this, e);

            for(Pair e : removedEntries.getSecond())
                Sync.toShared(this, e);


            TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
        }catch (IOException ex) {
            throw new ProtException(ex);
        }
    }
    public void openExplorerFolder(Source folderToOpen) throws IOException {
        String path = "";
        if(folderToOpen.equals(Source.SHARED))
            path = SHARED_PATH;
        else if(folderToOpen.equals(Source.PROT))
            path = OUTPUT_PATH;

        if(Constants.OS.equals("windows"))
            Runtime.getRuntime().exec("explorer " + path);
        else if(Constants.OS.equals("mac"))
            Runtime.getRuntime().exec("open "+ path);
        else {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(path));
            }
        }
    }





    @Override
    public boolean equals(Object o){
        if(o==null){
            return false;
        } else if(this!=o) {
            return false;
        } else if(!(o instanceof Registry))
            return false;

        Registry d = (Registry) o;
        return SHARED_PATH.equalsIgnoreCase(d.SHARED_PATH) && OUTPUT_PATH.equalsIgnoreCase(d.OUTPUT_PATH);
    }



    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String indent = "   ";
        sb.append(new File(SHARED_PATH).getName()+"\n");
        return "\n"+print(sb, root, indent, indent).toString();
    }
    private StringBuffer print(StringBuffer sb, PairFolder root, String originalIndent, String indent){
        for(PairFile f : root.files){
            sb.append(indent+f.realName()+" (file");
            if(f.isHidden())
                sb.append(", hidden)\n");
            else
                sb.append(")\n");
        }
        for(PairFolder f : root.folders){
            if(f.parentFolder()!=null)
                sb.append(indent+f.realName()+" (folder, parent:"+f.parentFolder().realName()+")\n");
            else
                sb.append(indent+f.realName()+" (folder, parent:ROOT)\n");
            print(sb, f, originalIndent, indent+originalIndent);
        }
        return sb;
    }
}
