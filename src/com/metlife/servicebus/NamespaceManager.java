/**
 * 
 */
package com.metlife.servicebus;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoft.windowsazure.services.servicebus.ServiceBusConfiguration;
import com.microsoft.windowsazure.services.servicebus.ServiceBusContract;
import com.microsoft.windowsazure.services.servicebus.ServiceBusService;
import com.microsoft.windowsazure.services.servicebus.models.CreateQueueResult;
import com.microsoft.windowsazure.services.servicebus.models.ListQueuesOptions;
import com.microsoft.windowsazure.services.servicebus.models.ListQueuesResult;
import com.microsoft.windowsazure.services.servicebus.models.QueueInfo;

/**
 * @author rprasad017
 * This class is used to ensure that backlog queues exists
 */
public class NamespaceManager {
	
	private ServiceBusContract service;
	
	public NamespaceManager(String namespace, String sasKeyName, String sasKey, String serviceBusRootUri) {
		Configuration config = ServiceBusConfiguration.configureWithSASAuthentication(
        		namespace, sasKeyName, sasKey, serviceBusRootUri);
		
		this.service = ServiceBusService.create(config);
	}

	public static NamespaceManager createFromConfig(String namespace, String sasKeyName, String sasKey, String serviceBusRootUri) {
		NamespaceManager namespaceManager = new NamespaceManager(namespace, sasKeyName,
				sasKey, serviceBusRootUri);
		return namespaceManager;
	}
	
	/**
	 * Creates queue
	 * Do not use this method
	 * @param queueInfo
	 * @return
	 */
	public QueueInfo CreateQueue(QueueInfo queueInfo) {
		try
		{
			CreateQueueResult result = service.createQueue(queueInfo);
			return result.getValue();
		}
		catch (ServiceException e)
		{
		    System.err.print("ServiceException encountered: ");
		    System.err.println(e.getMessage());
		}
		return null;
	}	
	
	/**
	 * Creates queue
	 * Do not use this method
	 * @param path
	 * @return
	 */
	public QueueInfo CreateQueue(String path) {
		QueueInfo queueInfo = createQueueInfo(path);
		try
		{
		    CreateQueueResult result = service.createQueue(queueInfo);
		    return result.getValue();
		}
		catch (ServiceException e)
		{
		    System.err.print("ServiceException encountered: ");
		    System.err.println(e.getMessage());
		    System.exit(-1);
		}
		return null;
	}
	
	/**
	 * Deleted existing queue
	 * @param path
	 */
	public void deleteQueue(String path) {
		try
		{
		    service.deleteQueue(path);
		}
		catch (ServiceException e)
		{
		    System.err.print("ServiceException encountered: ");
		    System.err.println(e.getMessage());
		    System.exit(-1);
		}
	}
	
	/**
	 * Retrieves collection of all queue infos in the service namespace
	 * @return
	 */
	public List<QueueInfo> getQueues() {
		List<QueueInfo> exisingQueues = new ArrayList<QueueInfo>();
		try {
			ListQueuesResult queueList = service.listQueues();
			for (QueueInfo queueInfo : queueList.getItems()) {
				exisingQueues.add(queueInfo);
			}
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return exisingQueues;
	}
	
	/**
	 * Retrieves collection of all queue names in the service namespace
	 * @return
	 */
	public List<String> getQueueNames() {
		List<QueueInfo> exisingQueues = getQueues();
		List<String> queuenames = new ArrayList<String>();
		for (QueueInfo queueInfo : exisingQueues) {
			queuenames.add(queueInfo.getPath());
		}
		return queuenames;
	}
	
	/**
	 * Retrieves collection of all queues in the service namespace with the specified filter
	 * @param filter
	 * @return
	 */
	public List<QueueInfo> getQueues(String filter) {
		List<QueueInfo> exisingQueues = new ArrayList<QueueInfo>();
		try {
			ListQueuesOptions options = ListQueuesOptions.DEFAULT;
			options.setFilter(filter);
			ListQueuesResult queueList = service.listQueues(options);
			for (QueueInfo queueInfo : queueList.getItems()) {
				exisingQueues.add(queueInfo);
			}
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return exisingQueues;
	}
	
	/**
	 * Construct QueueInfo
	 * @param queueName
	 * @return
	 */
	private QueueInfo createQueueInfo(String queueName) {
		QueueInfo queueInfo = new QueueInfo(queueName);
		queueInfo.setMaxSizeInMegabytes(5120l);
		queueInfo.setMaxDeliveryCount(Integer.MAX_VALUE);
		
		DatatypeFactory factory = null;
		try {
			factory = DatatypeFactory.newInstance();
			Duration maxDuration = factory.newDuration("PT5M");
			Duration lockDuration = factory.newDuration("PT2M");	// 1 minute
			
			queueInfo.setDefaultMessageTimeToLive(maxDuration);
			queueInfo.setAutoDeleteOnIdle(maxDuration);
			queueInfo.setLockDuration(lockDuration);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		queueInfo.setDeadLetteringOnMessageExpiration(true);
		queueInfo.setEnableBatchedOperations(true);
		return queueInfo;
	}

}
