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
package com.vmops.storage.secondary;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.vmops.agent.api.Answer;
import com.vmops.agent.api.Command;
import com.vmops.agent.api.PingCommand;
import com.vmops.agent.api.PingStorageCommand;
import com.vmops.agent.api.StartupCommand;
import com.vmops.agent.api.StartupStorageCommand;
import com.vmops.agent.api.storage.DownloadCommand;
import com.vmops.agent.api.storage.DownloadProgressCommand;
import com.vmops.host.Host;
import com.vmops.host.Host.Type;
import com.vmops.resource.ServerResource;
import com.vmops.resource.ServerResourceBase;
import com.vmops.storage.StorageLayer;
import com.vmops.storage.Volume;
import com.vmops.storage.StoragePool.StoragePoolType;
import com.vmops.storage.template.DownloadManager;
import com.vmops.storage.template.DownloadManagerImpl;
import com.vmops.storage.template.TemplateInfo;
import com.vmops.utils.component.ComponentLocator;

public class LocalSecondaryStorageResource extends ServerResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(LocalSecondaryStorageResource.class);
    int _timeout;
    
    String _instance;
    String _parent;
    
    String _dc;
    String _pod;
    String _guid;
    
    StorageLayer _storage;
    
    DownloadManager _dlMgr;
    
    @Override
    public void disconnected() {
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return _dlMgr.handleDownloadCommand((DownloadProgressCommand)cmd);
        } else if (cmd instanceof DownloadCommand) {
            return _dlMgr.handleDownloadCommand((DownloadCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public Type getType() {
        return Host.Type.SecondaryStorage;
    }
    
    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }
        
        _dc = (String)params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String)params.get("pod");
        
        _instance = (String)params.get("instance");

        _parent = (String)params.get("mount.path");
        if (_parent == null) {
            throw new ConfigurationException("No directory specified.");
        }
        
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            String value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.vmops.storage.JavaStorageLayer";
            }
            
            try {
                Class<StorageLayer> clazz = (Class<StorageLayer>)Class.forName(value);
                _storage = ComponentLocator.inject(clazz);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }

        if (!_storage.mkdirs(_parent)) {
            s_logger.warn("Unable to create the directory " + _parent);
            throw new ConfigurationException("Unable to create the directory " + _parent);
        }
     
        s_logger.info("Mount point established at " + _parent);

        params.put("template.parent", _parent);
        params.put(StorageLayer.InstanceConfigKey, _storage);
        
        _dlMgr = new DownloadManagerImpl();
        _dlMgr.configure("DownloadManager", params);
        
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public StartupCommand[] initialize() {
        
        final StartupStorageCommand cmd = new StartupStorageCommand(_parent, StoragePoolType.Filesystem, 1024l*1024l*1024l*1024l, _dlMgr.gatherTemplateInfo());
        cmd.setResourceType(Volume.StorageResourceType.SECONDARY_STORAGE);
        cmd.setIqn(null);
        fillNetworkInformation(cmd);
        cmd.setDataCenter(_dc);
        cmd.setPod(_pod);
        cmd.setGuid(_guid);
        cmd.setName(_guid);
        cmd.setVersion(LocalSecondaryStorageResource.class.getPackage().getImplementationVersion());
        
        /* gather TemplateInfo in second storage */
        final Map<String, TemplateInfo> tInfo = _dlMgr.gatherTemplateInfo();
        cmd.setTemplateInfo(tInfo);
        
        return new StartupCommand [] {cmd};
    }
    
    @Override
    protected String getDefaultScriptsDir() {
        return "scripts/storage/secondary";
    }
}
