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
import edduarte.protbox.core.PbxUser;
import edduarte.protbox.core.keyexchange.Request;
import edduarte.protbox.core.keyexchange.Response;
import edduarte.protbox.exception.ProtboxException;
import edduarte.protbox.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
public class RequestWatcher extends BaseWatcher {
    private final static Logger logger = LoggerFactory.getLogger(RequestWatcher.class);

    private final Consumer<Result> requestConsumer;

    private final Set<String> alreadyReceivedRequestHashes;


    public RequestWatcher(Path root, Consumer<Result> onRequestConsumer) throws IOException {
        super(root);
        this.alreadyReceivedRequestHashes = new HashSet<>();
        this.requestConsumer = onRequestConsumer;
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
        if (lastIndexOfSpecialChar <= 0) {
            // its a request file with a valid name (_<requester_hash>)

            // set a timer to delete this file after 2 minutes
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Constants.delete(createdFile);
                }
            }, 120000);

            String requestHash = createdFile.getName().substring(1);

            if (alreadyReceivedRequestHashes.contains(requestHash)) {
                return;
            }
            alreadyReceivedRequestHashes.add(requestHash);


            // wait 1 second to avoid incomplete readings or file locks
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(createdFile))) {
                final Request request = (Request) in.readObject();
                requestConsumer.accept(new Result(requestHash, createdFile.getParentFile(), request));

            } catch (IOException | ReflectiveOperationException ex) {
                logger.error("Error while reading request file.", ex);
            }
        }
    }


    @Override
    protected void onFileDeleted(File deletedFile) throws ProtboxException, IOException {
    }


    public static class Result {
        private final String requestHash;

        private final File parentFile;

        private final Request detectedRequest;


        private Result(String requestHash, File parentFile, Request detectedRequest) {
            this.requestHash = requestHash;
            this.parentFile = parentFile;
            this.detectedRequest = detectedRequest;
        }


        public void createResponseFile(Response response) throws IOException {
            String responseFileName = Constants.SPECIAL_FILE_FIRST_CHAR +
                    requestHash +
                    Constants.SPECIAL_FILE_FIRST_CHAR +
                    Utils.generateRandomHash();

            File responseFile = new File(parentFile, responseFileName);
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(responseFile))) {
                out.writeObject(response);
            }
        }


        public PbxUser getRequestingUser() {
            return detectedRequest.requestingUser;
        }


        public byte[] getEncodedUserPublicKey() {
            return detectedRequest.encodedUserPublicKey;
        }


        public byte[] getSignatureByteArray() {
            return detectedRequest.signatureByteArray;
        }
    }
}
