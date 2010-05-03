#!/usr/bin/env bash
# upgradevmdisk.sh -- upgrade size of disks of a VM (iscsi mode)

usage() {
  printf "Usage: %s: -v <instance-fs> -d <size> \n" $(basename $0) >&2
}

upgrade_disk() {
  local ifs=$1
  local disk_size=$2 

  zfs set volsize=${disk_size}M $ifs

  if [ $? -gt 0 ] 
  then
    return  6
  fi

  sbdadm modify-lu -s ${disk_size}M /dev/zvol/dsk/${ifs}

  if [ $? -gt 0 ]
  then
    return 7
  fi

  return 0
}

#set -x

vflag=
dflag=
disksize=
instancefs=

while getopts 'v:d:' OPTION
do
  case $OPTION in
  v)	vflag=1
		instancefs="$OPTARG"
		;;
  d)	dflag=1
		disksize="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$vflag" != "1"  -a "$dflag" != "1" ]
then
 usage
 exit 2
fi

if [ ${instancefs:0:1} == / ]
then
  instancefs=${instancefs:1}
fi


if [[ $(zfs get -H -o value -p type $instancefs) != volume ]]
then
  printf "Supplied instance fs doesn't exist\n" >&2
  exit 1
fi

if [ "$dflag" == 1 ] 
then
  if [[ $disksize -gt 0 ]]
  then 
    upgrade_disk $instancefs $disksize
  fi

  exit $? 
fi
exit 0
