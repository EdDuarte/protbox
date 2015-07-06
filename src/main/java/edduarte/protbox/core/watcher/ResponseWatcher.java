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

package edduarte.protbox.core.watcher;

import edduarte.protbox.core.Constants;
import edduarte.protbox.core.keyexchange.Response;
import edduarte.protbox.exception.ProtboxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class ResponseWatcher extends BaseWatcher {
    private final static Logger logger = LoggerFactory.getLogger(ResponseWatcher.class);

    private final String requestHash;

    private final Consumer<Response> responseConsumer;

    private final Set<String> alreadyReceivedResponseHashes;


    public ResponseWatcher(Path root, String requestHash, Consumer<Response> onResponseConsumer) throws IOException {
        super(root);
        this.requestHash = requestHash;
        this.alreadyReceivedResponseHashes = new HashSet<>();
        this.responseConsumer = onResponseConsumer;
    }


    @Override
    protected void onFileCreated(File createdFile) throws ProtboxException, IOException {
        if (createdFile.getName().charAt(0) != Constants.SPECIAL_FILE_FIRST_CHAR) {
            return;
        }

        if (createdFile.getName().length() == 1) {
            Constants.delete(createdFile);
        }

        int lastIndexOfSpecialChar = createdFile.getName().lastIndexOf(Constants.SPECIAL_FILE_FIRST_CHAR);
        if (lastIndexOfSpecialChar > 0) {

            // its a response file with a valid name (_<requester_hash>_<replier_hash>)
            String hash1 = createdFile.getName().substring(1, lastIndexOfSpecialChar);
            if (!requestHash.equals(hash1)) {
                // its a response to another user
                return;
            }

            // set a timer to delete this file after 2 minutes
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Constants.delete(createdFile);
                }
            }, 120000);

            String hash2 = createdFile.getName().substring(lastIndexOfSpecialChar + 1);
            if (alreadyReceivedResponseHashes.contains(hash2)) {
                return;
            }
            alreadyReceivedResponseHashes.add(hash2);


            // wait 1 second to avoid incomplete readings or file locks
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(createdFile))) {
                final Response response = (Response) in.readObject();
                responseConsumer.accept(response);

            } catch (IOException | ReflectiveOperationException ex) {
                logger.error("Error while reading response file.", ex);
            }
        }
    }


    @Override
    protected void onFileDeleted(File deletedFile) throws ProtboxException, IOException {
    }
}
