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


import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmops.agent.api.Answer;
import com.vmops.api.BaseCmd;
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobResult;
import com.vmops.async.AsyncJobVO;
import com.vmops.exception.InternalErrorException;
import com.vmops.exception.InvalidParameterValueException;
import com.vmops.exception.PermissionDeniedException;
import com.vmops.exception.ResourceAllocationException;
import com.vmops.serializer.GsonHelper;
import com.vmops.server.ManagementServer;
import com.vmops.service.ServiceOfferingVO;
import com.vmops.storage.InsufficientStorageCapacityException;
import com.vmops.storage.VMTemplateVO;
import com.vmops.user.Account;
import com.vmops.vm.UserVm;

public class DeployVMExecutor extends VMOperationExecutor {
    public static final Logger s_logger = Logger.getLogger(DeployVMExecutor.class.getName());

	@Override
    public boolean execute() {
		// currently deploy VM operation will not be sync-ed with any queue, execute it directly
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	
    	DeployVMParam param = gson.fromJson(job.getCmdInfo(), DeployVMParam.class);
    	try {
			UserVm vm = asyncMgr.getExecutorContext().getManagementServer().deployVirtualMachine(
				param.getUserId(), param.getAccountId(), param.getDataCenterId(),
				param.getServiceOfferingId(), param.getDataDiskOfferingId(),
				param.getTemplateId(), param.getrootDiskOfferingId(), param.getDomain(), 
				param.getPassword(), param.getDisplayName(), param.getGroup(), param.getUserData());
			
    		asyncMgr.completeAsyncJob(getJob().getId(),
        		AsyncJobResult.STATUS_SUCCEEDED, 0, composeResultObject(vm, param));
			
		} catch (ResourceAllocationException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to deploy VM: " + e.getMessage());
    		asyncMgr.completeAsyncJob(getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.VM_INSUFFICIENT_CAPACITY, e.getMessage());
			
		} catch (InvalidParameterValueException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to deploy VM: " + e.getMessage());
    		asyncMgr.completeAsyncJob(getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.VM_INVALID_PARAM_ERROR, e.getMessage());
		} catch (InternalErrorException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to deploy VM: " + e.getMessage());
    		asyncMgr.completeAsyncJob(getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch (InsufficientStorageCapacityException e) {
			if(s_logger.isDebugEnabled())
				s_logger.debug("Unable to deploy VM: " + e.getMessage());
    		asyncMgr.completeAsyncJob(getJob().getId(),
        		AsyncJobResult.STATUS_FAILED, BaseCmd.VM_INSUFFICIENT_CAPACITY, e.getMessage());
        } catch (PermissionDeniedException e) {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Unable to deploy VM: " + e.getMessage());
            asyncMgr.completeAsyncJob(getJob().getId(),
                AsyncJobResult.STATUS_FAILED, BaseCmd.ACCOUNT_ERROR, e.getMessage());
		} catch(Exception e) {
			s_logger.warn("Unable to deploy VM : " + e.getMessage(), e);
    		asyncMgr.completeAsyncJob(getJob().getId(),
            		AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
    	return true;
	}
    
	@Override
    public void processAnswer(VMOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	@Override
    public void processDisconnect(VMOperationListener listener, long agentId) {
	}

	@Override
    public void processTimeout(VMOperationListener listener, long agentId, long seq) {
	}
	
	private DeployVMResultObject composeResultObject(UserVm vm, DeployVMParam param) {
		DeployVMResultObject resultObject = new DeployVMResultObject();
		
		resultObject.setId(vm.getId());
		resultObject.setName(vm.getName());
		resultObject.setCreated(vm.getCreated());
		resultObject.setZoneId(vm.getDataCenterId());
		resultObject.setZoneName(getAsyncJobMgr().getExecutorContext().getManagementServer().findDataCenterById(vm.getDataCenterId()).getName());
		resultObject.setIpAddress(vm.getPrivateIpAddress());
		resultObject.setServiceOfferingId(vm.getServiceOfferingId());
		resultObject.setHaEnabled(vm.isHaEnabled());
		if (vm.getDisplayName() == null || vm.getDisplayName().length() == 0) {
			resultObject.setDisplayName(vm.getName());
		}
		else {
			resultObject.setDisplayName(vm.getDisplayName());
		}
		
		if (vm.getGroup() != null) {
			resultObject.setGroup(vm.getGroup());
		}
		
		if(vm.getState() != null)
			resultObject.setState(vm.getState().toString());
		
		ManagementServer managementServer = getAsyncJobMgr().getExecutorContext().getManagementServer();
        VMTemplateVO template = managementServer.findTemplateById(vm.getTemplateId());
        
        Account acct = managementServer.findAccountById(Long.valueOf(vm.getAccountId()));
        if (acct != null) {
        	resultObject.setAccount(acct.getAccountName());
        	resultObject.setDomainId(acct.getDomainId());
        	resultObject.setDomain(managementServer.findDomainIdById(acct.getDomainId()).getName());
        }
        
        if ( BaseCmd.isAdmin(acct.getType()) && (vm.getHostId() != null)) {
        	resultObject.setHostname(managementServer.getHostBy(vm.getHostId()).getName());
        	resultObject.setHostid(vm.getHostId());
        }
        
        String templateName = "none";
        boolean templatePasswordEnabled = false;
        String templateDisplayText = null;
        
        if (template != null) {
        	templateName = template.getName();
        	templatePasswordEnabled = template.getEnablePassword();
        	templateDisplayText = template.getDisplayText();
        	if (templateDisplayText == null) {
        		templateDisplayText = templateName;
        	}
        }
        
        if (templatePasswordEnabled) {
        	resultObject.setPassword(param.getPassword());
        } else {
        	resultObject.setPassword("");
        }
        
        // ISO Info
        Long isoId = vm.getIsoId();
        if (isoId != null) {
            VMTemplateVO iso = getAsyncJobMgr().getExecutorContext().getManagementServer().findTemplateById(isoId.longValue());
            if (iso != null) {
            	resultObject.setIsoId(isoId.longValue());
            	resultObject.setIsoName(iso.getName());
            }
        }
        
        resultObject.setTemplateId(vm.getTemplateId());
        resultObject.setTemplateName(templateName);
        resultObject.setTemplateDisplayText(templateDisplayText);
        resultObject.setPasswordEnabled(templatePasswordEnabled);
        
        ServiceOfferingVO offering = managementServer.findServiceOfferingById(vm.getServiceOfferingId());
        resultObject.setServiceOfferingId(vm.getServiceOfferingId());
        resultObject.setServiceOfferingName(offering.getName());

        resultObject.setCpuNumber(String.valueOf(offering.getCpu()));
        resultObject.setCpuSpeed(String.valueOf(offering.getSpeed()));
        resultObject.setMemory(String.valueOf(offering.getRamSize()));
        
		return resultObject;
	}
}
