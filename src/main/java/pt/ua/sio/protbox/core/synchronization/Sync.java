package pt.ua.sio.protbox.core.synchronization;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import pt.ua.sio.protbox.core.directory.*;
import pt.ua.sio.protbox.exception.ProtException;
import pt.ua.sio.protbox.gui.TrayApplet;
import pt.ua.sio.protbox.util.referencewrappers.DuoReference;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class Sync {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(Sync.class);

    private static final Queue<SyncEntry> toOutput = new PriorityBlockingQueue<>(10, new FileSizeComparator());

    private static final Queue<SyncEntry> toShared = new PriorityBlockingQueue<>(10, new FileSizeComparator());

    private static Thread t1, t2; // singleton threads


    private Sync(){}


    public static void start() {
        if(t1==null) {
            t1 = new Thread(new SharedToOutput());
            t1.start();
        }
        if(t2==null){
            t2 = new Thread(new OutputToShared());
            t2.start();
        }
        logger.info("Started syncing threads");
    }


    public static void stop() {
        if(t1!=null){
            t1.interrupt();
            t1 = null;
        }
        if(t2!=null){
            t2.interrupt();
            t2 = null;
        }
        logger.info("Stopped syncing threads");
    }


    public static void toOutput(final Directory directory, final PbxEntry newPbxEntry) {
        // check if toShared has the same corresponding entry (incoming conflict syncing)
        if(!findConflict(directory, newPbxEntry, toShared))
            toOutput.add(new SyncEntry(directory, newPbxEntry));
    }


    public static void toShared(final Directory directory, final PbxEntry newPbxEntry) {
        // check if toOutput has the same corresponding entry (incoming conflict syncing)
        if(!findConflict(directory, newPbxEntry, toOutput))
            toShared.add(new SyncEntry(directory, newPbxEntry));
    }


    private static boolean findConflict(final Directory directory, final PbxEntry newPbxEntry, Queue<SyncEntry> queueToCheck){
        for(SyncEntry e : queueToCheck){
            if(e.entry.equals(newPbxEntry)){
                try{
                    // removes it from that place
                    queueToCheck.remove(e);
                    File protFile = new File(directory.OUTPUT_PATH + File.separator + newPbxEntry.relativeRealPath());

                    directory.addConflicted(protFile, Source.OUTPUT);
                    Sync.toOutput(directory, newPbxEntry);

                    return true;
                }catch (ProtException ex){
                    logger.error(ex.toString());
                }
            }
        }

        return false;
    }


    public static DuoReference<List<PbxEntry>> removeEntriesOfDirectory(final Directory directory) {
        List<PbxEntry> toOutputRemoved = new ArrayList<>();
        List<PbxEntry> toSharedRemoved = new ArrayList<>();

        for (SyncEntry e : toOutput) {
            if (e.directory.equals(directory)) {
                toOutputRemoved.add(e.entry);
                toOutput.remove(e);
            }
        }
        for (SyncEntry e : toShared) {
            if (e.directory.equals(directory)) {
                toSharedRemoved.add(e.entry);
                toShared.remove(e);
            }
        }

        logger.info("Removed entries of directory " + directory.NAME);
        return new DuoReference<>(toOutputRemoved, toSharedRemoved);
    }


    // Thread that deals with shared to output PbxEntry movements
    private static class SharedToOutput implements Runnable {
        @Override
        public void run() {
            boolean statusOK = true;
            while(!Thread.currentThread().isInterrupted()){

                while (!toOutput.isEmpty()){
                    try{
                        statusOK = false;
                        TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING, Integer.toString(toOutput.size()+ toShared.size())+" files");
                        SyncEntry polled = toOutput.poll();
                        Directory directory = polled.directory;
                        PbxEntry toCreate = polled.entry;

                        File sharedFile = new File(directory.SHARED_PATH + File.separator + toCreate.relativeEncodedPath());
                        File outputFile = new File(directory.OUTPUT_PATH + File.separator + toCreate.relativeRealPath());

                        directory.SKIP_WATCHER_ENTRIES.add(outputFile.getAbsolutePath());
                        if(toCreate instanceof PbxFile)
                            writeAonB(directory, ((PbxFile)toCreate), Source.SHARED, sharedFile, outputFile);
                        else if(toCreate instanceof PbxFolder){
                            outputFile.mkdir();
                        }

                    }catch (Exception ex) {
                        logger.info(ex.toString());
                        ex.printStackTrace();
                    }
                }

                if(!statusOK){
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
                    statusOK = true;
                }

                try{
                    Thread.sleep(100);
                }catch (InterruptedException ex){}
            }
        }
    }

    // Thread that deals with output to shared PbxEntry movements
    private static class OutputToShared implements Runnable {
        @Override
        public void run() {
            boolean statusOK = true;
            while(!Thread.currentThread().isInterrupted()){

                while (!toShared.isEmpty()){
                    try{
                        statusOK = false;
                        TrayApplet.getInstance().status(TrayApplet.TrayStatus.UPDATING, Integer.toString(toOutput.size()+toShared.size())+" files");
                        SyncEntry polled = toShared.poll();
                        Directory directory = polled.directory;
                        PbxEntry toCreate = polled.entry;

                        File outputFile = new File(directory.OUTPUT_PATH + File.separator + toCreate.relativeRealPath());
                        File sharedFile = new File(directory.SHARED_PATH + File.separator + toCreate.relativeEncodedPath());

                        directory.SKIP_WATCHER_ENTRIES.add(sharedFile.getAbsolutePath());
                        if(toCreate instanceof PbxFile)
                            writeAonB(directory, ((PbxFile)toCreate), Source.OUTPUT, outputFile, sharedFile);
                        else if(toCreate instanceof PbxFolder){
                            sharedFile.mkdir();
                            try{
                                new File(sharedFile, "»==").createNewFile();
                            }catch (IOException ex){}
                        }

                    }catch (Exception ex) {
                        logger.info(ex.toString());
                        ex.printStackTrace();
                    }
                }

                if(!statusOK){
                    TrayApplet.getInstance().status(TrayApplet.TrayStatus.OKAY, "");
                    statusOK = true;
                }


                try{
                    Thread.sleep(100);
                }catch (InterruptedException ex){}
            }
        }
    }

    private static void writeAonB(final Directory directory, final PbxFile entry, final Source folderOfA, final File A, final File B){
        new Thread() {
            @Override
            public void run() {
                try{
                    byte[] data = FileUtils.readFileToByteArray(A);
                    if(folderOfA.equals(Source.SHARED))
                        data = directory.decrypt(data);
                    else if(folderOfA.equals(Source.OUTPUT))
                        data = directory.encrypt(data);

                    FileUtils.writeByteArrayToFile(B, data);
                    long newLM = A.lastModified();
                    entry.setLastModified(newLM);
                    B.setLastModified(newLM);
                }catch (Exception ex){}
            }
        }.start();
    }
}
