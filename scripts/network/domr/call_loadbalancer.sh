#!/usr/bin/env bash
# $Id: call_loadbalancer.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.0.0/java/scripts/vm/hypervisor/xenserver/patch/call_loadbalancer.sh $
# loadbalancer.sh -- reconfigure loadbalancer rules
#
#

usage() {
  printf "Usage: %s:  -i <domR eth1 ip>  -a <added public ip address> -d <removed> -f <load balancer config> \n" $(basename $0) >&2
}

set -x

# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}

# copy the new haproxy.cfg to DomR
copy_haproxy() {
  local domRIp=$1
  local cfg=$2

  scp -P 3922 -q -o StrictHostKeyChecking=no -i $CERT $cfg root@$domRIp:/etc/haproxy/haproxy.cfg.new
  return $?
}

iflag=
aflag=
dflag=
fflag=

while getopts 'i:a:d:f:' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  a)	aflag=1
		addedIps="$OPTARG"
		;;
  d)	dflag=1
		removedIps="$OPTARG"
		;;
  f)	fflag=1
		cfgfile="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

CERT="$(dirname $0)/id_rsa"

if [ "$iflag$fflag" != "11" ]
then
  usage
  exit 2
fi

# Check if DomR is up and running. If it isn't, exit 1.
check_gw "$domRIp"
if [ $? -gt 0 ]
then
  exit 1
fi

copy_haproxy $domRIp $cfgfile

if [ $? -gt 0 ]
then
  printf "Reconfiguring loadbalancer failed\n"
  exit 1
fi
	
ssh -p 3922 -q -o StrictHostKeyChecking=no -i $CERT root@$domRIp "/root/loadbalancer.sh $*"
exit $?	
