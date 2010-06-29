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
import com.cloud.utils.Pair;

public class CreateVlanRangeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVlanRangeCmd.class.getName());

    private static final String s_name = "createvlanrangeresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_VLAN, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.END_VLAN, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
    	Long startVlan = (Long) params.get(BaseCmd.Properties.START_VLAN.getName());
    	Long endVlan = (Long) params.get(BaseCmd.Properties.END_VLAN.getName());
    	
    	
    	//verify input parameters
        if (getManagementServer().findDataCenterById(zoneId) == null) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, "Zone with id " + zoneId + " doesn't exist in the system");
        }
        
        //TBD - waiting for alex to implemenet management server part of code
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        return returnValues;
    }
}
