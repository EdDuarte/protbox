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

package edduarte.protbox.core;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public class PbxUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Certificate[] certificateChain;

    private final X509Certificate userCertificate;

    private final String ccNumber;

    private final String userName;


    public PbxUser(Certificate[] certificateChain,
                   X509Certificate userCertificate,
                   String ccNumber,
                   String userName) {
        this.certificateChain = certificateChain;
        this.userCertificate = userCertificate;
        this.ccNumber = ccNumber;
        this.userName = userName;
    }


    public String getId() {
        return ccNumber;
    }


    public String getName() {
        return userName;
    }


    public X509Certificate getUserCertificate() {
        return userCertificate;
    }


    public Certificate[] getCertificateChain() {
        return certificateChain;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PbxUser))
            return false;

        PbxUser other = (PbxUser) obj;
        return this.getId().equalsIgnoreCase(other.getId()) &&
                this.getName().equalsIgnoreCase(other.getName()) &&
                this.getUserCertificate().equals(other.getUserCertificate());
    }


    @Override
    public String toString() {
        return userName;
    }
}
