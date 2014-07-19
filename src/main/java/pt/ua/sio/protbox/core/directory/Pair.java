package pt.ua.sio.protbox.core.directory;

import java.io.Serializable;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public class Pair implements Serializable {
    private static final long serialVersionUID = 1L;

    private final PairFolder parentFolder;
    private final String encodedName;
    private final String realName;
    boolean hidden;


    /**
     * An entry has a parentFolder, an encodedName and the realName.
     * By default, a entry is not hidden
     */
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

//    /**
//     * When a conflict happens this method is called to rename the file and it's
//     * representation on the Directory to conflicted version.
//     */
//    public void makeThisConflicted() {
//        // separate name from extension
//        int separator = realName.lastIndexOf(".");
//        String name = realName.substring(0, separator);
//        String ext = realName.substring(separator);
//
//        this.realName = name + " (conflicted copy from "+Directory.USER_NAME+")" + ext;
//        try{
//            this.encodedName = new String(Base64.encodeBase64((realName + "»" + Directory.USER_NAME).getBytes("UTF8")));
//        }catch (UnsupportedEncodingException ex) {
//            System.err.println(ex);
//        }
//    }

    /**
     * Returns the relative path from the root to this file, constructed with encoded names.
     * This is used to reach the original file in the shared folder
     */
    public String relativeEncodedPath() {
        String path = "";
        if(parentFolder!=null)
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
        if(parentFolder!=null)
            path = path + parentFolder.relativeRealPath() + java.io.File.separator;
        return path + this.realName;
    }

    /**
     * Returns if the entry is hidden or not. If this entry is hidden, it means that the file
     * by this entry was deleted from its native folders.
     * @return <tt>true</tt> if this entry is hidden or <tt>false</tt> if it isn't
     */
    public boolean isHidden() {
        return hidden;
    }


//    public void setLastModified(Date lastModified) {
//        this.lastModified = lastModified;
//    }


    public String toString() {
        return realName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Pair) {
            Pair pe = (Pair) obj;
            return this==pe || this.relativeRealPath().equalsIgnoreCase(pe.relativeRealPath()) &&
                    this.relativeEncodedPath().equalsIgnoreCase(pe.relativeEncodedPath());
        }
        return false;
    }
}
