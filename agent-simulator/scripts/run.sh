#!/usr/bin/env bash
#run.sh runs the agent simulator client.
java -cp cloud-utils.jar:agent-simulator.jar:log4j-1.2.15.jar:apache-log4j-extras-1.0.jar:ws-commons-util-1.0.2.jar:xmlrpc-client-3.1.3.jar:cloud-agent.jar:cloud-core.jar:xmlrpc-common-3.1.3.jar:javaee-api-5.0-1.jar:gson-1.3.jar:commons-httpclient-3.1.jar:commons-logging-1.1.1.jar:commons-codec-1.4.jar:commons-collections-3.2.1.jar:commons-pool-1.4.jar:.:./conf com.cloud.agent.AgentSimulator $@

 
