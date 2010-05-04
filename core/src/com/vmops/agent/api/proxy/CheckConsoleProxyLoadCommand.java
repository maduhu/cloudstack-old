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

package com.vmops.agent.api.proxy;


/**
 * CheckConsoleProxyLoadCommand implements one-shot console proxy load-scan command
 */
public class CheckConsoleProxyLoadCommand extends ProxyCommand {

	private long proxyVmId;
	private String proxyVmName;
	private String proxyManagementIp;
	private int proxyCmdPort;
	
	public CheckConsoleProxyLoadCommand() {
	}
	
	public CheckConsoleProxyLoadCommand(long proxyVmId, String proxyVmName, String proxyManagementIp, int proxyCmdPort) {
		this.proxyVmId = proxyVmId;
		this.proxyVmName = proxyVmName;
		this.proxyManagementIp = proxyManagementIp;
		this.proxyCmdPort = proxyCmdPort;
	}
	
	public long getProxyVmId() {
		return proxyVmId;
	}
	
	public String getProxyVmName() {
		return proxyVmName;
	}
	
	public String getProxyManagementIp() {
		return proxyManagementIp;
	}
	
	public int getProxyCmdPort() {
		return proxyCmdPort;
	}
	
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
