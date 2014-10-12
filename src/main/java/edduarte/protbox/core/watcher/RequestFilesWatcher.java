package edduarte.protbox.core.watcher;

import edduarte.protbox.exception.ProtException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class RequestFilesWatcher implements Runnable {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(RequestFilesWatcher.class);

    private Consumer<File> consumer;

    private WatchService watchService = FileSystems.getDefault().newWatchService();

    public RequestFilesWatcher(Path root, Consumer<File> Consumer) throws IOException {
        this.consumer = Consumer;
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
                        consumer.accept(file);
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
