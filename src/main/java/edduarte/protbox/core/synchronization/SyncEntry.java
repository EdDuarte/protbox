package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.PRegEntry;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
final class SyncEntry {
    final PReg reg;
    final PRegEntry PRegEntry;

    SyncEntry(PReg reg, PRegEntry PRegEntry) {
        this.reg = reg;
        this.PRegEntry = PRegEntry;
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
            return this == se || this.reg.equals(se.reg) && this.PRegEntry.equals(se.PRegEntry);
        }
        return false;
    }
}