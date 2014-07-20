package edduarte.protbox.core.directory;

import java.io.Serializable;
import java.util.*;

/**
 * PbxFolder is a entry that structures a folder in the Directory. Other than having the same
 * variables as {@link Pair}, this is also represented by a list of other folders and a list
 * of other files contained in this folder.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class PairFolder extends Pair implements Serializable {
    private static final long serialVersionUID = 1L;
    Set<PairFolder> folders;
    Set<PairFile> files;

    PairFolder(final PairFolder parentFolder, final String encodedName, final String realName) {
        super(parentFolder, encodedName, realName);
        folders = new LinkedHashSet<>();
        files = new LinkedHashSet<>();
    }

    public PairFolder addFolder(PairFolder f) {
        folders.add(f);
        return this;
    }

    public PairFolder addFile(PairFile f) {
        files.add(f);
        return this;
    }

    void removeEntry(Pair e){
        if(e instanceof PairFolder)
            folders.remove(e);
        else if(e instanceof PairFile)
            files.remove(e);
    }

    PairFile goToFile(String filename) {
        if(files.size()!=0){
            for(PairFile f : files){
                if(f.realName().equalsIgnoreCase(filename) ||
                        f.encodedName().equalsIgnoreCase(filename))
                    return f;
            }
        }
        return null;
    }

    public PairFolder goToFolder(String foldername) {
        if(folders.size()!=0){
            for(PairFolder f : folders){
                if(f.realName().equalsIgnoreCase(foldername) ||
                        f.encodedName().equalsIgnoreCase(foldername))
                    return f;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
