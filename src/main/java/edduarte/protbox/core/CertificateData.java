package edduarte.protbox.core;

import java.io.Serializable;
import java.security.PrivateKey;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class CertificateData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] encodedPublicKey;
    private final byte[] signatureBytes;
    private final PrivateKey exchangePrivateKey;

    public CertificateData(byte[] encodedPublicKey, byte[] signatureBytes, PrivateKey exchangePrivateKey) {

        this.encodedPublicKey = encodedPublicKey;
        this.signatureBytes = signatureBytes;
        this.exchangePrivateKey = exchangePrivateKey;
    }

    public byte[] getEncodedPublicKey() {
        return encodedPublicKey;
    }

    public byte[] getSignatureBytes() {
        return signatureBytes;
    }

    public PrivateKey getExchangePrivateKey() {
        return exchangePrivateKey;
    }
}
