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

//
// Javascript logger utility
// Author
//		Kelven Yang
//		2/25/2010
//

function Logger() {
	this.bDockEnabled = true;
	
	this.logWin = null;
	this.logger = null;
	this.header = null;
	
	this.bEnabled = true;
	this.level = 0;
	
	this.bMoving = false;
	this.offsetStart = {left: 0, top: 0};
	this.ptStart = {x: 0, y: 0};
}

Logger.DEFAULT_WIN_HEIGHT = 500;
Logger.LEVEL_TRACE = 0;
Logger.LEVEL_DEBUG = 1;
Logger.LEVEL_INFO = 2;
Logger.LEVEL_WARN = 3;
Logger.LEVEL_ERROR = 4;
Logger.LEVEL_FATAL = 5;
Logger.LEVEL_SYS = 100;

Logger.prototype = {
	
	open: function() {
		var logger = this;
		var logWinMarkup = [ 
				'<div class="logwin">',
				'<div class="logwin_title">',
					'<div class="logwin_title_actionbox">',
		        		'<a class="logwin_playbutton" href="#" cmd="1"></a>', 
		        		'<a class="logwin_stopbutton" href="#" cmd="2"></a>',
		        		'<a class="logwin_clrbutton" href="#" cmd="3"></a>',
		        		'<form action="#">',
						'<select class="select" id="template_type">',
		                  '<option value="0">TRACE</option>',
		                  '<option value="1">DEBUG</option>',
		                  '<option value="2">INFO</option>',
		                  '<option value="3">WARN</option>',
		                  '<option value="4">ERROR</option>',
		                  '<option value="5">FATAL</option>',
		         		'</select>',
		         		'</form>',
		            '</div>',
		            '<div class="logwin_title_rgtactionbox">',
			        	'<a class="logwin_minimizebutton" href="#" cmd="4"></a>',
			            '<a class="logwin_shrinkbutton" href="#" cmd="5"></a>',
		            '</div>',
				'</div>',
				'<div class="logwin_content"></div>',
				'</div>'
		    ].join('');
		
		this.logWin = $(logWinMarkup).appendTo(document.body);
		this.header = $('.logwin_title:first', this.logWin);
		this.logger = $('.logwin_content:first', this.logWin);

		$(".logwin_title", this.logWin).mousedown(function(e) {
			if($(e.target).attr('cmd'))
				return true;
			
			if(!logger.bMoving) {
				logger.bMoving = true;
				logger.offsetStart = logger.logWin.offset();
				logger.ptStart = {x: e.pageX, y: e.pageY};
				
				$(document).bind("mousemove", function(e) {
					if(logger.bMoving) {
						logger.enableDocking(false);
						
						var logWinNewLeft = logger.offsetStart.left + e.pageX - logger.ptStart.x;
						var logWinNewTop = logger.offsetStart.top + e.pageY - logger.ptStart.y;
						
						logger.logWin.css("left", logWinNewLeft + "px").css("top", logWinNewTop + "px");
					}
					return false; 
				});

				$(document).bind("mouseup", function(e) {
					if(logger.bMoving) {
						logger.bMoving = false;
						$(document).unbind("mousemove", arguments.callee.name);
						$(document).unbind("mouseup", arguments.callee.name);
						
						return false;
					}
					return true;
				});
			}
			
			// prevent default handling
			return false;
		}).dblclick(function(e) {
			logger.expand(!logger.isExpanded());
		});
		
		this.logWin.click(function(e) {
			if($(e.target).attr('cmd')) {
				switch($(e.target).attr('cmd')) {
				case '1' :
					logger.enable(true);
					break;
					
				case '2' :
					logger.enable(false);
					break;
					
				case '3' :
					logger.clear();
					break;
					
				case '4' :
					logger.enableDocking(true);
					logger.dockIn();
					break;
					
				case '5' :
					logger.expand(!logger.isExpanded());
					break;
					
				default :
					break;
				}
			}
		});
		
		$("#template_type", this.logWin).change(function(e) {
			logger.setLevel(parseInt($(this).val()));
		});
		
		this.logWin.css("left", (($(document.body).width() - this.logWin.width()) / 2) + "px");
		this.dockIn();
		
		this.log(Logger.LEVEL_SYS, "Logger started");
	},
	
	dockIn: function() {
		var logger = this;
		var offset = this.logWin.offset();
		var bottom = offset.top + this.logWin.height();
		var delta = bottom - 2;
		
		this.logWin.animate({top: (offset.top - delta) + "px"}, 200, 
			function() {
				logger.logWin.unbind("mouseleave");
				logger.logWin.bind("mouseenter", function(e) {
					if(logger.bDockEnabled)
						logger.dockOut();
				});
			} 
		);
	},
	
	dockOut: function() {
		var logger = this;
		this.logWin.animate({top: "0px"}, 200, 
			function() {
				logger.logWin.unbind("mouseenter");
				logger.logWin.bind("mouseleave", function(e) {
					if(logger.bDockEnabled) {
						var xPosInLogWin = e.pageX - logger.logWin.offset().left;
						var yPosInLogWin = e.pageY - logger.logWin.offset().top;
						
						if(xPosInLogWin < 0 || yPosInLogWin < 0 || 
							xPosInLogWin > logger.logWin.width() || yPosInLogWin > logger.logWin.height()) {
							logger.dockIn();
						}
					}
				});
			}
		);
	},
	
	enableDocking: function(bEnable) {
		this.bDockEnabled = bEnable;
	},
	
	log: function(level, message) {
		// Note : LEVEL_SYS message will always be logged
		if(this.logger && (level == Logger.LEVEL_SYS || this.bEnabled && level >= this.level)) {
			var curTime = new Date();
			var curTimeString = [
			    '', curTime.getMonth(),
			    '/', curTime.getDate(),
			    '/', curTime.getYear(),
			    ' ',
				curTime.getHours(),
				':', curTime.getMinutes(),
				":", curTime.getSeconds(),
				".", curTime.getMilliseconds()].join('');
			
			this.logger.append(this.getLevelDisplayString(level) + " - " + curTimeString + " - " + message + '<br>');
		}
	},
	
	clear: function() {
		if(this.logger) {
			this.logger.empty();
			this.log(Logger.LEVEL_SYS, "Logger is cleared");
		}
	},
	
	setLevel: function(level) {
		this.level = level;
		
		this.log(Logger.LEVEL_SYS, "Set logger trace level to " + this.getLevelDisplayString(level));
	},
	
	enable: function(bEnabled) {
		this.bEnabled = bEnabled;
		
		if(bEnabled)
			this.log(Logger.LEVEL_SYS, "Logger is enabled");
		else
			this.log(Logger.LEVEL_SYS, "Logger is disabled");
	},
	
	expand: function(bExpand) {
		if(bExpand) {
			this.logWin.height(Logger.DEFAULT_WIN_HEIGHT);
			this.logger.height(Logger.DEFAULT_WIN_HEIGHT - this.header.height());
		} else {
			this.logWin.height(this.header.height());
			this.logger.height(0);
		}
	},
	
	isExpanded: function() {
		return this.logWin.height() > this.header.height();
	},
	
	getLevelDisplayString: function(level) {
		switch(level) {
		case Logger.LEVEL_TRACE :
			return "TRACE";
			
		case Logger.LEVEL_DEBUG :
			return "DEBUG";
			
		case Logger.LEVEL_INFO :
			return "INFO";
			
		case Logger.LEVEL_WARN :
			return "WARN";
			
		case Logger.LEVEL_ERROR :
			return "ERROR";
			
		case Logger.LEVEL_FATAL :
			return "FATAL";
			
		case Logger.LEVEL_SYS :
			return "SYSINFO";
		}
		
		return "LEVEL " + level;
	}
};

