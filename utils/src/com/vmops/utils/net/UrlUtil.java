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
package com.vmops.utils.net;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UrlUtil {
    public static Map<String, String> parseQueryParameters(String query) {
        HashMap<String, String> values = new HashMap<String, String>();
        if (query == null) {
            return values;
        }
        
        if (query.startsWith("?")) {
            query = query.substring(1);
        }
        
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] tokens = part.split("=");
            values.put(tokens[0], tokens[1]);
        }
        
        return values;
    }
    
    public static Map<String, String> parseQueryParameters(URL url) {
        return parseQueryParameters(url.getQuery());
    }
}
