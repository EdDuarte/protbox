package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.directory.Registry;
import edduarte.protbox.core.directory.Pair;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
final class SyncEntry {
    final Registry directory;
    final Pair entry;

    SyncEntry(Registry directory, Pair entry){
        this.directory = directory;
        this.entry = entry;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof SyncEntry){
            SyncEntry se = (SyncEntry)obj;
            return this==se || this.directory.equals(se.directory) && this.entry.equals(se.entry);
//        } else if(obj instanceof PbxEntry){
//            PbxEntry pe = (PbxEntry)obj;
//            return this.entry.equals(pe);
        }
        return false;
    }
}