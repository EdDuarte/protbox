package edduarte.protbox.core.watcher;

import org.slf4j.LoggerFactory;
import edduarte.protbox.exception.ProtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class SpecificWatcher implements Runnable {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(SpecificWatcher.class);

    private Path root;
    private Process processToRun;


    private WatchService watchService = FileSystems.getDefault().newWatchService();

    public SpecificWatcher(Path root, Process processToRun) throws IOException {
        this.root = root;
        this.processToRun = processToRun;
        watch(root);
    }

    private void watch(Path path) throws IOException {
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    @Override
    public void run() {
        try {
            run_aux();
        } catch (ProtException |IOException ex) {
            logger.error(ex.toString());
        }
    }

    private void run_aux() throws ProtException, IOException{
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for(WatchEvent event : key.pollEvents()) {
                    Path relativePath = (Path)event.context();
                    File file = ((Path)key.watchable()).resolve(relativePath).toFile();

                    WatchEvent.Kind kind = event.kind();
                    if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)){
                        System.out.println("detected"+file.toString());
                        processToRun.run(file);
                    }
                }
                key.reset();
            }
        } catch(InterruptedException ex){

        } finally {
            watchService.close();
        }
    }

    public static abstract class Process {
        public abstract void run(File detectedFile) throws IOException, InterruptedException;
    }
}
