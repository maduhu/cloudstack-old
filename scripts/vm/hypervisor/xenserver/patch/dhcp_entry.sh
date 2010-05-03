#!/bin/bash
# dhcp_entry.sh -- add dhcp entry on domr
#

usage() {
  printf "Usage: %s: -r <domr-ip> -m <vm mac> -v <vm ip> -n <vm name>\n" $(basename $0) >&2
  exit 2
}

cert="$(dirname $0)/id_rsa"

add_dhcp_entry() {
  local domr=$1
  local mac=$2
  local ip=$3
  local vm=$4
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domr "/root/edithosts.sh $mac $ip $vm" >/dev/null
  return $?
}

domrIp=
vmMac=
vmIp=
vmName=

while getopts 'r:m:v:n:' OPTION
do
  case $OPTION in
  r)	domrIp="$OPTARG"
		;;
  v)	vmIp="$OPTARG"
		;;
  m)	vmMac="$OPTARG"
		;;
  n)	vmName="$OPTARG"
		;;
  ?)    usage
		exit 1
		;;
  esac
done

add_dhcp_entry $domrIp $vmMac $vmIp $vmName

exit $?
