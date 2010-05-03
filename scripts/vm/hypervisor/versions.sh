#!/bin/sh
# Output Linux distribution.

REV=`uname -r`
MACH=`uname -m`
KERNEL=`uname -r`
DIST="Unknown Linux"
REV="X.Y"
CODENAME=""

if [ -f /etc/redhat-release ] ; then
	DIST=`cat /etc/redhat-release | awk '{print $1}'`
	CODENAME=`cat /etc/redhat-release | sed s/.*\(// | sed s/\)//`
	REV=`cat /etc/redhat-release | awk '{print $3}'`
elif [ -f /etc/lsb-release ] ; then
	DIST=`cat /etc/lsb-release | grep DISTRIB_ID | tr "\n" ' '| sed s/.*=//`
	REV=`cat /etc/lsb-release | grep DISTRIB_RELEASE | tr "\n" ' '| sed s/.*=//`
	CODENAME=`cat /etc/lsb-release | grep DISTRIB_CODENAME | tr "\n" ' '| sed s/.*=//`
fi
	
echo Host.OS=${DIST} 
echo Host.OS.Version=${REV} 
echo Host.OS.Kernel.Version=${KERNEL}
