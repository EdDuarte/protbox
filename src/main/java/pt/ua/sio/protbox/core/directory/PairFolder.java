package pt.ua.sio.protbox.core.directory;

import java.io.Serializable;
import java.util.*;

/**
 * PbxFolder is a entry that structures a folder in the Directory. Other than having the same
 * variables as {@link ProtboxPair}, this is also represented by a list of other folders and a list
 * of other files contained in this folder.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)),
 *         Filipe Pinheiro (<a href="mailto:filipepinheiro@ua.pt">filipepinheiro@ua.pt</a>))
 * @version 1.0
 */
public final class PairFolder extends ProtboxPair implements Serializable {
    private static final long serialVersionUID = 1L;
    Set<PairFolder> folders;
    Set<PbxFile> files;

    PairFolder(final PairFolder parentFolder, final String encodedName, final String realName) {
        super(parentFolder, encodedName, realName);
        folders = new LinkedHashSet<>();
        files = new LinkedHashSet<>();
    }

    public PairFolder addFolder(PairFolder f) {
        folders.add(f);
        return this;
    }

    public PairFolder addFile(PbxFile f) {
        files.add(f);
        return this;
    }

    void removeEntry(ProtboxPair e){
        if(e instanceof PairFolder)
            folders.remove(e);
        else if(e instanceof PbxFile)
            files.remove(e);
    }

    PbxFile goToFile(String filename) {
        if(files.size()!=0){
            for(PbxFile f : files){
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
