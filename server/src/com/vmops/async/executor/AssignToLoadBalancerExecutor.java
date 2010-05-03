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
import com.vmops.async.AsyncJobManager;
import com.vmops.async.AsyncJobVO;
import com.vmops.async.BaseAsyncJobExecutor;
import com.vmops.serializer.GsonHelper;

public class AssignToLoadBalancerExecutor extends BaseAsyncJobExecutor {
    public static final Logger s_logger = Logger.getLogger(AssignToLoadBalancerExecutor.class.getName());

    @Override
    public boolean execute() {
        if (getSyncSource() == null) {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            LoadBalancerParam param = gson.fromJson(job.getCmdInfo(), LoadBalancerParam.class);
            asyncMgr.syncAsyncJobExecution(job.getId(), "Router", param.getDomainRouterId());

            // always true if it does not have sync-source
            return true;
        } else {
            Gson gson = GsonHelper.getBuilder().create();
            AsyncJobManager asyncMgr = getAsyncJobMgr();
            AsyncJobVO job = getJob();

            LoadBalancerParam param = gson.fromJson(job.getCmdInfo(), LoadBalancerParam.class);
            return asyncMgr.getExecutorContext().getNetworkMgr().executeAssignToLoadBalancer(this, param);
        }
    }
}
