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

package com.vmops.consoleproxy;

import com.vmops.agent.api.AgentControlAnswer;
import com.vmops.agent.api.ConsoleAccessAuthenticationCommand;
import com.vmops.agent.api.ConsoleProxyLoadReportCommand;
import com.vmops.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.vmops.utils.component.Manager;
import com.vmops.vm.ConsoleProxyVO;

public interface ConsoleProxyManager extends Manager {
	public static final int DEFAULT_PROXY_CAPACITY = 50;
	public static final int DEFAULT_STANDBY_CAPACITY = 10;
	public static final int DEFAULT_PROXY_VM_RAMSIZE = 1024;			// 1G
	
	public static final int DEFAULT_PROXY_CMD_PORT = 8001;
	public static final int DEFAULT_PROXY_VNC_PORT = 0;
	public static final int DEFAULT_PROXY_URL_PORT = 80;
	public static final int DEFAULT_PROXY_SESSION_TIMEOUT = 300000;		// 5 minutes
	
	public static final String ALERT_SUBJECT = "proxy-alert";
	
	public ConsoleProxyVO assignProxy(long dataCenterId, long userVmId);
	
	public ConsoleProxyVO startProxy(long proxyVmId);
	public boolean stopProxy(long proxyVmId);
	public boolean rebootProxy(long proxyVmId);
	public boolean destroyProxy(long proxyVmId);
	
	public void onLoadReport(ConsoleProxyLoadReportCommand cmd);
	public AgentControlAnswer onConsoleAccessAuthentication(ConsoleAccessAuthenticationCommand cmd);

	/**
	 * Will be obsolete soon
	 * @param answer
	 */
	public void onLoadAnswer(ConsoleProxyLoadAnswer answer);
}
