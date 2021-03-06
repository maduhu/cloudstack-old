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

public class ConsoleAccessAuthenticationCommand extends AgentControlCommand {
	
	private String _host;
	private String _port;
	private String _vmId;
	private String _sid;
	private String _ticket;
	
	public ConsoleAccessAuthenticationCommand() {
	}
	
	public ConsoleAccessAuthenticationCommand(String host, String port, String vmId, String sid, String ticket) {
		_host = host;
		_port = port;
		_vmId = vmId;
		_sid = sid;
		_ticket = ticket;
	}
	
	public String getHost() {
		return _host;
	}
	
	public String getPort() {
		return _port;
	}
	
	public String getVmId() {
		return _vmId;
	}
	
	public String getSid() {
		return _sid;
	}
	
	public String getTicket() {
		return _ticket;
	}
}
