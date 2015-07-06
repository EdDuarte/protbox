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

package edduarte.protbox.utils;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 1.0
 */
public class TokenParser {

    private final Map<String, String> tokenData;


    private TokenParser(Map<String, String> tokenData) {
        this.tokenData = tokenData;
    }


    public static TokenParser parse(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectDN().getName();

        Map<String, String> map = new HashMap<>();
        String[] split1 = subjectDN.split(", ");

        for (String s : split1) {
            String[] split2 = s.split("=");
            map.put(split2[0], split2[1]);
        }

        return new TokenParser(map);
    }


    public String getUserName() {
        return tokenData.get("CN");
    }


    public String getSerialNumber() {
        return tokenData.get("SERIALNUMBER");
    }
}
