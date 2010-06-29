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
package com.cloud.agent.api;

/**
 * This command encapsulates a primitive operation which enables coalescing the backed up VHD snapshots on the secondary server
 * This currently assumes that the secondary storage are mounted on the XenServer.  
 */
public class DeleteSnapshotBackupCommand extends SnapshotCommand {
    private String childUUID;
    
    protected DeleteSnapshotBackupCommand() {
        
    }
    
    /**
     * Given 2 VHD files on the secondary storage which are linked in a parent chain as follows:
     * backupUUID = parent(childUUID)
     * It gets another VHD 
     * previousBackupVHD = parent(backupUUID)
     * 
     * And
     * 1) it coalesces backupUuid into its parent.
     * 2) It deletes the VHD file corresponding to backupUuid
     * 3) It sets the parent VHD of childUUID to that of previousBackupUuid
     * 
     * It takes care of the cases when
     * 1) childUUID is null. - Step 3 is not done.
     * 2) previousBackupUUID is null 
     *       - Merge childUUID into its parent backupUUID
     *       - Set the UUID of the resultant VHD to childUUID
     *       - Essentially we are deleting the oldest VHD file and setting the current oldest VHD to childUUID                               
     *       
     * @param volumeName                  The name of the volume whose snapshot was taken (something like i-3-SV-ROOT) 
     * @param secondaryStoragePoolURL    This is what shows up in the UI when you click on Secondary storage. 
     *                                    In the code, it is present as: In the vmops.host_details table, there is a field mount.parent. This is the value of that field
     *                                    If you have better ideas on how to get it, you are welcome. 
     * @param backupUUID                  The VHD which has to be deleted    
     * @param childUUID                   The child VHD file of the backup whose parent is reset to its grandparent.  
     */
    public DeleteSnapshotBackupCommand(String primaryStoragePoolNameLabel,
                                       String secondaryStoragePoolURL,
                                       Long   dcId,
                                       Long   accountId,
                                       Long   volumeId,
                                       String backupUUID, 
                                       String childUUID) 
    {
        super(primaryStoragePoolNameLabel, secondaryStoragePoolURL, backupUUID, dcId, accountId, volumeId);
        this.childUUID = childUUID;
    }

    /**
     * @return the childUUID
     */
    public String getChildUUID() {
        return childUUID;
    }

}