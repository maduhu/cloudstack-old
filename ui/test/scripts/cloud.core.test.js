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

// Version: @VERSION@

var rootDomainId = 1;

var systemUserId = 1;
var adminUserId = 2;

var keycode_Enter = 13;

var activeDialogs = new Array();
function activateDialog(dialog) {
	activeDialogs[activeDialogs.length] = dialog;
	
	//bind Enter-Key-pressing event handler to the dialog 	
	dialog.keypress(function(event) {
	    if(event.keyCode == keycode_Enter) 	        
	        $('[aria-labelledby$='+dialog.attr("id")+']').find(":button:first").click();	    
	});
}
function removeDialogs() {
	for (var i = 0; i < activeDialogs.length; i++) {
		activeDialogs[i].remove();
	}
	activeDialogs = new Array();
}

function toRole(type) {
	if (type == "0") {
		return "User";
	} else if (type == "1") {
		return "Admin";
	} else if (type == "2") {
		return "Domain-Admin";
	}
}

// Validation functions
function validateString(label, field, errMsgField, isOptional) {  
    var isValid = true;
    var errMsg = "";
    var value = field.val();     
	if (isOptional!=true && (value == null || value.length == 0)) {	 //required field   
	    errMsg = label + " is a required value. ";	   
		isValid = false;		
	} 	
	else if (value!=null && value.length >= 255) {	    
	    errMsg = label + " must be less than 255 characters";	   
		isValid = false;		
	} 	
	else if(value.indexOf('"')!=-1) {
	    errMsg = "Double quotes are not allowed.";	   
		isValid = false;	
	}
	showError(isValid, field, errMsgField, errMsg);	
	return isValid;
}

function cleanErrMsg(field, errMsgField) {
    showError(true, field, errMsgField);
}	

function showError(isValid, field, errMsgField, errMsg) {    
	if(isValid) {
	    errMsgField.text("").hide();
	    field.addClass("text").removeClass("error_text");
	}
	else {
	    errMsgField.text(errMsg).show();
	    field.removeClass("text").addClass("error_text");	
	}
}

// others
function trim(val) {
    return val.replace(/^\s*/, "").replace(/\s*$/, "");
}

// Prevent cross-site-script(XSS) attack. 
// used right before adding user input to the DOM tree. e.g. DOM_element.html(sanitizeXSS(user_input));  
function sanitizeXSS(val) {     
    if(val == null)
        return val; 
    val = val.replace(/</g, "&lt;");  //replace < whose unicode is \u003c     
    val = val.replace(/>/g, "&gt;");  //replace > whose unicode is \u003e  
    return val;
}

// FUNCTION: Handles AJAX error callbacks.  You can pass in an optional function to 
// handle errors that are not already handled by this method.  
function handleError(xmlHttp, handleErrorCallback) {
	// User Not authenticated
	if (xmlHttp.status == 401) {
		$("#dialog_session_expired").dialog("open");
	} else if (handleErrorCallback != undefined) {
		handleErrorCallback();
	} else {
		var start = xmlHttp.responseText.indexOf("h1") + 3;
		var end = xmlHttp.responseText.indexOf("</h1");
		var errorMsg = xmlHttp.responseText.substring(start, end);
		$("#dialog_error").html("<p><b>Encountered an error:</b></p><br/><p>"+sanitizeXSS(errorMsg.substring(errorMsg.indexOf("-")+2))+"</p>").dialog("open");
	}
}



// Timezones
var timezones = new Object();
timezones['Etc/GMT+12']='[UTC-12:00] GMT-12:00';
timezones['Etc/GMT+11']='[UTC-11:00] GMT-11:00';
timezones['Pacific/Samoa']='[UTC-11:00] Samoa Standard Time';
timezones['Pacific/Honolulu']='[UTC-10:00] Hawaii Standard Time';
timezones['US/Alaska']='[UTC-09:00] Alaska Standard Time';
timezones['America/Los_Angeles']='[UTC-08:00] Pacific Standard Time';
timezones['Mexico/BajaNorte']='[UTC-08:00] Baja California';
timezones['US/Arizona']='[UTC-07:00] Arizona';
timezones['US/Mountain']='[UTC-07:00] Mountain Standard Time';
timezones['America/Chihuahua']='[UTC-07:00] Chihuahua, La Paz';
timezones['America/Chicago']='[UTC-06:00] Central Standard Time';
timezones['America/Costa_Rica']='[UTC-06:00] Central America';
timezones['America/Mexico_City']='[UTC-06:00] Mexico City, Monterrey';
timezones['Canada/Saskatchewan']='[UTC-06:00] Saskatchewan';
timezones['America/Bogota']='[UTC-05:00] Bogota, Lima';
timezones['America/New_York']='[UTC-05:00] Eastern Standard Time';
timezones['America/Caracas']='[UTC-04:00] Venezuela Time';
timezones['America/Asuncion']='[UTC-04:00] Paraguay Time';
timezones['America/Cuiaba']='[UTC-04:00] Amazon Time';
timezones['America/Halifax']='[UTC-04:00] Atlantic Standard Time';
timezones['America/La_Paz']='[UTC-04:00] Bolivia Time';
timezones['America/Santiago']='[UTC-04:00] Chile Time';
timezones['America/St_Johns']='[UTC-03:30] Newfoundland Standard Time';
timezones['America/Araguaina']='[UTC-03:00] Brasilia Time';
timezones['America/Argentina/Buenos_Aires']='[UTC-03:00] Argentine Time';
timezones['America/Cayenne']='[UTC-03:00] French Guiana Time';
timezones['America/Godthab']='[UTC-03:00] Greenland Time';
timezones['America/Montevideo']='[UTC-03:00] Uruguay Time]';
timezones['Etc/GMT+2']='[UTC-02:00] GMT-02:00';
timezones['Atlantic/Azores']='[UTC-01:00] Azores Time';
timezones['Atlantic/Cape_Verde']='[UTC-01:00] Cape Verde Time';
timezones['Africa/Casablanca']='[UTC] Casablanca';
timezones['Etc/UTC']='[UTC] Coordinated Universal Time';
timezones['Atlantic/Reykjavik']='[UTC] Reykjavik';
timezones['Europe/London']='[UTC] Western European Time';
timezones['CET']='[UTC+01:00] Central European Time';
timezones['Europe/Bucharest']='[UTC+02:00] Eastern European Time';
timezones['Africa/Johannesburg']='[UTC+02:00] South Africa Standard Time';
timezones['Asia/Beirut']='[UTC+02:00] Beirut';
timezones['Africa/Cairo']='[UTC+02:00] Cairo';
timezones['Asia/Jerusalem']='[UTC+02:00] Israel Standard Time';
timezones['Europe/Minsk']='[UTC+02:00] Minsk';
timezones['Europe/Moscow']='[UTC+03:00] Moscow Standard Time';
timezones['Africa/Nairobi']='[UTC+03:00] Eastern African Time';
timezones['Asia/Karachi']='[UTC+05:00] Pakistan Time';
timezones['Asia/Kolkata']='[UTC+05:30] India Standard Time';
timezones['Asia/Bangkok']='[UTC+05:30] Indochina Time';
timezones['Asia/Shanghai']='[UTC+08:00] China Standard Time';
timezones['Asia/Kuala_Lumpur']='[UTC+08:00] Malaysia Time';
timezones['Australia/Perth']='[UTC+08:00] Western Standard Time (Australia)';
timezones['Asia/Taipei']='[UTC+08:00] Taiwan';
timezones['Asia/Tokyo']='[UTC+09:00] Japan Standard Time';
timezones['Asia/Seoul']='[UTC+09:00] Korea Standard Time';
timezones['Australia/Adelaide']='[UTC+09:30] Central Standard Time (South Australia)';
timezones['Australia/Darwin']='[UTC+09:30] Central Standard Time (Northern Territory)';
timezones['Australia/Brisbane']='[UTC+10:00] Eastern Standard Time (Queensland)';
timezones['Australia/Canberra']='[UTC+10:00] Eastern Standard Time (New South Wales)';
timezones['Pacific/Guam']='[UTC+10:00] Chamorro Standard Time';
timezones['Pacific/Auckland']='[UTC+12:00] New Zealand Standard Time';

$(document).ready(function() {
	function showTestTab() {
		// Manage Events 
		mainContainer.load("content/tab_test.html", function() {	 
		
			//*** user (begin) ********************************************		
			function userJSONToTemplate(json, template) {   
				(index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");              
				template.attr("id", "user_"+json.id).data("userId", json.id);		            		    				    
				template.find("#user_role").text(toRole(json.accounttype));
				template.find("#user_userid").text(json.id);	        
				template.find("#user_username").text(json.username);
				template.find("#user_account").text(json.account);
				template.find("#user_domain").text(json.domain);	       
				template.find("#user_disabled").text(((json.isdisabled == "true") ? "Yes" : "No"));
				template.find("#user_email").text(json.email);
				template.find("#user_firstname").text(json.firstname);
				template.find("#user_lastname").text(json.lastname);
				template.find("#user_timezone").text(timezones[json.timezone]);
				template.data("timezone", json.timezone);
				if(json.id==systemUserId || json.id==adminUserId) 
					 template.find("#delete_link").hide();
			}
				
			function listUsers() {      
				var submenuContent = $("#submenu_content_user");        
				index = 0;	    
				$.ajax({
					data: "command=listUsers&response=json",
					dataType: "json",
					success: function(json) {			              	        
						var users = json.listusersresponse.user;
						var grid = submenuContent.find("#grid_content").empty();
						var template = $("#user_template");			   			
						if (users != null && users.length > 0) {				        			        
							for (var i = 0; i < users.length; i++) {
								var newTemplate = template.clone(true);
								userJSONToTemplate(users[i], newTemplate);
								grid.append(newTemplate.show());						   
							} 
						}		                         		    						
					},
					error: function(XMLHttpRequest) {				    			
						handleError(XMLHttpRequest);					
					}
				});
			}
			  
			$("#submenu_user").bind("click",function(event){	
			 
				$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
				currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
				currentSubMenu = $(this);  	
					
				var submenuContent = $("#submenu_content_user").show();
				$("#submenu_content_domain").hide();   		    
									
				listUsers();	
				
				return false;
			}); 
										 
			var dialogAddUser = $("#dialog_add_user");	   
			var addUserAccountTypeField = dialogAddUser.find("#add_user_account_type");
			var addUserDomainField = dialogAddUser.find("#add_user_domain");
							
			activateDialog($("#dialog_add_user").dialog({ 
				width:450,
				autoOpen: false,
				modal: true,
				zIndex: 2000
			}));
			
			$("#user_action_new").bind("click", function(event) {			   
				populateDomainField(addUserDomainField);	
								
				var submenuContent = $("#submenu_content_user");   		    
							
				dialogAddUser
				.dialog('option', 'buttons', { 					
					"Create": function() { 	
					    var thisDialog = $(this);
									    			
						// validate values
						var isValid = true;					
						isValid &= validateString("User name", thisDialog.find("#add_user_username"), thisDialog.find("#add_user_username_errormsg"), false);   //required
						isValid &= validateString("Password", thisDialog.find("#add_user_password"), thisDialog.find("#add_user_password_errormsg"), false);    //required	
						isValid &= validateString("Email", thisDialog.find("#add_user_email"), thisDialog.find("#add_user_email_errormsg"), true);              //optional	
						isValid &= validateString("First name", thisDialog.find("#add_user_firstname"), thisDialog.find("#add_user_firstname_errormsg"), true); //optional	
						isValid &= validateString("Last name", thisDialog.find("#add_user_lastname"), thisDialog.find("#add_user_lastname_errormsg"), true);    //optional	
						isValid &= validateString("Account", thisDialog.find("#add_user_account"), thisDialog.find("#add_user_account_errormsg"), true);        //optional	
						if (!isValid) return;
													
						var template  = $("#user_template").clone(true);	
						var loadingImg = template.find(".adding_loading");		
                        var rowContainer = template.find("#row_container");    	                               
                        loadingImg.find(".adding_text").text("Adding....");	
                        loadingImg.show();  
                        rowContainer.hide();                                   
                        submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    
																								
						var username = thisDialog.find("#add_user_username").val();
						var password = $.md5(thisDialog.find("#add_user_password").val());
						var email = thisDialog.find("#add_user_email").val();
						if(email == "")
							email = username;
						var firstname = thisDialog.find("#add_user_firstname").val();
						if(firstname == "")
							firstname = username;
						var lastname = thisDialog.find("#add_user_lastname").val();
						if(lastname == "")
							lastname = username;
						var account = thisDialog.find("#add_user_account").val();					
						if(account == "")
							account = username;
						var accountType = thisDialog.find("#add_user_account_type").val();					
						var domainId = thisDialog.find("#add_user_domain").val();
						if (parseInt(domainId) > rootDomainId && parseInt(accountType) == 1) {
							accountType = 2; // Change to domain admin 
						}
						
						var moreCriteria = [];
						var timezone = thisDialog.find("#add_user_timezone").val();	
						if(timezone != null && timezone.length > 0)
			                moreCriteria.push("&timezone="+encodeURIComponent(timezone));	
			        						
						thisDialog.dialog("close");					
											
						$.ajax({
							type: "POST",
							data: "command=createUser&username="+encodeURIComponent(username)+"&password="+encodeURIComponent(password)+"&email="+encodeURIComponent(email)+"&firstname="+encodeURIComponent(firstname)+"&lastname="+encodeURIComponent(lastname)+"&account="+account+"&accounttype="+accountType+"&domainid="+domainId+moreCriteria.join("")+"&response=json",
							dataType: "json",
							async: false,
							success: function(json) {								    					
								userJSONToTemplate(json.createuserresponse.user[0], template);	
								loadingImg.hide();  
                                rowContainer.show();    				
							},			
	                        error: function(XMLHttpResponse) {		                   
		                        handleError(XMLHttpResponse);	
		                        template.slideUp("slow", function(){ $(this).remove(); } );							    
	                        }								
						});						
					},
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				return false;
			});   
			
			activateDialog($("#dialog_edit_user").dialog({ 
				width:450,
				autoOpen: false,
				modal: true,
				zIndex: 2000
			}));
			
			activateDialog($("#dialog_change_password").dialog({ 
				width:450,
				autoOpen: false,
				modal: true,
				zIndex: 2000
			}));
			
			$("#user_template").bind("click", function(event) {		   
				var template = $(this);
				var id = template.data("userId");
				var target = event.target;
				switch(target.id) {
					case "edit_link":	    
						var dialogEditUser = $("#dialog_edit_user");		           
						
						dialogEditUser.find("#edit_user_username").val(template.find("#user_username").text());   
						if(id==systemUserId || id==adminUserId)
							dialogEditUser.find("#edit_user_username").attr("disabled", true);
						else
							dialogEditUser.find("#edit_user_username").attr("disabled", false);    
											
						dialogEditUser.find("#edit_user_email").val(template.find("#user_email").text());
						dialogEditUser.find("#edit_user_firstname").val(template.find("#user_firstname").text());
						dialogEditUser.find("#edit_user_lastname").val(template.find("#user_lastname").text());						
						dialogEditUser.find("#edit_user_timezone").val(template.data("timezone"));
						
						dialogEditUser
						.dialog('option', 'buttons', { 							
							"Save": function() { 	
							    var thisDialog = $(this);
												
								// validate values						   
								var isValid = true;					
								isValid &= validateString("User name", thisDialog.find("#edit_user_username"), thisDialog.find("#edit_user_username_errormsg"), false);	  //required					      
								isValid &= validateString("Email", thisDialog.find("#edit_user_email"), thisDialog.find("#edit_user_email_errormsg"), true);	          //optional
								isValid &= validateString("First name", thisDialog.find("#edit_user_firstname"), thisDialog.find("#edit_user_firstname_errormsg"), true); //optional
								isValid &= validateString("Last name", thisDialog.find("#edit_user_lastname"), thisDialog.find("#edit_user_lastname_errormsg"), true);	  //optional	   	
								if (!isValid) return;
															
								var username = trim(thisDialog.find("#edit_user_username").val());							  
								var email = trim(thisDialog.find("#edit_user_email").val());
								var firstname = trim(thisDialog.find("#edit_user_firstname").val());
								var lastname = trim(thisDialog.find("#edit_user_lastname").val()); 	
								var timezone = trim(thisDialog.find("#edit_user_timezone").val()); 							
																
								thisDialog.dialog("close");
								$.ajax({
									data: "command=updateUser&id="+id+"&username="+encodeURIComponent(username)+"&email="+encodeURIComponent(email)+"&firstname="+encodeURIComponent(firstname)+"&lastname="+encodeURIComponent(lastname)+"&timezone="+encodeURIComponent(timezone)+"&response=json",
									dataType: "json",
									success: function(json) {								      						    					
										template.find("#user_username").text(username);
										template.find("#user_email").text(email);
										template.find("#user_firstname").text(firstname);
										template.find("#user_lastname").text(lastname);		
										template.find("#user_timezone").text(timezones[timezone]);		
										template.data("timezone", timezone);
									}
								});
							},
							"Cancel": function() { 
								$(this).dialog("close"); 
							} 
						}).dialog("open");
						
						break;
						
					case "change_password_link":	    
						var dialogChangePassword = $("#dialog_change_password");			             
						dialogChangePassword.find("#change_password_password1").val("");         
						
						$("#dialog_change_password")
						.dialog('option', 'buttons', { 							
							"Save": function() { 	
							    var thisDialog = $(this);
							    					
								// validate values						   
								var isValid = true;					      	
								isValid &= validateString("Password", thisDialog.find("#change_password_password1"), thisDialog.find("#change_password_password1_errormsg"), false); //required						      		   	
								if (!isValid) return;
																						
								var password = $.md5(encodeURIComponent((thisDialog.find("#change_password_password1").val())));						   					
																
								thisDialog.dialog("close");
								$.ajax({
									data: "command=updateUser&id="+id+"&password="+password+"&response=json",
									dataType: "json",
									success: function(json) {							       				
									}
								});
							},
							"Cancel": function() { 
								$(this).dialog("close"); 
							} 
						}).dialog("open");
						
						break;
							
					case "delete_link":           
						$.ajax({
							data: "command=deleteUser&id="+id+"&response=json",
							dataType: "json",
							success: function(json) {						    
								$("#user_"+id).slideUp("slow", function() { 
									$(this).remove() 
								});
							}
						});           
						break;
				}
				return false;  //event.preventDefault() + event.stopPropogation()
			});        	
			//*** user (end) **********************************************
			 
			//*** domain (begin) ********************************************* 
			function domainJSONToTemplate(json, template) {   
				(index++ % 2 == 0)? template.addClass("smallrow_even"): template.addClass("smallrow_odd");		                
				template.attr("id", "domain_"+json.id).data("domainId", json.id);	  
				template.find("#domain_id").text(json.id);	   
				template.find("#domain_name").text(json.name);
				template.find("#domain_level").text(json.level);
				template.find("#parent_domain_name").text(json.parentdomainname);	  
				if(json.id == rootDomainId)
					template.find("#domain_links").hide();
			}
				
			function listDomains() {      
				var submenuContent = $("#submenu_content_domain");        
				index = 0;	    
				$.ajax({
					data: "command=listDomains&response=json",
					dataType: "json",
					success: function(json) {			            	        
						var domains = json.listdomainsresponse.domain;
						var grid = submenuContent.find("#grid_content").empty();
						var template = $("#domain_template");			   			
						if (domains != null && domains.length > 0) {				        			        
							for (var i = 0; i < domains.length; i++) {
								var newTemplate = template.clone(true);
								domainJSONToTemplate(domains[i], newTemplate);
								grid.append(newTemplate.show());						   
							} 
						}		                         		    						
					},
					error: function(XMLHttpRequest) {				    		    			
						handleError(XMLHttpRequest);					
					}
				});
			}
			
			$("#submenu_domain").bind("click",function(event){	
			 
				$(this).toggleClass("submenu_links_on").toggleClass("submenu_links_off");
				currentSubMenu.toggleClass("submenu_links_off").toggleClass("submenu_links_on");
				currentSubMenu = $(this);  	
					
				var submenuContent = $("#submenu_content_domain").show();
				$("#submenu_content_user").hide();   		    
									
				listDomains();
				
				return false;
			}); 
			
			activateDialog($("#dialog_add_domain").dialog({ 
				width:450,
				autoOpen: false,
				modal: true,
				zIndex: 2000
			}));
			
			$("#domain_action_new").bind("click", function(event) {
				var dialogAddDomain = $("#dialog_add_domain");
				populateDomainField(dialogAddDomain.find("#add_domain_parent"));
				
				var submenuContent = $("#submenu_content_domain");   			
				
				dialogAddDomain
				.dialog('option', 'buttons', { 					
					"Create": function() { 	
					    var thisDialog = $(this);
					    				    			
						// validate values
						var isValid = true;					
						isValid &= validateString("Name", thisDialog.find("#add_domain_name"), thisDialog.find("#add_domain_name_errormsg"));					
						if (!isValid) return;
												
						var template = $("#domain_template").clone(true);		
						var loadingImg = template.find(".adding_loading");		
                        var rowContainer = template.find("#row_container");    	                               
                        loadingImg.find(".adding_text").text("Adding....");	
                        loadingImg.show();  
                        rowContainer.hide();                                   
                        submenuContent.find("#grid_content").prepend(template.fadeIn("slow"));    
																	
						var name = trim(thisDialog.find("#add_domain_name").val());											
						var parentDomainId = thisDialog.find("#add_domain_parent").val();
						var moreCriteria = [];	
						if(parentDomainId!=null)
							moreCriteria.push("&parentdomainid="+parentDomainId);	
															
						thisDialog.dialog("close");					
								
						$.ajax({
							data: "command=createDomain&name="+encodeURIComponent(name)+moreCriteria.join("")+"&response=json",
							dataType: "json",
							async: false,
							success: function(json) {	   															       
								domainJSONToTemplate(json.createdomainresponse.domain[0], template);	
								loadingImg.hide();  
                                rowContainer.show();    				
							},			
	                        error: function(XMLHttpResponse) {		                   
		                        handleError(XMLHttpResponse);	
		                        template.slideUp("slow", function(){ $(this).remove(); } );							    
	                        }								
						});
						
					}, 
					"Cancel": function() { 
						$(this).dialog("close"); 
					} 
				}).dialog("open");
				return false;
			});    
			
			activateDialog($("#dialog_edit_domain").dialog({ 
				width:450,
				autoOpen: false,
				modal: true,
				zIndex: 2000
			}));
			
			$("#domain_template").bind("click", function(event) {		   
				var template = $(this);
				var id = template.data("domainId");
				var target = event.target;
				switch(target.id) {	        
					case "edit_link":
						var dialogEditDomain = $("#dialog_edit_domain");			            	           
						dialogEditDomain.find("#edit_domain_name").val(template.find("#domain_name").text());		           
					   
						dialogEditDomain
						.dialog('option', 'buttons', { 							
							"Save": function() { 
							    var thisDialog = $(this);
							     						
								// validate values						   
								var isValid = true;					
								isValid &= validateString("Name", thisDialog.find("#edit_domain_name"), thisDialog.find("#edit_domain_name_errormsg"));						       	   	
								if (!isValid) return;
															
								var name = trim(thisDialog.find("#edit_domain_name").val());																				
								
								thisDialog.dialog("close");
								$.ajax({
									data: "command=updateDomain&id="+id+"&name="+encodeURIComponent(name)+"&response=json",
									dataType: "json",
									success: function(json) {								      						    					
										template.find("#domain_name").text(name);								  	
									}
								});
							}, 
							"Cancel": function() { 
								$(this).dialog("close"); 
							} 
						}).dialog("open");
						
						break;       
					
					case "delete_link":           
						$.ajax({
							data: "command=deleteDomain&id="+id+"&response=json",
							dataType: "json",
							success: function(json) {						    
								$("#domain_"+id).slideUp("slow", function() { 
									$(this).remove() 
								});
							}
						});           
						break;
				}
			});  	
			//*** domain (end) *********************************************** 
			
			//*** shared (begin) *********************************************
			function populateDomainField(dropDownBox) {              
				$.ajax({
					data: "command=listDomains&response=json",
					dataType: "json",
					success: function(json) {			           
						var domains = json.listdomainsresponse.domain;								
						dropDownBox.empty();			        				       		            							
						if (domains != null && domains.length > 0) {
							for (var i = 0; i < domains.length; i++) 				
								dropDownBox.append("<option value='" + domains[i].id + "'>" + sanitizeXSS(domains[i].name) + "</option>"); 		
						}	
						dropDownBox.val(rootDomainId);				    	
					}
				});		    
			}	
			//*** shared (end) ***********************************************
			 
			var currentSubMenu = $("#submenu_user");           	   
			var index;         
			listUsers();	
		});
	}


    g_mySession = $.cookie("JSESSIONID");
	g_role = $.cookie("role");
	
	// We will be dropping all the main tab content into this container
	mainContainer = $("#maincontentarea");
	
	// Default AJAX Setup
	$.ajaxSetup({
		url: "/client/api",
		dataType: "json",
		cache: false,
		error: function(XMLHttpRequest) {
			handleError(XMLHttpRequest);
		},
		beforeSend: function(XMLHttpRequest) {
			if (g_mySession != $.cookie("JSESSIONID")) {
				$("#dialog_session_expired").dialog("open");
				return false;
			} else {
				return true;
			}
		}		
	});
	
	// FUNCTION: logs the user out
	function closeWindow() {
		window.close();
	}	
	
	$("#close_link").bind("click", function(event) {
		closeWindow();
	});
	
	// Dialogs
	$("#dialog_confirmation").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000
	});
	
	$("#dialog_info").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	
	$("#dialog_alert").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_alert").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_alert").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "Close": function() { $(this).dialog("close"); } }
	});
	$("#dialog_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_session_expired").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { closeWindow(); $(this).dialog("close"); } }
	});
	$("#dialog_session_expired").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_session_expired").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("#dialog_server_error").dialog({ 
		autoOpen: false,
		modal: true,
		zIndex: 2000,
		buttons: { "OK": function() { $(this).dialog("close"); } }
	});
	$("#dialog_server_error").siblings(".ui-widget-header").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	$("#dialog_server_error").siblings(".ui-dialog-buttonpane").find(".ui-state-default").css("background", "url('/client/css/images/ui-bg_errorglass_30_ffffff_1x400.png') repeat-x scroll 50% 50% #393939");
	
	$("body").css("background", "#FFF repeat top left");
	$("#mainmaster").show();
	showTestTab();
});



