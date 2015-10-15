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

package com.edduarte.protbox.core.watcher;

import com.edduarte.protbox.core.Constants;
import com.edduarte.protbox.exception.ProtboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
abstract class BaseWatcher extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(DefaultWatcher.class);

    private final WatchService watchService = FileSystems.getDefault().newWatchService();

    private final Path root;


    public BaseWatcher(Path root) throws IOException {
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
        } catch (ProtboxException | IOException ex) {
            logger.error("Error while watching directory " + root.toString() + ".", ex);
            run();
        }
    }


    private void run_aux() throws ProtboxException, IOException {
        try {
            if (Constants.verbose) {
                logger.info("WatchService for " + root.toString() + " started.");
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent event : key.pollEvents()) {
                        Path relativePath = (Path) event.context();
                        Path path = ((Path) key.watchable()).resolve(relativePath);

                        WatchEvent.Kind kind = event.kind();

                        if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                            if (path.toFile().isDirectory()) {
                                watchFolder(path);
                            }
                            onFileCreated(path.toFile());
                        } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                            onFileDeleted(path.toFile());
                        }
                    }
                    key.reset();
                } catch (ClosedWatchServiceException ex) {
                    break;
                }
            }
        } catch (InterruptedException ex) {
            if (Constants.verbose) {
                logger.info("WatchService for " + root.toString() + " stopped.");
            }
        } finally {
            watchService.close();
        }
    }


    protected abstract void onFileCreated(File createdFile) throws ProtboxException, IOException;


    protected abstract void onFileDeleted(File deletedFile) throws ProtboxException, IOException;


    @Override
    public void interrupt() {
        try {
            watchService.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        super.interrupt();
    }

}
