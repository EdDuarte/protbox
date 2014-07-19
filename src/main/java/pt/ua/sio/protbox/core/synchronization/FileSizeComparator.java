package pt.ua.sio.protbox.core.synchronization;

import pt.ua.sio.protbox.core.directory.Pair;
import pt.ua.sio.protbox.core.directory.PairFile;
import pt.ua.sio.protbox.core.directory.PairFolder;

import java.util.Comparator;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
final class FileSizeComparator implements Comparator<SyncEntry> {

    public int compare(SyncEntry sEntry1, SyncEntry sEntry2){
        Pair entry1 = sEntry1.entry;
        Pair entry2 = sEntry2.entry;

        if(entry1 instanceof PairFolder && entry2 instanceof PairFile){
            return -1;
        } else if(entry1 instanceof PairFile && entry2 instanceof PairFolder){
            return 1;
        } else if(entry1 instanceof PairFolder && entry2 instanceof PairFolder){
            return 0;
        } else if(entry1 instanceof PairFile && entry2 instanceof PairFile){
            long file1Size = ((PairFile)entry1).getSize();
            long file2Size = ((PairFile)entry2).getSize();
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
