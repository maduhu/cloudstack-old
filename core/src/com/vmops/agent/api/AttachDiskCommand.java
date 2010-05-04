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

package com.vmops.agent.api;

public class AttachDiskCommand extends Command {
	
	boolean attach;
	String vmName;
	String volumeFolder;
	String volumePath;
	String volumeName;
	
	// For XenServer
	String volumeNameLabel;
	
	protected AttachDiskCommand() {
	}
	
	public AttachDiskCommand(boolean attach, String vmName, String volumeFolder, String volumePath, String volumeName, String volumeNameLabel) {
		this.attach = attach;
		this.vmName = vmName;
		this.volumeFolder = volumeFolder;
		this.volumePath = volumePath;
		this.volumeName = volumeName;
		this.volumeNameLabel = volumeNameLabel;
	}
	
	@Override
    public boolean executeInSequence() {
        return true;
    }
	
	public boolean getAttach() {
		return attach;
	}
	
	public String getVmName() {
		return vmName;
	}
	
	public String getVolumeFolder() {
		return volumeFolder;
	}
	
	public String getVolumePath() {
		return volumePath;
	}
	
	public String getVolumeName() {
		return volumeName;
	}
	
	public String getVolumeNameLabel() {
		return volumeNameLabel;
	}
	
}
