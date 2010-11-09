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

function afterLoadRouterJSP() {
    
}

function routerToMidmenu(jsonObj, $midmenuItem1) {
    $midmenuItem1.attr("id", getMidmenuId(jsonObj));  
    $midmenuItem1.data("jsonObj", jsonObj); 
    
    $midmenuItem1.find("#first_row").text(jsonObj.name.substring(0,25)); 
    $midmenuItem1.find("#second_row").text(jsonObj.publicip.substring(0,25));
    updateVmStateInMidMenu(jsonObj, $midmenuItem1);       
}

function routerAfterDetailsTabAction(json, $midmenuItem1, id) {        
    var jsonObj = json.queryasyncjobresultresponse.router[0];    
    routerToMidmenu(jsonObj, $midmenuItem1);  
    routerJsonToDetailsTab($midmenuItem1);   
}

function routerToRightPanel($midmenuItem1) { 
    copyActionInfoFromMidMenuToRightPanel($midmenuItem1);
    routerJsonToDetailsTab($midmenuItem1);   
}

function routerJsonToDetailsTab($midmenuItem1) {   
    var jsonObj = $midmenuItem1.data("jsonObj"); 
    var $detailsTab = $("#right_panel_content #tab_content_details");    
    $detailsTab.data("jsonObj", jsonObj);         
    setVmStateInRightPanel(jsonObj.state, $detailsTab.find("#state"));  
    $detailsTab.find("#ipAddress").text(jsonObj.publicip);
    $detailsTab.find("#zonename").text(fromdb(jsonObj.zonename));
    $detailsTab.find("#name").text(fromdb(jsonObj.name));
    $detailsTab.find("#publicip").text(fromdb(jsonObj.publicip));
    $detailsTab.find("#privateip").text(fromdb(jsonObj.privateip));
    $detailsTab.find("#guestipaddress").text(fromdb(jsonObj.guestipaddress));
    $detailsTab.find("#hostname").text(fromdb(jsonObj.hostname));
    $detailsTab.find("#networkdomain").text(fromdb(jsonObj.networkdomain));
    $detailsTab.find("#account").text(fromdb(jsonObj.account));  
    setDateField(jsonObj.created, $detailsTab.find("#created"));	 
    
    resetViewConsoleAction(jsonObj, $detailsTab);   
    
    //***** actions (begin) *****    
    var $actionMenu = $("#right_panel_content #tab_content_details #action_link #action_menu");
    $actionMenu.find("#action_list").empty();
    var noAvailableActions = true;
   
    if (jsonObj.state == 'Running') {   
        buildActionLinkForTab("Stop Router", routerActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
        buildActionLinkForTab("Reboot Router", routerActionMap, $actionMenu, $midmenuItem1, $detailsTab);	  
        noAvailableActions = false;      
    }
    else if (jsonObj.state == 'Stopped') {        
        buildActionLinkForTab("Start Router", routerActionMap, $actionMenu, $midmenuItem1, $detailsTab);	
        noAvailableActions = false;
    }  
    
    // no available actions 
	if(noAvailableActions == true) {
	    $actionMenu.find("#action_list").append($("#no_available_actions").clone().show());
	}	   
    //***** actions (end) *****		    
}        
  
var routerActionMap = {  
    "Stop Router": {
        api: "stopRouter",            
        isAsyncJob: true,
        asyncJobResponse: "stoprouterresponse",
        inProcessText: "Stopping Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    },
    "Start Router": {
        api: "startRouter",            
        isAsyncJob: true,
        asyncJobResponse: "startrouterresponse",
        inProcessText: "Starting Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    },
    "Reboot Router": {
        api: "rebootRouter",           
        isAsyncJob: true,
        asyncJobResponse: "rebootrouterresponse",
        inProcessText: "Rebooting Router....",
        afterActionSeccessFn: routerAfterDetailsTabAction
    }
}   