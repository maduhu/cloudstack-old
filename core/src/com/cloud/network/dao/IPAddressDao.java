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

package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.IPAddressVO;
import com.cloud.utils.db.GenericDao;

public interface IPAddressDao extends GenericDao<IPAddressVO, String> {
	
    /**
     * @param accountId account id
     * @param domainId id of the account's domain
     * @param dcId data center id
     * @param sourceNat is it for source nat?
     * @return public ip address
     */
	public String assignIpAddress(long accountId, long domainId, long vlanDbId, boolean sourceNat);
	
	public void unassignIpAddress(String ipAddress);
	
	public List<IPAddressVO> listByAccountId(long accountId);
	
    public List<IPAddressVO> listByAccountDcId(long accountId, long dcId, boolean sourceNat);
	
	public List<IPAddressVO> listByAccountDcId(long accountId, long dcId);
	
	public List<IPAddressVO> listByDcId(long dcId);
	
	public List<IPAddressVO> listByVlanDbId(long vlanDbId);

	public List<IPAddressVO> listByAccountIdSourceNat(long accountId, boolean sourceNat);
	
	public List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress);
	
	public int countIPs(long dcId, long vlanDbId, long accountId, boolean onlyCountAllocated);
	
	public boolean mark(long dcId, String ip);
}
