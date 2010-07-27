#!/usr/bin/python
# $Id: hostvmstats.py 10054 2010-06-29 22:09:31Z abhishek $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x/java/scripts/vm/hypervisor/xenserver/hostvmstats.py $

import XenAPI
import urllib
import time
import logging
logging.basicConfig(filename='/tmp/xapilog',level=logging.DEBUG)
                      
def get_xapi_session():
    xapi = XenAPI.xapi_local();
    xapi.login_with_password("","")
    return xapi._session

def get_stats(collect_host_stats, consolidation_function, interval, start_time):
  try:
    session = get_xapi_session()
    
    if collect_host_stats == "true" :
    	url = "http://localhost/rrd_updates?"
   	url += "session_id=" + session
   	url += "&host=" + collect_host_stats
    	url += "&cf=" + consolidation_function
    	url += "&interval=" + str(interval)
    	url += "&start=" + str(int(time.time())-100)
    else :
    	url = "http://localhost/rrd_updates?"
   	url += "session_id=" + session
   	url += "&host=" + collect_host_stats
    	url += "&cf=" + consolidation_function
    	url += "&interval=" + str(interval)
    	url += "&start=" + str(int(time.time())-100)

    logging.debug("Calling URL: %s",url)
    sock = urllib.URLopener().open(url)
    xml = sock.read()
    sock.close()
    logging.debug("Size of returned XML: %s",len(xml))
    return xml
  except Exception,e:
    logging.exception("get_stats() failed")
    raise
