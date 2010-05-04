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

package com.vmops.async.executor;

import com.vmops.serializer.Param;

public class ListHostStatsResultObject {
	@Param(name="cpuutilization")
	double cpuUtilization;
	
	@Param(name="usermemory")
	long usedMemory;
	
	@Param(name="freememory")
	long freeMemory;
	
	@Param(name="totalmemory")
	long totalMemory;
	
	@Param(name="publicnetworkreadkbs")
	double publicNetworkReadKBs;
	
	@Param(name="publicnetworkwritekbs")
	double publicNetworkWriteKBs;
	
    public long getUsedMemory() {
    	return usedMemory;
    }
    
    public void setUsedMemory(long usedMemory) {
    	this.usedMemory = usedMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }
    
    public void setFreeMemory(long freeMemory) {
    	this.freeMemory = freeMemory;
    }

    public long getTotalMemory() {
    	return totalMemory;
    }
    
    public void setTotalMemory(long totalMemory) {
    	this.totalMemory = totalMemory;
    }
    
    public double getCpuUtilization() {
        return cpuUtilization;
    }
    
    public void setCpuUtilization(double cpuUtilization) {
    	this.cpuUtilization = cpuUtilization;
    }
    
    public double getPublicNetworkReadKBs() {
    	return publicNetworkReadKBs;
    }
    
    public void setPublicNetworkReadKBs(double publicNetworkReadKBs) {
    	this.publicNetworkReadKBs = publicNetworkReadKBs;
    }
    
    public double getPublicNetworkWriteKBs() {
    	return publicNetworkWriteKBs;
    }
    
    public void setPublicNetworkWriteKBs(double publicNetworkWriteKBs) {
    	this.publicNetworkWriteKBs = publicNetworkWriteKBs;
    }
}
