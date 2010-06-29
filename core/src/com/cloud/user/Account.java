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

package com.cloud.user;

public interface Account {
    public static final short ACCOUNT_TYPE_NORMAL = 0;
    public static final short ACCOUNT_TYPE_ADMIN = 1;
    public static final short ACCOUNT_TYPE_DOMAIN_ADMIN = 2;
    public static final short ACCOUNT_TYPE_READ_ONLY_ADMIN = 3;

    public static final String ACCOUNT_STATE_DISABLED = "disabled";
    public static final String ACCOUNT_STATE_ENABLED = "enabled";
    public static final String ACCOUNT_STATE_LOCKED = "locked";

    public static final long ACCOUNT_ID_SYSTEM = 1; 

    public Long getId();
    public String getAccountName();
    public void setAccountName(String accountId);
    public short getType();
    public void setType(short type);
    public String getState();
    public void setState(String state);
    public Long getDomainId();
    public void setDomainId(Long domainId);
}
