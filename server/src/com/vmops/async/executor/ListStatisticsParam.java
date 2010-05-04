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

import java.util.List;

public class ListStatisticsParam {
	
	public enum StatisticsType { UserVm, Host;}

	private StatisticsType type;
	
	// Used for UserVm
	private List<Long> vmIds;
	
	// Used for Host
	private List<Long> hostIds;

	public ListStatisticsParam() {
	}
	
	public StatisticsType getType() {
		return type;
	}
	
	public void setType(StatisticsType type) {
		this.type = type;
	}
	
	public List<Long> getVmIds() {
		return vmIds;
	}
	
	public void setVmIds(List<Long> vmIds) {
		this.vmIds = vmIds;
	}
	
	public List<Long> getHostIds() {
		return hostIds;
	}
	
	public void setHostIds(List<Long> hostIds) {
		this.hostIds = hostIds;
	}

}
