package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.Folder;
import edduarte.protbox.core.registry.*;
import edduarte.protbox.exception.ProtException;
import edduarte.protbox.ui.TrayApplet;
import edduarte.protbox.utils.Ref;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class Sync {
    private static final Logger logger = LoggerFactory.getLogger(Sync.class);

    private static final Queue<SyncPair> toProt = new PriorityBlockingQueue<>(10, new FileSizeComparator());
    private static final Queue<SyncPair> toShared = new PriorityBlockingQueue<>(10, new FileSizeComparator());
    private static Thread t1, t2; // singleton threads

    private Sync() {
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


    public static void toProt(final PReg reg, final Pair newPair) {
        // check if toShared queue has the same corresponding pair (incoming conflict synchronization)
        if (!findConflict(reg, newPair, toShared))
            toProt.add(new SyncPair(reg, newPair));
    }


    public static void toShared(final PReg directory, final Pair newPair) {
        // check if toProt queue has the same corresponding pair (incoming conflict synchronization)
        if (!findConflict(directory, newPair, toProt))
            toShared.add(new SyncPair(directory, newPair));
    }


    private static boolean findConflict(final PReg reg, final Pair newPair, Queue<SyncPair> queueToCheck) {
        for (SyncPair e : queueToCheck) {
            if (e.pair.equals(newPair)) {
                try {

                    // removes it from that queue
                    queueToCheck.remove(e);
                    File protFile = new File(reg.PROT_PATH + File.separator + newPair.relativeRealPath());

                    reg.addConflicted(protFile, Folder.PROT);
                    Sync.toProt(reg, newPair);

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


    public static Ref.Duo<List<Pair>> removeSyncPairsForReg(final PReg reg) {
        List<Pair> toProtRemoved = new ArrayList<>();
        List<Pair> toSharedRemoved = new ArrayList<>();

        toProt.stream()
                .filter(e -> e.reg.equals(reg))
                .forEach(e -> {
                    toProtRemoved.add(e.pair);
                    toProt.remove(e);
                });

        toShared.stream()
                .filter(e -> e.reg.equals(reg))
                .forEach(e -> {
                    toSharedRemoved.add(e.pair);
                    toShared.remove(e);
                });

        if (Constants.verbose) {
            logger.info("Removed entries of registry " + reg.ID);
        }

        return Ref.of1(toProtRemoved, toSharedRemoved);
    }


    private static void writeAonB(final PReg directory, final PairFile entry, final Folder folderOfA, final File a, final File b) {
        new Thread() {
            @Override
            public void run() {
                try {
                    byte[] data = FileUtils.readFileToByteArray(a);
                    if (folderOfA.equals(Folder.SHARED))
                        data = directory.decrypt(data);
                    else if (folderOfA.equals(Folder.PROT))
                        data = directory.encrypt(data);

                    FileUtils.writeByteArrayToFile(b, data);
                    long newLM = a.lastModified();
                    entry.setLastModified(new Date(newLM));
                    b.setLastModified(newLM);

                } catch (IOException|GeneralSecurityException ex) {
                    if (Constants.verbose) {
                        logger.error("Error while syncing file " + a.getName() + " from " + folderOfA.name().toLowerCase() + " folder.", ex);
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
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING, Integer.toString(toProt.size() + toShared.size()) + " files");
                }

                while (!toProt.isEmpty()) {
                    SyncPair polled = toProt.poll();
                    PReg reg = polled.reg;
                    Pair pairToSync = polled.pair;

                    File sharedFile = new File(reg.SHARED_PATH + File.separator + pairToSync.relativeEncodedPath());
                    File protFile = new File(reg.PROT_PATH + File.separator + pairToSync.relativeRealPath());

                    reg.SKIP_WATCHER_ENTRIES.add(protFile.getAbsolutePath());
                    if (pairToSync instanceof PairFile)
                        writeAonB(reg, ((PairFile) pairToSync), Folder.SHARED, sharedFile, protFile);
                    else if (pairToSync instanceof PairFolder) {
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
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING, Integer.toString(toProt.size() + toShared.size()) + " files");
                }

                while (!toShared.isEmpty()) {
                    SyncPair polled = toShared.poll();
                    PReg reg = polled.reg;
                    Pair pairToSync = polled.pair;

                    File protFile = new File(reg.PROT_PATH + File.separator + pairToSync.relativeRealPath());
                    File sharedFile = new File(reg.SHARED_PATH + File.separator + pairToSync.relativeEncodedPath());

                    reg.SKIP_WATCHER_ENTRIES.add(sharedFile.getAbsolutePath());
                    if (pairToSync instanceof PairFile)
                        writeAonB(reg, ((PairFile) pairToSync), Folder.PROT, protFile, sharedFile);
                    else if (pairToSync instanceof PairFolder) {
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
