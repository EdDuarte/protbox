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
import com.edduarte.protbox.exception.ProtboxException;
import com.edduarte.protbox.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PbxFile is a entry that structures a file in the Registry. Other than having the same
 * variables as {@link PbxEntry}, a PbxFile is also represented by a set of snapshots, each one
 * representing a past state of the file's data, size and last modified date.
 *
 * @author Ed Duarte (<a href="mailto:ed@edduarte.com">ed@edduarte.com</a>)
 * @version 2.0
 */
public final class PbxFile extends PbxEntry implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(PbxFile.class);

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
    public void createSnapshotFromFile(File snapshotFile, FolderOption fromFolder) throws ProtboxException {
        logger.info("-------------- " + snapshotFile.getName() + " " + new Date(snapshotFile.lastModified()).toString());
        try {
            byte[] fileData;

            if (fromFolder.equals(FolderOption.SHARED)) {
                try {
                    fileData = parentRegistry.decrypt(FileUtils.readFileToByteArray(snapshotFile), true);
                } catch (ProtboxException ex) {
                    logger.info("There was a problem while decrypting. Ignoring file " + snapshotFile.getName() + ".", ex);
                    return;
                }

            } else {
                fileData = FileUtils.readFileToByteArray(snapshotFile);
            }

            long fileSize = snapshotFile.length();
            Date lastModifiedDate = new Date(snapshotFile.lastModified());
            snapshotStack.push(new Snapshot(fileData, fileSize, lastModifiedDate));

        } catch (IOException ex) {
            throw new ProtboxException(ex);
        }
    }


    /**
     * Sets the data of the file to the specified data.
     */
    public void writeSnapshotToFile(int index, File snapshotFile, FolderOption toFolder) throws ProtboxException {
        Snapshot snapshot;
        try {
            snapshot = snapshotStack.remove(index);
            snapshotStack.forcePush(snapshot);

        } catch (IndexOutOfBoundsException ex) {
            return;
        }
        try {
            if (toFolder.equals(FolderOption.SHARED)) {
                FileUtils.writeByteArrayToFile(snapshotFile, parentRegistry.encrypt(snapshot.getSnapshotData(), true));

            } else {
                FileUtils.writeByteArrayToFile(snapshotFile, snapshot.getSnapshotData());
            }

            snapshotFile.setLastModified(snapshot.getLastModifiedDate().getTime());

        } catch (IOException ex) {
            throw new ProtboxException(ex);
        }
    }


    public BackupPolicy getBackupPolicy() {
        return backupPolicy;
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
        } else {
            this.backupPolicy = backupPolicy;
        }
    }


    /**
     * Suggests Java Garbage Collector to stop storing this file's data.
     */
    void clearSnapshots() {
        snapshotStack.clear();
        snapshotStack = null;
        System.gc();
    }


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

        private final byte[] data;

        private final long dataSize;

        private final Date lastModified;


        /**
         * Constructor to instantiate a snapshot, which will be used for backup
         * and restoring purposes.
         */
        private Snapshot(byte[] data, long dataSize, Date lastModified) {
            this.data = data;
            this.dataSize = dataSize;
            this.lastModified = lastModified;
        }


        /**
         * Default constructor to instantiate a empty snapshot, by storing an empty array,
         * a data size of 0 and a last modified date set to the current time.
         */
        private static Snapshot empty() {
            return new Snapshot(new byte[0], 0, Constants.getToday());
        }


        public byte[] getSnapshotData() {
            return data;
        }


        public long getSnapshotSize() {
            return dataSize;
        }


        public Date getLastModifiedDate() {
            return lastModified;
        }


        @Override
        public String toString() {
            return Utils.readableFileSize(getSnapshotSize()) + ", at " +
                    Constants.formatDate(getLastModifiedDate());
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Snapshot snapshot = (Snapshot) o;
            return dataSize == snapshot.dataSize &&
                    Arrays.equals(data, snapshot.data) &&
                    lastModified.equals(snapshot.lastModified);
        }


        @Override
        public int hashCode() {
            int result = Arrays.hashCode(data);
            result = 31 * result + (int) (dataSize ^ (dataSize >>> 32));
            result = 31 * result + lastModified.hashCode();
            return result;
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
                forcePush(newSnapshot);
            }
        }


        public void forcePush(Snapshot newSnapshot) {
            super.push(newSnapshot);
        }
    }
}
