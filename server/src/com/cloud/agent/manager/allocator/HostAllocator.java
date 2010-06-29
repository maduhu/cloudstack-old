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
package com.cloud.agent.manager.allocator;

import java.util.Set;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.service.ServiceOffering;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.UserVm;

public interface HostAllocator extends Adapter {
	Host allocateTo(ServiceOffering offering, DiskOfferingVO diskOffering, Host.Type type, DataCenterVO dc, HostPodVO pod, StoragePoolVO sp, VMTemplateVO template, Set<Host> avoid);
	boolean isVirtualMachineUpgradable(final UserVm vm, final ServiceOffering offering);
}
