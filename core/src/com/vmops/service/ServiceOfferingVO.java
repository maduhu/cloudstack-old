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

package com.vmops.service;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.gson.annotations.Expose;
import com.vmops.storage.Volume.StorageResourceType;
import com.vmops.utils.db.GenericDao;


@Entity
@Table(name="service_offering")
public class ServiceOfferingVO implements ServiceOffering {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
	private Long id = null;
    
    @Column(name="name")
	private String name = null;
    
    @Column(name="cpu")
	private int cpu;
    
    @Column(name="speed")
    private int speed;
    
    @Column(name="ram_size")
	private int ramSize;
    
    @Column(name="nw_rate")
    private int rateMbps;
    
    @Column(name="mc_rate")
    private int multicastRateMbps;
    
    @Column(name="mirrored")
    private boolean mirroredVolumes;
    
    @Column(name="ha_enabled")
    private boolean offerHA;
    
    @Column(name="display_text")
	private String displayText = null;
    
    @Column(name="guest_ip_type")
    @Enumerated(EnumType.STRING)
    private GuestIpType guestIpType = GuestIpType.Virtualized;
    
    @Column(name="use_local_storage")
    private boolean useLocalStorage;
    
	@Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    protected ServiceOfferingVO() {
    }

    public ServiceOfferingVO(Long id, String name, int cpu, int ramSize, int speed, int rateMbps, int multicastRateMbps, boolean offerHA, String displayText, boolean localStorageRequired) {
        this(id, name, cpu, ramSize, speed, rateMbps, multicastRateMbps, offerHA, displayText, GuestIpType.Virtualized, localStorageRequired);
    }
    
    public ServiceOfferingVO(Long id, String name, int cpu, int ramSize, int speed, int rateMbps, int multicastRateMbps, boolean offerHA, String displayText, GuestIpType guestIpType, boolean useLocalStorage) {
        this.id = id;
        this.name = name;
        this.cpu = cpu;
        this.ramSize = ramSize;
        this.speed = speed;
        this.rateMbps = rateMbps;
        this.multicastRateMbps = multicastRateMbps;
        this.offerHA = offerHA;
        this.displayText = displayText;
        this.guestIpType = guestIpType;
        this.useLocalStorage = useLocalStorage;
    }

	@Override
	public boolean getOfferHA() {
	    return offerHA;
	}
	
	
	public Long getId() {
		return id;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int getCpu() {
	    return cpu;
	}
	
	public void setCpu(int cpu) {
		this.cpu = cpu;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setRamSize(int ramSize) {
		this.ramSize = ramSize;
	}

	@Override
	public int getSpeed() {
	    return speed;
	}
	
	@Override
	public int getRamSize() {
	    return ramSize;
	}
	
	public Date getCreated() {
		return created;
	}
	
	public Date getRemoved() {
		return removed;
	}

	public void setMirroredVolumes(boolean mirroredVolumes) {
		this.mirroredVolumes = mirroredVolumes;
	}

	public boolean isMirroredVolumes() {
		return mirroredVolumes;
	}
	
	public String getDisplayText() {
		return displayText;
	}

	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}

	public void setRateMbps(int rateMbps) {
		this.rateMbps = rateMbps;
	}

	public int getRateMbps() {
		return rateMbps;
	}

	public void setMulticastRateMbps(int multicastRateMbps) {
		this.multicastRateMbps = multicastRateMbps;
	}

	public int getMulticastRateMbps() {
		return multicastRateMbps;
	}

	public void setGuestIpType(GuestIpType guestIpType) {
		this.guestIpType = guestIpType;
	}

	public GuestIpType getGuestIpType() {
		return guestIpType;
	}
	
	public boolean getUseLocalStorage() {
		return useLocalStorage;
	}

}
