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
package com.vmops.utils.db;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.vmops.utils.Pair;
import com.vmops.utils.Ternary;
import com.vmops.utils.exception.VmopsRuntimeException;
import com.vmops.utils.net.MacAddress;
import com.vmops.utils.time.InaccurateClock;

public class Merovingian {
	private static final Logger s_logger = Logger.getLogger(Merovingian.class);
	
	private static final String ACQUIRE_SQL = "INSERT IGNORE INTO op_lock (op_lock.key, op_lock.mac, op_lock.ip, op_lock.thread) VALUES (?, ?, ?, ?)";
	private static final String INQUIRE_SQL = "SELECT op_lock.ip FROM op_lock WHERE op_lock.key = ?";
	private static final String RELEASE_SQL = "DELETE FROM op_lock WHERE op_lock.key = ?";
	private static final String CLEAR_SQL = "DELETE FROM op_lock WHERE op_lock.mac = ? AND op_lock.ip = ?";
	
	private final static HashMap<String, Pair<Lock, Integer>> s_memLocks = new HashMap<String, Pair<Lock, Integer>>(1027);
	
	private final LinkedHashMap<String, Ternary<Savepoint, Integer, Long>> _locks = new LinkedHashMap<String, Ternary<Savepoint, Integer, Long>>();
	private int _previousIsolation = Connection.TRANSACTION_NONE;
	
	private final static String s_macAddress;
	private final static String s_ipAddress;
	static {
		s_macAddress = MacAddress.getMacAddress().toString(":");
		String address = null;
		try {
			InetAddress addr = InetAddress.getLocalHost();
			address = addr.getHostAddress().toString();
		} catch (UnknownHostException e) {
			address = "127.0.0.1";
		}
		
		s_ipAddress = address;
	}
	
	private final Transaction _txn;
	
	public Merovingian(short dbId) {
		_txn = new Transaction(Merovingian.class.getName(), true, dbId);
	}
	
	public void close() {
		_txn.close(Merovingian.class.getName());
	}
	
	protected Connection getConnection(String key) {
		_txn.takeOver("locking", false);
		
		try {
			Connection conn = _txn.getConnection();
			if (_previousIsolation == Connection.TRANSACTION_NONE) {
				_previousIsolation = conn.getTransactionIsolation();
				conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
				conn.setAutoCommit(false);
			}
			return conn;
		} catch (SQLException e) {
			_txn.close();
			throw new VmopsRuntimeException("Unable to acquire db connection for locking " + key, e);
		}
	}
	
	public boolean acquire(String key, int timeInSeconds) {
		Pair<Lock, Integer> memLock = null;
	    boolean acquiredDbLock = false;
	    boolean acquiredMemLock = false;
	    try {
    	    synchronized(s_memLocks) {
    	        memLock = s_memLocks.get(key);
    	        if (memLock == null) {
    	            Lock l = new ReentrantLock();
    	            memLock = new Pair<Lock, Integer>(l, 0);
    	            s_memLocks.put(key, memLock);
    	        }
    	        
    	        memLock.second(memLock.second() + 1);
    	    }
    	    
    	    if (!memLock.first().tryLock(timeInSeconds, TimeUnit.SECONDS)) {
    	        return false;
    	    }
    	    acquiredMemLock = true;
    	    
    		Ternary<Savepoint, Integer, Long> lock = _locks.get(key);
    		if (lock != null) {
    			lock.second(lock.second() + 1);
    			if (s_logger.isTraceEnabled()) {
    				s_logger.trace("Lock: Reacquiring " + key + " Count: " + lock.second());
    			}
    			acquiredDbLock = true;
    			return true;
    		}
    		
    		if (_locks.size() == 0) {
    			getConnection(key);
    		}
    		
    		PreparedStatement pstmt = null;
    		long startTime = InaccurateClock.getTime();
    		while ((InaccurateClock.getTime() - startTime) < (timeInSeconds * 1000)) {
    			if (isLocked(key)) {
    				try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e) {
    				}
    			} else {
    				acquiredDbLock = doAcquire(key);
    				if (acquiredDbLock) {
    					return true;
    				}
    			}
    		}
    		if (s_logger.isTraceEnabled()) {
    			s_logger.trace("Lock: Timed out on acquiring lock " + key);
    		}
    		return false;
	    } catch (InterruptedException e) {
	        s_logger.debug("Interrupted while trying to acquire " + key);
	        return false;
	    } finally {
	        if (!acquiredMemLock || !acquiredDbLock) {
	            synchronized(s_memLocks) {
	                if (memLock.second(memLock.second() - 1) <= 0) {
	                    s_memLocks.remove(key);
	                }
	            }
            }
	        
            if (acquiredMemLock && !acquiredDbLock) {
                memLock.first().unlock();
            }
	    }
	}
	
	protected boolean doAcquire(String key) {
		PreparedStatement pstmt = null;
		try {
			Savepoint sp = _txn.setSavepoint(key);

			pstmt = _txn.prepareAutoCloseStatement(ACQUIRE_SQL);
			pstmt.setString(1, key);
			pstmt.setString(2, s_macAddress);
			pstmt.setString(3, s_ipAddress);
			pstmt.setString(4, Thread.currentThread().getName());
			long startTime = InaccurateClock.getTime();
			String exceptionMessage = null;
			try {
				int rows = pstmt.executeUpdate();
				if (rows == 1) {
					if (s_logger.isTraceEnabled()) {
						s_logger.trace("Lock: lock acquired for " + key);
					}
					Ternary<Savepoint, Integer, Long> lock = new Ternary<Savepoint, Integer, Long>(sp, 1, InaccurateClock.getTime());
					_locks.put(key, lock);
					return true;
				}
			} catch(SQLException e) {
				s_logger.warn("Lock: Retrying lock " + key + ".  Waited " + (InaccurateClock.getTime() - startTime), e);
			}
			
			_txn.rollback(sp);
			s_logger.trace("Lock: Unable to acquire DB lock " + key);
		} catch (SQLException e) {
			s_logger.warn("Lock: Unable to acquire db connection for locking " + key, e);
		}
		return false;
	}
	
	public boolean isLocked(String key) {
		Connection conn = getConnection(key);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = conn.prepareStatement(INQUIRE_SQL);
			pstmt.setString(1, key);
			rs = pstmt.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			s_logger.warn("SQL exception " + e.getMessage(), e);
			throw new VmopsRuntimeException("SQL Exception on inquiry", e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException e) {
				s_logger.warn("Unexpected SQL exception " + e.getMessage(), e);
			}
			_txn.close();
		}
	}
	
	public void clear() {
		if (_locks.size() == 0) {
			return;
		}
		
		Set<String> keys = new HashSet<String>(_locks.keySet());

		//
		// disable assertion, when assert support is enabled, it throws an exception
		// which eventually eats the following on important messages for diagnostic
		//
		
		// assert (false) : "Who acquired locks but didn't release them? " + keys.toArray(new String[keys.size()]);
		
		for (String key : keys) {
			s_logger.warn("Lock: This is not good guys!  Automatically releasing lock: " + key);
			release(key);
		}
		
		_locks.clear();
	}
	
	public boolean release(String key) {
		boolean lastRef = false;
	    try {
    		assert _locks.size() > 0 : "There are no locks here. Why are you trying to release " + key;
    		if (s_logger.isDebugEnabled() && _locks.keySet().iterator().next().equals(key)) {
    			s_logger.trace("Lock: Releasing out of order for " + key);
    		}
    		
    		Ternary<Savepoint, Integer, Long> lock = _locks.get(key);
    		if (lock == null) {
    			assert false : "Releasing a lock that is not acquired: " + key;
    			return false;
    		}
    		
    		if (lock.second() > 1) {
    			lock.second(lock.second() -1);
    			if (s_logger.isTraceEnabled()) {
    				s_logger.trace("Lock: Releasing " + key + " but not in DB " + lock.second());
    			}
    			return false;
    		}
    		
    		if (s_logger.isTraceEnabled()) {
    			s_logger.trace("Lock: Releasing " + key + " after " + (InaccurateClock.getTime() - lock.third()));
    		}
    		_txn.rollback(lock.first());
    		_locks.remove(key);
    		if (_locks.size() == 0) {
    			closeConnection();
    		}
    		lastRef = true;
    		
	    } finally {
    		synchronized(s_memLocks) {
    		    Pair<Lock, Integer> memLock = s_memLocks.get(key);
    		    memLock.second(memLock.second() - 1);
    		    if (memLock.second() <= 0) {
    		        s_memLocks.remove(key);
    		    }
    		    
    		    if(lastRef)
    		    	memLock.first().unlock();
    		}
	    }
		return true;
	}
	
	public void closeConnection() {
		try {
			Connection conn = _txn.getConnection();
			conn.setTransactionIsolation(_previousIsolation);
			conn.setAutoCommit(true);
			_previousIsolation = Connection.TRANSACTION_NONE;
		} catch (SQLException e) {
			s_logger.warn("Unexpected SQL exception " + e.getMessage(), e);
		} finally {
			_txn.close();
		}
	}
}
