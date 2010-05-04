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

package com.vmops.domain.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.Local;

import com.vmops.domain.DomainVO;
import com.vmops.utils.db.GenericDaoBase;
import com.vmops.utils.db.GlobalLock;
import com.vmops.utils.db.SearchBuilder;
import com.vmops.utils.db.SearchCriteria;
import com.vmops.utils.db.Transaction;

@Local(value={DomainDao.class})
public class DomainDaoImpl extends GenericDaoBase<DomainVO, Long> implements DomainDao {
	protected SearchBuilder<DomainVO> DomainNameLikeSearch;
	protected SearchBuilder<DomainVO> ParentDomainNameLikeSearch;
	protected SearchBuilder<DomainVO> DomainPairSearch;
	
	public DomainDaoImpl () {
		DomainNameLikeSearch = createSearchBuilder();
		DomainNameLikeSearch.and("name", DomainNameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
		DomainNameLikeSearch.done();
		
		ParentDomainNameLikeSearch = createSearchBuilder();
		ParentDomainNameLikeSearch.and("name", ParentDomainNameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
		ParentDomainNameLikeSearch.and("parent", ParentDomainNameLikeSearch.entity().getName(), SearchCriteria.Op.EQ);
		ParentDomainNameLikeSearch.done();

		DomainPairSearch = createSearchBuilder();
		DomainPairSearch.and("id", DomainPairSearch.entity().getId(), SearchCriteria.Op.IN);
		DomainPairSearch.done();
	}
	
	public List<DomainVO> findDomainsLike(String domain) {
        SearchCriteria sc = DomainNameLikeSearch.create();
        sc.setParameters("name", "%" + domain + "%");
        return listActiveBy(sc);
    }
	
    public List<DomainVO> findDomainsLike(Long parentId, String domain) {
    	if(parentId == null)
    		return findDomainsLike(domain);
    		
        SearchCriteria sc = ParentDomainNameLikeSearch.create();
        sc.setParameters("name", "%" + domain + "%");
        sc.setParameters("parent", parentId);
        return listActiveBy(sc);
    }
	
    public void update(Long id, String domainName) {
        DomainVO ub = createForUpdate();
        ub.setName(domainName);
        update(id, ub);
    }
    
    private static String allocPath(DomainVO parentDomain) {
    	long nextChildSeq = parentDomain.getNextChildSeq();
    	String path = parentDomain.getPath();
    	if(path.length() == 0) {
    		return String.valueOf(nextChildSeq) + "/";
    	} else {
    		return path + String.valueOf(nextChildSeq) + "/";
    	}
    }

    @Override
    public synchronized DomainVO create(DomainVO domain) {
    	
    	long parent = DomainVO.ROOT_DOMAIN;
    	if(domain.getParent() != null && domain.getParent().longValue() >= DomainVO.ROOT_DOMAIN) {
    		parent = domain.getParent().longValue();
    	}
    	
    	DomainVO parentDomain = findById(parent);
    	if(parentDomain == null) {
            s_logger.error("Unable to load parent domain: " + parent);
    		return null;
    	}
    	
        GlobalLock lock = GlobalLock.getInternLock("lock.domain." + parent);
        if(!lock.lock(3600)) {		
        	// wait up to 1 hour, if it comes up to here, something is wrong
            s_logger.error("Unable to lock parent domain: " + parent);
    		return null;
        }
    	
        Transaction txn = Transaction.currentTxn();
    	try {
    		txn.start();
    		
            domain.setPath(allocPath(parentDomain));
            domain.setLevel(parentDomain.getLevel() + 1);
            
            parentDomain.setNextChildSeq(parentDomain.getNextChildSeq() + 1);
            parentDomain.setChildCount(parentDomain.getChildCount() + 1);
            persist(domain);
            update(parentDomain.getId(), parentDomain);
            
    		txn.commit();
    		return domain;
    	} catch(Exception e) {
    		s_logger.error("Unable to create domain due to " + e.getMessage(), e);
    		txn.rollback();
    		return null;
    	} finally {
    		lock.unlock();
    	}
    }

    @Override
    public boolean remove(Long id) {
        // check for any active users / domains assigned to the given domain id and don't remove the domain if there are any
    	if (id != null && id.longValue() == DomainVO.ROOT_DOMAIN.longValue()) {
    		s_logger.error("Can not remove domain " + id + " as it is ROOT domain");
    		return false;
    	}
    	
        DomainVO domain = findById(id);
        if(domain == null) {
        	s_logger.error("Unable to remove domain as domain " + id + " no longer exists");
        	return false;
        }
        
        if(domain.getParent() == null) {
        	s_logger.error("Invalid domain " + id + ", orphan?");
        	return false;
        }
    	
        DomainVO parentDomain = findById(domain.getParent());
        if(parentDomain == null) {
            s_logger.error("Unable to load parent domain: " + domain.getParent());
            return false;
        }
        
        GlobalLock lock = GlobalLock.getInternLock("lock.domain." + domain.getParent());
        if(!lock.lock(Integer.MAX_VALUE)) {
        	s_logger.error("Unable to lock parent domain: " + domain.getParent());
        	return false;
        }
        
        String sql = "SELECT * from account where domain_id = " + id + " and removed is null";
        String sql1 = "SELECT * from domain where parent = " + id + " and removed is null";

        boolean success = false;
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }
            stmt = txn.prepareAutoCloseStatement(sql1);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return false;
            }
            
        	txn.start();
        	parentDomain.setChildCount(parentDomain.getChildCount() - 1);
        	update(parentDomain.getId(), parentDomain);
            success = super.remove(id);
            txn.commit();
        } catch (SQLException ex) {
            success = false;
            s_logger.error("error removing domain: " + id, ex);
            txn.rollback();
        } finally {
        	lock.unlock();
        }
        return success;
    }

    @Override
    public DomainVO findDomainByName(String domainName) {
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
        return findOneActiveBy(sc);
    }
    
    @Override
    public DomainVO findDomainByName(Long parentId, String domainName) {
    	if(parentId == null)
    		return findDomainByName(domainName);
    	
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("name", SearchCriteria.Op.EQ, domainName);
        sc.addAnd("parent", SearchCriteria.Op.EQ, parentId);
        return findOneActiveBy(sc);
    }

    @Override
    public boolean isChildDomain(Long parentId, Long childId) {
        if ((parentId == null) || (childId == null)) {
            return false;
        }

        if (parentId.equals(childId)) {
            return true;
        }

        boolean result = false;
        SearchCriteria sc = DomainPairSearch.create();
        sc.setParameters("id", parentId, childId);

        List<DomainVO> domainPair = listActiveBy(sc);

        if ((domainPair != null) && (domainPair.size() == 2)) {
            DomainVO d1 = domainPair.get(0);
            DomainVO d2 = domainPair.get(1);

            if (d1.getId().equals(parentId)) {
                result = d2.getPath().startsWith(d1.getPath());
            } else {
                result = d1.getPath().startsWith(d2.getPath());
            }
        }
        return result;
    }
}
