#!/bin/bash
# Version @VERSION@

#set -x
 
usage() {
  printf "Usage: %s [uuid of this host] [interval in seconds]\n" $(basename $0) >&2

}

if [ -z $1 ]; then
  usage
  exit 2
fi

if [ -z $2 ]; then
  usage
  exit 3
fi


hbs=
while true 
do 
  date=`date +%s`
  lvscan
  hbs=`ls -l /dev/VG*/hb-$1 | awk '{print $9}'`
  for hb in $hbs
  do
    echo $date | dd of=$hb count=100 bs=1
    if [ $? -ne 0 ]; then
      reboot -f
      echo "Problem with $hb";
    fi
  done
  sleep $2
done

