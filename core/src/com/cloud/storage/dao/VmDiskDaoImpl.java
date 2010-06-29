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

package com.cloud.storage.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.VmDiskVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={VmDiskDao.class})
public class VmDiskDaoImpl extends GenericDaoBase<VmDiskVO, Long> implements VmDiskDao {
    protected final SearchBuilder<VmDiskVO> InstanceIdSearch;

    @Override
    public List<VmDiskVO> findByInstanceId(Long instanceId) {
        SearchCriteria sc = InstanceIdSearch.create();
        sc.setParameters("instanceId", instanceId);
        return listActiveBy(sc);
    }

    protected VmDiskDaoImpl() {
        InstanceIdSearch = createSearchBuilder();
        InstanceIdSearch.and("instanceId", InstanceIdSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        InstanceIdSearch.done();
    }
}
