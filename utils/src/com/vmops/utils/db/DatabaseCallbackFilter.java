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
package com.vmops.utils.db;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.CallbackFilter;

public class DatabaseCallbackFilter implements CallbackFilter {
    @Override
    public int accept(Method method) {
        return checkAnnotation(method) ? 1 : 0;
    }
    
    public static boolean checkAnnotation(Method method) {
        DB db = method.getAnnotation(DB.class);
        if (db != null) {
            return db.inject();
        }
        Class<?> clazz = method.getDeclaringClass();
        do {
            db = clazz.getAnnotation(DB.class);
            if (db != null) {
                return db.inject();
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        return false;
    }
}
