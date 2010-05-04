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
package com.vmops.agent.resource.virtualnetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.SocketChannel;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;

import com.vmops.agent.api.Answer;
import com.vmops.agent.api.Command;
import com.vmops.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.vmops.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.vmops.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.vmops.agent.api.routing.DhcpEntryCommand;
import com.vmops.agent.api.routing.IPAssocCommand;
import com.vmops.agent.api.routing.LoadBalancerCfgCommand;
import com.vmops.agent.api.routing.SavePasswordCommand;
import com.vmops.agent.api.routing.SetFirewallRuleCommand;
import com.vmops.agent.api.routing.UserDataCommand;
import com.vmops.utils.NumbersUtil;
import com.vmops.utils.component.Manager;
import com.vmops.utils.script.OutputInterpreter;
import com.vmops.utils.script.Script;

/**
 * VirtualNetworkResource controls and configures virtual networking
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *  }
 **/
@Local(value={VirtualRoutingResource.class})
public class VirtualRoutingResource implements Manager {
    private static final Logger s_logger = Logger.getLogger(VirtualRoutingResource.class);
    private String _savepasswordPath; 	// This script saves a random password to the DomR file system
    private String _ipassocPath;
    private String _publicIpAddress;
    private String _firewallPath;
    private String _loadbPath;
    private String _dhcpEntryPath;
    private String _userDataPath;
    private String _publicEthIf;
    private String _privateEthIf;

    
	private int _timeout;
	private int _startTimeout;
	private String _scriptsDir;
	private String _name;
	private int _sleep;
	private int _retry;
	private int _port;

    public Answer executeRequest(final Command cmd) {
        try {
        	if (cmd instanceof SetFirewallRuleCommand) {
                return execute((SetFirewallRuleCommand)cmd);
            }else if (cmd instanceof LoadBalancerCfgCommand) {
                return execute((LoadBalancerCfgCommand)cmd);
            } else if (cmd instanceof IPAssocCommand) {
                return execute((IPAssocCommand)cmd);
            } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
            	return execute((CheckConsoleProxyLoadCommand)cmd);
            } else if(cmd instanceof WatchConsoleProxyLoadCommand) {
            	return execute((WatchConsoleProxyLoadCommand)cmd);
            }  else if (cmd instanceof SavePasswordCommand) {
            	return execute((SavePasswordCommand)cmd);
            }  else if (cmd instanceof DhcpEntryCommand) {
            	return execute((DhcpEntryCommand)cmd);
            } else if (cmd instanceof UserDataCommand) {
            	return execute ((UserDataCommand)cmd);
            } else {
            	return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    protected Answer execute(UserDataCommand cmd) {
    	File tmpFile = null;
    	try {
    		tmpFile = File.createTempFile("udata_", null);
    		FileOutputStream outStream = new FileOutputStream(tmpFile);
    		byte [] data = Base64.decodeBase64(cmd.getUserData());//userdata is supplied in url-safe unchunked mode
    		outStream.write(data); 
    		outStream.close();
    	} catch (IOException e) {
    		String tmpDir = System.getProperty("java.io.tmpdir");
    		s_logger.warn("Failed to create temporary file: is " + tmpDir + " full?", e);
    		return new Answer(cmd, false, "Failed to create or write to temporary file: is " + tmpDir + " full? " + e.getMessage() );
    	}

    	final Script command  = new Script(_userDataPath, _timeout, s_logger);
    	command.add("-r", cmd.getRouterPrivateIpAddress());
    	command.add("-v", cmd.getVmIpAddress());
    	command.add("-d", tmpFile.getAbsolutePath());
    	command.add("-n", cmd.getVmName());

    	final String result = command.execute();
    	if (tmpFile != null) {
    		boolean deleted = tmpFile.delete();
    		if (!deleted) {
    			s_logger.warn("Failed to clean up temp file after sending userdata");
    			tmpFile.deleteOnExit();
    		}
    	}
    	return new Answer(cmd, result==null, result);
	}

	protected Answer execute(final LoadBalancerCfgCommand cmd) {
    	
		File tmpCfgFile = null;
    	try {
    		String cfgFilePath = "";
    		String routerIP = null;
    		
    		if (cmd.getRouterIp() != null) {
    			tmpCfgFile = File.createTempFile(cmd.getRouterIp().replace('.', '_'), "cfg");
				final PrintWriter out
			   	= new PrintWriter(new BufferedWriter(new FileWriter(tmpCfgFile)));
				for (int i=0; i < cmd.getConfig().length; i++) {
					out.println(cmd.getConfig()[i]);
				}
				out.close();
				cfgFilePath = tmpCfgFile.getAbsolutePath();
				routerIP = cmd.getRouterIp();
    		}
			
			final String result = setLoadBalancerConfig(cfgFilePath,
											      cmd.getAddFwRules(), cmd.getRemoveFwRules(),
											      routerIP);
			
			return new Answer(cmd, result == null, result);
		} catch (final IOException e) {
			return new Answer(cmd, false, e.getMessage());
		} finally {
			if (tmpCfgFile != null) {
				tmpCfgFile.delete();
			}
		}
	}

    protected Answer execute(final IPAssocCommand cmd) {
        final String result = assignPublicIpAddress(cmd.getRouterName(), cmd.getRouterIp(), cmd.getPublicIp(), cmd.isAdd(), cmd.isSourceNat(), cmd.getVlanId(), cmd.getVlanGateway(), cmd.getVlanNetmask());
        if (result != null) {
            return new Answer(cmd, false, result);
        }
        return new Answer(cmd);
	}

	private String setLoadBalancerConfig(final String cfgFile,
			final String[] addRules, final String[] removeRules, String routerIp) {
		
		if (routerIp == null) routerIp = "none";
		
        final Script command = new Script(_loadbPath, _timeout, s_logger);
        
        command.add("-i", routerIp);
        command.add("-f", cfgFile);
        
        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
        	for (int i=0; i< addRules.length; i++) {
        		sb.append(addRules[i]).append(',');
        	}
        	command.add("-a", sb.toString());
        }
        
        sb = new StringBuilder();
        if (removeRules.length > 0) {
        	for (int i=0; i< removeRules.length; i++) {
        		sb.append(removeRules[i]).append(',');
        	}
        	command.add("-d", sb.toString());
        }
        
        return command.execute();
	}

    protected synchronized Answer execute(final SavePasswordCommand cmd) {
    	final String password = cmd.getPassword();
    	final String routerPrivateIPAddress = cmd.getRouterPrivateIpAddress();
    	final String vmName = cmd.getVmName();
    	final String vmIpAddress = cmd.getVmIpAddress();
    	final String local =  vmName;

    	// Run save_password_to_domr.sh
        final String result = savePassword(routerPrivateIPAddress, vmIpAddress, password, local);
        if (result != null) {
        	return new Answer(cmd, false, "Unable to save password to DomR.");
        } else {
        	return new Answer(cmd);
        }
    }
    
    protected synchronized Answer execute (final DhcpEntryCommand cmd) {
    	final Script command  = new Script(_dhcpEntryPath, _timeout, s_logger);
    	command.add("-r", cmd.getRouterPrivateIpAddress());
    	command.add("-v", cmd.getVmIpAddress());
    	command.add("-m", cmd.getVmMac());
    	command.add("-n", cmd.getVmName());

    	final String result = command.execute();
    	return new Answer(cmd, result==null, result);
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




    public synchronized String savePassword(final String privateIpAddress, final String vmIpAddress, final String password, final String localPath) {
    	final Script command  = new Script(_savepasswordPath, _startTimeout, s_logger);
    	command.add("-r", privateIpAddress);
    	command.add("-v", vmIpAddress);
    	command.add("-p", password);
    	command.add(localPath);

    	return command.execute();
    }

    
    public String assignPublicIpAddress(final String vmName, final long id, final String vnet, final String privateIpAddress, final String macAddress, final String publicIpAddress) {

        final Script command = new Script(_ipassocPath, _timeout, s_logger);
        command.add("-A");
        command.add("-f"); //first ip is source nat ip
        command.add("-r", vmName);
        command.add("-i", privateIpAddress);
        command.add("-a", macAddress);
        command.add("-l", publicIpAddress);

        return command.execute();
    }

    public String assignPublicIpAddress(final String vmName, final String privateIpAddress, final String publicIpAddress, final boolean add, final boolean sourceNat, final String vlanId, final String vlanGateway, final String vlanNetmask) {

        final Script command = new Script(_ipassocPath, _timeout, s_logger);
        if (add) {
        	command.add("-A");
        } else {
        	command.add("-D");
        }
        if (sourceNat) {
        	command.add("-f");
        }
        command.add("-i", privateIpAddress);
        command.add("-l", publicIpAddress);
        command.add("-r", vmName);
        
        command.add("-n", vlanNetmask);
        
        if (vlanId != null) {
        	command.add("-v", vlanId);
        	command.add("-g", vlanGateway);
        }

        return command.execute();
    }

    public String setFirewallRules(final boolean enable, final String routerName, final String routerIpAddress, final String protocol,
    							   final String publicIpAddress, final String publicPort, final String privateIpAddress, final String privatePort,
    							   String oldPrivateIP,  String oldPrivatePort, String vlanNetmask) {
        
    	if (routerIpAddress == null) {
        	s_logger.warn("SetFirewallRuleCommand did nothing because Router IP address was null when creating rule for public IP: " + publicIpAddress);
            return null;    
        }
    	
    	if (oldPrivateIP == null) oldPrivateIP = "";
    	if (oldPrivatePort == null) oldPrivatePort = "";
    	
        final Script command = new Script(_firewallPath, _timeout, s_logger);
        
        command.add(enable ? "-A" : "-D");
        command.add("-P", protocol);
        command.add("-l", publicIpAddress);
        command.add("-p", publicPort);
        command.add("-n", routerName);
        command.add("-i", routerIpAddress);
        command.add("-r", privateIpAddress);
        command.add("-d", privatePort);
        command.add("-N", vlanNetmask);
        command.add("-w", oldPrivateIP);
        command.add("-x", oldPrivatePort);
        
        return command.execute();
    }
    
    private boolean isBridgeExists(String bridgeName) {
    	Script command = new Script("/bin/sh", _timeout);
    	command.add("-c");
    	command.add("brctl show|grep " + bridgeName);
    	final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
    	String result = command.execute(parser);
    	if (result != null || parser.getLine() == null) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    private void deleteBridge(String brName) {
    	Script cmd = new Script("/bin/sh", _timeout);
    	cmd.add("-c");
    	cmd.add("ifconfig " + brName + " down;brctl delbr " + brName);
    	cmd.execute();
    }
    
    private boolean isDNSmasqRunning(String dnsmasqName) {
    	Script cmd = new Script("/bin/sh", _timeout);
    	cmd.add("-c");
    	cmd.add("ls -l /var/run/libvirt/network/" + dnsmasqName + ".pid");
    	String result = cmd.execute();
    	if (result != null) {
    		return false;
    	} else
    		return true;
    }
    
    private void stopDnsmasq(String dnsmasqName) {
    	Script cmd = new Script("/bin/sh", _timeout);
    	cmd.add("-c");
    	cmd.add("kill -9 `cat /var/run/libvirt/network/"  + dnsmasqName +".pid`");
    	cmd.execute();
    }
    
    public void cleanupPrivateNetwork(String privNwName, String privBrName){
    	if (isDNSmasqRunning(privNwName)) {
    		stopDnsmasq(privNwName);
    	}
    	if (isBridgeExists(privBrName)) {
    		deleteBridge(privBrName);
    	}
    }
    
    protected Answer execute(final SetFirewallRuleCommand cmd) {
        final String result = setFirewallRules(cmd.isEnable(),
        								 cmd.getRouterName(),
                                         cmd.getRouterIpAddress(),
                                         cmd.getProtocol().toLowerCase(),
                                         cmd.getPublicIpAddress(),
                                         cmd.getPublicPort(),
                                         cmd.getPrivateIpAddress(),
                                         cmd.getPrivatePort(),
                                         cmd.getOldPrivateIP(),
                                         cmd.getOldPrivatePort(),
                                         cmd.getVlanNetmask());
                                         
        return new Answer(cmd, result == null, result);
    }
    
    protected String getDefaultScriptsDir() {
        return "scripts/network/domr/dom0";
    }
    
    protected String findScript(final String script) {
        return Script.findScript(_scriptsDir, script);
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
    	_name = name;

        _scriptsDir = (String)params.get("domr.scripts.dir");
        if (_scriptsDir == null) {
        	if(s_logger.isInfoEnabled())
        		s_logger.info("VirtualRoutingResource _scriptDir can't be initialized from domr.scripts.dir param, use default" );
            _scriptsDir = getDefaultScriptsDir();
        }
        
    	if(s_logger.isInfoEnabled())
    		s_logger.info("VirtualRoutingResource _scriptDir to use: " + _scriptsDir);
        
        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 120) * 1000;
        
        value = (String)params.get("start.script.timeout");
        _startTimeout = NumbersUtil.parseInt(value, 360) * 1000;
        
        value = (String)params.get("ssh.sleep");
        _sleep = NumbersUtil.parseInt(value, 5) * 1000;
        
        value = (String)params.get("ssh.retry");
        _retry = NumbersUtil.parseInt(value, 24);
        
        value = (String)params.get("ssh.port");
        _port = NumbersUtil.parseInt(value, 3922);
        
        _ipassocPath = findScript("ipassoc.sh");
        if (_ipassocPath == null) {
            throw new ConfigurationException("Unable to find the ipassoc.sh");
        }
        s_logger.info("ipassoc.sh found in " + _ipassocPath);

        _publicIpAddress = (String)params.get("public.ip.address");
        if (_publicIpAddress != null) {
            s_logger.warn("Incoming public ip address is overriden.  Will always be using the same ip address: " + _publicIpAddress);
        }

        _firewallPath = findScript("firewall.sh");
        if (_firewallPath == null) {
            throw new ConfigurationException("Unable to find the firewall.sh");
        }

        _loadbPath = findScript("loadbalancer.sh");
        if (_loadbPath == null) {
            throw new ConfigurationException("Unable to find the loadbalancer.sh");
        }

        _savepasswordPath = findScript("save_password_to_domr.sh");
        if(_savepasswordPath == null) {
        	throw new ConfigurationException("Unable to find save_password_to_domr.sh");
        }
        
        _dhcpEntryPath = findScript("dhcp_entry.sh");
        if(_dhcpEntryPath == null) {
        	throw new ConfigurationException("Unable to find dhcp_entry.sh");
        }
        
        _userDataPath = findScript("user_data.sh");
        if(_userDataPath == null) {
        	throw new ConfigurationException("Unable to find user_data.sh");
        }
        
        _publicEthIf = (String)params.get("public.network.device");
        if (_publicEthIf == null) {
            _publicEthIf = "xenbr1";
        }
        _publicEthIf = _publicEthIf.toLowerCase();

        _privateEthIf = (String)params.get("private.network.device");
        if (_privateEthIf == null) {
        	_privateEthIf = "xenbr0";
        }
        _privateEthIf = _privateEthIf.toLowerCase();
        
        return true;
    }


    public String connect(final String ipAddress) {
    	return connect(ipAddress, _port);
    }

    public String connect(final String ipAddress, final int port) {
        for (int i = 0; i <= _retry; i++) {
            SocketChannel sch = null;
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to connect to " + ipAddress);
                }
                sch = SocketChannel.open();
                sch.configureBlocking(true);
                
                final InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (final IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not connect to " + ipAddress);
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (final IOException e) {}
                }
            }
            try {
                Thread.sleep(_sleep);
            } catch (final InterruptedException e) {
            }
        }
        
        s_logger.debug("Unable to logon to " + ipAddress);
        
        return "Unable to connect";
    }
    
    
	@Override
	public String getName() {
		return _name;
	}



	@Override
	public boolean start() {
		return true;
	}



	@Override
	public boolean stop() {
		return true;
	}


}


