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
package com.vmops.agent;

import java.util.List;
import java.util.Set;

import com.vmops.agent.api.Answer;
import com.vmops.agent.api.Command;
import com.vmops.dc.DataCenterVO;
import com.vmops.dc.HostPodVO;
import com.vmops.exception.AgentUnavailableException;
import com.vmops.exception.InternalErrorException;
import com.vmops.exception.OperationTimedoutException;
import com.vmops.host.Host;
import com.vmops.host.HostStats;
import com.vmops.host.HostVO;
import com.vmops.host.Status;
import com.vmops.host.Status.Event;
import com.vmops.service.ServiceOffering;
import com.vmops.service.ServiceOfferingVO;
import com.vmops.storage.DiskOfferingVO;
import com.vmops.storage.StoragePoolVO;
import com.vmops.storage.VMTemplateVO;
import com.vmops.storage.VirtualMachineTemplate;
import com.vmops.utils.component.Manager;
import com.vmops.vm.UserVm;
import com.vmops.vm.VMInstanceVO;

/**
 * AgentManager manages hosts.  It directly coordinates between the
 * DAOs and the connections it manages.
 */
public interface AgentManager extends Manager {
    
	/**
	 * easy send method that returns null if there's any errors.  It handles all exceptions.
	 * 
	 * @param hostId host id
	 * @param cmd command to send.
	 * @return Answer if successful; null if not.
	 */
    Answer easySend(Long hostId, Command cmd);
    
    /**
     * Synchronous sending a command to the agent.
     * 
     * @param hostId id of the agent on host
     * @param cmd command
     * @return an Answer
     */
    Answer send(Long hostId, Command cmd, int timeout) throws AgentUnavailableException, OperationTimedoutException;
    
    Answer send(Long hostId, Command cmd) throws AgentUnavailableException, OperationTimedoutException;
    
    /**
     * Synchronous sending a list of commands to the agent.
     * 
     * @param hostId id of the agent on host
     * @param cmds array of commands
     * @param isControl Commands sent contains control commands
     * @param stopOnError should the agent stop execution on the first error.
     * @return an array of Answer
     */
    Answer[] send(Long hostId, Command []  cmds, boolean stopOnError) throws AgentUnavailableException, OperationTimedoutException;
    
    Answer[] send(Long hostId, Command []  cmds, boolean stopOnError, int timeout) throws AgentUnavailableException, OperationTimedoutException;
    
    /**
     * Asynchronous sending of a command to the agent.
     * @param hostId id of the agent on the host.
     * @param cmd Command to send.
     * @param listener the listener to process the answer.
     * @return sequence number.
     */
    long gatherStats(Long hostId, Command cmd, Listener listener);
    
    /**
     * Asynchronous sending of a command to the agent.
     * @param hostId id of the agent on the host.
     * @param cmds Commands to send.
     * @param stopOnError should the agent stop execution on the first error.
     * @param listener the listener to process the answer.
     * @return sequence number.
     */
    long send(Long hostId, Command[] cmds, boolean stopOnError, Listener listener) throws AgentUnavailableException;
    
    /**
     * Register to listen for host events.  These are mostly connection and
     * disconnection events.
     * 
     * @param listener
     * @param connections listen for connections
     * @param commands listen for connections
     * @return id to unregister if needed.
     */
    int registerForHostEvents(Listener listener, boolean connections, boolean commands);
    
    /**
     * Unregister for listening to host events.
     * @param id returned from registerForHostEvents
     */
    void unregisterForHostEvents(int id);
    
    /**
     * @return hosts currently connected.
     */
    Set<Long> getConnectedHosts();
    
    /**
     * Disconnect the agent.
     * 
     * @param hostId host to disconnect.
     * @param reason the reason why we're disconnecting.
     * 
     */
    void disconnect(long hostId, Status.Event event, boolean investigate);
	
    /**
     * Obtains statistics for a host; vCPU utilisation, memory utilisation, and network utilisation
     * @param hostId
     * @return HostStats
     * @throws InternalErrorException
     */
	HostStats getHostStatistics(long hostId) throws InternalErrorException;
	
	Long getGuestOSCategoryId(long hostId);
	
	/**
	 * Find a host based on the type needed, data center to deploy in, pod
	 * to deploy in, service offering, template, and list of host to avoid.
	 */

	Host findHost(Host.Type type, DataCenterVO dc, HostPodVO pod, StoragePoolVO sp, ServiceOffering offering, DiskOfferingVO diskOffering, VMTemplateVO template, VMInstanceVO vm, Host currentHost, Set<Host> avoid);
	
	/**
	 * Updates a host
	 * @param hostId
	 * @param guestOSCategoryId
	 */
	void updateHost(long hostId, long guestOSCategoryId);
	
	/**
     * Deletes a host
     * 
     * @param hostId
     * @param true if deleted, false otherwise
     */
    boolean deleteHost(long hostId);
	
	/**
	 * Find a pod based on the user id, template, and data center.
	 * 
	 * @param template
	 * @param dc
	 * @param userId
	 * @return
	 */
    HostPodVO findPod(VirtualMachineTemplate template, ServiceOfferingVO offering, DataCenterVO dc, long userId, Set<Long> avoids);

    /**
     * Put the agent in maintenance mode.
     * 
     * @param hostId id of the host to put in maintenance mode.
     * @return true if it was able to put the agent into maintenance mode.  false if not.
     */
    boolean maintain(long hostId) throws AgentUnavailableException;
    
    boolean maintenanceFailed(long hostId);
    
    /**
     * Cancel the maintenance mode.
     * 
     * @param hostId host id
     * @return true if it's done.  false if not.
     */
    boolean cancelMaintenance(long hostId);

    /**
     * Check to see if a virtual machine can be upgraded to the given service offering
     *
     * @param vm
     * @param offering
     * @return true if the host can handle the upgrade, false otherwise
     */
    boolean isVirtualMachineUpgradable(final UserVm vm, final ServiceOffering offering);
    
    public boolean executeUserRequest(long hostId, Event event) throws AgentUnavailableException;
    public boolean reconnect(final long hostId) throws AgentUnavailableException;
    
    public List<HostVO> discoverHosts(long dcId, Long podId, String url, String username, String password);
}
