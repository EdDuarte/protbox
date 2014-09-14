package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.Pair;
import edduarte.protbox.core.registry.PairFile;
import edduarte.protbox.core.registry.PairFolder;

import java.util.Comparator;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
final class FileSizeComparator implements Comparator<SyncPair> {

    public int compare(SyncPair sEntry1, SyncPair sEntry2) {
        Pair entry1 = sEntry1.pair;
        Pair entry2 = sEntry2.pair;

        if (entry1 instanceof PairFolder && entry2 instanceof PairFile) {
            return -1;

        } else if (entry1 instanceof PairFile && entry2 instanceof PairFolder) {
            return 1;

        } else if (entry1 instanceof PairFolder && entry2 instanceof PairFolder) {
            return 0;

        } else if (entry1 instanceof PairFile && entry2 instanceof PairFile) {
            long file1Size = ((PairFile) entry1).getFileSize();
            long file2Size = ((PairFile) entry2).getFileSize();
            if (file1Size < file2Size) {
                return -1;

            } else if (file1Size == file2Size) {
                return 0;

            } else if (file1Size > file2Size) {
                return 1;

            }

        }

        return 0;
    }
}
