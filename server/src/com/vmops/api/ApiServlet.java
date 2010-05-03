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

package com.vmops.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.vmops.user.Account;
import com.vmops.user.UserContext;
import com.vmops.utils.Pair;
import com.vmops.utils.exception.VmopsRuntimeException;

@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());

    private ApiServer _apiServer = null;

    public ApiServlet() {
        super();
        _apiServer = ApiServer.getInstance();
        if (_apiServer == null) {
            throw new VmopsRuntimeException("ApiServer not initialized");
        }
    }

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
	    processRequest(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp);
    }

    @SuppressWarnings("unchecked")
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
        try {
            Map<String, Object[]> params = new HashMap<String, Object[]>();
            params.putAll(req.getParameterMap());
            HttpSession session = req.getSession(false);

            // get the response format since we'll need it in a couple of places
            String responseType = BaseCmd.RESPONSE_TYPE_XML;
            Object[] responseTypeParam = params.get("response");
            if (responseTypeParam != null) {
                responseType = (String)responseTypeParam[0];
            }

            Object[] commandObj = params.get("command");
            if (commandObj != null) {
                String command = (String)commandObj[0];
                if ("logout".equalsIgnoreCase(command)) {
                    // if this is just a logout, invalidate the session and return
                    if (session != null) {
                        String userIdStr = (String)session.getAttribute("userId");
                        if (userIdStr != null) {
                            _apiServer.logoutUser(Long.parseLong(userIdStr));
                        }
                        session.invalidate();
                    }
                    writeResponse(resp, getLogoutSuccessResponse(responseType), false, responseType);
                    return;
                } else if ("login".equalsIgnoreCase(command)) {
                    // if this is a login, authenticate the user and return
                    if (session != null) session.invalidate();
                	session = req.getSession(true);
                    String[] username = (String[])params.get("username");
                    String[] password = (String[])params.get("password");
                    String[] domainIdArr = (String[])params.get("domainid");
                    if (domainIdArr == null) {
                    	domainIdArr = (String[])params.get("domainId");
                    }
                    String[] domainName = (String[])params.get("domain");
                    Long domainId = null;
                    if ((domainIdArr != null) && (domainIdArr.length > 0)) {
                        domainId = new Long(Long.parseLong(domainIdArr[0]));
                    }
                    String domain = null;
                    if (domainName != null) {
                    	domain = domainName[0];
                    }
                    if ((username != null) && (password != null)) {
                        List<Pair<String, Object>> sessionParams = _apiServer.loginUser(username[0], password[0], domainId, domain);
                        if (sessionParams != null) {
                            for (Pair<String, Object> sessionParam : sessionParams) {
                                session.setAttribute(sessionParam.first(), sessionParam.second());
                            }
                            String loginResponse = getLoginSuccessResponse(session, responseType);
                            writeResponse(resp, loginResponse, false, responseType);
                            return;
                        } else {
                            // TODO:  fall through to API key, or just fail here w/ auth error? (HTTP 401)
                            session.invalidate();
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "failed to authenticated user, check username/password are correct");
                            return;
                        }
                    }
                }
            }

            boolean isNew = ((session == null) ? true : session.isNew());

            Object accountObj = null;
            String userId = null;
            String account = null;
            String domainId = null;
            
            if (!isNew) {
                userId = (String)session.getAttribute("userId");
                account = (String)session.getAttribute("account");
                domainId = (String)session.getAttribute("domainId");
                accountObj = session.getAttribute("accountobj");

                // Do a sanity check here to make sure the user hasn't already been deleted
                if ((userId != null) && (account != null) && (accountObj != null) && _apiServer.verifyUser(Long.valueOf(userId))) {
                    String[] command = (String[])params.get("command");
                    if (command == null) {
                        s_logger.info("missing command, ignoring request...");
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no command specified");
                        return;
                    }   
                } else {
                    // Clear out the variables we retrieved from the session and invalidate the session.  This ensures
                    // we won't allow a request across management server restarts if the userId was serialized to the
                    // stored session
                    userId = null;
                    account = null;
                    accountObj = null;
                    session.invalidate();
                }
            }

            setupUserContext(userId, accountObj, session != null ? session.getId() : null);

            if (_apiServer.verifyRequest(params, userId)) {
            	if (accountObj != null) {
            		Account userAccount = (Account)accountObj;
            		if (userAccount.getType() == Account.ACCOUNT_TYPE_NORMAL) {
            			params.put(BaseCmd.Properties.USER_ID.getName(), new String[] { userId });
                        params.put(BaseCmd.Properties.ACCOUNT.getName(), new String[] { account });
                        params.put(BaseCmd.Properties.DOMAIN_ID.getName(), new String[] { domainId });
                		params.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { accountObj });
            		} else {
            			params.put(BaseCmd.Properties.USER_ID.getName(), new String[] { userId });
            			params.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { accountObj });
            		}
            	}
            	try {
            		String response = _apiServer.handleRequest(params, false, responseType);
            		writeResponse(resp, response != null ? response : "", false, responseType);
            	} catch (ServerApiException se) {
            		resp.sendError(se.getErrorCode(), se.getDescription());
            	}
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials and/or request signature");
            }

            // cleanup user context to prevent from being peeked in other request context
            UserContext.unregisterContext();
        } catch (IOException ioex) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("exception processing request: " + ioex);
            }
        } catch (Exception ex) {
            s_logger.error("unknown exception writing api response", ex);
        }
    }
    
    private void setupUserContext(String userIdStr, Object account, String sessionId) {
    	Long userId = null;
    	Long accountId = null;
    	
    	if(userIdStr != null)
    		userId = Long.parseLong(userIdStr);
    	if(account != null)
    		accountId = ((Account)account).getId();

    	UserContext.registerContext(userId, accountId, sessionId, false);
    }

    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
    private void writeResponse(HttpServletResponse resp, String response, boolean isError, String responseType) {
        try {
            // is text/plain sufficient for XML and JSON?
            if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                resp.setContentType("text/javascript");
            } else {
                resp.setContentType("text/xml");
            }
            resp.setStatus(isError? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK);
            byte[] respBytes = response.getBytes();
            resp.setContentLength(respBytes.length);
            OutputStream os = resp.getOutputStream();
            os.write(respBytes);
            os.flush();
            os.close();
            resp.flushBuffer();
        } catch (IOException ioex) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("exception writing response: " + ioex);
            }
        } catch (Exception ex) {
            s_logger.error("unknown exception writing api response", ex);
        }
    }

    private String getLoginSuccessResponse(HttpSession session, String responseType) {
        StringBuffer sb = new StringBuffer();
        int inactiveInterval = session.getMaxInactiveInterval();
        String account = (String)session.getAttribute("account");
        String userName = (String)session.getAttribute("userName");
        String firstName = (String)session.getAttribute("firstName");
        String lastName = (String)session.getAttribute("lastName");
        String domainid = (String)session.getAttribute("domainId");
        String networkType = (String)session.getAttribute("networkType");
        String hypervisorType = (String)session.getAttribute("hypervisorType");
        Short type = (Short)session.getAttribute("type");
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"loginresponse\" : { \"timeout\" : \"" + inactiveInterval + "\",\"username\" : \"" + userName + "\", \"firstname\" : \"" + firstName + "\", \"lastname\" : \"" + lastName + "\",\"account\" : \"" + account + "\", \"domainid\" : \"" + domainid + "\", \"type\" : \"" + type + "\", \"networktype\" : \"" + networkType + "\", \"hypervisortype\" : \"" + hypervisorType + "\" } }");
        } else {
            sb.append("<loginresponse><timeout>" + inactiveInterval + "</timeout><username>" + userName + "</username><firstname>" + firstName + "</firstname><lastname>" + lastName + "</lastname><account>" + account + "</account><domainid>" + domainid + "</domainid><type>"+ type + "</type><networktype>"+ networkType + "</networktype><hypervisortype>" + hypervisorType + "</hypervisortype></loginresponse>");
        }
        return sb.toString();
    }

    private String getLogoutSuccessResponse(String responseType) {
        StringBuffer sb = new StringBuffer();
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"logoutresponse\" : { \"description\" : \"success\" } }");
        } else {
            sb.append("<logoutresponse><description>success</description></logoutresponse>");
        }
        return sb.toString();
    }
}
