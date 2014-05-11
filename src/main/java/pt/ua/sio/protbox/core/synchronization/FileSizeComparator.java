package pt.ua.sio.protbox.core.synchronization;

import pt.ua.sio.protbox.core.directory.PbxEntry;
import pt.ua.sio.protbox.core.directory.PbxFile;
import pt.ua.sio.protbox.core.directory.PbxFolder;

import java.util.Comparator;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
final class FileSizeComparator implements Comparator<SyncEntry> {

    public int compare(SyncEntry sEntry1, SyncEntry sEntry2){
        PbxEntry entry1 = sEntry1.entry;
        PbxEntry entry2 = sEntry2.entry;

        if(entry1 instanceof PbxFolder && entry2 instanceof PbxFile){
            return -1;
        } else if(entry1 instanceof PbxFile && entry2 instanceof PbxFolder){
            return 1;
        } else if(entry1 instanceof PbxFolder && entry2 instanceof PbxFolder){
            return 0;
        } else if(entry1 instanceof PbxFile && entry2 instanceof PbxFile){
            long file1Size = ((PbxFile)entry1).getSize();
            long file2Size = ((PbxFile)entry2).getSize();
            if(file1Size<file2Size){
                return -1;
            }else if(file1Size==file2Size){
                return 0;
            }else if(file1Size>file2Size){
                return 1;
            }
        }
        return 0;
    }
}
