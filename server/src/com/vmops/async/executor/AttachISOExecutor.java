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

package com.vmops.async.executor;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmops.agent.api.Answer;
import com.vmops.api.BaseCmd;
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobResult;
import com.vmops.async.AsyncJobVO;
import com.vmops.serializer.GsonHelper;

public class AttachISOExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(AttachISOExecutor.class.getName());
    
	public boolean execute() {
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	Gson gson = GsonHelper.getBuilder().create();
    	
		if(getSyncSource() == null) {
	    	AttachISOParam param = gson.fromJson(job.getCmdInfo(), AttachISOParam.class);
	    	asyncMgr.syncAsyncJobExecution(job.getId(), "AttachISO", param.getVmId());
	    	
	    	// always true if it does not have sync-source
	    	return true;
		} else {
	    	AttachISOParam param = gson.fromJson(job.getCmdInfo(), AttachISOParam.class);
	    	
	    	long vmId = param.getVmId();
	    	long userId = param.getUserId();
	    	long isoId = param.getIsoId();
	    	boolean attach = param.isAttach();
	    	
	    	// Build the success/failure messages
	    	String successMsg;
	    	String failureMsg;
	    	
	    	if (attach) {
	    		successMsg = "Successfully inserted CD.";
	    		failureMsg = "Failed to insert CD. Please make sure an existing CD is not currently in use.";
	    	} else {
	    		successMsg = "Successfully ejected CD.";
	    		failureMsg = "Failed to eject CD. Please make sure an existing CD is not currently in use.";
	    	}

	    	try {	
	    		boolean result = asyncMgr.getExecutorContext().getManagementServer().attachISOToVM(vmId, userId, isoId, attach);
				if (result)
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, successMsg);
				else
					asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, failureMsg);
	    	} catch(Exception e) {
	    		s_logger.warn("Unable to attach ISO: " + e.getMessage(), e);
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
	    	}
	    	
			return true;
		}
	}
	
	public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	public void processDisconnect(VMOperationListener listener, long agentId) {
	}

	public void processTimeout(VMOperationListener listener, long agentId, long seq) {
	}
	
}
