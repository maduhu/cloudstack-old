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

package com.vmops.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.vmops.host.HostVO;
import com.vmops.server.ManagementServer;
import com.vmops.utils.component.ComponentLocator;
import com.vmops.vm.UserVm;

/**
 * Thumbnail access : /console?cmd=thumbnail&vm=xxx&w=xxx&h=xxx
 * Console access : /conosole?cmd=access&vm=xxx
 * Authentication : /console?cmd=auth&vm=xxx&sid=xxx
 */
public class ConsoleProxyServlet extends HttpServlet {
	private static final long serialVersionUID = -5515382620323808168L;
	public static final Logger s_logger = Logger.getLogger(ConsoleProxyServlet.class.getName());
	private static final int DEFAULT_THUMBNAIL_WIDTH = 144;
	private static final int DEFAULT_THUMBNAIL_HEIGHT = 110;
	
	private final ManagementServer _ms = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);
	
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		doGet(req, resp);
	}
	
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		try {
			String cmd = req.getParameter("cmd");
			if(cmd == null || !isValidCmd(cmd)) {
				s_logger.info("invalid console servlet command: " + cmd);
				sendResponse(resp, "");
				return;
			}
				
			String vmIdString = req.getParameter("vm");
			long vmId = 0;
			try {
				vmId = Long.parseLong(vmIdString);
			} catch(NumberFormatException e) {
				s_logger.info("invalid console servlet command parameter: " + vmIdString);
				sendResponse(resp, "");
				return;
			}
			
			if(!checkSessionPermision(vmId)) {
				sendResponse(resp, "Permission denied");
				return;
			}
			
			if(cmd.equalsIgnoreCase("thumbnail"))
				handleThumbnailRequest(req, resp, vmId);
			else if(cmd.equalsIgnoreCase("access"))
				handleAccessRequest(req, resp, vmId);
			else 
				handleAuthRequest(req, resp, vmId);
			
		} catch (Throwable e) {
			s_logger.error("Unexepected exception in ConsoleProxyServlet", e);
			sendResponse(resp, "");
		}
	}
	
	private void handleThumbnailRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {
		UserVm vm = _ms.findUserVMInstanceById(vmId);
		if(vm == null) {
			s_logger.warn("VM " + vmId + " does not exist, sending blank response for thumbnail request");
			sendResponse(resp, "");
			return;
		}
		
		if(vm.getHostId() == null) {
			s_logger.warn("VM " + vmId + " lost host info, sending blank response for thumbnail request");
			sendResponse(resp, "");
			return;
		}
		
		HostVO host = _ms.getHostBy(vm.getHostId());
		if(host == null) {
			s_logger.warn("VM " + vmId + "'s host does not exist, sending blank response for thumbnail request");
			sendResponse(resp, "");
			return;
		}
		
		String rootUrl = _ms.getConsoleAccessUrlRoot(vmId);
		if(rootUrl == null) {
			sendResponse(resp, "");
			return;
		}
		
		int w = DEFAULT_THUMBNAIL_WIDTH;
		int h = DEFAULT_THUMBNAIL_HEIGHT;
		
		String value = req.getParameter("w");
		try {
			w = Integer.parseInt(value);
		} catch(NumberFormatException e) {
		}
		
		value = req.getParameter("h");
		try {
			h = Integer.parseInt(value);
		} catch(NumberFormatException e) {
		}
		
		try {
			resp.sendRedirect(composeThumbnailUrl(rootUrl, vm, host, w, h));
		} catch (IOException e) {
			if(s_logger.isInfoEnabled())
				s_logger.info("Client may already close the connection");
		}
	}
	
	private void handleAccessRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {
		UserVm vm = _ms.findUserVMInstanceById(vmId);
		if(vm == null) {
			s_logger.warn("VM " + vmId + " does not exist, sending blank response for console access request");
			sendResponse(resp, "");
			return;
		}
		
		if(vm.getHostId() == null) {
			s_logger.warn("VM " + vmId + " lost host info, sending blank response for console access request");
			sendResponse(resp, "");
			return;
		}
		
		HostVO host = _ms.getHostBy(vm.getHostId());
		if(host == null) {
			s_logger.warn("VM " + vmId + "'s host does not exist, sending blank response for console access request");
			sendResponse(resp, "");
			return;
		}
		
		String rootUrl = _ms.getConsoleAccessUrlRoot(vmId);
		if(rootUrl == null) {
			sendResponse(resp, "<html><body><p>Console access will be ready in a few minutes. Please try it again later.</p></body></html>");
			return;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<html><frameset><frame src=\"").append(composeConsoleAccessUrl(rootUrl, vm, host));
		sb.append("\"></frame></frameset></html>");
		sendResponse(resp, sb.toString());
	}
	
	private void handleAuthRequest(HttpServletRequest req, HttpServletResponse resp, long vmId) {
		
		// TODO authentication channel between console proxy VM and management server needs to be secured, 
		// the data is now being sent through private network, but this is apparently not enough
		UserVm vm = _ms.findUserVMInstanceById(vmId);
		if(vm == null) {
			s_logger.warn("VM " + vmId + " does not exist, sending failed response for authentication request from console proxy");
			sendResponse(resp, "failed");
			return;
		}
		
		if(vm.getHostId() == null) {
			s_logger.warn("VM " + vmId + " lost host info, failed response for authentication request from console proxy");
			sendResponse(resp, "failed");
			return;
		}
		
		HostVO host = _ms.getHostBy(vm.getHostId());
		if(host == null) {
			s_logger.warn("VM " + vmId + "'s host does not exist, sending failed response for authentication request from console proxy");
			sendResponse(resp, "failed");
			return;
		}
		
		String sid = req.getParameter("sid");
		if(sid == null || !sid.equals(vm.getVncPassword())) {
			s_logger.warn("sid " + sid + " in url does not match stored sid " + vm.getVncPassword());
			sendResponse(resp, "failed");
			return;
		}
		
		sendResponse(resp, "success");
	}
	
	private String composeThumbnailUrl(String rootUrl, UserVm vm, HostVO host, int w, int h) {
		StringBuffer sb = new StringBuffer(rootUrl);
		sb.append("/getscreen?host=").append(host.getPrivateIpAddress());
		sb.append("&port=").append(_ms.getVncPort(vm));
		sb.append("&sid=").append(vm.getVncPassword());
		sb.append("&w=").append(w).append("&h=").append(h);
		sb.append("&tag=").append(vm.getId());

		if(s_logger.isInfoEnabled())
			s_logger.info("Compose thumbnail url: " + sb.toString());
		return sb.toString();
	}
	
	private String composeConsoleAccessUrl(String rootUrl, UserVm vm, HostVO host) {
		StringBuffer sb = new StringBuffer(rootUrl);
		sb.append("/ajax?host=").append(host.getPrivateIpAddress());
		sb.append("&port=").append(_ms.getVncPort(vm));
		sb.append("&sid=").append(vm.getVncPassword());
		sb.append("&tag=").append(vm.getId());
		
		if(s_logger.isInfoEnabled())
			s_logger.info("Compose console url: " + sb.toString());
		return sb.toString();
	}
	
	private void sendResponse(HttpServletResponse resp, String content) {
		try {
			resp.getWriter().print(content);
		} catch(IOException e) {
			if(s_logger.isInfoEnabled())
				s_logger.info("Client may already close the connection");
		}
	}
	
	private boolean checkSessionPermision(long vmId) {
		// TODO: skip security check for now
		return true;
	}
	
	private boolean isValidCmd(String cmd) {
		if(cmd.equalsIgnoreCase("thumbnail") || cmd.equalsIgnoreCase("access") || cmd.equalsIgnoreCase("auth"))
			return true;
		
		return false;
	}
}
