package edduarte.protbox.core.synchronization;

import edduarte.protbox.core.registry.PReg;
import edduarte.protbox.core.registry.Pair;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
final class SyncPair {
    final PReg reg;
    final Pair pair;

    SyncPair(PReg reg, Pair pair) {
        this.reg = reg;
        this.pair = pair;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;

        } else if (this == obj) {
            return true;
        }

        if (obj instanceof SyncPair) {
            SyncPair se = (SyncPair) obj;
            return this == se || this.reg.equals(se.reg) && this.pair.equals(se.pair);
        }
        return false;
    }
}