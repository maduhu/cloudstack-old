#!/bin/bash
# $Id: reconfigLB.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x/java/patches/kvm/root/reconfigLB.sh $
# @VERSION@

# save previous state
  mv /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.old
  mv /var/run/haproxy.pid /var/run/haproxy.pid.old

  mv /etc/haproxy/haproxy.cfg.new /etc/haproxy/haproxy.cfg
  kill -TTOU $(cat /var/run/haproxy.pid.old)
  sleep 2
  if haproxy -D -p /var/run/haproxy.pid -f /etc/haproxy/haproxy.cfg; then
    echo "New haproxy instance successfully loaded, stopping previous one."
    kill -KILL $(cat /var/run/haproxy.pid.old)
    rm -f /var/run/haproxy.pid.old
    exit 0
  else
    echo "New instance failed to start, resuming previous one."
    kill -TTIN $(cat /var/run/haproxy.pid.old)
    rm -f /var/run/haproxy.pid
    mv /var/run/haproxy.pid.old /var/run/haproxy.pid
    mv /etc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg.new
    mv /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg
    exit 1
  fi
