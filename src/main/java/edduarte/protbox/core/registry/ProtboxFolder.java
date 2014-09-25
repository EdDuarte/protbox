package edduarte.protbox.core.registry;

import java.io.Serializable;
import java.util.*;

/**
 * PRegFolder is a entry that structures a folder in the Registry. Other than having the same
 * variables as {@link ProtboxEntry}, a PairFolder is also represented by a list of other subFolders and a list
 * of other subFiles contained in this folder.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class ProtboxFolder extends ProtboxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<ProtboxFolder> subFolders;
    private final Set<ProtboxFile> subFiles;

    ProtboxFolder(final ProtboxFolder parentFolder, final String encodedName, final String realName) {
        super(parentFolder, encodedName, realName);
        subFolders = new LinkedHashSet<>();
        subFiles = new LinkedHashSet<>();
    }


    /**
     * Adds the specified PairFolder as a child of this PairFolder.
     */
    public ProtboxFolder addFolder(ProtboxFolder f) {
        subFolders.add(f);
        return this;
    }


    /**
     * Adds the specified PairFile as a child of this PairFolder.
     */
    public ProtboxFolder addFile(ProtboxFile f) {
        subFiles.add(f);
        return this;
    }


    /**
     * Returns a iterable structure of sub-folders that are contained in this PairFolder.
     *
     * @return a iterable structure of sub-folders that are contained in this PairFolder.
     */
    Collection<ProtboxFolder> getSubFolders() {
        return Collections.unmodifiableCollection(subFolders);
    }


    /**
     * Returns a iterable structure of sub-files that are contained in this PairFolder.
     *
     * @return a iterable structure of sub-files that are contained in this PairFolder.
     */
    Collection<ProtboxFile> getSubFiles() {
        return Collections.unmodifiableCollection(subFiles);
    }


    /**
     * Removes the specified PairFile or PairFolder from being a child of this PairFolder.
     */
    void remove(ProtboxEntry e) {
        if (e instanceof ProtboxFolder)
            subFolders.remove(e);
        else if (e instanceof ProtboxFile)
            subFiles.remove(e);
    }


    /**
     * Returns a sub-file of this PairFolder with the specified real or encoded name.
     *
     * @param fileName the real or encoded name of the PairFile to return.
     * @return a sub-file of this PairFolder with the specified real or encoded name.
     */
    ProtboxFile goToFile(String fileName) {
        Optional<ProtboxFile> value = subFiles.stream()
                .filter(f -> f.realName().equalsIgnoreCase(fileName) || f.encodedName().equalsIgnoreCase(fileName))
                .findFirst();

        return value.get();
    }


    /**
     * Returns a sub-folder of this PairFolder with the specified real or encoded name.
     *
     * @param folderName the real or encoded name of the PairFolder to return.
     * @return a sub-folder of this PairFolder with the specified real or encoded name.
     */
    ProtboxFolder goToFolder(String folderName) {
        Optional<ProtboxFolder> value = subFolders.stream()
                .filter(f -> f.realName().equalsIgnoreCase(folderName) || f.encodedName().equalsIgnoreCase(folderName))
                .findFirst();

        return value.get();
    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
