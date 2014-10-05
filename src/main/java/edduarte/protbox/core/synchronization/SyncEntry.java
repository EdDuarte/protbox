package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.PbxEntry;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
final class SyncEntry {
    final PReg reg;
    final PbxEntry entry;

    SyncEntry(PReg reg, PbxEntry entry) {
        this.reg = reg;
        this.entry = entry;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;

        } else if (this == obj) {
            return true;
        }

        if (obj instanceof SyncEntry) {
            SyncEntry se = (SyncEntry) obj;
            return this == se || this.reg.equals(se.reg) && this.entry.equals(se.entry);
        }
        return false;
    }
}