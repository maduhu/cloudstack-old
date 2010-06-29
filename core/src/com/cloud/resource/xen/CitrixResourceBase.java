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
package com.cloud.resource.xen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.CreateZoneVlanAnswer;
import com.cloud.agent.api.CreateZoneVlanCommand;
import com.cloud.agent.api.DeleteSnapshotBackupAnswer;
import com.cloud.agent.api.DeleteSnapshotBackupCommand;
import com.cloud.agent.api.DeleteSnapshotsDirCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartConsoleProxyAnswer;
import com.cloud.agent.api.StartConsoleProxyCommand;
import com.cloud.agent.api.StartRouterAnswer;
import com.cloud.agent.api.StartRouterCommand;
import com.cloud.agent.api.StartSecStorageVmAnswer;
import com.cloud.agent.api.StartSecStorageVmCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.ValidateSnapshotAnswer;
import com.cloud.agent.api.ValidateSnapshotCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.WatchNetworkAnswer;
import com.cloud.agent.api.WatchNetworkCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRuleCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.CreatePrivateTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.ManageVolumeAnswer;
import com.cloud.agent.api.storage.ManageVolumeCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ShareAnswer;
import com.cloud.agent.api.storage.ShareCommand;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.HypervisorType;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePool.StoragePoolType;
import com.cloud.storage.Volume.StorageResourceType;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachineName;
import com.trilead.ssh2.SCPClient;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostCpu;
import com.xensource.xenapi.HostMetrics;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMGuestMetrics;
import com.xensource.xenapi.XenAPIObject;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.JoiningHostCannotContainSharedSrs;
import com.xensource.xenapi.Types.VmPowerState;
import com.xensource.xenapi.Types.XenAPIException;

/**
 * Encapsulates the interface to the XenServer API.
 * 
 */
@Local(value = ServerResource.class)
public abstract class CitrixResourceBase implements ServerResource {
    private static final Logger                    s_logger                  = Logger.getLogger(CitrixResourceBase.class);
    protected static final XenServerConnectionPool _connPool                 = XenServerConnectionPool.getInstance();
    protected static final String                  SR_MOUNT_BASE             = "/var/run/sr-mount/";
    protected static final int                     MB                        = 1024 * 1024;
    String                                         _name;
    String                                         _username;
    String                                         _password;
    String                                         _scriptsDir               = "scripts/vm/storage/xenserver";
    protected final int                            _retry                    = 24;
    protected final int                            _sleep                    = 10000;
    long                                           _dcId;
    String                                         _pod;
    protected HashMap<String, State>               _vms                      = new HashMap<String, State>(20);
    String                                         _patchPath;
    String                                         _privateNetworkName;
    String                                         _publicNetworkName;
    String                                         _storageNetworkName1;
    String                                         _storageNetworkName2;
    protected int                                  _wait;
    protected IAgentControl                        _agentControl;
    Map<String, String>                            _domrIPMap                = new ConcurrentHashMap<String, String>();
    private final HashMap<String, String>          s_osType                  = new HashMap<String, String>();

    final XenServerHost                            _host                     = new XenServerHost();

    // Guest and Host Performance Statistics
    boolean                                        _collectHostStats         = true;
    String                                         _consolidationFunction    = "AVERAGE";
    int                                            _pollingIntervalInSeconds = 60;

    StorageLayer                                   _storage;

    protected enum SRType {
        NFS, LVM, ISCSI, ISO, LVMOISCSI;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        public boolean equals(String type) {
            return super.toString().equalsIgnoreCase(type);
        }
    }

    protected static HashMap<Types.VmPowerState, State> s_statesTable;
    protected String                                    _localGateway;
    static {
        s_statesTable = new HashMap<Types.VmPowerState, State>();
        s_statesTable.put(Types.VmPowerState.HALTED, State.Stopped);
        s_statesTable.put(Types.VmPowerState.PAUSED, State.Running);
        s_statesTable.put(Types.VmPowerState.RUNNING, State.Running);
        s_statesTable.put(Types.VmPowerState.SUSPENDED, State.Running);
        s_statesTable.put(Types.VmPowerState.UNKNOWN, State.Unknown);
        s_statesTable.put(Types.VmPowerState.UNRECOGNIZED, State.Unknown);
    }

    protected boolean isRefNull(XenAPIObject object) {
        return (object == null || object.toWireString().equals("OpaqueRef:NULL"));
    }

    @Override
    public void disconnected() {
        s_logger.debug("Logging out of " + _host.uuid);
        if (_host.pool != null) {
            _connPool.disconnect(_host.uuid, _host.pool);
            _host.pool = null;
        }
    }

    protected void destroyStoppedVm() {
        Map<VM, VM.Record> vmentries = null;
        Connection conn = getConnection();
        for (int i = 0; i < 2; i++) {
            try {
                vmentries = VM.getAllRecords(conn);
                break;
            } catch (final Throwable e) {
                s_logger.warn("Unable to get vms", e);
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException ex) {

            }
        }
        if (vmentries == null) {
            return;
        }
        for (Map.Entry<VM, VM.Record> vmentry : vmentries.entrySet()) {
            VM.Record record = vmentry.getValue();
            if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                continue; // Skip DOM0
            }
            if (record.powerState != Types.VmPowerState.HALTED) {
                continue;
            }

            try {
                if (isRefNull(record.affinity) || !record.affinity.getUuid(conn).equals(_host.uuid)) {
                    continue;
                }
                vmentry.getKey().destroy(conn);
            } catch (Exception e) {
                String msg = "VM destroy failed for " + record.nameLabel +
                             " due to " + e.getMessage();
                s_logger.warn(msg, e);
            }
        }
    }

    protected void cleanupDiskMounts() {
        Connection conn = getConnection();

        Map<SR, SR.Record> srs;
        try {
            srs = SR.getAllRecords(conn);
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get the SRs " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get SRs " + e.toString(), e);
        } catch (XmlRpcException e) {
            throw new CloudRuntimeException("Unable to get SRs " + e.getMessage());
        }

        for (Map.Entry<SR, SR.Record> sr : srs.entrySet()) {
            SR.Record rec = sr.getValue();
            if (SRType.NFS.equals(rec.type) || (SRType.ISO.equals(rec.type) && rec.nameLabel.endsWith("iso"))) {
                if (rec.PBDs == null || rec.PBDs.size() == 0) {
                    cleanSR(sr.getKey(), rec);
                    continue;
                }

                for (PBD pbd : rec.PBDs) {

                    if (isRefNull(pbd)) {
                        continue;
                    }
                    PBD.Record pbdr = null;
                    try {
                        pbdr = pbd.getRecord(conn);
                    } catch (XenAPIException e) {
                        s_logger.warn("Unable to get pbd record " + e.toString());
                    } catch (XmlRpcException e) {
                        s_logger.warn("Unable to get pbd record " + e.getMessage());
                    }

                    if (pbdr == null) {
                        continue;
                    }

                    try {
                        if (pbdr.host.getUuid(conn).equals(_host.uuid)) {
                            if (!currentlyAttached(sr.getKey(), rec, pbd, pbdr)) {
                                pbd.unplug(conn);
                                pbd.destroy(conn);
                                cleanSR(sr.getKey(), rec);
                            } else if (!pbdr.currentlyAttached) {
                                pbd.plug(conn);
                            }
                        }

                    } catch (XenAPIException e) {
                        s_logger.warn("Catch XenAPIException due to" + e.toString(), e);
                    } catch (XmlRpcException e) {
                        s_logger.warn("Catch XmlRpcException due to" + e.getMessage(), e);
                    }
                }
            }
        }
    }

    protected Pair<VM, VM.Record> getVmByNameLabel(Connection conn,
                                                   Host host,
                                                   String nameLabel,
                                                   boolean getRecord)
    throws XmlRpcException, XenAPIException
    {
        Set<VM> vms = host.getResidentVMs(conn);
        for (VM vm : vms) {
            VM.Record rec = null;
            String name = null;
            if (getRecord) {
                rec = vm.getRecord(conn);
                name = rec.nameLabel;
            } else {
                name = vm.getNameLabel(conn);
            }
            if (name.equals(nameLabel)) {
                return new Pair<VM, VM.Record>(vm, rec);
            }
        }

        return null;
    }

    protected boolean currentlyAttached(SR sr,
                                        SR.Record rec,
                                        PBD pbd,
                                        PBD.Record pbdr)
    {
        String status = null;
        if (SRType.NFS.equals(rec.type)) {
            status = callHostPlugin("checkMount", "mount", rec.uuid);
        } else if (SRType.LVMOISCSI.equals(rec.type)) {
            String scsiid = pbdr.deviceConfig.get("SCSIid");
            if (scsiid.isEmpty()) {
                return false;
            }
            status = callHostPlugin("checkIscsi", "scsiid", scsiid);
        }

        if (status != null && status.equalsIgnoreCase("1")) {
            s_logger.debug("currently attached " + pbdr.uuid);
            return true;
        } else {
            s_logger.debug("currently not attached " + pbdr.uuid);
            return false;
        }
    }

    protected boolean pingdomr(String host, String port) {
        String status;
        status = callHostPlugin("pingdomr", "host", host, "port", port);

        if (status == null || status.isEmpty()) {
            return false;
        }

        return true;

    }

    protected boolean pingxenserver() {
        String status;
        status = callHostPlugin("pingxenserver");

        if (status == null || status.isEmpty()) {
            return false;
        }

        return true;

    }

    protected String logX(XenAPIObject obj, String msg) {
        return new StringBuilder("Host ").append(_host.ip)
                                         .append(" ")
                                         .append(obj.toWireString())
                                         .append(": ")
                                         .append(msg)
                                         .toString();
    }

    protected void cleanSR(SR sr, SR.Record rec) {
        Connection conn = getConnection();
        if (rec.VDIs != null) {
            for (VDI vdi : rec.VDIs) {

                VDI.Record vdir;
                try {
                    vdir = vdi.getRecord(conn);
                } catch (XenAPIException e) {
                    s_logger.debug("Unable to get VDI: " + e.toString());
                    continue;
                } catch (XmlRpcException e) {
                    s_logger.debug("Unable to get VDI: " + e.getMessage());
                    continue;
                }

                if (vdir.VBDs == null)
                    continue;

                for (VBD vbd : vdir.VBDs) {
                    try {
                        VBD.Record vbdr = vbd.getRecord(conn);
                        VM.Record vmr = vbdr.VM.getRecord(conn);
                        if ((!isRefNull(vmr.residentOn) &&
                            vmr.residentOn.getUuid(conn).equals(_host.uuid)) ||
                            (isRefNull(vmr.residentOn) && !isRefNull(vmr.affinity) && vmr.affinity.getUuid(conn)
                                                                                                                                                                       .equals(_host.uuid))) {
                            if (vmr.powerState != VmPowerState.HALTED && vmr.powerState != VmPowerState.UNKNOWN &&
                                vmr.powerState != VmPowerState.UNRECOGNIZED)
                            {
                                try {
                                    vbdr.VM.hardShutdown(conn);
                                } catch (XenAPIException e) {
                                    s_logger.debug("Shutdown hit error " + vmr.nameLabel + ": " + e.toString());
                                }
                            }
                            try {
                                vbdr.VM.destroy(conn);
                            } catch (XenAPIException e) {
                                s_logger.debug("Destroy hit error " + vmr.nameLabel + ": " + e.toString());
                            } catch (XmlRpcException e) {
                                s_logger.debug("Destroy hit error " + vmr.nameLabel + ": " + e.getMessage());
                            }
                            vbd.destroy(conn);
                            break;
                        }
                    } catch (XenAPIException e) {
                        s_logger.debug("Unable to get VBD: " + e.toString());
                        continue;
                    } catch (XmlRpcException e) {
                        s_logger.debug("Uanbel to get VBD: " + e.getMessage());
                        continue;
                    }
                }
            }
        }

        for (PBD pbd : rec.PBDs) {
            PBD.Record pbdr = null;
            try {
                pbdr = pbd.getRecord(conn);
                pbd.unplug(conn);
                pbd.destroy(conn);
            } catch (XenAPIException e) {
                s_logger.warn("PBD " + ((pbdr != null) ? "(uuid:" + pbdr.uuid + ")" : "") + "destroy failed due to " + e.toString());
            } catch (XmlRpcException e) {
                s_logger.warn("PBD " + ((pbdr != null) ? "(uuid:" + pbdr.uuid + ")" : "") + "destroy failed due to " + e.getMessage());
            }
        }

        try {
            rec = sr.getRecord(conn);
            if (rec.PBDs == null || rec.PBDs.size() == 0) {
                sr.forget(conn);
                return;
            }
        } catch (XenAPIException e) {
            s_logger.warn("Unable to retrieve sr again: " + e.toString(), e);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to retrieve sr again: " + e.getMessage(), e);
        }
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof SetFirewallRuleCommand) {
            return execute((SetFirewallRuleCommand) cmd);
        } else if (cmd instanceof LoadBalancerCfgCommand) {
            return execute((LoadBalancerCfgCommand) cmd);
        } else if (cmd instanceof IPAssocCommand) {
            return execute((IPAssocCommand) cmd);
        } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
            return execute((CheckConsoleProxyLoadCommand) cmd);
        } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
            return execute((WatchConsoleProxyLoadCommand) cmd);
        } else if (cmd instanceof SavePasswordCommand) {
            return execute((SavePasswordCommand) cmd);
        } else if (cmd instanceof DhcpEntryCommand) {
            return execute((DhcpEntryCommand) cmd);
        } else if (cmd instanceof VmDataCommand) {
            return execute((VmDataCommand) cmd);
        } else if (cmd instanceof StartCommand) {
            return execute((StartCommand) cmd);
        } else if (cmd instanceof StartRouterCommand) {
            return execute((StartRouterCommand) cmd);
        } else if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand) cmd);
        } else if (cmd instanceof GetHostStatsCommand) {
            return execute((GetHostStatsCommand) cmd);
        } else if (cmd instanceof GetVmStatsCommand) {
            return execute((GetVmStatsCommand) cmd);
        } else if (cmd instanceof WatchNetworkCommand) {
            return execute((WatchNetworkCommand) cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return execute((CheckHealthCommand) cmd);
        } else if (cmd instanceof StopCommand) {
            return execute((StopCommand) cmd);
        } else if (cmd instanceof RebootRouterCommand) {
            return execute((RebootRouterCommand) cmd);
        } else if (cmd instanceof RebootCommand) {
            return execute((RebootCommand) cmd);
        } else if (cmd instanceof CheckVirtualMachineCommand) {
            return execute((CheckVirtualMachineCommand) cmd);
        } else if (cmd instanceof PrepareForMigrationCommand) {
            return execute((PrepareForMigrationCommand) cmd);
        } else if (cmd instanceof MigrateCommand) {
            return execute((MigrateCommand) cmd);
        } else if (cmd instanceof CreateCommand) {
            return execute((CreateCommand) cmd);
        } else if (cmd instanceof DestroyCommand) {
            return execute((DestroyCommand) cmd);
        } else if (cmd instanceof ShareCommand) {
            return execute((ShareCommand) cmd);
        } else if (cmd instanceof ModifyStoragePoolCommand) {
            return execute((ModifyStoragePoolCommand) cmd);
        } else if (cmd instanceof DeleteStoragePoolCommand) {
            return execute((DeleteStoragePoolCommand) cmd);
        } else if (cmd instanceof ManageVolumeCommand) {
            return execute((ManageVolumeCommand) cmd);
        } else if (cmd instanceof CopyVolumeCommand) {
            return execute((CopyVolumeCommand) cmd);
        } else if (cmd instanceof AttachVolumeCommand) {
            return execute((AttachVolumeCommand) cmd);
        } else if (cmd instanceof AttachIsoCommand) {
            return execute((AttachIsoCommand) cmd);
        } else if (cmd instanceof ValidateSnapshotCommand) {
            return execute((ValidateSnapshotCommand) cmd);
        } else if (cmd instanceof ManageSnapshotCommand) {
            return execute((ManageSnapshotCommand) cmd);
        } else if (cmd instanceof BackupSnapshotCommand) {
            return execute((BackupSnapshotCommand) cmd);
        } else if (cmd instanceof DeleteSnapshotBackupCommand) {
            return execute((DeleteSnapshotBackupCommand) cmd);
        } else if (cmd instanceof CreateVolumeFromSnapshotCommand) {
            return execute((CreateVolumeFromSnapshotCommand) cmd);
        } else if (cmd instanceof DeleteSnapshotsDirCommand) {
            return execute((DeleteSnapshotsDirCommand) cmd);
        } else if (cmd instanceof CreatePrivateTemplateCommand) {
            return execute((CreatePrivateTemplateCommand) cmd);
        } else if (cmd instanceof CreatePrivateTemplateFromSnapshotCommand) {
            return execute((CreatePrivateTemplateFromSnapshotCommand) cmd);
        } else if (cmd instanceof GetStorageStatsCommand) {
            return execute((GetStorageStatsCommand) cmd);
        } else if (cmd instanceof PrimaryStorageDownloadCommand) {
            return execute((PrimaryStorageDownloadCommand) cmd);
        } else if (cmd instanceof StartConsoleProxyCommand) {
            return execute((StartConsoleProxyCommand) cmd);
        } else if (cmd instanceof StartSecStorageVmCommand) {
            return execute((StartSecStorageVmCommand) cmd);
        } else if (cmd instanceof GetVncPortCommand) {
            return execute((GetVncPortCommand) cmd);
        } else if (cmd instanceof SetupCommand) {
            return execute((SetupCommand) cmd);
        } else if (cmd instanceof CreateZoneVlanCommand) {
            return execute((CreateZoneVlanCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else if (cmd instanceof PingTestCommand) {
            return execute((PingTestCommand)cmd);
        } else if (cmd instanceof CheckOnHostCommand) {
            return execute((CheckOnHostCommand)cmd);
        } else if (cmd instanceof ModifySshKeysCommand) {
            return execute((ModifySshKeysCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }
    
    
    protected Answer execute(ModifySshKeysCommand cmd) {       
        String publickey =  cmd.getPubKey();
        String privatekey = cmd.getPrvKey();


        com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_host.ip, 22);
        try {
            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(_username, _password)) {
                throw new Exception("Unable to authenticate");
            }
            SCPClient scp = new SCPClient(sshConnection);
                
            scp.put(publickey.getBytes(), "id_rsa.pub", "/opt/xensource/bin", "0600");
            scp.put(privatekey.getBytes(), "id_rsa", "/opt/xensource/bin", "0600");
            return new Answer(cmd);
            
        } catch (Exception e) {
            String msg = " scp ssh key failed due to " + e.toString() + " - " + e.getMessage();
            s_logger.warn(msg);
        } finally {
            sshConnection.close();
        }
        return new Answer(cmd, false, "modifySshkeys failed");
    }
    
    private boolean doPingTest( final String computingHostIp ) {
        
        String args = "-h " + computingHostIp;
        String result = callHostPlugin("pingtest", "args", args);
        if (result == null || result.isEmpty())
            return false;
        return true;
    }
    
    protected CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
        return new CheckOnHostAnswer(cmd, null, "Not Implmeneted");
    }

    private boolean doPingTest( final String domRIp, final String vmIp ) {
        String args = "-i " + domRIp + " -p " + vmIp;
        String result = callHostPlugin("pingtest", "args", args);
        if (result == null || result.isEmpty())
            return false;
        return true;
    }

    
    private Answer execute(PingTestCommand cmd) {
        boolean result = false;
        final String computingHostIp = cmd.getComputingHostIp(); //TODO, split the command into 2 types

        if (computingHostIp != null) {
            result = doPingTest(computingHostIp);
        } else {
            result = doPingTest(cmd.getRouterIp(), cmd.getPrivateIp());
        }

        if (!result) {
            return new Answer(cmd, false, "PingTestCommand failed");
        }
        return new Answer(cmd);
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        Connection conn = getConnection();
        try {
            Pool pool = Pool.getByUuid(conn, _host.pool);
            Pool.Record poolr = pool.getRecord(conn);

            Host.Record hostr = poolr.master.getRecord(conn);
            if (!_host.uuid.equals(hostr.uuid)) {
                s_logger.debug("Not the master node so just return ok: " + _host.ip);
                return new MaintainAnswer(cmd);
            }
            Map<Host, Host.Record> hostMap = Host.getAllRecords(conn);
            if (hostMap.size() == 1) {
                s_logger.debug("There's no one to take over as master");
                return new MaintainAnswer(cmd, "Only master in the pool");
            }
            Host newMaster = null;
            Host.Record newMasterRecord = null;
            for (Map.Entry<Host, Host.Record> entry : hostMap.entrySet()) {
                if (!_host.uuid.equals(entry.getValue().uuid)) {
                    newMaster = entry.getKey();
                    newMasterRecord = entry.getValue();
                    s_logger.debug("New master for the XenPool is " + newMasterRecord.uuid + " : " + newMasterRecord.address);
                    try {
            _connPool.switchMaster(_host.uuid, _host.pool, conn, newMaster, _username, _password, _wait);
            return new MaintainAnswer(cmd, "New Master is " + newMasterRecord.address);
                    } catch (XenAPIException e) {
                    	s_logger.warn("Unable to switch the new master to " + newMasterRecord.uuid + ": " + newMasterRecord.address + " Trying again...");
                    } catch (XmlRpcException e) {
                        s_logger.warn("Unable to switch the new master to " + newMasterRecord.uuid + ": " + newMasterRecord.address + " Trying again...");
                    }
                }
            }
            return new MaintainAnswer(cmd, false, "Unable to find an appropriate host to set as the new master");
        } catch (XenAPIException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to put server in maintainence mode", e);
            return new MaintainAnswer(cmd, false, e.getMessage());
        }
    }

    protected SetupAnswer execute(SetupCommand cmd) {
        return new SetupAnswer(cmd);
    }

    protected Answer execute(StartSecStorageVmCommand cmd) {

        final String vmName = cmd.getVmName();
        SecondaryStorageVmVO storage = cmd.getSecondaryStorageVmVO();
        try {
            Connection conn = getConnection();

            Network network = Network.getByUuid(conn, _host.privateNetwork);

            String bootArgs = cmd.getBootArgs();
            bootArgs += " zone=" + _dcId;
            bootArgs += " pod=" + _pod;
            bootArgs += " localgw=" + _localGateway;
            String result = startSystemVM(vmName,
                                          storage.getVlanId(),
                                          network,
                                          cmd.getVolumes(),
                                          bootArgs,
                                          storage.getPrivateIpAddress(),
                                          storage.getPrivateMacAddress(),
                                          storage.getPublicIpAddress(),
                                          storage.getPublicMacAddress(),
                                          cmd.getProxyCmdPort(),
                                          storage.getRamSize());
            if (result == null) {
                return new StartSecStorageVmAnswer(cmd);
            }
            return new StartSecStorageVmAnswer(cmd, result);

        } catch (Exception e) {
            String msg = "Exception caught while starting router vm " + vmName + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new StartSecStorageVmAnswer(cmd, msg);
        }
    }

    protected Answer execute(final SetFirewallRuleCommand cmd) {
        String args;

        if (cmd.isEnable()) {
            args = "-A";
        } else {
            args = "-D";
        }

        args += " -P " + cmd.getProtocol().toLowerCase();
        args += " -l " + cmd.getPublicIpAddress();
        args += " -p " + cmd.getPublicPort();
        args += " -n " + cmd.getRouterName();
        args += " -i " + cmd.getRouterIpAddress();
        args += " -r " + cmd.getPrivateIpAddress();
        args += " -d " + cmd.getPrivatePort();
        args += " -N " + cmd.getVlanNetmask();

        String oldPrivateIP = cmd.getOldPrivateIP();
        String oldPrivatePort = cmd.getOldPrivatePort();

        if (oldPrivateIP != null) {
            args += " -w " + oldPrivateIP;
        }

        if (oldPrivatePort != null) {
            args += " -x " + oldPrivatePort;
        }

        String result = callHostPlugin("setFirewallRule", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "SetFirewallRule failed");
        }
        return new Answer(cmd);
    }

    protected Answer execute(final LoadBalancerCfgCommand cmd) {
        String routerIp = cmd.getRouterIp();

        if (routerIp == null) {
            return new Answer(cmd);
        }

        String tmpCfgFilePath = "/tmp/" + cmd.getRouterIp().replace('.', '_') + ".cfg";
        String tmpCfgFileContents = "";
        for (int i = 0; i < cmd.getConfig().length; i++) {
            tmpCfgFileContents += cmd.getConfig()[i];
            tmpCfgFileContents += "\n";
        }

        String result = callHostPlugin("createFile",
                                       "filepath",
                                       tmpCfgFilePath,
                                       "filecontents",
                                       tmpCfgFileContents);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerCfgCommand failed to create HA proxy cfg file.");
        }

        String[] addRules = cmd.getAddFwRules();
        String[] removeRules = cmd.getRemoveFwRules();

        String args = "";
        args += "-i " + routerIp;
        args += " -f " + tmpCfgFilePath;

        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
            for (int i = 0; i < addRules.length; i++) {
                sb.append(addRules[i]).append(',');
            }

            args += " -a " + sb.toString();
        }

        sb = new StringBuilder();
        if (removeRules.length > 0) {
            for (int i = 0; i < removeRules.length; i++) {
                sb.append(removeRules[i]).append(',');
            }

            args += " -d " + sb.toString();
        }

        result = callHostPlugin("setLoadBalancerRule", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "LoadBalancerCfgCommand failed");
        }

        callHostPlugin("deleteFile", "filepath", tmpCfgFilePath);

        return new Answer(cmd);
    }

    protected synchronized Answer execute(final DhcpEntryCommand cmd) {
        String args = "-r " + cmd.getRouterPrivateIpAddress();
        args += " -v " + cmd.getVmIpAddress();
        args += " -m " + cmd.getVmMac();
        args += " -n " + cmd.getVmName();
        String result = callHostPlugin("saveDhcpEntry", "args", args);
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "DhcpEntry failed");
        }
        return new Answer(cmd);
    }

    protected Answer execute(final VmDataCommand cmd) {    	
        String userData = cmd.getUserData();
        userData = (userData == null) ? "none" : userData;
        
        String result = callHostPlugin("vm_data", 
        							   "routerIP", cmd.getRouterPrivateIpAddress(),
        							   "vmIP", cmd.getVmIpAddress(),
        							   "metadata,service-offering", cmd.getServiceOffering(),
        							   "metadata,availability-zone", cmd.getZoneName(),
        							   "metadata,local-ipv4", cmd.getGuestIP(),
        							   "metadata,local-hostname", cmd.getVmName(),
        							   "metadata,public-ipv4", cmd.getRouterPublicIpAddress(),
        							   "metadata,public-hostname", cmd.getRouterPublicIpAddress(),
        							   "metadata,instance-id", cmd.getVmInstanceName(),
        							   "userdata,user-data", userData);        
        
        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "vm_data failed");
        } else {
        	return new Answer(cmd);
        }
       
    }

    protected Answer execute(final SavePasswordCommand cmd) {
        final String password = cmd.getPassword();
        final String routerPrivateIPAddress = cmd.getRouterPrivateIpAddress();
        final String vmName = cmd.getVmName();
        final String vmIpAddress = cmd.getVmIpAddress();
        final String local = vmName;

        // Run save_password_to_domr.sh
        String args = "-r " + routerPrivateIPAddress;
        args += " -v " + vmIpAddress;
        args += " -p " + password;
        args += " " + local;
        String result = callHostPlugin("savePassword", "args", args);

        if (result == null || result.isEmpty()) {
            return new Answer(cmd, false, "savePassword failed");
        }
        return new Answer(cmd);
    }

    protected void assignPublicIpAddress(final String vmName,
                                         final String privateIpAddress,
                                         final String publicIpAddress,
                                         final boolean add,
                                         final boolean firstIP,
                                         final boolean sourceNat,
                                         final String vlanId,
                                         final String vlanGateway,
                                         final String vlanNetmask,
                                         final String vifMacAddress)
    throws InternalErrorException
    {

        try {
            Connection conn = getConnection();
            VM router = getVM(conn, vmName);

            // Determine the correct VIF on DomR to associate/disassociate the
            // IP address with
            VIF correctVif = getCorrectVif(router, vlanId);

            // If we are associating an IP address and DomR doesn't have a VIF
            // for the specified vlan ID, we need to add a VIF
            // If we are disassociating the last IP address in the VLAN, we need
            // to remove a VIF
            boolean addVif = false;
            boolean removeVif = false;
            if (add && correctVif == null) {
                addVif = true;
            } else if (!add && firstIP) {
                removeVif = true;
            }

            if (addVif) {
                // Add a new VIF to DomR
                String vifDeviceNum = getLowestAvailableVIFDeviceNum(router);

                if (vifDeviceNum == null) {
                    throw new InternalErrorException("There were no more available slots for a new VIF on router: " + router.getNameLabel(conn));
                }

                VIF.Record vifr = new VIF.Record();
                vifr.VM = router;
                vifr.device = vifDeviceNum;
                vifr.MAC = vifMacAddress;

                if ("untagged".equalsIgnoreCase(vlanId)) {
                    vifr.network = Network.getByUuid(conn, _host.publicNetwork);
                } else {
                    Network vlanNetwork = enableVlanNetwork(Long.valueOf(vlanId), _host.publicNetwork, _host.publicPif);

                    if (vlanNetwork == null) {
                        throw new InternalErrorException("Failed to enable VLAN network with tag: " + vlanId);
                    }

                    vifr.network = vlanNetwork;
                }

                correctVif = VIF.create(conn, vifr);
                correctVif.plug(conn);
                // Add iptables rule for network usage
                networkUsage(privateIpAddress, "addVif", "eth" + correctVif.getDevice(conn));
            }

            if (correctVif == null) {
                throw new InternalErrorException("Failed to find DomR VIF to associate/disassociate IP with.");
            }

            String args;
            if (add) {
                args = "-A";
            } else {
                args = "-D";
            }
            if (sourceNat) {
                args += " -f";
            }
            args += " -i ";
            args += privateIpAddress;
            args += " -l ";
            args += publicIpAddress;
            args += " -c ";
            args += "eth" + correctVif.getDevice(conn);
            args += " -g ";
            args += vlanGateway;

            String result = callHostPlugin("ipassoc", "args", args);
            if (result == null || result.isEmpty()) {
                throw new InternalErrorException("Xen plugin \"ipassoc\" failed.");
            }

            if (removeVif) {
                Network network = correctVif.getNetwork(conn);

                // Remove the VIF from DomR
                correctVif.unplug(conn);
                correctVif.destroy(conn);

                // Disable the VLAN network if necessary
                disableVlanNetwork(network);
                //Mark this vif to be removed from network usage
                networkUsage(privateIpAddress, "deleteVif", "eth" + correctVif.getDevice(conn));
            }

        } catch (XenAPIException e) {
            String msg = "Unable to assign public IP address due to " + e.toString();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        } catch (final XmlRpcException e) {
            String msg = "Unable to assign public IP address due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        }

    }

    protected String networkUsage(final String privateIpAddress,
                                  final String option,
                                  final String vif) {
        String args = null;
        if (option.equals("get")) {
            args = "-g";
        } else if (option.equals("create")) {
            args = "-c";
        } else if (option.equals("reset")) {
            args = "-r";
        } else if (option.equals("addVif")) {
            args = "-a";
            args += vif;
        } else if (option.equals("deleteVif")) {
            args = "-d";
            args += vif;
        }
        
        args += " -i ";
        args += privateIpAddress;
        return callHostPlugin("networkUsage", "args", args);
    }

    protected Answer execute(final IPAssocCommand cmd) {
        try {
            assignPublicIpAddress(cmd.getRouterName(),
                                  cmd.getRouterIp(),
                                  cmd.getPublicIp(),
                                  cmd.isAdd(),
                                  cmd.isFirstIP(),
                                  cmd.isSourceNat(),
                                  cmd.getVlanId(),
                                  cmd.getVlanGateway(),
                                  cmd.getVlanNetmask(),
                                  cmd.getVifMacAddress());
        } catch (InternalErrorException e) {
            return new Answer(cmd, false, e.getMessage());
        }

        return new Answer(cmd);
    }

    protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
        Connection conn = getConnection();

        try {
            Set<VM> vms = VM.getByNameLabel(conn, cmd.getName());
            return new GetVncPortAnswer(cmd, getVncPort(vms.iterator().next()));
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get vnc port " + e.toString(), e);
            return new GetVncPortAnswer(cmd, e.toString());
        } catch (Exception e) {
            s_logger.warn("Unable to get vnc port ", e);
            return new GetVncPortAnswer(cmd, e.getMessage());
        }
    }

    protected StorageResourceType getStorageResourceType() {
        return StorageResourceType.STORAGE_POOL;
    }

    protected CheckHealthAnswer execute(CheckHealthCommand cmd) {
        boolean result = pingxenserver();
        return new CheckHealthAnswer(cmd, result);
    }

    protected WatchNetworkAnswer execute(WatchNetworkCommand cmd) {
        WatchNetworkAnswer answer = new WatchNetworkAnswer(cmd);
        for (String domr : _domrIPMap.keySet()) {
            long[] stats = getNetworkStats(domr);
            answer.addStats(domr, stats[0], stats[1]);
        }
        return answer;
    }

    protected long[] getNetworkStats(String domr) {
        String result = networkUsage(_domrIPMap.get(domr), "get", null);
        long[] stats = new long[2];
        if (result != null) {
            String[] splitResult = result.split(":");
            int i = 0;
            while (i < splitResult.length - 1) {
                stats[0] += (new Long(splitResult[i++])).longValue();
                stats[1] += (new Long(splitResult[i++])).longValue();
            }
        }
        return stats;
    }

    protected GetHostStatsAnswer execute(GetHostStatsCommand cmd) {
        Connection conn = getConnection();
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            Host.Record record = host.getRecord(conn);

            // Determine CPU utilisation
            Set<HostCpu> cpus = record.hostCPUs;
            double cpuUtilization = 0.0d;
            int count = 0;
            for (HostCpu cpu : cpus) {
                cpuUtilization += cpu.getUtilisation(conn);
                count++;
            }
            cpuUtilization = cpuUtilization / count;

            // Determine memory utilisation
            HostMetrics metrics = record.metrics;
            final long freeMemory = metrics.getMemoryFree(conn);
            final long totalMemory = metrics.getMemoryTotal(conn);

            return new GetHostStatsAnswer(cmd,
                                          cpuUtilization,
                                          freeMemory,
                                          totalMemory,
                                          0,
                                          0);
        } catch (XenAPIException e) {
            String msg = "Unable to get host stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetHostStatsAnswer(cmd, msg);
        } catch (XmlRpcException e) {
            String msg = "Unable to get host stats" + e.getMessage();
            s_logger.warn(msg, e);
            return new GetHostStatsAnswer(cmd, msg);
        }
    }

    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        List<String> vmNames = cmd.getVmNames();

        Connection conn = getConnection();
        try {

            // Determine the UUIDs of the requested VMs
            List<String> vmUUIDs = new ArrayList<String>();
            for (String vmName : vmNames) {
                VM vm = getVM(conn, vmName);
                vmUUIDs.add(vm.getUuid(conn));
            }

            HashMap<String, VmStatsEntry> vmStatsUUIDMap = getVmStats(cmd, vmUUIDs);
            HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();

            for (String vmUUID : vmStatsUUIDMap.keySet()) {
                vmStatsNameMap.put(vmNames.get(vmUUIDs.indexOf(vmUUID)), vmStatsUUIDMap.get(vmUUID));
            }

            return new GetVmStatsAnswer(cmd, vmStatsNameMap);
        } catch (XenAPIException e) {
            String msg = "Unable to get VM stats" + e.toString();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, null);
        } catch (XmlRpcException e) {
            String msg = "Unable to get VM stats" + e.getMessage();
            s_logger.warn(msg, e);
            return new GetVmStatsAnswer(cmd, null);
        }
    }

    protected HashMap<String, VmStatsEntry> getVmStats(GetVmStatsCommand cmd,
                                                       List<String> vmUUIDs) {
        HashMap<String, VmStatsEntry> vmResponseMap = new HashMap<String, VmStatsEntry>();

        for (String vmUUID : vmUUIDs) {
            vmResponseMap.put(vmUUID, new VmStatsEntry(0, 0, 0, 0));
        }

        Object[] rrdData = getRRDData();
        Integer numRows = (Integer) rrdData[0];
        Integer numColumns = (Integer) rrdData[1];
        Node legend = (Node) rrdData[2];
        Node dataNode = (Node) rrdData[3];

        NodeList legendChildren = legend.getChildNodes();
        for (int col = 0; col < numColumns; col++) {

            if (legendChildren == null || legendChildren.item(col) == null) {
                continue;
            }

            String columnMetadata = getXMLNodeValue(legendChildren.item(col));

            if (columnMetadata == null) {
                continue;
            }

            String[] columnMetadataList = columnMetadata.split(":");

            if (columnMetadataList.length != 4) {
                continue;
            }

            String type = columnMetadataList[1];
            String uuid = columnMetadataList[2];
            String param = columnMetadataList[3];

            if (type.equals("vm") && vmResponseMap.keySet().contains(uuid)) {
                VmStatsEntry vmStatsAnswer = vmResponseMap.get(uuid);

                if (param.contains("cpu")) {
                    vmStatsAnswer.setNumCPUs(vmStatsAnswer.getNumCPUs() + 1);
                    vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() + getDataAverage(dataNode,
                                                                                                       col,
                                                                                                       numRows));
                } else if (param.equals("vif_0_rx")) {
                    vmStatsAnswer.setNetworkReadKBs(vmStatsAnswer.getNetworkReadKBs() + getDataAverage(dataNode,
                                                                                                       col,
                                                                                                       numRows));
                } else if (param.equals("vif_0_tx")) {
                    vmStatsAnswer.setNetworkWriteKBs(vmStatsAnswer.getNetworkWriteKBs() + getDataAverage(dataNode,
                                                                                                         col,
                                                                                                         numRows));
                }
            }

        }

        for (String vmUUID : vmResponseMap.keySet()) {
            VmStatsEntry vmStatsAnswer = vmResponseMap.get(vmUUID);
            vmStatsAnswer.setCPUUtilization(vmStatsAnswer.getCPUUtilization() / vmStatsAnswer.getNumCPUs());
        }

        return vmResponseMap;
    }

    protected Object[] getRRDData() {
        String stats = getHostAndVmStatsRawXML();
        StringReader statsReader = new StringReader(stats);
        InputSource statsSource = new InputSource(statsReader);

        Document doc = null;
        try {
            doc = DocumentBuilderFactory.newInstance()
                                        .newDocumentBuilder()
                                        .parse(statsSource);
        } catch (Exception e) {
        }

        NodeList firstLevelChildren = doc.getChildNodes();
        NodeList secondLevelChildren = (firstLevelChildren.item(0)).getChildNodes();
        Node metaNode = secondLevelChildren.item(0);
        Node dataNode = secondLevelChildren.item(1);

        Integer numRows = 0;
        Integer numColumns = 0;
        Node legend = null;
        NodeList metaNodeChildren = metaNode.getChildNodes();
        for (int i = 0; i < metaNodeChildren.getLength(); i++) {
            Node n = metaNodeChildren.item(i);
            if (n.getNodeName().equals("rows")) {
                numRows = Integer.valueOf(getXMLNodeValue(n));
            } else if (n.getNodeName().equals("columns")) {
                numColumns = Integer.valueOf(getXMLNodeValue(n));
            } else if (n.getNodeName().equals("legend")) {
                legend = n;
            }
        }

        return new Object[] { numRows, numColumns, legend, dataNode };
    }

    protected String getXMLNodeValue(Node n) {
        return n.getChildNodes().item(0).getNodeValue();
    }

    protected double getDataAverage(Node dataNode, int col, int numRows) {
        double value = 0;

        int numRowsUsed = 0;
        for (int row = 0; row < numRows; row++) {
            Node data = dataNode.getChildNodes()
                                .item(numRows - 1 - row)
                                .getChildNodes()
                                .item(col + 1);
            Double currentDataAsDouble = Double.valueOf(getXMLNodeValue(data));
            if (!currentDataAsDouble.equals(Double.NaN)) {
                numRowsUsed += 1;
                value += currentDataAsDouble;
            }
        }

        return (numRowsUsed == 0) ? value : (value / numRowsUsed);
    }

    protected String getHostAndVmStatsRawXML() {
        Date currentDate = new Date();
        String startTime = String.valueOf(currentDate.getTime() / 1000 - 1000);

        return callHostPlugin("gethostvmstats",
                              "collectHostStats",
                              String.valueOf(_collectHostStats),
                              "consolidationFunction",
                              _consolidationFunction,
                              "interval",
                              String.valueOf(_pollingIntervalInSeconds),
                              "startTime",
                              startTime);
    }

    protected void recordWarning(final VM vm,
                                 final String message,
                                 final Throwable e) {
        Connection conn = getConnection();
        final StringBuilder msg = new StringBuilder();
        try {
            final Long domId = vm.getDomid(conn);
            msg.append("[").append(domId != null ? domId : -1l).append("] ");
        } catch (final BadServerResponse e1) {
        } catch (final XmlRpcException e1) {
        } catch (XenAPIException e1) {
        }
        msg.append(message);
    }

    protected State convertToState(Types.VmPowerState ps) {
        final State state = s_statesTable.get(ps);
        return state == null ? State.Unknown : state;
    }

    protected HashMap<String, State> getAllVms() {
        final HashMap<String, State> vmStates = new HashMap<String, State>();
        Connection conn = getConnection();

        Set<VM> vms = null;
        for (int i = 0; i < 2; i++) {
            try {
                Host host = Host.getByUuid(conn, _host.uuid);
                vms = host.getResidentVMs(conn);
                break;
            } catch (final Throwable e) {
                s_logger.warn("Unable to get vms", e);
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException ex) {

            }
        }
        if (vms == null) {
            return null;
        }
        for (VM vm : vms) {
            VM.Record record = null;
            for (int i = 0; i < 2; i++) {
                try {
                    record = vm.getRecord(conn);
                    break;
                } catch (XenAPIException e1) {
                    s_logger.debug("VM.getRecord failed on host:" + _host.uuid + " due to " + e1.toString());
                } catch (XmlRpcException e1) {
                    s_logger.debug("VM.getRecord failed on host:" + _host.uuid + " due to " + e1.getMessage());
                }
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException ex) {

                }
            }
            if (record == null) {
                continue;
            }
            if (record.isControlDomain || record.isASnapshot || record.isATemplate) {
                continue; // Skip DOM0
            }

            VmPowerState ps = record.powerState;
            final State state = convertToState(ps);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("VM " + record.nameLabel + ": powerstate = " + ps + "; vm state=" + state.toString());
            }
            vmStates.put(record.nameLabel, state);
        }

        return vmStates;
    }

    protected State getVmState(final String vmName) {
        Connection conn = getConnection();
        int retry = 3;
        while (retry-- > 0) {
            try {
                Set<VM> vms = VM.getByNameLabel(conn, vmName);
                for (final VM vm : vms) {
                    return convertToState(vm.getPowerState(conn));
                }
            } catch (final BadServerResponse e) {
                // There is a race condition within xen such that if a vm is
                // deleted and we
                // happen to ask for it, it throws this stupid response. So
                // if this happens,
                // we take a nap and try again which then avoids the race
                // condition because
                // the vm's information is now cleaned up by xen. The error
                // is as follows
                // com.xensource.xenapi.Types$BadServerResponse
                // [HANDLE_INVALID, VM,
                // 3dde93f9-c1df-55a7-2cde-55e1dce431ab]
                s_logger.info("Unable to get a vm PowerState due to " + e.toString() +
                              ". We are retrying.  Count: " + retry);
                try {
                    Thread.sleep(3000);
                } catch (final InterruptedException ex) {

                }
            } catch (XenAPIException e) {
                String msg = "Unable to get a vm PowerState due to " + e.toString();
                s_logger.warn(msg, e);
                break;
            } catch (final XmlRpcException e) {
                String msg = "Unable to get a vm PowerState due to " + e.getMessage();
                s_logger.warn(msg, e);
                break;
            }
        }

        return State.Stopped;
    }

    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        final String vmName = cmd.getVmName();
        final State state = getVmState(vmName);
        Integer vncPort = null;
        if (state == State.Running) {
            synchronized (_vms) {
                _vms.put(vmName, State.Running);
            }
        }

        return new CheckVirtualMachineAnswer(cmd, state, vncPort);
    }

    protected PrepareForMigrationAnswer execute(final PrepareForMigrationCommand cmd) {
        /*
         * 
         * String result = null;
         * 
         *  List<VolumeVO> vols = cmd.getVolumes();
         *  result = mountwithoutvdi(vols, cmd.getMappings());
         *  if (result != null) {
         *      return new PrepareForMigrationAnswer(cmd, false, result);
         *  }
         */
        final String vmName = cmd.getVmName();
        try {

            Connection conn = getConnection();
            Set<Host> hosts = Host.getAll(conn);
            // workaround before implementing xenserver pool
            // no migration
            if (hosts.size() <= 1) {
                return new PrepareForMigrationAnswer(cmd,
                                                     false,
                                                     "not in a same xenserver pool");
            }
            // if the vm have CD
            // 1. make iosSR shared
            // 2. create pbd in target xenserver
            SR sr = getISOSRbyVmName(cmd.getVmName());
            if (sr != null) {
                Set<PBD> pbds = sr.getPBDs(conn);
                boolean found = false;
                for (PBD pbd : pbds) {
                    if (Host.getByUuid(conn, _host.uuid)
                            .equals(pbd.getHost(conn))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    sr.setShared(conn, true);
                    PBD pbd = pbds.iterator().next();
                    PBD.Record pbdr = new PBD.Record();
                    pbdr.deviceConfig = pbd.getDeviceConfig(conn);
                    pbdr.host = Host.getByUuid(conn, _host.uuid);
                    pbdr.SR = sr;
                    PBD newpbd = PBD.create(conn, pbdr);
                    newpbd.plug(conn);
                }
            }
            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            if (vms.size() != 1) {
                String msg = "There are " + vms.size() + " " + vmName;
                s_logger.warn(msg);
                return new PrepareForMigrationAnswer(cmd, false, msg);
            }

            VM vm = vms.iterator().next();

            // check network
            Set<VIF> vifs = vm.getVIFs(conn);
            for (VIF vif : vifs) {
                Network network = vif.getNetwork(conn);
                Set<PIF> pifs = network.getPIFs(conn);
                for (PIF pif : pifs) {
                    long vlan = pif.getVLAN(conn);
                    if (vlan != -1) {
                        Network vlanNetwork = enableVlanNetwork(vlan,
                                                                _host.privateNetwork,
                                                                _host.privatePif);

                        if (vlanNetwork == null) {
                            throw new InternalErrorException("Failed to enable VLAN network with tag: " + vlan);
                        }
                    }

                }

            }

            synchronized (_vms) {
                _vms.put(cmd.getVmName(), State.Migrating);
            }
            return new PrepareForMigrationAnswer(cmd, true, null);
        } catch (Exception e) {
            String msg = "catch exception " + e.getMessage();
            s_logger.warn(msg, e);
            return new PrepareForMigrationAnswer(cmd, false, msg);
        }
    }

    protected Answer execute(final PrimaryStorageDownloadCommand cmd) {
        SR tmpltsr = null;
        String tmplturl = cmd.getUrl();
        int index = tmplturl.lastIndexOf("/");
        String mountpoint = tmplturl.substring(0, index);
        String tmpltname = null;
        if (index < tmplturl.length() - 1)
            tmpltname = tmplturl.substring(index + 1).replace(".vhd", "");
        try {
            Connection conn = getConnection();
            String pUuid = cmd.getPoolUuid();
            SR poolsr = null;
            Set<SR> srs = SR.getByNameLabel(conn, pUuid);
            if (srs.size() != 1) {
                String msg = "There are " + srs.size() +
                             " SRs with same name: " +
                             pUuid;
                s_logger.warn(msg);
                return new DownloadAnswer(null,
                                          0,
                                          msg,
                                          com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                                          "",
                                          "",
                                          0);
            } else {
                poolsr = srs.iterator().next();
            }

            /* Does the template exist in primary storage pool? If yes, no copy */
            VDI vmtmpltvdi = null;

            Set<VDI> vdis = VDI.getByNameLabel(conn, "Template " + cmd.getName());

            for (VDI vdi : vdis) {
                VDI.Record vdir = vdi.getRecord(conn);
                if (vdir.SR.equals(poolsr)) {
                    vmtmpltvdi = vdi;
                    break;
                }
            }
            String uuid;
            if (vmtmpltvdi == null) {
                tmpltsr = createNfsSRbyURI(new URI(mountpoint), false);
                tmpltsr.scan(conn);
                VDI tmpltvdi = null;

                if (tmpltname != null) {
                    tmpltvdi = getVDIbyUuid(tmpltname);
                }
                if (tmpltvdi == null) {
                    vdis = tmpltsr.getVDIs(conn);
                    for (VDI vdi : vdis) {
                        tmpltvdi = vdi;
                        break;
                    }
                }
                if (tmpltvdi == null) {
                    String msg = "Unable to find template vdi on secondary storage" + "host:" +
                                 _host.uuid +
                                 "pool: " +
                                 tmplturl;
                    s_logger.warn(msg);
                    return new DownloadAnswer(null,
                                              0,
                                              msg,
                                              com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                                              "",
                                              "",
                                              0);
                }
                vmtmpltvdi = tmpltvdi.copy(conn, poolsr);

                vmtmpltvdi.setNameLabel(conn, "Template " + cmd.getName());
                // vmtmpltvdi.setNameDescription(conn, cmd.getDescription());
                uuid = vmtmpltvdi.getUuid(conn);

            } else
                uuid = vmtmpltvdi.getUuid(conn);

            // Determine the size of the template
            long createdSize = vmtmpltvdi.getVirtualSize(conn);

            DownloadAnswer answer = new DownloadAnswer(null,
                                                       100,
                                                       cmd,
                                                       com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                                                       uuid,
                                                       uuid);
            answer.setTemplateSize(createdSize);

            return answer;

        } catch (XenAPIException e) {
            String msg = "XenAPIException:" + e.toString() +
                         "host:" +
                         _host.uuid +
                         "pool: " +
                         tmplturl;
            s_logger.warn(msg, e);
            return new DownloadAnswer(null,
                                      0,
                                      msg,
                                      com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                                      "",
                                      "",
                                      0);
        } catch (Exception e) {
            String msg = "XenAPIException:" + e.getMessage() +
                         "host:" +
                         _host.uuid +
                         "pool: " +
                         tmplturl;
            s_logger.warn(msg, e);
            return new DownloadAnswer(null,
                                      0,
                                      msg,
                                      com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOAD_ERROR,
                                      "",
                                      "",
                                      0);
        } finally {
            removeSR(tmpltsr);
        }

    }

    protected String removeSRSync(SR sr) {
        if (sr == null) {
            return null;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }

        Connection conn = getConnection();
        long waittime = 0;
        try {
            Set<VDI> vdis = sr.getVDIs(conn);
            for (VDI vdi : vdis) {
                Map<java.lang.String, Types.VdiOperations> currentOperation = vdi.getCurrentOperations(conn);
                if (currentOperation == null || currentOperation.size() == 0) {
                    continue;
                }
                if (waittime >= 1800000) {
                    String msg = "This template is being used, try late time";
                    s_logger.warn(msg);
                    return msg;
                }
                waittime += 30000;
                try {
                    Thread.sleep(30000);
                } catch (final InterruptedException ex) {
                }
            }
            removeSR(sr);
            return null;
        } catch (XenAPIException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.toString()), e);
        } catch (XmlRpcException e) {
            s_logger.warn(logX(sr, "Unable to get current opertions " + e.getMessage()), e);
        }
        String msg = "Remove SR failed";
        s_logger.warn(msg);
        return msg;

    }

    protected void removeSR(SR sr) {
        if (sr == null) {
            return;
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(logX(sr, "Removing SR"));
        }
        for( int i=0; i<2; i++ ) {
            Connection conn = getConnection();
            try {
                Set<PBD> pbds = sr.getPBDs(conn);
                for (PBD pbd : pbds) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(logX(pbd, "Unplugging pbd"));
                    }
                    if (pbd.getCurrentlyAttached(conn)) {
                        pbd.unplug(conn);
                    }
                    pbd.destroy(conn);
                }
    
                pbds = sr.getPBDs(conn);
                if (pbds.size() == 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(logX(sr, "Forgetting"));
                    }
                    sr.forget(conn);
                    return;
                }
    
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(logX(sr, "There are still pbd attached"));
                    if (s_logger.isTraceEnabled()) {
                        for (PBD pbd : pbds) {
                            s_logger.trace(logX(pbd, " Still attached"));
                        }
                    }
                }
            } catch (XenAPIException e) {
                s_logger.debug(logX(sr, "Catch XenAPIException: " + e.toString()));
            } catch (XmlRpcException e) {
                s_logger.debug(logX(sr, "Catch Exception: " + e.getMessage()));
            }
        }
        s_logger.warn(logX(sr, "Unable to remove SR"));
    }

    protected MigrateAnswer execute(final MigrateCommand cmd) {
        final String vmName = cmd.getVmName();
        State state = null;

        synchronized (_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }
        try {
            Connection conn = getConnection();
            Set<VM> vms = VM.getByNameLabel(conn, vmName);

            String ipaddr = cmd.getDestinationIp();

            Set<Host> hosts = Host.getAll(conn);
            Host dsthost = null;
            for (Host host : hosts) {
                if (host.getAddress(conn).equals(ipaddr)) {
                    dsthost = host;
                    break;
                }
            }
            // if it is windows, we will not fake it is migrateable,
            // windows requires PV driver to migrate

            for (VM vm : vms) {
                if (!cmd.isWindows()) {
                    String uuid = vm.getUuid(conn);
                    String result = callHostPlugin("preparemigration",
                                                   "uuid",
                                                   uuid);
                    if (result == null || result.isEmpty()) {
                        return new MigrateAnswer(cmd,
                                                 false,
                                                 "migration failed",
                                                 null);
                    }
                    // check if pv version is successfully set up
                    int i = 0;
                    for (; i < 3; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (final InterruptedException ex) {
                        }
                        VMGuestMetrics vmmetric = vm.getGuestMetrics(conn);

                        if (isRefNull(vmmetric))
                            continue;

                        Map<String, String> PVversion = vmmetric.getPVDriversVersion(conn);
                        if (PVversion != null && PVversion.containsKey("major")) {
                            break;
                        }

                    }

                    if (i >= 3) {
                        String msg = "migration failed due to can not fake PV driver for " + vmName;
                        s_logger.warn(msg);
                        return new MigrateAnswer(cmd, false, msg, null);
                    }
                }
                final Map<String, String> options = new HashMap<String, String>();
                vm.poolMigrate(conn, dsthost, options);
                state = State.Stopping;

            }
            return new MigrateAnswer(cmd, true, "migration succeeded", null);
        } catch (XenAPIException e) {
            String msg = "migration failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new MigrateAnswer(cmd, false, msg, null);
        } catch (XmlRpcException e) {
            String msg = "migration failed due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new MigrateAnswer(cmd, false, msg, null);
        } finally {
            synchronized (_vms) {
                _vms.put(vmName, state);
            }
        }

    }

    protected State getRealPowerState(String label) {
        Connection conn = getConnection();
        int i = 0;
        s_logger.trace("Checking on the HALTED State");
        for (; i < 20; i++) {
            try {
                Set<VM> vms = VM.getByNameLabel(conn, label);
                if (vms == null || vms.size() == 0) {
                    continue;
                }

                VM vm = vms.iterator().next();

                VmPowerState vps = vm.getPowerState(conn);
                if (vps != null && vps != VmPowerState.HALTED &&
                    vps != VmPowerState.UNKNOWN &&
                    vps != VmPowerState.UNRECOGNIZED) {
                    return convertToState(vps);
                }
            } catch (XenAPIException e) {
                String msg = "Unable to get real power state due to " + e.toString();
                s_logger.warn(msg, e);
            } catch (XmlRpcException e) {
                String msg = "Unable to get real power state due to " + e.getMessage();
                s_logger.warn(msg, e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        return State.Stopped;
    }

    protected VM.Record getControlDomain() {
        Connection conn = getConnection();
        Map<VM, VM.Record> vms = null;
        try {
            vms = VM.getAllRecords(conn);
        } catch (final Throwable e) {
            s_logger.warn("Unable to get vms", e);
            throw new CloudRuntimeException("Unable to get vms", e);
        }

        for (final VM.Record record : vms.values()) {
            if (record.isControlDomain) {
                return record;
            }
        }

        return null;
    }

    protected HashMap<String, State> sync() {
        HashMap<String, State> newStates;
        HashMap<String, State> oldStates = null;

        final HashMap<String, State> changes = new HashMap<String, State>();

        synchronized (_vms) {
            newStates = getAllVms();
            if (newStates == null) {
                s_logger.debug("Unable to get the vm states so no state sync at this point.");
                return null;
            }

            oldStates = new HashMap<String, State>(_vms.size());
            oldStates.putAll(_vms);

            for (final Map.Entry<String, State> entry : newStates.entrySet()) {
                final String vm = entry.getKey();

                State newState = entry.getValue();
                final State oldState = oldStates.remove(vm);

                if (newState == State.Stopped && oldState != State.Stopping &&
                    oldState != null &&
                    oldState != State.Stopped) {
                    newState = getRealPowerState(vm);
                }

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm +
                                   ": xen has state " +
                                   newState +
                                   " and we have state " +
                                   (oldState != null ? oldState.toString()
                                                    : "null"));
                }

                if (vm.startsWith("migrating")) {
                    s_logger.debug("Migrating from xen detected.  Skipping");
                    continue;
                }
                if (oldState == null) {
                    _vms.put(vm, newState);
                    s_logger.debug("Detecting a new state but couldn't find a old state so adding it to the changes: " + vm);
                    changes.put(vm, newState);
                } else if (oldState == State.Starting) {
                    if (newState == State.Running) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Stopped) {
                        s_logger.debug("Ignoring vm " + vm +
                                       " because of a lag in starting the vm.");
                    }
                } else if (oldState == State.Migrating) {
                    if (newState == State.Running) {
                        s_logger.debug("Detected that an migrating VM is now running: " + vm);
                        _vms.put(vm, newState);
                    }
                } else if (oldState == State.Stopping) {
                    if (newState == State.Stopped) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Running) {
                        s_logger.debug("Ignoring vm " + vm +
                                       " because of a lag in stopping the vm. ");
                    }
                } else if (oldState != newState) {
                    _vms.put(vm, newState);
                    if (newState == State.Stopped) {
                        /*
                         * if (_vmsKilled.remove(vm)) { s_logger.debug("VM " +
                         * vm + " has been killed for storage. "); newState =
                         * State.Error; }
                         */
                    }
                    changes.put(vm, newState);
                }
            }

            for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
                final String vm = entry.getKey();
                final State oldState = entry.getValue();

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + " is now missing from xen so reporting stopped");
                }

                if (oldState == State.Stopping) {
                    s_logger.debug("Ignoring VM " + vm + " in transition state stopping.");
                    _vms.remove(vm);
                } else if (oldState == State.Starting) {
                    s_logger.debug("Ignoring VM " + vm + " in transition state starting.");
                } else if (oldState == State.Stopped) {
                    _vms.remove(vm);
                } else if (oldState == State.Migrating) {
                    s_logger.debug("Ignoring VM " + vm + " in migrating state.");
                } else {
                    State state = State.Stopped;
                    /*
                     * if (_vmsKilled.remove(entry.getKey())) {
                     * s_logger.debug("VM " + vm +
                     * " has been killed by storage monitor"); state =
                     * State.Error; }
                     */
                    changes.put(entry.getKey(), state);
                }
            }
        }

        return changes;
    }

    protected ReadyAnswer execute(ReadyCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        // Ignore the result of the callHostPlugin. Even if unmounting the
        // snapshots dir fails, let Ready command
        // succeed.
        callHostPlugin("unmountSnapshotsDir", "dcId", dcId.toString());
        return new ReadyAnswer(cmd);
    }

    //
    // using synchronized on VM name in the caller does not prevent multiple
    // commands being sent against
    // the same VM, there will be a race condition here in finally clause and
    // the main block if
    // there are multiple requests going on
    //
    // Therefore, a lazy solution is to add a synchronized guard here
    protected int getVncPort(VM vm) {
        Connection conn = getConnection();

        VM.Record record;
        try {
            record = vm.getRecord(conn);
        } catch (XenAPIException e) {
            String msg = "Unable to get vnc-port due to " + e.toString();
            s_logger.warn(msg, e);
            return -1;
        } catch (XmlRpcException e) {
            String msg = "Unable to get vnc-port due to " + e.getMessage();
            s_logger.warn(msg, e);
            return -1;
        }
        String hvm = "true";
        if (record.HVMBootPolicy.isEmpty()) {
            hvm = "false";
        }

        String vncport = callHostPlugin("getvncport",
                                        "domID",
                                        record.domid.toString(),
                                        "hvm",
                                        hvm);
        if (vncport == null || vncport.isEmpty()) {
            return -1;
        }

        vncport = vncport.replace("\n", "");
        return NumbersUtil.parseInt(vncport, -1);
    }

    protected Answer execute(final RebootCommand cmd) {

        synchronized (_vms) {
            _vms.put(cmd.getVmName(), State.Starting);
        }

        try {
            Connection conn = getConnection();
            Set<VM> vms = null;
            try {
                vms = VM.getByNameLabel(conn, cmd.getVmName());
            } catch (XenAPIException e0) {
                s_logger.debug("getByNameLabel failed " + e0.toString());
                return new RebootAnswer(cmd, "getByNameLabel failed " + e0.toString());
            } catch (Exception e0) {
                s_logger.debug("getByNameLabel failed " + e0.getMessage());
                return new RebootAnswer(cmd, "getByNameLabel failed");
            }
            for (VM vm : vms) {
                try {
                    vm.cleanReboot(conn);
                } catch (XenAPIException e) {
                    s_logger.debug("Do Not support Clean Reboot, fall back to hard Reboot: " + e.toString());
                    try {
                        vm.hardReboot(conn);
                    } catch (XenAPIException e1) {
                        s_logger.debug("Caught exception on hard Reboot " + e1.toString());
                        return new RebootAnswer(cmd, "reboot failed: " + e1.toString());
                    } catch (XmlRpcException e1) {
                        s_logger.debug("Caught exception on hard Reboot " + e1.getMessage());
                        return new RebootAnswer(cmd, "reboot failed");
                    }
                } catch (XmlRpcException e) {
                    String msg = "Clean Reboot failed due to " + e.getMessage();
                    s_logger.warn(msg, e);
                    return new RebootAnswer(cmd, msg);
                }
            }
            return new RebootAnswer(cmd, "reboot succeeded", null, null);
        } finally {
            synchronized (_vms) {
                _vms.put(cmd.getVmName(), State.Running);
            }
        }
    }

    protected Answer execute(RebootRouterCommand cmd) {
        Long bytesSent = 0L;
        Long bytesRcvd = 0L;
        if (VirtualMachineName.isValidRouterName(cmd.getVmName())) {
            long[] stats = getNetworkStats(cmd.getVmName());
            bytesSent = stats[0];
            bytesRcvd = stats[1];
        }
        RebootAnswer answer = (RebootAnswer) execute((RebootCommand) cmd);
        answer.setBytesSent(bytesSent);
        answer.setBytesReceived(bytesRcvd);
        if (answer.getResult()) {
            String cnct = connect(cmd.getVmName(), cmd.getPrivateIpAddress());
            networkUsage(cmd.getPrivateIpAddress(), "create", null);
            if (cnct == null) {
                _domrIPMap.put(cmd.getVmName(), cmd.getPrivateIpAddress());
                return answer;
            } else {
                return new Answer(cmd, false, cnct);
            }
        }
        return answer;
    }

    protected VM createVmFromTemplate(Connection conn, StartCommand cmd)
            throws XenAPIException, XmlRpcException {
        Set<VM> templates;
        VM vm = null;
        String guestOsTypeName = cmd.getGuestOSDescription();
        templates = VM.getByNameLabel(conn, guestOsTypeName);
        assert templates.size() == 1 : "Should only have 1 template but found " + templates.size();
        VM template = templates.iterator().next();
        vm = template.createClone(conn, cmd.getVmName());
        vm.removeFromOtherConfig(conn, "disks");

        if (!(guestOsTypeName.startsWith("Windows") || guestOsTypeName.startsWith("Citrix") || guestOsTypeName.startsWith("Other"))) {
            if (cmd.getBootFromISO())
                vm.setPVBootloader(conn, "eliloader");
            else
                vm.setPVBootloader(conn, "pygrub");

            vm.addToOtherConfig(conn, "install-repository", "cdrom");
        }
        return vm;
    }

    public boolean joinPool(String address, String username, String password) {
        Connection conn = getConnection();
        Connection poolConn = null;
        try {
            // set the _host.poolUuid to the old pool uuid in case it's not set.
            _host.pool = getPoolUuid();

            // Connect and find out about the new connection to the new pool.
            poolConn = _connPool.connect(address, username, password, _wait);
            Map<Pool, Pool.Record> pools = Pool.getAllRecords(poolConn);
            Pool.Record pr = pools.values().iterator().next();

            // Now join it.
            String masterAddr = pr.master.getAddress(poolConn);
            Pool.join(conn, masterAddr, username, password);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Joined the pool at " + masterAddr);
            }
            disconnected(); // Logout of our own session.
            try {
                // slave will restart xapi in 10 sec
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }

            // Set the pool uuid now to the newest pool.
            _host.pool = pr.uuid;
            URL url;
            try {
                url = new URL("http://" + _host.ip);
            } catch (MalformedURLException e1) {
                throw new CloudRuntimeException("Problem with url " + _host.ip);
            }
            Connection c = null;
            for (int i = 0; i < 15; i++) {
                c = new Connection(url, _wait);
                try {
                    Session.loginWithPassword(c,
                                              _username,
                                              _password,
                                              APIVersion.latest().toString());
                    s_logger.debug("Still waiting for the conversion to the master");
                    Session.logout(c);
                    c.dispose();
                } catch (Types.HostIsSlave e) {
                    try {
                        Session.logout(c);
                        c.dispose();
                    } catch (XmlRpcException e1) {
                        s_logger.debug("Unable to logout of test connection due to " + e1.getMessage());
                    } catch (XenAPIException e1) {
                        s_logger.debug("Unable to logout of test connection due to " + e1.getMessage());
                    }
                    break;
                } catch (XmlRpcException e) {
                    s_logger.debug("XmlRpcException: Still waiting for the conversion to the master");
                } catch (Exception e) {
                    s_logger.debug("Exception: Still waiting for the conversion to the master");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
            return true;
        } catch (JoiningHostCannotContainSharedSrs e) {
            s_logger.warn("Unable to allow host " + _host.uuid +
                          " to join pool " +
                          address, e);
            return false;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to allow host " + _host.uuid +
                          " to join pool " +
                          address, e);
            return false;
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to allow host " + _host.uuid +
                          " to join pool " +
                          address, e);
            return false;
        } finally {
            if (poolConn != null) {
                XenServerConnectionPool.logout(poolConn);
            }
        }
    }

    public boolean leavePool() {
        Connection conn = getConnection();
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            Pool.eject(conn, host);
            return true;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to eject host " + _host.uuid, e);
            return false;
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to eject host " + _host.uuid, e);
            return false;
        }
    }

    protected void startvmfailhandle(VM vm, List<Ternary<SR, VDI, VolumeVO>> mounts) {
        Connection conn = getConnection();

        if (vm != null) {
            try {

                if (vm.getPowerState(conn) == VmPowerState.RUNNING) {
                    try {
                        vm.hardShutdown(conn);
                    } catch (Exception e) {
                        String msg = "VM hardshutdown failed due to " + e.toString();
                        s_logger.warn(msg);
                    }
                }
                if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                    try {
                        vm.destroy(conn);
                    } catch (Exception e) {
                        String msg = "VM destroy failed due to " + e.toString();
                        s_logger.warn(msg);
                    }
                }
            } catch (Exception e) {
                String msg = "VM getPowerState failed due to " + e.toString();
                s_logger.warn(msg);
            }
        }
        if (mounts != null) {
            for (Ternary<SR, VDI, VolumeVO> mount : mounts) {
                VDI vdi = mount.second();
                Set<VBD> vbds = null;
                try {
                    vbds = vdi.getVBDs(conn);
                } catch (Exception e) {
                    String msg = "VDI getVBDS failed due to " + e.toString();
                    s_logger.warn(msg);
                    continue;
                }
                for (VBD vbd : vbds) {
                    try {
                        vbd.unplug(conn);
                        vbd.destroy(conn);
                    } catch (Exception e) {
                        String msg = "VBD destroy failed due to " + e.toString();
                        s_logger.warn(msg);
                    }
                }
            }
        }
    }

    protected void setMemory(Connection conn, VM vm, long memsize)
            throws XmlRpcException, XenAPIException {
        vm.setMemoryStaticMin(conn, memsize);
        vm.setMemoryDynamicMin(conn, memsize);

        vm.setMemoryDynamicMax(conn, memsize);
        vm.setMemoryStaticMax(conn, memsize);
    }

    protected StartAnswer execute(StartCommand cmd) {
        State state = State.Stopped;
        Connection conn = getConnection();
        VM vm = null;
        SR isosr = null;
        List<Ternary<SR, VDI, VolumeVO>> mounts = null;
        for (int retry = 0; retry < 2; retry++) {
            try {
                synchronized (_vms) {
                    _vms.put(cmd.getVmName(), State.Starting);
                }

                List<VolumeVO> vols = cmd.getVolumes();

                mounts = mount(vols);
                if (retry == 1) {
                    // at the second time, try hvm
                    cmd.setGuestOSDescription("Other install media");
                }

                vm = createVmFromTemplate(conn, cmd);

                long memsize = cmd.getRamSize() * 1024L * 1024L;
                setMemory(conn, vm, memsize);

                vm.setIsATemplate(conn, false);

                vm.setVCPUsMax(conn, (long) cmd.getCpu());
                vm.setVCPUsAtStartup(conn, (long) cmd.getCpu());

                Host host = Host.getByUuid(conn, _host.uuid);
                vm.setAffinity(conn, host);

                Map<String, String> vcpuparam = new HashMap<String, String>();

                vcpuparam.put("weight", Integer.toString(cmd.getCpuWeight()));
                vcpuparam.put("cap", Integer.toString(cmd.getUtilization()));
                vm.setVCPUsParams(conn, vcpuparam);

                boolean bootFromISO = cmd.getBootFromISO();

                /* create root VBD */
                VBD.Record vbdr = new VBD.Record();
                Ternary<SR, VDI, VolumeVO> mount = mounts.get(0);
                vbdr.VM = vm;
                vbdr.VDI = mount.second();
                vbdr.bootable = !bootFromISO;
                vbdr.userdevice = "0";
                vbdr.mode = Types.VbdMode.RW;
                vbdr.type = Types.VbdType.DISK;
                VBD.create(conn, vbdr);

                /* determine available slots to attach data volumes to */
                List<String> availableSlots = new ArrayList<String>();
                availableSlots.add("1");
                availableSlots.add("2");
                availableSlots.add("4");
                availableSlots.add("5");
                availableSlots.add("6");
                availableSlots.add("7");

                /* create data VBDs */
                for (int i = 1; i < mounts.size(); i++) {
                    String userDevice = availableSlots.get(0);
                    mount = mounts.get(i);
                    VDI vdi = mount.second();
                    vdi.setNameLabel(conn, cmd.getVmName() + "-DATA");
                    vbdr.VM = vm;
                    vbdr.VDI = mount.second();
                    vbdr.bootable = false;
                    vbdr.userdevice = userDevice;
                    vbdr.mode = Types.VbdMode.RW;
                    vbdr.type = Types.VbdType.DISK;
                    vbdr.unpluggable = true;
                    VBD.create(conn, vbdr);
                    availableSlots.remove(userDevice);
                }

                /* create CD-ROM VBD */
                VBD.Record cdromVBDR = new VBD.Record();
                cdromVBDR.VM = vm;
                cdromVBDR.empty = true;
                cdromVBDR.bootable = bootFromISO;
                cdromVBDR.userdevice = "3";
                cdromVBDR.mode = Types.VbdMode.RO;
                cdromVBDR.type = Types.VbdType.CD;
                VBD cdromVBD = VBD.create(conn, cdromVBDR);

                /* insert the ISO VDI if isoPath is not null */
                String isopath = cmd.getISOPath();
                if (isopath != null) {
                    int index = isopath.lastIndexOf("/");

                    String mountpoint = isopath.substring(0, index);
                    URI uri = new URI(mountpoint);
                    isosr = createIsoSRbyURI(uri, cmd.getVmName(), false);

                    String isoname = isopath.substring(index + 1);

                    VDI isovdi = getVDIbyLocationandSR(isoname, isosr);

                    if (isovdi == null) {
                        String msg = " can not find ISO " + cmd.getISOPath();
                        s_logger.warn(msg);
                        return new StartAnswer(cmd, msg);
                    } else {
                        cdromVBD.insert(conn, isovdi);
                    }

                }

                createVIF(conn,
                          vm,
                          cmd.getGuestMacAddress(),
                          cmd.getGuestNetworkId(),
                          cmd.getNetworkRateMbps(),
                          "0",
                          false);

                if (cmd.getExternalMacAddress() != null && cmd.getExternalVlan() != null) {
                    createVIF(conn,
                              vm,
                              cmd.getExternalMacAddress(),
                              cmd.getExternalVlan(),
                              0,
                              "1",
                              true);
                }

                /* set action after crash as destroy */
                vm.setActionsAfterCrash(conn, Types.OnCrashBehaviour.DESTROY);

                vm.start(conn, false, true);

                state = State.Running;
                return new StartAnswer(cmd);

            } catch (XenAPIException e) {
                String errormsg = e.toString();
                String msg = "Exception caught while starting VM due to message:" + errormsg;
                if (!errormsg.contains("Unable to find partition containing kernel")) {
                    s_logger.warn(msg, e);
                    startvmfailhandle(vm, mounts);
                    removeSR(isosr);
                } else {
                    startvmfailhandle(vm, mounts);
                    removeSR(isosr);
                    continue;
                }
                state = State.Stopped;
                return new StartAnswer(cmd, msg);
            } catch (Exception e) {
                String msg = "Exception caught while starting VM due to message:" + e.getMessage();
                s_logger.warn(msg, e);
                startvmfailhandle(vm, mounts);
                removeSR(isosr);
                state = State.Stopped;
                return new StartAnswer(cmd, msg);
            } finally {
                synchronized (_vms) {
                    _vms.put(cmd.getVmName(), state);
                }

            }
        }
        String msg = "Start VM failed";
        return new StartAnswer(cmd, msg);
    }

    protected void createVIF(Connection conn,
                             VM vm,
                             String mac,
                             String vlanTag,
                             int rate,
                             String devNum,
                             boolean isPub)
    throws XenAPIException,
           XmlRpcException,
           InternalErrorException
    {
        VIF.Record vifr = new VIF.Record();
        vifr.VM = vm;
        vifr.device = devNum;
        vifr.MAC = mac;

        Network vlanNetwork = null;
        if (isPub) {
            vlanNetwork = enableVlanNetwork(Long.valueOf(vlanTag),
                                            _host.publicNetwork,
                                            _host.publicPif);
        } else {
            vlanNetwork = enableVlanNetwork(Long.valueOf(vlanTag),
                                            _host.privateNetwork,
                                            _host.privatePif);
        }

        if (vlanNetwork == null) {
            throw new InternalErrorException("Failed to enable VLAN network with tag: " + vlanTag);
        }

        vifr.network = vlanNetwork;
        if (rate != 0) {
            vifr.qosAlgorithmType = "ratelimit";
            vifr.qosAlgorithmParams = new HashMap<String, String>();
            vifr.qosAlgorithmParams.put("kbps", Integer.toString(rate * 1000));
        }

        VIF.create(conn, vifr);
    }

    protected StopAnswer execute(final StopCommand cmd) {
        String vmName = cmd.getVmName();
        try {
            Connection conn = getConnection();

            Set<VM> vms = VM.getByNameLabel(conn, vmName);
            // stop vm which is running on this host or is in halted state
            for (VM vm : vms) {
                VM.Record vmr = vm.getRecord(conn);
                if(vmr.powerState != VmPowerState.RUNNING) 
                    continue;
                if(isRefNull(vmr.residentOn))
                    continue;
                if(vmr.residentOn.getUuid(conn).equals(_host.uuid))
                    continue;
                vms.remove(vm);
            }

            if (vms.size() == 0) {
                s_logger.warn("VM does not exist on XenServer" + _host.uuid);
                synchronized (_vms) {
                    _vms.remove(vmName);
                }
                return new StopAnswer(cmd, "VM does not exist", 0, 0L, 0L);
            }
            Long bytesSent = 0L;
            Long bytesRcvd = 0L;
            for (VM vm : vms) {
                VM.Record vmr = vm.getRecord(conn);

                if (vmr.isControlDomain) {
                    String msg = "Tring to Shutdown control domain";
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg);
                }

                if (vmr.powerState == VmPowerState.RUNNING && !isRefNull(vmr.residentOn) &&
                    !vmr.residentOn.getUuid(conn).equals(_host.uuid)) {
                    String msg = "Stop Vm " + vmName +
                                 " failed due to this vm is not running on this host: " +
                                 _host.uuid +
                                 " but host:" +
                                 vmr.residentOn.getUuid(conn);
                    s_logger.warn(msg);
                    return new StopAnswer(cmd, msg);
                }

                State state = null;
                synchronized (_vms) {
                    state = _vms.get(vmName);
                    _vms.put(vmName, State.Stopping);
                }

                try {
                    if (vmr.powerState == VmPowerState.RUNNING) {
                        /* when stop a vm, set affinity to current xenserver */
                        vm.setAffinity(conn, vm.getResidentOn(conn));
                        try {
                            if (VirtualMachineName.isValidRouterName(vmName)) {
                                long[] stats = getNetworkStats(vmName);
                                bytesSent = stats[0];
                                bytesRcvd = stats[1];
                            }
                            vm.cleanShutdown(conn);
                        } catch (XenAPIException e) {
                            s_logger.debug("Do Not support Clean Shutdown, fall back to hard Shutdown: " + e.toString());
                            try {
                                vm.hardShutdown(conn);
                            } catch (XenAPIException e1) {
                                String msg = "Hard Shutdown failed due to " + e1.toString();
                                s_logger.warn(msg, e1);
                                return new StopAnswer(cmd, msg);
                            } catch (XmlRpcException e1) {
                                String msg = "Hard Shutdown failed due to " + e1.getMessage();
                                s_logger.warn(msg, e1);
                                return new StopAnswer(cmd, msg);
                            }
                        } catch (XmlRpcException e) {
                            String msg = "Clean Shutdown failed due to " + e.getMessage();
                            s_logger.warn(msg, e);
                            return new StopAnswer(cmd, msg);
                        }
                    }
                } catch (Exception e) {
                    String msg = "Catch exception " + e.getClass().toString() +
                                 " when stop VM:" +
                                 cmd.getVmName();
                    s_logger.debug(msg);
                    return new StopAnswer(cmd, msg);
                } finally {

                    try {
                        if (vm.getPowerState(conn) == VmPowerState.HALTED) {
                            Set<VIF> vifs = vm.getVIFs(conn);
                            List<Network> networks = new ArrayList<Network>();
                            for (VIF vif : vifs) {
                                networks.add(vif.getNetwork(conn));
                            }

                            vm.destroy(conn);
                            state = State.Stopped;
                            SR sr = getISOSRbyVmName(cmd.getVmName());
                            removeSR(sr);
                            if (VirtualMachineName.isValidRouterName(vmName)) {
                                _domrIPMap.remove(vmName);
                            }

                            // Disable any VLAN networks that aren't used
                            // anymore
                            for (Network network : networks) {
                                if (network.getNameLabel(conn)
                                           .startsWith("VLAN")) {
                                    disableVlanNetwork(network);
                                }
                            }
                        }
                    } catch (XenAPIException e) {
                        String msg = "VM destroy failed in Stop " + vmName +
                                     " Command due to " +
                                     e.toString();
                        s_logger.warn(msg, e);
                    } catch (Exception e) {
                        String msg = "VM destroy failed in Stop " + vmName +
                                     " Command due to " +
                                     e.getMessage();
                        s_logger.warn(msg, e);
                    } finally {
                        synchronized (_vms) {
                            _vms.put(vmName, state);
                        }
                    }
                }
            }
            return new StopAnswer(cmd,
                                  "Stop VM " + vmName + " Succeed",
                                  0,
                                  bytesSent,
                                  bytesRcvd);
        } catch (XenAPIException e) {
            String msg = "Stop Vm " + vmName + " fail due to " + e.toString();
            s_logger.warn(msg, e);
            return new StopAnswer(cmd, msg);
        } catch (XmlRpcException e) {
            String msg = "Stop Vm " + vmName + " fail due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new StopAnswer(cmd, msg);
        }
    }

    protected String connect(final String vmName,
                             final String ipAddress,
                             final int port) {
        for (int i = 0; i <= _retry; i++) {
            try {
                Connection conn = getConnection();

                Set<VM> vms = VM.getByNameLabel(conn, vmName);
                if (vms.size() < 1) {
                    String msg = "VM " + vmName + " is not running";
                    s_logger.warn(msg);
                    return msg;
                }
            } catch (Exception e) {
                String msg = "VM.getByNameLabel " + vmName +
                             " failed due to " +
                             e.toString();
                s_logger.warn(msg, e);
                return msg;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to connect to " + ipAddress);
            }
            if (pingdomr(ipAddress, Integer.toString(port)))
                return null;
            try {
                Thread.sleep(_sleep);
            } catch (final InterruptedException e) {
            }
        }

        s_logger.debug("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

    protected String connect(final String vmname, final String ipAddress) {
        return connect(vmname, ipAddress, 3922);
    }

    protected StartRouterAnswer execute(StartRouterCommand cmd) {
        final String vmName = cmd.getVmName();
        final DomainRouter router = cmd.getRouter();
        try {
            String tag = router.getVnet();
            Network network = enableVlanNetwork(Long.parseLong(tag),
                                                _host.privateNetwork,
                                                _host.privatePif);

            if (network == null) {
                throw new InternalErrorException("Failed to enable VLAN network with tag: " + tag);
            }

            String bootArgs = cmd.getBootArgs();

            String result = startSystemVM(vmName,
                                          router.getVlanId(),
                                          network,
                                          cmd.getVolumes(),
                                          bootArgs,
                                          router.getPrivateIpAddress(),
                                          router.getPrivateMacAddress(),
                                          router.getPublicIpAddress(),
                                          router.getPublicMacAddress(),
                                          3922,
                                          router.getRamSize());
            if (result == null) {
                networkUsage(router.getPrivateIpAddress(), "create", null);
                _domrIPMap.put(cmd.getVmName(), router.getPrivateIpAddress());
                return new StartRouterAnswer(cmd);
            }
            return new StartRouterAnswer(cmd, result);

        } catch (Exception e) {
            String msg = "Exception caught while starting router vm " + vmName +
                         " due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
            return new StartRouterAnswer(cmd, msg);
        }
    }

    protected String startSystemVM(String vmName,
                                   String vlanId,
                                   Network nw0,
                                   List<VolumeVO> vols,
                                   String bootArgs,
                                   String privateIp,
                                   String privateMacAddr,
                                   String publicIp,
                                   String publicMacAddr,
                                   int cmdPort,
                                   long ramSize) {

        VM vm = null;
        List<Ternary<SR, VDI, VolumeVO>> mounts = null;
        Connection conn = getConnection();
        State state = State.Stopped;
        try {
            synchronized (_vms) {
                _vms.put(vmName, State.Starting);
            }

            mounts = mount(vols);

            assert mounts.size() == 1 : "System VMs should have only 1 partition but we actually have " + mounts.size();

            Ternary<SR, VDI, VolumeVO> mount = mounts.get(0);

            if (!patchSystemVm(mount.second(), vmName)) { // FIXME make this
                // nonspecific
                String msg = "patch system vm failed";
                s_logger.warn(msg);
                return msg;
            }

            Set<VM> templates = VM.getByNameLabel(conn, "CentOS 5.3");
            if (templates.size() == 0) {
                templates = VM.getByNameLabel(conn, "CentOS 5.3 (64-bit)");
                if (templates.size() == 0) {
                    String msg = " can not find template CentOS 5.3 ";
                    s_logger.warn(msg);
                    return msg;
                }
            }

            VM template = templates.iterator().next();

            vm = template.createClone(conn, vmName);

            vm.removeFromOtherConfig(conn, "disks");

            vm.setPVBootloader(conn, "pygrub");

            long memsize = ramSize * 1024L * 1024L;
            setMemory(conn, vm, memsize);
            vm.setIsATemplate(conn, false);

            vm.setVCPUsAtStartup(conn, 1L);

            Host host = Host.getByUuid(conn, _host.uuid);
            vm.setAffinity(conn, host);

            /* create VBD */
            VBD.Record vbdr = new VBD.Record();

            vbdr.VM = vm;
            vbdr.VDI = mount.second();
            vbdr.bootable = true;
            vbdr.userdevice = "0";
            vbdr.mode = Types.VbdMode.RW;
            vbdr.type = Types.VbdType.DISK;
            VBD.create(conn, vbdr);

            /* create VIF0 */
            VIF.Record vifr = new VIF.Record();
            vifr.VM = vm;
            vifr.device = "0";
            vifr.MAC = privateMacAddr;
            vifr.network = nw0;
            VIF.create(conn, vifr);
            /* create VIF1 */
            vifr.VM = vm;
            vifr.device = "1";
            vifr.MAC = privateMacAddr;
            vifr.network = Network.getByUuid(conn, _host.privateNetwork);
            VIF.create(conn, vifr);
            /* create VIF2 */
            vifr.VM = vm;
            vifr.device = "2";
            vifr.MAC = publicMacAddr;
            vifr.network = Network.getByUuid(conn, _host.publicNetwork);
            if ("untagged".equalsIgnoreCase(vlanId)) {
                vifr.network = Network.getByUuid(conn, _host.publicNetwork);
            } else {
                Network vlanNetwork = enableVlanNetwork(Long.valueOf(vlanId),
                                                        _host.publicNetwork,
                                                        _host.publicPif);

                if (vlanNetwork == null) {
                    throw new InternalErrorException("Failed to enable VLAN network with tag: " + vlanId);
                }

                vifr.network = vlanNetwork;
            }
            VIF.create(conn, vifr);
            /* set up PV dom argument */
            String pvargs = vm.getPVArgs(conn);
            pvargs = pvargs + bootArgs;

            if (s_logger.isInfoEnabled())
                s_logger.info("PV args for system vm are " + pvargs);
            vm.setPVArgs(conn, pvargs);

            /* destroy console */
            Set<Console> consoles = vm.getRecord(conn).consoles;

            for (Console console : consoles) {
                console.destroy(conn);
            }

            /* set action after crash as destroy */
            vm.setActionsAfterCrash(conn, Types.OnCrashBehaviour.DESTROY);

            vm.start(conn, false, true);

            if (s_logger.isInfoEnabled())
                s_logger.info("Ping system vm command port, " + privateIp + ":" + cmdPort);

            state = State.Running;
            String result = connect(vmName, privateIp, cmdPort);
            if (result != null) {
                String msg = "Can not ping System vm " + vmName;
                s_logger.warn(msg);
                return msg;
            } else {
                if (s_logger.isInfoEnabled())
                    s_logger.info("Ping system vm command port succeeded for vm " + vmName);
            }
            return null;

        } catch (XenAPIException e) {
            String msg = "Exception caught while starting System vm " + vmName + " due to " + e.toString();
            s_logger.warn(msg, e);
            startvmfailhandle(vm, mounts);
            state = State.Stopped;
            return msg;
        } catch (Exception e) {
            String msg = "Exception caught while starting System vm " + vmName + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            startvmfailhandle(vm, mounts);
            state = State.Stopped;
            return msg;
        } finally {
            synchronized (_vms) {
                _vms.put(vmName, state);
            }
        }
    }

    // TODO : need to refactor it to reuse code with StartRouter
    protected Answer execute(final StartConsoleProxyCommand cmd) {
        final String vmName = cmd.getVmName();
        final ConsoleProxyVO proxy = cmd.getProxy();
        try {
            Connection conn = getConnection();
            Network network = Network.getByUuid(conn, _host.privateNetwork);
            String bootArgs = cmd.getBootArgs();
            bootArgs += " zone=" + _dcId;
            bootArgs += " pod=" + _pod;
            bootArgs += " guid=Proxy." + proxy.getId();
            bootArgs += " proxy_vm=" + proxy.getId();
            bootArgs += " localgw=" + _localGateway;

            String result = startSystemVM(vmName,
                                          proxy.getVlanId(),
                                          network,
                                          cmd.getVolumes(),
                                          bootArgs,
                                          proxy.getPrivateIpAddress(),
                                          proxy.getPrivateMacAddress(),
                                          proxy.getPublicIpAddress(),
                                          proxy.getPublicMacAddress(),
                                          cmd.getProxyCmdPort(),
                                          proxy.getRamSize());
            if (result == null) {
                return new StartConsoleProxyAnswer(cmd);
            }
            return new StartConsoleProxyAnswer(cmd, result);

        } catch (Exception e) {
            String msg = "Exception caught while starting router vm " + vmName + " due to " + e.getMessage();
            s_logger.warn(msg, e);
            return new StartConsoleProxyAnswer(cmd, msg);
        }
    }

    protected boolean patchSystemVm(VDI vdi, String vmName) {
        if (vmName.startsWith("r-")) {
            return patchSpecialVM(vdi, vmName, "router");
        } else if (vmName.startsWith("v-")) {
            return patchSpecialVM(vdi, vmName, "consoleproxy");
        } else if (vmName.startsWith("s-")) {
            return patchSpecialVM(vdi, vmName, "secstorage");
        } else {
            throw new CloudRuntimeException("Tried to patch unknown type of system vm");
        }
    }

    protected String getUnusedDeviceNum(VM vm) {
        // Figure out the disk number to attach the VM to
        try {
            Connection conn = getConnection();
            Set<String> allowedVBDDevices = vm.getAllowedVBDDevices(conn);
            if (allowedVBDDevices.size() == 0)
                throw new CloudRuntimeException("Could not find an available slot in VM with name: " + vm.getNameLabel(conn) +
                                                " to attach a new disk.");
            return allowedVBDDevices.iterator().next();
        } catch (XmlRpcException e) {
            String msg = "Catch XmlRpcException due to: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Catch XenAPIException due to: " + e.toString();
            s_logger.warn(msg, e);
        }
        throw new CloudRuntimeException("Could not find an available slot in VM with name to attach a new disk.");
    }

    protected boolean patchSpecialVM(VDI vdi, String vmname, String vmtype) {
        // patch special vm here, domr, domp
        VBD vbd = null;
        Connection conn = getConnection();
        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            
            Set<VM> vms = host.getResidentVMs(conn);        

            for (VM vm : vms) {
                VM.Record vmrec = null;
                try {
                    vmrec = vm.getRecord(conn);
                } catch (Exception e) {
                    String msg = "VM.getRecord failed due to " + e.toString() + " " + e.getMessage();
                    s_logger.warn(msg);
                    continue;
                }
                if (vmrec.isControlDomain) {

                    /* create VBD */
                    VBD.Record vbdr = new VBD.Record();
                    vbdr.VM = vm;
                    vbdr.VDI = vdi;
                    vbdr.bootable = false;
                    vbdr.userdevice = getUnusedDeviceNum(vm);
                    vbdr.unpluggable = true;
                    vbdr.mode = Types.VbdMode.RW;
                    vbdr.type = Types.VbdType.DISK;

                    vbd = VBD.create(conn, vbdr);

                    vbd.plug(conn);

                    String device = vbd.getDevice(conn);

                    return patchspecialvm(vmname, device, vmtype);
                }
            }

        } catch (XenAPIException e) {
            String msg = "patchSpecialVM faile on " + _host.uuid + " due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (Exception e) {
            String msg = "patchSpecialVM faile on " + _host.uuid + " due to " + e.getMessage();
            s_logger.warn(msg, e);
        } finally {
            if (vbd != null) {
                try {
                    if (vbd.getCurrentlyAttached(conn)) {
                        vbd.unplug(conn);
                    }
                    vbd.destroy(conn);
                } catch (XmlRpcException e) {
                    String msg = "Catch XmlRpcException due to " + e.getMessage();
                    s_logger.warn(msg, e);
                } catch (XenAPIException e) {
                    String msg = "Catch XenAPIException due to " + e.toString();
                    s_logger.warn(msg, e);
                }

            }
        }
        return false;
    }

    protected boolean patchspecialvm(String vmname, String device, String vmtype) {
        String result = callHostPlugin("patchdomr",
                                       "vmname",
                                       vmname,
                                       "vmtype",
                                       vmtype,
                                       "device",
                                       "/dev/" + device);
        if (result == null || result.isEmpty())
            return false;
        return true;
    }

    protected String callHostPlugin(String cmd, String... params) {
        Map<String, String> args = new HashMap<String, String>();
        try {
            Connection conn = getConnection();
            Host host = Host.getByUuid(conn, _host.uuid);
            for (int i = 0; i < params.length; i += 2) {
                args.put(params[i], params[i + 1]);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("callHostPlugin executing for command " + cmd +
                               " with " +
                               getArgsString(args));
            }

            String result = host.callPlugin(conn, "vmops", cmd, args);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("callHostPlugin Result: " + result);
            }
            return result.replace("\n", "");
        } catch (XenAPIException e) {
            s_logger.warn("callHostPlugin failed for cmd: " + cmd +
                          " with args " +
                          getArgsString(args) +
                          " due to " +
                          e.toString());
        } catch (XmlRpcException e) {
            s_logger.debug("callHostPlugin failed for cmd: " + cmd +
                           " with args " +
                           getArgsString(args) +
                           " due to " +
                           e.getMessage());
        }
        return null;
    }

    protected String getArgsString(Map<String, String> args) {
        StringBuilder argString = new StringBuilder();
        for (Map.Entry<String, String> arg : args.entrySet()) {
            argString.append(arg.getKey() + ": " + arg.getValue() + ", ");
        }
        return argString.toString();
    }

    protected boolean setIptables() {
        String result = callHostPlugin("setIptables");
        if (result == null || result.isEmpty())
            return false;
        return true;
    }

    protected Nic getLocalNetwork(Connection conn, String name)
            throws XmlRpcException, XenAPIException {
        Set<Network> networks = Network.getByNameLabel(conn, name);
        for (Network network : networks) {
            Network.Record nr = network.getRecord(conn);
            for (PIF pif : nr.PIFs) {
                PIF.Record pr = pif.getRecord(conn);
                if (_host.uuid.equals(pr.host.getUuid(conn))) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found a network called " + name +
                                       " on host=" +
                                       _host.ip +
                                       ";  Network=" +
                                       nr.uuid +
                                       "; pif=" +
                                       pr.uuid);
                    }

                    return new Nic(network, nr, pif, pr);
                }
            }
        }

        return null;
    }

    protected VIF getCorrectVif(VM router, String vlanId) {
        try {
            Connection conn = getConnection();
            Set<VIF> routerVIFs = router.getVIFs(conn);
            for (VIF vif : routerVIFs) {
                Network vifNetwork = vif.getNetwork(conn);
                if (vlanId.equals("untagged")) {
                    if (vifNetwork.getUuid(conn).equals(_host.publicNetwork)) {
                        return vif;
                    }
                } else {
                    if (vifNetwork.getNameLabel(conn).equals("VLAN" + vlanId)) {
                        return vif;
                    }
                }
            }
        } catch (XmlRpcException e) {
            String msg = "Caught XmlRpcException: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Caught XenAPIException: " + e.toString();
            s_logger.warn(msg, e);
        }

        return null;
    }

    protected String getLowestAvailableVIFDeviceNum(VM vm) {
        try {
            Connection conn = getConnection();
            Set<String> availableDeviceNums = vm.getAllowedVIFDevices(conn);
            Iterator<String> deviceNumsIterator = availableDeviceNums.iterator();
            List<Integer> sortedDeviceNums = new ArrayList<Integer>();

            while (deviceNumsIterator.hasNext()) {
                try {
                    sortedDeviceNums.add(Integer.valueOf(deviceNumsIterator.next()));
                } catch (NumberFormatException e) {
                    s_logger.debug("Obtained an invalid value for an available VIF device number for VM: " + vm.getNameLabel(conn));
                    return null;
                }
            }

            Collections.sort(sortedDeviceNums);
            return String.valueOf(sortedDeviceNums.get(0));
        } catch (XmlRpcException e) {
            String msg = "Caught XmlRpcException: " + e.getMessage();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Caught XenAPIException: " + e.toString();
            s_logger.warn(msg, e);
        }

        return null;
    }

    protected List<Ternary<SR, VDI, VolumeVO>> mount(List<VolumeVO> vos) {
        ArrayList<Ternary<SR, VDI, VolumeVO>> mounts = new ArrayList<Ternary<SR, VDI, VolumeVO>>(vos.size());

        for (VolumeVO vol : vos) {
            String vdiuuid = vol.getPath();
            SR sr = null;
            VDI vdi = null;
            // Look up the VDI
            vdi = getVDIbyUuid(vdiuuid);

            Ternary<SR, VDI, VolumeVO> ter = new Ternary<SR, VDI, VolumeVO>(sr,
                                                                            vdi,
                                                                            vol);
            mounts.add(ter);
        }
        return mounts;
    }

    protected Network getNetworkByName(String name) throws BadServerResponse,
            XenAPIException, XmlRpcException {
        Connection conn = getConnection();

        Set<Network> networks = Network.getByNameLabel(conn, name);
        if (networks.size() > 0) {
            assert networks.size() == 1 : "How did we find more than one network with this name label" + name +
                                          "?  Strange....";
            return networks.iterator().next(); // Found it.
        }

        return null;
    }

    protected Network enableVlanNetwork(long tag,
                                        String networkUuid,
                                        String pifUuid)
    throws XenAPIException,
           XmlRpcException {
        // In XenServer, vlan is added by
        // 1. creating a network.
        // 2. creating a vlan associating network with the pif.
        // We always create
        // 1. a network with VLAN[vlan id in decimal]
        // 2. a vlan associating the network created with the pif to private
        // network.
        Connection conn = getConnection();

        Network vlanNetwork = null;
        String name = "VLAN" + Long.toString(tag);

        synchronized (name.intern()) {
            vlanNetwork = getNetworkByName(name);
            if (vlanNetwork == null) { // Can't find it, then create it.
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating VLAN for " + tag +
                                   " on host " +
                                   _host.ip);
                }
                Network.Record nwr = new Network.Record();
                nwr.nameLabel = name;
                nwr.bridge = name;
                vlanNetwork = Network.create(conn, nwr);
            }

            PIF nPif = PIF.getByUuid(conn, pifUuid);
            PIF.Record nPifr = nPif.getRecord(conn);

            Network.Record vlanNetworkr = vlanNetwork.getRecord(conn);
            if (vlanNetworkr.PIFs != null) {
                for (PIF pif : vlanNetworkr.PIFs) {
                    PIF.Record pifr = pif.getRecord(conn);
                    if (pifr.device.equals(nPifr.device) && pifr.host.equals(nPifr.host)) {
                        pif.plug(conn);
                        return vlanNetwork;
                    }
                }
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating VLAN " + tag + " on host " + _host.ip);
            }
            VLAN vlan = VLAN.create(conn, nPif, tag, vlanNetwork);
            PIF untaggedPif = vlan.getUntaggedPIF(conn);
            if (!untaggedPif.getCurrentlyAttached(conn)) {
                untaggedPif.plug(conn);
            }
        }

        return vlanNetwork;
    }

    protected String getOsType(String type) {
        return s_osType.get(type);
    }

    protected void disableVlanNetwork(Network network)
            throws InternalErrorException {
        try {
            Connection conn = getConnection();
            if (network.getVIFs(conn).isEmpty()) {
                Iterator<PIF> pifs = network.getPIFs(conn).iterator();
                while (pifs.hasNext()) {
                    PIF pif = pifs.next();
                    pif.unplug(conn);
                }
            }
        } catch (XenAPIException e) {
            String msg = "Unable to disable VLAN network due to " + e.toString();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        } catch (XmlRpcException e) {
            String msg = "Unable to disable VLAN network due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new InternalErrorException(msg);
        } catch (Exception e) {
            throw new InternalErrorException(e.getMessage());
        }
    }

    protected SR getLocalLVMSR() {
        Connection conn = getConnection();

        try {
            Map<SR, SR.Record> map = SR.getAllRecords(conn);
            for (Map.Entry<SR, SR.Record> entry : map.entrySet()) {
                SR.Record srRec = entry.getValue();
                if (SRType.LVM.equals(srRec.type)) {
                    Set<PBD> pbds = srRec.PBDs;
                    if (pbds == null) {
                        continue;
                    }
                    for (PBD pbd : pbds) {
                        Host host = pbd.getHost(conn);
                        if (!isRefNull(host) && host.getUuid(conn).equals(_host.uuid)) {
                            if (!pbd.getCurrentlyAttached(conn)) {
                                pbd.plug(conn);
                            }
                            SR sr = entry.getKey();
                            sr.scan(conn);
                            return sr;
                        }
                    }
                }
            }
        } catch (XenAPIException e) {
            String msg = "Unable to get local LVMSR in host:" + _host.uuid + e.toString();
            s_logger.warn(msg);
        } catch (XmlRpcException e) {
            String msg = "Unable to get local LVMSR in host:" + _host.uuid + e.getCause();
            s_logger.warn(msg);
        }
        return null;

    }

    protected StartupStorageCommand initializeLocalSR() {

        SR lvmsr = getLocalLVMSR();
        if (lvmsr == null) {
            return null;
        }
        try {
            Connection conn = getConnection();
            String lvmuuid = lvmsr.getUuid(conn);
            long cap = lvmsr.getPhysicalSize(conn);
            if (cap < 0)
                return null;
            long avail = cap - lvmsr.getPhysicalUtilisation(conn);
            lvmsr.setNameLabel(conn, lvmuuid);
            String name = "VMOps local storage pool in host : " + _host.uuid;
            lvmsr.setNameDescription(conn, name);
            Host host = Host.getByUuid(conn, _host.uuid);
            String address = host.getAddress(conn);
            StoragePoolInfo pInfo = new StoragePoolInfo(name,
                                                        lvmuuid,
                                                        address,
                                                        SRType.LVM.toString(),
                                                        SRType.LVM.toString(),
                                                        StoragePoolType.LVM,
                                                        cap,
                                                        avail);
            StartupStorageCommand cmd = new StartupStorageCommand();
            cmd.setPoolInfo(pInfo);
            cmd.setGuid(_host.uuid);
            cmd.setResourceType(StorageResourceType.STORAGE_POOL);
            return cmd;
        } catch (XenAPIException e) {
            String msg = "build startupstoragecommand err in host:" + _host.uuid + e.toString();
            s_logger.warn(msg);
        } catch (XmlRpcException e) {
            String msg = "build startupstoragecommand err in host:" + _host.uuid + e.getMessage();
            s_logger.warn(msg);
        }
        return null;

    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {

            if (!pingxenserver()) {
                Thread.sleep(1000);
                if (!pingxenserver()) {
                    s_logger.warn(" can not ping xenserver " + _host.uuid);
                    return null;
                }

            }

            HashMap<String, State> newStates = sync();
            if (newStates == null) {
                newStates = new HashMap<String, State>();
            }
            return new PingRoutingCommand(getType(), id, newStates);
        } catch (Exception e) {
            s_logger.warn("Unable to get current status", e);
            return null;
        }
    }

    @Override
    public Type getType() {
        return com.cloud.host.Host.Type.Routing;
    }

    protected void getPVISO(StartupStorageCommand sscmd) {
        Connection conn = getConnection();
        try {
            Set<VDI> vids = VDI.getByNameLabel(conn, "xs-tools.iso");
            if (vids.isEmpty())
                return;
            VDI pvISO = vids.iterator().next();
            String uuid = pvISO.getUuid(conn);
            Map<String, TemplateInfo> pvISOtmlt = new HashMap<String, TemplateInfo>();
            TemplateInfo tmplt = new TemplateInfo("xs-tools.iso",
                                                  uuid,
                                                  pvISO.getVirtualSize(conn),
                                                  true);
            pvISOtmlt.put("xs-tools", tmplt);
            sscmd.setTemplateInfo(pvISOtmlt);
        } catch (XenAPIException e) {
            s_logger.debug("Can't get xs-tools.iso: " + e.toString());
        } catch (XmlRpcException e) {
            s_logger.debug("Can't get xs-tools.iso: " + e.toString());
        }
    }

    protected boolean getHostInfo() {
        Connection conn = getConnection();

        try {
            Host myself = Host.getByUuid(conn, _host.uuid);
            _host.pool = getPoolUuid();

            String name = "cloud-private";
            if (_privateNetworkName != null) {
                name = _privateNetworkName;
            }

            _localGateway = callHostPlugin("getgateway",
                                           "mgmtIP",
                                           myself.getAddress(conn));
            if (_localGateway == null || _localGateway.isEmpty()) {
                s_logger.warn("can not get gateway for host :" + _host.uuid);
                return false;
            }

            Nic privateNic = getLocalNetwork(conn, name);
            if (privateNic == null) {
                s_logger.debug("Unable to find any private network.  Trying to determine that by route for host " + _host.ip);
                name = callHostPlugin("getnetwork",
                                      "mgmtIP",
                                      myself.getAddress(conn));
                if (name == null || name.isEmpty()) {
                    s_logger.warn("Unable to determine the private network for host " + _host.ip);
                    return false;
                }
                privateNic = getLocalNetwork(conn, name);
                if (privateNic == null) {
                    s_logger.warn("Unable to get private network " + name);
                    return false;
                }
            }

            Nic publicNic = null;
            if (_publicNetworkName != null && !_publicNetworkName.equals(_privateNetworkName)) {
                publicNic = getLocalNetwork(conn, _publicNetworkName);
                if (publicNic == null) {
                    s_logger.warn("Unable to find public network " + _publicNetworkName + " for host " + _host.ip);
                    return false;
                }
            } else {
                publicNic = privateNic;
            }

            _host.privatePif = privateNic.pr.uuid;
            _host.privateNetwork = privateNic.nr.uuid;
            _host.publicPif = publicNic.pr.uuid;
            _host.publicNetwork = publicNic.nr.uuid;
            _privateNetworkName = privateNic.nr.nameLabel;
            _publicNetworkName = publicNic.nr.nameLabel;

            Nic storageNic1 = getLocalNetwork(conn, _storageNetworkName1);
            if (storageNic1 == null) {
                storageNic1 = privateNic;
            }

            _host.storageNetwork1 = storageNic1.nr.uuid;
            _host.storagePif1 = storageNic1.pr.uuid;

            Nic storageNic2 = getLocalNetwork(conn, _storageNetworkName2);
            if (storageNic2 != null) {
                _host.storageNetwork2 = storageNic2.nr.uuid;
                _host.storagePif2 = storageNic2.pr.uuid;
            }

            return true;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to get host information for " + _host.ip, e);
            return false;
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to get host information for " + _host.ip, e);
            return false;
        }
    }

    @Override
    public StartupCommand[] initialize() {
        disconnected();

        setupServer();

        if (!getHostInfo()) {
            s_logger.warn("Unable to get host information for " + _host.ip);
            return null;
        }
        destroyStoppedVm();
        StartupRoutingCommand cmd = new StartupRoutingCommand();
        fillHostInfo(cmd);

        cleanupDiskMounts();

        Map<String, State> changes = null;
        synchronized (_vms) {
            _vms.clear();
            changes = sync();
        }

        _domrIPMap.clear();
        if (changes != null) {
            for (final Map.Entry<String, State> entry : changes.entrySet()) {
                final String vm = entry.getKey();
                State state = entry.getValue();
                if (VirtualMachineName.isValidRouterName(vm) && (state == State.Running)) {
                    syncDomRIPMap(vm);
                }
            }
        }

        cmd.setHypervisorType(HypervisorType.XenServer);
        cmd.setChanges(changes);

        StartupStorageCommand sscmd = initializeLocalSR();

        _host.pool = getPoolUuid();

        if (sscmd != null) {
            /* report pv driver iso */
            getPVISO(sscmd);
            return new StartupCommand[] { cmd, sscmd };
        }

        return new StartupCommand[] { cmd };
    }

    protected String getPoolUuid() {
        Connection conn = getConnection();
        try {
            Map<Pool, Pool.Record> pools = Pool.getAllRecords(conn);
            assert (pools.size() == 1) : "Tell me how pool size can be " + pools.size();
            Pool.Record rec = pools.values().iterator().next();
            return rec.uuid;
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to get pool ", e);
        } catch (XmlRpcException e) {
            throw new CloudRuntimeException("Unable to get pool ", e);
        }
    }

    protected void setupServer() {
        Connection conn = getConnection();

        String version = CitrixResourceBase.class.getPackage()
                                                 .getImplementationVersion();

        try {
            Host host = Host.getByUuid(conn, _host.uuid);
            /* enable host in case it is disabled somehow */
            host.enable(conn);
            /* push patches to XenServer */
            Host.Record hr = host.getRecord(conn);

            Iterator<String> it = hr.tags.iterator();

            while (it.hasNext()) {
                String tag = it.next();
                if (tag.startsWith("vmops-version-")) {
                    if (tag.contains(version)) {
                        s_logger.info(logX(host, "Host " + hr.address +
                                                 " is already setup."));
                        return;
                    } else {
                        it.remove();
                    }
                }
            }

            com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(hr.address, 22);
            try {
                sshConnection.connect(null, 60000, 60000);
                if (!sshConnection.authenticateWithPassword(_username, _password)) {
                    throw new CloudRuntimeException("Unable to authenticate");
                }

                SCPClient scp = new SCPClient(sshConnection);
                File file = new File(_patchPath);

                Properties props = new Properties();
                props.load(new FileInputStream(file));

                String path = _patchPath.substring(0, _patchPath.lastIndexOf(File.separator) + 1);
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    String k = (String) entry.getKey();
                    String v = (String) entry.getValue();

                    assert (k != null && k.length() > 0 && v != null && v.length() > 0) : "Problems with " + k + "=" + v;

                    String[] tokens = v.split(",");
                    String f = null;
                    if (tokens.length == 3 && tokens[0].length() > 0) {
                        if (tokens[0].startsWith("/")) {
                            f = tokens[0];
                        }
                        else if (tokens[0].startsWith("~")) {
                            String homedir = System.getenv("HOME");
                            f = homedir + tokens[0].substring(1) + k;
                        } else {
                            f = path + tokens[0] + '/' + k;
                        }
                    } else {
                        f = path + k;
                    }
                    String d = tokens[tokens.length - 1];
                    f = f.replace('/', File.separatorChar);

                    String p = "0755";
                    if (tokens.length == 3) {
                        p = tokens[1];
                    } else if (tokens.length == 2) {
                        p = tokens[0];
                    }

                    if (!new File(f).exists()) {
                        s_logger.warn("We cannot locate " + f);
                        continue;
                    }
                    s_logger.info("Copying " + f +
                                  " to " +
                                  d +
                                  " on " +
                                  hr.address +
                                  " with permission " +
                                  p);
                    scp.put(f, d, p);
                    s_logger.info("Copied " + f +
                                  " to " +
                                  d +
                                  " on " +
                                  hr.address +
                                  " with permission " +
                                  p);
                }
            } catch (IOException e) {
                throw new CloudRuntimeException("Unable to setup the server correctly", e);
            } finally {
                sshConnection.close();
            }

            if (!setIptables()) {
                s_logger.warn("set xenserver Iptable failed");
            }

            hr.tags.add("vmops-version-" + version);
            host.setTags(conn, hr.tags);
        } catch (XenAPIException e) {
            String msg = "Xen setup failed due to " + e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information " + e.toString(), e);
        } catch (XmlRpcException e) {
            String msg = "Xen setup failed due to " + e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException("Unable to get host information ", e);
        }
    }

    protected SR getSRByNameLabelandHost(String name) throws BadServerResponse,
            XenAPIException, XmlRpcException {
        Connection conn = getConnection();
        Set<SR> srs = SR.getByNameLabel(conn, name);
        SR ressr = null;
        for (SR sr : srs) {
            Set<PBD> pbds;
            pbds = sr.getPBDs(conn);
            for (PBD pbd : pbds) {
                PBD.Record pbdr = pbd.getRecord(conn);
                if (pbdr.host != null &&
                    pbdr.host.getUuid(conn).equals(_host.uuid)) {
                    if (!pbdr.currentlyAttached) {
                        pbd.plug(conn);
                    }
                    ressr = sr;
                    break;
                }
            }
        }
        return ressr;
    }

    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {

        try {
            Connection conn = getConnection();
            Set<SR> srs = SR.getByNameLabel(conn, cmd.getStorageId());

            if (srs.size() != 1) {
                String msg = "There are " + srs.size() +
                             " storageid: " +
                             cmd.getStorageId();
                s_logger.warn(msg);
                return new GetStorageStatsAnswer(cmd, msg);
            }

            SR sr = srs.iterator().next();

            sr.scan(conn);
            long capacity = sr.getPhysicalSize(conn);
            long used = sr.getPhysicalUtilisation(conn);
            return new GetStorageStatsAnswer(cmd, capacity, used);
        } catch (XenAPIException e) {
            String msg = "GetStorageStats Exception:" + e.toString() +
                         "host:" +
                         _host.uuid +
                         "storageid: " +
                         cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        } catch (XmlRpcException e) {
            String msg = "GetStorageStats Exception:" + e.getMessage() +
                         "host:" +
                         _host.uuid +
                         "storageid: " +
                         cmd.getStorageId();
            s_logger.warn(msg);
            return new GetStorageStatsAnswer(cmd, msg);
        }
    }

    protected boolean checkSR(SR sr) {

        try {
            Connection conn = getConnection();
            SR.Record srr = sr.getRecord(conn);
            Set<PBD> pbds = sr.getPBDs(conn);
            if (pbds.size() == 0) {
                String msg = "There is no PBDs for this SR: " + _host.uuid;
                s_logger.warn(msg);
                return false;
            }
            Set<Host> hosts = Host.getAll(conn);
            for (Host host : hosts) {
                boolean found = false;
                for (PBD pbd : pbds) {
                    if (host.equals(pbd.getHost(conn))) {
                        PBD.Record pbdr = pbd.getRecord(conn);
                        if (currentlyAttached(sr, srr, pbd, pbdr)) {
                            if (!pbdr.currentlyAttached) {
                                pbd.plug(conn);
                            }
                        } else {
                            if (pbdr.currentlyAttached) {
                                pbd.unplug(conn);
                            }
                            pbd.plug(conn);
                        }
                        pbds.remove(pbd);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    PBD.Record pbdr =
                        srr.PBDs.iterator().next().getRecord(conn);
                    pbdr.host = host;
                    pbdr.uuid = "";
                    PBD pbd = PBD.create(conn, pbdr);
                    pbd.plug(conn);
                }
            }

        } catch (Exception e) {
            String msg = "checkSR failed host:" + _host.uuid;
            s_logger.warn(msg);
            return false;
        }
        return true;
    }

    protected Answer execute(ModifyStoragePoolCommand cmd) {
        StoragePoolVO pool = cmd.getPool();
        try {
            Connection conn = getConnection();

            SR sr = getStorageRepository(conn, pool);
            if (!checkSR(sr)) {
                String msg = "ModifyStoragePoolCommand checkSR failed! host:" + _host.uuid +
                             " pool: " +
                             pool.getName() +
                             pool.getHostAddress() +
                             pool.getPath();
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            }
            sr.setNameLabel(conn, pool.getUuid());
            sr.setNameDescription(conn, pool.getName());
            long capacity = sr.getPhysicalSize(conn);
            long available = capacity - sr.getPhysicalUtilisation(conn);
            if (capacity == -1) {
                String msg = "Pool capacity is -1! pool: " + pool.getName() +
                             pool.getHostAddress() +
                             pool.getPath();
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            }
            Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
            ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd,
                                                                         capacity,
                                                                         available,
                                                                         tInfo);
            return answer;
        } catch (XenAPIException e) {
            String msg = "ModifyStoragePoolCommand XenAPIException:" + e.toString() +
                         " host:" +
                         _host.uuid +
                         " pool: " +
                         pool.getName() +
                         pool.getHostAddress() +
                         pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            String msg = "ModifyStoragePoolCommand XenAPIException:" + e.getMessage() +
                         " host:" +
                         _host.uuid +
                         " pool: " +
                         pool.getName() +
                         pool.getHostAddress() +
                         pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    protected Answer execute(DeleteStoragePoolCommand cmd) {
        StoragePoolVO pool = cmd.getPool();
        try {
            Connection conn = getConnection();
            SR sr = getStorageRepository(conn, pool);
            if (!checkSR(sr)) {
                String msg = "DeleteStoragePoolCommand checkSR failed! host:" + _host.uuid +
                             " pool: " +
                             pool.getName() +
                             pool.getHostAddress() +
                             pool.getPath();
                s_logger.warn(msg);
                return new Answer(cmd, false, msg);
            }
            sr.setNameLabel(conn, pool.getUuid());
            sr.setNameDescription(conn, pool.getName());

            Answer answer = new Answer(cmd, true, "success");
            return answer;
        } catch (XenAPIException e) {
            String msg = "DeleteStoragePoolCommand XenAPIException:" + e.toString() +
                         " host:" +
                         _host.uuid +
                         " pool: " +
                         pool.getName() +
                         pool.getHostAddress() +
                         pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        } catch (Exception e) {
            String msg = "DeleteStoragePoolCommand XenAPIException:" + e.getMessage() +
                         " host:" +
                         _host.uuid +
                         " pool: " +
                         pool.getName() +
                         pool.getHostAddress() +
                         pool.getPath();
            s_logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }

    }

    public Connection getConnection() {
        return _connPool.connect(_host.uuid,
                                 _host.ip,
                                 _username,
                                 _password,
                                 _wait);
    }

    protected void fillHostInfo(StartupRoutingCommand cmd) {
        long speed = 0;
        int cpus = 0;
        long ram = 0;

        Connection conn = getConnection();

        long dom0Ram = 0;
        final StringBuilder caps = new StringBuilder();
        try {

            Host host = Host.getByUuid(conn, _host.uuid);
            Host.Record hr = host.getRecord(conn);

            Map<String, String> details = cmd.getHostDetails();
            if (details == null) {
                details = new HashMap<String, String>();
            }
            details.put("private.network.device", _privateNetworkName);
            details.put("public.network.device", _publicNetworkName);
            cmd.setHostDetails(details);
            cmd.setName(hr.nameLabel);
            cmd.setGuid(_host.uuid);
            cmd.setDataCenter(Long.toString(_dcId));
            for (final String cap : hr.capabilities) {
                if (cap.length() > 0) {
                    caps.append(cap).append(" , ");
                }
            }
            if (caps.length() > 0) {
                caps.delete(caps.length() - 3, caps.length());
            }
            cmd.setCaps(caps.toString());

            Set<HostCpu> hcs = host.getHostCPUs(conn);
            cpus = hcs.size();
            for (final HostCpu hc : hcs) {
                speed = hc.getSpeed(conn);
            }
            cmd.setSpeed(speed);
            cmd.setCpus(cpus);

            long free = 0;
            HostMetrics hm = host.getMetrics(conn);
            HostMetrics.Record hmr = hm.getRecord(conn);
            ram = hmr.memoryTotal;
            free = hmr.memoryFree;

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Total Ram: " + ram + " Free Ram: " + free);
            }

            Set<VM> vms = host.getResidentVMs(conn);
            for (VM vm : vms) {
                final VM.Record record = vm.getRecord(conn);
                if (record.powerState == Types.VmPowerState.RUNNING && !record.isControlDomain) {
                    free += record.memoryDynamicMin;
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Adding memory " + record.memoryDynamicMin +
                                       " used by vm: " +
                                       record.nameLabel);
                    }
                }
            }
            dom0Ram = ram - free;
            cmd.setMemory(ram);
            cmd.setDom0MinMemory(dom0Ram);

            PIF pif = PIF.getByUuid(conn, _host.privatePif);
            PIF.Record pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setPrivateIpAddress(pifr.IP);
                cmd.setPrivateMacAddress(pifr.MAC);
                cmd.setPrivateNetmask(pifr.netmask);
            }

            pif = PIF.getByUuid(conn, _host.storagePif1);
            pifr = pif.getRecord(conn);
            if (pifr.IP != null && pifr.IP.length() > 0) {
                cmd.setStorageIpAddress(pifr.IP);
                cmd.setStorageMacAddress(pifr.MAC);
                cmd.setStorageNetmask(pifr.netmask);
            }

            if (_host.storagePif2 != null) {
                pif = PIF.getByUuid(conn, _host.storagePif2);
                pifr = pif.getRecord(conn);
                if (pifr.IP != null && pifr.IP.length() > 0) {
                    cmd.setStorageIpAddressDeux(pifr.IP);
                    cmd.setStorageMacAddressDeux(pifr.MAC);
                    cmd.setStorageNetmaskDeux(pifr.netmask);
                }
            }

            Map<String, String> configs = hr.otherConfig;
            cmd.setIqn(configs.get("iscsi_iqn"));

            cmd.setPod(_pod);
            cmd.setVersion(CitrixResourceBase.class.getPackage()
                                                   .getImplementationVersion());

        } catch (final XmlRpcException e) {
            throw new CloudRuntimeException("XML RPC Exception" + e.getMessage(), e);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("XenAPIException" + e.toString(), e);
        }
    }

    public CitrixResourceBase() {
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        _name = name;
        _host.uuid = (String) params.get("guid");
        try {
            _dcId = Long.parseLong((String) params.get("zone"));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Unable to get the zone " + params.get("zone"));
        }
        _name = _host.uuid;
        _host.ip = (String) params.get("url");
        _username = (String) params.get("username");
        _password = (String) params.get("password");
        _pod = (String) params.get("pod");
        _privateNetworkName = (String) params.get("private.network.device");
        _publicNetworkName = (String) params.get("public.network.device");

        _storageNetworkName1 = (String) params.get("storage.network.device1");
        if (_storageNetworkName1 == null) {
            _storageNetworkName1 = "cloud-stor1";
        }
        _storageNetworkName2 = (String) params.get("storage.network.device2");
        if (_storageNetworkName2 == null) {
            _storageNetworkName2 = "cloud-stor2";
        }

        String value = (String) params.get("wait");
        _wait = NumbersUtil.parseInt(value, 1800);

        if (_pod == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_host.ip == null) {
            throw new ConfigurationException("Unable to get the host address");
        }

        if (_username == null) {
            throw new ConfigurationException("Unable to get the username");
        }

        if (_password == null) {
            throw new ConfigurationException("Unable to get the password");
        }

        if (_host.uuid == null) {
            throw new ConfigurationException("Unable to get the uuid");
        }

        params.put("domr.scripts.dir", "scripts/network/domr");

        String patchPath = (String) params.get("xenserver.patch.dir");
        if (patchPath == null) {
            patchPath = "scripts/vm/hypervisor/xenserver/patch";
        }

        _patchPath = Script.findScript(patchPath, "patch");
        if (_patchPath == null) {
            throw new ConfigurationException("Unable to find all of patch files for xenserver");
        }

        _storage = (StorageLayer) params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            value = (String) params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }

            try {
                Class<?> clazz = Class.forName(value);
                _storage = (StorageLayer) ComponentLocator.inject(clazz);
                _storage.configure("StorageLayer", params);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }

        return true;
    }

    protected void addVolumes(final List<VolumeVO> vols,
                              StoragePoolVO pool,
                              VDI vdi,
                              Volume.VolumeType type) {

        Connection conn = getConnection();

        VDI.Record vdir;
        try {
            vdir = vdi.getRecord(conn);

            VolumeVO vol = new VolumeVO(null,
                                        vdir.nameLabel,
                                        -1,
                                        -1,
                                        -1,
                                        -1,
                                        new Long(-1),
                                        pool.getUuid(),
                                        vdir.uuid,
                                        vdir.virtualSize,
                                        type);
            vol.setNameLabel(vdir.nameLabel);
            vols.add(vol);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Exception " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("There is no Local storage for this xenserver");
        }

    }

    void destroyVDI(VDI vdi) {
        try {
            Connection conn = getConnection();
            vdi.destroy(conn);

        } catch (Exception e) {
            String msg = "destroy VDI failed due to " + e.toString();
            s_logger.warn(msg);
        }
    }

    protected CreateAnswer execute(final CreateCommand cmd) {
        VDI rootvdi = null;
        VDI datavdi = null;
        try {
            Connection conn = getConnection();

            List<VolumeVO> vols = new ArrayList<VolumeVO>();

            StoragePoolVO pool = cmd.getPool();

            SR poolsr = getStorageRepository(conn, pool);
            if (poolsr == null) {
                String msg = "can not find storage pool in host " + _host.uuid +
                             " with name " +
                             pool.getUuid();
                s_logger.warn(msg);
                return new CreateAnswer(cmd, msg);

            }

            String tmpltname = cmd.getTemplatePath();

            if (tmpltname == null) {
                // create blank root virtual disk

                if (cmd.getRootDiskSizeByte() > 0) {
                    VDI.Record vdir = new VDI.Record();
                    vdir.nameLabel = cmd.getVmName() + "-ROOT";
                    vdir.SR = poolsr;
                    vdir.type = Types.VdiType.USER;
                    vdir.virtualSize = cmd.getRootDiskSizeByte();

                    rootvdi = VDI.create(conn, vdir);
                    addVolumes(vols, pool, rootvdi, Volume.VolumeType.ROOT);
                }

                if (cmd.getDataDiskSizeByte() > 0) {
                    VDI.Record vdir = new VDI.Record();
                    vdir.nameLabel = cmd.getVmName() + "-DATA";
                    vdir.SR = poolsr;
                    vdir.type = Types.VdiType.USER;
                    vdir.virtualSize = cmd.getDataDiskSizeByte();

                    datavdi = VDI.create(conn, vdir);
                    addVolumes(vols, pool, datavdi, Volume.VolumeType.DATADISK);
                }

            } else {
                VDI tmpltvdi = null;

                tmpltvdi = getVDIbyUuid(tmpltname);
                rootvdi = tmpltvdi.createClone(conn,
                                               new HashMap<String, String>());
                rootvdi.setNameLabel(conn, cmd.getVmName() + "-ROOT");

                addVolumes(vols, pool, rootvdi, Volume.VolumeType.ROOT);

                if (cmd.getDataDiskSizeByte() > 0) {
                    VDI.Record vdir = new VDI.Record();
                    vdir.nameLabel = cmd.getVmName() + "-DATA";
                    vdir.SR = poolsr;
                    vdir.type = Types.VdiType.USER;
                    vdir.virtualSize = cmd.getDataDiskSizeByte();
                    datavdi = VDI.create(conn, vdir);
                    addVolumes(vols, pool, datavdi, Volume.VolumeType.DATADISK);
                }
            }

            for (VolumeVO vol : vols) {
                vol.setStorageResourceType(getStorageResourceType());
            }

            return new CreateAnswer(cmd, vols);
        } catch (XenAPIException e) {
            String msg = "create vm failed due to " + e.toString();
            s_logger.warn(msg, e);
            if (rootvdi != null) {
                destroyVDI(rootvdi);
            }
            if (datavdi != null) {
                destroyVDI(datavdi);
            }
            return new CreateAnswer(cmd, msg);
        } catch (Exception e) {
            String msg = "create vm failed due to " + e.getMessage();
            s_logger.warn(msg, e);
            if (rootvdi != null) {
                destroyVDI(rootvdi);
            }
            if (datavdi != null) {
                destroyVDI(datavdi);
            }
            return new CreateAnswer(cmd, msg);
        }
    }

    protected SR getISOSRbyVmName(String vmName) {

        Connection conn = getConnection();
        try {
            Set<SR> srs = SR.getByNameLabel(conn, vmName + "-ISO");
            if (srs.size() == 0) {
                return null;
            } else if (srs.size() == 1) {
                return srs.iterator().next();
            } else {
                String msg = "getIsoSRbyVmName failed due to there are more than 1 SR having same Label";
                s_logger.warn(msg);
            }
        } catch (XenAPIException e) {
            String msg = "getIsoSRbyVmName failed due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (Exception e) {
            String msg = "getIsoSRbyVmName failed due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
        return null;
    }

    protected SR createNfsSRbyURI(URI uri, boolean shared) {
        try {
            Connection conn = getConnection();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating a " +
                                (shared ? "shared SR for " : "not shared SR for ") +
                                uri);
            }

            Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("server", uri.getHost());
            deviceConfig.put("serverpath", path);
            String name = UUID.nameUUIDFromBytes(new String(uri.getHost() + path).getBytes()).toString();
            if (!shared) {
                Set<SR> srs = SR.getByNameLabel(conn, name);
                for (SR sr : srs) {
                    SR.Record record = sr.getRecord(conn);
                    if (SRType.NFS.equals(record.type) &&
                        record.contentType.equals("user") &&
                        !record.shared) {
                        removeSRSync(sr);
                    }
                }
            }

            Host host = Host.getByUuid(conn, _host.uuid);

            SR sr = SR.create(conn,
                              host,
                              deviceConfig,
                              new Long(0),
                              "",
                              uri.getHost() + uri.getPath(),
                              SRType.NFS.toString(),
                              "user",
                              shared,
                              new HashMap<String, String>());

            sr.setNameLabel(conn, name);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(logX(sr,
                                    "Created a SR; UUID is " + sr.getUuid(conn)));
            }
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            String msg = "Can not create second storage SR mountpoint: " + uri.getHost() +
                         uri.getPath() +
                         " due to " +
                         e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "Can not create second storage SR mountpoint: " + uri.getHost() +
                         uri.getPath() +
                         " due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected SR createIsoSRbyURI(URI uri, String vmName, boolean shared) {
        try {
            Connection conn = getConnection();

            Map<String, String> deviceConfig = new HashMap<String, String>();
            String path = uri.getPath();
            path = path.replace("//", "/");
            deviceConfig.put("location", uri.getHost() + ":" + uri.getPath());
            Host host = Host.getByUuid(conn, _host.uuid);
            SR sr = SR.create(conn,
                              host,
                              deviceConfig,
                              new Long(0),
                              uri.getHost() + uri.getPath(),
                              "iso",
                              "iso",
                              "iso",
                              shared,
                              new HashMap<String, String>());
            sr.setNameLabel(conn, vmName + "-ISO");
            sr.setNameDescription(conn, deviceConfig.get("location"));
            sr.scan(conn);
            return sr;
        } catch (XenAPIException e) {
            String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() +
                         uri.getPath() +
                         " due to " +
                         e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "createIsoSRbyURI failed! mountpoint: " + uri.getHost() +
                         uri.getPath() +
                         " due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected VDI getVDIbyLocationandSR(String loc, SR sr) {
        Connection conn = getConnection();
        try {
            Set<VDI> vdis = sr.getVDIs(conn);
            for (VDI vdi : vdis) {
                if (vdi.getLocation(conn).startsWith(loc)) {
                    return vdi;
                }
            }

            String msg = "can not getVDIbyLocationandSR " + loc;
            s_logger.warn(msg);
            return null;
        } catch (XenAPIException e) {
            String msg = "getVDIbyLocationandSR exception " + loc +
                         " due to " +
                         e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "getVDIbyLocationandSR exception " + loc +
                         " due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }

    }

    protected VDI getVDIbyUuid(String uuid) {
        try {
            Connection conn = getConnection();
            return VDI.getByUuid(conn, uuid);
        } catch (XenAPIException e) {
            String msg = "VDI getByUuid failed " + uuid +
                         " due to " +
                         e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "VDI getByUuid failed " + uuid +
                         " due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        }

    }

    protected SR getIscsiSR(Connection conn, StoragePoolVO pool) {

        synchronized (pool.getUuid().intern()) {
            Map<String, String> deviceConfig = new HashMap<String, String>();
            try {
                String target = pool.getHostAddress().trim();
                String path = pool.getPath().trim();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }

                String tmp[] = path.split("/");
                if (tmp.length != 3) {
                    String msg = "Wrong iscsi path " + pool.getPath() +
                                 " it should be /targetIQN/LUN";
                    s_logger.warn(msg);
                    throw new CloudRuntimeException(msg);
                }
                String targetiqn = tmp[1].trim();
                String lunid = tmp[2].trim();
                String scsiid = "";

                Set<SR> srs = SR.getByNameLabel(conn, pool.getUuid());
                for (SR sr : srs) {
                    if (!SRType.LVMOISCSI.equals(sr.getType(conn)))
                        continue;

                    Set<PBD> pbds = sr.getPBDs(conn);
                    if (pbds.isEmpty())
                        continue;

                    PBD pbd = pbds.iterator().next();

                    Map<String, String> dc = pbd.getDeviceConfig(conn);

                    if (dc == null)
                        continue;

                    if (dc.get("target") == null)
                        continue;

                    if (dc.get("targetIQN") == null)
                        continue;

                    if (dc.get("lunid") == null)
                        continue;

                    if (target.equals(dc.get("target")) && targetiqn.equals(dc.get("targetIQN")) &&
                        lunid.equals(dc.get("lunid"))) {
                        return sr;
                    }

                }
                deviceConfig.put("target", target);
                deviceConfig.put("targetIQN", targetiqn);

                Host host = Host.getByUuid(conn, _host.uuid);
                SR sr = null;
                try {
                    sr = SR.create(conn,
                                   host,
                                   deviceConfig,
                                   new Long(0),
                                   pool.getUuid(),
                                   pool.getName(),
                                   SRType.LVMOISCSI.toString(),
                                   "user",
                                   true,
                                   new HashMap<String, String>());
                } catch (XenAPIException e) {
                    String errmsg = e.toString();
                    if (errmsg.contains("SR_BACKEND_FAILURE_107")) {
                        String lun[] = errmsg.split("<LUN>");
                        boolean found = false;
                        for (int i = 1; i < lun.length; i++) {
                            int blunindex = lun[i].indexOf("<LUNid>") + 7;
                            int elunindex = lun[i].indexOf("</LUNid>");
                            String ilun = lun[i].substring(blunindex, elunindex);
                            ilun = ilun.trim();
                            if (ilun.equals(lunid)) {
                                int bscsiindex = lun[i].indexOf("<SCSIid>") + 8;
                                int escsiindex = lun[i].indexOf("</SCSIid>");
                                scsiid = lun[i].substring(bscsiindex,
                                                          escsiindex);
                                scsiid = scsiid.trim();
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String msg = "can not find LUN " + lunid +
                                         " in " +
                                         errmsg;
                            s_logger.warn(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        String msg = "Unable to create Iscsi SR  " + deviceConfig +
                                     " due to  " +
                                     e.toString();
                        s_logger.warn(msg, e);
                        throw new CloudRuntimeException(msg, e);
                    }
                }
                deviceConfig.put("SCSIid", scsiid);
                sr = SR.create(conn,
                               host,
                               deviceConfig,
                               new Long(0),
                               pool.getUuid(),
                               pool.getName(),
                               SRType.LVMOISCSI.toString(),
                               "user",
                               true,
                               new HashMap<String, String>());
                sr.scan(conn);
                return sr;

            } catch (XenAPIException e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig +
                             " due to  " +
                             e.toString();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            } catch (Exception e) {
                String msg = "Unable to create Iscsi SR  " + deviceConfig +
                             " due to  " +
                             e.getMessage();
                s_logger.warn(msg, e);
                throw new CloudRuntimeException(msg, e);
            }
        }
    }

    protected SR getNfsSR(StoragePoolVO pool) {
        Connection conn = getConnection();

        Map<String, String> deviceConfig = new HashMap<String, String>();
        try {

            String server = pool.getHostAddress();
            String serverpath = pool.getPath();
            serverpath = serverpath.replace("//", "/");
            Set<SR> srs = SR.getAll(conn);
            for (SR sr : srs) {
                if (!SRType.NFS.equals(sr.getType(conn)))
                    continue;

                Set<PBD> pbds = sr.getPBDs(conn);
                if (pbds.isEmpty())
                    continue;

                PBD pbd = pbds.iterator().next();

                Map<String, String> dc = pbd.getDeviceConfig(conn);

                if (dc == null)
                    continue;

                if (dc.get("server") == null)
                    continue;

                if (dc.get("serverpath") == null)
                    continue;

                if (server.equals(dc.get("server")) && serverpath.equals(dc.get("serverpath"))) {
                    return sr;
                }

            }

            deviceConfig.put("server", server);
            deviceConfig.put("serverpath", serverpath);
            Host host = Host.getByUuid(conn, _host.uuid);
            SR sr = SR.create(conn,
                              host,
                              deviceConfig,
                              new Long(0),
                              pool.getUuid(),
                              pool.getName(),
                              SRType.NFS.toString(),
                              "user",
                              true,
                              new HashMap<String, String>());
            sr.scan(conn);
            return sr;

        } catch (XenAPIException e) {
            String msg = "Unable to create NFS SR  " + deviceConfig +
                         " due to  " +
                         e.toString();
            s_logger.warn(msg, e);
            throw new CloudRuntimeException(msg, e);
        } catch (Exception e) {
            String msg = "Unable to create NFS SR  " + deviceConfig +
                         " due to  " +
                         e.getMessage();
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    protected Answer execute(DestroyCommand cmd) {
        List<VolumeVO> vols;
        vols = cmd.getVolumes();

        Connection conn = getConnection();
        for (VolumeVO vol : vols) {
            // Look up the VDI
            String volumeUUID = vol.getPath();
            VDI vdi = null;
            try {
                vdi = getVDIbyUuid(volumeUUID);
            } catch (Exception e) {
                String msg = "getVDIbyUuid failed due to " + e.toString();
                s_logger.warn(msg);
                continue;
            }
            Set<VBD> vbds = null;
            try {
                vbds = vdi.getVBDs(conn);
            } catch (Exception e) {
                String msg = "VDI getVBDS failed due to " + e.toString();
                s_logger.warn(msg);
                continue;
            }
            for (VBD vbd : vbds) {
                try {
                    vbd.unplug(conn);
                    vbd.destroy(conn);
                } catch (Exception e) {
                    String msg = "VM destroy failed due to " + e.toString();
                    s_logger.warn(msg);
                }
            }
            try {
                vdi.destroy(conn);
            } catch (Exception e) {
                String msg = "VDI destroy failed due to " + e.toString();
                s_logger.warn(msg);
                continue;
            }
        }

        return new Answer(cmd, true, "Success");
    }

    protected ShareAnswer execute(final ShareCommand cmd) {
        if (!cmd.isShare()) {
            SR sr = getISOSRbyVmName(cmd.getVmName());
            Connection conn = getConnection();
            try {
                if (sr != null) {
                    Set<VM> vms = VM.getByNameLabel(conn, cmd.getVmName());
                    if (vms.size() == 0) {
                        removeSR(sr);
                    }
                }
            } catch (Exception e) {
                String msg = "SR.getNameLabel failed due to  " + e.getMessage() +
                             e.toString();
                s_logger.warn(msg);
            }
        }
        return new ShareAnswer(cmd, new HashMap<String, Integer>());
    }

    protected CopyVolumeAnswer execute(final CopyVolumeCommand cmd) {
        String volumeUUID = cmd.getVolumePath();
        StoragePoolVO pool = cmd.getPool();
        String secondaryStorageURL = cmd.getSecondaryStorageURL();

        URI uri = null;
        try {
            uri = new URI(secondaryStorageURL);
        } catch (URISyntaxException e) {
            return new CopyVolumeAnswer(cmd,
                                        false,
                                        "Invalid secondary storage URL specified.",
                                        null,
                                        null);
        }

        String remoteVolumesMountPath = uri.getHost() + ":" +
                                        uri.getPath() +
                                        "/volumes/";
        String volumeFolder = String.valueOf(cmd.getVolumeId()) + "/";
        boolean toSecondaryStorage = cmd.toSecondaryStorage();

        String errorMsg = "Failed to copy volume";
        SR primaryStoragePool = null;
        SR secondaryStorage = null;
        VDI srcVolume = null;
        VDI destVolume = null;
        Connection conn = getConnection();
        try {
            if (toSecondaryStorage) {
                // Create the volume folder
                if (!createSecondaryStorageFolder(remoteVolumesMountPath,
                                                  volumeFolder)) {
                    throw new InternalErrorException("Failed to create the volume folder.");
                }

                // Create a SR for the volume UUID folder
                secondaryStorage = createNfsSRbyURI(new URI(secondaryStorageURL + "/volumes/" +
                                                            volumeFolder),
                                                    false);

                // Look up the volume on the source primary storage pool
                srcVolume = getVDIbyUuid(volumeUUID);

                // Copy the volume to secondary storage
                destVolume = srcVolume.copy(conn, secondaryStorage);
            } else {
                // Mount the volume folder
                secondaryStorage = createNfsSRbyURI(new URI(secondaryStorageURL + "/volumes/" +
                                                            volumeFolder),
                                                    false);

                // Look up the volume on secondary storage
                Set<VDI> vdis = secondaryStorage.getVDIs(conn);
                for (VDI vdi : vdis) {
                    if (vdi.getUuid(conn).equals(volumeUUID)) {
                        srcVolume = vdi;
                        break;
                    }
                }

                if (srcVolume == null) {
                    throw new InternalErrorException("Failed to find volume on secondary storage.");
                }

                // Copy the volume to the primary storage pool
                primaryStoragePool = getStorageRepository(conn, pool);
                destVolume = srcVolume.copy(conn, primaryStoragePool);
            }

            String srUUID;

            if (primaryStoragePool == null) {
                srUUID = secondaryStorage.getUuid(conn);
            } else {
                srUUID = primaryStoragePool.getUuid(conn);
            }

            String destVolumeUUID = destVolume.getUuid(conn);

            return new CopyVolumeAnswer(cmd, true, null, srUUID, destVolumeUUID);
        } catch (XenAPIException e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new CopyVolumeAnswer(cmd, false, e.toString(), null, null);
        } catch (Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new CopyVolumeAnswer(cmd, false, e.getMessage(), null, null);
        } finally {
            if (!toSecondaryStorage && srcVolume != null) {
                // Delete the volume on secondary storage
                destroyVDI(srcVolume);
            }

            removeSR(secondaryStorage);
            if (!toSecondaryStorage) {
                // Delete the volume folder on secondary storage
                deleteSecondaryStorageFolder(remoteVolumesMountPath,
                                             volumeFolder);
            }
        }

    }

    protected ManageVolumeAnswer execute(final ManageVolumeCommand cmd) {
        boolean add = cmd.getAdd();
        String volumeUUID = cmd.getPath();
        StoragePoolVO pool = cmd.getPool();

        String op = add ? "add" : "delete";
        s_logger.debug("Trying to " + op +
                       " volume with UUID: " +
                       volumeUUID +
                       " on primary storage pool: " +
                       pool.getName());

        String errorMsg;
        if (add) {
            errorMsg = "Failed to create volume";
        } else {
            errorMsg = "Failed to delete volume";
        }

        // Make sure the storage pool is valid
        if (add && pool.getUuid().isEmpty()) {
            return new ManageVolumeAnswer(cmd,
                                          false,
                                          "Pool doesn't have pool uuid",
                                          null,
                                          null,
                                          null);
        }

        VDI vdi = null;
        SR secondaryStorage = null;
        Connection conn = getConnection();
        try {
            if (add) {
                // Find a storage repository on the pool
                SR vmsr = getStorageRepository(conn, pool);

                // Create a new data volume
                VDI.Record vdir = new VDI.Record();
                vdir.SR = vmsr;
                vdir.type = Types.VdiType.USER;
                vdir.virtualSize = cmd.getDiskSizeByte();
                vdir.nameLabel = "detached";
                vdi = VDI.create(conn, vdir);

                // Determine the UUID and size of the created data volume
                vdir = vdi.getRecord(conn);
                String uuid = vdir.uuid;
                long createdSize = vdir.virtualSize;

                // Determine the name of the SR
                String srName = vmsr.getNameLabel(conn);

                return new ManageVolumeAnswer(cmd,
                                              true,
                                              null,
                                              createdSize,
                                              srName,
                                              uuid);
            } else {
                // Look up the VDI on primary storage
                vdi = getVDIbyUuid(volumeUUID);

                // Destroy the VDI
                vdi.destroy(conn);

                return new ManageVolumeAnswer(cmd, true, null, null, null, null);
            }
        } catch (XenAPIException e) {
            if (add && vdi != null) {
                destroyVDI(vdi);
            }
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new ManageVolumeAnswer(cmd,
                                          false,
                                          e.toString(),
                                          null,
                                          null,
                                          null);
        } catch (Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new ManageVolumeAnswer(cmd,
                                          false,
                                          e.getMessage(),
                                          null,
                                          null,
                                          null);
        } finally {
            removeSR(secondaryStorage);
        }
    }

    protected Answer execute(final AttachVolumeCommand cmd) {
        boolean attach = cmd.getAttach();
        String vmName = cmd.getVmName();
        String volumeUUID = cmd.getVolumePath();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach volume";
        } else {
            errorMsg = "Failed to detach volume";
        }

        Connection conn = getConnection();
        try {
            // Look up the VDI
            VDI vdi = getVDIbyUuid(volumeUUID);
            // Look up the VM
            VM vm = getVM(conn, vmName);
            /* For HVM guest, if no pv driver installed, no attach/detach */
            boolean isHVM;
            if (vm.getPVBootloader(conn).equalsIgnoreCase(""))
                isHVM = true;
            else
                isHVM = false;
            VMGuestMetrics vgm = vm.getGuestMetrics(conn);
            boolean pvDrvInstalled = false;
            if (!isRefNull(vgm) && vgm.getPVDriversUpToDate(conn)) {
                pvDrvInstalled = true;
            }
            if (isHVM && !pvDrvInstalled) {
                s_logger.warn(errorMsg + ": You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected");
                return new Answer(cmd,
                                  false,
                                  "You attempted an operation that requires PV drivers to be installed on the VM. Please install them by inserting xen-pv-drv.iso.");
            }
            if (attach) {
                // Figure out the disk number to attach the VM to
                String diskNumber = getUnusedDeviceNum(vm);

                // Create a new VBD
                VBD.Record vbdr = new VBD.Record();
                vbdr.VM = vm;
                vbdr.VDI = vdi;
                vbdr.bootable = false;
                vbdr.userdevice = diskNumber;
                vbdr.mode = Types.VbdMode.RW;
                vbdr.type = Types.VbdType.DISK;
                vbdr.unpluggable = true;
                VBD vbd = VBD.create(conn, vbdr);

                // Attach the VBD to the VM
                vbd.plug(conn);

                // Update the VDI's label to include the VM name
                vdi.setNameLabel(conn, vmName + "-DATA");

                return new Answer(cmd);
            } else {
                // Look up all VBDs for this VDI
                Set<VBD> vbds = vdi.getVBDs(conn);

                // Detach each VBD from its VM, and then destroy it
                for (VBD vbd : vbds) {
                    VBD.Record vbdr = vbd.getRecord(conn);

                    if (vbdr.currentlyAttached) {
                        vbd.unplug(conn);
                    }

                    vbd.destroy(conn);
                }

                // Update the VDI's label to be "detached"
                vdi.setNameLabel(conn, "detached");

                return new Answer(cmd);
            }
        } catch (XenAPIException e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.toString());
        } catch (Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.getMessage());
        }

    }

    protected Answer execute(final AttachIsoCommand cmd) {
        boolean attach = cmd.isAttach();
        String vmName = cmd.getVmName();
        String isoURL = cmd.getIsoPath();

        String errorMsg;
        if (attach) {
            errorMsg = "Failed to attach ISO";
        } else {
            errorMsg = "Failed to detach ISO";
        }

        Connection conn = getConnection();
        try {
            if (attach) {
                VBD isoVBD = null;

                // Find the VM
                VM vm = getVM(conn, vmName);

                // Find the ISO VDI
                VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                // Find the VM's CD-ROM VBD
                Set<VBD> vbds = vm.getVBDs(conn);
                for (VBD vbd : vbds) {
                    String userDevice = vbd.getUserdevice(conn);
                    Types.VbdType type = vbd.getType(conn);

                    if (userDevice.equals("3") && type == Types.VbdType.CD) {
                        isoVBD = vbd;
                        break;
                    }
                }

                if (isoVBD == null) {
                    throw new CloudRuntimeException("Unable to find CD-ROM VBD for VM: " + vmName);
                } else {
                    // If an ISO is already inserted, eject it
                    if (isoVBD.getEmpty(conn) == false) {
                        isoVBD.eject(conn);
                    }

                    // Insert the new ISO
                    isoVBD.insert(conn, isoVDI);
                }

                return new Answer(cmd);
            } else {
                // Find the VM
                VM vm = getVM(conn, vmName);
                String vmUUID = vm.getUuid(conn);

                // Find the ISO VDI
                VDI isoVDI = getIsoVDIByURL(conn, vmName, isoURL);

                SR sr = isoVDI.getSR(conn);

                // Look up all VBDs for this VDI
                Set<VBD> vbds = isoVDI.getVBDs(conn);

                // Iterate through VBDs, and if the VBD belongs the VM, eject
                // the ISO from it
                for (VBD vbd : vbds) {
                    VM vbdVM = vbd.getVM(conn);
                    String vbdVmUUID = vbdVM.getUuid(conn);

                    if (vbdVmUUID.equals(vmUUID)) {
                        // If an ISO is already inserted, eject it
                        if (!vbd.getEmpty(conn)) {
                            vbd.eject(conn);
                        }

                        break;
                    }
                }

                if (!sr.getNameLabel(conn).startsWith("XenServer Tools")) {
                    removeSR(sr);
                }

                return new Answer(cmd);
            }
        } catch (XenAPIException e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.toString());
        } catch (Exception e) {
            s_logger.warn(errorMsg + ": " + e.toString(), e);
            return new Answer(cmd, false, e.getMessage());
        }
    }

    protected ValidateSnapshotAnswer execute(final ValidateSnapshotCommand cmd) {
        String primaryStoragePoolNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        String volumeUuid = cmd.getVolumeUuid(); // Precondition: not null
        String firstBackupUuid = cmd.getFirstBackupUuid();
        String previousSnapshotUuid = cmd.getPreviousSnapshotUuid();
        String templateUuid = cmd.getTemplateUuid();

        // By default assume failure
        String details = "Could not validate previous snapshot backup UUID " + "because the primary Storage SR could not be created from the name label: " +
                         primaryStoragePoolNameLabel;
        boolean success = false;
        String expectedSnapshotBackupUuid = null;
        String actualSnapshotBackupUuid = null;
        String actualSnapshotUuid = null;

        Boolean isISCSI = false;
        String primaryStorageSRUuid = null;
        Connection conn = getConnection();
        try {
            SR primaryStorageSR = getSRByNameLabelandHost(primaryStoragePoolNameLabel);

            if (primaryStorageSR != null) {
                primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
                isISCSI = SRType.LVMOISCSI.equals(primaryStorageSR.getType(conn));
            }
        } catch (BadServerResponse e) {
            details += ", reason: " + e.getMessage();
            s_logger.error(details, e);
        } catch (XenAPIException e) {
            details += ", reason: " + e.getMessage();
            s_logger.error(details, e);
        } catch (XmlRpcException e) {
            details += ", reason: " + e.getMessage();
            s_logger.error(details, e);
        }

        if (primaryStorageSRUuid != null) {
            if (templateUuid == null) {
                templateUuid = "";
            }
            if (firstBackupUuid == null) {
                firstBackupUuid = "";
            }
            if (previousSnapshotUuid == null) {
                previousSnapshotUuid = "";
            }
            String result = callHostPlugin("validateSnapshot",
                                           "primaryStorageSRUuid",
                                           primaryStorageSRUuid,
                                           "volumeUuid",
                                           volumeUuid,
                                           "firstBackupUuid",
                                           firstBackupUuid,
                                           "previousSnapshotUuid",
                                           previousSnapshotUuid,
                                           "templateUuid",
                                           templateUuid,
                                           "isISCSI",
                                           isISCSI.toString());
            if (result == null || result.isEmpty()) {
                details = "Validating snapshot backup for volume with UUID: " + volumeUuid +
                          " failed because there was an exception in the plugin";
                // callHostPlugin exception which has been logged already
            } else {
                String[] uuids = result.split("#", -1);
                if (uuids.length >= 3) {
                    expectedSnapshotBackupUuid = uuids[1];
                    actualSnapshotBackupUuid = uuids[2];
                }
                if (uuids.length >= 4) {
                    actualSnapshotUuid = uuids[3];
                } else {
                    actualSnapshotUuid = "";
                }
                if (uuids[0].equals("1")) {
                    success = true;
                    details = null;
                } else {
                    details = "Previous snapshot backup on the primary storage is invalid. " + "Expected: " +
                              expectedSnapshotBackupUuid +
                              " Actual: " +
                              actualSnapshotBackupUuid;
                    // success is still false
                }
                s_logger.debug("ValidatePreviousSnapshotBackup returned " + " success: " +
                               success +
                               " details: " +
                               details +
                               " expectedSnapshotBackupUuid: " +
                               expectedSnapshotBackupUuid +
                               " actualSnapshotBackupUuid: " +
                               actualSnapshotBackupUuid +
                               " actualSnapshotUuid: " +
                               actualSnapshotUuid);
            }
        }

        return new ValidateSnapshotAnswer(cmd,
                                                        success,
                                                        details,
                                                        expectedSnapshotBackupUuid,
                                                        actualSnapshotBackupUuid,
                                                        actualSnapshotUuid);
    }

    protected ManageSnapshotAnswer execute(final ManageSnapshotCommand cmd) {
        long snapshotId = cmd.getSnapshotId();
        String snapshotName = cmd.getSnapshotName();

        // By default assume failure
        boolean success = false;
        String cmdSwitch = cmd.getCommandSwitch();
        String snapshotOp = "Unsupported snapshot command." + cmdSwitch;
        if (cmdSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
            snapshotOp = "create";
        } else if (cmdSwitch.equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
            snapshotOp = "destroy";
        }
        String details = "ManageSnapshotCommand operation: " + snapshotOp +
                         " Failed for snapshotId: " +
                         snapshotId;
        String snapshotUUID = null;

        Connection conn = getConnection();
        try {
            if (cmdSwitch.equals(ManageSnapshotCommand.CREATE_SNAPSHOT)) {
                // Look up the volume
                String volumeUUID = cmd.getVolumePath();

                VDI volume = getVDIbyUuid(volumeUUID);

                // Create a snapshot
                VDI snapshot = volume.snapshot(conn,
                                               new HashMap<String, String>());

                if (snapshotName != null) {
                    snapshot.setNameLabel(conn, snapshotName);
                }

                // Determine the UUID of the snapshot
                VDI.Record vdir = snapshot.getRecord(conn);
                snapshotUUID = vdir.uuid;

                success = true;
                details = null;
            } else if (cmd.getCommandSwitch()
                          .equals(ManageSnapshotCommand.DESTROY_SNAPSHOT)) {
                // Look up the snapshot
                snapshotUUID = cmd.getSnapshotPath();
                VDI snapshot = getVDIbyUuid(snapshotUUID);

                snapshot.destroy(conn);
                snapshotUUID = null;
                success = true;
                details = null;
            }
        } catch (XenAPIException e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += ", reason: " + e.toString();
            s_logger.warn(details, e);
        }

        return new ManageSnapshotAnswer(cmd,
                                        snapshotId,
                                        snapshotUUID,
                                        success,
                                        details);
    }

    protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateCommand cmd) {
        String secondaryStorageURL = cmd.getSecondaryStorageURL();
        String snapshotUUID = cmd.getSnapshotPath();
        String userSpecifiedName = cmd.getTemplateName();

        SR secondaryStorage = null;
        VDI privateTemplate = null;
        Connection conn = getConnection();
        try {
            URI uri = new URI(secondaryStorageURL);
            String remoteTemplateMountPath = uri.getHost() + ":" +
                                             uri.getPath() +
                                             "/template/";
            String templateFolder = cmd.getAccountId() + "/" +
                                    cmd.getTemplateId() +
                                    "/";
            String templateDownloadFolder = createTemplateDownloadFolder(remoteTemplateMountPath,
                                                                         templateFolder);
            String templateInstallFolder = "tmpl/" + templateFolder;

            // Create a SR for the secondary storage download folder
            secondaryStorage = createNfsSRbyURI(new URI(secondaryStorageURL + "/template/" +
                                                        templateDownloadFolder),
                                                false);

            // Look up the snapshot and copy it to secondary storage
            VDI snapshot = getVDIbyUuid(snapshotUUID);
            privateTemplate = snapshot.copy(conn, secondaryStorage);

            if (userSpecifiedName != null) {
                privateTemplate.setNameLabel(conn, userSpecifiedName);
            }

            // Determine the template file name and install path
            VDI.Record vdir = privateTemplate.getRecord(conn);
            String templateName = vdir.uuid;
            String templateFilename = templateName + ".vhd";
            String installPath = "template/" + templateInstallFolder +
                                 templateFilename;

            // Determine the template's virtual size and then forget the VDI
            long virtualSize = privateTemplate.getVirtualSize(conn);
            // Create the template.properties file in the download folder, move
            // the template and the template.properties file
            // to the install folder, and then delete the download folder
            if (!postCreatePrivateTemplate(remoteTemplateMountPath,
                                           templateDownloadFolder,
                                           templateInstallFolder,
                                           templateFilename,
                                           templateName,
                                           userSpecifiedName,
                                           null,
                                           virtualSize,
                                           cmd.getTemplateId())) {
                throw new InternalErrorException("Failed to create the template.properties file.");
            }

            return new CreatePrivateTemplateAnswer(cmd,
                                                   true,
                                                   null,
                                                   installPath,
                                                   virtualSize,
                                                   templateName,
                                                   ImageFormat.VHD);
        } catch (XenAPIException e) {
            if (privateTemplate != null) {
                destroyVDI(privateTemplate);
            }

            s_logger.warn("CreatePrivateTemplate Failed due to " + e.toString(),
                          e);
            return new CreatePrivateTemplateAnswer(cmd,
                                                   false,
                                                   e.toString(),
                                                   null,
                                                   0,
                                                   null,
                                                   null);
        } catch (Exception e) {
            s_logger.warn("CreatePrivateTemplate Failed due to " + e.getMessage(),
                          e);
            return new CreatePrivateTemplateAnswer(cmd,
                                                   false,
                                                   e.getMessage(),
                                                   null,
                                                   0,
                                                   null,
                                                   null);
        } finally {
            // Remove the secondary storage SR
            removeSR(secondaryStorage);
        }
    }

    private String createTemplateDownloadFolder(String remoteTemplateMountPath,
                                                String templateFolder)
            throws InternalErrorException, URISyntaxException {
        String templateDownloadFolder = "download/" + _host.uuid +
                                        "/" +
                                        templateFolder;

        // Create the download folder
        if (!createSecondaryStorageFolder(remoteTemplateMountPath,
                                          templateDownloadFolder)) {
            throw new InternalErrorException("Failed to create the template download folder.");
        }
        return templateDownloadFolder;
    }

    protected CreatePrivateTemplateAnswer execute(final CreatePrivateTemplateFromSnapshotCommand cmd) {
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolURL = cmd.getSecondaryStoragePoolURL();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        String origTemplateInstallPath = cmd.getOrigTemplateInstallPath();
        Long newTemplateId = cmd.getNewTemplateId();
        String userSpecifiedName = cmd.getTemplateName();

        // By default, assume failure
        String details = "Failed to create private template " + newTemplateId +
                         " from snapshot for volume: " +
                         volumeId +
                         " with backupUuid: " +
                         backedUpSnapshotUuid;
        String newTemplatePath = null;
        boolean result = false;
        long virtualSize = 0;
        try {
            URI uri = new URI(secondaryStoragePoolURL);
            String remoteTemplateMountPath = uri.getHost() + ":" +
                                             uri.getPath() +
                                             "/template/";
            String templateFolder = cmd.getAccountId() + "/" +
                                    newTemplateId +
                                    "/";
            String templateDownloadFolder = createTemplateDownloadFolder(remoteTemplateMountPath,
                                                                         templateFolder);
            String templateInstallFolder = "tmpl/" + templateFolder;
            // Yes, create a template vhd
            Pair<VHDInfo, String> vhdDetails = createVHDFromSnapshot(primaryStorageNameLabel,
                                                                     dcId,
                                                                     accountId,
                                                                     volumeId,
                                                                     secondaryStoragePoolURL,
                                                                     backedUpSnapshotUuid,
                                                                     origTemplateInstallPath,
                                                                     templateDownloadFolder);

            VHDInfo vhdInfo = vhdDetails.first();
            String failureDetails = vhdDetails.second();
            if (vhdInfo == null) {
                if (failureDetails != null) {
                    details += failureDetails;
                }
            } else {
                String templateName = vhdInfo.getUuid();
                String templateFilename = templateName + ".vhd";
                String templateInstallPath = templateInstallFolder + File.separator +
                                             templateFilename;

                newTemplatePath = "template" + File.separator +
                                  templateInstallPath;

                virtualSize = vhdInfo.getVirtualSize();
                // create the template.properties file
                result = postCreatePrivateTemplate(remoteTemplateMountPath,
                                                   templateDownloadFolder,
                                                   templateInstallFolder,
                                                   templateFilename,
                                                   templateName,
                                                   userSpecifiedName,
                                                   null,
                                                   virtualSize,
                                                   newTemplateId);
                if (!result) {
                    details += ", reason: Could not create the template.properties file on secondary storage dir: " + templateInstallFolder;
                } else {
                    // Aaah, success.
                    details = null;
                }

            }
        } catch (XenAPIException e) {
            details += ", reason: " + e.getMessage();
            s_logger.error(details, e);
        } catch (Exception e) {
            details += ", reason: " + e.getMessage();
            s_logger.error(details, e);
        }
        return new CreatePrivateTemplateAnswer(cmd,
                                               result,
                                               details,
                                               newTemplatePath,
                                               virtualSize,
                                               null,
                                               ImageFormat.VHD);
    }

    protected BackupSnapshotAnswer execute(final BackupSnapshotCommand cmd) {
        String primaryStorageNameLabel      = cmd.getPrimaryStoragePoolNameLabel();
        String snapshotUuid                 = cmd.getSnapshotUuid(); // not null: Precondition.
        String volumeUuid                   = cmd.getVolumeUuid();
        Long dcId                           = cmd.getDataCenterId();
        Long accountId                      = cmd.getAccountId();
        Long volumeId                       = cmd.getVolumeId();
        String secondaryStoragePoolURL      = cmd.getSecondaryStoragePoolURL();
        String lastBackedUpSnapshotUuid     = cmd.getLastBackedUpSnapshotUuid();
        String firstBackupUuid              = cmd.getFirstBackupUuid();
        String prevSnapshotUuid             = cmd.getPrevSnapshotUuid();
        boolean isFirstSnapshotOfRootVolume = cmd.isFirstSnapshotOfRootVolume();

        // By default assume failure
        String details = null;
        boolean success = false;
        String snapshotBackupUuid = null;
        try {
            Connection conn = getConnection();
            SR primaryStorageSR = getSRByNameLabelandHost(primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not backup snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            String primaryStorageSRUuid = primaryStorageSR.getUuid(conn);
            Boolean isISCSI = SRType.LVMOISCSI.equals(primaryStorageSR.getType(conn));

            URI uri = new URI(secondaryStoragePoolURL);
            String secondaryStorageMountPath = uri.getHost() + ":" +
                                               uri.getPath();

            if (secondaryStorageMountPath == null) {
                details = "Couldn't backup snapshot because the URL passed: " + secondaryStoragePoolURL +
                          " is invalid.";
            } else {
                boolean gcHappened = true;
                if (prevSnapshotUuid != null) {
                    assert firstBackupUuid != null;
                    gcHappened = waitForGC(primaryStorageSRUuid, prevSnapshotUuid, firstBackupUuid, isISCSI);
                }
                if (gcHappened) {
                    snapshotBackupUuid = backupSnapshot(primaryStorageSRUuid,
                                                        volumeUuid,
                                                        dcId,
                                                        accountId,
                                                        volumeId,
                                                        secondaryStorageMountPath,
                                                        lastBackedUpSnapshotUuid,
                                                        firstBackupUuid,
                                                        isFirstSnapshotOfRootVolume,
                                                        isISCSI);
                    success = (snapshotBackupUuid != null);
                }
                else {
                    s_logger.warn("GC hasn't happened yet for previousBackupSnapshotUuid: " + lastBackedUpSnapshotUuid + ". Will retry again after 1 min");
                }
            }

            if (!success) {
                // Mark the snapshot as removed in the database.
                // When the next snapshot is taken, it will be
                // 1) deleted from the DB 2) The snapshotUuid will be deleted from the primary
                // 3) the snapshotBackupUuid will be copied to secondary
                // 4) if possible it will be coalesced with the next snapshot.

            } else if (prevSnapshotUuid != null) {
                // Destroy the previous snapshot, if it exists.
                // We destroy the previous snapshot only if the current snapshot
                // backup succeeds.
                // The aim is to keep the VDI of the last 'successful' snapshot
                // so that it doesn't get merged with the
                // new one
                // and muddle the vhd chain on the secondary storage.
                details = "Successfully backedUp the snapshotUuid: " + snapshotUuid +
                          " to secondary storage.";
                destroySnapshotOnPrimaryStorage(prevSnapshotUuid);
            }

        } catch (XenAPIException e) {
            details = "BackupSnapshot Failed due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details = "BackupSnapshot Failed due to " + e.getMessage();
            s_logger.warn(details, e);
        }

        return new BackupSnapshotAnswer(cmd,
                                        success,
                                        details,
                                        snapshotBackupUuid);
    }

    protected CreateVolumeFromSnapshotAnswer execute(final CreateVolumeFromSnapshotCommand cmd) {
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolURL = cmd.getSecondaryStoragePoolURL();
        String backedUpSnapshotUuid = cmd.getSnapshotUuid();
        String templatePath = cmd.getTemplatePath();

        // By default, assume the command has failed and set the params to be
        // passed to CreateVolumeFromSnapshotAnswer appropriately
        boolean result = false;
        // Generic error message.
        String details = "Failed to create volume from snapshot for volume: " + volumeId +
                         " with backupUuid: " +
                         backedUpSnapshotUuid;
        String vhdUUID = null;
        SR temporarySROnSecondaryStorage = null;
        String mountPointOfTemporaryDirOnSecondaryStorage = null;
        try {
            VDI vdi = null;
            Connection conn = getConnection();
            SR primaryStorageSR = getSRByNameLabelandHost(primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                throw new InternalErrorException("Could not create volume from snapshot because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }

            Boolean isISCSI = SRType.LVMOISCSI.equals(primaryStorageSR.getType(conn));

            // Get the absolute path of the template on the secondary storage.
            URI uri = new URI(secondaryStoragePoolURL);
            String secondaryStorageMountPath = uri.getHost() + ":" +
                                               uri.getPath();

            if (secondaryStorageMountPath == null) {
                details += " because the URL passed: " + secondaryStoragePoolURL +
                           " is invalid.";
                return new CreateVolumeFromSnapshotAnswer(cmd,
                                                          result,
                                                          details,
                                                          vhdUUID);
            }

            // Create a volume and not a template
            String templateDownloadFolder = "";

            VHDInfo vhdInfo = createVHDFromSnapshot(dcId,
                                                    accountId,
                                                    volumeId,
                                                    secondaryStorageMountPath,
                                                    backedUpSnapshotUuid,
                                                    templatePath,
                                                    templateDownloadFolder,
                                                    isISCSI);
            if (vhdInfo == null) {
                details += " because the vmops plugin on XenServer failed at some point";
            } else {
                vhdUUID = vhdInfo.getUuid();
                String tempDirRelativePath = "snapshots" + File.separator +
                                             accountId +
                                             File.separator +
                                             volumeId +
                                             "_temp";
                mountPointOfTemporaryDirOnSecondaryStorage =
                    secondaryStorageMountPath + File.separator + tempDirRelativePath;

                uri = new URI("nfs://" + mountPointOfTemporaryDirOnSecondaryStorage);
                // No need to check if the SR already exists. It's a temporary
                // SR destroyed when this method exits.
                // And two createVolumeFromSnapshot operations cannot proceed at
                // the same time.
                temporarySROnSecondaryStorage = createNfsSRbyURI(uri, false);
                if (temporarySROnSecondaryStorage == null) {
                    details += "because SR couldn't be created on " + mountPointOfTemporaryDirOnSecondaryStorage;
                } else {
                    s_logger.debug("Successfully created temporary SR on secondary storage " + temporarySROnSecondaryStorage.getNameLabel(conn) +
                                   "with uuid " +
                                   temporarySROnSecondaryStorage.getUuid(conn) +
                                   " and scanned it");
                    // createNFSSRbyURI also scans the SR and introduces the VDI

                    vdi = getVDIbyUuid(vhdUUID);

                    if (vdi != null) {
                        s_logger.debug("Successfully created VDI on secondary storage SR " + temporarySROnSecondaryStorage.getNameLabel(conn) +
                                       " with uuid " +
                                       vhdUUID);
                        s_logger.debug("Copying VDI: " + vdi.getLocation(conn) +
                                       " from secondary to primary");
                        VDI vdiOnPrimaryStorage = vdi.copy(conn,
                                                           primaryStorageSR);
                        // vdi.copy introduces the vdi into the database. Don't
                        // need to do a scan on the primary
                        // storage.

                        if (vdiOnPrimaryStorage != null) {
                            vhdUUID = vdiOnPrimaryStorage.getUuid(conn);
                            s_logger.debug("Successfully copied and introduced VDI on primary storage with path " + vdiOnPrimaryStorage.getLocation(conn) +
                                           " and uuid " +
                                           vhdUUID);
                            result = true;
                            details = null;

                        } else {
                            details += ". Could not copy the vdi " + vhdUUID +
                                       " to primary storage";
                        }
                        
                        // The VHD on temporary was scanned and introduced as a VDI
                        // destroy it as we don't need it anymore.
                        vdi.destroy(conn);
                    } else {
                        details += ". Could not scan and introduce vdi with uuid: " + vhdUUID;
                    }
                }
            }
        } catch (XenAPIException e) {
            details += " due to " + e.toString();
            s_logger.warn(details, e);
        } catch (Exception e) {
            details += " due to " + e.getMessage();
            s_logger.warn(details, e);
        } finally {
            // In all cases, if the temporary SR was created, forget it.
            if (temporarySROnSecondaryStorage != null) {
                removeSR(temporarySROnSecondaryStorage);
                // Delete the temporary directory created.
                File folderPath = new File(mountPointOfTemporaryDirOnSecondaryStorage);
                String remoteMountPath = folderPath.getParent();
                String folder = folderPath.getName();
                deleteSecondaryStorageFolder(remoteMountPath, folder);
            }
        }
        if (!result) {
            // Is this logged at a higher level?
            s_logger.error(details);
        }

        // In all cases return something.
        return new CreateVolumeFromSnapshotAnswer(cmd, result, details, vhdUUID);
    }

    protected DeleteSnapshotBackupAnswer execute(final DeleteSnapshotBackupCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolURL = cmd.getSecondaryStoragePoolURL();
        String backupUUID = cmd.getSnapshotUuid();
        String childUUID = cmd.getChildUUID();
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();

        String details = null;
        boolean success = false;

        SR primaryStorageSR = null;
        Boolean isISCSI = false;
        try {
            Connection conn = getConnection();
            primaryStorageSR = getSRByNameLabelandHost(primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                details = "Could not delete snapshot backup because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel;
                throw new InternalErrorException("Could not delete snapshot backup because the primary Storage SR could not be created from the name label: " + primaryStorageNameLabel);
            }
            isISCSI = SRType.LVMOISCSI.equals(primaryStorageSR.getType(conn));
        } catch (XenAPIException e) {
            details = "Couldn't determine primary SR type " + e.getMessage();
            s_logger.error(details, e);
        } catch (Exception e) {
            details = "Couldn't determine primary SR type " + e.getMessage();
            s_logger.error(details, e);
        }

        if (primaryStorageSR != null) {
            URI uri = null;
            try {
                uri = new URI(secondaryStoragePoolURL);
            } catch (URISyntaxException e) {
                details = "Error finding the secondary storage URL" + e.getMessage();
                s_logger.error(details, e);
            }
            if (uri != null) {
                String secondaryStorageMountPath = uri.getHost() + ":" +
                                                   uri.getPath();

                if (secondaryStorageMountPath == null) {
                    details = "Couldn't delete snapshot because the URL passed: " + secondaryStoragePoolURL +
                              " is invalid.";
                } else {
                    details = deleteSnapshotBackup(dcId,
                                                   accountId,
                                                   volumeId,
                                                   secondaryStorageMountPath,
                                                   backupUUID,
                                                   childUUID,
                                                   isISCSI);
                    success = (details != null && details.equals("1"));
                    if (success) {
                        s_logger.debug("Successfully deleted snapshot backup " + backupUUID);
                    }
                }
            }
        }
        return new DeleteSnapshotBackupAnswer(cmd, success, details);
    }

    protected Answer execute(DeleteSnapshotsDirCommand cmd) {
        Long dcId = cmd.getDataCenterId();
        Long accountId = cmd.getAccountId();
        Long volumeId = cmd.getVolumeId();
        String secondaryStoragePoolURL = cmd.getSecondaryStoragePoolURL();
        String snapshotUUID = cmd.getSnapshotUuid();
        String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();

        String details = null;
        boolean success = false;

        SR primaryStorageSR = null;
        try {
            primaryStorageSR = getSRByNameLabelandHost(primaryStorageNameLabel);
            if (primaryStorageSR == null) {
                details = "Could not delete snapshotsDir for volume " + volumeId +
                          " because the primary Storage SR could not be created from the name label: " +
                          primaryStorageNameLabel;
            }
        } catch (XenAPIException e) {
            details = "Couldn't determine primary SR type " + e.getMessage();
            s_logger.error(details, e);
        } catch (Exception e) {
            details = "Couldn't determine primary SR type " + e.getMessage();
            s_logger.error(details, e);
        }

        if (primaryStorageSR != null) {
            if (snapshotUUID != null) {
                VDI snapshotVDI = getVDIbyUuid(snapshotUUID);
                if (snapshotVDI != null) {
                    destroyVDI(snapshotVDI);
                }
            }
        }
        URI uri = null;
        try {
            uri = new URI(secondaryStoragePoolURL);
        } catch (URISyntaxException e) {
            details = "Error finding the secondary storage URL" + e.getMessage();
            s_logger.error(details, e);
        }
        if (uri != null) {
            String secondaryStorageMountPath = uri.getHost() + ":" +
                                               uri.getPath();

            if (secondaryStorageMountPath == null) {
                details = "Couldn't delete snapshotsDir because the URL passed: " + secondaryStoragePoolURL +
                          " is invalid.";
            } else {
                details = deleteSnapshotsDir(dcId,
                                             accountId,
                                             volumeId,
                                             secondaryStorageMountPath);
                success = (details != null && details.equals("1"));
                if (success) {
                    s_logger.debug("Successfully deleted snapshotsDir for volume: " + volumeId);
                }
            }
        }

        return new Answer(cmd, success, details);
    }

    private VM getVM(Connection conn, String vmName) {
        // Look up VMs with the specified name
        Set<VM> vms;
        try {
            vms = VM.getByNameLabel(conn, vmName);
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to get " + vmName + ": " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get " + vmName + ": " + e.getMessage(), e);
        }

        // If there are no VMs, throw an exception
        if (vms.size() == 0)
            throw new CloudRuntimeException("VM with name: " + vmName + " does not exist.");

        // If there is more than one VM, print a warning
        if (vms.size() > 1)
            s_logger.warn("Found " + vms.size() + " VMs with name: " + vmName);

        // Return the first VM in the set
        return vms.iterator().next();
    }

    protected VDI getIsoVDIByURL(Connection conn, String vmName, String isoURL) {
        SR isoSR = null;
        String mountpoint = null;
        if (isoURL.startsWith("xs-tools")) {
            try {
                Set<VDI> vdis = VDI.getByNameLabel(conn, isoURL);
                if (vdis.isEmpty()) {
                    throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
                }
                return vdis.iterator().next();

            } catch (XenAPIException e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            } catch (Exception e) {
                throw new CloudRuntimeException("Unable to get pv iso: " + isoURL + " due to " + e.toString());
            }
        }

        int index = isoURL.lastIndexOf("/");
        mountpoint = isoURL.substring(0, index);

        URI uri;
        try {
            uri = new URI(mountpoint);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            throw new CloudRuntimeException("isoURL is wrong: " + isoURL);
        }
        isoSR = getISOSRbyVmName(vmName);
        if (isoSR == null) {
            isoSR = createIsoSRbyURI(uri, vmName, false);
        }

        String isoName = isoURL.substring(index + 1);

        VDI isoVDI = getVDIbyLocationandSR(isoName, isoSR);

        if (isoVDI != null) {
            return isoVDI;
        } else {
            throw new CloudRuntimeException("Could not find ISO with URL: " + isoURL);
        }
    }

    protected SR getStorageRepository(Connection conn, StoragePoolVO pool) {
        Set<SR> srs;
        try {
            srs = SR.getByNameLabel(conn, pool.getUuid());
        } catch (XenAPIException e) {
            throw new CloudRuntimeException("Unable to get SR " + pool.getUuid() + " due to " + e.toString(), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to get SR " + pool.getUuid() + " due to " + e.getMessage(), e);
        }

        if (srs.size() > 1) {
            throw new CloudRuntimeException("More than one storage repository was found for pool with uuid: " + pool.getUuid());
        } else if (srs.size() == 1) {
            SR sr = srs.iterator().next();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("SR retrieved for " + pool.getId() +
                               " is mapped to " +
                               sr.toString());
            }
            return sr;
        } else {
            if (pool.getPoolType() == StoragePool.StoragePoolType.NetworkFilesystem)
                return getNfsSR(pool);
            else if (pool.getPoolType() == StoragePool.StoragePoolType.IscsiLUN)
                return getIscsiSR(conn, pool);
            else
                throw new CloudRuntimeException("The pool type: " + pool.getPoolType().name() + " is not supported.");
        }
    }

    protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd,
                                    cmd.getProxyVmId(),
                                    cmd.getProxyVmName(),
                                    cmd.getProxyManagementIp(),
                                    cmd.getProxyCmdPort());
    }

    protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
        return executeProxyLoadScan(cmd,
                                    cmd.getProxyVmId(),
                                    cmd.getProxyVmName(),
                                    cmd.getProxyManagementIp(),
                                    cmd.getProxyCmdPort());
    }

    protected CreateZoneVlanAnswer execute(CreateZoneVlanCommand cmd) {
        Connection conn = getConnection();
        try {
            final DomainRouter router = cmd.getRouter();
            VM vm = getVM(conn, router.getInstanceName());
            // ToDo: Using vif 3 for now. Sync with multiple public VLAN feature
            // to avoid conflict
            createVIF(conn,
                      vm,
                      router.getGuestZoneMacAddress(),
                      router.getZoneVlan(),
                      0,
                      "3",
                      true);
            return new CreateZoneVlanAnswer(cmd);
        } catch (XenAPIException e) {
            String msg = "Exception caught while creating zone vlan: " + e.toString();
            s_logger.warn(msg, e);
            return new CreateZoneVlanAnswer(cmd, msg);
        } catch (Exception e) {
            String msg = "Exception caught while creating zone vlan: " + e.getMessage();
            s_logger.warn(msg, e);
            return new CreateZoneVlanAnswer(cmd, msg);
        }
    }

    protected Answer executeProxyLoadScan(final Command cmd,
                                          final long proxyVmId,
                                          final String proxyVmName,
                                          final String proxyManagementIp,
                                          final int cmdPort) {
        String result = null;

        final StringBuffer sb = new StringBuffer();
        sb.append("http://")
          .append(proxyManagementIp)
          .append(":" + cmdPort)
          .append("/cmd/getstatus");

        boolean success = true;
        try {
            final URL url = new URL(sb.toString());
            final URLConnection conn = url.openConnection();

            // setting TIMEOUTs to avoid possible waiting until death situations
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

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
        } catch (final IOException e) {
            s_logger.warn("Unable to open console proxy command port url, console proxy address : " + proxyManagementIp);
            success = false;
        }

        return new ConsoleProxyLoadAnswer(cmd,
                                          proxyVmId,
                                          proxyVmName,
                                          success,
                                          result);
    }

    protected boolean createSecondaryStorageFolder(String remoteMountPath,
                                                   String newFolder) {
        String result = callHostPlugin("create_secondary_storage_folder",
                                       "remoteMountPath",
                                       remoteMountPath,
                                       "newFolder",
                                       newFolder);
        return (result != null);
    }

    protected boolean deleteSecondaryStorageFolder(String remoteMountPath,
                                                   String folder) {
        String result = callHostPlugin("delete_secondary_storage_folder",
                                       "remoteMountPath",
                                       remoteMountPath,
                                       "folder",
                                       folder);
        return (result != null);
    }

    protected boolean postCreatePrivateTemplate(String remoteTemplateMountPath,
                                                String templateDownloadFolder,
                                                String templateInstallFolder,
                                                String templateFilename,
                                                String templateName,
                                                String templateDescription,
                                                String checksum,
                                                long virtualSize,
                                                long templateId) {

        if (templateDescription == null) {
            templateDescription = "";
        }

        if (checksum == null) {
            checksum = "";
        }

        String result = callHostPlugin("post_create_private_template",
                                       "remoteTemplateMountPath",
                                       remoteTemplateMountPath,
                                       "templateDownloadFolder",
                                       templateDownloadFolder,
                                       "templateInstallFolder",
                                       templateInstallFolder,
                                       "templateFilename",
                                       templateFilename,
                                       "templateName",
                                       templateName,
                                       "templateDescription",
                                       templateDescription,
                                       "checksum",
                                       checksum,
                                       "virtualSize",
                                       String.valueOf(virtualSize),
                                       "templateId",
                                       String.valueOf(templateId));

        boolean success = false;
        if (result != null && !result.isEmpty()) {
            // Else, command threw an exception which has already been logged.

            String[] tmp = result.split("#");
            String status = tmp[0];

            if (status != null && status.equalsIgnoreCase("1")) {
                s_logger.debug("Successfully created template.properties file on secondary storage dir: " + templateInstallFolder);
                success = true;
            } else {
                s_logger.warn("Could not create template.properties file on secondary storage dir: " + templateInstallFolder +
                              " for templateId: " +
                              templateId +
                              ". Failed with status " +
                              status);
            }
        }

        return success;
    }

    protected boolean waitForGC(String primaryStorageSRUuid,
                                String previousSnapshotUuid,
                                String firstBackupUuid,
                                Boolean isISCSI)
    {
        boolean success = false;
        String result = callHostPlugin("validatePreviousSnapshotBackup",
                                       "primaryStorageSRUuid",
                                       primaryStorageSRUuid,
                                       "previousSnapshotUuid",
                                       previousSnapshotUuid,
                                       "firstBackupUuid",
                                       firstBackupUuid,
                                       "isISCSI",
                                       isISCSI.toString());
        
        
        if (result != null && !result.isEmpty()) {
            String[] expectedActual = result.split("#");
            String status = expectedActual[0];
            
            if (expectedActual.length == 3) {
                String expectedParentOfPreviousSnapshot = expectedActual[1];
                String actualParentOfPreviousSnapshot = expectedActual[2];
                if (status.equals("1") && expectedParentOfPreviousSnapshot.equals(actualParentOfPreviousSnapshot)) {
                    success = true;
                }
                else {
                    s_logger.error("Could not backup snapshot because the previous snapshot hasn't been coalesced by XenServer GC." + 
                                   "Expected parent of previous snapshot: " + expectedParentOfPreviousSnapshot + 
                                   " Actual parent of previous snapshot: " + actualParentOfPreviousSnapshot);
                }
            }
        }
        return success;
    }
    
    // Each argument is put in a separate line for readability.
    // Using more lines does not harm the environment.
    protected String backupSnapshot(String primaryStorageSRUuid,
                                    String volumeUuid,
                                    Long dcId,
                                    Long accountId,
                                    Long volumeId,
                                    String secondaryStorageMountPath,
                                    String lastBackedUpSnapshotUuid,
                                    String firstBackupUuid,
                                    Boolean isFirstSnapshotOfRootVolume,
                                    Boolean isISCSI) {
        String backupSnapshotUuid = null;

        if (lastBackedUpSnapshotUuid == null) {
            lastBackedUpSnapshotUuid = "";
        }
        if (firstBackupUuid == null) {
            firstBackupUuid = "";
        }
        
        // Each argument is put in a separate line for readability.
        // Using more lines does not harm the environment.
        String results = callHostPlugin("backupSnapshot",
                                        "primaryStorageSRUuid",
                                        primaryStorageSRUuid,
                                        "volumeUuid",
                                        volumeUuid,
                                        "dcId",
                                        dcId.toString(),
                                        "accountId",
                                        accountId.toString(),
                                        "volumeId",
                                        volumeId.toString(),
                                        "secondaryStorageMountPath",
                                        secondaryStorageMountPath,
                                        "lastBackedUpSnapshotUuid",
                                        lastBackedUpSnapshotUuid,
                                        "firstBackupUuid",
                                        firstBackupUuid,
                                        "isFirstSnapshotOfRootVolume",
                                        isFirstSnapshotOfRootVolume.toString(),
                                        "isISCSI",
                                        isISCSI.toString());

        if (results == null || results.isEmpty()) {
            // errString is already logged.
            return null;
        }

        String[] tmp = results.split("#");
        String status = tmp[0];
        backupSnapshotUuid = tmp[1];

        // status == "1" if and only if backupSnapshotUuid != null
        // So we don't rely on status value but return backupSnapshotUuid as an
        // indicator of success.
        String failureString = "Could not copy backupUuid: " + backupSnapshotUuid +
                               " of volume: "           + volumeUuid +
                               " from primary storage " + primaryStorageSRUuid +
                               " to secondary storage " + secondaryStorageMountPath;
        if (status != null && status.equalsIgnoreCase("1") &&
            backupSnapshotUuid != null) {
            s_logger.debug("Successfully copied backupUuid: " + backupSnapshotUuid +
                           " of volume: " + volumeUuid +
                           " to secondary storage");
        } else {
            s_logger.debug(failureString + ". Failed with status: " + status);
        }

        return backupSnapshotUuid;
    }

    protected boolean destroySnapshotOnPrimaryStorage(String snapshotUuid) {
        // Precondition snapshotUuid != null
        try {
            Connection conn = getConnection();
            VDI snapshot = getVDIbyUuid(snapshotUuid);
            if (snapshot == null) {
                throw new InternalErrorException("Could not destroy snapshot " + snapshotUuid +
                                                 " because the snapshot VDI was null");
            }
            snapshot.destroy(conn);
            s_logger.debug("Successfully destroyed snapshotUuid: " + snapshotUuid +
                           " on primary storage");
            return true;
        } catch (XenAPIException e) {
            String msg = "Destroy snapshotUuid: " + snapshotUuid +
                         " on primary storage failed due to " +
                         e.toString();
            s_logger.error(msg, e);
        } catch (Exception e) {
            String msg = "Destroy snapshotUuid: " + snapshotUuid +
                         " on primary storage failed due to " +
                         e.getMessage();
            s_logger.warn(msg, e);
        }

        return false;
    }

    protected String deleteSnapshotBackup(Long dcId,
                                          Long accountId,
                                          Long volumeId,
                                          String secondaryStorageMountPath,
                                          String backupUUID,
                                          String childUUID,
                                          Boolean isISCSI) {

        // If anybody modifies the formatting below again, I'll skin them
        String result = callHostPlugin("deleteSnapshotBackup",
                                       "backupUUID",
                                       backupUUID,
                                       "childUUID",
                                       childUUID,
                                       "dcId",
                                       dcId.toString(),
                                       "accountId",
                                       accountId.toString(),
                                       "volumeId",
                                       volumeId.toString(),
                                       "secondaryStorageMountPath",
                                       secondaryStorageMountPath,
                                       "isISCSI",
                                       isISCSI.toString());

        return result;
    }

    protected String deleteSnapshotsDir(Long dcId,
                                        Long accountId,
                                        Long volumeId,
                                        String secondaryStorageMountPath) {
        // If anybody modifies the formatting below again, I'll skin them
        String result = callHostPlugin("deleteSnapshotsDir",
                                       "dcId",
                                       dcId.toString(),
                                       "accountId",
                                       accountId.toString(),
                                       "volumeId",
                                       volumeId.toString(),
                                       "secondaryStorageMountPath",
                                       secondaryStorageMountPath);

        return result;
    }

    // If anybody messes up with the formatting, I'll skin them
    protected Pair<VHDInfo, String> createVHDFromSnapshot(String primaryStorageNameLabel,
                                                          Long dcId,
                                                          Long accountId,
                                                          Long volumeId,
                                                          String secondaryStoragePoolURL,
                                                          String backedUpSnapshotUuid,
                                                          String templatePath,
                                                          String templateDownloadFolder)
            throws XenAPIException, IOException, XmlRpcException,
            InternalErrorException, URISyntaxException {
        // Return values
        String details = null;
        Connection conn = getConnection();
        SR primaryStorageSR = getSRByNameLabelandHost(primaryStorageNameLabel);
        if (primaryStorageSR == null) {
            throw new InternalErrorException("Could not create volume from snapshot " + "because the primary Storage SR could not be created from the name label: " +
                                             primaryStorageNameLabel);
        }

        Boolean isISCSI = SRType.LVMOISCSI.equals(primaryStorageSR.getType(conn));

        // Get the absolute path of the template on the secondary storage.
        URI uri = new URI(secondaryStoragePoolURL);
        String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
        VHDInfo vhdInfo = null;
        if (secondaryStorageMountPath == null) {
            details = " because the URL passed: " + secondaryStoragePoolURL +
                      " is invalid.";
        } else {
            vhdInfo = createVHDFromSnapshot(dcId,
                                            accountId,
                                            volumeId,
                                            secondaryStorageMountPath,
                                            backedUpSnapshotUuid,
                                            templatePath,
                                            templateDownloadFolder,
                                            isISCSI);
            if (vhdInfo == null) {
                details = " because the vmops plugin on XenServer failed at some point";
            }
        }

        return new Pair<VHDInfo, String>(vhdInfo, details);
    }

    protected VHDInfo createVHDFromSnapshot(Long dcId,
                                            Long accountId,
                                            Long volumeId,
                                            String secondaryStorageMountPath,
                                            String backedUpSnapshotUuid,
                                            String templatePath,
                                            String templateDownloadFolder,
                                            Boolean isISCSI) {
        String vdiUUID = null;

        String failureString = "Could not create volume from " + backedUpSnapshotUuid;
        String results = callHostPlugin("createVolumeFromSnapshot",
                                        "dcId",
                                        dcId.toString(),
                                        "accountId",
                                        accountId.toString(),
                                        "volumeId",
                                        volumeId.toString(),
                                        "secondaryStorageMountPath",
                                        secondaryStorageMountPath,
                                        "backedUpSnapshotUuid",
                                        backedUpSnapshotUuid,
                                        "templatePath",
                                        templatePath,
                                        "templateDownloadFolder",
                                        templateDownloadFolder,
                                        "isISCSI",
                                        isISCSI.toString());

        if (results == null || results.isEmpty()) {
            // Command threw an exception which has already been logged.
            return null;
        }
        String[] tmp = results.split("#");
        String status = tmp[0];
        vdiUUID = tmp[1];
        Long virtualSizeInMB = 0L;
        if (tmp.length == 3) {
            virtualSizeInMB = Long.valueOf(tmp[2]);
        }
        // status == "1" if and only if vdiUUID != null
        // So we don't rely on status value but return vdiUUID as an indicator
        // of success.

        if (status != null && status.equalsIgnoreCase("1") && vdiUUID != null) {
            s_logger.debug("Successfully created vhd file with all data on secondary storage : " + vdiUUID);
        } else {
            s_logger.debug(failureString + ". Failed with status " +
                           status +
                           " with vdiUuid " +
                           vdiUUID);
        }
        return new VHDInfo(vdiUUID, virtualSizeInMB * MB);

    }

    protected void syncDomRIPMap(String vm) {
        // VM is a DomR, get its IP and add to domR-IP map
        Connection conn = getConnection();
        VM vm1 = getVM(conn, vm);
        try {
            String pvargs = vm1.getPVArgs(conn);
            if (pvargs != null) {
                pvargs = pvargs.replaceAll(" ", "\n");
                Properties pvargsProps = new Properties();
                pvargsProps.load(new StringReader(pvargs));
                String ip = pvargsProps.getProperty("eth1ip");
                if (ip != null) {
                    _domrIPMap.put(vm, ip);
                }
            }
        } catch (BadServerResponse e) {
            String msg = "Unable to update domR IP map due to: " + e.toString();
            s_logger.warn(msg, e);
        } catch (XenAPIException e) {
            String msg = "Unable to update domR IP map due to: " + e.toString();
            s_logger.warn(msg, e);
        } catch (XmlRpcException e) {
            String msg = "Unable to update domR IP map due to: " + e.toString();
            s_logger.warn(msg, e);
        } catch (IOException e) {
            String msg = "Unable to update domR IP map due to: " + e.toString();
            s_logger.warn(msg, e);
        }
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        disconnected();
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

    protected class Nic {
        public Network        n;
        public Network.Record nr;
        public PIF            p;
        public PIF.Record     pr;

        public Nic(Network n, Network.Record nr, PIF p, PIF.Record pr) {
            this.n = n;
            this.nr = nr;
            this.p = p;
            this.pr = pr;
        }
    }

    // A list of UUIDs that are gathered from the XenServer when
    // the resource first connects to XenServer. These UUIDs do
    // not change over time.
    protected class XenServerHost {
        public String uuid;
        public String ip;
        public String publicNetwork;
        public String privateNetwork;
        public String storageNetwork1;
        public String storageNetwork2;
        public String publicPif;
        public String privatePif;
        public String storagePif1;
        public String storagePif2;
        public String pool;
    }

    private class VHDInfo {
        private final String uuid;
        private final Long   virtualSize;

        public VHDInfo(String uuid, Long virtualSize) {
            this.uuid = uuid;
            this.virtualSize = virtualSize;
        }

        /**
         * @return the uuid
         */
        public String getUuid() {
            return uuid;
        }

        /**
         * @return the virtualSize
         */
        public Long getVirtualSize() {
            return virtualSize;
        }
    }
}
