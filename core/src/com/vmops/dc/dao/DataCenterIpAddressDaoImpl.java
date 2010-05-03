/**
 *  Copyright (C) 2010 VMOps, Inc.  All rights reserved.
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.vmops.dc.DataCenterIpAddressVO;
import com.vmops.utils.db.GenericDao;
import com.vmops.utils.db.GenericDaoBase;
import com.vmops.utils.db.SearchBuilder;
import com.vmops.utils.db.SearchCriteria;
import com.vmops.utils.db.Transaction;
import com.vmops.utils.exception.VmopsRuntimeException;
import com.vmops.utils.net.NetUtils;

@Local(value={DataCenterIpAddressDaoImpl.class})
public class DataCenterIpAddressDaoImpl extends GenericDaoBase<DataCenterIpAddressVO, Long> implements GenericDao<DataCenterIpAddressVO, Long> {
    
	private static final String COUNT_ALL_PRIVATE_IPS = "SELECT count(*) from `vmops`.`op_dc_ip_address_alloc` where pod_id = ? AND data_center_id = ?";
	private static final String COUNT_ALLOCATED_PRIVATE_IPS = "SELECT count(*) from `vmops`.`op_dc_ip_address_alloc` where pod_id = ? AND data_center_id = ? AND taken is not null";
	
    private final SearchBuilder<DataCenterIpAddressVO> FreeIpSearch;
    private final SearchBuilder<DataCenterIpAddressVO> IpDcSearch;
    private final SearchBuilder<DataCenterIpAddressVO> PodDcSearch;
    private final SearchBuilder<DataCenterIpAddressVO> PodDcIpSearch;
    private final SearchBuilder<DataCenterIpAddressVO> FreePodDcIpSearch;
    
    public DataCenterIpAddressVO takeIpAddress(long dcId, long podId, long instanceId) {
        SearchCriteria sc = FreeIpSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("pod", podId);
        
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            
            DataCenterIpAddressVO  vo = lock(sc, true);
            if (vo == null) {
                txn.rollback();
                return vo;
            }
            vo.setTakenAt(new Date());
            vo.setInstanceId(instanceId);
            update(vo.getId(), vo);
            txn.commit();
            return vo;
        } catch (Exception e) {
            txn.rollback();
            throw new VmopsRuntimeException("Caught Exception ", e);
        }
    }
    
    public boolean deleteIpAddressByPod(long podId) {
        Transaction txn = Transaction.currentTxn();
        try {
            String deleteSql = "DELETE FROM `vmops`.`op_dc_ip_address_alloc` WHERE `pod_id` = ?";
            PreparedStatement stmt = txn.prepareAutoCloseStatement(deleteSql);
            stmt.setLong(1, podId);
            return stmt.execute();
        } catch(Exception e) {
            throw new VmopsRuntimeException("Caught Exception ", e);
        }
    }
    
    public boolean mark(long dcId, long podId, String ip) {
        SearchCriteria sc = FreePodDcIpSearch.create();
        sc.setParameters("podId", podId);
        sc.setParameters("dcId", dcId);
        sc.setParameters("ipAddress", ip);
        
        DataCenterIpAddressVO vo = createForUpdate();
        vo.setTakenAt(new Date());
        
        return update(vo, sc) >= 1;
    }
    
    public void addIpRange(long dcId, long podId, String start, String end) {
        Transaction txn = Transaction.currentTxn();
        String insertSql = "INSERT INTO `vmops`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
        PreparedStatement stmt = null;
        
        long startIP = NetUtils.ip2Long(start);
        long endIP = NetUtils.ip2Long(end);
        
        while (startIP <= endIP) {
            try {
                stmt = txn.prepareAutoCloseStatement(insertSql);
                stmt.setString(1, NetUtils.long2Ip(startIP));
                stmt.setLong(2, dcId);
                stmt.setLong(3, podId);
                stmt.executeUpdate();
                stmt.close();
            } catch (Exception ex) {
                s_logger.warn("Unable to persist " + NetUtils.long2Ip(startIP) + " due to " + ex.getMessage());
            }
            startIP++;
        }
    }
    
    public void releaseIpAddress(String ipAddress, long dcId, Long instanceId) {
    	if (s_logger.isDebugEnabled()) {
    		s_logger.debug("Releasing ip address: " + ipAddress + " data center " + dcId);
    	}
        SearchCriteria sc = IpDcSearch.create();
        sc.setParameters("ip", ipAddress);
        sc.setParameters("dc", dcId);
        sc.setParameters("instance", instanceId);

        DataCenterIpAddressVO vo = createForUpdate();
        
        vo.setTakenAt(null);
        vo.setInstanceId(null);
        update(vo, sc);
    }
    
    protected DataCenterIpAddressDaoImpl() {
    	super();
        FreeIpSearch = createSearchBuilder();
        FreeIpSearch.addAnd("dc", FreeIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreeIpSearch.addAnd("pod", FreeIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        FreeIpSearch.addAnd("taken", FreeIpSearch.entity().getTakenAt(), SearchCriteria.Op.NULL);
        FreeIpSearch.done();
        
        IpDcSearch = createSearchBuilder();
        IpDcSearch.addAnd("ip", IpDcSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        IpDcSearch.addAnd("dc", IpDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        IpDcSearch.addAnd("instance", IpDcSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        IpDcSearch.done();
        
        PodDcSearch = createSearchBuilder();
        PodDcSearch.addAnd("podId", PodDcSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodDcSearch.addAnd("dataCenterId", PodDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        PodDcSearch.done();
        
        PodDcIpSearch = createSearchBuilder();
        PodDcIpSearch.addAnd("dcId", PodDcIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        PodDcIpSearch.addAnd("podId", PodDcIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodDcIpSearch.addAnd("ipAddress", PodDcIpSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        PodDcIpSearch.done();
        
        FreePodDcIpSearch = createSearchBuilder();
        FreePodDcIpSearch.addAnd("dcId", FreePodDcIpSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.addAnd("podId", FreePodDcIpSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.addAnd("ipAddress", FreePodDcIpSearch.entity().getIpAddress(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.addAnd("taken", FreePodDcIpSearch.entity().getTakenAt(), SearchCriteria.Op.EQ);
        FreePodDcIpSearch.done();
    }
    
    public List<DataCenterIpAddressVO> listByPodIdDcId(long podId, long dcId) {
		SearchCriteria sc = PodDcSearch.create();
		sc.setParameters("podId", podId);
		sc.setParameters("dataCenterId", dcId);
		return listBy(sc);
	}
    
    public List<DataCenterIpAddressVO> listByPodIdDcIdIpAddress(long podId, long dcId, String ipAddress) {
    	SearchCriteria sc = PodDcIpSearch.create();
    	sc.setParameters("dcId", dcId);
		sc.setParameters("podId", podId);
		sc.setParameters("ipAddress", ipAddress);
		return listBy(sc);
    }
    
    public int countIPs(long podId, long dcId, boolean onlyCountAllocated) {
		Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		int ipCount = 0;
		try {
			String sql = "";
			if (onlyCountAllocated) sql = COUNT_ALLOCATED_PRIVATE_IPS;
			else sql = COUNT_ALL_PRIVATE_IPS;
			
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, podId);
            pstmt.setLong(2, dcId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) ipCount = rs.getInt(1);
            
        } catch (Exception e) {
            s_logger.warn("Exception searching for routers and proxies", e);
        }
        return ipCount;
	}
}
