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

package com.edduarte.protbox.core.keyexchange;

import java.io.Serializable;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String directoryAlgorithm;

    public final byte[] encryptedPairKey;
//    public final byte[] encryptedIntegrityKey;


    public Response(String directoryAlgorithm, byte[] encryptedPairKey/*, byte[] encryptedIntegrityKey*/) {
        this.directoryAlgorithm = directoryAlgorithm;
        this.encryptedPairKey = encryptedPairKey;
//        this.encryptedIntegrityKey = encryptedIntegrityKey;
    }
}
