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

package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

public interface VlanDao extends GenericDao<VlanVO, Long> {
	
	public VlanVO findByZoneAndVlanId(long zoneId, String vlanId);
	
	public List<VlanVO> findByZone(long zoneId);
	
	public List<VlanVO> listByZoneAndType(long zoneId, Vlan.VlanType vlanType);
	
	public List<VlanVO> listVlansForPod(long podId);
	
	public List<VlanVO> listVlansForPodByType(long podId, Vlan.VlanType vlanType);
	
	public void addToPod(long podId, long vlanDbId);
	
    public Pair<String, VlanVO>  assignIpAddress(long zoneId, long accountId, long domainId, VlanType vlanType, boolean sourceNat);

	List<VlanVO> listVlansForAccountByType(Long zoneId, long accountId, VlanType vlanType);
	
	public Pair<String, VlanVO> assignPodDirectAttachIpAddress(long zoneId, long podId, long accountId, long domainId);
	
	boolean zoneHasDirectAttachUntaggedVlans(long zoneId);

}
