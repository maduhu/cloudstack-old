<%@ page import="java.util.Date" %>

<%
long milliseconds = new Date().getTime();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<meta http-equiv='cache-control' content='no-cache'>  
	<meta http-equiv='expires' content='0'>  
	<meta http-equiv='pragma' content='no-cache'>

	<title>cloud.com - User Console</title>
	
	<!--  Style Sheet -->
	<link rel= "stylesheet" href="css/main.css" type="text/css" />
	<link rel= "stylesheet" href="css/jquery-ui.custom.css" type="text/css" />
	<link rel= "stylesheet" href="css/logger.css" type="text/css" />
	
	<!-- 
		Custom Style Sheet 
		- Default Cloud.com styling of the site.  This file contains the easiest portion of the site
        that can be styled to your companie's need such as logo, top navigation, and dialogs.		
	-->
	<link rel= "stylesheet" href="css/cloud_custom.css" type="text/css" />
	
	<!-- Javascripts -->
	
	<!--
		If you wish to use the minified version for production, uncomment the script
		tag below and remove/comment out the rest of the javascript below.
	-->
	<!-- <script type="text/javascript" src="scripts/cloud.core.min.js"></script> -->

	<script type="text/javascript" src="scripts/jquery.min.js"></script>
	<script type="text/javascript" src="scripts/jquery-ui.custom.min.js"></script>
	<script type="text/javascript" src="scripts/date.js"></script>
	<script type="text/javascript" src="scripts/jquery.cookies.js"></script>
	<script type="text/javascript" src="scripts/jquery.timers.js"></script>
	<script type="text/javascript" src="scripts/jquery.md5.js"></script>
	
	<!-- Callback script for Single Signon -->
	<script type="text/javascript" src="scripts/cloud.core.callbacks.js?t=<%=milliseconds%>"></script>
	
	<!-- cloud.com scripts -->
	<script type="text/javascript" src="scripts/cloud.logger.js?t=<%=milliseconds%>"></script>
	<script type="text/javascript" src="scripts/cloud.core.js?t=<%=milliseconds%>"></script>
	<script type="text/javascript" src="scripts/cloud.core.init.js?t=<%=milliseconds%>"></script>
		
	<!-- Favicon -->
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
	
	<!-- Title -->
	<title>Cloud.com CloudStack</title>
</head>
<body>
<div id="logoutpage" style="display:none;">
	<div id="logoutpage_mainmaster">
    	<div id="logoutpage_mainbox">
        	<div class="logoutpage_mainbox_top">
            	<div class="logout_logocontainer">
                	<div class="logout_logo"></div>
                </div>
                <div class="logout_titlecontainer">
					<h1>Welcome to Console &hellip;</h1>
                </div>
            </div>
        </div>
		<div class="logoutpage_mainbox_mid">
			<div class="logoutpage_formcontent">
				<form id="loginForm" name="loginForm" method="post" action="#">
					<ol>
						<li><label for="user_name">Username</label>
							<input class="text" type="text" name="account_username" id="account_username" />
						</li>
						<li><label for="password">Password</label>
							<input class="text" type="password" name="account_password" id="account_password" AUTOCOMPLETE="off" />
						</li>
						<li><label for="Domain">Domain</label>
							<input class="text" type="text" name="account_domain" id="account_domain" value="/"/>
						</li>
					</ol>
					<div class="loginbutton_box">
						<a class="loginbutton" id="loginbutton" href="#"></a>
						<p style="display:none"></p>
					</div>
				</form>
			</div>
		</div>
		<div class="logoutpage_mainbox_bot"></div>
    </div>
</div>

<div id="overlay_black"></div>
<div id="mainmaster">

<!-- Header -->
<div id="header">
	<div class="header_top">
        <div class="header_topleft">
            <a class="logo" href="#"></a>
        </div>
		<div class="header_topright">
			<div class="usernav_container">
				<div class="usernav_containerleft"></div>
				<div class="usernav_containermid">
					<div class="usernav">
						Welcome, <span id="header_username"></span> | <a id="logoutaccount_link" href="#">Log Out</a>
					</div>
				</div> 
			</div>
			<div class="usernav_containerright"></div>
		</div>
    </div>
    <div class="header_bot" id="global_nav">
        <div class="globalnav_container" id="menutab_role_user" style="display:none">
			<div id="menutab_dashboard_user" class="menutab_off">Dashboard</div>
            <div id="menutab_vm" class="menutab_off">Instances</div>
			<div id="menutab_storage" class="menutab_off">Storage</div>			
			<div id="menutab_networking" class="menutab_off">Network</div>
			<div id="menutab_templates" class="menutab_off">Templates</div>
			<div id="menutab_events" class="menutab_off">Events</div>
        </div>
		<div class="globalnav_container" id="menutab_role_root" style="display:none">
			<div id="menutab_dashboard_root" class="admin_menutab_off">Dashboard</div>
            <div id="menutab_vm" class="admin_menutab_off">Instances</div>
			<div id="menutab_hosts" class="admin_menutab_off">Hosts</div>
			<div id="menutab_storage" class="admin_menutab_off">Storage</div>			
			<div id="menutab_networking" class="admin_menutab_off">Network</div>
			<div id="menutab_templates" class="admin_menutab_off">Templates</div>
			<div id="menutab_accounts" class="admin_menutab_off">Accounts</div>
			<div id="menutab_domain" class="admin_menutab_off">Domains</div>
			<div id="menutab_events" class="admin_menutab_off">Events</div>
            <div id="menutab_configuration" class="admin_menutab_off">Configuration</div>
        </div>
		<div class="globalnav_container" id="menutab_role_domain" style="display:none">
			<div id="menutab_dashboard_domain" class="admin_menutab_off">Dashboard</div>
            <div id="menutab_vm" class="admin_menutab_off">Instances</div>
			<div id="menutab_storage" class="admin_menutab_off">Storage</div>			
			<div id="menutab_networking" class="admin_menutab_off">Network</div>
			<div id="menutab_templates" class="admin_menutab_off">Templates</div>
			<div id="menutab_accounts" class="admin_menutab_off">Accounts</div>
			<div id="menutab_domain" class="admin_menutab_off">Domains</div>
			<div id="menutab_events" class="admin_menutab_off">Events</div>
        </div>
    </div>
</div>
<!-- END Header -->

<!-- submenus -->
<div class="submenu_tab">
	<div class="title_testlinks" id="launch_test" style="display:none">Launch Test Provisioning Tool</div>
</div>

<!-- Main Content Area -->
<div id="maincontentarea"></div>
<!-- END Main Content Area -->

<!-- Dialogs -->
<div id="dialog_confirmation" title="Confirmation" style="display:none"></div>
<div id="dialog_info" title="Info" style="display:none"></div>
<div id="dialog_alert" title="Alert" style="display:none"></div>
<div id="dialog_error" title="Error" style="display:none"></div>
<div id="dialog_session_expired" title="Session Expired" style="display:none">
	<p>Your session has expired.  Please click 'OK' to return to the login screen.</p>
</div>

</div>
</body>
</html>
