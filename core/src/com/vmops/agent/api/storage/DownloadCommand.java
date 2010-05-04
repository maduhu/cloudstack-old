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
package com.vmops.agent.api.storage;

import com.vmops.storage.VMTemplateVO;
import com.vmops.storage.Storage.ImageFormat;


/**
 * @author chiradeep
 *
 */
public class DownloadCommand extends AbstractDownloadCommand {

	private boolean hvm;
	private String description;
	private String checksum;
	private long id;
	
	protected DownloadCommand() {
	}
	
	
	public DownloadCommand(DownloadCommand that) {
	    super(that);
	    this.hvm = that.hvm;
	    this.checksum = that.checksum;
	    this.id = that.id;
	    this.description = that.description;
	}
	
	public DownloadCommand(VMTemplateVO template) {
	    super(template.getUniqueName(), template.getUrl(), template.getFormat(), template.getAccountId());
	    this.hvm = template.requiresHvm();
	    this.checksum = template.getChecksum();
	    this.id = template.getId();
	    this.description = template.getDisplayText();
	}

	public DownloadCommand(String url, String name, ImageFormat format, boolean isHvm, Long accountId, String descr, String cksum) {
	    super(name, url, format, accountId);
		this.setHvm(isHvm);
		this.description = descr;
		this.checksum = cksum;
	}
	
	public long getId() {
	    return id;
	}
	
	public void setHvm(boolean hvm) {
		this.hvm = hvm;
	}

	public boolean isHvm() {
		return hvm;
	}

	public String getDescription() {
		return description;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
