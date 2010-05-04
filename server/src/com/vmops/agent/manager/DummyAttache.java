/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
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
package com.vmops.agent.manager;

import com.vmops.agent.transport.Request;
import com.vmops.exception.AgentUnavailableException;
import com.vmops.host.Status;


public class DummyAttache extends AgentAttache {

	public DummyAttache(long id, boolean maintenance) {
		super(id, maintenance);
	}


	@Override
	public void disconnect(Status state) {

	}

	
	@Override
	protected boolean isClosed() {
		return false;
	}

	
	@Override
	public void send(Request req) throws AgentUnavailableException {

	}

}
