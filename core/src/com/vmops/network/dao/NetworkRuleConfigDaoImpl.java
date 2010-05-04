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

package com.vmops.network.dao;

import java.util.List;

import javax.ejb.Local;

import com.vmops.network.NetworkRuleConfigVO;
import com.vmops.utils.db.GenericDaoBase;
import com.vmops.utils.db.SearchBuilder;
import com.vmops.utils.db.SearchCriteria;

@Local(value={NetworkRuleConfigDao.class})
public class NetworkRuleConfigDaoImpl extends GenericDaoBase<NetworkRuleConfigVO, Long> implements NetworkRuleConfigDao {
    protected SearchBuilder<NetworkRuleConfigVO> SecurityGroupIdSearch;

    protected NetworkRuleConfigDaoImpl() {
        SecurityGroupIdSearch  = createSearchBuilder();
        SecurityGroupIdSearch.and("securityGroupId", SecurityGroupIdSearch.entity().getSecurityGroupId(), SearchCriteria.Op.EQ);
        SecurityGroupIdSearch.done();
    }

    public List<NetworkRuleConfigVO> listBySecurityGroupId(long securityGroupId) {
        SearchCriteria sc = SecurityGroupIdSearch.create();
        sc.setParameters("securityGroupId", securityGroupId);
        return listActiveBy(sc);
    }

    public void deleteBySecurityGroup(long securityGroupId) {
        SearchCriteria sc = SecurityGroupIdSearch.create();
        sc.setParameters("securityGroupId", securityGroupId);
        delete(sc);
    }
}
