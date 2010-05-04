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

package com.vmops.api.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vmops.api.BaseCmd;
import com.vmops.api.ServerApiException;
import com.vmops.user.Account;
import com.vmops.utils.Pair;
import com.vmops.vm.UserVmVO;

public class DetachIsoCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DetachIsoCmd.class.getName());

    private static final String s_name = "detachisoresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VIRTUAL_MACHINE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long) params.get(BaseCmd.Properties.USER_ID.getName());
        Long vmId = (Long) params.get(BaseCmd.Properties.VIRTUAL_MACHINE_ID.getName());

        // Verify input parameters
        UserVmVO vmInstanceCheck = getManagementServer().findUserVMInstanceById(vmId.longValue());
        if (vmInstanceCheck == null) {
            throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Unable to find a virtual machine with id " + vmId);
        }

        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId().longValue() != vmInstanceCheck.getAccountId()) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to detach ISO from virtual machine " + vmInstanceCheck.getName() + " for this account");
                }
            } else if (!getManagementServer().isChildDomain(account.getDomainId(), vmInstanceCheck.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to detach ISO from virtual machine " + vmInstanceCheck.getName() + ", permission denied.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null)
            userId = new Long(1);

        try {
            long jobId = getManagementServer().detachISOFromVMAsync(vmId.longValue(), userId);

            if (jobId == 0) {
                s_logger.warn("Unable to schedule async-job for AttachIsoCmd");
            } else {
                if(s_logger.isDebugEnabled())
                    s_logger.debug("AttachIsoCmd has been accepted, job id: " + jobId);
            }
            
            List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            
            return returnValues;
        } catch (ServerApiException apiEx) {
            s_logger.error("Exception detaching ISO", apiEx);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to detach ISO: " + apiEx.getDescription());
        } catch (Exception ex) {
            s_logger.error("Exception detaching ISO", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to detach ISO: " + ex.getMessage());
        }

    }
}
