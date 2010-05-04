<<<<<<< .mine
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

=======
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

>>>>>>> .r7854
package com.vmops.async.executor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.vmops.agent.api.Answer;
import com.vmops.api.BaseCmd;
import com.vmops.async.AsyncInstanceCreateStatus;
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobResult;
import com.vmops.async.AsyncJobVO;
import com.vmops.async.BaseAsyncJobExecutor;
import com.vmops.async.executor.ListStatisticsParam.StatisticsType;
import com.vmops.async.executor.VolumeOperationParam.VolumeOp;
import com.vmops.serializer.GsonHelper;
import com.vmops.server.ManagementServer;
import com.vmops.storage.VolumeVO;
import com.vmops.vm.UserVm;
import com.vmops.vm.VmStats;
import com.vmops.exception.InternalErrorException;
import com.vmops.host.HostStats;


public class ListStatisticsExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(ListStatisticsExecutor.class.getName());

	public boolean execute() {
    	Gson gson = GsonHelper.getBuilder().create();
    	AsyncJobManager asyncMgr = getAsyncJobMgr();
    	AsyncJobVO job = getJob();
    	
    	ListStatisticsParam param = gson.fromJson(job.getCmdInfo(), ListStatisticsParam.class);
    	
    	try {
    		ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
    		StatisticsType type = param.getType();
    		
    		if (type == StatisticsType.UserVm) {
    			List<VmStats> vmStatsList = managementServer.listVirtualMachineStatistics(param.getVmIds());
            	asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeListVmStatsResultObject(vmStatsList, param));
    		} else if (type == StatisticsType.Host) {
    			List<HostStats> hostStatsList = managementServer.listHostStatistics(param.getHostIds());
            	asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, composeListHostStatsResultObject(hostStatsList, param));
    		} else {
    			throw new Exception("Invalid List Statistics type. Valid types are: UserVm, Host.");
    		}
    					
		} catch (InternalErrorException e) {
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		} catch (Exception e) {
			s_logger.warn("Unhandled Exception executing list statistics for type: " + param.getType(), e);
			asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
		}
		
    	return true;
	}
    
	public void processAnswer(VolumeOperationListener listener, long agentId, long seq, Answer answer) {
	}
	
	public void processDisconnect(VolumeOperationListener listener, long agentId) {
	}

	public void processTimeout(VolumeOperationListener listener, long agentId, long seq) {
	}
	
	protected List<ListVmStatsResultObject> composeListVmStatsResultObject(List<VmStats> vmStatsList, ListStatisticsParam param) {
		List<ListVmStatsResultObject> resultObjects = new ArrayList<ListVmStatsResultObject>();
		
		for (VmStats vmStats : vmStatsList) {
			ListVmStatsResultObject resultObject = new ListVmStatsResultObject();
			resultObject.setVCPUUtilisation(vmStats.getVCPUUtilisation());
			resultObject.setDiskReadKBs(vmStats.getDiskReadKBs());
			resultObject.setDiskWriteKBs(vmStats.getDiskWriteKBs());
			resultObject.setNetworkReadKBs(vmStats.getNetworkReadKBs());
			resultObject.setNetworkWriteKBs(vmStats.getNetworkWriteKBs());
			resultObjects.add(resultObject);
		}
		
		return resultObjects;
	}

	private List<ListHostStatsResultObject> composeListHostStatsResultObject(List<HostStats> hostStatsList, ListStatisticsParam param) {
        List<ListHostStatsResultObject> resultObjects = new ArrayList<ListHostStatsResultObject>();
        
        for (HostStats hostStats : hostStatsList) {
        	ListHostStatsResultObject resultObject = new ListHostStatsResultObject();
        	resultObject.setCpuUtilization(hostStats.getCpuUtilization());
        	resultObject.setUsedMemory(hostStats.getUsedMemory());
        	resultObject.setFreeMemory(hostStats.getFreeMemory());
        	resultObject.setTotalMemory(hostStats.getTotalMemory());
        	resultObject.setPublicNetworkReadKBs(hostStats.getPublicNetworkReadKBs());
        	resultObject.setPublicNetworkWriteKBs(hostStats.getPublicNetworkWriteKBs());
        	resultObjects.add(resultObject);
        }

        return resultObjects;
    }
}
