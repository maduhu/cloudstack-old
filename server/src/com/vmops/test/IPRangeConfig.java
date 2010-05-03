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

package com.vmops.test;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import com.vmops.utils.component.ComponentLocator;
import com.vmops.utils.db.DB;
import com.vmops.utils.db.Transaction;
import com.vmops.utils.net.NetUtils;


public class IPRangeConfig {
	
	public static void main(String[] args) {
		IPRangeConfig config = ComponentLocator.inject(IPRangeConfig.class);
		config.run(args);
		System.exit(0);
    }
	
	private String usage() {
		return "Usage: ./change_ip_range.sh [add|delete] [public zone | private pod zone] startIP endIP";
	}
	
	public void run(String[] args) {
		if (args.length < 2) printError(usage());
		
		String op = args[0];
		String type = args[1];
		
		if (type.equals("public")) {
			if (args.length != 4 && args.length != 5) printError(usage());
			String zone = args[2];
			String startIP = args[3];
			String endIP = null;
			if (args.length == 5) endIP = args[4];
			
			String result = checkErrors(type, op, null, zone, startIP, endIP);
			if (!result.equals("success")) printError(result);
			
			long zoneId = PodZoneConfig.getZoneId(zone);
			result = changeRange(op, "public", -1, zoneId, startIP, endIP);
			result.replaceAll("<br>", "/n");
			System.out.println(result);
		} else if (type.equals("private")) {
			if (args.length != 5 && args.length != 6) printError(usage());
			String pod = args[2];
			String zone = args[3];;
			String startIP = args[4];
			String endIP = null;
			if (args.length == 6) endIP = args[5];
			
			String result = checkErrors(type, op, pod, zone, startIP, endIP);
			if (!result.equals("success")) printError(result);
			
			long podId = PodZoneConfig.getPodId(pod, zone);
			long zoneId = PodZoneConfig.getZoneId(zone);
			result = changeRange(op, "private", podId, zoneId, startIP, endIP);
			result.replaceAll("<br>", "/n");
			System.out.println(result);
		} else {
			printError(usage());
		}
	}
	
	public List<String> changePublicIPRangeGUI(String op, String zone, String startIP, String endIP) {
		String result = checkErrors("public", op, null, zone, startIP, endIP);
		if (!result.equals("success")) return DatabaseConfig.genReturnList("false", result);
		
		long zoneId = PodZoneConfig.getZoneId(zone);
		result = changeRange(op, "public", -1, zoneId, startIP, endIP);
		
		return DatabaseConfig.genReturnList("true", result);
	}
	
	public List<String> changePrivateIPRangeGUI(String op, String pod, String zone, String startIP, String endIP) {
		String result = checkErrors("private", op, pod, zone, startIP, endIP);
		if (!result.equals("success")) return DatabaseConfig.genReturnList("false", result);
		
		long podId = PodZoneConfig.getPodId(pod, zone);
		long zoneId = PodZoneConfig.getZoneId(zone);
		result = changeRange(op, "private", podId, zoneId, startIP, endIP);
		
		return DatabaseConfig.genReturnList("true", result);
	}

	private String checkErrors(String type, String op, String pod, String zone, String startIP, String endIP) {
		if (!op.equals("add") && !op.equals("delete")) return usage();
		
		if (type.equals("public")) {
			// Check that the zone is valid
			if (!PodZoneConfig.validZone(zone)) return "Please specify a valid zone.";
		} else if (type.equals("private")) {
			// Check that the pod and zone are valid
			if (!PodZoneConfig.validZone(zone)) return "Please specify a valid zone.";
			if (!PodZoneConfig.validPod(pod, zone)) return "Please specify a valid pod.";
		}
		
		if (!validIP(startIP)) return "Please specify a valid start IP";
		
		if (!validOrBlankIP(endIP)) return "Please specify a valid end IP";
		
		// Check that the IPs that are being added are compatible with either the zone's public netmask, or the pod's CIDR
		if (type.equals("public")) {
			// String publicNetmask = getPublicNetmask(zone);
			// String publicGateway = getPublicGateway(zone);
			
			// if (publicNetmask == null) return "Please ensure that your zone's public net mask is specified";
			// if (!sameSubnet(startIP, endIP, publicNetmask)) return "Please ensure that your start IP and end IP are in the same subnet, as per the zone's netmask.";
			// if (!sameSubnet(startIP, publicGateway, publicNetmask)) return "Please ensure that your start IP is in the same subnet as your zone's gateway, as per the zone's netmask.";
			// if (!sameSubnet(endIP, publicGateway, publicNetmask)) return "Please ensure that your end IP is in the same subnet as your zone's gateway, as per the zone's netmask.";
		} else if (type.equals("private")) {
			String cidrAddress = getCidrAddress(pod, zone);
			long cidrSize = getCidrSize(pod, zone);

			if (!sameSubnetCIDR(startIP, endIP, cidrSize)) return "Please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.";
			if (!sameSubnetCIDR(startIP, cidrAddress, cidrSize)) return "Please ensure that your start IP is in the same subnet as the pod's CIDR address.";
			if (!sameSubnetCIDR(endIP, cidrAddress, cidrSize)) return "Please ensure that your end IP is in the same subnet as the pod's CIDR address.";
		}
		
		if (!validIPRange(startIP, endIP)) return "Please specify a valid IP range.";
		
		return "success";
	}
	
	private String genChangeRangeSuccessString(Vector<String> problemIPs, String op) {
		if (problemIPs == null) return "";
		
		if (problemIPs.size() == 0) {
			if (op.equals("add")) return "Successfully added all IPs in the specified range.";
			else if (op.equals("delete")) return "Successfully deleted all IPs in the specified range.";
			else return "";
		} else {
			String successString = "";
			if (op.equals("add")) successString += "Failed to add the following IPs, because they are already in the database: <br><br>";
			else if (op.equals("delete")) successString += "Failed to delete the following IPs, because they are in use: <br><br>";
			
			for (int i = 0; i < problemIPs.size(); i++) {
				successString += problemIPs.elementAt(i);
				if (i != (problemIPs.size() - 1)) successString += ", ";
			}
			
			successString += "<br><br>";
			
			if (op.equals("add")) successString += "Successfully added all other IPs in the specified range.";
			else if (op.equals("delete")) successString += "Successfully deleted all other IPs in the specified range.";
			
			return successString;
		}
	}
	
	private String changeRange(String op, String type, long podId, long zoneId, String startIP, String endIP) {
		
		// Go through all the IPs and add or delete them
		Vector<String> problemIPs = null;
		if (op.equals("add")) {
			problemIPs = saveIPRange(type, podId, zoneId, 1, startIP, endIP);
		} else if (op.equals("delete")) {
			problemIPs = deleteIPRange(type, podId, zoneId, 1, startIP, endIP);
		}
		
		if (problemIPs == null) return null;
		else return genChangeRangeSuccessString(problemIPs, op);
	}
	
	private String genSuccessString(Vector<String> problemIPs, String op) {
		if (problemIPs == null) return "";
		
		if (problemIPs.size() == 0) {
			if (op.equals("add")) return "Successfully added all IPs in the specified range.";
			else if (op.equals("delete")) return "Successfully deleted all IPs in the specified range.";
			else return "";
		} else {
			String successString = "";
			if (op.equals("add")) successString += "Failed to add the following IPs, because they are already in the database: <br><br>";
			else if (op.equals("delete")) successString += "Failed to delete the following IPs, because they are in use: <br><br>";
			
			for (int i = 0; i < problemIPs.size(); i++) {
				successString += problemIPs.elementAt(i);
				if (i != (problemIPs.size() - 1)) successString += ", ";
			}
			
			successString += "<br><br>";
			
			if (op.equals("add")) successString += "Successfully added all other IPs in the specified range.";
			else if (op.equals("delete")) successString += "Successfully deleted all other IPs in the specified range.";
			
			return successString;
		}
	}
	
	public static String getCidrAddress(String pod, String zone) {
		long dcId = PodZoneConfig.getZoneId(zone);
		String selectSql = "SELECT * FROM `vmops`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
		String errorMsg = "Could not read CIDR address for pod/zone: " + pod + "/" + zone + " from database. Please contact VMOps Support.";
		return DatabaseConfig.getDatabaseValueString(selectSql, "cidr_address", errorMsg);
	}
	
	public static long getCidrSize(String pod, String zone) {
		long dcId = PodZoneConfig.getZoneId(zone);
		String selectSql = "SELECT * FROM `vmops`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
		String errorMsg = "Could not read CIDR address for pod/zone: " + pod + "/" + zone + " from database. Please contact VMOps Support.";
		return DatabaseConfig.getDatabaseValueLong(selectSql, "cidr_size", errorMsg);
	}

	@DB
	protected Vector<String> deleteIPRange(String type, long podId, long zoneId, long vlanDbId, String startIP, String endIP) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = startIPLong;
    	if (endIP != null) endIPLong = NetUtils.ip2Long(endIP);
 
    	Transaction txn = Transaction.currentTxn();
    	Vector<String> problemIPs = null;
    	if (type.equals("public")) problemIPs = deletePublicIPRange(txn, startIPLong, endIPLong, vlanDbId);
    	else if (type.equals("private")) problemIPs = deletePrivateIPRange(txn, startIPLong, endIPLong, podId, zoneId);
    	
    	return problemIPs;
    }
	
	private Vector<String> deletePublicIPRange(Transaction txn, long startIP, long endIP, long vlanDbId) {
		String deleteSql = "DELETE FROM `vmops`.`user_ip_address` WHERE public_ip_address = ? AND vlan_id = ?";
		String isPublicIPAllocatedSelectSql = "SELECT * FROM `vmops`.`user_ip_address` WHERE public_ip_address = ? AND vlan_id = ?";
		
		Vector<String> problemIPs = new Vector<String>();
		PreparedStatement stmt = null;
		PreparedStatement isAllocatedStmt = null;
		
		Connection conn = null;
		try {
			conn = txn.getConnection();
			stmt = conn.prepareStatement(deleteSql);
			isAllocatedStmt = conn.prepareStatement(isPublicIPAllocatedSelectSql);
		} catch (SQLException e) {
			return null;
		}
		
		while (startIP <= endIP) {
			if (!isPublicIPAllocated(NetUtils.long2Ip(startIP), vlanDbId, isAllocatedStmt)) {
				try {
					stmt.clearParameters();
					stmt.setString(1, NetUtils.long2Ip(startIP));
					stmt.setLong(2, vlanDbId);
					stmt.executeUpdate();
				} catch (Exception ex) {
				}
			} else {
				problemIPs.add(NetUtils.long2Ip(startIP));
			}
			startIP += 1;
		}
			
        return problemIPs;
	}
	
	private Vector<String> deletePrivateIPRange(Transaction txn, long startIP, long endIP, long podId, long zoneId) {
		String deleteSql = "DELETE FROM `vmops`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND pod_id = ? AND data_center_id = ?";
		String isPrivateIPAllocatedSelectSql = "SELECT * FROM `vmops`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND data_center_id = ? AND pod_id = ?";
		
		Vector<String> problemIPs = new Vector<String>();
		PreparedStatement stmt = null;
		PreparedStatement isAllocatedStmt = null;
				
		Connection conn = null;
		try {
			conn = txn.getConnection();
			stmt = conn.prepareStatement(deleteSql);
			isAllocatedStmt = conn.prepareStatement(isPrivateIPAllocatedSelectSql);
		} catch (SQLException e) {
			System.out.println("Exception: " + e.getMessage());
			printError("Unable to start DB connection to delete private IPs. Please contact VMOps Support.");
		}
		
		while (startIP <= endIP) {
			if (!isPrivateIPAllocated(NetUtils.long2Ip(startIP), podId, zoneId, isAllocatedStmt)) {
				try {
					stmt.clearParameters();
					stmt.setString(1, NetUtils.long2Ip(startIP));
					stmt.setLong(2, podId);
					stmt.setLong(3, zoneId);
					stmt.executeUpdate();
				} catch (Exception ex) {
				}
			} else {
				problemIPs.add(NetUtils.long2Ip(startIP));
			}
        	startIP += 1;
		}

        return problemIPs;
	}
	
	private boolean isPublicIPAllocated(String ip, long vlanDbId, PreparedStatement stmt) {
		try {
        	stmt.clearParameters();
        	stmt.setString(1, ip);
        	stmt.setLong(2, vlanDbId);
        	ResultSet rs = stmt.executeQuery();
        	if (rs.next()) return (rs.getString("allocated") != null);
        	else return false;
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
            return true;
        }
	}
	
	private boolean isPrivateIPAllocated(String ip, long podId, long zoneId, PreparedStatement stmt) {
		try {
			stmt.clearParameters();
        	stmt.setString(1, ip);
        	stmt.setLong(2, zoneId);
        	stmt.setLong(3, podId);
        	ResultSet rs = stmt.executeQuery();
        	if (rs.next()) return (rs.getString("taken") != null);
        	else return false;
        } catch (SQLException ex) {
        	System.out.println(ex.getMessage());
            return true;
        }
	}
	
	@DB
	public Vector<String> saveIPRange(String type, long podId, long zoneId, long vlanDbId, String startIP, String endIP) {
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong = startIPLong;
    	if (endIP != null) endIPLong = NetUtils.ip2Long(endIP);
    	
    	Transaction txn = Transaction.currentTxn();
    	Vector<String> problemIPs = null;
    	
    	if (type.equals("public")) problemIPs = savePublicIPRange(txn, startIPLong, endIPLong, zoneId, vlanDbId);
    	else if (type.equals("private")) problemIPs = savePrivateIPRange(txn, startIPLong, endIPLong, podId, zoneId);
    	
    	return problemIPs;
    }
    
	private Vector<String> savePublicIPRange(Transaction txn, long startIP, long endIP, long zoneId, long vlanDbId) {
		String insertSql = "INSERT INTO `vmops`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id) VALUES (?, ?, ?)";
		Vector<String> problemIPs = new Vector<String>();
		PreparedStatement stmt = null;
		
		Connection conn = null;
		try {
			conn = txn.getConnection();
		} catch (SQLException e) {
			return null;
		}
        
        while (startIP <= endIP) {
        	try {
        		stmt = conn.prepareStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIP));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, vlanDbId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		problemIPs.add(NetUtils.long2Ip(startIP));
        	}
        	startIP += 1;
        }
        
        return problemIPs;
	}
	
	private Vector<String> savePrivateIPRange(Transaction txn, long startIP, long endIP, long podId, long zoneId) {
		String insertSql = "INSERT INTO `vmops`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
		Vector<String> problemIPs = new Vector<String>();
		PreparedStatement stmt = null;
		
		Connection conn = null;
		try {
			conn = txn.getConnection();
		} catch (SQLException e) {
			System.out.println("Exception: " + e.getMessage());
			printError("Unable to start DB connection to save private IPs. Please contact VMOps Support.");
		}
		
        while (startIP <= endIP) {
        	try {
        		stmt = conn.prepareStatement(insertSql);
        		stmt.setString(1, NetUtils.long2Ip(startIP));
        		stmt.setLong(2, zoneId);
        		stmt.setLong(3, podId);
        		stmt.executeUpdate();
        		stmt.close();
        	} catch (Exception ex) {
        		 problemIPs.add(NetUtils.long2Ip(startIP));
        	}
        	startIP += 1;
        }
        
        return problemIPs;
	}
	
	public static String getPublicNetmask(String zone) {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `vmops`.`data_center` WHERE name = \"" + zone + "\"", "netmask",
								"Unable to start DB connection to read public netmask. Please contact VMOps Support.");
	}
		
	public static String getPublicGateway(String zone) {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `vmops`.`data_center` WHERE name = \"" + zone + "\"", "gateway",
								"Unable to start DB connection to read public gateway. Please contact VMOps Support.");
	}
	
	public static String getGuestIpNetwork() {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `vmops`.`configuration` WHERE name = \"guest.ip.network\"", "value",
		"Unable to start DB connection to read guest IP network. Please contact VMOps Support.");
	}
	
	public static String getGuestNetmask() {
		return DatabaseConfig.getDatabaseValueString("SELECT * FROM `vmops`.`configuration` WHERE name = \"guest.netmask\"", "value",
		"Unable to start DB connection to read guest netmask. Please contact VMOps Support.");
	}
	
	public static String getGuestSubnet() {
		String guestIpNetwork = getGuestIpNetwork();
		String guestNetmask = getGuestNetmask();
		
		if (guestIpNetwork == null || guestIpNetwork.isEmpty()) printError("Please enter a valid guest IP network address.");
		if (guestNetmask == null || guestNetmask.isEmpty()) printError("Please enter a valid guest IP network netmask");
		
		return NetUtils.getSubNet(guestIpNetwork, guestNetmask);
	}

	public static long getGuestCidrSize() {
		String guestNetmask = getGuestNetmask();
		return NetUtils.getCidrSize(guestNetmask);
	}
	
	public static boolean validCIDR(final String cidr) {
		if (cidr == null || cidr.isEmpty()) return false;
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) return false;
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!validIP(cidrAddress)) return false;
        int cidrSizeNum = -1;
        
        try {
        	cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
        	return false;
        }
        
        if (cidrSizeNum < 1 || cidrSizeNum > 32) return false;
        
        return true;
	}
	
	public static boolean validOrBlankIP(final String ip) {
	    if (ip == null || ip.isEmpty()) return true;
	    return validIP(ip);
	}
	    
	public static boolean validIP(final String ip) {
		final String[] ipAsList = ip.split("\\.");
		
	    // The IP address must have four octets
	    if (Array.getLength(ipAsList) != 4) {
	    	return false;
	    }
	    
	    for (int i = 0; i < 4; i++) {
	    	// Each octet must be an integer
	    	final String octetString = ipAsList[i];
	    	int octet;
	    	try {
	    		octet = Integer.parseInt(octetString);
	    	} catch(final Exception e) {
	    		return false;
	   		}
	    	// Each octet must be between 0 and 255, inclusive
	    	if (octet < 0 || octet > 255) return false;

	    	// Each octetString must have between 1 and 3 characters
	    	if (octetString.length() < 1 || octetString.length() > 3) return false;
	   		
	   	}
	    
	   	// IP is good, return true
		return true;
	}
	   
    public static boolean validIPRange(String startIP, String endIP) {
    	if (endIP == null || endIP.isEmpty()) return true;
    	
    	long startIPLong = NetUtils.ip2Long(startIP);
    	long endIPLong =  NetUtils.ip2Long(endIP);
	   	return (startIPLong < endIPLong);
    }
    
    public static boolean sameSubnet(final String ip1, final String ip2, final String netmask) {
    	if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) return true;
    	String subnet1 = NetUtils.getSubNet(ip1, netmask);
    	String subnet2 = NetUtils.getSubNet(ip2, netmask);
    	
    	return (subnet1.equals(subnet2));
    }
    
    public static boolean sameSubnetCIDR(final String ip1, final String ip2, final long cidrSize) {
    	if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) return true;
    	String subnet1 = NetUtils.getCidrSubNet(ip1, cidrSize);
    	String subnet2 = NetUtils.getCidrSubNet(ip2, cidrSize);
    	
    	return (subnet1.equals(subnet2));
    }
    
	private static void printError(String message) {
		DatabaseConfig.printError(message);
	}
	
}
