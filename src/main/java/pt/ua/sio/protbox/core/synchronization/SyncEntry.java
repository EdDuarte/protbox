package pt.ua.sio.protbox.core.synchronization;

import pt.ua.sio.protbox.core.directory.Directory;
import pt.ua.sio.protbox.core.directory.PbxEntry;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
final class SyncEntry {
    Directory directory;
    PbxEntry entry;

    SyncEntry(Directory directory, PbxEntry entry){
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