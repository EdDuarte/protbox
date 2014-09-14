package edduarte.protbox.core;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Certificate[] certificateChain;
    private final X509Certificate userCertificate;
    private final String ccNumber;
    private final String userName;
    private final String machineName;
    private final String machineSerialNo;


    public User(Certificate[] certificateChain,
                X509Certificate userCertificate,
                String ccNumber,
                String userName,
                String machineName,
                String machineSerialNo) {
        this.certificateChain = certificateChain;
        this.userCertificate = userCertificate;
        this.ccNumber = ccNumber;
        this.userName = userName;
        this.machineName = machineName;
        this.machineSerialNo = machineSerialNo;
    }


    public String getId() {
        return ccNumber;
    }


    public String getName() {
        return userName;
    }


    public String getMachineName() {
        return machineName;
    }


    public X509Certificate getUserCertificate() {
        return userCertificate;
    }


    public Certificate[] getCertificateChain() {
        return certificateChain;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof User))
            return false;

        User other = (User) obj;
        return this.getId().equalsIgnoreCase(other.getId()) &&
                this.getName().equalsIgnoreCase(other.getName()) &&
                this.getUserCertificate().equals(other.getUserCertificate()) &&
                this.machineSerialNo.equals(other.machineSerialNo);
    }


    @Override
    public String toString() {
        return userName;
    }
}
