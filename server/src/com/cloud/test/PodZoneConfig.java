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

package com.cloud.test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;

public class PodZoneConfig {
	
	public static void main(String[] args) {
		PodZoneConfig config = ComponentLocator.inject(PodZoneConfig.class);
		//config.run(args);
		System.exit(0);
    }
	
	private void printUsage() {
		System.out.println("Usage to add pod: ./add_pod_or_zone.sh pod podName zoneName CIDR");
		System.out.println("Usage to add zone: ./add_pod_or_zone.sh zone zoneName gateway netmask dns1 [dns2] [dns3] [dns4]");
		System.exit(1);
	}
	
//	public void run(String[] args) {
//		if (args.length == 0) printUsage();
//		
//		String podOrZone = args[0];
//		
//		if (podOrZone.equals("pod")) {
//			if (args.length != 4) printUsage();
//			
//			String podName = args[1];
//			String zoneName = args[2];
//			String cidr = args[3];
//						
//			if (!validZone(zoneName)) printError("Please specify a valid zone.");
//			long dcId = getZoneId(zoneName);
//			
//			// Get the individual cidrAddress and cidrSize values
//			String cidrAddress = null;
//			String cidrSize = null;
//			if (IPRangeConfig.validCIDR(cidr)) {
//				String[] cidrPair = cidr.split("\\/");
//				cidrAddress = cidrPair[0];
//				cidrSize = cidrPair[1];
//			} else {
//				printError("Please enter a valid CIDR for pod: " + podName);
//			}
//			
//			if (cidrAddress == null || cidrSize == null) printError("Please enter a valid CIDR for pod: " + podName);
//			
//			HashMap<Long, Vector<Object>> currentPodCidrSubnets = getCurrentPodCidrSubnets(dcId);
//			Vector<Object> newCidrPair = new Vector<Object>();
//			newCidrPair.add(0, cidrAddress);
//			newCidrPair.add(1, new Long(Long.parseLong(cidrSize)));
//			currentPodCidrSubnets.put(new Long(-1), newCidrPair);
//			String result = checkPodCidrSubnets(dcId, currentPodCidrSubnets);
//			if (!result.equals("success")) printError(result);
//			
//			savePod(true, -1, podName, dcId, cidr, -1 , -1);
//		} else if (podOrZone.equals("zone")) {
//			if (args.length < 5) printUsage();
//			
//			String zoneName = args[1];
//			String gateway = args[2];
//			String netmask = args[3];
//			String dns1 = args[4];
//			String dns2 = null;
//			String dns3 = null;
//			String dns4 = null;
//			
//			if (args.length >= 6) dns2 = args[5];
//			if (args.length >= 7) dns3 = args[6];
//			if (args.length == 8) dns4 = args[7];
//			
//			// Check IP validity for gateway, netmask, and DNS addresses
//			if (!IPRangeConfig.validIP(gateway)) printError("Please enter a valid gateway address.");
//			if (!IPRangeConfig.validIP(netmask)) printError("Please enter a valid netmask.");
//			if (dns1 != null  && !IPRangeConfig.validIP(dns1)) printError("Please enter a valid IP address for DNS1");
//			if (dns2 != null  && !IPRangeConfig.validIP(dns2)) printError("Please enter a valid IP address for DNS2");
//			if (dns3 != null  && !IPRangeConfig.validIP(dns3)) printError("Please enter a valid IP address for DNS3");
//			if (dns4 != null  && !IPRangeConfig.validIP(dns4)) printError("Please enter a valid IP address for DNS4");
//
//			saveZone(true, -1, zoneName, dns1, dns2, dns3, dns4, -1, -1);
//		} else {
//			printUsage();
//		}
//	}
	
	
	
	public List<String> savePodGUI(String podName, String zoneName, String cidr) {
		// Check if the zone is valid
		if (!validZone(zoneName)) return DatabaseConfig.genReturnList("false", "Please specify a valid zone.");
		long dcId = getZoneId(zoneName);
		
		// Check if the pod already exists
		if (getPodId(podName, zoneName) != -1) return DatabaseConfig.genReturnList("false", "A pod in with that name already exists in zone " + zoneName + ". Please specify a different pod name. ");
		
		// Get the individual cidrAddress and cidrSize values
		String cidrAddress = null;
		String cidrSize = null;
		if (IPRangeConfig.validCIDR(cidr)) {
			String[] cidrPair = cidr.split("\\/");
			cidrAddress = cidrPair[0];
			cidrSize = cidrPair[1];
		} else {
			return DatabaseConfig.genReturnList("false", "Please enter a valid CIDR for pod: " + podName);
		}
		
		// Check if the CIDR is valid
		if (cidrAddress == null || cidrSize == null) return DatabaseConfig.genReturnList("false", "Please enter a valid CIDR for pod: " + podName);
		
		// Check if the CIDR conflicts with the Guest Network or other pods
		HashMap<Long, Vector<Object>> currentPodCidrSubnets = getCurrentPodCidrSubnets(dcId);
		Vector<Object> newCidrPair = new Vector<Object>();
		newCidrPair.add(0, cidrAddress);
		newCidrPair.add(1, new Long(Long.parseLong(cidrSize)));
		currentPodCidrSubnets.put(new Long(-1), newCidrPair);
		String result = checkPodCidrSubnets(dcId, currentPodCidrSubnets);
		if (!result.equals("success")) return DatabaseConfig.genReturnList("false", result);
		
		savePod(false, -1, podName, dcId, cidr, -1, -1);
		
		return DatabaseConfig.genReturnList("true", "");
	}
	
	public List<String> saveZoneGUI(String zoneName, String gateway, String netmask, String dns1, String dns2, String dns3, String dns4, String guestNetworkCidr) {
		
		// Check IP validity for gateway, netmask, and DNS addresses
		if (!IPRangeConfig.validIP(gateway)) return DatabaseConfig.genReturnList("false", "Please enter a valid gateway address.");
		if (!IPRangeConfig.validIP(netmask)) return DatabaseConfig.genReturnList("false", "Please enter a valid netmask.");
		if (dns1 != null  && !IPRangeConfig.validIP(dns1)) return DatabaseConfig.genReturnList("false", "Please enter a valid IP address for DNS1");
		if (dns2 != null  && !IPRangeConfig.validIP(dns2)) return DatabaseConfig.genReturnList("false", "Please enter a valid IP address for DNS2");
		if (dns3 != null  && !IPRangeConfig.validIP(dns3)) return DatabaseConfig.genReturnList("false", "Please enter a valid IP address for DNS3");
		if (dns4 != null  && !IPRangeConfig.validIP(dns4)) return DatabaseConfig.genReturnList("false", "Please enter a valid IP address for DNS4");
		if (guestNetworkCidr != null  && !IPRangeConfig.validCIDR(guestNetworkCidr)) return DatabaseConfig.genReturnList("false", "Please enter a valid guest network cidr");
		
		saveZone(false, -1, zoneName, dns1, dns2, dns3, dns4, -1, -1,guestNetworkCidr);
		
		return DatabaseConfig.genReturnList("true", "");
	}
	
	public void savePod(boolean printOutput, long id, String name, long dcId, String cidr, int vlanStart, int vlanEnd) {
		// Check that the cidr was valid
		if (!IPRangeConfig.validCIDR(cidr)) printError("Please enter a valid CIDR for pod: " + name);
		
		// Get the individual cidrAddress and cidrSize values
		String[] cidrPair = cidr.split("\\/");
		String cidrAddress = cidrPair[0];
		String cidrSize = cidrPair[1];

		String sql = null;
		if (id != -1) sql = "INSERT INTO `cloud`.`host_pod_ref` (id, name, data_center_id, cidr_address, cidr_size) " + "VALUES ('" + id + "','" + name + "','" + dcId + "','" + cidrAddress + "','" + cidrSize + "')";
		else sql = "INSERT INTO `cloud`.`host_pod_ref` (name, data_center_id, cidr_address, cidr_size) " + "VALUES ('" + name + "','" + dcId + "','" + cidrAddress + "','" + cidrSize + "')";
			
        DatabaseConfig.saveSQL(sql, "Failed to save pod due to exception. Please contact Cloud Support.");
        
        /*
        // Hardcode the vnet range to be the full range
        int begin = 0x64;
        int end = 64000;
        
        // If vnet arguments were passed in, use them
        if (vlanStart != -1 && vlanEnd != -1) {
            begin = vlanStart;
            end = vlanEnd;
        }
        
        long podId = getPodId(name, dcId);
        String insertVlan = "INSERT INTO `vmops`.`op_pod_vlan_alloc` (vlan, data_center_id, pod_id) VALUES ( ?, ?, ?)";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVlan);
            for (int i = begin; i <= end; i++) {
                stmt.setString(1, Integer.toString(i));
                stmt.setLong(2, dcId);
                stmt.setLong(3, podId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException ex) {
            printError("Error creating vlan for the pod. Please contact Cloud Support.");
        }
        */
        if (printOutput) System.out.println("Successfuly saved pod.");
	}
	
	public void checkAllPodCidrSubnets() {
		Vector<Long> allZoneIDs = getAllZoneIDs();
		for (Long dcId : allZoneIDs) {
			HashMap<Long, Vector<Object>> currentPodCidrSubnets = getCurrentPodCidrSubnets(dcId.longValue());
			String result = checkPodCidrSubnets(dcId.longValue(), currentPodCidrSubnets);
			if (!result.equals("success")) printError(result);
		}
	}
	
	private String checkPodCidrSubnets(long dcId, HashMap<Long, Vector<Object>> currentPodCidrSubnets) {
		
//		DataCenterDao _dcDao = null;
//        final ComponentLocator locator = ComponentLocator.getLocator("management-server");
        
//        _dcDao = locator.getDao(DataCenterDao.class);
		// For each pod, return an error if any of the following is true:
		// 1. The pod's CIDR subnet conflicts with the guest network subnet
		// 2. The pod's CIDR subnet conflicts with the CIDR subnet of any other pod
		
		String zoneName  = PodZoneConfig.getZoneName(dcId);
		
		//get the guest network cidr and guest netmask from the zone
//		DataCenterVO dcVo = _dcDao.findById(dcId);
		
		String guestNetworkCidr = IPRangeConfig.getGuestNetworkCidr(dcId);
		
		if (guestNetworkCidr == null || guestNetworkCidr.isEmpty()) return "Please specify a valid guest cidr";
        String[] cidrTuple = guestNetworkCidr.split("\\/");
        
		String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
		long guestCidrSize = Long.parseLong(cidrTuple[1]);
        
        // Iterate through all pods in this zone
		for (Long podId : currentPodCidrSubnets.keySet()) {
			String podName;
			if (podId.longValue() == -1) podName = "newPod";
			else podName = PodZoneConfig.getPodName(podId.longValue(), dcId);
			
			Vector<Object> cidrPair = currentPodCidrSubnets.get(podId);
			String cidrAddress = (String) cidrPair.get(0);
			long cidrSize = ((Long) cidrPair.get(1)).longValue();
			
			long cidrSizeToUse = -1;
			if (cidrSize < guestCidrSize) cidrSizeToUse = cidrSize;
			else cidrSizeToUse = guestCidrSize;
			
			String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
			String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);
			
			// Check that cidrSubnet does not equal guestSubnet
			if (cidrSubnet.equals(guestSubnet)) {
				if (podName.equals("newPod")) {
					return "The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.";
				} else {
					return "Warning: The subnet of pod " + podName + " in zone " + zoneName + " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.";
				}
			}
			
			// Iterate through the rest of the pods
			for (Long otherPodId : currentPodCidrSubnets.keySet()) {
				if (podId.equals(otherPodId)) continue;
				
				// Check that cidrSubnet does not equal otherCidrSubnet
				Vector<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
				String otherCidrAddress = (String) otherCidrPair.get(0);
				long otherCidrSize = ((Long) otherCidrPair.get(1)).longValue();
				
				if (cidrSize < otherCidrSize) cidrSizeToUse = cidrSize;
				else cidrSizeToUse = otherCidrSize;
				
				cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
				String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);
				
				if (cidrSubnet.equals(otherCidrSubnet)) {
					String otherPodName = PodZoneConfig.getPodName(otherPodId.longValue(), dcId);
					if (podName.equals("newPod")) {
						return "The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName + ". Please specify a different CIDR.";
					} else {
						return "Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName + " have conflicting CIDR subnets. Please change the CIDR of one of these pods.";
					}
				}
			}
		}
		
		return "success";
	}
	
	@DB
	protected HashMap<Long, Vector<Object>> getCurrentPodCidrSubnets(long dcId) {
		HashMap<Long, Vector<Object>> currentPodCidrSubnets = new HashMap<Long, Vector<Object>>();
		
		String selectSql = "SELECT id, cidr_address, cidr_size FROM host_pod_ref WHERE data_center_id=" + dcId;
		Transaction txn = Transaction.currentTxn();
		try {
        	PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
        	ResultSet rs = stmt.executeQuery();
        	while (rs.next()) {
        		Long podId = rs.getLong("id");
        		String cidrAddress = rs.getString("cidr_address");
        		long cidrSize = rs.getLong("cidr_size");
        		Vector<Object> cidrPair = new Vector<Object>();
        		cidrPair.add(0, cidrAddress);
        		cidrPair.add(1, new Long(cidrSize));
        		currentPodCidrSubnets.put(podId, cidrPair);
        	}
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
        	printError("There was an issue with reading currently saved pod CIDR subnets. Please contact Cloud Support.");
            return null;
        }
        
        return currentPodCidrSubnets;
	}
	
	public void deletePod(String name, long dcId) {
		String sql = "DELETE FROM `cloud`.`host_pod_ref` WHERE name=\"" + name + "\" AND data_center_id=\"" + dcId + "\"";
		DatabaseConfig.saveSQL(sql, "Failed to delete pod due to exception. Please contact Cloud Support.");
	}
	
	public long getVlanDbId(String zone, String vlanId) {
		long zoneId = getZoneId(zone);
		
		return DatabaseConfig.getDatabaseValueLong("SELECT * FROM `cloud`.`vlan` WHERE data_center_id=\"" + zoneId + "\" AND vlan_id =\"" + vlanId + "\"", "id",
		"Unable to start DB connection to read vlan DB id. Please contact Cloud Support.");
    }
	
	public List<String> modifyVlan(String zone, boolean add, String vlanId, String vlanGateway, String vlanNetmask, String pod, String vlanType) {
    	// Check if the zone is valid
    	long zoneId = getZoneId(zone);
    	if (zoneId == -1)
    		return genReturnList("false", "Please specify a valid zone.");
    	
    	Long podId = pod!=null?getPodId(pod, zone):null;
    	if (podId != null && podId == -1)
    		return genReturnList("false", "Please specify a valid pod.");
    	
    	if (add) {
    		
    		// Make sure the gateway is valid
    		if (!NetUtils.isValidIp(vlanGateway))
    			return genReturnList("false", "Please specify a valid gateway.");
    		
    		// Make sure the netmask is valid
    		if (!NetUtils.isValidIp(vlanNetmask))
    			return genReturnList("false", "Please specify a valid netmask.");
    		
    		// Check if a vlan with the same vlanId already exists in the specified zone
    		if (getVlanDbId(zone, vlanId) != -1)
    			return genReturnList("false", "A VLAN with the specified VLAN ID already exists in zone " + zone + ".");
    		
    		/*
    		// Check if another vlan in the same zone has the same subnet
    		String newVlanSubnet = NetUtils.getSubNet(vlanGateway, vlanNetmask);
    		List<VlanVO> vlans = _vlanDao.findByZone(zoneId);
    		for (VlanVO vlan : vlans) {
    			String currentVlanSubnet = NetUtils.getSubNet(vlan.getVlanGateway(), vlan.getVlanNetmask());
    			if (newVlanSubnet.equals(currentVlanSubnet))
    				return genReturnList("false", "The VLAN with ID " + vlan.getVlanId() + " in zone " + zone + " has the same subnet. Please specify a different gateway/netmask.");
    		}
    		*/
    		
    		// Everything was fine, so persist the VLAN
    		saveVlan(zoneId, podId, vlanId, vlanGateway, vlanNetmask, vlanType);
            if (podId != null) {
            	long vlanDbId = getVlanDbId(zone, vlanId);
            	String sql = "INSERT INTO `cloud`.`pod_vlan_map` (pod_id, vlan_db_id) " + "VALUES ('" + podId + "','" + vlanDbId  + "')";
                DatabaseConfig.saveSQL(sql, "Failed to save pod_vlan_map due to exception vlanDbId=" + vlanDbId + ", podId=" + podId + ". Please contact Cloud Support.");
            }
    		
    		return genReturnList("true", "Successfully added VLAN.");
    		
    	} else {
    		return genReturnList("false", "That operation is not suppored.");
    	}
    	
    	/*
    	else {
    		
    		// Check if a VLAN actually exists in the specified zone
    		long vlanDbId = getVlanDbId(zone, vlanId);
    		if (vlanDbId == -1)
    			return genReturnList("false", "A VLAN with ID " + vlanId + " does not exist in zone " + zone);
    		
    		// Check if there are any public IPs that are in the specified vlan.
    		List<IPAddressVO> ips = _publicIpAddressDao.listByVlanDbId(vlanDbId);
    		if (ips.size() != 0)
    			return genReturnList("false", "Please delete all IP addresses that are in VLAN " + vlanId + " before deleting the VLAN.");
    		
    		// Delete the vlan
    		_vlanDao.delete(vlanDbId);
    		
    		return genReturnList("true", "Successfully deleted VLAN.");
    	}
    	*/
    }
	
	@DB
	public void saveZone(boolean printOutput, long id, String name, String dns1, String dns2, String dns3, String dns4, int vnetStart, int vnetEnd, String guestNetworkCidr) {
		
		if (printOutput) System.out.println("Saving zone, please wait...");
		
		String columns = null;
		String values = null;
		
		if (id != -1) {
			columns = "(id, name";
			values = "('" + id + "','" + name + "'";
		} else {
			columns = "(name";
			values = "('" + name + "'";
		}

		if (dns1 != null) {
			columns += ", dns1";
			values += ",'" + dns1 + "'";
		}
		
		if (dns2 != null) {
			columns += ", dns2";
			values += ",'" + dns2 + "'";
		}
		
		if (dns3 != null) {
			columns += ", internal_dns1";
			values += ",'" + dns3 + "'";
		}
		
		if (dns4 != null) {
			columns += ", internal_dns2";
			values += ",'" + dns4 + "'";
		}
		
		if(guestNetworkCidr != null) {
			columns += ", guest_network_cidr";
			values += ",'" + guestNetworkCidr + "'";
		}
		
			
			
		columns += ")";
		values += ")";
		
		String sql = "INSERT INTO `cloud`.`data_center` " + columns +  " VALUES " + values;
		
		DatabaseConfig.saveSQL(sql, "Failed to save zone due to exception. Please contact Cloud Support.");
		
		// Hardcode the vnet range to be the full range
		int begin = 0x64;
        int end = 64000;
        
        // If vnet arguments were passed in, use them
        if (vnetStart != -1 && vnetEnd != -1) {
        	begin = vnetStart;
        	end = vnetEnd;
        }
        
        long dcId = getZoneId(name);
        String insertVnet = "INSERT INTO `cloud`.`op_dc_vnet_alloc` (vnet, data_center_id) VALUES ( ?, ?)";

        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i = begin; i <= end; i++) {
                stmt.setString(1, Integer.toString(i));
                stmt.setLong(2, dcId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException ex) {
            printError("Error creating vnet for the data center. Please contact Cloud Support.");
        }
		
		if (printOutput) System.out.println("Successfully saved zone.");
	}
	
	public void deleteZone(String name) {
		String sql = "DELETE FROM `cloud`.`data_center` WHERE name=\"" + name + "\"";
		DatabaseConfig.saveSQL(sql, "Failed to delete zone due to exception. Please contact Cloud Support.");
	}
	
	public void saveVlan(long zoneId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String vlanType) {
		String sql = "INSERT INTO `cloud`.`vlan` (vlan_id, vlan_gateway, vlan_netmask, data_center_id, vlan_type) " + "VALUES ('" + vlanId + "','" + vlanGateway + "','" + vlanNetmask + "','" + zoneId + "','" + vlanType + "')";
        DatabaseConfig.saveSQL(sql, "Failed to save vlan due to exception. Please contact Cloud Support.");
	}
	
	public static long getPodId(String pod, String zone) {
		long dcId = getZoneId(zone);
		String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
		String errorMsg = "Could not read pod ID fro mdatabase. Please contact Cloud Support.";
		return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
	}
	
	public static long getPodId(String pod, long dcId) {
        String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
        String errorMsg = "Could not read pod ID fro mdatabase. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
    }
	
	public static long getZoneId(String zone) {
		String selectSql = "SELECT * FROM `cloud`.`data_center` WHERE name = \"" + zone + "\"";
		String errorMsg = "Could not read zone ID from database. Please contact Cloud Support.";
		return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
	}
	
	@DB
	public Vector<Long> getAllZoneIDs() {
		Vector<Long> allZoneIDs = new Vector<Long>();
		
		String selectSql = "SELECT id FROM data_center";
		Transaction txn = Transaction.currentTxn();
		try {
        	PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
        	ResultSet rs = stmt.executeQuery();
        	while (rs.next()) {
        		Long dcId = rs.getLong("id");
        		allZoneIDs.add(dcId);
        	}
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
        	printError("There was an issue with reading zone IDs. Please contact Cloud Support.");
            return null;
        }
        
        return allZoneIDs;
	}
	
	
	public static boolean validPod(String pod, String zone) {
		return (getPodId(pod, zone) != -1);
	}
	
	public static boolean validZone(String zone) {
		return (getZoneId(zone) != -1);
	}
	
	public static String getPodName(long podId, long dcId) {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`host_pod_ref` WHERE id=" + podId + " AND data_center_id=" + dcId, "name",
		"Unable to start DB connection to read pod name. Please contact Cloud Support.");
	}
	
	public static String getZoneName(long dcId) {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`data_center` WHERE id=" + dcId, "name",
		"Unable to start DB connection to read zone name. Please contact Cloud Support.");
	}
	
	private static void printError(String message) {
		DatabaseConfig.printError(message);
	}
	
	private List<String> genReturnList(String success, String message) {
    	List<String> returnList = new ArrayList<String>();
    	returnList.add(0, success);
    	returnList.add(1, message);
    	return returnList;
    }
	
}
