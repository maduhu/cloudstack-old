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
import com.vmops.network.SecurityGroupVO;
import com.vmops.user.Account;
import com.vmops.utils.Pair;

public class CreateSecurityGroupCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSecurityGroupCmd.class.getName());

    private static final String s_name = "createsecuritygroupresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        //s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DESCRIPTION, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        //Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
        String description = (String)params.get(BaseCmd.Properties.DESCRIPTION.getName());
        Long accountId = null;

        if (account != null) {
            if (isAdmin(account.getType())) {
                if (domainId != null) {
                    if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create security group in domain " + domainId + ", permission denied.");
                    }
                } else {
                    // the admin must be creating the security group
                    if (account != null) {
                        accountId = account.getId();
                        domainId = account.getDomainId();
                    }
                }
            } else {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            if ((accountName != null) && (domainId != null)) {
                Account userAccount = getManagementServer().findActiveAccount(accountName, domainId);
                if (userAccount != null) {
                    accountId = userAccount.getId();
                } else {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "could not find account " + accountName + " in domain " + domainId);
                }
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create security group, no account specified.");
        }

        boolean isNameInUse = getManagementServer().isSecurityGroupNameInUse(domainId, accountId, name);

        if (isNameInUse) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to create security group, a group with name " + name + " already exisits.");
        }

        SecurityGroupVO securityGroup = getManagementServer().createSecurityGroup(name, description, domainId, accountId);

        List<Pair<String, Object>> embeddedObject = new ArrayList<Pair<String, Object>>();
        
        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), securityGroup.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), securityGroup.getName()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), securityGroup.getDescription()));
        
        Account accountTemp = getManagementServer().findAccountById(securityGroup.getAccountId());
        if (accountTemp != null) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
        }
        embeddedObject.add(new Pair<String, Object>("securitygroup", new Object[] { returnValues } ));
        return embeddedObject;
    }
}
