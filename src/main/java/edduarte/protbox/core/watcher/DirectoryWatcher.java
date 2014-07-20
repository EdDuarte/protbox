package edduarte.protbox.core.watcher;

import org.slf4j.LoggerFactory;
import edduarte.protbox.core.Constants;
import edduarte.protbox.core.directory.Registry;
import edduarte.protbox.core.directory.Source;
import edduarte.protbox.exception.ProtException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public final class DirectoryWatcher implements Runnable {
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
    private Registry directory;
    private Source fromFolder;
    private Path root;
    private WatchService watchService = FileSystems.getDefault().newWatchService();


    public DirectoryWatcher(Registry directory, Source fromFolder, Path root) throws IOException {
        this.directory = directory;
        this.fromFolder = fromFolder;
        this.root = root;
        watchFolder(root);
    }

    private void watchFolder(Path root) throws IOException {
        watch(root);
        Files.walkFileTree(root, new SimpleFileVisitor<Path>(){

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
        } catch (ProtException|IOException ex) {
            logger.error(ex.toString());
            run();
        }
    }

    private void run_aux() throws ProtException, IOException{
        try {
            if(Constants.verbose) logger.info("WatchService for "+root.toString()+" start.");

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    for(WatchEvent event : key.pollEvents()) {
                        Path relativePath = (Path)event.context();
                        Path path = ((Path)key.watchable()).resolve(relativePath);
                        String absolutePath = path.toFile().getAbsolutePath();

                        // if file exists at directories SKIP_ENTRIES list, ignore the file,
                        // since it was already added or altered by the application itself
                        if((relativePath.getFileName().toString().contains("»") && fromFolder.equals(Source.SHARED))
                                || relativePath.toFile().getName().equalsIgnoreCase("»==")){
                            // IGNORE THESE FILES AT ALL COST !!!
                            logger.info("detected "+relativePath+" and not doing anything about it");
                        } else {
                            WatchEvent.Kind kind = event.kind();

                            if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)){
                                if(directory.SKIP_WATCHER_ENTRIES.contains(absolutePath)){
                                    directory.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
                                } else {
                                    if(Constants.verbose) logger.info("DirectoryWatch["+directory.NAME +"|"+fromFolder.name()+"]: ADDED "+absolutePath);

                                    if (path.toFile().isDirectory()) {
                                        watchFolder(path);
                                    }
                                    directory.add(path.toFile(), fromFolder);
                                }
                            }

                            else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)){
                                if(directory.SKIP_WATCHER_ENTRIES.contains(absolutePath)){
                                    directory.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
                                } else {
                                    if(Constants.verbose) logger.info("DirectoryWatch["+directory.NAME +"|"+fromFolder.name()+"]: DELETED "+absolutePath);
                                    directory.delete(path, fromFolder);
                                }
                            }
                        }
                    }
                    key.reset();
                } catch (FileNotFoundException|FileSystemException ex) {
                    // try again
                } catch (ClosedWatchServiceException ex){
                    break;
                }
            }
        } catch(InterruptedException ex){
            if(Constants.verbose) logger.info("WatchService for "+root.toString()+" was interrupted.");
        } finally {
            watchService.close();
        }
    }
}
