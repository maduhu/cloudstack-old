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

package com.vmops.utils;

public class Profiler {
	private long startTickInMs;
	private long stopTickInMs;
	
	public Profiler() {
		startTickInMs = 0;
		stopTickInMs = 0;
	}
	
	public long start() {
		startTickInMs = System.currentTimeMillis();
		return startTickInMs;
	}
	
	public long stop() {
		stopTickInMs = System.currentTimeMillis();
		return stopTickInMs;
	}
	
	public long getDuration() {
		return stopTickInMs - startTickInMs;
	}
}
