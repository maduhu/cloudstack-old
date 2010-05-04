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

package com.vmops.dc.dao;

import java.util.List;

import javax.ejb.Local;

import com.vmops.dc.PodVlanMapVO;
import com.vmops.utils.db.GenericDaoBase;
import com.vmops.utils.db.SearchBuilder;
import com.vmops.utils.db.SearchCriteria;

@Local(value={PodVlanMapDao.class})
public class PodVlanMapDaoImpl extends GenericDaoBase<PodVlanMapVO, Long> implements PodVlanMapDao {
    
	protected SearchBuilder<PodVlanMapVO> PodSearch;
	protected SearchBuilder<PodVlanMapVO> PodVlanSearch;
	
	@Override
	public List<PodVlanMapVO> listPodVlanMaps(long podId) {
		SearchCriteria sc = PodSearch.create();
    	sc.setParameters("podId", podId);
    	return listBy(sc);
	}
	
	@Override
	public PodVlanMapVO findPodVlanMap(long podId, long vlanDbId) {
		SearchCriteria sc = PodVlanSearch.create();
		sc.setParameters("podId", podId);
		sc.setParameters("vlanDbId", vlanDbId);
		return findOneBy(sc);
	}
	
    public PodVlanMapDaoImpl() {
    	PodSearch = createSearchBuilder();
    	PodSearch.and("podId", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.done();
        
        PodVlanSearch = createSearchBuilder();
        PodVlanSearch.and("podId", PodVlanSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodVlanSearch.and("vlanDbId", PodVlanSearch.entity().getVlanDbId(), SearchCriteria.Op.EQ);
        PodVlanSearch.done();
    }
    
}
