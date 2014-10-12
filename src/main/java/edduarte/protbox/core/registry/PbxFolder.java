package edduarte.protbox.core.registry;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * PRegFolder is a entry that structures a folder in the Registry. Other than having the same
 * variables as {@link PbxEntry}, a folder is also represented by a list of other subFolders and a list
 * of other subFiles contained in this folder.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
public final class PbxFolder extends PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Set<PbxFolder> subFolders;
    private final Set<PbxFile> subFiles;

    PbxFolder(final PReg parentRegistry,
              final PbxFolder parentFolder,
              final String encodedName,
              final String realName) {

        super(parentRegistry, parentFolder, encodedName, realName);
        subFolders = new LinkedHashSet<>();
        subFiles = new LinkedHashSet<>();
    }


    /**
     * Adds the specified folder as a child of this folder.
     */
    public PbxFolder addFolder(PbxFolder f) {
        subFolders.add(f);
        return this;
    }


    /**
     * Adds the specified file as a child of this folder.
     */
    public PbxFolder addFile(PbxFile f) {
        subFiles.add(f);
        return this;
    }


    /**
     * Returns a iterable structure of sub-folders that are contained in this folder.
     *
     * @return a iterable structure of sub-folders that are contained in this folder.
     */
    Stream<PbxFolder> getSubFolders() {
        return subFolders.stream();
    }


    /**
     * Returns a iterable structure of sub-files that are contained in this folder.
     *
     * @return a iterable structure of sub-files that are contained in this folder.
     */
    Stream<PbxFile> getSubFiles() {
        return subFiles.stream();
    }


    /**
     * Removes the specified file or folder from being a child of this folder.
     */
    void remove(PbxEntry e) {
        if (e instanceof PbxFolder) {
            subFolders.remove(e);

        } else if (e instanceof PbxFile) {
            subFiles.remove(e);

        }
    }


    /**
     * Returns a sub-file of this folder with the specified real or encoded name.
     *
     * @param fileName the real or encoded name of the file to return.
     * @return a sub-file of this folder with the specified real or encoded name.
     */
    PbxFile goToFile(String fileName) {
        Optional<PbxFile> value = subFiles.stream()
                .filter(f -> f.realName().equalsIgnoreCase(fileName) || f.encodedName().equalsIgnoreCase(fileName))
                .findFirst();

        return value.isPresent() ? value.get() : null;
    }


    /**
     * Returns a sub-folder of this folder with the specified real or encoded name.
     *
     * @param folderName the real or encoded name of the folder to return.
     * @return a sub-folder of this folder with the specified real or encoded name.
     */
    PbxFolder goToFolder(String folderName) {
        Optional<PbxFolder> value = subFolders.stream()
                .filter(f -> f.realName().equalsIgnoreCase(folderName) || f.encodedName().equalsIgnoreCase(folderName))
                .findFirst();

        return value.isPresent() ? value.get() : null;
    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
