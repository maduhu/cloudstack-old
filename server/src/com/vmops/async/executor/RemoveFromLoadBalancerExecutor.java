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
import com.vmops.api.BaseCmd;
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobResult;
import com.vmops.async.AsyncJobVO;
import com.vmops.async.BaseAsyncJobExecutor;
import com.vmops.exception.InvalidParameterValueException;
import com.vmops.serializer.GsonHelper;
import com.vmops.server.ManagementServer;

public class RemoveFromLoadBalancerExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(RemoveFromLoadBalancerExecutor.class.getName());

    @Override
    public boolean execute() {
        Gson gson = GsonHelper.getBuilder().create();
        AsyncJobManager asyncMgr = getAsyncJobMgr();
        AsyncJobVO job = getJob();
        LoadBalancerParam param = gson.fromJson(job.getCmdInfo(), LoadBalancerParam.class);

        if (getSyncSource() == null) {
            asyncMgr.syncAsyncJobExecution(job.getId(), "Router", param.getLoadBalancerId());

            // always true if it does not have sync-source
            return true;
        } else {
            ManagementServer managementServer = asyncMgr.getExecutorContext().getManagementServer();
            try {
                boolean result = managementServer.removeFromLoadBalancer(param.getUserId().longValue(), param.getLoadBalancerId().longValue(), param.getInstanceIdList());
                if (result) {
                    asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, Boolean.valueOf(result).toString());
                } else {
                    asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, Boolean.valueOf(result).toString());
                }
            } catch(InvalidParameterValueException ex) {
                if (s_logger.isInfoEnabled()) {
                    s_logger.info("Unable to remove from load balancer : " + ex.getMessage());
                }
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, ex.getMessage());
            } catch(Exception e) {
                s_logger.warn("Unable to remove from load balancer : " + e.getMessage(), e);
                asyncMgr.completeAsyncJob(getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
            }
            return true;
        }
    }
}
