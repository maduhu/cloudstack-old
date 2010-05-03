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
package com.vmops.template;

import java.net.URI;
import java.util.List;

import com.vmops.exception.InternalErrorException;
import com.vmops.storage.StoragePoolVO;
import com.vmops.storage.VMTemplateStoragePoolVO;
import com.vmops.storage.VMTemplateVO;
import com.vmops.storage.Storage.FileSystem;
import com.vmops.storage.Storage.ImageFormat;
import com.vmops.utils.component.Manager;

/**
 * TemplateManager manages the templates stored
 * on secondary storage.  It is responsible for
 * creating private/public templates.  It is
 * also responsible for downloading.
 */
public interface TemplateManager extends Manager {
    /**
     * Creates a Template
     * 
     * @param displayText user readable name.
     * @param isPublic is this a public template?
     * @param format which image format is the template.
     * @param fs what is the file system on the template
     * @param url url to download the template from.
     * @param chksum chksum to compare it to.
     * @param requiresHvm does this template require hvm?
     * @param bits is the os contained on the template 32 bit?
     * @param enablePassword Does the template support password change.
     * @param guestOSId OS that is on the template
     * @param bootable true if this template will represent a bootable ISO
     * @return id of the template created.
     */
    Long create(long userId, String displayText, boolean isPublic, ImageFormat format, FileSystem fs, URI url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable);
    
    /**
     * Prepares a template for vm creation for a certain storage pool.
     * 
     * @param template template to prepare
     * @param pool pool to make sure the template is ready in.
     * @return VMTemplateStoragePoolVO if preparation is complete; null if not.
     */
    VMTemplateStoragePoolVO prepareTemplateForCreate(VMTemplateVO template, StoragePoolVO pool);
    
    /**
     * Copies a template from its current secondary storage server to the secondary storage server in the specified zone.
     * @param templateId
     * @param zoneId
     * @return true if success
     * @throws InternalErrorException
     */
    boolean copy(long templateId, long zoneId) throws InternalErrorException;
    
    /**
     * Deletes a template from secondary storage servers
     * @param templateId
     * @param zoneId - optional. If specified, will only delete the template from the specified zone's secondary storage server.
     * @return true if success
     */
    boolean delete(long templateId, Long zoneId) throws InternalErrorException;
    
    /**
     * Lists templates in the specified storage pool that are not being used by any VM.
     * @param pool
     * @return list of VMTemplateStoragePoolVO
     */
    List<VMTemplateStoragePoolVO> getUnusedTemplatesInPool(StoragePoolVO pool);
    
    /**
     * Deletes a template in the specified storage pool.
     * @param templatePoolVO
     */
    void evictTemplateFromStoragePool(VMTemplateStoragePoolVO templatePoolVO);
    
}
