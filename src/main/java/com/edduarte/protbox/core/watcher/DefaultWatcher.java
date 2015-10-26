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
import com.edduarte.protbox.core.FolderOption;
import com.edduarte.protbox.core.registry.PReg;
import com.edduarte.protbox.exception.ProtboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * @author Ed Duarte (<a href="mailto:ed@edduarte.com">ed@edduarte.com</a>)
 * @version 2.0
 */
public final class DefaultWatcher extends BaseWatcher {
    private final static Logger logger = LoggerFactory.getLogger(DefaultWatcher.class);

    private PReg reg;

    private FolderOption fromFolder;


    public DefaultWatcher(PReg reg, FolderOption fromFolder, Path root) throws IOException {
        super(root);
        this.reg = reg;
        this.fromFolder = fromFolder;
    }


    @Override
    protected void onFileCreated(File createdFile) throws ProtboxException, IOException {
        if (createdFile.getName().charAt(0) == Constants.SPECIAL_FILE_FIRST_CHAR) {
            return;
        }

        String absolutePath = createdFile.getAbsolutePath();

        if (reg.SKIP_WATCHER_ENTRIES.contains(absolutePath)) {
            reg.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
        } else {
            if (Constants.verbose) {
                logger.info("DirectoryWatch[" + reg.id + "|" + fromFolder.name() + "]: ADDED " + absolutePath);
            }

            reg.add(createdFile, fromFolder);
        }
    }


    @Override
    protected void onFileDeleted(File deletedFile) throws ProtboxException, IOException {
        if (deletedFile.getName().charAt(0) == Constants.SPECIAL_FILE_FIRST_CHAR) {
            return;
        }

        String absolutePath = deletedFile.getAbsolutePath();

        if (reg.SKIP_WATCHER_ENTRIES.contains(absolutePath)) {
            reg.SKIP_WATCHER_ENTRIES.removeAll(Collections.singleton(absolutePath));
        } else {
            if (Constants.verbose) {
                logger.info("DirectoryWatch[" + reg.id + "|" + fromFolder.name() + "]: DELETED " + absolutePath);
            }

            reg.delete(deletedFile, fromFolder);
        }
    }
}
