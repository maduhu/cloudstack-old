/*
This file is meant to help with implementing single signon integration.  If you are using the
cloud.com default UI, there is no need to touch this file.
*/

/*
This callback function is called when either the session has timed out for the user,
the session ID has been changed (i.e. another user logging into the UI via a different tab), 
or it's the first time the user has come to this page.
*/
function onLogoutCallback() {
	// Returning true means the LOGIN page will be show.  If you wish to redirect the user
	// to different login page, this is where you would do that.
	return true;
}

/*
For single signon purposes, you should set three cookies so that the UI does not force a logout.

JSESSIONID : jsessionid generated by the mgmt server
username : username
role : 0 = User, 1 = ROOT Admin, 2 = Domain Admin

All three values can be retrieved by making a successful AJAX login call to the mgmt server.

If you cannot set these cookies before redirecting to this mgmt UI.  You can uncomment the code
below and as long as you can retrieve the username, password, and domain in another manner (i.e. through
a querystring), you can execute the login command below.
*/

/*
$(document).ready(function() {
	//var username = encodeURIComponent($("#account_username").val());
	//var password = encodeURIComponent($("#account_password").val());
	//var domain = encodeURIComponent($("#account_domain").val());
	//var url = "/client/api?command=login&username="+username+"&password="+password+"&domain="+domain+"&response=json";
	
	// Test URL
	var url = "/client/api?command=login&username=admin&password=password&domain=ROOT&response=json";
	$.ajax({
		url: url,
		dataType: "json",
		async: false,
		success: function(json) {
			$.cookie('username', json.loginresponse.username);
			$.cookie('role', json.loginresponse.type);
			$.cookie('networktype', json.loginresponse.networktype);
			$.cookie('hypervisortype', json.loginresponse.hypervisortype);
		},
		error: function() {
			// This means the login failed.  You should redirect to your login page.
		},
		beforeSend: function(XMLHttpRequest) {
			return true;
		}
	});
});
*/