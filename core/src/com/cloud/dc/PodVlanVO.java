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
package com.cloud.dc;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="op_pod_vlan_alloc")
public class PodVlanVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;
    
    @Column(name="taken", nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    Date takenAt;

    @Column(name="vlan", updatable=false, nullable=false)
    protected String vlan;
    
    @Column(name="data_center_id") 
    long dataCenterId;
    
    @Column(name="pod_id", updatable=false, nullable=false)
    protected long podId;

    @Column(name="account_id")
    protected Long accountId;
    
    public Date getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Date taken) {
        this.takenAt = taken;
    }

    public PodVlanVO(String vlan, long dataCenterId, long podId) {
        this.vlan = vlan;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.takenAt = null;
    }
    
    public Long getId() {
        return id;
    }
    
    public Long getAccountId() {
    	return accountId;
    }
    
    public String getVlan() {
        return vlan;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getPodId() {
        return podId;
    }

    public void setAccountId(Long accountId) {
    	this.accountId = accountId;
    }
    
    protected PodVlanVO() {
    }
}
