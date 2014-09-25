package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.ProtboxEntry;
import edduarte.protbox.core.registry.ProtboxFile;
import edduarte.protbox.core.registry.ProtboxFolder;

import java.util.Comparator;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 2.0
 */
final class FileSizeComparator implements Comparator<SyncEntry> {

    public int compare(SyncEntry sEntry1, SyncEntry sEntry2) {
        ProtboxEntry entry1 = sEntry1.entry;
        ProtboxEntry entry2 = sEntry2.entry;

        if (entry1 instanceof ProtboxFolder && entry2 instanceof ProtboxFile) {
            return -1;

        } else if (entry1 instanceof ProtboxFile && entry2 instanceof ProtboxFolder) {
            return 1;

        } else if (entry1 instanceof ProtboxFolder && entry2 instanceof ProtboxFolder) {
            return 0;

        } else if (entry1 instanceof ProtboxFile && entry2 instanceof ProtboxFile) {
            long file1Size = ((ProtboxFile) entry1).getFileSize();
            long file2Size = ((ProtboxFile) entry2).getFileSize();
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
