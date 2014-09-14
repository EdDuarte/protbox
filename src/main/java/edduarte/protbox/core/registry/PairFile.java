package edduarte.protbox.core.registry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * PairFile is a entry that structures a file in the Registry. Other than having the same
 * variables as {@link Pair}, a PairFile is also represented by an array of bytes that contains
 * the data of the file. This array is only filled when the correspondent file in the native
 * folders are hidden.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PairFile extends Pair implements Serializable {
    private static final long serialVersionUID = 1L;

    private byte[] data;
//    private long fileSize;
    private Date lastModified;

    PairFile(final PairFolder parentFolder, final String encodedName, final String realName, final Date lastModified, long fileSize) {
        super(parentFolder, encodedName, realName);
        this.lastModified = lastModified;
//        this.fileSize = fileSize;
    }


    /**
     * Returns the data of the file represented by this pair.
     *
     * @return the data of the file represented by this pair.
     */
    byte[] getData() {
        return data;
    }


    /**
     * Sets the data of the file to the specified data.
     */
    void setData(byte[] data) {
        this.data = data;
    }


    /**
     * Suggests Java Garbage Collector to stop storing this pair's data.
     */
    void emptyData() {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
            data = null;
            System.gc();
        }
    }


    /**
     * Returns the file size of the file represented by this pair.
     *
     * @return the file size of the file represented by this pair.
     */
    public int getFileSize() {
        return data.length;
    }


//    /**
//     * Sets the file size of the file to the specified file size.
//     */
//    void setFileSize(long fileSize) {
//        this.fileSize = fileSize;
//    }


    /**
     * Returns the last modified date of the file represented by this pair.
     *
     * @return the last modified date of the file represented by this pair.
     */
    public Date getLastModified() {
        return lastModified;
    }


    /**
     * Sets the last modified date of the file to the specified last modified date.
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
