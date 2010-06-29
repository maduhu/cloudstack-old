/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage;

public class Storage {
    public enum ImageFormat {
        QCOW2(true, true, false),
        RAW(false, false, false),
        VHD(true, true, true),
        ISO(false, false, false);
        
        private final boolean thinProvisioned;
        private final boolean supportSparse;
        private final boolean supportSnapshot;
        
        private ImageFormat(boolean thinProvisioned, boolean supportSparse, boolean supportSnapshot) {
            this.thinProvisioned = thinProvisioned;
            this.supportSparse = supportSparse;
            this.supportSnapshot = supportSnapshot;
        }
        
        public boolean isThinProvisioned() {
            return thinProvisioned;
        }
        
        public boolean supportsSparse() {
            return supportSparse;
        }
        
        public boolean supportSnapshot() {
            return supportSnapshot;
        }
        
        public String getFileExtension() {
            return toString().toLowerCase();
        }
    }
    
    public enum FileSystem {
        ext3,
        ntfs,
        fat,
        fat32,
        ext2,
        ext4,
        cdfs,
        hpfs,
        ufs,
        hfs,
        hfsp
    }
    
    public enum Type {
        Filesystem,         //local directory
        NetworkFilesystem,  //NFS or CIFS
        IscsiLUN,           //shared LUN, with a clusterfs overlay
        IscsiLUNPerVolume,  //for e.g., ZFS Comstar
        ISO,                // for iso image
        LVM,                // XenServer local LVM SR
    }
}
