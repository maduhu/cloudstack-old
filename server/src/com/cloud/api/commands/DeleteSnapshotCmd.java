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

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class DeleteSnapshotCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSnapshotCmd.class.getName());
    private static final String s_name = "deletesnapshotresponse";
	private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

	static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }
	
	public String getName() {
        return s_name;
    }
	
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }
    
    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long snapshotId = (Long)params.get(BaseCmd.Properties.ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());

        //Verify parameters
        Snapshot snapshotCheck = getManagementServer().findSnapshotById(snapshotId.longValue());
        if (snapshotCheck == null) {
            throw new ServerApiException (BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId);
        }

        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId() != snapshotCheck.getAccountId()) {
                    throw new ServerApiException(BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId + " for this account");
                }
            } else {
                Account snapshotOwner = getManagementServer().findAccountById(snapshotCheck.getAccountId());
                if ((snapshotOwner == null) || !getManagementServer().isChildDomain(account.getDomainId(), snapshotOwner.getDomainId())) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to delete snapshot " + snapshotId + ", permission denied.");
                }
            }
        }

        //If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        try {
            long jobId = getManagementServer().deleteSnapshotAsync(userId, snapshotId);
            if(jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for DeleteSnapshot comamnd");
            } else {
    	        if(s_logger.isDebugEnabled())
    	        	s_logger.debug("DeleteSnapshot command has been accepted, job id: " + jobId);
            }
            
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            
            return returnValues;
            
        } catch (Exception ex) {
        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "internal error deleting snapshot " + ex.getMessage());
        }
    }
}
