package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.PRegEntry;
import edduarte.protbox.core.registry.PRegFile;
import edduarte.protbox.core.registry.PRegFolder;

import java.util.Comparator;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
final class FileSizeComparator implements Comparator<SyncEntry> {

    public int compare(SyncEntry sEntry1, SyncEntry sEntry2) {
        PRegEntry entry1 = sEntry1.PRegEntry;
        PRegEntry entry2 = sEntry2.PRegEntry;

        if (entry1 instanceof PRegFolder && entry2 instanceof PRegFile) {
            return -1;

        } else if (entry1 instanceof PRegFile && entry2 instanceof PRegFolder) {
            return 1;

        } else if (entry1 instanceof PRegFolder && entry2 instanceof PRegFolder) {
            return 0;

        } else if (entry1 instanceof PRegFile && entry2 instanceof PRegFile) {
            long file1Size = ((PRegFile) entry1).getFileSize();
            long file2Size = ((PRegFile) entry2).getFileSize();
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
