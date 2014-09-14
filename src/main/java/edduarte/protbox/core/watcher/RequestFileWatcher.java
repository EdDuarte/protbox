package edduarte.protbox.core.watcher;

import edduarte.protbox.exception.ProtException;
import edduarte.protbox.utils.Callback;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class RequestFileWatcher implements Runnable {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(RequestFileWatcher.class);

    private Callback<File> callback;


    private WatchService watchService = FileSystems.getDefault().newWatchService();

    public RequestFileWatcher(Path root, Callback<File> callback) throws IOException {
        this.callback = callback;
        watch(root);
    }

    private void watch(Path path) throws IOException {
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    @Override
    public void run() {
        try {
            run_aux();
        } catch (ProtException | IOException ex) {
            logger.error(ex.toString());
        }
    }

    private void run_aux() throws ProtException, IOException {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent event : key.pollEvents()) {
                    Path relativePath = (Path) event.context();
                    File file = ((Path) key.watchable()).resolve(relativePath).toFile();

                    WatchEvent.Kind kind = event.kind();
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        System.out.println("detected" + file.toString());
                        callback.onResult(file);
                    }
                }
                key.reset();
            }
        } catch (InterruptedException ex) {

        } finally {
            watchService.close();
        }
    }
}
