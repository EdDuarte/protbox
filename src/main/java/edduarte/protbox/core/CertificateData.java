package edduarte.protbox.core;

import java.io.Serializable;
import java.security.PrivateKey;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class CertificateData implements Serializable {

    private static final long serialVersionUID = 1L;

    public final byte[] encodedPublicKey;
    public final byte[] signatureBytes;
    public final PrivateKey exchangePrivateKey;

    public CertificateData(byte[] encodedPublicKey, byte[] signatureBytes, PrivateKey exchangePrivateKey) {

        this.encodedPublicKey = encodedPublicKey;
        this.signatureBytes = signatureBytes;
        this.exchangePrivateKey = exchangePrivateKey;
    }
}
