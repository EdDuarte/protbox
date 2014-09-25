package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.ProtboxRegistry;
import edduarte.protbox.core.registry.ProtboxEntry;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
final class SyncEntry {
    final ProtboxRegistry reg;
    final ProtboxEntry entry;

    SyncEntry(ProtboxRegistry reg, ProtboxEntry entry) {
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