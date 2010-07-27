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

import org.apache.log4j.Logger;

import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskOfferingVO.Type;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value={DiskOfferingDao.class})
public class DiskOfferingDaoImpl extends GenericDaoBase<DiskOfferingVO, Long> implements DiskOfferingDao {
    private static final Logger s_logger = Logger.getLogger(DiskOfferingDaoImpl.class);

    private final SearchBuilder<DiskOfferingVO> DomainIdSearch;
    private final Attribute _typeAttr;

    protected DiskOfferingDaoImpl() {
        DomainIdSearch  = createSearchBuilder();
        DomainIdSearch.and("domainId", DomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        DomainIdSearch.done();
        
        _typeAttr = _allAttributes.get("type");
    }

    @Override
    public List<DiskOfferingVO> listByDomainId(long domainId) {
        SearchCriteria sc = DomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        // FIXME:  this should not be exact match, but instead should find all available disk offerings from parent domains
        return listActiveBy(sc);
    }
    
    @Override
    protected List<DiskOfferingVO> searchAll(SearchCriteria sc, final Filter filter, final Boolean lock, final boolean cache) {
        sc.addAnd(_typeAttr, Op.EQ, Type.Disk);
        return super.searchAll(sc, filter, lock, cache);
    }
    
    @Override
    public List<Object[]> searchAll(SearchCriteria sc, final Filter filter) {
        sc.addAnd(_typeAttr, Op.EQ, Type.Disk);
        return super.searchAll(sc, filter);
    }
    
    @Override
    protected List<DiskOfferingVO> executeList(final String sql, final Object... params) {
        StringBuilder builder = new StringBuilder(sql);
        int index = builder.indexOf("WHERE");
        if (index == -1) {
            builder.append(" WHERE type=?");
        } else {
            builder.insert(index + 6, "type=? ");
        }
        
        return super.executeList(sql, Type.Disk, params);
    }
}
