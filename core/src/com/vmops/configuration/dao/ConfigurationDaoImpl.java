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

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.vmops.configuration.ConfigurationVO;
import com.vmops.utils.db.GenericDaoBase;
import com.vmops.utils.db.SearchBuilder;
import com.vmops.utils.db.SearchCriteria;
import com.vmops.utils.db.Transaction;

@Local(value={ConfigurationDao.class})
public class ConfigurationDaoImpl extends GenericDaoBase<ConfigurationVO, String> implements ConfigurationDao {
    private Map<String, String> _configs = null;

    SearchBuilder<ConfigurationVO> InstanceSearch;
    SearchBuilder<ConfigurationVO> NameSearch;
    
    public static final String UPDATE_CONFIGURATION_SQL = "UPDATE configuration SET value = ? WHERE name = ?";
    public static final String GET_CONFIGURATION_VALUE_SQL = "SELECT * FROM configuration WHERE name = ?";

    public ConfigurationDaoImpl () {}

    @Override
    public Map<String, String> mapByComponent(String component) {
        return mapByComponent("DEFAULT", component);
    }

    @Override
    public Map<String, String> mapByComponent(String instance, String component) {
        return getConfiguration(instance, new HashMap<String, Object>());
    }
    
    @Override
    public Map<String, String> getConfiguration(String instance, Map<String, ? extends Object> params) {
        if (_configs == null) {
            _configs = new HashMap<String, String>();

            SearchCriteria sc = InstanceSearch.create();
            sc.setParameters("instance", "DEFAULT");

            List<ConfigurationVO> configurations = listBy(sc);

            for (ConfigurationVO config : configurations) {
            	if (config.getValue() != null)
            		_configs.put(config.getName(), config.getValue());
            }

            sc = InstanceSearch.create();
            sc.setParameters("instance", instance);

            configurations = listBy(sc);

            for (ConfigurationVO config : configurations) {
            	if (config.getValue() != null)
            		_configs.put(config.getName(), config.getValue());
            }

        }

        mergeConfigs(_configs, params);
        return _configs;
    }

    protected void mergeConfigs(Map<String, String> dbParams, Map<String, ? extends Object> xmlParams) {
        for (Map.Entry<String, ? extends Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }
    
    @Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	super.configure(name, params);
    	
        InstanceSearch = createSearchBuilder();
        InstanceSearch.and("instance", InstanceSearch.entity().getInstance(), SearchCriteria.Op.EQ);
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        return true;
    }
    
    @Override
    public boolean update(String name, String value) {
    	Transaction txn = Transaction.currentTxn();
		try {
			PreparedStatement stmt = txn.prepareStatement(UPDATE_CONFIGURATION_SQL);
			stmt.setString(1, value);
			stmt.setString(2, name);
			stmt.executeUpdate();
			return true;
		} catch (Exception e) {
			s_logger.warn("Unable to update Configuration Value", e);
		}
		return false;
    }
    
    @Override
    public String getValue(String name) {
    	SearchCriteria sc = NameSearch.create();
        sc.setParameters("name", name);
        List<ConfigurationVO> configurations = listBy(sc);
        
        if (configurations.size() == 0) return null;
        
        ConfigurationVO config = configurations.get(0);
        String value = config.getValue();
        
        if (value == null)
        	return "";
        else
        	return value;
    }
}
