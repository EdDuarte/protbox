package edduarte.protbox.core.registry;

import java.io.Serializable;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public abstract class PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final PReg parentRegistry;
    private final PbxFolder parentFolder;
    private final String encodedName;
    private final String realName;
    boolean hidden;


    PbxEntry(final PReg parentRegistry,
             final PbxFolder parentFolder,
             final String encodedName,
             final String realName) {
        this.parentRegistry = parentRegistry;
        this.parentFolder = parentFolder;
        this.encodedName = encodedName;
        this.realName = realName;
        this.hidden = false;
    }

    /**
     * The parentFolder is a Folder Entry(PbxFolder).
     */
    public PbxFolder parentFolder() {
        return parentFolder;
    }

    /**
     * The encoded name is name used in the shared folder,
     * which is obtained by encoding the real name in Base64.
     */
    public String encodedName() {
        return encodedName;
    }

    /**
     * The real name of the entry, used in the Prot folder.
     */
    public String realName() {
        return realName;
    }


    /**
     * Returns the relative path from the root to this file, constructed with encoded names.
     * This is used to reach the original file in the shared folder
     */
    public String relativeEncodedPath() {
        String path = "";
        if (parentFolder != null)
            path = path + parentFolder.relativeEncodedPath() + java.io.File.separator;
        return path + this.encodedName;
    }

    /**
     * Returns the relative path from the root to this file, constructed with the real names
     * and including extensions (only available if this entry is a file).
     * This is used to reach the original file in the protbox/output folder
     */
    public String relativeRealPath() {
        String path = "";
        if (parentFolder != null)
            path = path + parentFolder.relativeRealPath() + java.io.File.separator;
        return path + this.realName;
    }

    /**
     * Returns if the entry is hidden or not. If this entry is hidden, it means that the
     * file or folder represented by this entry is hidden from its native folders.
     *
     * @return <tt>true</tt> if this entry is hidden or <tt>false</tt> if it isn't
     */
    public boolean isHidden() {
        return hidden;
    }


    public String toString() {
        return realName;
    }


    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof PbxEntry) {
            PbxEntry pe = (PbxEntry) obj;
            return this.relativeRealPath().equalsIgnoreCase(pe.relativeRealPath()) &&
                    this.relativeEncodedPath().equalsIgnoreCase(pe.relativeEncodedPath());
        }

        return false;
    }
}
