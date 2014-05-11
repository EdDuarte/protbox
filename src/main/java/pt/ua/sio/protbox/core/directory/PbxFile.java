package pt.ua.sio.protbox.core.directory;

import java.io.Serializable;
import java.util.Date;

/**
 * PbxFile is a entry that structures a file in the Directory. Other than having the same
 * variables as {@link PbxEntry}, this is also represented by an array of bytes that contains
 * the data of the file. This array is only filled when the correspondent file in the native
 * folders are deleted.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class PbxFile extends PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    byte[] data;
    Date lastModified;
    long fileSize;

    PbxFile(final PbxFolder parentFolder, final String encodedName, final String realName, final Date lastModified, long fileSize) {
        super(parentFolder, encodedName, realName);
        this.lastModified = lastModified;
        this.fileSize = fileSize;
    }

    /**
     * Returns the last modified date of the file represented by this entry
     * @return the last modified date of the file represented by this entry
     */
    public Date lastModified() {
        return lastModified;
    }

    public void setLastModified(long newLM) {
        this.lastModified = new Date(newLM);
    }

    public long getSize() {
        return fileSize;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
