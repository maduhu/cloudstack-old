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

package com.vmops.cluster;

import com.vmops.agent.Listener;
import com.vmops.agent.api.Answer;
import com.vmops.agent.api.Command;
import com.vmops.exception.AgentUnavailableException;
import com.vmops.exception.OperationTimedoutException;
import com.vmops.host.Status.Event;
import com.vmops.utils.component.Manager;

public interface ClusterManager extends Manager {
	public static final int DEFAULT_HEARTBEAT_INTERVAL = 1500;
	public static final int DEFAULT_HEARTBEAT_THRESHOLD = 30000;
	public static final String ALERT_SUBJECT = "cluster-alert";
	
    public Answer[] execute(String strPeer, long agentId, Command [] cmds, boolean stopOnError);
    public long executeAsync(String strPeer, long agentId, Command[] cmds, boolean stopOnError, Listener listener);
    public boolean onAsyncResult(String executingPeer, long agentId, long seq, Answer[] answers);
    public boolean forwardAnswer(String targetPeer, long agentId, long seq, Answer[] answers);
    
    public Answer[] sendToAgent(Long hostId, Command []  cmds, boolean stopOnError) throws AgentUnavailableException, OperationTimedoutException;
    public long sendToAgent(Long hostId, Command[] cmds, boolean stopOnError, Listener listener) throws AgentUnavailableException;
    public boolean executeAgentUserRequest(long agentId, Event event) throws AgentUnavailableException;
    public Boolean propagateAgentEvent(long agentId, Event event) throws AgentUnavailableException;
	
	public int getHeartbeatThreshold();
	public long getId();
	public String getSelfPeerName();
	public String getSelfNodeIP();
    public String getPeerName(long agentHostId);
	
	public void regiterListener(ClusterManagerListener listener);
	public void unregisterListener(ClusterManagerListener listener);
    public ManagementServerHostVO getPeer(String peerName);
    
    /**
     * Broadcast the command to all of the  management server nodes.
     * @param agentId agent id this broadcast is regarding
     * @param cmds commands to broadcast
     */
    public void broadcast(long agentId, Command[] cmds);
}
