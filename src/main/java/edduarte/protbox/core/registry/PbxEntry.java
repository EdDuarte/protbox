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

package edduarte.protbox.core.registry;

import java.io.Serializable;

/**
 * @author Ed Duarte (<a href="mailto:edmiguelduarte@gmail.com">edmiguelduarte@gmail.com</a>)
 * @version 2.0
 */
public abstract class PbxEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final PReg parentRegistry;

    private final PbxFolder parentFolder;

    private final String encodedName;

    private final String realName;

    boolean areNativeFilesDeleted;


    PbxEntry(final PReg parentRegistry,
             final PbxFolder parentFolder,
             final String encodedName,
             final String realName) {
        this.parentRegistry = parentRegistry;
        this.parentFolder = parentFolder;
        this.encodedName = encodedName;
        this.realName = realName;
        this.areNativeFilesDeleted = false;
    }


    /**
     * The parentFolder is a Folder Entry(PbxFolder).
     */
    public PbxFolder parentFolder() {
        return parentFolder;
    }


    /**
     * The encoded name is name used in the shared folder,
     * which is obtained by encoding the real name in Base64.
     */
    public String encodedName() {
        return encodedName;
    }


    /**
     * The real name of the entry, used in the Prot folder.
     */
    public String realName() {
        return realName;
    }


    /**
     * Returns the relative path from the root to this file, constructed with encoded names.
     * This is used to reach the original file in the shared folder
     */
    public String relativeEncodedPath() {
        String path = "";
        if (parentFolder != null)
            path = path + parentFolder.relativeEncodedPath() + java.io.File.separator;
        return path + this.encodedName;
    }


    /**
     * Returns the relative path from the root to this file, constructed with the real names
     * and including extensions (only available if this entry is a file).
     * This is used to reach the original file in the protbox/output folder
     */
    public String relativeRealPath() {
        String path = "";
        if (parentFolder != null)
            path = path + parentFolder.relativeRealPath() + java.io.File.separator;
        return path + this.realName;
    }


    /**
     * Returns if the files or folders represented by this entry are deleted from its native folders.
     *
     * @return <tt>true</tt> if the respective files are deleted or <tt>false</tt> if they are not
     */
    public boolean areNativeFilesDeleted() {
        return areNativeFilesDeleted;
    }


    public String toString() {
        return realName;
    }


    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof PbxEntry) {
            PbxEntry pe = (PbxEntry) obj;
            return this.relativeRealPath().equalsIgnoreCase(pe.relativeRealPath()) &&
                    this.relativeEncodedPath().equalsIgnoreCase(pe.relativeEncodedPath());
        }

        return false;
    }
}
