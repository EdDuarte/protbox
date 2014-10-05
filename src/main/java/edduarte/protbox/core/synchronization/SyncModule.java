package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderOption;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.PbxEntry;
import edduarte.protbox.core.registry.PbxFile;
import edduarte.protbox.core.registry.PbxFolder;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.utils.dataholders.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class SyncModule {
    private static final Logger logger = LoggerFactory.getLogger(SyncModule.class);

    private static final Queue<SyncEntry> toProt = new PriorityBlockingQueue<>(10, new FileSizeComparator());
    private static final Queue<SyncEntry> toShared = new PriorityBlockingQueue<>(10, new FileSizeComparator());
    private static Thread t1, t2; // singleton threads

    private SyncModule() {
    }

    public static void start() {
        if (t1 == null) {
            t1 = new Thread(new SharedToProt());
            t1.start();
        }
        if (t2 == null) {
            t2 = new Thread(new ProtToShared());
            t2.start();
        }

        if (Constants.verbose) {
            logger.info("Started syncing threads");
        }
    }


    public static void stop() {
        if (t1 != null) {
            t1.interrupt();
            t1 = null;
        }
        if (t2 != null) {
            t2.interrupt();
            t2 = null;
        }

        if (Constants.verbose) {
            logger.info("Stopped syncing threads");
        }
    }


    public static void toProt(final PReg reg, final PbxEntry newEntry) {
        // check if toShared queue has the same corresponding pair (incoming conflict synchronization)
        if (!findConflict(reg, newEntry, toShared))
            toProt.add(new SyncEntry(reg, newEntry));
    }


    public static void toShared(final PReg directory, final PbxEntry newEntry) {
        // check if toProt queue has the same corresponding pair (incoming conflict synchronization)
        if (!findConflict(directory, newEntry, toProt))
            toShared.add(new SyncEntry(directory, newEntry));
    }


    private static boolean findConflict(final PReg reg,
                                        final PbxEntry newEntry,
                                        final Queue<SyncEntry> queueToCheck) {

        for (SyncEntry e : queueToCheck) {
            if (e.entry.equals(newEntry)) {
                try {

                    // removes it from that queue
                    queueToCheck.remove(e);
                    File protFile = new File(reg.getPair().getProtFolderPath() +
                            File.separator + newEntry.relativeRealPath());

                    reg.addConflicted(protFile, FolderOption.PROT);
                    SyncModule.toProt(reg, newEntry);

                    return true;

                } catch (ProtException ex) {
                    if (Constants.verbose) {
                        logger.error("Error while finding conflicted synchronization requests.", ex);
                    }
                }
            }
        }

        return false;
    }


    public static Pair<List<PbxEntry>> removeSyncPairsForReg(final PReg reg) {
        List<PbxEntry> toProtRemoved = new ArrayList<>();
        List<PbxEntry> toSharedRemoved = new ArrayList<>();

        toProt.stream()
                .filter(e -> e.reg.equals(reg))
                .forEach(e -> {
                    toProtRemoved.add(e.entry);
                    toProt.remove(e);
                });

        toShared.stream()
                .filter(e -> e.reg.equals(reg))
                .forEach(e -> {
                    toSharedRemoved.add(e.entry);
                    toShared.remove(e);
                });

        if (Constants.verbose) {
            logger.info("Removed entries of registry " + reg.id);
        }

        return new Pair<>(toProtRemoved, toSharedRemoved);
    }


    private static void writeAonB(final PbxFile entry, final FolderOption folderOfA, final File a, final File b) {
        new Thread() {
            @Override
            public void run() {
                try {
//                    byte[] data = FileUtils.readFileToByteArray(a);
//                    if (folderOfA.equals(FolderOption.SHARED))
//                        data = directory.decrypt(data);
//                    else if (folderOfA.equals(FolderOption.PROT))
//                        data = directory.encrypt(data);
//
//                    FileUtils.writeByteArrayToFile(b, data);
//                    long newLM = a.lastModified();
//                    entry.setLastModified(new Date(newLM));
//                    b.setLastModified(newLM);

                    entry.createSnapshotFromFile(a, folderOfA);
                    entry.writeSnapshotToFile(0, b, folderOfA.inverse());

                } catch (ProtException ex) {
                    if (Constants.verbose) {
                        logger.error("Error while syncing file " + a.getName() +
                                " from " + folderOfA.name().toLowerCase() + " folder.", ex);
                    }
                }
            }
        }.start();
    }


    // Thread that deals with shared to prot pair movements
    private static class SharedToProt implements Runnable {
        @Override
        public void run() {
            boolean statusOK = true;
            while (!Thread.currentThread().isInterrupted()) {

                if (!toProt.isEmpty()) {
                    statusOK = false;
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING,
                            Integer.toString(toProt.size() + toShared.size()) + " files");
                }

                while (!toProt.isEmpty()) {
                    SyncEntry polled = toProt.poll();
                    PReg reg = polled.reg;
                    PbxEntry entryToSync = polled.entry;

                    File sharedFile = new File(reg.getPair().getSharedFolderPath() +
                            File.separator + entryToSync.relativeEncodedPath());
                    File protFile = new File(reg.getPair().getProtFolderPath() +
                            File.separator + entryToSync.relativeRealPath());

                    reg.SKIP_WATCHER_ENTRIES.add(protFile.getAbsolutePath());
                    if (entryToSync instanceof PbxFile)
                        writeAonB(((PbxFile) entryToSync), FolderOption.SHARED, sharedFile, protFile);
                    else if (entryToSync instanceof PbxFolder) {
                        protFile.mkdir();
                    }
                }

                if (!statusOK) {
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
                    statusOK = true;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    // Thread that deals with prot to shared pair movements
    private static class ProtToShared implements Runnable {
        @Override
        public void run() {
            boolean statusOK = true;
            while (!Thread.currentThread().isInterrupted()) {

                if (!toProt.isEmpty()) {
                    statusOK = false;
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING,
                            Integer.toString(toProt.size() + toShared.size()) + " files");
                }

                while (!toShared.isEmpty()) {
                    SyncEntry polled = toShared.poll();
                    PReg reg = polled.reg;
                    PbxEntry entryToSync = polled.entry;

                    File protFile = new File(reg.getPair().getProtFolderPath() +
                            File.separator + entryToSync.relativeRealPath());
                    File sharedFile = new File(reg.getPair().getSharedFolderPath() +
                            File.separator + entryToSync.relativeEncodedPath());

                    reg.SKIP_WATCHER_ENTRIES.add(sharedFile.getAbsolutePath());
                    if (entryToSync instanceof PbxFile)
                        writeAonB(((PbxFile) entryToSync), FolderOption.PROT, protFile, sharedFile);
                    else if (entryToSync instanceof PbxFolder) {
                        sharedFile.mkdir();
                    }

                }

                if (!statusOK) {
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
                    statusOK = true;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
            }
        }
    }
}
