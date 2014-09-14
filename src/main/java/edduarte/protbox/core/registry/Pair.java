package edduarte.protbox.core.registry;

import java.io.Serializable;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public class Pair implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PairFolder parentFolder;
    private final String encodedName;
    private final String realName;
    boolean hidden;
    
    
    Pair(final PairFolder parentFolder, final String encodedName, final String realName) {
        this.parentFolder = parentFolder;
        this.encodedName = encodedName;
        this.realName = realName;
        this.hidden = false;
    }

    /**
     * The parentFolder is a Folder Entry(PbxFolder).
     */
    public PairFolder parentFolder() {
        return parentFolder;
    }

    /**
     * The encoded name is name used in the shared folder,
     * which is obtained by encoding the realName in Base64
     */
    public String encodedName() {
        return encodedName;
    }

    /**
     * The realName is a the real name of the file.
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
     * and including extensions (only available if this pair is a file).
     * This is used to reach the original file in the protbox/output folder
     */
    public String relativeRealPath() {
        String path = "";
        if (parentFolder != null)
            path = path + parentFolder.relativeRealPath() + java.io.File.separator;
        return path + this.realName;
    }

    /**
     * Returns if the pair is hidden or not. If this pair is hidden, it means that the
     * file or folder represented by this pair is hidden from its native folders.
     *
     * @return <tt>true</tt> if this pair is hidden or <tt>false</tt> if it isn't
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

        if (obj instanceof Pair) {
            Pair pe = (Pair) obj;
            return this.relativeRealPath().equalsIgnoreCase(pe.relativeRealPath()) &&
                   this.relativeEncodedPath().equalsIgnoreCase(pe.relativeEncodedPath());
        }

        return false;
    }
}
