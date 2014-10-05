package edduarte.protbox.core.registry;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderOption;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.utils.Utils;
import edduarte.protbox.utils.dataholders.Triple;
import org.apache.commons.io.FileUtils;

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
 * folders are hidden.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PbxFile extends PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private SnapshotStack dataStack;

    PbxFile(final PReg parentRegistry,
            final PbxFolder parentFolder,
            final String encodedName,
            final String realName) {

        super(parentRegistry, parentFolder, encodedName, realName);
        this.dataStack = new SnapshotStack();
    }


    /**
     * Returns the most recent data for the file represented by this file.
     *
     * @return the most recent data for the file represented by this file, or
     * a empty byte array if this file has no backup data stored
     */
    public Snapshot getLatestSnapshot() {
        Snapshot mostRecentSnapshot = dataStack.peek();
        return mostRecentSnapshot != null ? mostRecentSnapshot : Snapshot.empty();
    }


    /**
     * Returns a listing of all snapshots in string representation,
     *
     * @return the most recent data for the file represented by this file, or
     * a empty byte array if this file has no backup data stored
     */
    List<String> snapshotsToString() {
        return dataStack
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
        return dataStack.size();
    }


    /**
     * Sets the data of the file to the specified data.
     */
    public void createSnapshotFromFile(File snapshotFile, FolderOption fromFolder) throws ProtException {
        try {
            byte[] fileData;

            if (fromFolder == FolderOption.SHARED) {
                fileData = parentRegistry.decrypt(FileUtils.readFileToByteArray(snapshotFile));

            } else {
                fileData = FileUtils.readFileToByteArray(snapshotFile);
            }

            long fileSize = snapshotFile.length();
            Date lastModifiedDate = new Date(snapshotFile.lastModified());
            dataStack.push(new Snapshot(fileData, fileSize, lastModifiedDate));

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
            snapshot = dataStack.get(index);

        } catch (IndexOutOfBoundsException ex) {
            return;
        }
        try {
            snapshotFile.setLastModified(snapshot.getSnapshotLastModifiedDate().getTime());

            if (toFolder == FolderOption.SHARED) {
                FileUtils.writeByteArrayToFile(snapshotFile, parentRegistry.encrypt(snapshot.getSnapshotData()));

            } else {
                FileUtils.writeByteArrayToFile(snapshotFile, snapshot.getSnapshotData());
            }

        } catch (IOException | GeneralSecurityException ex) {
            throw new ProtException(ex);
        }
    }


    /**
     * Suggests Java Garbage Collector to stop storing this file's data.
     */
    void clearData() {
//        if (dataStack != null) {
//            Arrays.fill(dataStack, (byte) 0);
//            dataStack = null;
//            System.gc();
//        }
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

    public static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;

        private Triple<byte[], Long, Date> dataHolder;

        /**
         * Constructor to instantiate a snapshot, which will be used for backup
         * and restoring purposes.
         */
        private Snapshot(byte[] data, long dataSize, Date snapshotLastModified) {
            this.dataHolder = new Triple<>(data, dataSize, snapshotLastModified);
        }


        /**
         * Default constructor to instantiate a empty snapshot, by storing an empty array,
         * a data size of 0 and a last modified date set to the current time.
         */
        private static Snapshot empty() {
            return new Snapshot(new byte[0], 0, Constants.getToday());
        }

        public byte[] getSnapshotData() {
            return dataHolder.first;
        }


        public long getSnapshotSize() {
            return dataHolder.second;
        }


        public Date getSnapshotLastModifiedDate() {
            return dataHolder.third;
        }

        @Override
        public String toString() {
            return Constants.formatDate(getSnapshotLastModifiedDate()) + ", at " +
                    Utils.readableFileSize(getSnapshotSize());
        }
    }

    private final class SnapshotStack extends LinkedList<Snapshot> {

        private SnapshotStack() {
            super();
        }

        @Override
        public void push(Snapshot newSnapshot) {
            while (size() > parentRegistry.getMaxDeletedSize()) {
                removeLast();
            }
            super.push(newSnapshot);
        }
    }
}
