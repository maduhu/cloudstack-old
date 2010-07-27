package com.cloud.migration;

import java.io.File;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.migration.DiskOffering21VO.Type;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class Db20to21MigrationUtil {
    private static final Logger s_logger = Logger.getLogger(Db20to21MigrationUtil.class);

    protected DataCenterDao _dcDao;
	protected HostPodDao _podDao;
	protected ConfigurationDao _configDao;
	protected ClusterDao _clusterDao;
	protected HostDao _hostDao;
	protected StoragePoolDao _spDao;
	protected DomainDao _domainDao;
	protected ServiceOffering20Dao _serviceOffering20Dao;
	protected DiskOffering20Dao _diskOffering20Dao;
	protected ServiceOffering21Dao _serviceOffering21Dao;
	protected DiskOffering21Dao _diskOffering21Dao;
	protected ConsoleProxyDao _consoleProxyDao;
	protected SecondaryStorageVmDao _secStorageVmDao;
	protected VMInstanceDao _vmInstanceDao;
	protected VolumeDao _volumeDao;
	protected UserVmDao _userVmDao;
	protected DomainRouterDao _routerDao;
	
	protected long _consoleProxyServiceOfferingId;
	protected long _secStorageServiceOfferingId;
	protected long _domRServiceOfferingId;
	
	private void migrateZones() {
		boolean createCluster = false;
		String value = _configDao.getValue("xen.create.pools.in.pod");
		if(value == null || !value.equalsIgnoreCase("true")) {
			s_logger.info("System is not configured to use Xen server pool, we will skip creating cluster for pods");
		} else {
			createCluster = true;
		}
		
		// Displaying summarize data center/pod configuration from old DB before we continue
		SearchBuilder<DataCenterVO> sb = _dcDao.createSearchBuilder();
		sb.selectField(sb.entity().getId());
		sb.selectField(sb.entity().getName());
        sb.select(Func.COUNT, (Object[])null);
        sb.groupBy(sb.entity().getId(), sb.entity().getName());
        sb.done();
        
        SearchCriteria sc = sb.create();
        List<Object[]> results = _dcDao.searchAll(sc, (Filter)null);
        if(results.size() > 0) {
        	System.out.println("We've found following zones are deployed in your database");
        	for(Object[] cols : results) {
            	System.out.println("\tid: " + cols[0] + ",\tname: " + (String)cols[1] + ",\tpods in zone: " + (Long)cols[2]);
        	}
        	System.out.println("From 2.0 to 2.1, pod is required to have gateway configuration");
        	
        	for(Object[] cols : results) {
        		migrateZonePods(((BigInteger)cols[0]).longValue(), (String)cols[1], createCluster);
        		
				s_logger.info("Set system VM guest MAC in zone" + (String)cols[1]);
				migrateSystemVmGuestMacAndState(((BigInteger)cols[0]).longValue());
        	}
        } else {
        	System.out.println("We couldn't find any zone being deployed. Skip Zone/Pod migration");
        }
	}
	
	private void migrateZonePods(long zoneId, String zoneName, boolean createCluster) {
		SearchBuilder<HostPodVO> sb = _podDao.createSearchBuilder();
		sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
		sb.done();
		
		SearchCriteria sc = sb.create();
		sc.setParameters("zoneId", zoneId);
		
		List<HostPodVO> pods = _podDao.search(sc, null);
		if(pods.size() > 0) {
			for(HostPodVO pod : pods) {
				System.out.println("Migrating pod " + pod.getName() + " in zone " + zoneName + "...");
				System.out.println("Current pod " + pod.getName() + " configuration as");
				System.out.println("\tCIDR: " + pod.getCidrAddress() + "/" + pod.getCidrSize());
				System.out.println("\tGateway: " + pod.getGateway());
				System.out.print("Please type your gateway address for the pod: ");

				String gateway = readInput();
				pod.setGateway(gateway);
				_podDao.update(pod.getId(), pod);
				if(createCluster)
					migrateHostsInPod(zoneId, pod.getId(), pod.getName());
				
				System.out.println("Set last_host_id for VMs in pod " + pod.getName());
				migrateVmInstanceLastHostId(zoneId, pod.getId());
				
				System.out.println("Setup link local addresses, it will take a while, please wait...");
		    	String ipNums = _configDao.getValue("linkLocalIp.nums");
		    	int nums = Integer.parseInt(ipNums);
		    	if (nums > 16 || nums <= 0) {
		    		nums = 10;
		    	}
		    	
		    	/*local link ip address starts from 169.254.0.2 - 169.254.(nums)*/
		    	String[] ipRanges = NetUtils.getLinkLocalIPRange(nums);
				_dcDao.addLinkLocalPrivateIpAddress(zoneId, pod.getId(), ipRanges[0], ipRanges[1]);
			}
		}
	}
	
	private void migrateHostsInPod(long zoneId, long podId, String podName) {
		System.out.println("Creating cluster for pod " + podName);
		
		ClusterVO cluster = null; 
		
		SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
		sb.and("dc", sb.entity().getDataCenterId(), Op.EQ);
		sb.and("pod", sb.entity().getPodId(), Op.EQ);
		sb.and("type", sb.entity().getType(), Op.EQ);
		sb.done();
		
		SearchCriteria sc = sb.create();
		sc.setParameters("dc", zoneId);
		sc.setParameters("pod", podId);
		sc.setParameters("type", Host.Type.Routing);
		
		// join cluster for hosts in pod
		List<HostVO> hostsInPod = _hostDao.search(sc, null);
		if(hostsInPod.size() > 0) {
			if(cluster == null) {
				cluster = new ClusterVO(zoneId, podId, String.valueOf(podId));
				cluster = _clusterDao.persist(cluster);
			}
			
			for(HostVO host : hostsInPod) {
				host.setClusterId(cluster.getId());
				_hostDao.update(host.getId(), host);
				
				System.out.println("Join host " + host.getName() + " to auto-formed cluster");
			}
		}
		
		SearchBuilder<StoragePoolVO> sbPool = _spDao.createSearchBuilder();
		sbPool.and("dc", sbPool.entity().getDataCenterId(), Op.EQ);
		sbPool.and("pod", sbPool.entity().getPodId(), Op.EQ);
		sbPool.and("poolType", sbPool.entity().getPoolType(), Op.IN);
		sbPool.done();
		
		SearchCriteria scPool = sbPool.create();
		scPool.setParameters("dc", zoneId);
		scPool.setParameters("pod", podId);
		scPool.setParameters("poolType", StoragePoolType.NetworkFilesystem.toString(), StoragePoolType.IscsiLUN.toString());
		
		List<StoragePoolVO> sPoolsInPod = _spDao.search(scPool, null);
		if(sPoolsInPod.size() > 0) {
			if(cluster == null) {
				cluster = new ClusterVO(zoneId, podId, String.valueOf(podId));
				cluster = _clusterDao.persist(cluster);
			}
			
			for(StoragePoolVO spool : sPoolsInPod) {
				spool.setClusterId(cluster.getId());
				_spDao.update(spool.getId(), spool);
				
				System.out.println("Join host " + spool.getName() + " to auto-formed cluster");
			}
		}
	}
	
	private void composeDomainPath(DomainVO domain, StringBuilder sb) {
		if(domain.getParent() == null) {
			sb.append("/");
		} else {
			DomainVO parent = _domainDao.findById(domain.getParent());
			composeDomainPath(parent, sb);
			
			if(domain.getName().contains("/")) {
				System.out.println("Domain " + domain.getName() + " contains invalid domain character, replace it with -");
				sb.append(domain.getName().replace('/', '-'));
			} else {
				sb.append(domain.getName());
			}
			sb.append("/");
		}
	}
	
	private void migrateDomains() {
		System.out.println("Migrating domains...");

		// we shouldn't have too many domains in the system, use a very dumb way to setup domain path
		List<DomainVO> domains = _domainDao.listAll();
		for(DomainVO domain : domains) {
			StringBuilder path = new StringBuilder();
			composeDomainPath(domain, path);
			
			System.out.println("Convert domain path, domin: " + domain.getId() + ", path:" + path.toString());
			
			domain.setPath(path.toString());
			_domainDao.update(domain.getId(), domain);
		}
		
		System.out.println("All domains have been migrated to 2.1 format");
	}
	
	private void migrateServiceOfferings() {
		System.out.println("Migrating service offering...");

		List<ServiceOffering20VO> oldServiceOfferings = _serviceOffering20Dao.listAll();
		for(ServiceOffering20VO so20 : oldServiceOfferings) {
			ServiceOffering21VO so21 = new ServiceOffering21VO(so20.getName(), so20.getCpu(), so20.getRamSize(), so20.getSpeed(), so20.getRateMbps(),
				so20.getMulticastRateMbps(), so20.getOfferHA(), so20.getDisplayText(), so20.getGuestIpType(),
				so20.getUseLocalStorage(), false, null);
			so21 = _serviceOffering21Dao.persist(so21);

			if(so20.getId().longValue() != so21.getId().longValue()) {
				// Update all foreign reference from old value to new value, need to be careful with foreign key constraints
				updateServiceOfferingReferences(so20.getId().longValue(), so21.getId().longValue());
			}
		}
		
		boolean useLocalStorage = Boolean.parseBoolean(_configDao.getValue(Config.SystemVMUseLocalStorage.key()));
		
		// create service offering for system VMs and update references
		int proxyRamSize = NumbersUtil.parseInt(
			_configDao.getValue(Config.ConsoleProxyRamSize.key()), 
			ConsoleProxyManager.DEFAULT_PROXY_VM_RAMSIZE);
		ServiceOffering21VO soConsoleProxy = new ServiceOffering21VO("Fake Offering For DomP", 1,
			proxyRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized,
			useLocalStorage, true, null);
		soConsoleProxy.setUniqueName("Cloud.com-ConsoleProxy");
		soConsoleProxy = _serviceOffering21Dao.persist(soConsoleProxy);
		updateConsoleProxyServiceOfferingReferences(soConsoleProxy.getId());
		_consoleProxyServiceOfferingId = soConsoleProxy.getId();
		
		int secStorageVmRamSize = NumbersUtil.parseInt(
			_configDao.getValue(Config.SecStorageVmRamSize.key()), 
			SecondaryStorageVmManager.DEFAULT_SS_VM_RAMSIZE);
		ServiceOffering21VO soSecondaryVm = new ServiceOffering21VO("Fake Offering For Secondary Storage VM", 1, 
			secStorageVmRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, useLocalStorage, true, null);
		soSecondaryVm.setUniqueName("Cloud.com-SecondaryStorage");
		soSecondaryVm = _serviceOffering21Dao.persist(soSecondaryVm);
		updateSecondaryStorageServiceOfferingReferences(soSecondaryVm.getId());
		_secStorageServiceOfferingId = soSecondaryVm.getId();
		
        int routerRamSize = NumbersUtil.parseInt(_configDao.getValue("router.ram.size"), 128);
        ServiceOffering21VO soDomainRouter = new ServiceOffering21VO("Fake Offering For DomR", 1, 
        	routerRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, useLocalStorage, true, null);
        soDomainRouter.setUniqueName("Cloud.Com-SoftwareRouter");
        soDomainRouter = _serviceOffering21Dao.persist(soDomainRouter);
        updateDomainRouterServiceOfferingReferences(soDomainRouter.getId());
		_domRServiceOfferingId = soDomainRouter.getId();
		
		System.out.println("Service offering has been migrated to 2.1 format");
	}
	
	private void updateConsoleProxyServiceOfferingReferences(long serviceOfferingId) {
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
            		"UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='ConsoleProxy')");
            pstmt.setLong(1, serviceOfferingId);
            
            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for console proxy service offering change, affected rows: " + rows);
		} catch (SQLException e) {
			s_logger.error("Unhandled exception: ", e);
		} finally {
			txn.close();
		}
	}

	private void updateSecondaryStorageServiceOfferingReferences(long serviceOfferingId) {
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
            		"UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='SecondaryStorageVm')");
            pstmt.setLong(1, serviceOfferingId);
            
            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for secondary storage service offering change, affected rows: " + rows);
		} catch (SQLException e) {
			s_logger.error("Unhandled exception: ", e);
		} finally {
			txn.close();
		}
	}
	
	private void updateDomainRouterServiceOfferingReferences(long serviceOfferingId) {
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement(
            		"UPDATE volumes SET disk_offering_id=? WHERE instance_id IN (SELECT id FROM vm_instance WHERE type='DomainRouter')");
            pstmt.setLong(1, serviceOfferingId);
            
            int rows = pstmt.executeUpdate();
            s_logger.info("Update volumes for secondary storage service offering change, affected rows: " + rows);
		} catch (SQLException e) {
			s_logger.error("Unhandled exception: ", e);
		} finally {
			txn.close();
		}
	}
	
	private void updateServiceOfferingReferences(long oldServiceOfferingId, long newServiceOfferingId) {
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement("UPDATE user_vm SET service_offering_id=? WHERE service_offering_id=?");
            
            pstmt.setLong(1, newServiceOfferingId);
            pstmt.setLong(2, oldServiceOfferingId);
            
            int rows = pstmt.executeUpdate();
            s_logger.info("Update user_vm for service offering change (" + oldServiceOfferingId + "->" + newServiceOfferingId + "), affected rows: " + rows);
	            
		} catch (SQLException e) {
			s_logger.error("Unhandled exception: ", e);
		} finally {
			txn.close();
		}
	}
	
	private void migrateDiskOfferings() {
		System.out.println("Migrating disk offering...");
		
		List<DiskOffering20VO> oldDiskOfferings = _diskOffering20Dao.listAll();
		for(DiskOffering20VO do20 : oldDiskOfferings) {
			DiskOffering21VO do21 = new DiskOffering21VO(do20.getDomainId(), do20.getName(), do20.getDisplayText(), do20.getDiskSize(), 
				do20.getMirrored(), null);
			do21.setType(Type.Disk);
			
			do21 = _diskOffering21Dao.persist(do21);
			if(do20.getId().longValue() != do21.getId().longValue()) {
				updateDiskOfferingReferences(do20.getId().longValue(), do21.getId().longValue());
			}
		}
		
		FixupNullDiskOfferingInVolumes();
		
		System.out.println("Disk offering has been migrated to 2.1 format");
	}
	
	private void FixupNullDiskOfferingInVolumes() {
		System.out.println("Fixup NULL disk_offering_id references in volumes table ...");
		
		SearchCriteria scDiskOffering = _diskOffering21Dao.createSearchCriteria();
		List<DiskOffering21VO> offeringList = _diskOffering21Dao.search(scDiskOffering, 
			new Filter(DiskOffering21VO.class, "diskSize", true, null, null));
		
		for(DiskOffering21VO offering : offeringList) {
			s_logger.info("Disk offering name: " + offering.getName() + ", disk size: " + offering.getDiskSizeInBytes());
		}
		
		SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
		sb.and("diskOfferingId", sb.entity().getDiskOfferingId(), Op.NULL);
		sb.done();
		
		SearchCriteria sc = sb.create();
		List<VolumeVO> volumes = _volumeDao.search(sc, null);
		
		if(volumes.size() > 0) {
			for(VolumeVO vol : volumes) {
				if(vol.getInstanceId() != null) {
					VMInstanceVO vmInstance = _vmInstanceDao.findById(vol.getInstanceId());
					
					if(vmInstance.getType() == VirtualMachine.Type.User) {
						// if the volume is for user VM, we can retrieve the information from service_offering_id
						UserVmVO  userVm = _userVmDao.findById(vol.getInstanceId());
						if(userVm != null) {
							// following operation requires that all service offerings should have been fixed up already
							vol.setDiskOfferingId(userVm.getServiceOfferingId());
						} else {
							System.out.println("Data integrity could not be fixed up for volume: " + vol.getId() + " because its owner user vm no longer exists");
						}
					} else if(vmInstance.getType() == VirtualMachine.Type.ConsoleProxy) {
						vol.setDiskOfferingId(this._consoleProxyServiceOfferingId);
					} else if(vmInstance.getType() == VirtualMachine.Type.SecondaryStorageVm) {
						vol.setDiskOfferingId(this._secStorageServiceOfferingId);
					} else if(vmInstance.getType() == VirtualMachine.Type.DomainRouter) {
						vol.setDiskOfferingId(this._domRServiceOfferingId);
					}
				} else {
					System.out.println("volume: " + vol.getId() + " is standalone, fix disck_offering_id based on volume size");

					// try to guess based on volume size and fill it in
					boolean found = false;
					for(DiskOffering21VO do21 : offeringList) {
						if(vol.getSize() > do21.getDiskSizeInBytes()) {
							found = true;
							System.out.println("volume: " + vol.getId() + " disck_offering_id is fixed to " + do21.getId());
							vol.setDiskOfferingId(do21.getId());
							break;
						}
					}
					
					if(!found) {
						System.out.println("volume: " + vol.getId() + " disck_offering_id is fixed to " + offeringList.get(offeringList.size() - 1).getId());
						vol.setDiskOfferingId(offeringList.get(offeringList.size() - 1).getId());
					}
				}
				
				_volumeDao.update(vol.getId(), vol);
			}
		}
		
		System.out.println("Disk offering fixup is done");
	}
	
	private void updateDiskOfferingReferences(long oldDiskOfferingId, long newDiskOfferingId) {
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
	        PreparedStatement pstmt = null;
            pstmt = txn.prepareAutoCloseStatement("UPDATE vm_disk SET disk_offering_id=? WHERE disk_offering_id=?");
            
            pstmt.setLong(1, newDiskOfferingId);
            pstmt.setLong(2, oldDiskOfferingId);
            
            int rows = pstmt.executeUpdate();
            pstmt.close();
            
            s_logger.info("Update vm_disk for disk offering change (" + oldDiskOfferingId + "->" + newDiskOfferingId + "), affected rows: " + rows);
            
            pstmt = txn.prepareAutoCloseStatement("UPDATE volumes SET disk_offering_id=? WHERE disk_offering_id=?");
            pstmt.setLong(1, newDiskOfferingId);
            pstmt.setLong(2, oldDiskOfferingId);
            rows = pstmt.executeUpdate();
            pstmt.close();
            s_logger.info("Update volumes for disk offering change (" + oldDiskOfferingId + "->" + newDiskOfferingId + "), affected rows: " + rows);
		} catch (SQLException e) {
			s_logger.error("Unhandled exception: ", e);
		} finally {
			txn.close();
		}
	}
	
	private void migrateSystemVmGuestMacAndState(long zoneId) {
		// for console proxy VMs
		SearchBuilder<ConsoleProxyVO> sb = _consoleProxyDao.createSearchBuilder();
		sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
		sb.done();
		
		SearchCriteria sc = sb.create();
		sc.setParameters("zoneId", zoneId);
		
		List<ConsoleProxyVO> proxies =_consoleProxyDao.search(sc, null);
		for(ConsoleProxyVO proxy : proxies) {
			String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(zoneId, (1L << 31));
			String guestMacAddress = macAddresses[0];
			
			proxy.setGuestMacAddress(guestMacAddress);
			if(proxy.getState() == State.Running || proxy.getState() == State.Starting) {
				System.out.println("System VM " + proxy.getName() + " is in active state, mark it to Stopping state for migration");
				proxy.setState(State.Stopping);
			}
			
			String guestIpAddress = _dcDao.allocateLinkLocalPrivateIpAddress(proxy.getDataCenterId(), proxy.getPodId(), proxy.getId());
			proxy.setGuestIpAddress(guestIpAddress);
			proxy.setGuestNetmask("255.255.0.0");
			
			System.out.println("Assign link loal address to proxy " + proxy.getName() + ", link local address: " + guestIpAddress);
			_consoleProxyDao.update(proxy.getId(), proxy);
		}
		
		// for secondary storage VMs
		SearchBuilder<SecondaryStorageVmVO> sb2 = _secStorageVmDao.createSearchBuilder();
		sb2.and("zoneId", sb2.entity().getDataCenterId(), Op.EQ);
		sb2.done();
		
		SearchCriteria sc2 = sb2.create();
		sc2.setParameters("zoneId", zoneId);
		
		List<SecondaryStorageVmVO> secStorageVms =_secStorageVmDao.search(sc2, null);
		for(SecondaryStorageVmVO secStorageVm : secStorageVms) {
			String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(zoneId, (1L << 31));
			String guestMacAddress = macAddresses[0];
			
			secStorageVm.setGuestMacAddress(guestMacAddress);
			if(secStorageVm.getState() == State.Running || secStorageVm.getState() == State.Starting) {
				System.out.println("System VM " + secStorageVm.getName() + " is in active state, mark it to Stopping state for migration");
				secStorageVm.setState(State.Stopping);
			}
			
			String guestIpAddress = _dcDao.allocateLinkLocalPrivateIpAddress(secStorageVm.getDataCenterId(), secStorageVm.getPodId(), secStorageVm.getId());
			secStorageVm.setGuestIpAddress(guestIpAddress);
			secStorageVm.setGuestNetmask("255.255.0.0");
			
			System.out.println("Assign link loal address to secondary storage VM " + secStorageVm.getName() + ", link local address: " + guestIpAddress);
			_secStorageVmDao.update(secStorageVm.getId(), secStorageVm);
		}
		
		// for Domain Router VMs
		// Although we can list those we are interested, but just too lazy, list all of them and check their states. 
		SearchBuilder<DomainRouterVO> sb3 = _routerDao.createSearchBuilder();
		sb3.and("zoneId", sb3.entity().getDataCenterId(), Op.EQ);
		sb3.done();
		
		SearchCriteria sc3 = sb3.create();
		sc3.setParameters("zoneId", zoneId);
		List<DomainRouterVO> domRs = _routerDao.search(sc3, null);
		for(DomainRouterVO router :  domRs) {
			if(router.getState() == State.Running || router.getState() == State.Starting) {
				router.setState(State.Stopping);
				
				System.out.println("System VM " + router.getName() + " is in active state, mark it to Stopping state for migration");
				_routerDao.update(router.getId(), router);
			}
		}
	}
	
	private void migrateVmInstanceLastHostId(long zoneId, long podId) {
		SearchBuilder<VMInstanceVO> sb = _vmInstanceDao.createSearchBuilder();
		sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
		sb.and("podId", sb.entity().getPodId(), Op.EQ);
		sb.done();
		
		Random rand = new Random();
		SearchCriteria sc = sb.create();
		sc.setParameters("zoneId", zoneId);
		sc.setParameters("podId", podId);
		List<VMInstanceVO> vmInstances = _vmInstanceDao.search(sc, null);
		List<HostVO> podHosts = getHostsInPod(zoneId, podId);
		for(VMInstanceVO vm : vmInstances) {
			if(vm.getHostId() != null) {
				vm.setLastHostId(vm.getHostId());
			} else {
				if(podHosts.size() > 0) {
					int next = rand.nextInt(podHosts.size());
					vm.setLastHostId(podHosts.get(next).getId());
				}
			}
			_vmInstanceDao.update(vm.getId(), vm);
		}
	}
	
	private List<HostVO> getHostsInPod(long zoneId, long podId) {
		SearchBuilder<HostVO> sb = _hostDao.createSearchBuilder();
		sb.and("zoneId", sb.entity().getDataCenterId(), Op.EQ);
		sb.and("podId", sb.entity().getPodId(), Op.EQ);
		sb.and("type", sb.entity().getType(), Op.EQ);
		sb.done();
		
		SearchCriteria sc = sb.create();
		sc.setParameters("zoneId", zoneId);
		sc.setParameters("podId", podId);
		sc.setParameters("type", Host.Type.Routing.toString());
		
		return _hostDao.search(sc, null);
	}
	
	private void migrateVolumDeviceIds() {
		System.out.println("Migrating device_id for volumes, this may take a while, please wait...");
		SearchCriteria sc = _vmInstanceDao.createSearchCriteria();
		List<VMInstanceVO> vmInstances = _vmInstanceDao.search(sc, null);
		
		long deviceId = 1;
		for(VMInstanceVO vm: vmInstances) {
			SearchBuilder<VolumeVO> sb = _volumeDao.createSearchBuilder();
			sb.and("instanceId", sb.entity().getInstanceId(), Op.EQ);
			sb.done();
			
			SearchCriteria sc2 = sb.create();
			sc2.setParameters("instanceId", vm.getId());
			
			List<VolumeVO> volumes = _volumeDao.search(sc2, null);
			deviceId = 1;	// reset for each VM iteration
			for(VolumeVO vol : volumes) {
				if(vol.getVolumeType() == VolumeType.ROOT) {
					System.out.println("Setting root volume device id to zero, vol: " + vol.getName() + ", instance: " + vm.getName());
					
					vol.setDeviceId(0L);
				} else if(vol.getVolumeType() == VolumeType.DATADISK) {
					System.out.println("Setting data volume device id, vol: " + vol.getName() + ", instance: " + vm.getName() + ", device id: " + deviceId);
					
					vol.setDeviceId(deviceId);
					
					// don't use device ID 3
					if(++deviceId == 3)
						deviceId++;
				} else {
					System.out.println("Unsupported volume type found for volume: " + vol.getName());
				}
				
				_volumeDao.update(vol.getId(), vol);
			}
		}
		
		System.out.println("Migrating device_id for volumes done");
	}
	
	private void doMigration() {
		setupComponents();
		
		migrateZones();
		migrateDomains();
		migrateServiceOfferings();
		migrateDiskOfferings();
		migrateVolumDeviceIds();
		
		System.out.println("Migration done");
	}
	
	private String readInput() {
		try {
			Scanner in = new Scanner(System.in);
			String input =  in.nextLine();
			return input;
		} catch(NoSuchElementException e) {
			return "";
		}
	}
	
	private void setupComponents() {
		ComponentLocator.getLocator("migration", "migration-components.xml", "log4j-cloud.xml");
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		
		_configDao = locator.getDao(ConfigurationDao.class);
		_podDao = locator.getDao(HostPodDao.class);
		_dcDao = locator.getDao(DataCenterDao.class);
		_clusterDao = locator.getDao(ClusterDao.class);
		_hostDao = locator.getDao(HostDao.class);
		_spDao = locator.getDao(StoragePoolDao.class);
		_domainDao = locator.getDao(DomainDao.class);
		_serviceOffering20Dao = locator.getDao(ServiceOffering20Dao.class);
		_diskOffering20Dao = locator.getDao(DiskOffering20Dao.class);
		_serviceOffering21Dao = locator.getDao(ServiceOffering21Dao.class);
		_diskOffering21Dao = locator.getDao(DiskOffering21Dao.class);
		_consoleProxyDao = locator.getDao(ConsoleProxyDao.class);
		_secStorageVmDao = locator.getDao(SecondaryStorageVmDao.class);
		_vmInstanceDao = locator.getDao(VMInstanceDao.class);
		_volumeDao = locator.getDao(VolumeDao.class);
		_userVmDao = locator.getDao(UserVmDao.class);
		_routerDao = locator.getDao(DomainRouterDao.class);
	}
	
	public static void main(String[] args) {
        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

        if(file != null) {
			System.out.println("Log4j configuration from : " + file.getAbsolutePath());
			DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
		} else {
			System.out.println("Configure log4j with default properties");
		}
		
		new Db20to21MigrationUtil().doMigration();
		System.exit(0);
	}
}
