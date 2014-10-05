package edduarte.protbox.core.watcher;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.FolderOption;
import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.exception.ProtException;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class RegistryWatcher implements Runnable {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(RegistryWatcher.class);
    private PReg reg;
    private FolderOption fromFolder;
    private Path root;
    private WatchService watchService = FileSystems.getDefault().newWatchService();


    public RegistryWatcher(PReg reg, FolderOption fromFolder, Path root) throws IOException {
        this.reg = reg;
        this.fromFolder = fromFolder;
        this.root = root;
        watchFolder(root);
    }

    private void watchFolder(Path root) throws IOException {
        watch(root);
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                watch(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void watch(Path path) throws IOException {
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
    }

    @Override
    public void run() {
        try {
            run_aux();
        } catch (ProtException | IOException ex) {
            logger.error("Error while watching the PReg " + reg.id + ".", ex);
            run();
        }
    }

    private void run_aux() throws ProtException, IOException {
        try {
            if (Constants.verbose) {
                logger.info("WatchService for " + root.toString() + " start.");
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent event : key.pollEvents()) {
                        Path relativePath = (Path) event.context();
                        Path path = ((Path) key.watchable()).resolve(relativePath);
                        String absolutePath = path.toFile().getAbsolutePath();

                        // if file exists at directories SKIP_ENTRIES list, ignore the file,
                        // since it was already added or altered by the application itself
                        if ((relativePath.getFileName().toString().contains("»") && fromFolder.equals(FolderOption.SHARED))
                                || relativePath.toFile().getName().equalsIgnoreCase("»==")) {
                            // IGNORE THESE FILES AT ALL COST !!!
                            logger.info("detected " + relativePath + " and not doing anything about it");
                        } else {
                            WatchEvent.Kind kind = event.kind();

                            if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                                if (reg.SKIP_WATCHER_ENTRIES.contains(absolutePath)) {
                                    reg.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
                                } else {
                                    if (Constants.verbose)
                                        logger.info("DirectoryWatch[" + reg.id + "|" + fromFolder.name() + "]: ADDED " + absolutePath);

                                    if (path.toFile().isDirectory()) {
                                        watchFolder(path);
                                    }
                                    reg.add(path.toFile(), fromFolder);
                                }
                            } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                                if (reg.SKIP_WATCHER_ENTRIES.contains(absolutePath)) {
                                    reg.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
                                } else {
                                    if (Constants.verbose)
                                        logger.info("DirectoryWatch[" + reg.id + "|" + fromFolder.name() + "]: DELETED " + absolutePath);
                                    reg.delete(path, fromFolder);
                                }
                            }
                        }
                    }
                    key.reset();
                } catch (FileNotFoundException | FileSystemException ex) {
                    // try again
                } catch (ClosedWatchServiceException ex) {
                    break;
                }
            }
        } catch (InterruptedException ex) {
            if (Constants.verbose) logger.info("WatchService for " + root.toString() + " was interrupted.");
        } finally {
            watchService.close();
        }
    }
}
