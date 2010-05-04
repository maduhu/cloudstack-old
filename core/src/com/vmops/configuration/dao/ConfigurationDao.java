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

package com.vmops.configuration.dao;

import java.util.Map;

import com.vmops.configuration.ConfigurationVO;
import com.vmops.utils.db.GenericDao;

public interface ConfigurationDao extends GenericDao<ConfigurationVO, String> {
	public Map<String, String> mapByComponent(String component);
	public Map<String, String> mapByComponent(String instance, String component);
	
	/**
	 * Retrieves the configuration for the a certain instance.  It merges
	 * the configuration for the DEFAULT instance with the instance specified
	 * and the parameters passed in.
	 * 
	 * The priority order in case of name collision is
	 *    1. params passed in.
	 *    2. configuration for the instance.
	 *    3. configuration for the DEFAULT instance.
	 * 
	 * @param component component to retrieve it for.
	 * @param params parameters from the components.xml which will override the database values.
	 * @return a consolidated look at the configuration parameters.
	 */
    public Map<String, String> getConfiguration(String instance, Map<String, ? extends Object> params);
    
    /**
     * Updates a configuration value
     * @param name the name of the configuration value to update
     * @param value the new value
     * @return true if success, false if failure
     */
    public boolean update(String name, String value);
    
    /**
     * Gets the value for the specified configuration name
     * @return value
     */
    public String getValue(String name);
    
    
}
