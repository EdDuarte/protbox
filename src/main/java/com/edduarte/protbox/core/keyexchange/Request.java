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

import com.edduarte.protbox.core.PbxUser;

import java.io.Serializable;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    public final PbxUser requestingUser;

    public final byte[] encodedUserPublicKey;

    public final byte[] signatureByteArray;


    public Request(PbxUser requestingUser, byte[] encodedUserPublicKey, byte[] signatureByteArray) {
        this.requestingUser = requestingUser;
        this.encodedUserPublicKey = encodedUserPublicKey;
        this.signatureByteArray = signatureByteArray;
    }
}
