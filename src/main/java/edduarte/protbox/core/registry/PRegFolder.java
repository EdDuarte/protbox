package edduarte.protbox.core.registry;

import java.io.Serializable;
import java.util.*;

/**
 * PRegFolder is a entry that structures a folder in the Registry. Other than having the same
 * variables as {@link PRegEntry}, a PairFolder is also represented by a list of other subFolders and a list
 * of other subFiles contained in this folder.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PRegFolder extends PRegEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<PRegFolder> subFolders;
    private final Set<PRegFile> subFiles;

    PRegFolder(final PRegFolder parentFolder, final String encodedName, final String realName) {
        super(parentFolder, encodedName, realName);
        subFolders = new LinkedHashSet<>();
        subFiles = new LinkedHashSet<>();
    }


    /**
     * Adds the specified PairFolder as a child of this PairFolder.
     */
    public PRegFolder addFolder(PRegFolder f) {
        subFolders.add(f);
        return this;
    }


    /**
     * Adds the specified PairFile as a child of this PairFolder.
     */
    public PRegFolder addFile(PRegFile f) {
        subFiles.add(f);
        return this;
    }


    /**
     * Returns a iterable structure of sub-folders that are contained in this PairFolder.
     *
     * @return a iterable structure of sub-folders that are contained in this PairFolder.
     */
    Collection<PRegFolder> getSubFolders() {
        return Collections.unmodifiableCollection(subFolders);
    }


    /**
     * Returns a iterable structure of sub-files that are contained in this PairFolder.
     *
     * @return a iterable structure of sub-files that are contained in this PairFolder.
     */
    Collection<PRegFile> getSubFiles() {
        return Collections.unmodifiableCollection(subFiles);
    }


    /**
     * Removes the specified PairFile or PairFolder from being a child of this PairFolder.
     */
    void remove(PRegEntry e) {
        if (e instanceof PRegFolder)
            subFolders.remove(e);
        else if (e instanceof PRegFile)
            subFiles.remove(e);
    }


    /**
     * Returns a sub-file of this PairFolder with the specified real or encoded name.
     *
     * @param fileName the real or encoded name of the PairFile to return.
     * @return a sub-file of this PairFolder with the specified real or encoded name.
     */
    PRegFile goToFile(String fileName) {
        Optional<PRegFile> value = subFiles.stream()
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
    PRegFolder goToFolder(String folderName) {
        Optional<PRegFolder> value = subFolders.stream()
                .filter(f -> f.realName().equalsIgnoreCase(folderName) || f.encodedName().equalsIgnoreCase(folderName))
                .findFirst();

        return value.get();
    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
