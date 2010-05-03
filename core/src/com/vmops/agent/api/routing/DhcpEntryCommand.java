/**
 *  Copyright (C) 2010 VMOps, Inc.  All rights reserved.
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

package com.vmops.agent.api.routing;


public class DhcpEntryCommand extends RoutingCommand {

    String vmMac;
    String vmIpAddress;
    String routerPrivateIpAddress;
    String vmName;
    
    protected DhcpEntryCommand() {
    	
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
    
    public DhcpEntryCommand(String vmMac, String vmIpAddress, String routerPrivateIpAddress, String vmName) {
        this.vmMac = vmMac;
        this.vmIpAddress = vmIpAddress;
        this.routerPrivateIpAddress = routerPrivateIpAddress;
        this.vmName = vmName;
    }
    
	public String getVmMac() {
		return vmMac;
	}
	
	public String getRouterPrivateIpAddress() {
		return routerPrivateIpAddress;
	}
	
	public String getVmIpAddress() {
		return vmIpAddress;
	}
	
	public String getVmName() {
		return vmName;
	}
	
}
