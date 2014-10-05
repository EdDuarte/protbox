package edduarte.protbox.utils;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eduardo Duarte (<a href="mailto:eduarte@ubiwhere.com">eduarte@ubiwhere.com</a>)
 * @version 1.0
 */
public class TokenParser {

    private final Map<String, String> tokenData;

    private TokenParser(Map<String, String> tokenData) {
        this.tokenData = tokenData;
    }

    public static TokenParser parse(X509Certificate certificate) {
        String subjectDN = certificate.getSubjectDN().getName();
        System.out.println(subjectDN);

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
