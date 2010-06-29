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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.utils.Pair;

public class CreateStoragePoolCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateStoragePoolCmd.class.getName());

    private static final String s_name = "createstoragepoolresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.URL, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.TRUE));

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
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
        String poolName = (String)params.get(BaseCmd.Properties.NAME.getName());
        String storageUri = (String)params.get(BaseCmd.Properties.URL.getName());
        Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());

        
        //verify input parameters
    	DataCenterVO zone = getManagementServer().findDataCenterById(zoneId);
    	if (zone == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find zone by id " + zoneId);
    	}
    	
    	try {
    		URI uri = new URI(storageUri);
    		if (uri.getScheme() == null)
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "scheme is null " + storageUri + ", add nfs:// as a prefix");
    		else if (uri.getScheme().equalsIgnoreCase("nfs")) {
    			String uriHost = uri.getHost();
    			String uriPath = uri.getPath();
    			if (uriHost == null || uriPath == null || uriHost.trim().isEmpty() || uriPath.trim().isEmpty()) {
    				throw new ServerApiException(BaseCmd.PARAM_ERROR, "host or path is null, should be nfs://hostname/path");
    			}
    		}
    	} catch (URISyntaxException e) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, storageUri + " is not a valid uri");
    	}

        StoragePoolVO storagePool = null;
		try {
			storagePool = getManagementServer().addPool(zoneId, podId, poolName, storageUri);
		} catch (ResourceInUseException e) {
    		throw new ServerApiException(BaseCmd.STORAGE_RESOURCE_IN_USE, e.getMessage());
		} catch (URISyntaxException e) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, storageUri + " is not a valid uri");
		} catch (IllegalArgumentException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, e.getMessage());
		} catch (UnknownHostException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, e.getMessage());
		} catch (ResourceAllocationException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR,e.getMessage());
		} 
 
        if (storagePool == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create storage pool");
        }
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), Long.toString(storagePool.getId())));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), storagePool.getDataCenterId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().getDataCenterBy(storagePool.getDataCenterId()).getName()));        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), storagePool.getPodId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), getManagementServer().getPodBy(storagePool.getPodId()).getName()));        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), storagePool.getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), storagePool.getHostAddress()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.PATH.getName(), storagePool.getPath()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(storagePool.getCreated())));
        
        if (storagePool.getPoolType() != null) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TYPE.getName(), storagePool.getPoolType().toString()));
        }
        
        StorageStats stats = getManagementServer().getStoragePoolStatistics(storagePool.getId());
        long capacity = storagePool.getCapacityBytes();
        long available = storagePool.getAvailableBytes() ;
        long used = capacity - available;

        if (stats != null) {
       	 used = stats.getByteUsed();
       	 available = capacity - used;
        }

        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_TOTAL.getName(), Long.valueOf(storagePool.getCapacityBytes()).toString()));
        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISK_SIZE_ALLOCATED.getName(), Long.valueOf(used).toString()));

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        embeddedObject.add(new Pair<String, Object>("storagepool", new Object[] { returnValues } ));
        return embeddedObject;
    }
}
