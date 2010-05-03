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
package com.vmops.agent.api.storage;



/**
 * @author chiradeep
 *
 */
public class DownloadProgressCommand extends DownloadCommand {
	public static enum RequestType {GET_STATUS, ABORT, RESTART, PURGE, GET_OR_RESTART}
	private String jobId;
	private RequestType request;

	protected DownloadProgressCommand() {
		super();
	}
	
	public DownloadProgressCommand(DownloadCommand cmd, String jobId, RequestType req) {
	    super(cmd);

		this.jobId = jobId;
		this.setRequest(req);
	}

	public String getJobId() {
		return jobId;
	}

	public void setRequest(RequestType request) {
		this.request = request;
	}

	public RequestType getRequest() {
		return request;
	}
}
