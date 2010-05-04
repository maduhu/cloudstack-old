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
import com.vmops.configuration.ResourceLimitVO;
import com.vmops.configuration.ResourceCount.ResourceType;
import com.vmops.domain.DomainVO;
import com.vmops.server.Criteria;
import com.vmops.user.Account;
import com.vmops.user.AccountVO;
import com.vmops.utils.Pair;

public class ListResourceLimitsCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListResourceLimitsCmd.class.getName());

    private static final String s_name = "listresourcelimitsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.RESOURCE_TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
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
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Integer type = (Integer) params.get(BaseCmd.Properties.RESOURCE_TYPE.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Long accountId = null;
        
        // If account is specified, you must be an admin
        if (accountName != null) {
        	if (domainId == null) {
        		throw new ServerApiException(BaseCmd.PARAM_ERROR, "You must specify domain Id for the account: " + accountName);
        	}
        	Account userAccount = getManagementServer().findAccountByName(accountName, domainId);
        	if (userAccount == null) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
            }
        	// Now verify the admin can see this account
        	// if account is null, it must be accessing from 8096
        	if (account != null) {
        		if (account.getType() == Account.ACCOUNT_TYPE_NORMAL) {
        			throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "You do not have permission to access this account: " + accountName);
        		} else if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || account.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN){
        			// If this is a domain admin, we make sure the domain admin and the account belong in the same domain or
        			// the account's domain is a child domain of the parent
        			if (account.getDomainId() != userAccount.getDomainId() && !getManagementServer().isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
        				throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "You do not have permission as domain-admin to access limits for this account: " + accountName);
        			}
        		}
        	}
        	accountId = userAccount.getId();
        	domainId = DomainVO.ROOT_DOMAIN;
        } else if (domainId != null) {
        	if (!domainId.equals(DomainVO.ROOT_DOMAIN)) {
        		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Only ROOT domain limits can be retrieved right now");
        	}
        } else {
        	if (account != null) {
        		accountId = account.getId();
        	} else {
        		accountId = AccountVO.ACCOUNT_ID_SYSTEM;
        	}
        	domainId = DomainVO.ROOT_DOMAIN;
        }
        
        // Map resource type
        ResourceType resourceType = null;
        try {
        	if (type != null) {
        		resourceType = ResourceType.values()[type];
        	}
        } catch (ArrayIndexOutOfBoundsException e) {
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid resource type.");
        }
        
        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        
        Criteria c = new Criteria("id", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));
    	c.addCriteria(Criteria.ACCOUNTID, accountId);
    	c.addCriteria(Criteria.DOMAINID, domainId);
    	c.addCriteria(Criteria.TYPE, resourceType);
    	
    	List<ResourceLimitVO> limits = null;
        try {
            limits = getManagementServer().searchForLimits(c);
        } catch (Exception ex) {
            s_logger.error("Exception listing limits", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to list resource limits due to exception: " + ex.getMessage());
        }

        List<Pair<String, Object>> limitTags = new ArrayList<Pair<String, Object>>();
        Object[] lTag = new Object[limits.size()];
        int i = 0;
        for (ResourceLimitVO limit : limits) {
            List<Pair<String, Object>> limitData = new ArrayList<Pair<String, Object>>();
            //limitData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), limit.getId()));
            if (limit.getDomainId() != null)
            {
            	limitData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), limit.getDomainId().toString()));
            	limitData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(limit.getDomainId()).getName()));
            }
            	
            if (limit.getAccountId() != null) {
        		Account accountTemp = getManagementServer().findAccountById(limit.getAccountId());
                if (accountTemp != null) {
                	limitData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
                	limitData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
                	limitData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
                }
        	}
            	
            ResourceType limitType = limit.getType();
            ResourceType[] possibleLimitTypes = ResourceType.values();
            int limitNum = 0;
            for (int j = 0; j < possibleLimitTypes.length; j++) {
            	if (possibleLimitTypes[j].equals(limitType)) {
            		limitNum = j;
            		break;
            	}
            }
        	limitData.add(new Pair<String, Object>(BaseCmd.Properties.RESOURCE_TYPE.getName(), limitNum));
            limitData.add(new Pair<String, Object>(BaseCmd.Properties.MAX.getName(), limit.getMax()));

            lTag[i++] = limitData;
        }
        Pair<String, Object> limitTag = new Pair<String, Object>("resourcelimit", lTag);
        limitTags.add(limitTag);
        return limitTags;
    }
}
