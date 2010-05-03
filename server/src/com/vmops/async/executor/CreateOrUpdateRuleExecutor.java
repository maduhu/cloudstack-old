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
import com.vmops.api.BaseCmd;
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobResult;
import com.vmops.async.AsyncJobVO;
import com.vmops.async.BaseAsyncJobExecutor;
import com.vmops.exception.InternalErrorException;
import com.vmops.exception.InvalidParameterValueException;
import com.vmops.exception.NetworkRuleConflictException;
import com.vmops.exception.PermissionDeniedException;
import com.vmops.network.NetworkRuleConfigVO;
import com.vmops.serializer.GsonHelper;
import com.vmops.server.ManagementServer;

public class CreateOrUpdateRuleExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(CreateOrUpdateRuleExecutor.class.getName());
	
	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    	CreateOrUpdateRuleParam param = gson.fromJson(job.getCmdInfo(), CreateOrUpdateRuleParam.class);
    	
    	try {
    		if(s_logger.isDebugEnabled())
    			s_logger.debug("Executing createOrUpdateRule, uid: " + job.getUserId() + ", security gid: " + param.getSecurityGroupId()
    				+ ", addr: " + param.getAddress() + ", port: " + param.getPort() + ", private IP: " + param.getPrivateIpAddress()
    				+ ", private port: " + param.getPrivatePort() + ", protocol: " + param.getProtocol() + ", algorithm: " + param.getAlgorithm());
    		
    		NetworkRuleConfigVO rule = managementServer.createOrUpdateRule(job.getUserId(), param.getSecurityGroupId(), 
				param.getAddress(), param.getPort(), param.getPrivateIpAddress(), param.getPrivatePort(),
				param.getProtocol(), param.getAlgorithm());
			
    		if(rule != null) {
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("createOrUpdateRule executed successfully, complete async-execution");
        					
				asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, 
					composeResultObject(managementServer, param, rule));
				
    		} else {
    			s_logger.warn("createOrUpdateRule execution failed: null rule object is returned, complete async-execution");
    			
				asyncMgr.completeAsyncJob(getJob().getId(), 
		    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, "operation failed");
    		}
		} catch (InvalidParameterValueException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to creat or update rule: " + e.getMessage());
			asyncMgr.completeAsyncJob(getJob().getId(), 
	    		AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
		} catch (PermissionDeniedException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to creat or update rule: " + e.getMessage());
			asyncMgr.completeAsyncJob(getJob().getId(), 
	    		AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, e.getMessage());
		} catch (NetworkRuleConflictException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to creat or update rule: " + e.getMessage());
			if(param.isForwarding())
				asyncMgr.completeAsyncJob(getJob().getId(),
					AsyncJobResult.STATUS_FAILED, BaseCmd.NET_CONFLICT_IPFW_RULE_ERROR, e.getMessage());
			else
				asyncMgr.completeAsyncJob(getJob().getId(),
					AsyncJobResult.STATUS_FAILED, BaseCmd.NET_CONFLICT_LB_RULE_ERROR, e.getMessage());
		} catch (InternalErrorException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to creat or update rule: " + e.getMessage());
			asyncMgr.completeAsyncJob(getJob().getId(), 
	    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch(Exception e) {
			s_logger.warn("Unable to creat or update rule: " + e.getMessage(), e);
			asyncMgr.completeAsyncJob(getJob().getId(), 
	    		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
		return true;
	}
	
	private CreateOrUpdateRuleResultObject composeResultObject(ManagementServer managementServer,
		CreateOrUpdateRuleParam param, NetworkRuleConfigVO rule) {
		
		CreateOrUpdateRuleResultObject resultObject = new CreateOrUpdateRuleResultObject();
		resultObject.setRuleId(rule.getId());
		resultObject.setPublicIp(param.getAddress());
		resultObject.setPublicPort(Integer.valueOf(param.getPort()));
		resultObject.setPrivateIp(param.getPrivateIpAddress());
		resultObject.setPrivatePort(Integer.valueOf(param.getPrivatePort()));
		resultObject.setEnabled(true);
		resultObject.setAlgorithm(param.getAlgorithm());
		resultObject.setProtocol(param.getProtocol());
		resultObject.setSecurityGroupId(param.getSecurityGroupId());
		return resultObject;
	}
}
