#!/usr/bin/env bash
# install.sh -- installs an agent
#
#

usage() {
  printf "Usage: %s: -d [directory to deploy to] -z [zip file] -h [host] -p [pod] -c [data center] -m [expert|novice|setup]\n" $(basename $0) >&2
}

mode=
host=
pod=
zone=

deploydir=
confdir=
zipfile=
typ=

#set -x

while getopts 'd:z:x:m:h:p:c:' OPTION
do
  case "$OPTION" in
  d)	deploydir="$OPTARG"
		;;
  z)    zipfile="$OPTARG"
                ;;
  m)    mode="$OPTARG"
                ;;
  h)    host="$OPTARG"
                ;;
  p)    pod="$OPTARG"
                ;;
  c)    zone="$OPTARG"
                ;;
  ?)	usage
		exit 2
		;;
  esac
done

printf "NOTE: You must have root privileges to install and run this program.\n"

if [ "$mode" == "setup" ]; then
  mode="expert"
  deploydir="/usr/local/vmops/agent-simulator"
  confdir="/etc/vmops"
  /bin/cp -f $deploydir/conf/agent.properties $confdir/agent.properties
  if [ $? -gt 0 ]; then
    printf "ERROR: Failed to copy the agent.properties file into the right place."
    exit 10;
  fi
else
  confdir="$deploydir/conf"
fi

if [ "$host" == "" ]; then
  if [ "$mode" != "expert" ]
  then
    printf "Host name or ip address of management server [Required]: "
    read host
    if [ "$host" == "" ]; then
      printf "ERROR: Host is required\n"
      exit 23;
    fi
  fi
fi

port=
if [ "$mode" != "expert" ]
then
    printf "Port number of management server [defaults to 8250]: "
    read port
fi
if [ "$port" == "" ]
then
    port=8250
fi

if [ "$zone" == "" ]; then
  if [ "$mode" != "expert" ]; then
    printf "Availability Zone [Required]: "
    read zone
    if [ "$zone" == "" ]; then
	printf "ERROR: Zone is required\n";
	exit 21;
    fi
  fi
fi

if [ "$pod" == "" ]; then
  if [ "$mode" != "expert" ]; then
    printf "Pod [Required]: "
    read pod
    if ["$pod" == ""]; then
      printf "ERROR: Pod is required\n";
      exit 22;
    fi
  fi
fi

workers=
if [ "$mode" != "expert" ]; then
    printf "# of workers to start [defaults to 3]: "
    read workers
fi
if [ "$workers" == "" ]; then
    workers=3
fi

if [ "$deploydir" == "" ]; then 
    if [ "$mode" != "expert" ]; then
        printf "Directory to deploy to [defaults to /usr/local/vmops/agent-simulator]: "
        read deploydir
    fi
    if [ "$deploydir" == "" ]; then 
        deploydir="/usr/local/vmops/agent-simulator"
    fi
fi
if ! mkdir -p $deploydir
then
  printf "ERROR: Unable to create $deploydir\n"
  exit 5
fi

if [ "$zipfile" == "" ]; then 
    if [ "$mode" != "expert" ]; then
        printf "Path of the zip file [defaults to agent-simulator.zip]: "
        read zipfile
    fi
    if [ "$zipfile" == "" ]; then
        zipfile="agent-simulator.zip"
    fi

fi
if ! unzip -o $zipfile -d $deploydir
then
  printf "ERROR: Unable to unzip $zipfile to $deploydir\n"
  exit 6
fi

if ! chmod +x $deploydir/*.sh
then
  printf "ERROR: Unable to change scripts to executable.\n"
  exit 9
fi

if [ "$host" != "" ]; then
  sed s/@HOST@/"$host"/ $confdir/agent.properties > $confdir/tmp
  /bin/mv -f $confdir/tmp $confdir/agent.properties
else
  printf "INFO: host is not set\n"
fi

if [ "$port" != "" ]; then
  sed s/@PORT@/"$port"/ $confdir/agent.properties > $confdir/tmp
  /bin/mv -f $confdir/tmp $confdir/agent.properties
else
  printf "INFO: Port is not set\n"
fi

if [ "$pod" != "" ]; then
  sed s/@POD@/"$pod"/ $confdir/agent.properties > $confdir/tmp
  /bin/mv -f $confdir/tmp $confdir/agent.properties
else
  printf "INFO: Pod is not set\n"
fi

if [ "$zone" != "" ]; then
  sed s/@ZONE@/"$zone"/ $confdir/agent.properties > $confdir/tmp
  /bin/mv -f $confdir/tmp $confdir/agent.properties
else
  printf "INFO: Zone is not set\n"
fi

if [ "$workers" != "" ]; then
  sed s/@WORKERS@/"$workers"/ $confdir/agent.properties > $confdir/tmp
  /bin/mv -f $confdir/tmp $confdir/agent.properties
else
  printf "INFO: Workers is not set\n"
fi

printf "SUCCESS: Installation is now complete. If you like to make changes, edit $confdir/agent.properties\n"
exit 0
