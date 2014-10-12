package edduarte.protbox.core.registry;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderOption;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.tuples.Triple;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PRegFile is a entry that structures a file in the Registry. Other than having the same
 * variables as {@link PbxEntry}, a fileFile is also represented by an array of bytes that contains
 * the data of the file. This array is only filled when the correspondent file in the native
 * folders are areNativeFilesDeleted.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PbxFile extends PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private SnapshotStack snapshotStack;
    private BackupPolicy backupPolicy;


    PbxFile(final PReg parentRegistry,
            final PbxFolder parentFolder,
            final String encodedName,
            final String realName) {

        super(parentRegistry, parentFolder, encodedName, realName);
        this.snapshotStack = new SnapshotStack();
        this.backupPolicy = BackupPolicy.Regular10;
    }


    /**
     * Returns the most recent data for the file represented by this file.
     *
     * @return the most recent data for the file represented by this file, or
     * a empty byte array if this file has no backup data stored
     */
    public Snapshot getLatestSnapshot() {
        Snapshot mostRecentSnapshot = snapshotStack.peek();
        return mostRecentSnapshot != null ? mostRecentSnapshot : Snapshot.empty();
    }


    /**
     * Returns a listing of all snapshots in string representation,
     *
     * @return the most recent data for the file represented by this file, or
     * a empty byte array if this file has no backup data stored
     */
    public List<String> snapshotsToString() {
        return snapshotStack
                .stream()
                .map(Snapshot::toString)
                .collect(Collectors.toList());
    }


    /**
     * Returns the number of currently stored snapshot data for the file
     * represented by this entry.
     *
     * @return the number of stored snapshots
     */
    int getSnapshotCount() {
        return snapshotStack.size();
    }


    /**
     * Sets the data of the file to the specified data.
     */
    public void createSnapshotFromFile(File snapshotFile, FolderOption fromFolder) throws ProtException {
        System.out.println("-------------- " + snapshotFile.getName() + " " + new Date(snapshotFile.lastModified()).toString());
        try {
            byte[] fileData;

            if (fromFolder == FolderOption.SHARED) {
                fileData = parentRegistry.decrypt(FileUtils.readFileToByteArray(snapshotFile));

            } else {
                fileData = FileUtils.readFileToByteArray(snapshotFile);
            }

            long fileSize = snapshotFile.length();
            Date lastModifiedDate = new Date(snapshotFile.lastModified());
            snapshotStack.push(new Snapshot(fileData, fileSize, lastModifiedDate));

        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    /**
     * Sets the data of the file to the specified data.
     */
    public void writeSnapshotToFile(int index, File snapshotFile, FolderOption toFolder) throws ProtException {
        Snapshot snapshot;
        try {
            snapshot = snapshotStack.get(index);

        } catch (IndexOutOfBoundsException ex) {
            return;
        }
        try {
            if (toFolder == FolderOption.SHARED) {
                FileUtils.writeByteArrayToFile(snapshotFile, parentRegistry.encrypt(snapshot.getSnapshotData()));

            } else {
                FileUtils.writeByteArrayToFile(snapshotFile, snapshot.getSnapshotData());
            }

            snapshotFile.setLastModified(snapshot.getSnapshotLastModifiedDate().getTime());
//            snapshotFile.setLastModified(new Date().getTime());

        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    public void setBackupPolicy(BackupPolicy backupPolicy) {
        if (!backupPolicy.equals(BackupPolicy.Ask)) {
            boolean changeBackupPolicy = true;
            if (snapshotStack.size() > backupPolicy.maxBackupSize) {
                changeBackupPolicy = JOptionPane.showConfirmDialog(null,
                        "The number of stored backup copies have been reduced to " + backupPolicy.maxBackupSize +
                                ", and prior backups above that number will be deleted.\n" +
                                "Are you sure you want to change the backup policy?",
                        "Confirm backup policy change",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_NO_OPTION;
            }

            if (changeBackupPolicy) {
                this.backupPolicy = backupPolicy;
                while (snapshotStack.size() > backupPolicy.maxBackupSize) {
                    snapshotStack.removeLast();
                }
            }
        }
    }


    public BackupPolicy getBackupPolicy() {
        return backupPolicy;
    }


    /**
     * Suggests Java Garbage Collector to stop storing this file's data.
     */
    void clearSnapshots() {
//        Snapshot s = getLatestSnapshot();

        snapshotStack.clear();
        snapshotStack = null;
        System.gc();

//        snapshotStack = new SnapshotStack();
//        snapshotStack.push(s);
    }


//    /**
//     * Returns the file size of the file represented by this file.
//     *
//     * @return the file size of the file represented by this file.
//     */
//    public int getFileSize() {
//        return getLatestSnapshot().length;
//    }
//
//
//    /**
//     * Returns the last modified date of the file represented by this file.
//     *
//     * @return the last modified date of the file represented by this file.
//     */
//    public Date getLastModified() {
//        return lastModified;
//    }
//
//
//    /**
//     * Sets the last modified date of the file to the specified last modified date.
//     */
//    public void setLastModified(Date lastModified) {
//        this.lastModified = lastModified;
//    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }


    public enum BackupPolicy {

        None(1), // don't backup this file
        Regular10(10), // backup 10 copies
        Regular50(50), // backup 50 copies
        Regular100(100), // backup 100 copies
        Ask(0); // ask every time

        private int maxBackupSize;

        private BackupPolicy(int maxBackupSize) {
            this.maxBackupSize = maxBackupSize;
        }
    }

    public static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private Triple<byte[], Long, Date> triple;


        /**
         * Constructor to instantiate a snapshot, which will be used for backup
         * and restoring purposes.
         */
        private Snapshot(byte[] data, long dataSize, Date snapshotLastModified) {
            this.triple = Triple.of(data, dataSize, snapshotLastModified);
        }


        /**
         * Default constructor to instantiate a empty snapshot, by storing an empty array,
         * a data size of 0 and a last modified date set to the current time.
         */
        private static Snapshot empty() {
            return new Snapshot(new byte[0], 0, Constants.getToday());
        }


        public byte[] getSnapshotData() {
            return triple.first;
        }


        public long getSnapshotSize() {
            return triple.second;
        }


        public Date getSnapshotLastModifiedDate() {
            return triple.third;
        }


        @Override
        public String toString() {
            return Utils.readableFileSize(getSnapshotSize()) + ", at " +
                    Constants.formatDate(getSnapshotLastModifiedDate());
        }


        @Override
        public int hashCode() {
            return triple.hashCode();
        }


        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return triple.equals(obj);
        }
    }

    private final class SnapshotStack extends LinkedList<Snapshot> {

        private SnapshotStack() {
            super();
        }

        @Override
        public void push(Snapshot newSnapshot) {
            if (backupPolicy.equals(BackupPolicy.Ask)) {
                super.push(newSnapshot);
                if (JOptionPane.showConfirmDialog(null,
                        "A new version of '" + realName() + "' has been detected.\n" +
                                "Would you like to backup the previous version?",
                        "Confirm Backup",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
                    removeLast();
                }

            } else {
                while (size() > backupPolicy.maxBackupSize) {
                    removeLast();
                }
                super.push(newSnapshot);
            }
        }
    }
}
