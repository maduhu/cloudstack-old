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
import com.vmops.async.AsyncJobVO;
import com.vmops.network.NetworkRuleConfigVO;
import com.vmops.server.Criteria;
import com.vmops.user.Account;
import com.vmops.utils.Pair;

public class ListNetworkRulesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListNetworkRulesCmd.class.getName());

    private static final String s_name = "listnetworkrulesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SECURITY_GROUP_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
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
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long groupId = (Long)params.get(BaseCmd.Properties.SECURITY_GROUP_ID.getName());

        // FIXME:  validate that the domain admin can list network rules for the group in question
        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Invalid domain id (" + domainId + ") given, unable to list network rules.");
                }
                if (accountName != null) {
                    Account userAcct = getManagementServer().findActiveAccount(accountName, domainId);
                    if (userAcct != null) {
                        accountId = userAcct.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            }
        } else {
            accountId = account.getId();
        }

        Criteria c = new Criteria("id", Boolean.TRUE, null, null);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.GROUPID, groupId);
        c.addCriteria(Criteria.ACCOUNTID, accountId);
        
        List<NetworkRuleConfigVO> netRules = getManagementServer().searchForNetworkRules(c);

        if (netRules == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error searching for network rules for security group " + groupId);
        }

        List<Pair<String, Object>> netRulesTags = new ArrayList<Pair<String, Object>>();
        Object[] netRuleTag = new Object[netRules.size()];
        int i = 0;
        for (NetworkRuleConfigVO netRule : netRules) {
            List<Pair<String, Object>> netRuleData = new ArrayList<Pair<String, Object>>();
            netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), netRule.getId().toString()));
            netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.SECURITY_GROUP_ID.getName(), Long.valueOf(netRule.getSecurityGroupId()).toString()));
            netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_PORT.getName(), netRule.getPublicPort()));
            netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_PORT.getName(), netRule.getPrivatePort()));
            netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.PROTOCOL.getName(), netRule.getProtocol()));

            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("network_rule_config", netRule.getId());
            if(asyncJob != null) {
            	netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
            	netRuleData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
            } 
            netRuleTag[i++] = netRuleData;
        }
        Pair<String, Object> eventTag = new Pair<String, Object>("networkrule", netRuleTag);
        netRulesTags.add(eventTag);
        return netRulesTags;
    }
}
