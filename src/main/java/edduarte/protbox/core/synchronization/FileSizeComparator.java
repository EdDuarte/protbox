/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.PbxEntry;
import edduarte.protbox.core.registry.PbxFile;
import edduarte.protbox.core.registry.PbxFolder;

import java.util.Comparator;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
final class FileSizeComparator implements Comparator<SyncEntry> {

    public int compare(SyncEntry sEntry1, SyncEntry sEntry2) {
        PbxEntry entry1 = sEntry1.entry;
        PbxEntry entry2 = sEntry2.entry;

        if (entry1 instanceof PbxFolder && entry2 instanceof PbxFile) {
            return -1;

        } else if (entry1 instanceof PbxFile && entry2 instanceof PbxFolder) {
            return 1;

        } else if (entry1 instanceof PbxFolder && entry2 instanceof PbxFolder) {
            return 0;

        } else if (entry1 instanceof PbxFile && entry2 instanceof PbxFile) {
            long file1Size = ((PbxFile) entry1).getLatestSnapshot().getSnapshotSize();
            long file2Size = ((PbxFile) entry2).getLatestSnapshot().getSnapshotSize();
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
