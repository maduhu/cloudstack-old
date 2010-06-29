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
package com.cloud.agent.resource.consoleproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.Agent.ExitStatus;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConsoleAccessAuthenticationAnswer;
import com.cloud.agent.api.ConsoleAccessAuthenticationCommand;
import com.cloud.agent.api.ConsoleProxyLoadReportCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingComputingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupProxyCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.exception.AgentControlChannelException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;
import com.cloud.vm.State;

/**
 * 
 * I don't want to introduce extra cross-cutting concerns into console proxy process, as it involves configurations like
 * zone/pod, agent auto self-upgrade etc. I also don't want to introduce more module dependency issues into our build system,
 * cross-communication between this resource and console proxy will be done through reflection. As a result, come out with
 * following solution to solve the problem of building a communication channel between consoole proxy and management server.
 *
 * We will deploy an agent shell inside console proxy VM, and this agent shell will launch current console proxy from within
 * this special server resource, through it console proxy can build a communication channel with management server.
 * 
 */
public class ConsoleProxyResource extends ServerResourceBase implements ServerResource {
    static final Logger s_logger = Logger.getLogger(ConsoleProxyResource.class);
    
    private final Properties _properties = new Properties();
    private Thread _consoleProxyMain = null;
    long _proxyVmId;
    int _proxyPort;
    
    @Override
    public Answer executeRequest(final Command cmd) {
        if (cmd instanceof CheckConsoleProxyLoadCommand) {
            return execute((CheckConsoleProxyLoadCommand)cmd);
        } else if(cmd instanceof WatchConsoleProxyLoadCommand) {
            return execute((WatchConsoleProxyLoadCommand)cmd);
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if(cmd instanceof CheckHealthCommand) {
        	return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }
    
    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
    }

    private Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            final InputStream is = conn.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final StringBuilder sb2 = new StringBuilder();
            String line = null;
            try {
                while ((line = reader.readLine()) != null)
                    sb2.append(line + "\n");
                result = sb2.toString();
            } catch (final IOException e) {
                success = false;
            } finally {
                try {
                    is.close();
                } catch (final IOException e) {
                    s_logger.warn("Exception when closing , console proxy address : " + proxyManagementIp);
                    success = false;
                }
            }
        } catch(final IOException e) {
            s_logger.warn("Unable to open console proxy command port url, console proxy address : " + proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success, result);
    }
    
    @Override
    protected String getDefaultScriptsDir() {
        return null;
    }
    
    public Type getType() {
        return Host.Type.ConsoleProxy;
    }
    
    @Override
    public synchronized StartupCommand [] initialize() {
        final StartupProxyCommand cmd = new StartupProxyCommand();
        fillNetworkInformation(cmd);
        cmd.setProxyPort(_proxyPort);
        cmd.setProxyVmId(_proxyVmId);
        return new StartupCommand[] {cmd};
    }

    @Override
    public void disconnected() {
    }
    
    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingComputingCommand(Type.ConsoleProxy, id, new HashMap<String, State>());
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        for(Map.Entry<String, Object> entry : params.entrySet()) {
        	_properties.put(entry.getKey(), entry.getValue());
        }
        
        String value = (String)params.get("consoleproxy.httpListenPort");

        value = (String)params.get("premium");
        if(value != null && value.equals("premium"))
        	_proxyPort = 443;
        else
        	_proxyPort = 80;

        value = (String)params.get("proxy_vm");
        _proxyVmId = NumbersUtil.parseLong(value, 0);

    	String  eth1ip = (String)params.get("eth1ip");
        if (eth1ip != null) {
        	params.put("private.network.device", "eth1");
        }
        
        String  localgw = (String)params.get("localgw");
        if (localgw != null) {
        	String eth1mask = (String)params.get("eth1mask");
        	String internalDns1 = (String)params.get("dns1");
        	String internalDns2 = (String)params.get("dns2");

        	if (internalDns1 == null) {
        		s_logger.warn("No DNS entry found during configuration of NfsSecondaryStorage");
        	} else {
        		addRouteToInternalIp(localgw, eth1ip, eth1mask, internalDns1);
        	}
        	
        	String mgmtHost = (String)params.get("host");
        	addRouteToInternalIp(localgw, eth1ip, eth1mask, mgmtHost);
        	if (internalDns2 != null) {
            	addRouteToInternalIp(localgw, eth1ip, eth1mask, internalDns2);
        	}
        }
        
        if(s_logger.isInfoEnabled())
        	s_logger.info("Receive proxyVmId in ConsoleProxyResource configuration as " + _proxyVmId);
        
        launchConsoleProxy();
        return true;
    }
    
    private void addRouteToInternalIp(String localgw, String eth1ip, String eth1mask, String destIp) {
    	if (destIp == null) {
    		s_logger.debug("addRouteToInternalIp: destIp is null");
			return;
		}
    	if (!NetUtils.isValidIp(destIp)){
    		s_logger.warn(" destIp is not a valid ip address destIp=" + destIp);
    		return;
    	}
    	boolean inSameSubnet = false;
    	if (eth1ip != null && eth1mask != null) {
    		inSameSubnet = NetUtils.sameSubnet(eth1ip, destIp, eth1mask);
    	} else {
			s_logger.warn("addRouteToInternalIp: unable to determine same subnet: eth1ip=" + eth1ip + ", dest ip=" + destIp + ", eth1mask=" + eth1mask);
    	}
    	if (inSameSubnet) {
			s_logger.info("addRouteToInternalIp: dest ip " + destIp + " is in the same subnet as eth1 ip " + eth1ip);
    		return;
    	}
    	Script command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("ip route delete " + destIp);
    	command.execute();
		command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("ip route add " + destIp + " via " + localgw);
    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in configuring route to internal ip err=" + result );
    	} else {
			s_logger.info("addRouteToInternalIp: added route to internal ip=" + destIp + " via " + localgw);
    	}
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    private void launchConsoleProxy() {
    	final Object resource = this;
    	
		_consoleProxyMain = new Thread(new Runnable() {
			public void run() {
				try {
					Class<?> consoleProxyClazz = Class.forName("com.cloud.consoleproxy.ConsoleProxy");
					try {
						Method method = consoleProxyClazz.getMethod("startWithContext", Properties.class, Object.class);
						method.invoke(null, _properties, resource);
					} catch (SecurityException e) {
						s_logger.error("Unable to launch console proxy due to SecurityException");
						System.exit(ExitStatus.Error.value());
					} catch (NoSuchMethodException e) {
						s_logger.error("Unable to launch console proxy due to NoSuchMethodException");
						System.exit(ExitStatus.Error.value());
					} catch (IllegalArgumentException e) {
						s_logger.error("Unable to launch console proxy due to IllegalArgumentException");
						System.exit(ExitStatus.Error.value());
					} catch (IllegalAccessException e) {
						s_logger.error("Unable to launch console proxy due to IllegalAccessException");
						System.exit(ExitStatus.Error.value());
					} catch (InvocationTargetException e) {
						s_logger.error("Unable to launch console proxy due to InvocationTargetException");
						System.exit(ExitStatus.Error.value());
					}
				} catch (final ClassNotFoundException e) {
					s_logger.error("Unable to launch console proxy due to ClassNotFoundException");
					System.exit(ExitStatus.Error.value());
				}
			}
		}, "Console-Proxy-Main");
		_consoleProxyMain.setDaemon(true);
		_consoleProxyMain.start();
    }

    public boolean authenticateConsoleAccess(String vmId, String sid) {
    	ConsoleAccessAuthenticationCommand cmd = new ConsoleAccessAuthenticationCommand(vmId, sid);
    	
    	try {
			AgentControlAnswer answer = getAgentControl().sendRequest(cmd, 10000);
			if(answer != null) {
				return ((ConsoleAccessAuthenticationAnswer)answer).succeeded();
			} else {
				s_logger.error("Authentication failed for vm: " + vmId + " with sid: " + sid);
			}
			
		} catch (AgentControlChannelException e) {
			s_logger.error("Unable to send out console access authentication request due to " + e.getMessage(), e);
		}
    	
    	return false;
    }
    
    public void reportLoadInfo(String gsonLoadInfo) {
    	ConsoleProxyLoadReportCommand cmd = new ConsoleProxyLoadReportCommand(_proxyVmId, gsonLoadInfo);
    	try {
			getAgentControl().postRequest(cmd);
			
			if(s_logger.isDebugEnabled())
				s_logger.debug("Report proxy load info, proxy : " + _proxyVmId + ", load: " + gsonLoadInfo);
		} catch (AgentControlChannelException e) {
			s_logger.error("Unable to send out load info due to " + e.getMessage(), e);
		}
    }
}
