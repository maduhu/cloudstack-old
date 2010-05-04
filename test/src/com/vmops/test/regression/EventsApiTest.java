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

package com.vmops.test.regression;

import java.sql.Statement;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

public class EventsApiTest extends TestCase{
public static final Logger s_logger = Logger.getLogger(EventsApiTest.class.getName());
	
	
	public EventsApiTest(){
		this.setClient();
		this.setParam(new HashMap<String, String>());
	}
	
	public boolean executeTest(){
		int error=0;	
		Element rootElement = this.getInputFile().getDocumentElement();
		NodeList commandLst = rootElement.getElementsByTagName("command");
		
		
		//Analyze each command, send request and build the array list of api commands
		for (int i=0; i<commandLst.getLength(); i++) {
			Node fstNode = commandLst.item(i);
		    Element fstElmnt = (Element) fstNode;
		    
		    
		    //!!!check if we need to execute mySql command
		    NodeList commandName = fstElmnt.getElementsByTagName("name");
		    Element commandElmnt = (Element) commandName.item(0);
		    NodeList commandNm = commandElmnt.getChildNodes();
		    if (((Node) commandNm.item(0)).getNodeValue().equals("mysqlupdate")) {
		    	//establish connection to mysql server and execute an update command
		    	NodeList mysqlList = fstElmnt.getElementsByTagName("mysqlcommand");
		    	for (int j=0; j<mysqlList.getLength(); j++) {
		    		Element itemVariableElement = (Element) mysqlList.item(j);
		    		
		    		s_logger.info("Executing mysql command " + itemVariableElement.getTextContent());
		    		try {
		    			Statement st = this.getConn().createStatement();
		    			st.executeUpdate(itemVariableElement.getTextContent());
		    		} catch (Exception ex) {
		    			s_logger.error(ex);
		    			return false;
		    		}	
		    	}
		    } 
		    
		    else if (((Node) commandNm.item(0)).getNodeValue().equals("agentcommand")) {
		    	//connect to all the agents and execute agent command
		    	NodeList commandList = fstElmnt.getElementsByTagName("commandname");
		    	Element commandElement = (Element) commandList.item(0);
		    	NodeList ipList = fstElmnt.getElementsByTagName("ip");
		    	for (int j=0; j<ipList.getLength(); j++) {
		    		Element itemVariableElement = (Element) ipList.item(j);
		    		
		    		s_logger.info("Attempting to SSH into agent " + itemVariableElement.getTextContent());
		    		try {
		    			Connection conn = new Connection(itemVariableElement.getTextContent());
						conn.connect(null, 60000, 60000);

						s_logger.info("SSHed successfully into agent " + itemVariableElement.getTextContent());

						boolean isAuthenticated = conn.authenticateWithPassword("root",
								"password");

						if (isAuthenticated == false) {
							s_logger.info("Authentication failed for root with password");
							return false;
						}
						
						Session sess = conn.openSession();
						s_logger.info("Executing : " + commandElement.getTextContent());
						sess.execCommand(commandElement.getTextContent());
						Thread.sleep(60000);
						sess.close();
						conn.close();
						
		    		} catch (Exception ex) {
		    			s_logger.error(ex);
		    			return false;
		    		}	
		    	}
		    } 
		    
		    else {
			    //new command
				ApiCommand api = new ApiCommand(fstElmnt, this.getParam());
				
				//send a command
				api.sendCommand(this.getClient());
				
				
				//verify the response of the command
				if ((api.getError() == true) && (api.getResponseCode() == 200)) {
					s_logger.error("Test case " + api.getTestCaseId() + " failed. Command that was supposed to fail, passed. The command was sent with the following url " + api.getUrl());
					error++;
				}
				else if (!(api.getError() == true) && (api.getResponseCode() == 200)) {
					//verify if response is suppposed to be empty
					if (api.getEmpty() == true) {
						if (api.isEmpty() == true) {
							s_logger.info("Test case " + api.getTestCaseId() + " passed. Empty response was returned as expected. Command was sent with url " + api.getUrl());
						}
						else {
							s_logger.error("Test case " + api.getTestCaseId() + " failed. Empty response was expected. Command was sent with url " + api.getUrl());
						}
					} else {
						if (api.isEmpty() != false) 
							s_logger.error("Test case " + api.getTestCaseId() + " failed. Non-empty response was expected. Command was sent with url " + api.getUrl());
						else {
							//set parameters for the future use
							if (api.setParam(this.getParam()) == false) {
								s_logger.error("Exiting the test...Command " + api.getName() + " didn't return parameters needed for the future use. The command was sent with url " + api.getUrl());
								return false;
							}
							else if (api.getTestCaseId() != null){
								s_logger.info("Test case " + api.getTestCaseId() + " passed. Command was sent with the url " + api.getUrl());
							}
						}	
					}
				}
				else if (!(api.getError() == true) && (api.getResponseCode() != 200)) {
					s_logger.error("Command " + api.getName() + " failed with an error code " + api.getResponseCode() + " . Command was sent with url  " + api.getUrl());
					if (api.getRequired() == true) {
						s_logger.info("The command is required for the future use, so exiging");
						return false;
					}
					error++;
				}
				else if (api.getTestCaseId() != null){
						s_logger.info("Test case " + api.getTestCaseId() +  " passed. Command that was supposed to fail, failed. Command was sent with url " + api.getUrl());
				
				}	
		    }
		}
		
		//verify events with userid parameter - test case 97
		HashMap<String, Integer> expectedEvents = new HashMap<String, Integer>();
		expectedEvents.put("VM.START", 1);
		boolean eventResult = ApiCommand.verifyEvents(expectedEvents, "INFO", "http://" + this.getParam().get("hostip") + ":8096", "userid=" + this.getParam().get("userid1") + "&type=VM.START");
		s_logger.info("Test case 97 - listEvent command verification result is  " + eventResult);
		
		//verify error events
		eventResult = ApiCommand.verifyEvents("../metadata/error_events.properties", "ERROR", "http://" + this.getParam().get("hostip") + ":8096", this.getParam().get("erroruseraccount"));
		s_logger.info("listEvent command verification result is  " + eventResult);
		
		
			if (error != 0)
				return false;
			else
				return true;
	}
}
