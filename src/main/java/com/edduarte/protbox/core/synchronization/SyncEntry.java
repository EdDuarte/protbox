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

package com.edduarte.protbox.core.synchronization;

import com.edduarte.protbox.core.registry.PReg;
import com.edduarte.protbox.core.registry.PbxEntry;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
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