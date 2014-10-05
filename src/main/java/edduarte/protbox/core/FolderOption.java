package edduarte.protbox.core;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public enum FolderOption {

    SHARED, PROT;

    public FolderOption inverse() {

        if (this.equals(SHARED)) {
            return PROT;

        } else if (this.equals(PROT)) {
            return SHARED;

        } else {
            return null;

        }
    }

}
