package edduarte.protbox.core;


import javax.swing.*;
import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String machineName;
    private final String machineSerialNo;
    private final ImageIcon photo;
    private final X509Certificate certificate;
    private boolean available;

    public User(String id, String name, String machineName, String machineSerialNo, ImageIcon photo, X509Certificate certificate){
        this.id = id;
        this.name = name;
        this.machineName = machineName;
        this.machineSerialNo = machineSerialNo;
        this.photo = photo;
        this.certificate = certificate;
        this.available = true;
    }

    public String getId(){
        return id;
    }

    public String getName() {
        String[] names = name.toLowerCase().split(" ");
        String firstAndLast;
        if(names.length>1)
            firstAndLast = org.apache.commons.lang3.StringUtils.capitalize(names[0])+" "+org.apache.commons.lang3.StringUtils.capitalize(names[names.length-1]);
        else
            firstAndLast = org.apache.commons.lang3.StringUtils.capitalize(names[0]);

        return firstAndLast;
    }

    public String getMachineName() {
        return machineName;
    }

    public ImageIcon getPhoto(){
        return photo;
    }

    public X509Certificate getCertificate() {
//        try{
//            CertificateFactory cf = CertificateFactory.getInstance("X509");
//            Certificate cert = cf.generateCertificate(
//                    new ByteArrayInputStream(certificate));
//
//            return cert;
//        }catch (CertificateException ex){
//            System.err.println(ex);
//            return null;
//        }
        return certificate;
    }

    public boolean isAvailable(){
        return available;
    }

    public void makeAvailable() {
        this.available = true;
    }

    public void makeUnavailable() {
        this.available = false;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof User))
            return false;

        User other = (User) obj;
        return  this.getId().equalsIgnoreCase(other.getId()) &&
                this.getName().equalsIgnoreCase(other.getName()) &&
                this.getCertificate().equals(other.getCertificate()) &&
                this.machineSerialNo.equals(other.machineSerialNo);
    }

    @Override
    public String toString(){
        return name;
    }
}
